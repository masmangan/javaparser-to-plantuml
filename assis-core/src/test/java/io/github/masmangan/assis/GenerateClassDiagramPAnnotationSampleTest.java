/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertAnyLineContainsAll;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class GenerateClassDiagramPAnnotationSampleTest {

	@Test
	void parameterSample_rendersAnnotationsInlineOnParameters() throws Exception {
		String puml = TestWorkbench.generatePumlFromSample("samples/pannotation", Paths.get("target", "tmp-tests"),
				"param-annotations");

		// method line should contain method name plus all parameter annotations
		// (this forces “inline stereotypes” behavior)
		assertAnyLineContainsAll(puml, "show(", "<<PathVariable>>", "<<RequestParam>>", "<<NotNull>>");

		// optional loose presence
		assertPumlContains(puml, "<<PathVariable>>");
		assertPumlContains(puml, "<<RequestParam>>");
		assertPumlContains(puml, "<<NotNull>>");
	}
}