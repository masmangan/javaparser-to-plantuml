/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.javaparser.ast.CompilationUnit;

import io.github.masmangan.assis.internal.ClassDiagramGeneration;
import io.github.masmangan.assis.internal.DeclaredIndex;
import io.github.masmangan.assis.io.SmartSourceRootManager;

/**
 * Generates a PlantUML class diagram from one or more Java source roots.
 *
 * <p>
 * This generator parses Java sources using JavaParser (configured for Java 17),
 * indexes declared types (including nested types), and writes a PlantUML
 * class-diagram file.
 *
 * <h2>Determinism</h2>
 * <p>
 * For reproducible output, callers should provide source roots in a stable
 * order. If {@code sourceRoots} is an unordered set, consider sorting it before
 * calling this method.
 *
 * <h2>Output</h2>
 * <p>
 * The diagram includes:
 * <ul>
 * <li>Type declarations grouped by package</li>
 * <li>Inheritance and implementation relationships</li>
 * <li>Nesting relationships for inner/nested types</li>
 * <li>Associations inferred from fields and record components</li>
 * </ul>
 *
 * @author Marco Mangan
 */
public class GenerateClassDiagram {

	/**
	 * Logger used by the generator to report progress and parse/write issues.
	 */
	private static final Logger logger = Logger.getLogger(GenerateClassDiagram.class.getName());

	/**
	 * No constructor available.
	 */
	private GenerateClassDiagram() {
	}

	/**
	 * Generates a PlantUML class diagram from the given Java source roots.
	 *
	 * <p>
	 * Each path in {@code sourceRoots} should point to a directory that represents
	 * a Java source root (e.g., {@code src/main/java}). Non-existing roots are
	 * logged as warnings.
	 *
	 * <p>
	 * If {@code outDir} exists and is a directory, the output file name is fixed as
	 * {@code class-diagram.puml} within that directory. Otherwise, output directory
	 * is created; file paths rejected.
	 *
	 * @param sourceRoots one or more Java source roots; must not be {@code null}
	 * @param outDir      output directory; must not be {@code null}
	 * @throws NullPointerException if {@code sourceRoots} or {@code outDir} is
	 *                              {@code null}
	 * @throws IOException          if an I/O error occurs while reading sources or
	 *                              writing the output file
	 */
	public static void generate(final Set<Path> sourceRoots, final Path outDir) throws IOException {
		Objects.requireNonNull(outDir, "outDir");

		Path dir = outDir.normalize();
		if (Files.exists(dir) && !Files.isDirectory(dir)) {
			throw new IllegalArgumentException("outDir must be a directory: " + dir.toAbsolutePath());
		}
		Files.createDirectories(dir);
		Path outputFile = dir.resolve("class-diagram.puml");
		logger.log(Level.INFO, () -> "Writing " + outputFile);

		List<CompilationUnit> units = SmartSourceRootManager.autoscan(sourceRoots);

		DeclaredIndex index = new DeclaredIndex();
		index.fill(units);

		new ClassDiagramGeneration(outputFile, index).run();

	}

}