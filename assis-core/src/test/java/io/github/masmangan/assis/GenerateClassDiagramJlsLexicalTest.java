/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContainsClass;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContainsPackage;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// This test intentionally requires FQN resolution.
// Removing FQN support must break this test.
class GenerateClassDiagramJlsLexicalTest {
	@TempDir
	Path tempDir;

	@Test
	void generatesDiagramContainingLexical() throws Exception {
		String puml = TestWorkbench.generatePumlFromSample("jls/lexical", tempDir, "lexical");

		assertPumlContainsPackage(puml, "testPackage");
		assertPumlContainsPackage(puml, "other");

		assertPumlContainsClass(puml, "testPackage.Test");
		assertPumlContainsClass(puml, "testPackage.Other");
		assertPumlContainsClass(puml, "other.Other");

	}
}
