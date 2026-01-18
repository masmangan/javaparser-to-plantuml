/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.io;

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
 * A {@link SourceRoot} subclass that parses Java source files recursively even
 * when directory names are not valid Java identifiers (for example, module
 * folders containing {@code '-'}).
 *
 * <p>
 * The {@code root} may or may not be the root of the package structure. Parsing
 * starts at {@code startPackage} and visits all subdirectories that are
 * considered eligible. Eligible directories must not be hidden and must not
 * match common tool directory names (see {@link #SKIP_DIR_NAMES}). If you need
 * to process such a directory, point {@code root} directly to it.
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
 *
 * <p>
 * This class aims to preserve the original {@link SourceRoot} behavior
 * (parse/write/cache) while extending directory traversal and providing a
 * default symbol-solver configuration.
 *
 * @see SourceRoot
 * @see JavaSymbolSolver
 * @see JavaParserTypeSolver
 */
public class SmartSourceRoot extends SourceRoot {
	/*
	 * Design note -----------
	 *
	 * This implementation currently relies on behavior in JavaParser/SourceRoot
	 * that is not part of a strong, documented compatibility contract. Today it
	 * works because parsing ultimately trusts package declarations inside files
	 * rather than requiring directory names to match valid Java identifiers.
	 *
	 * If future JavaParser/SymbolSolver revisions change this behavior,
	 * SmartSourceRoot may need to take ownership of the feature by explicitly
	 * discovering package roots.
	 *
	 * Planned direction:
	 *
	 * - Walk the file system under a project root and detect one or more package
	 * roots (possibly across multiple modules).
	 *
	 * - Configure a type solver per discovered package root (or otherwise model
	 * module boundaries) rather than assuming a single solver rooted at {@code
	 * rootPath}.
	 *
	 * - Keep the "unparsed types remain unresolved" rule unless/until the system is
	 * updated to accept reflection-based resolution.
	 *
	 * This is a deliberate but fragile use of SourceRoot, chosen to avoid
	 * duplicating logic while the behavior remains stable.
	 */

	private final Path rootPath;

	private boolean locked = false;

	/**
	 * Directory names skipped during traversal (VCS metadata, build output, IDE
	 * caches).
	 */
	public static final Set<String> SKIP_DIR_NAMES = Set.of(".git", ".idea", ".gradle", ".mvn", "target", "build",
			"out", "node_modules");

	/**
	 * Creates a smart source root.
	 *
	 * <p>
	 * {@code root} may be the package root itself (for example,
	 * {@code javaparser/javaparser-core/src/main/java}) or a directory above it
	 * (for example, {@code javaparser/javaparser-core/src}).
	 *
	 * <p>
	 * If you point to a directory above the package root, ensure it is intentional:
	 * a single call may traverse additional folders such as {@code src/main/java}
	 * and {@code src/test/java}. Non-source folders may also be visited unless
	 * excluded by the directory filters.
	 *
	 * @param root the root directory used as the base for traversal and symbol
	 *             solving
	 */
	public SmartSourceRoot(Path root) {
		super(root);
		this.rootPath = root;

		CombinedTypeSolver ts = new CombinedTypeSolver();
		// Intentionally source-only: unparsed types remain unresolved (no
		// ReflectionTypeSolver).
		ts.add(new JavaParserTypeSolver(root));

		JavaSymbolSolver jss = new JavaSymbolSolver(ts);

		ParserConfiguration cfg = new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17)
				.setSymbolResolver(jss);

		super.setParserConfiguration(cfg);
		locked = true;
	}

	/**
	 * {@inheritDoc}
	 *
	 * This implementation differs from {@link SourceRoot} by traversing directories
	 * whose names are not valid Java identifiers and by skipping hidden directories
	 * and common tool directories (see {@link #SKIP_DIR_NAMES}).
	 */
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
				return shouldVisitDirectory(dir) ? CONTINUE : SKIP_SUBTREE;
			}
		});

		return getCache();
	}

	private boolean shouldVisitDirectory(Path dir) throws IOException {
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