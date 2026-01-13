/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertAnyLineContainsAll;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContainsClass;
import static io.github.masmangan.assis.TestWorkbench.generatePumlFromSample;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * This regression test is related to Issue #12.
 *
 */
class ProcessBuilderSampleTest {

	@TempDir
	Path tempDir;

	@Test
	void generatesEnumConstantsInDeclarationOrder() throws Exception {
		String puml = generatePumlFromSample("samples/bugs/bug12", tempDir, "bug12");

		assertPumlContainsClass(puml, "java.lang.ProcessBuilder");
		assertPumlContainsClass(puml, "java.lang.ProcessBuilder$Redirect");
		assertAnyLineContainsAll(puml, "java.lang.ProcessBuilder$Redirect$Type");
	}

}