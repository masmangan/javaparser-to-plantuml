/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;

class GenerateClassDiagramHelloSampleTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramContainingHelloClass() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/hello",
                tempDir,
                "hello");
        assertPumlContains(puml, "class Hello");
        assertPumlContains(puml, "main");
    }

}