/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContainsClass;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramClassModifiersSampleTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramContainingClassModifiers() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/cmodifiers",
                tempDir,
                "cmodifiers");

        assertPumlContains(puml,"abstract class \"samples.cmodifiers.AbstractClass\"");
        assertPumlContains(puml,
            "class \"samples.cmodifiers.FinalClass\" <<final>>");
        assertPumlContainsClass(puml,
            "samples.cmodifiers.PlainClass");

    }
}
