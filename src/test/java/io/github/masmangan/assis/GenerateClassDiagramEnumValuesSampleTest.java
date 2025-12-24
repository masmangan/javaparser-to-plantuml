package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class GenerateClassDiagramEnumValuesSampleTest {

    @Test
    void rendersEnumConstantsInDeclarationOrder() throws Exception {
        Path tempDir = Path.of("target", "tmp-enumvalues");

        String puml = generatePumlFromSample(
                "samples/enumvalues",
                tempDir,
                "enum-values"
        );

        assertAnyLineContainsAll(puml, "enum", "Kind");

        assertAnyLineContainsAll(puml, "CLASS");
        assertAnyLineContainsAll(puml, "INTERFACE");
        assertAnyLineContainsAll(puml, "ENUM");
        assertAnyLineContainsAll(puml, "RECORD");
        assertAnyLineContainsAll(puml, "ANNOTATION");
    }
}