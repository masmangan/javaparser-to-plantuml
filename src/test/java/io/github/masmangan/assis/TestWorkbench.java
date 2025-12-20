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

public class TestWorkbench {

    static Path copySampleProjectToTemp(String resourcePath, Path targetDir)
            throws IOException, URISyntaxException {

        URL url = GenerateClassDiagramHelloSampleTest.class.getClassLoader().getResource(resourcePath);
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

static String generatePumlFromSample(String resourcePath, Path tempDir, String tempFolderName) throws Exception {
    Path sampleRoot = copySampleProjectToTemp(resourcePath, tempDir.resolve(tempFolderName));
    Path outputFile = tempDir.resolve("diagram.puml");
    GenerateClassDiagram.generate(sampleRoot, outputFile);
    return Files.readString(outputFile, StandardCharsets.UTF_8);
}

    static void deleteRecursively(Path path) throws IOException {
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

    static void assertPumlContains(String puml, String fragment) {
        assertTrue(puml.contains(fragment),
                "Expected generated PlantUML to contain " + fragment + ". Content:\n" + puml);
    }
}
