/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 */
final class CliArgs {

	enum Mode {
		RUN, HELP, VERSION
	}

	final Mode mode;

	/**
	 * Null => auto-discovery. Otherwise: directories-only, potentially multiple
	 * entries.
	 */
	final Set<Path> sourceRoots;

	/**
	 * Null => default docs/diagrams/src/
	 */
	final Path outDir;

	private CliArgs(Mode mode, Set<Path> sourceRoots, Path outDir) {
		this.mode = mode;
		this.sourceRoots = sourceRoots;
		this.outDir = outDir;
	}

	/**
	 *
	 * @param args
	 * @return
	 */
	static CliArgs parse(String[] args) {
		if (args == null) {
			args = new String[0];
		}

		Set<Path> srcRoots = new LinkedHashSet<>();
		;
		Path outDir = null;

		boolean skip = false;
		for (int i = 0; i < args.length; i++) {
			if (skip) {
				skip = false;
				continue;
			}
			String a = args[i];

			if (isHelp(a)) {
				return new CliArgs(Mode.HELP, null, null);
			}

			if (isVersion(a)) {
				return new CliArgs(Mode.VERSION, null, null);
			}

			if (isSourcePath(a)) {
				skip = true;
				srcRoots.addAll(parseSourcePath(args, i, a));
			} else if (isOutputDirectory(a)) {
				skip = true;
				outDir = parseOutputDirectory(args, outDir, i, a);
			} else {
				throw new IllegalArgumentException("Unknown option: " + a + "\n\n" + usage);
			}
		}

		return new CliArgs(Mode.RUN, srcRoots, outDir);
	}

	private static Path parseOutputDirectory(String[] args, Path outDir, int i, String a) {
		if (outDir != null) {
			throw new IllegalArgumentException("Duplicate option: -d\n\n" + usage);
		}

		requireValue(args, i, a);
		outDir = Path.of(args[i + 1]);
		return outDir;
	}

	private static Set<Path> parseSourcePath(String[] args, int i, String a) {
		requireValue(args, i, a);
		String raw = args[i + 1];

		// another parse option
		Set<Path> parsed = parsePathList(raw);
		return parsed;
	}

	private static boolean isOutputDirectory(String a) {
		return "-d".equals(a);
	}

	private static boolean isSourcePath(String a) {
		return "--source-path".equals(a) || "-sourcepath".equals(a);
	}

	private static boolean isVersion(String a) {
		return "--version".equals(a) || "-version".equals(a);
	}

	private static boolean isHelp(String a) {
		return "--help".equals(a) || "-help".equals(a) || "-?".equals(a);
	}

	private static Set<Path> parsePathList(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException("Empty value for --source-path/-sourcepath\n\n" + usage);
		}

		String sep = File.pathSeparator;
		String[] parts = raw.split(java.util.regex.Pattern.quote(sep));

		LinkedHashSet<Path> out = new LinkedHashSet<>();
		for (String p : parts) {
			if (p == null || p.isBlank()) {
				continue;
			}
			out.add(Path.of(p.trim()));
		}

		if (out.isEmpty()) {
			throw new IllegalArgumentException("No source directories provided in --source-path\n\n" + usage);
		}

		return out;
	}

	private static void requireValue(String[] args, int i, String opt) {
		if (i + 1 >= args.length) {
			throw new IllegalArgumentException("Missing value for " + opt + "\n\n" + usage);
		}
	}

	static String usage = """
			Usage: java -jar assis.jar <options>

			where possible options include:
			  --help, -help, -?
			        Print this help message
			  --version, -version
			        Version information
			  --source-path <path>, -sourcepath <path>
			        Specify where to find input source files
			  -d <directory>
			        Specify where to place generated .puml files

			Defaults:

			  Source path auto-discovery defaults to the first available:
			      --source-path src/main/java/
			      --source-path src/
			      --source-path .

			  Generation defaults to:
			      -d docs/diagrams/src/
			""";

	private CliArgs() {
		throw new AssertionError();
	}
}