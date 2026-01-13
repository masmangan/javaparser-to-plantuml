/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertAnyLineContainsAll;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;
import static io.github.masmangan.assis.TestWorkbench.assertPumlNotContains;
import static io.github.masmangan.assis.TestWorkbench.generatePumlFromSample;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramRecordAssociationStereotypeSampleTest {
	@TempDir
	Path tempDir;

	@Test
	void generatesAssociationFromRecordComponentWithStereotype() throws Exception {
		String puml = generatePumlFromSample("samples/rstereo", tempDir, "rstereo");

		assertPumlContains(puml, "record \"samples.rstereo.Order\"");
		assertPumlContains(puml, "class \"samples.rstereo.Customer\"");

		assertPumlNotContains(puml, "customer : Customer");

		assertAnyLineContainsAll(puml, "\"samples.rstereo.Order\"", "-->", "\"samples.rstereo.Customer\"", "customer",
				"<<Deprecated>>");
	}
}