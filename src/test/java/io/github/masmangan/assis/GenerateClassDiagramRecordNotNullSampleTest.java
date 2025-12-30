package io.github.masmangan.assis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import static io.github.masmangan.assis.TestWorkbench.*;

public class GenerateClassDiagramRecordNotNullSampleTest {
    @TempDir
    Path tempDir;
    
    @Test
    void generatesRecordWithStereotypeOnExternalTypeComponent() throws Exception {
        String puml = generatePumlFromSample(
                "samples/rnull",
                tempDir,
                "rnull"
        );

        assertPumlContains(puml, "record \"samples.rnull.OwnerDto\"");
        assertAnyLineContainsAll(puml, "id", ":", "int");
        assertAnyLineContainsAll(puml, "name", ":", "String", "<<NotNull>>");
        assertAppearsInOrder(puml, "id", "name");
    }
}