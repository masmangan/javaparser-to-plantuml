package io.github.masmangan.assis;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContains;
import static io.github.masmangan.assis.TestWorkbench.assertPumlNotContains;

class GenerateClassDiagramDefaultPackageSampleTest {
    @TempDir
    Path tempDir;

    @Test
    void generatesDiagramContainingDefaultPackageClass() throws Exception {
        String puml = TestWorkbench.generatePumlFromSample(
                "samples/defaultpackage",
                tempDir,
                "defaultpackage");

        assertPumlContains(puml, "class Hello");
        assertPumlNotContains(puml, "package");

    }
}
