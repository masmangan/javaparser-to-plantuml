package io.github.masmangan.assis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

class PlantUMLWriterTest {

	@Test
	void testPlantUMLWriter() {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			// no-op
		}

		assertEquals("", sw.toString());
	}

	@Test
	void beginAndEndDiagramProducesMinimalBlock() throws Exception {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			w.beginDiagram("name");
			w.endDiagram("name");
		}

		String expected = """
				@startuml "name"
				@enduml
				""";

		assertEquals(expected, sw.toString());
	}

	@Test
	void emptyPackageInsideDiagramIsProperlyClosed() throws Exception {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			w.beginDiagram("name");
			w.beginPackage("P");
			w.endPackage("P");
			w.endDiagram("name");
		}

		String expected = """
				@startuml "name"
				package "P" { /' @assis:begin package "P" '/
				} /' @assis:end package "P" '/
				@enduml
				""";

		assertEquals(expected, sw.toString());
	}
	
	@Test
	void emptyClassInsideDiagramIsProperlyClosed() throws Exception {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			w.beginDiagram("name");
			w.beginClass("A", "");
			w.endClass("A");
			w.endDiagram("name");
		}

		String expected = """
				@startuml "name"
				class "A" { /' @assis:begin class "A" '/
				} /' @assis:end class "A" '/
				@enduml
				""";

		assertEquals(expected, sw.toString());
	}

	@Test
	void emptyClassInsidePackageGetsNoIndentation() throws Exception {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			w.beginDiagram("name");
			w.beginPackage("P");
			w.beginClass("A", "");
			w.endClass("A");
			w.endPackage("P");
			w.endDiagram("name");
		}

		String expected = """
				@startuml "name"
				package "P" { /' @assis:begin package "P" '/
				  class "A" { /' @assis:begin class "A" '/
				  } /' @assis:end class "A" '/
				} /' @assis:end package "P" '/
				@enduml
				""";

		assertEquals(expected, sw.toString());
	}
	
}
