/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

public class TestWorkbench {
    static final Path GALLERY_ROOT = Paths.get("target", "assis-gallery");

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
        saveToGallery(outputFile, resourcePath, tempFolderName);
        return Files.readString(outputFile, StandardCharsets.UTF_8);
    }

    static void saveToGallery(Path generatedPuml, String testName, String fileBaseName) throws IOException {
        String safeTest = safeFileName(testName);
        String safeBase = safeFileName(fileBaseName);

        Path dir = GALLERY_ROOT.resolve(safeTest);
        Files.createDirectories(dir);

        String ts = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path dst = dir.resolve(safeBase + "-" + ts + ".puml");

        Files.copy(generatedPuml, dst, StandardCopyOption.REPLACE_EXISTING);
    }

    static String safeFileName(String s) {
        if (s == null || s.isBlank())
            return "_";
        return s.replaceAll("[^a-zA-Z0-9._-]+", "_");
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

    static void assertPumlNotContains(String puml, String fragment) {
        assertFalse(puml.contains(fragment),
                "Expected generated PlantUML to not contain " + fragment + ". Content:\n" + puml);
    }

    static void assertPumlContainsName(String puml, String name) {
        assertTrue(
                puml.contains(name) || puml.contains("\"" + name + "\""),
                "Expected diagram to contain name: " + name);
    }

    static void assertPumlContainsPackage(String puml, String name) {
        assertTrue(
                puml.contains(name) || puml.contains("package \"" + name + "\""),
                "Expected diagram to contain package: " + name);
    }

    static void assertPumlContainsClass(String puml, String name) {
        assertTrue(
                puml.contains(name) || puml.contains("class \"" + name + "\""),
                "Expected diagram to contain class: " + name);
    }

    static void assertAnyLineContainsAll(String puml, String... tokens) {
        boolean ok = puml.lines().anyMatch(line -> {
            for (String t : tokens) {
                if (!line.contains(t))
                    return false;
            }
            return true;
        });
        assertTrue(ok, "Expected a line containing: " + String.join(" AND ", tokens) + ". Content:\n" + puml);
    }

    static void assertAppearsInOrder(String text, String... fragments) {
        int pos = -1;
        for (String f : fragments) {
            int next = text.indexOf(f);
            if (next < 0)
                throw new AssertionError("Missing fragment: " + f + "\n" + text);
            if (next <= pos)
                throw new AssertionError("Out of order: " + f + "\n" + text);
            pos = next;
        }
    }
}
