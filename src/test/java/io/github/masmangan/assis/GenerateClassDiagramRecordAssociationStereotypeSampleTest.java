/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import static io.github.masmangan.assis.TestWorkbench.*;

class GenerateClassDiagramRecordAssociationStereotypeSampleTest {
    @TempDir
    Path tempDir;
    @Test
    void generatesAssociationFromRecordComponentWithStereotype() throws Exception {
        String puml = generatePumlFromSample(
                "samples/rstereo",
                tempDir,
                "rstereo"
        );

        assertPumlContains(puml, "record \"samples.rstereo.Order\"");
        assertPumlContains(puml, "class \"samples.rstereo.Customer\"");

        assertPumlNotContains(puml, "customer : Customer");

        assertAnyLineContainsAll(
                puml,
                "\"samples.rstereo.Order\"",
                "-->",
                "\"samples.rstereo.Customer\"",
                "customer",
                "<<Deprecated>>"
        );
    }
}