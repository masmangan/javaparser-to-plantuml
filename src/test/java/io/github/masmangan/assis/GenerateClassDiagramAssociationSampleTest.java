package io.github.masmangan.assis;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

        assertPumlContains(puml, "Order --> Customer : buyer");
        assertPumlContains(puml, "class Order");
        assertPumlContains(puml, "class Customer");

    }
}
