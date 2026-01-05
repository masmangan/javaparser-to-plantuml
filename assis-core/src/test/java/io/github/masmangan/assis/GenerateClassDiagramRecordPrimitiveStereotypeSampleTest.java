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

class GenerateClassDiagramRecordPrimitiveStereotypeSampleTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesRecordWithStereotypeOnPrimitiveComponent() throws Exception {
        String puml = generatePumlFromSample(
                "samples/rannotation",
                tempDir,
                "rannotation"
        );

        assertPumlContains(puml, "record \"samples.rannotation.Point\"");
        assertAnyLineContainsAll(puml, "px", ":", "int", "<<Deprecated>>");
        assertAnyLineContainsAll(puml, "py", ":", "int");
        assertAppearsInOrder(puml, "px", "py");
    }
}