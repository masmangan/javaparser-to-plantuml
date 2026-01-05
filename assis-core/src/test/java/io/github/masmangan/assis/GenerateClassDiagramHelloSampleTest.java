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

class GenerateClassDiagramHelloSampleTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramContainingHelloClass() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/hello",
                tempDir,
                "hello");
        assertPumlContainsClass(puml, "samples.hello.Hello");
        assertPumlContains(puml, "main");
    }

}