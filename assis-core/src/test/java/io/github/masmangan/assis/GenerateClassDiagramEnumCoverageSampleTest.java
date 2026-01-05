/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertAnyLineContainsAll;
import static io.github.masmangan.assis.TestWorkbench.assertAppearsInOrder;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;
import static io.github.masmangan.assis.TestWorkbench.generatePumlFromSample;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramEnumCoverageSampleTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesEnumConstantsInDeclarationOrder() throws Exception {
        String puml = generatePumlFromSample(
                "samples/enums/values",
                tempDir,
                "enums-values"
        );

        assertAnyLineContainsAll(puml, "enum", "Kind");

        assertAnyLineContainsAll(puml, "CLASS");
        assertAnyLineContainsAll(puml, "INTERFACE");
        assertAnyLineContainsAll(puml, "ENUM");
        assertAnyLineContainsAll(puml, "RECORD");
        assertAnyLineContainsAll(puml, "ANNOTATION");
    }

    @Test
    void generatesEnumWithTypeStereotypeAndOrderedConstants() throws Exception {
        String puml = generatePumlFromSample(
                "samples/enums/type",
                tempDir,
                "enums-type"
        );

        assertAnyLineContainsAll(puml, "enum", "\"samples.enums.type.Choice\"", "<<Deprecated>>");
        assertAppearsInOrder(puml, "A", "B");
    }

    @Test
    void generatesEnumWithFieldAndFieldStereotype() throws Exception {
        String puml = generatePumlFromSample(
                "samples/enums/fields",
                tempDir,
                "enums-fields"
        );

        assertPumlContains(puml, "enum \"samples.enums.fields.Color\"");
        assertAppearsInOrder(puml, "RED", "BLUE");
        assertAnyLineContainsAll(puml, "rgb", ":", "int", "<<Deprecated>>");
    }

    @Test
    void generatesEnumWithConstructorAndCtorStereotype() throws Exception {
        String puml = generatePumlFromSample(
                "samples/enums/constructors",
                tempDir,
                "enums-constructors"
        );

        assertPumlContains(puml, "enum \"samples.enums.constructors.Flag\"");
        assertAnyLineContainsAll(puml, "<<create>>", "Flag(", "code", ":", "int", "<<Deprecated>>");
        assertAnyLineContainsAll(puml, "code", ":", "int");
    }

    @Test
    void generatesEnumWithMethodsAndMethodStereotypesIncludingStatic() throws Exception {
        String puml = generatePumlFromSample(
                "samples/enums/methods",
                tempDir,
                "enums-methods"
        );

        assertPumlContains(puml, "enum \"samples.enums.methods.Op\"");
        assertAnyLineContainsAll(puml, "apply(", "a", ":", "int", "b", ":", "int", ") : int", "<<Deprecated>>");
        assertAnyLineContainsAll(puml, "parse(", "s", ":", "String", ") : Op", "{static}", "<<Deprecated>>");
    }
}