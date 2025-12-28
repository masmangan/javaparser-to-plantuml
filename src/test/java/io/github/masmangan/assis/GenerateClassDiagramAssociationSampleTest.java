/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContainsClass;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;

class GenerateClassDiagramAssociationSampleTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramContainingAssociation() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/association",
                tempDir,
                "association");

        assertPumlContains(puml, "\"samples.association.Order\" --> \"samples.association.Customer\" : buyer");
        assertPumlContainsClass(puml, "samples.association.Order");
        assertPumlContainsClass(puml, "samples.association.Customer");

    }
}
