/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;

public class SmartSourceRootManager {
	/**
	 * Logger used by the generator to report progress and parse/write issues.
	 */
	static final Logger logger = Logger.getLogger(SmartSourceRootManager.class.getName());

	/**
	 * Scans directories on sourceRoots.
	 *
	 * <p>
	 * Best effort scanning will discard invalid and proceed with a simple warning.
	 * This behavior can scan a large mass of files even in the presence of partial
	 * failure.
	 *
	 * @param sourceRoots one or more Java source roots; must not be {@code null}
	 * @param index       index to be filled with compilation units scanned from
	 *                    sources roots
	 * @param units       list of compilation units scanned
	 * @throws IOException if an I/O error occurs while reading sources
	 */
	public static List<CompilationUnit> autoscan(Set<Path> sourceRoots) throws IOException {
		Objects.requireNonNull(sourceRoots, "sourceRoots");

		List<CompilationUnit> units = new ArrayList<>();

		logger.log(Level.INFO, () -> "Scanning started");

		List<Path> roots = sourceRoots.stream().sorted(Comparator.comparing(Path::toString)).toList();

		for (Path src : roots) {

			logger.log(Level.INFO, () -> "Scanning " + src);

			if (!Files.exists(src)) {
				logger.log(Level.WARNING, () -> "@assis:bogus-src: Source folder does not exist: " + src);
				continue;
			}

			SmartSourceRoot root = new SmartSourceRoot(src);

			List<ParseResult<CompilationUnit>> results = root.tryToParse("");

			for (ParseResult<CompilationUnit> r : results) {
				r.getResult().ifPresent(units::add);
			}

		}

		units.sort(Comparator.comparing(unit -> unit.getStorage().map(s -> s.getPath().toString()).orElse("")));

		return units;
	}

}
