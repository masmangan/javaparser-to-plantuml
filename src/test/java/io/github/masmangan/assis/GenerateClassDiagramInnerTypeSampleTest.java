/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertAppearsInOrder;
import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GenerateClassDiagramInnerTypeSampleTest {

    private static final String RESOURCE_PATH = "samples/inner";

    @TempDir
    Path tempDir;

    @Test
    void generatesNestedEnumInsideClass() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                RESOURCE_PATH,
                tempDir,
                "inner");

        assertPumlContains(puml, "class \"samples.inner.SimpleInner\"");
        assertPumlContains(puml, "enum \"samples.inner.SimpleInner_E\"");
        assertAppearsInOrder(puml, "X", "Y");
        assertPumlContains(puml,
                "\"samples.inner.SimpleInner\" +-- \"samples.inner.SimpleInner_E\"");
    }
}