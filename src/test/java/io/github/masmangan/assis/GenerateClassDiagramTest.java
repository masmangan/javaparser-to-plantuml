/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramTest {

    @TempDir
    Path tempDir;

    @Test
    void generatedDiagramContainsExpectedClasses() throws Exception {
        Path output = tempDir.resolve("diagram.puml");

        GenerateClassDiagram.generate(
                Path.of("src/main/java"),
                output);

        String content = Files.readString(output);

        assertTrue(content.contains("class AssisApp"),
                "Diagram should contain AssisApp");

        assertTrue(content.contains("class GenerateClassDiagram"),
                "Diagram should contain GenerateClassDiagram");
    }
}