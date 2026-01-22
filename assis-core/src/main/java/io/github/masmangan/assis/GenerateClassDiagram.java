/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
 * <p>
 * This generator normalizes and sorts source roots to ensure reproducible
 * output, regardless of input order.
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

	private static final String CLASS_DIAGRAM_PUML = "class-diagram.puml";

	private static final Logger logger = Logger.getLogger(GenerateClassDiagram.class.getName());

	/*
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
	 * @param sourceRoots one or more Java source roots; must not be {@code null} or
	 *                    empty
	 *
	 * @param outDir      output directory; must not be {@code null}; if it exists,
	 *                    it must be a directory
	 *
	 * @throws NullPointerException     if {@code sourceRoots} or {@code outDir} is
	 *                                  {@code null}
	 *
	 * @throws IllegalArgumentException if {@code outDir} exists and is not a
	 *                                  directory; if sourceRoots is empty
	 *
	 * @throws IOException              if an I/O error occurs while reading sources
	 *                                  or writing the output file
	 */
	public static void generate(final Set<Path> sourceRoots, final Path outDir) throws IOException {
		checkSourceRoots(sourceRoots);
		Objects.requireNonNull(outDir, "outDir");

		Path dir = outDir.toAbsolutePath().normalize();
		if (Files.exists(dir) && !Files.isDirectory(dir)) {
			throw new IllegalArgumentException("outDir must be a directory: " + dir);
		}
		Files.createDirectories(dir);
		Path outputFile = dir.resolve(CLASS_DIAGRAM_PUML);
		logger.log(Level.INFO, () -> "Generating " + outputFile + "...");

		List<Path> sortedSourceRoots = sortRootsByPath(sourceRoots);
		List<CompilationUnit> units = SmartSourceRootManager.autoscan(sortedSourceRoots);

		DeclaredIndex index = new DeclaredIndex();
		index.fill(units);

		new ClassDiagramGeneration(outputFile, index).run();
		logger.log(Level.INFO, () -> "Writing " + outputFile + " complete.");
	}

	private static List<Path> sortRootsByPath(Set<Path> sourceRoots) {
		// @formatter:off
		return sourceRoots.stream()
				.map(p -> p.toAbsolutePath().normalize())
				.distinct()
				.sorted(Comparator.comparing(Path::toString))
				.toList();
		// @formatter:on
	}

	private static void checkSourceRoots(Set<Path> sourceRoots) {
		Objects.requireNonNull(sourceRoots, "sourceRoots");
		if (sourceRoots.isEmpty()) {
			throw new IllegalArgumentException("sourceRoots must not be empty.");
		}
	}

}