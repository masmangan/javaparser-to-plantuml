/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertAppearsInOrder;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;
import static io.github.masmangan.assis.TestWorkbench.assertPumlNotContains;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramInnerTypeSampleTest {

	private static final String RESOURCE_PATH = "samples/inner";

	@TempDir
	Path tempDir;

	@Test
	void generatesNestedTypes_andNestingEdges() throws Exception {
		String puml = TestWorkbench.generatePumlFromSample(RESOURCE_PATH, tempDir, "inner");

		// Existing: nested enum inside class
		assertPumlContains(puml, "class \"samples.inner.SimpleInner\"");
		assertPumlContains(puml, "enum \"samples.inner.SimpleInner$E\"");
		assertAppearsInOrder(puml, "X", "Y");
		assertPumlContains(puml, "\"samples.inner.SimpleInner\" +-- \"samples.inner.SimpleInner$E\"");

		// New: deep nesting A -> B -> C
		assertPumlContains(puml, "class \"samples.inner.A\"");
		assertPumlContains(puml, "class \"samples.inner.A$B\"");
		assertPumlContains(puml, "class \"samples.inner.A$B$C\"");

		// Correct edges (immediate ownership)
		assertPumlContains(puml, "\"samples.inner.A\" +-- \"samples.inner.A$B\"");
		assertPumlContains(puml, "\"samples.inner.A$B\" +-- \"samples.inner.A$B$C\"");

		// Must NOT flatten ownership
		assertPumlNotContains(puml, "\"samples.inner.A\" +-- \"samples.inner.A$B$C\"");
	}
}