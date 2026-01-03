/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.cli;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AssisAppTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramFromHelloUsingSourcepathAndD() throws Exception {
        Path srcMainJava = tempDir.resolve("src/main/java");
        Files.createDirectories(srcMainJava);

        Files.writeString(
            srcMainJava.resolve("Hello.java"),
            """
            public class Hello {
            }
            """,
            UTF_8
        );

        Path out = tempDir.resolve("docs/diagrams/src/class-diagram.puml");
        Files.createDirectories(out.getParent());

        var outBuf = new ByteArrayOutputStream();
        var errBuf = new ByteArrayOutputStream();

        int code = AssisApp.run(
            new String[] { "-sourcepath", srcMainJava.toString(), "-d", out.toString() },
            new PrintStream(outBuf, true, UTF_8),
            new PrintStream(errBuf, true, UTF_8)
        );

        assertEquals(0, code, "stderr:\n" + errBuf.toString(UTF_8));
        assertTrue(Files.exists(out), "Expected output file to exist: " + out);

        String puml = Files.readString(out, UTF_8);
        assertTrue(
            puml.contains("Hello") || puml.contains("\"Hello\""),
            "Expected diagram to mention Hello. Content:\n" + puml
        );
    }

    @Test
    void createsBackupWhenOutputAlreadyExists() throws Exception {
        Path srcMainJava = tempDir.resolve("src/main/java");
        Files.createDirectories(srcMainJava);

        Files.writeString(
            srcMainJava.resolve("Hello.java"),
            "public class Hello {}",
            UTF_8
        );

        Path out = tempDir.resolve("docs/diagrams/src/class-diagram.puml");
        Files.createDirectories(out.getParent());

        // Simulate a manually edited diagram.
        String manual = "@startuml\n' manual edit\n@enduml\n";
        Files.writeString(out, manual, UTF_8);

        var outBuf = new ByteArrayOutputStream();
        var errBuf = new ByteArrayOutputStream();

        int code = AssisApp.run(
            new String[] { "--source-path", srcMainJava.toString(), "-d", out.toString() },
            new PrintStream(outBuf, true, UTF_8),
            new PrintStream(errBuf, true, UTF_8)
        );

        assertEquals(0, code, "stderr:\n" + errBuf.toString(UTF_8));

        // Backup must exist next to the out file and MUST NOT end with .puml
        try (var list = Files.list(out.getParent())) {
            var backups = list
                .filter(p -> p.getFileName().toString().startsWith("class-diagram.puml.bak-"))
                .toList();

            assertTrue(!backups.isEmpty(), "Expected at least one backup file next to: " + out);

            Path bak = backups.get(0);
            assertTrue(!bak.getFileName().toString().endsWith(".puml"), "Backup must not end with .puml: " + bak);

            String bakContent = Files.readString(bak, UTF_8);
            assertEquals(manual, bakContent, "Backup must preserve the previous content");
        }

        String puml = Files.readString(out, UTF_8);
        assertTrue(
            puml.contains("Hello") || puml.contains("\"Hello\""),
            "Expected regenerated diagram to mention Hello. Content:\n" + puml
        );
    }

    @Test
    void invalidArgumentReturnsNonZero() throws Exception {
        var outBuf = new ByteArrayOutputStream();
        var errBuf = new ByteArrayOutputStream();

        int code = AssisApp.run(
            new String[] { "-nope" },
            new PrintStream(outBuf, true, UTF_8),
            new PrintStream(errBuf, true, UTF_8)
        );

        assertTrue(code != 0, "Expected non-zero exit code for invalid arg");
        String err = errBuf.toString(UTF_8);
        assertTrue(err.contains("Unknown option"), "Expected error message. Got:\n" + err);
    }
}