/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

final class SourceLocator {

    private static final Logger LOG = Logger.getLogger(SourceLocator.class.getName());

    static final Path MAVEN = Path.of("src/main/java");
    static final Path SRC   = Path.of("src");
    static final Path DOT   = Path.of(".");

    private static final List<Path> CANDIDATES = List.of(MAVEN, SRC, DOT);

    /**
     * Resolves source roots (directories only).
     *
     * If requested != null:
     *   - validates each directory exists and contains at least one .java somewhere under it.
     *   - returns normalized absolute paths (stable for downstream).
     *
     * Else:
     *   - auto-discovery chooses first of: src/main/java, src, .
     *   - returns singleton set containing the chosen directory (normalized absolute).
     */
    static Set<Path> resolve(Set<Path> requested) throws IOException {
    	
        if (requested != null) {
            return extractRequested(requested);
        }

        return extractFirstDefault();

     }

	private static Set<Path> extractFirstDefault() throws IOException {
		boolean mavenDirExists = Files.isDirectory(MAVEN);
        boolean mavenHasJava = mavenDirExists && containsJava(MAVEN);
        boolean dotHasJava = Files.isDirectory(DOT) && containsJava(DOT);

        for (Path candidate : CANDIDATES) {
            Path abs = candidate.toAbsolutePath().normalize();
            LOG.info(() -> "Trying source directory: " + abs);

            if (!Files.isDirectory(candidate)) continue;
            if (!containsJava(candidate)) continue;

            LOG.info(() -> "Using source directory: " + abs);

            if (candidate.equals(DOT)) {
                LOG.warning(() ->
                    "Falling back to '.' as source root. " +
                    "If you expected Maven layout, run ASSIS from the project root, " +
                    "or use -sourcepath/--source-path to point to the desired folder."
                );

                if (mavenDirExists && !mavenHasJava && dotHasJava) {
                    LOG.warning(() ->
                        "Note: 'src/main/java' exists but contains no .java files; '.' does. " +
                        "This often happens with BlueJ/intro projects or when running from a subfolder."
                    );
                }
            }

            return Set.of(abs);
        }
        LOG.severe("No Java source directory found. Tried: src/main/java, src, .");
        throw new IllegalStateException("No Java source directory found (tried: src/main/java, src, .)");

	}

	private static Set<Path> extractRequested(Set<Path> requested) throws IOException {
		LinkedHashSet<Path> out = new LinkedHashSet<>();
		for (Path dir : requested) {
		    if (dir == null) continue;

		    Path abs = dir.toAbsolutePath().normalize();
		    LOG.info(() -> "Using explicit source path (javac-like): " + abs);

		    validateHasJavaOrThrow(abs, /*isExplicit*/ true);
		    out.add(abs);
		}

		if (out.isEmpty()) {
		    throw new IllegalArgumentException("No valid source directories provided.");
		}

		return out;
	}

    private static void validateHasJavaOrThrow(Path dir, boolean isExplicit) throws IOException {
        if (!Files.exists(dir)) {
            throw new IllegalArgumentException("Source path does not exist: " + dir);
        }
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("Source path is not a directory: " + dir);
        }
        if (!containsJava(dir)) {
            String hint = isExplicit
                ? "No .java files found under -sourcepath/--source-path: " + dir
                : "No .java files found under: " + dir;
            throw new IllegalArgumentException(hint);
        }
    }

    private static boolean containsJava(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.anyMatch(p -> p.toString().endsWith(".java"));
        }
    }

    private SourceLocator() {}
}