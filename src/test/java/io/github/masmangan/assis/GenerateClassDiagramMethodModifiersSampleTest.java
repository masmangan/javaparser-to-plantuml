package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramMethodModifiersSampleTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramContainingMethodModifiers() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/mmodifiers",
                tempDir,
                "mmodifiers");

        assertPumlContains(puml, "class AbstractMethods");
        assertPumlContains(puml, "abstractMethod");
        assertPumlContains(puml, "{abstract}");

        assertPumlContains(puml, "class ConcreteMethods");
        assertPumlContains(puml, "finalMethod");
        assertPumlContains(puml, "{final}");

        assertPumlContains(puml, "staticMethod");
        assertPumlContains(puml, "{static}");

        assertPumlContains(puml, "normalMethod");
    }
}
