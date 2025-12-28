/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContainsClass;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramMethodModifiersSampleTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramContainingMethodModifiers() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/mmodifiers",
                tempDir,
                "mmodifiers");

        assertPumlContainsClass(puml, "samples.mmodifiers.AbstractMethods");
        assertPumlContains(puml, "abstractMethod");
        assertPumlContains(puml, "{abstract}");

        assertPumlContainsClass(puml, "samples.mmodifiers.ConcreteMethods");
        assertPumlContains(puml, "finalMethod");
        assertPumlContains(puml, "{final}");

        assertPumlContains(puml, "staticMethod");
        assertPumlContains(puml, "{static}");

        assertPumlContains(puml, "normalMethod");
    }
}
