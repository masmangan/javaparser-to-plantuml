package io.github.masmangan.assis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import static io.github.masmangan.assis.TestWorkbench.*;

class GenerateClassDiagramRecordPrimitiveStereotypeSampleTest {

    @TempDir
    Path tempDir;
    
    @Test
    void generatesRecordWithStereotypeOnPrimitiveComponent() throws Exception {
        String puml = generatePumlFromSample(
                "samples/rannotation",
                tempDir,
                "rannotation"
        );

        assertPumlContains(puml, "record \"samples.rannotation.Point\"");
        assertAnyLineContainsAll(puml, "px", ":", "int", "<<Deprecated>>");
        assertAnyLineContainsAll(puml, "py", ":", "int");
        assertAppearsInOrder(puml, "px", "py");
    }
}