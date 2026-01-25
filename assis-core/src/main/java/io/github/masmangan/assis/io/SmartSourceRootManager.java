/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

import io.github.masmangan.assis.util.DeterministicPathList;

/**
 * Scans a set of Java source root directories.
 *
 * <p>
 * Uses one {@link SmartSourceRoot} per directory.
 *
 * @see SmartSourceRoot
 */
public class SmartSourceRootManager {

	private static final Logger logger = Logger.getLogger(SmartSourceRootManager.class.getName());

	
	private SmartSourceRootManager() {
	}
	
	/**
	 * Scans the given Java source root directories and parses all {@code .java}
	 * files found.
	 *
	 * <p>
	 * This is a best-effort scan: missing directories are skipped with a warning.
	 *
	 * Roots that yield no compilation units also log a warning.
	 *
	 * <p>
	 * Scan order is deterministic. Callers are expected to supply
	 * {@code sourceRoots} in normalized, lexicographically sorted order
	 *
	 * Returned compilation units are sorted by declared package and type names
	 * (file system paths are intentionally ignored).
	 *
	 * @param sortedSourceRoots one or more Java source root directories; must not
	 *                          be {@code null} or empty
	 * @return compilation units successfully parsed from all roots
	 * @throws IOException if an I/O error occurs while scanning or parsing
	 */
	public static List<CompilationUnit> autoscan(DeterministicPathList sortedSourceRoots) throws IOException {
		Objects.requireNonNull(sortedSourceRoots);
		
		List<CompilationUnit> units = new ArrayList<>();

		logger.log(Level.INFO, () -> "Scanning started");

		for (Path src : sortedSourceRoots) {

			logger.log(Level.INFO, () -> "Scanning " + src);

			if (!Files.exists(src)) {
				logger.log(Level.WARNING, () -> "Source folder does not exist: " + src);
				continue;
			}

			SmartSourceRoot root = new SmartSourceRoot(src);

			List<ParseResult<CompilationUnit>> results = root.tryToParse("");

			int addedFromThisRoot = 0;
			for (ParseResult<CompilationUnit> r : results) {
				Optional<CompilationUnit> opt = r.getResult();
				if (opt.isPresent()) {
				    units.add(opt.get());
				    addedFromThisRoot++;
				}
			}

			if (addedFromThisRoot == 0) {
				logger.log(Level.WARNING, () -> "Source folder yields no compilation units: " + src);
			}
		}

		warnOnPrimaryTypeCollisions(units);

		sortUnitsByPackage(units);
		return units;
	}

	private static void warnOnPrimaryTypeCollisions(List<CompilationUnit> units) {
		Map<String, List<String>> occurrences = new HashMap<>();

		for (CompilationUnit u : units) {
			String pkg = u.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
			String primary = u.getPrimaryTypeName().orElse("<no-primary-type>");
			String key = pkg.isEmpty() ? primary : (pkg + "." + primary);

			String where = u.getStorage().map(s -> s.getPath().toString()).orElse("<no-storage>");

			occurrences.computeIfAbsent(key, k -> new ArrayList<>()).add(where);
		}

		for (Map.Entry<String, List<String>> e : occurrences.entrySet()) {
			List<String> whereList = e.getValue();
			if (whereList.size() > 1) {
				String locations = whereList.stream().distinct().sorted().collect(Collectors.joining(", "));

				logger.log(Level.WARNING, () -> "Duplicate primary type detected for \"" + e.getKey()
						+ "\" across multiple compilation units; results may be inconsistent. Files: " + locations);
			}
		}
	}

	private static void sortUnitsByPackage(List<CompilationUnit> units) {
		Comparator<CompilationUnit> bySemanticIdentity = Comparator
				.comparing((CompilationUnit u) -> u.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse(""))
				.thenComparing(u -> u.getPrimaryTypeName().orElse("")).thenComparing(
						u -> u.getTypes().stream().map(t -> t.getNameAsString()).sorted().reduce("", String::concat));

		units.sort(bySemanticIdentity);
	}

}
