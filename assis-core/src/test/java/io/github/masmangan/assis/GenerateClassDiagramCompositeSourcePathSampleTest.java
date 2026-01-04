package io.github.masmangan.assis;

import static io.github.masmangan.assis.TestWorkbench.assertPumlContainsName;
import static io.github.masmangan.assis.TestWorkbench.generatePumlFromSample;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateClassDiagramCompositeSourcePathSampleTest {

	@TempDir
	Path tempDir;

	@Test
	void compositeSourcePath_scansBothRoots_pbThenPa() throws Exception {
		String puml = generatePumlFromSample("samples/sp/pb", "samples/sp/pa", tempDir, "composite");

		assertPumlContainsName(puml, "pa.A");
		assertPumlContainsName(puml, "pb.B");
	}

	@Test
	void compositeSourcePath_scansBothRoots_paThenPb() throws Exception {
		String puml = generatePumlFromSample("samples/sp/pb", "samples/sp/pa", tempDir, "composite");

		assertPumlContainsName(puml, "pa.A");
		assertPumlContainsName(puml, "pb.B");
	}

}