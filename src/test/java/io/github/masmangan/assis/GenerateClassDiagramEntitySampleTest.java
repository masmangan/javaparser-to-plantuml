package io.github.masmangan.assis;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;

class GenerateClassDiagramEntitySampleTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramContainingEntity() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/entity",
                tempDir,
                "entity");

        assertPumlContains(puml, "class Owner");
        assertPumlContains(puml,"<<Entity>>");
    }
}
