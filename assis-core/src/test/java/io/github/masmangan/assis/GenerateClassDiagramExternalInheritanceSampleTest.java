/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertAnyLineContainsAll;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContainsClass;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramExternalInheritanceSampleTest {
	@TempDir
	Path tempDir;

	@Test
	void generatesDiagramContainingInheritance() throws Exception {
		String puml = TestWorkbench.generatePumlFromSample("samples/pkgext", tempDir, "pkgext");

		// types
		assertPumlContainsClass(puml, "ext.Smart1");
		assertPumlContainsClass(puml, "ext.Smart2");

		// Could have the resolution checking the explicit export
		assertAnyLineContainsAll(puml, "ext.Smart1", "--|>", "SourceRoot");
	}

}
