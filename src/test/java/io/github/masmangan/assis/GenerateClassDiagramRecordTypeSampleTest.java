/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
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
    }
}