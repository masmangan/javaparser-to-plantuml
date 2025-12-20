package io.github.masmangan.assis;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;

public class DiagramGenerationModifiersSampleTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramContainingHelloClass() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample("samples/modifiers", tempDir, "modifiers");

        assertPumlContains(puml, "abstract class AbstractClass");
        assertPumlContains(puml, "class FinalClass <<final>>");
        assertPumlContains(puml, "class PlainClass");

    }
}
