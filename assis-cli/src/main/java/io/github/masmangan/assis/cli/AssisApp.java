/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.cli;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.masmangan.assis.GenerateClassDiagram;

/**
 * The {@code AssisApp} class is the PlantUML diagram generator entry point.
 */
public final class AssisApp {

	private static final Logger LOG = Logger.getLogger(AssisApp.class.getName());

	/**
	 * Default output directory (PlantUML/Jebbs convention).
	 */
	static final String DEFAULT_OUT_DIR = "docs/diagrams/src";

	/**
	 *
	 */
	private AssisApp() {
	}

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		int code = run(args);
		System.exit(code);
	}

	/**
	 * Test-friendly runner.
	 *
	 * @return exit code (0 success, non-zero failure)
	 */
	static int run(String[] args) {
		final CliArgs cli;
		try {
			cli = CliArgs.parse(args);
		} catch (IllegalArgumentException e) {
			LOG.log(Level.SEVERE, () -> e.getMessage());
			return 1;
		}

		if (cli.mode == CliArgs.Mode.HELP) {
			LOG.log(Level.CONFIG, () -> CliArgs.usage);
			return 0;
		}

		if (cli.mode == CliArgs.Mode.VERSION) {
			LOG.log(Level.CONFIG, () -> "ASSIS " + GenerateClassDiagram.versionOrDev());
			return 0;
		}

		final Set<Path> sourceRoots;
		try {
			sourceRoots = SourceLocator.resolve(cli.sourceRoots);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, () -> "ERROR: " + e.getMessage());
			return 2;
		}

		final Path outDir = (cli.outDir != null) ? cli.outDir.toAbsolutePath().normalize()
				: Path.of(DEFAULT_OUT_DIR).toAbsolutePath().normalize();

		try {
			if (Files.exists(outDir) && !Files.isDirectory(outDir)) {
				throw new IllegalArgumentException("-d must be a directory: " + outDir);
			}
			Files.createDirectories(outDir);

			LOG.info(() -> "Generating diagrams from source roots:");
			for (Path r : sourceRoots) {
				LOG.info(() -> "  - " + r);
			}
			LOG.info(() -> "Writing outputs to: " + outDir);

			GenerateClassDiagram.generate(sourceRoots, outDir);

			return 0;
		} catch (Exception e) {
			LOG.log(Level.SEVERE, () -> "ERROR: " + e.getMessage());
			return 3;
		}
	}

}