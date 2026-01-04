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

        Path out = tempDir.resolve("docs/diagrams/src/");
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

        String puml = Files.readString(out.resolve("class-diagram.puml"), UTF_8);
        assertTrue(
            puml.contains("Hello") || puml.contains("\"Hello\""),
            "Expected diagram to mention Hello. Content:\n" + puml
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
    
    @Test
    void checkHelpKnobExists() throws Exception {
        var outBuf = new ByteArrayOutputStream();
        var errBuf = new ByteArrayOutputStream();

        int code = AssisApp.run(
            new String[] { "--help" },
            new PrintStream(outBuf, true, UTF_8),
            new PrintStream(errBuf, true, UTF_8)
        );

        assertTrue(code == 0, "Expected non-zero exit code for invalid arg");
        String out = outBuf.toString(UTF_8);
        assertTrue(out.contains("Usage"), "Expected error message. Got:\n" + out);
    }
    
    @Test
    void checkHelpVersionExists() throws Exception {
        var outBuf = new ByteArrayOutputStream();
        var errBuf = new ByteArrayOutputStream();

        int code = AssisApp.run(
            new String[] { "--version" },
            new PrintStream(outBuf, true, UTF_8),
            new PrintStream(errBuf, true, UTF_8)
        );

        assertTrue(code == 0, "Expected non-zero exit code for invalid arg");
        String out = outBuf.toString(UTF_8);
        assertTrue(out.contains("ASSIS"), "Expected error message. Got:\n" + out);
    }
    
}