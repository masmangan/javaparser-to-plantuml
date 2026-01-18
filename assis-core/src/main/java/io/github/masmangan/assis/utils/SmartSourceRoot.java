/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.utils;

import static com.github.javaparser.utils.CodeGenerationUtils.packageAbsolutePath;
import static com.github.javaparser.utils.Utils.assertNotNull;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.utils.Log;
import com.github.javaparser.utils.SourceRoot;

/**
 * SourceRoot that also parses folders that are not valid Java identifiers (e.g.
 * with '-').
 */
public class SmartSourceRoot extends SourceRoot {

	/**
	 *
	 */
	private final Path rootPath;

	/**
	 *
	 */
	private boolean locked = false;

	/**
	 *
	 */
	private static final Set<String> SKIP_DIR_NAMES = Set.of(".git", ".idea", ".gradle", ".mvn", "target", "build",
			"out", "node_modules");

	/**
	 *
	 * @param root
	 */
	public SmartSourceRoot(Path root) {
		super(root);
		this.rootPath = root;

		CombinedTypeSolver ts = new CombinedTypeSolver();
		// ts.add(new ReflectionTypeSolver()); // JDK / runtime types
		ts.add(new JavaParserTypeSolver(root)); // source types under this root

		JavaSymbolSolver jss = new JavaSymbolSolver(ts);

		ParserConfiguration cfg = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
				.setSymbolResolver(jss);

		super.setParserConfiguration(cfg);
		locked = true;
	}

	@Override
	public List<ParseResult<CompilationUnit>> tryToParse(String startPackage) throws IOException {
		assertNotNull(startPackage);
		logPackageNew(startPackage);

		final Path startPath = packageAbsolutePath(rootPath, startPackage);
		if (!Files.exists(startPath)) {
			return getCache();
		}

		Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (!attrs.isDirectory() && file.toString().endsWith(".java")) {
					Path parent = file.getParent();
					Path relative = (parent == null) ? rootPath : rootPath.relativize(parent);

					String pkgPath = relative.toString().replace('\\', '/');

					tryToParse(pkgPath, file.getFileName().toString());
				}
				return CONTINUE;
			}

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return isSensibleDirectoryToEnterNew(dir) ? CONTINUE : SKIP_SUBTREE;
			}
		});

		return getCache();
	}

	/**
	 *
	 * @param dir
	 * @return
	 * @throws IOException
	 */
	private boolean isSensibleDirectoryToEnterNew(Path dir) throws IOException {
		String name = dir.getFileName() == null ? "" : dir.getFileName().toString();

		if (!rootPath.equals(dir) && Files.isHidden(dir)) {
			Log.trace("Not processing directory \"%s\"", () -> name);
			return false;
		}

		if (SKIP_DIR_NAMES.contains(name)) {
			Log.trace("Skipping directory \"%s\"", () -> name);
			return false;
		}

		return true;
	}

	@Override
	public SourceRoot setParserConfiguration(ParserConfiguration parserConfiguration) {
		if (locked) {
			throw new IllegalArgumentException("SmartSourceRoot has a fixed configuration!");
		}
		return super.setParserConfiguration(parserConfiguration);
	}

	/**
	 *
	 * @param startPackage
	 */
	private void logPackageNew(String startPackage) {
		if (!startPackage.isEmpty()) {
			Log.info("Parsing package \"%s\"", () -> startPackage);
		}
	}
}