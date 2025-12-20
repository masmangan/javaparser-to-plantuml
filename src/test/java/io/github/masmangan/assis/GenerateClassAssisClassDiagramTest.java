/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassAssisClassDiagramTest {

        @TempDir
        Path tempDir;

        @Test
        void generatedDiagramContainsAssisClasses() throws Exception {
                Path output = tempDir.resolve("diagram.puml");

                GenerateClassDiagram.generate(
                                Path.of("src/main/java"),
                                output);

                String content = Files.readString(output);

                assertPumlContains(content, "class AssisApp");

                assertPumlContains(content, "class GenerateClassDiagram");
        }
}