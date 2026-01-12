/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramRecordTypeSampleTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesRecordWithComponents() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/records", tempDir, "records");

        TestWorkbench.assertPumlContains(puml, "package \"samples.records\"");
        TestWorkbench.assertPumlContains(puml, "record \"samples.records.Point\"");

        TestWorkbench.assertAnyLineContainsAll(puml, "x", ":", "int");
        TestWorkbench.assertAnyLineContainsAll(puml, "y", ":", "int");

        // --- new asserts for Outer.Inner component in record R ---

        TestWorkbench.assertPumlContains(puml, "record \"samples.records.R\"");
        TestWorkbench.assertPumlContains(puml, "class \"samples.records.Outer$Inner\"");

        // nesting edge Outer +-- Outer$Inner
        TestWorkbench.assertPumlContains(puml,
                "\"samples.records.Outer\" +-- \"samples.records.Outer$Inner\"");

        // association edge from record R to Outer$Inner with role "x"
        TestWorkbench.assertAnyLineContainsAll(puml,
                "\"samples.records.R\"", "--->", "\"samples.records.Outer$Inner\"", "x");
    }
}