/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class GenerateClassDiagramDeterminismSampleTest {

    @Test
    void twoConsecutiveGenerationsHaveSameOutput() throws Exception {
        Path tempDir = Path.of("target", "tmp-determinism");
        Files.createDirectories(tempDir);

        // reuse an existing sample
        Path sampleRoot = TestWorkbench.copySampleProjectToTemp(
                "samples/enumvalues",
                tempDir.resolve("enumvalues")
        );

        Path out1 = tempDir.resolve("diagram-1.puml");
        Path out2 = tempDir.resolve("diagram-2.puml");

        GenerateClassDiagram.generate(sampleRoot, out1);
        Thread.sleep(1100); // optional now, but keeps the intent clear
        GenerateClassDiagram.generate(sampleRoot, out2);

        String a = Files.readString(out1, StandardCharsets.UTF_8);
        String b = Files.readString(out2, StandardCharsets.UTF_8);

        assertEquals(a, b);
    }
}