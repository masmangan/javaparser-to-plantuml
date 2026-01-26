/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertAnyLineContainsAll;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContainsName;
import static io.github.masmangan.assis.TestWorkbench.assertPumlNotContains;
import static io.github.masmangan.assis.TestWorkbench.generatePumlFromSample;

import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramDependencyCoverageSamplesTest {

	@TempDir
	Path tempDir;

	@Test
	void methodBodyDependency() throws Exception {
		String puml = generatePumlFromSample("samples/deps/bylocal", tempDir, "bylocal");

		assertPumlContainsName(puml, "A");
		assertPumlContainsName(puml, "B");
		assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.B");
	}

	@Test
	void methodReturnTypeCreatesDependency() throws Exception {
		String puml = generatePumlFromSample("samples/deps/byreturn", tempDir, "byreturn");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p1.B");
		assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.B");
	}

	@Disabled("FAILED: Enable one test at a time while adding samples and implementing '..>' emission.")
	@Test
	void dep02_methodParameterCreatesDependency() throws Exception {
		String puml = generatePumlFromSample("samples/deps/byparam", tempDir, "byparam");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p1.B");
		assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.B");
	}

	@Disabled("FAILED: Enable one test at a time while adding samples and implementing '..>' emission.")
	@Test
	void throwsClauseCreatesDependency() throws Exception {
		String puml = generatePumlFromSample("samples/deps/bythrows", tempDir, "bythrows");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p1.X");
		assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.X");
	}
	
	@Test
	void genericTypeArgumentDoesNotCreateDependencyInFactsMode() throws Exception {
		String puml = generatePumlFromSample("samples/deps/generic", tempDir, "generic");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p1.D");

		// Don't infer dependency to D from List<D> in facts mode
		assertPumlNotContains(puml, "..> \"p1.D\"");
		assertPumlNotContains(puml, "..> p1.D");
		assertPumlNotContains(puml, "p1.A\" ..> \"p1.D\"");
		assertPumlNotContains(puml, "p1.A ..> p1.D");
	}

	@Test
	void methodUsageDoesNotCreateAssociation() throws Exception {
		String puml = generatePumlFromSample("samples/deps/omr", tempDir, "omr");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p1.B");

		assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.B");

		// No association arrow caused by method-only usage
		assertPumlNotContains(puml, "p1.A\" --> \"p1.B");
		assertPumlNotContains(puml, "p1.A --> p1.B");
	}

	@Test
	void crossPackageDependencyInSignatureUsesFqn() throws Exception {
		String puml = generatePumlFromSample("samples/deps/crosspkg", tempDir, "crosspkg");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p2.G");
		assertAnyLineContainsAll(puml, "p1.A", "..>", "p2.G");
	}

	@Disabled("FAILED: Enable one sample at a time while adding sample folders/files.")
	@Test
	void methodReturnTypeDoesNotCreateDependency() throws Exception {
		String puml = generatePumlFromSample("samples/deps/nodepret", tempDir, "nodepret");

		assertAnyLineContainsAll(puml, "A", "-->", "B", ":", "b");
		assertPumlNotContains(puml, "..>");
	}

	@Disabled("FAILED: Enable one sample at a time while adding sample folders/files.")
	@Test
	void onlyFieldAssociationsAreEmitted() throws Exception {
		// Do we need class C?
		String puml = generatePumlFromSample("samples/deps/of", tempDir, "of");

		assertAnyLineContainsAll(puml, "A", "-->", "B", ":", "b");
		assertPumlNotContains(puml, "..>");
	}
}