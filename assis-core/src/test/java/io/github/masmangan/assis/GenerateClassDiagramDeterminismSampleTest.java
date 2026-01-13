/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;

class GenerateClassDiagramDeterminismSampleTest {

	@Test
	void twoConsecutiveGenerationsHaveSameOutput() throws Exception {
		Path tempDir = Path.of("target", "tmp-determinism");
		Files.createDirectories(tempDir);

		// reuse an existing sample
		Path sampleRoot = TestWorkbench.copySampleProjectToTemp("samples/enums/values", tempDir.resolve("enumvalues"));

		Path outDir1 = tempDir.resolve("out-" + "diagram1");
		Files.createDirectories(outDir1);

		Path outDir2 = tempDir.resolve("out-" + "diagram2");
		Files.createDirectories(outDir2);

		GenerateClassDiagram.generate(Set.of(sampleRoot), outDir1);
		GenerateClassDiagram.generate(Set.of(sampleRoot), outDir2);

		Path outputFile1 = outDir1.resolve("class-diagram.puml");
		Path outputFile2 = outDir2.resolve("class-diagram.puml");

		String a = Files.readString(outputFile1, StandardCharsets.UTF_8);
		String b = Files.readString(outputFile2, StandardCharsets.UTF_8);

		assertEquals(a, b);
	}
}