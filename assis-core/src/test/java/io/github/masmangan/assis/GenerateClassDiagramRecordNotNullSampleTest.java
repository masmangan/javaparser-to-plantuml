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

class GenerateClassDiagramRecordNotNullSampleTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesRecordWithStereotypeOnExternalTypeComponent() throws Exception {
        String puml = generatePumlFromSample(
                "samples/rnull",
                tempDir,
                "rnull"
        );

        assertPumlContains(puml, "record \"samples.rnull.OwnerDto\"");
        assertAnyLineContainsAll(puml, "id", ":", "int");
        assertAnyLineContainsAll(puml, "name", ":", "String", "<<NotNull>>");
        assertAppearsInOrder(puml, "id", "name");
    }
}