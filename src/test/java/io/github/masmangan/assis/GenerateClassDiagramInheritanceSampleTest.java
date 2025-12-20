/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;

class GenerateClassDiagramInheritanceSampleTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramContainingInheritance() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/inheritance",
                tempDir,
                "inheritance");

        // types
        assertPumlContains(puml, "interface A");
        assertPumlContains(puml, "interface B");
        assertPumlContains(puml, "class Base");
        assertPumlContains(puml, "class Child");

        // relationships
        assertPumlContains(puml, "A <|-- B");
        assertPumlContains(puml, "Base <|-- Child");
        assertPumlContains(puml, "B <|.. Child");
    }
}
