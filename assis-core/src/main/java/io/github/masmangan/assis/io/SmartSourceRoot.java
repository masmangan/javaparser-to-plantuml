/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.io;

import static com.github.javaparser.utils.CodeGenerationUtils.packageAbsolutePath;
import static com.github.javaparser.utils.Utils.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.utils.Log;
import com.github.javaparser.utils.SourceRoot;

import io.github.masmangan.assis.util.DeterministicFileTreeWalker;
import io.github.masmangan.assis.util.DeterministicPathList;

/**
 * A {@link SourceRoot} subclass that parses Java source files recursively even
 * when directory names are not valid Java identifiers (for example, module
 * folders containing {@code '-'}).
 *
 * <p>
 * Package names are taken from {@code package} declarations in the parsed
 * source files, not from directory names.
 *
 * <p>
 * This source root installs a fixed {@link JavaSymbolSolver} backed by a
 * {@link JavaParserTypeSolver} rooted at {@code rootPath}. The configuration is
 * locked after construction because other parts of the system rely on a stable
 * "unparsed types remain unresolved" rule (i.e., no
 * {@code ReflectionTypeSolver}).
 */
public class SmartSourceRoot extends SourceRoot {

	private final Path rootPath;
	private boolean locked = false;

	/**
	 * Directory names skipped during traversal (VCS metadata, build output, IDE
	 * caches).
	 */
	public static final Set<String> SKIP_DIR_NAMES = Set.of(".git", ".idea", ".gradle", ".mvn", "target", "build",
			"out", "node_modules");

	public SmartSourceRoot(Path root) {
		super(root);
		this.rootPath = root;

		CombinedTypeSolver ts = new CombinedTypeSolver();
		// Intentionally source-only: unparsed types remain unresolved (no
		// ReflectionTypeSolver).
		
		ts.add(new JavaParserTypeSolver(root));
		ts.add(new ReflectionTypeSolver());

		JavaSymbolSolver jss = new JavaSymbolSolver(ts);

		ParserConfiguration cfg = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
				.setSymbolResolver(jss);

		super.setParserConfiguration(cfg);
		locked = true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * This implementation differs from {@link SourceRoot} by:
	 * <ul>
	 * <li>Deterministic file discovery order (D1) via
	 * {@link DeterministicFileTreeWalker}
	 * <li>Traversing directories whose names are not valid Java identifiers
	 * <li>Skipping hidden directories and common tool directories
	 * </ul>
	 */
	@Override
	public List<ParseResult<CompilationUnit>> tryToParse(String startPackage) throws IOException {
		assertNotNull(startPackage);
		logPackageNew(startPackage);

		final Path startPath = packageAbsolutePath(rootPath, startPackage);
		if (!Files.exists(startPath)) {
			return getCache();
		}

		// D1: deterministic discovery order within this source root.
		DeterministicPathList javaFiles = DeterministicFileTreeWalker.discoverJavaFiles(Set.of(startPath),
				dir -> shouldVisitDirectory(dir, startPath));

		for (Path file : javaFiles) {
			Path parent = file.getParent();
			Path relative = (parent == null) ? rootPath : rootPath.relativize(parent);
			String pkgPath = relative.toString().replace('\\', '/');
			tryToParse(pkgPath, file.getFileName().toString());
		}

		return getCache();
	}

	/**
	 * Directory filter used during traversal.
	 *
	 * @param dir       directory being considered
	 * @param startPath directory where traversal started for this call
	 */
	private boolean shouldVisitDirectory(Path dir, Path startPath) {
		String name = (dir.getFileName() == null) ? "" : dir.getFileName().toString();

		try {
			if (!dir.equals(startPath) && Files.isHidden(dir)) {
				Log.trace("Not processing directory \"%s\"", () -> name);
				return false;
			}
		} catch (IOException e) {
			// Best effort: if we cannot determine, do not skip
			Log.trace("Could not determine if directory is hidden: \"%s\"", () -> name);
		}

		if (SKIP_DIR_NAMES.contains(name)) {
			Log.trace("Skipping directory \"%s\"", () -> name);
			return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * In this subclass, the parser configuration is fixed after construction;
	 * calling this method is not supported.
	 */
	@Override
	public SourceRoot setParserConfiguration(ParserConfiguration parserConfiguration) {
		if (locked) {
			throw new IllegalArgumentException("SmartSourceRoot has a fixed configuration!");
		}
		return super.setParserConfiguration(parserConfiguration);
	}

	private void logPackageNew(String startPackage) {
		if (!startPackage.isEmpty()) {
			Log.info("Parsing package \"%s\"", () -> startPackage);
		}
	}
}