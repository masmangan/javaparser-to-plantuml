/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertAnyLineContainsAll;
import static io.github.masmangan.assis.TestWorkbench.assertAppearsInOrder;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;
import static io.github.masmangan.assis.TestWorkbench.generatePumlFromSample;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramAnnotationCoverageSampleTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesEnumConstantsInDeclarationOrder() throws Exception {
        String puml = generatePumlFromSample(
                "samples/annotations",
                tempDir,
                "annotations"
        );

        
        assertAnyLineContainsAll(puml, "Retention");
        assertAnyLineContainsAll(puml, "Target");
        
        assertAnyLineContainsAll(puml, "MyAnnotation");
        assertAnyLineContainsAll(puml, "value");
        assertAnyLineContainsAll(puml, "level");
        assertAnyLineContainsAll(puml, "1");


    }    
 
}