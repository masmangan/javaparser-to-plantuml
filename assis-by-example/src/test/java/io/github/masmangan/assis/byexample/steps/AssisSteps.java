/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.byexample.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.masmangan.assis.GenerateClassDiagram;


/**
 * 
 */
public class AssisSteps {

	private Path tempProjectDir;
	private String generatedPlantUml;

	@After
	public void cleanup() throws IOException {
		if (tempProjectDir != null && Files.exists(tempProjectDir)) {
			Files.walk(tempProjectDir).sorted((a, b) -> b.compareTo(a)).forEach(p -> {
				try {
					Files.deleteIfExists(p);
				} catch (IOException e) {
					// throw new IllegalStateException();
				}
			});
		}
	}

	@Given("a file named {string} with the content")
	public void a_file_named_with_the_content(String filename, String docString) throws IOException {
		if (tempProjectDir == null) {
			tempProjectDir = Files.createTempDirectory("assis-by-example-");
		}

		String normalized = filename.replace('\\', '/');
		Path target = tempProjectDir.resolve(normalized);

		Files.createDirectories(target.getParent());
		Files.writeString(target, docString, StandardCharsets.UTF_8);
	}

	@When("ASSIS generates a class diagram")
	public void assis_generates_a_class_diagram() throws IOException {
		if (tempProjectDir == null) {
			tempProjectDir = Files.createTempDirectory("assis-by-example");
		}

		Path outDir = tempProjectDir.resolve("out");
		Files.createDirectories(outDir);
		Path outFile = outDir.resolve("class-diagram.puml");

		GenerateClassDiagram.generate(Set.of(tempProjectDir), outFile);

		try (var stream = Files.list(outDir)) {
			Path puml = stream.filter(p -> p.getFileName().toString().endsWith(".puml")).findFirst()
					.orElseThrow(() -> new IllegalStateException("ASSIS did not generate any .puml file in " + outDir));

			generatedPlantUml = Files.readString(puml, StandardCharsets.UTF_8);
		}
	}

	@Then("the diagram contains")
	public void the_diagram_contains(String docString) {
		if (generatedPlantUml == null) {
			throw new IllegalStateException("No diagram captured. Did you run 'When ASSIS generates a class diagram'?");
		}

		assertTrue(generatedPlantUml.contains(docString), () -> "Expected diagram to contain:\n---\n" + docString
				+ "\n---\nBut it was:\n---\n" + generatedPlantUml + "\n---");
	}
}