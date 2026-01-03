/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.cli;

import java.nio.file.Path;

final class CliArgs {

    enum Mode { RUN, HELP, VERSION }

    final Mode mode;
    final Path sourcePath; // null => auto-discovery
    final Path outPath;    // null => default docs/diagrams/src/class-diagram.puml

    private CliArgs(Mode mode, Path sourcePath, Path outPath) {
        this.mode = mode;
        this.sourcePath = sourcePath;
        this.outPath = outPath;
    }

    static CliArgs parse(String[] args) {
        if (args == null) args = new String[0];

        Path src = null;
        Path out = null;

        for (int i = 0; i < args.length; i++) {
            String a = args[i];

            if ("--help".equals(a) || "-help".equals(a) || "-?".equals(a)) {
                return new CliArgs(Mode.HELP, null, null);
            }

            if ("--version".equals(a) || "-version".equals(a)) {
                return new CliArgs(Mode.VERSION, null, null);
            }

            if ("--source-path".equals(a) || "-sourcepath".equals(a)) {
                requireValue(args, i, a);
                src = Path.of(args[++i]);
                continue;
            }

            if ("-d".equals(a)) {
                requireValue(args, i, a);
                out = Path.of(args[++i]);
                continue;
            }

            throw new IllegalArgumentException("Unknown option: " + a + "\n\n" + usage());
        }

        return new CliArgs(Mode.RUN, src, out);
    }

    private static void requireValue(String[] args, int i, String opt) {
        if (i + 1 >= args.length) {
            throw new IllegalArgumentException("Missing value for " + opt + "\n\n" + usage());
        }
    }

    static String usage() {
        return String.join("\n",
            "Usage: assis <options>",
            "",
            "Options (javac-like):",
            "  --help, -help, -?                 Print this help message",
            "  --version, -version               Version information",
            "  --source-path <path>, -sourcepath <path>",
            "                                   Specify where to find input source files",
            "  -d <file>                          Output .puml destination file",
            "",
            "Defaults:",
            "  source: auto-discovery tries src/main/java, then src, then .",
            "  out:    docs/diagrams/src/class-diagram.puml"
        );
    }

    private CliArgs() { throw new AssertionError(); }
}