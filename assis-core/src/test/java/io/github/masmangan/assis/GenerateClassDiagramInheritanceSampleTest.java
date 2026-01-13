/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertAnyLineContainsAll;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContainsClass;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramInheritanceSampleTest {
	@TempDir
	Path tempDir;

	@Test
	void generatesDiagramContainingInheritance() throws Exception {
		String puml = TestWorkbench.generatePumlFromSample("samples/inheritance", tempDir, "inheritance");

		// types
		assertPumlContains(puml, "interface \"samples.inheritance.A\"");
		assertPumlContains(puml, "interface \"samples.inheritance.B\"");
		assertPumlContainsClass(puml, "samples.inheritance.Base");
		assertPumlContainsClass(puml, "samples.inheritance.Child");

		assertPumlContains(puml, "enum \"samples.inheritance.E\"");

		// relationships
		assertPumlContains(puml, "\"samples.inheritance.B\" --|> \"samples.inheritance.A\"");
		assertPumlContains(puml, "\"samples.inheritance.Child\" --|> \"samples.inheritance.Base\"");
		assertPumlContains(puml, "\"samples.inheritance.Child\" ..|> \"samples.inheritance.B\"");

		assertAnyLineContainsAll(puml, "E", "..|>", "A");
	}
}
