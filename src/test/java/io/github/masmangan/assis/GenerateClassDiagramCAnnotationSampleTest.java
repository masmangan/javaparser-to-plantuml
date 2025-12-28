/*
 * Copyright (c) 2025, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;
 
import static io.github.masmangan.assis.TestWorkbench.assertAnyLineContainsAll;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class GenerateClassDiagramCAnnotationSampleTest {

    @Test
    void fooSample_rendersAnnotationsOnTypeFieldAndMethod() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/cannotation",               // <-- your resource folder
                Paths.get("target", "tmp-tests"),
                "cannotation");

        assertAnyLineContainsAll(puml, "class", "samples.cannotation.FooTest", "<<Deprecated>>");

        assertAnyLineContainsAll(puml, "name", "<<NotNull>>");

        assertAnyLineContainsAll(puml, "testFoo", "<<Test>>");
    }
}