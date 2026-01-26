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

	// -------------------------------------------------------------------------
	// Dep 04 — Generic type argument does NOT create dependency in Rodrigues/facts
// Is this the source? https://www.researchgate.net/publication/220676637_A_lightweight_approach_to_datatype-generic_rewriting
	// package p1; import java.util.List;
	// class D {} class A { void m(List<D> ds) {} }
	// Expect: NO dependency to p1.D via type-arg
	// (You may or may not emit dependency to java.util.List; we don't assert it.)
	// -------------------------------------------------------------------------

	@Disabled("Enable after implementing '..>' and confirming generic args are not unpacked in facts mode.")
	@Test
	void genericTypeArgumentDoesNotCreateDependencyInFactsMode() throws Exception {
		String puml = generatePumlFromSample("samples/deps/dep04-generic-arg", tempDir, "dep04-generic-arg");

		assertPumlContainsName(puml, "p1.A");
		assertPumlContainsName(puml, "p1.D");

		// Don't infer dependency to D from List<D> in facts mode
		assertPumlNotContains(puml, "..> \"p1.D\"");
		assertPumlNotContains(puml, "..> p1.D");
		assertPumlNotContains(puml, "p1.A\" ..> \"p1.D\"");
		assertPumlNotContains(puml, "p1.A ..> p1.D");
	}

	// -------------------------------------------------------------------------
	// Dep 05 — Dependency only (no field association)
	// package p1; class B {} class A { B m(){ return null; } }
	// Ensure you do NOT accidentally emit association '-->' when only a method
	// mentions B.
	// Expect: contains '..>' and does NOT contain '-->' between A and B.
	// -------------------------------------------------------------------------

	@Disabled("Enable after implementing '..>' to ensure method usage doesn't create '-->' association.")
	@Test
	void methodUsageDoesNotCreateAssociation() throws Exception {
		String puml = generatePumlFromSample("samples/deps/dep05-no-association", tempDir, "dep05-no-association");

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

	// -------------------------------------------------------------------------
	// Sample 10 — Method return type MUST NOT create dependency
	// class A { B b; B m(){ return b; } }
	// Contains: A --> B : b
	// Not contains: A ..> B
	// -------------------------------------------------------------------------

	@Disabled("Enable one sample at a time while adding sample folders/files.")
	@Test
	void sample10_methodReturnTypeDoesNotCreateDependency() throws Exception {
		String puml = generatePumlFromSample("samples/associations/sample10-no-method-dependency", tempDir,
				"sample10-no-method-dependency");

		assertAnyLineContainsAll(puml, "A", "-->", "B", ":", "b");
		assertPumlNotContains(puml, "..>");
	}

	// -------------------------------------------------------------------------
	// Sample 11 — Only fields count; method return doesn't add relations
	// class B {} class C {}
	// class A { B b; C c; B m() {} }
	// Expected: A --> B : b
	// Not contains: A ..> B (dependency)
	// -------------------------------------------------------------------------

	@Disabled("Enable one sample at a time while adding sample folders/files.")
	@Test
	void sample11_onlyFieldAssociationsAreEmitted() throws Exception {
		String puml = generatePumlFromSample("samples/associations/sample11-only-fields", tempDir,
				"sample11-only-fields");

		assertAnyLineContainsAll(puml, "A", "-->", "B", ":", "b");
		assertPumlNotContains(puml, "..>");
	}
}