/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramRecordAssociationSampleTest {

    @TempDir
    Path tempDir;
    
    @Test
    void generatesAssociationFromRecordComponent() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/rassociation",
                tempDir,
                "ownerdto"
        );

        TestWorkbench.assertPumlContains(
                puml,
                "record \"samples.rassociation.OwnerDto\""
        );

        TestWorkbench.assertAnyLineContainsAll(
                puml,
                "id", ":", "int"
        );

        TestWorkbench.assertPumlNotContains(
                puml,
                "name : Name"
        );

        TestWorkbench.assertPumlContains(
                puml,
                "\"samples.rassociation.OwnerDto\" --> \"samples.rassociation.Name\" : name"
        );
    }
}