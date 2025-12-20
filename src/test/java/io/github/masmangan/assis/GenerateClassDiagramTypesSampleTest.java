/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;

class GenerateClassDiagramTypesSampleTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramContainingTypes() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/types",
                tempDir,
                "types");

        assertPumlContains(puml, "class Class");
        assertPumlContains(puml, "interface Interface");
        assertPumlContains(puml, "enum Enumeration");
        assertPumlContains(puml, "annotation Annotation");
        assertPumlContains(puml, "record Record");
    }

}