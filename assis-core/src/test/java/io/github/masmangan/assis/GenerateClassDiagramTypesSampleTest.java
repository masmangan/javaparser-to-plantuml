/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramTypesSampleTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramContainingTypes() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/types",
                tempDir,
                "types");

        assertPumlContains(puml, "class \"samples.types.Class\"");
        assertPumlContains(puml, "interface \"samples.types.Interface\"");
        assertPumlContains(puml, "enum \"samples.types.Enumeration\"");
        assertPumlContains(puml, "annotation \"samples.types.Annotation\"");
        assertPumlContains(puml, "record \"samples.types.Record\"");
    }

}