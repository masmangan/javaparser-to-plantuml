/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import java.net.URISyntaxException;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.Comparator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiagramGenerationHelloRecordSampleTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramContainingHelloClass() throws Exception {
        Path sampleRoot = copySampleProjectToTemp("samples/hellorecord", tempDir.resolve("hellorecordsample"));

        Path inputDir = sampleRoot.resolve(".");
        Path outputFile = tempDir.resolve("diagram.puml");

        GenerateClassDiagram.generate(
                inputDir,
                outputFile);
        String puml = Files.readString(outputFile, StandardCharsets.UTF_8);

            assertTrue(puml.contains("class HelloRecord"),
                "Expected generated PlantUML to contain 'class HelloRecord'. Content:\n" + puml);

        assertTrue(puml.contains("class Greeting"),
                "Expected generated PlantUML to contain 'class Greeting'. Content:\n" + puml);
    }

    private static Path copySampleProjectToTemp(String resourcePath, Path targetDir)
            throws IOException, URISyntaxException {

        URL url = DiagramGenerationHelloSampleTest.class.getClassLoader().getResource(resourcePath);
        if (url == null) {
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }

        Path sourceDir = Paths.get(url.toURI());

        if (Files.exists(targetDir)) {
            deleteRecursively(targetDir);
        }
        Files.createDirectories(targetDir);

        try (var walk = Files.walk(sourceDir)) {
            walk.forEach(src -> {
                try {
                    Path rel = sourceDir.relativize(src);
                    Path dst = targetDir.resolve(rel.toString());
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dst);
                    } else {
                        Files.createDirectories(dst.getParent());
                        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return targetDir;
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path))
            return;

        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}