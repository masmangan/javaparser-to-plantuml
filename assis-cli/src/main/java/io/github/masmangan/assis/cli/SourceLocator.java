/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

final class SourceLocator {

    private static final Logger LOG = Logger.getLogger(SourceLocator.class.getName());

    static final Path MAVEN = Path.of("src/main/java");
    static final Path SRC   = Path.of("src");
    static final Path DOT   = Path.of(".");

    private static final List<Path> CANDIDATES = List.of(MAVEN, SRC, DOT);

    /**
     * Resolves the source path.
     * - If requested != null: validates it (must exist, be dir, contain .java)
     * - Else: auto-discovery tries src/main/java, then src, then .
     */
    static Path resolve(Path requested) throws IOException {
        if (requested != null) {
            LOG.info(() -> "Using explicit source path (javac-like): " + requested.toAbsolutePath());
            validateHasJavaOrThrow(requested, /*isExplicit*/ true);
            return requested;
        }

        boolean mavenDirExists = Files.isDirectory(MAVEN);
        boolean mavenHasJava = mavenDirExists && containsJava(MAVEN);
        boolean dotHasJava = Files.isDirectory(DOT) && containsJava(DOT);

        for (Path candidate : CANDIDATES) {
            LOG.info(() -> "Trying source directory: " + candidate.toAbsolutePath());

            if (!Files.isDirectory(candidate)) continue;
            if (!containsJava(candidate)) continue;

            LOG.info(() -> "Using source directory: " + candidate.toAbsolutePath());

            // Pedagogical logs
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

            return candidate;
        }

        LOG.severe("No Java source directory found. Tried: src/main/java, src, .");
        throw new IllegalStateException("No Java source directory found (tried: src/main/java, src, .)");
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