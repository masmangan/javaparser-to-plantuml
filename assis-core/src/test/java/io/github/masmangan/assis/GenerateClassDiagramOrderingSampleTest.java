/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertAppearsInOrder;
import static io.github.masmangan.assis.TestWorkbench.generatePumlFromSample;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class GenerateClassDiagramOrderingSampleTest {

    @Test
    void packageAndTypeOrderingIsDeterministic() throws Exception {
        Path tempDir = Path.of("target", "tmp-ordering");

        String puml = generatePumlFromSample(
                "samples/ordering",
                tempDir,
                "ordering"
        );

        // If you want to lock package order:
        assertAppearsInOrder(puml,
                "package \"samples.ordering.p1\"",
                "package \"samples.ordering.p2\""
        );

        // If you want to lock type order within p1:
        assertAppearsInOrder(puml,
                "class \"samples.ordering.p1.A\"",
                "class \"samples.ordering.p1.B\""
        );
    }
}