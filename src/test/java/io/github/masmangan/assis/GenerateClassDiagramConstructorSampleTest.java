package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class GenerateClassDiagramConstructorSampleTest {

    @Test
    void rendersAllConstructorVisibilitiesWithCreateStereotype() throws Exception {
        Path tempDir = Path.of("target", "tmp-constructors");

        String puml = generatePumlFromSample(
                "samples/constructor",
                tempDir,
                "person-constructors"
        );

        // Public no-arg
        assertAnyLineContainsAll(puml, "+", "<<create>>", "Person", "()");

        // Public single-arg
        assertAnyLineContainsAll(puml, "+", "<<create>>", "Person", "name : String");

        // Protected two-arg
        assertAnyLineContainsAll(puml, "#", "<<create>>", "Person", "name : String", "age : int");

        // Package-private
        assertAnyLineContainsAll(puml, "~", "<<create>>", "Person", "weight : double");

        // Private
        assertAnyLineContainsAll(puml, "-", "<<create>>", "Person", "age : int");

        // Copy / convenience
        assertAnyLineContainsAll(puml, "+", "<<create>>", "Person", "other : Person");
    }
}