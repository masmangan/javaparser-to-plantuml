/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertAnyLineContainsAll;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContainsName;
import static io.github.masmangan.assis.TestWorkbench.assertPumlNotContains;
import static io.github.masmangan.assis.TestWorkbench.generatePumlFromSample;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;

import java.nio.file.Path;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramAssociationCoverageSamplesTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesAssociationForSimpleFieldSamePackage() throws Exception {
        String puml = generatePumlFromSample(
                "samples/associations/fields",
                tempDir,
                "fields"
        );

        assertPumlContainsName(puml, "p1.A");
        assertPumlContainsName(puml, "p1.B");
        assertAnyLineContainsAll(puml, "p1.A", "-->", "p1.B", ":", "b");
    }

    @Test
    void generatesAssociationsForArrays1dAnd2d() throws Exception {
        String puml = generatePumlFromSample(
                "samples/associations/arrays",
                tempDir,
                "arrays"
        );

        assertPumlContainsName(puml, "p1.ArraysSample");
        assertPumlContainsName(puml, "p1.C");

        assertAnyLineContainsAll(puml, "p1.ArraysSample", "-->", "p1.C", ":", "cs");
        assertAnyLineContainsAll(puml, "p1.ArraysSample", "-->", "p1.C", ":", "matrix");
    }

    @Test
    void rendersGenericFieldButDoesNotCreateAssociationYet() throws Exception {
        String puml = generatePumlFromSample(
                "samples/associations/lists",
                tempDir,
                "lists"
        );

        assertPumlContains(puml, "class \"p1.ListField\"");
        assertPumlContains(puml, "class \"p1.D\"");

        assertAnyLineContainsAll(puml, "ds", ":", "List<D>");

        assertPumlNotContains(puml, "-->");
    }

    @Test
    void sample04_rendersRecordComponentListAsFieldWithoutAssociationInterpretation() throws Exception {
        String puml = generatePumlFromSample(
                "samples/associations/records",
                tempDir,
                "records"
        );

        assertPumlContains(puml, "record \"p1.ListRecord\"");
        assertPumlContains(puml, "class \"p1.E\"");

        assertAnyLineContainsAll(puml, "es", ":", "List<E>");

        assertPumlNotContains(puml, "-->");
    }

    @Test
    void sample05_rendersOptionalFieldAndRecordComponentAsFieldsWithoutAssociationInterpretation() throws Exception {
        String puml = generatePumlFromSample(
                "samples/associations/optionals",
                tempDir,
                "optionals"
        );

        assertPumlContainsName(puml, "p1.OptionalField");
        assertPumlContainsName(puml, "p1.OptionalRecord");
        assertPumlContainsName(puml, "p1.F");

        assertAnyLineContainsAll(puml, "f", ":", "Optional<F>");

        assertPumlNotContains(puml, "-->");
    }

    @Test
    void sample06_generatesAssociationForCrossPackageTypeUsingFqn() throws Exception {
        String puml = generatePumlFromSample(
                "samples/associations/cross",
                tempDir,
                "cross"
        );

        assertPumlContainsName(puml, "p1.CrossPackage");
        assertPumlContainsName(puml, "p2.G");
        assertAnyLineContainsAll(puml, "p1.CrossPackage", "-->", "p2.G", ":", "g");
    }

    @Test
    void sample07_generatesAssociationForEnumField() throws Exception {
        String puml = generatePumlFromSample(
                "samples/associations/enum",
                tempDir,
                "enum"
        );

        assertPumlContainsName(puml, "p1.EnumFieldSample");
        assertPumlContainsName(puml, "p1.H");
        assertAnyLineContainsAll(puml, "p1.EnumFieldSample", "-->", "p1.H", ":", "h");
    }

    // -------------------------------------------------------------------------
    // Sample 08 — Interface constant
    // package p1; class I {}
    // interface InterfaceConstant { I DEFAULT = null; }
    // "p1.InterfaceConstant" --> "p1.I" : DEFAULT
    // -------------------------------------------------------------------------

    @Disabled("Enable one sample at a time while adding sample folders/files.")
    @Test
    void sample08_generatesAssociationForInterfaceConstantField() throws Exception {
        String puml = generatePumlFromSample(
                "samples/associations/sample08-interface-constant",
                tempDir,
                "sample08-interface-constant"
        );

        assertPumlContainsName(puml, "p1.InterfaceConstant");
        assertPumlContainsName(puml, "p1.I");
        assertAnyLineContainsAll(puml, "p1.InterfaceConstant", "-->", "p1.I", ":", "DEFAULT");
    }

    // -------------------------------------------------------------------------
    // Sample 09 — Field annotation stereotypes in relationship (field + record component)
    // "p1.FieldStereotypeSample" --> "p1.J" : j <<AssocTag>>
    // "p1.RecordStereotypeSample" --> "p1.J" : j <<AssocTag>>
    // -------------------------------------------------------------------------

    @Disabled("Enable one sample at a time while adding sample folders/files.")
    @Test
    void sample09_generatesAssociationStereotypesFromAnnotations() throws Exception {
        String puml = generatePumlFromSample(
                "samples/associations/sample09-field-stereotype",
                tempDir,
                "sample09-field-stereotype"
        );

        assertPumlContainsName(puml, "p1.FieldStereotypeSample");
        assertPumlContainsName(puml, "p1.RecordStereotypeSample");
        assertPumlContainsName(puml, "p1.J");

        assertAnyLineContainsAll(puml, "p1.FieldStereotypeSample", "-->", "p1.J", ":", "j", "<<AssocTag>>");
        assertAnyLineContainsAll(puml, "p1.RecordStereotypeSample", "-->", "p1.J", ":", "j", "<<AssocTag>>");
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