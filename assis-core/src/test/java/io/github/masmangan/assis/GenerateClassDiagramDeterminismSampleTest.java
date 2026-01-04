/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GenerateClassDiagramDeterminismSampleTest {

    @Test
    void twoConsecutiveGenerationsHaveSameOutput() throws Exception {
        Path tempDir = Path.of("target", "tmp-determinism");
        Files.createDirectories(tempDir);

        // reuse an existing sample
        Path sampleRoot = TestWorkbench.copySampleProjectToTemp(
                "samples/enums/values",
                tempDir.resolve("enumvalues")
        );

        Path out1 = tempDir.resolve("diagram-1.puml");
        Path out2 = tempDir.resolve("diagram-2.puml");

        GenerateClassDiagram.generate(Set.of(sampleRoot), out1);
        GenerateClassDiagram.generate(Set.of(sampleRoot), out2);

        String a = Files.readString(out1, StandardCharsets.UTF_8);
        String b = Files.readString(out2, StandardCharsets.UTF_8);

        assertEquals(a, b);
    }
}