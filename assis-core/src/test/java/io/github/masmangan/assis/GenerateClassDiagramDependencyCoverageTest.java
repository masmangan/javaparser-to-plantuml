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

// TODO: Completar testes de dependências
class GenerateClassDiagramDependencyCoverageSamplesTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // Dep 01 — Method return type creates dependency
    // package p1; class B {} class A { B m(){ return null; } }
    // Expect: "p1.A" ..> "p1.B"
    // -------------------------------------------------------------------------

    @Disabled("Enable one test at a time while adding samples and implementing '..>' emission.")
    @Test
    void dep01_methodReturnTypeCreatesDependency() throws Exception {
        String puml = generatePumlFromSample(
                "samples/deps/dep01-return",
                tempDir,
                "dep01-return"
        );

        assertPumlContainsName(puml, "p1.A");
        assertPumlContainsName(puml, "p1.B");
        assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.B");
    }

    // -------------------------------------------------------------------------
    // Dep 02 — Method parameter type creates dependency
    // package p1; class B {} class A { void m(B b) {} }
    // Expect: "p1.A" ..> "p1.B"
    // -------------------------------------------------------------------------

    @Disabled("Enable one test at a time while adding samples and implementing '..>' emission.")
    @Test
    void dep02_methodParameterCreatesDependency() throws Exception {
        String puml = generatePumlFromSample(
                "samples/deps/dep02-param",
                tempDir,
                "dep02-param"
        );

        assertPumlContainsName(puml, "p1.A");
        assertPumlContainsName(puml, "p1.B");
        assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.B");
    }

    // -------------------------------------------------------------------------
    // Dep 03 — Throws clause creates dependency
    // package p1; class X extends Exception {} class A { void m() throws X {} }
    // Expect: "p1.A" ..> "p1.X"
    // -------------------------------------------------------------------------

    @Disabled("Enable one test at a time while adding samples and implementing '..>' emission.")
    @Test
    void dep03_throwsClauseCreatesDependency() throws Exception {
        String puml = generatePumlFromSample(
                "samples/deps/dep03-throws",
                tempDir,
                "dep03-throws"
        );

        assertPumlContainsName(puml, "p1.A");
        assertPumlContainsName(puml, "p1.X");
        assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.X");
    }

    // -------------------------------------------------------------------------
    // Dep 04 — Generic type argument does NOT create dependency in Rodrigues/facts
    // package p1; import java.util.List;
    // class D {} class A { void m(List<D> ds) {} }
    // Expect: NO dependency to p1.D via type-arg
    // (You may or may not emit dependency to java.util.List; we don't assert it.)
    // -------------------------------------------------------------------------

    @Disabled("Enable after implementing '..>' and confirming generic args are not unpacked in facts mode.")
    @Test
    void dep04_genericTypeArgumentDoesNotCreateDependencyInFactsMode() throws Exception {
        String puml = generatePumlFromSample(
                "samples/deps/dep04-generic-arg",
                tempDir,
                "dep04-generic-arg"
        );

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
    // Ensure you do NOT accidentally emit association '-->' when only a method mentions B.
    // Expect: contains '..>' and does NOT contain '-->' between A and B.
    // -------------------------------------------------------------------------

    @Disabled("Enable after implementing '..>' to ensure method usage doesn't create '-->' association.")
    @Test
    void dep05_methodUsageDoesNotCreateAssociation() throws Exception {
        String puml = generatePumlFromSample(
                "samples/deps/dep05-no-association",
                tempDir,
                "dep05-no-association"
        );

        assertPumlContainsName(puml, "p1.A");
        assertPumlContainsName(puml, "p1.B");

        assertAnyLineContainsAll(puml, "p1.A", "..>", "p1.B");

        // No association arrow caused by method-only usage
        assertPumlNotContains(puml, "p1.A\" --> \"p1.B");
        assertPumlNotContains(puml, "p1.A --> p1.B");
    }

    // -------------------------------------------------------------------------
    // Dep 06 — Cross-package dependency (imported type used in signature)
    // package p2; public class G {}
    // package p1; import p2.G; class A { G m(){ return null; } }
    // Expect: "p1.A" ..> "p2.G"
    // -------------------------------------------------------------------------

    @Disabled("Enable after implementing '..>' and confirming FQN resolution for dependencies.")
    @Test
    void dep06_crossPackageDependencyInSignatureUsesFqn() throws Exception {
        String puml = generatePumlFromSample(
                "samples/deps/dep06-cross-package",
                tempDir,
                "dep06-cross-package"
        );

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
        String puml = generatePumlFromSample(
                "samples/associations/sample10-no-method-dependency",
                tempDir,
                "sample10-no-method-dependency"
        );

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
        String puml = generatePumlFromSample(
                "samples/associations/sample11-only-fields",
                tempDir,
                "sample11-only-fields"
        );

        assertAnyLineContainsAll(puml, "A", "-->", "B", ":", "b");
        assertPumlNotContains(puml, "..>");
    }    
}