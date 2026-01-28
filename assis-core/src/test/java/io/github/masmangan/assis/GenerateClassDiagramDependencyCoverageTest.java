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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GenerateClassDiagramDependencyCoverageSamplesTest {

	@TempDir
	Path tempDir;

	// related to visitor bug
//	@Test
//	void methodBodyDependency() throws Exception {
//		String puml = generatePumlFromSample("samples/deps/bylocal", tempDir, "bylocal");
//
//		assertPumlContainsName(puml, "A");
//		assertPumlContainsName(puml, "B");
//		assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.B");
//	}

	@Test
	void methodReturnTypeCreatesDependency() throws Exception {
		String puml = generatePumlFromSample("samples/deps/byreturn", tempDir, "byreturn");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p1.B");
		assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.B");
	}

	@Test
	void methodParameterCreatesDependency() throws Exception {
		String puml = generatePumlFromSample("samples/deps/byparam", tempDir, "byparam");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p1.B");
		assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.B");
	}

	@Test
	void throwsClauseCreatesDependency() throws Exception {
		String puml = generatePumlFromSample("samples/deps/bythrows", tempDir, "bythrows");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p1.X");
		assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.X");
	}

	@Test
	void genericTypeArgumentCreateDependency() throws Exception {
		String puml = generatePumlFromSample("samples/deps/generic", tempDir, "generic");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p1.D");

		assertAnyLineContainsAll(puml, "p1.A", "..>", "java.util.List");
		assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.D");

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

	@Test
	void methodReturnTypeDoesNotCreateDependency() throws Exception {
		String puml = generatePumlFromSample("samples/deps/nodepret", tempDir, "nodepret");

		assertAnyLineContainsAll(puml, "A", "-->", "b", "B");
		assertPumlNotContains(puml, "..>");
	}

	@Test
	void onlyFieldAssociationsAreEmitted() throws Exception {
		// Do we need class C?
		String puml = generatePumlFromSample("samples/deps/of", tempDir, "of");

		assertAnyLineContainsAll(puml, "A", "-->", "b", "B");
		assertAnyLineContainsAll(puml, "A", "-->", "c", "C");
		assertPumlNotContains(puml, "..>");
	}

	@ParameterizedTest(name = "dependency coverage: {0}")
	@ValueSource(strings = { "byinstanceof", "bycast", "byclassliteral", "byscope" })
	void dependencyCoverageSamples(String sample) throws Exception {
		String puml = generatePumlFromSample("samples/deps/" + sample, tempDir, sample);

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p1.B");
		assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.B");
		assertPumlNotContains(puml, "-->");

	}

}