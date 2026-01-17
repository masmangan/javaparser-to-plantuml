package io.github.masmangan.assis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

class PlantUMLWriterTest {

	@Test
	void testPlantUMLWriter() {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			// no-op
		} catch (Exception e) {
			fail(e);
		}

		assertEquals("", sw.toString());
	}

	@Test
	void beginAndEndDiagramProducesMinimalBlock() {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			w.beginDiagram("D");
			w.endDiagram("D");
		} catch (Exception e) {
			fail(e);
		}

		String expected = """
				@startuml "D"
				@enduml
				""";

		assertEquals(expected, sw.toString());
	}

	@Test
	void emptyPackageInsideDiagramIsProperlyClosed() {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			w.beginDiagram("D");
			w.beginPackage("P");
			w.endPackage("P");
			w.endDiagram("D");
		} catch (Exception e) {
			fail(e);
		}

		String expected = """
				@startuml "D"
				package "P" { /' @assis:begin package "P" '/
				} /' @assis:end package "P" '/
				@enduml
				""";

		assertEquals(expected, sw.toString());
	}

	@Test
	void emptyClassInsideDiagramIsProperlyClosed() {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			w.beginDiagram("D");
			w.beginClass("A", "", "");
			w.endClass("A");
			w.endDiagram("D");
		} catch (Exception e) {
			fail(e);
		}

		String expected = """
				@startuml "D"
				class "A" { /' @assis:begin class "A" '/
				} /' @assis:end class "A" '/
				@enduml
				""";

		assertEquals(expected, sw.toString());
	}

	@Test
	void emptyClassInsidePackageGetsIndentation() {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			w.beginDiagram("D");
			w.beginPackage("P");
			w.beginClass("A", "", "");
			w.endClass("A");
			w.endPackage("P");
			w.endDiagram("D");
		} catch (Exception e) {
			fail(e);
		}

		String expected = """
				@startuml "D"
				package "P" { /' @assis:begin package "P" '/
				  class "A" { /' @assis:begin class "A" '/
				  } /' @assis:end class "A" '/
				} /' @assis:end package "P" '/
				@enduml
				""";

		assertEquals(expected, sw.toString());
	}

	@Test
	void associationWithRole() {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			w.connectAssociation("A", "B", "r", "");
		} catch (Exception e) {
			fail(e);
		}

		String expected = """
				"A" ---> "r" "B"
				""";

		assertEquals(expected, sw.toString());
	}

	@Test
	void associationWithRoleAndStereotype() {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			w.connectAssociation("A", "B", "r", "<<OneToMany>>");
		} catch (Exception e) {
			fail(e);
		}

		String expected = """
				"A" ---> "r" "B" : <<OneToMany>>
				""";

		assertEquals(expected, sw.toString());
	}

	@Test
	void associationWithRoleAndTag() {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			w.withBeforeTag("T", () -> w.connectAssociation("A", "B", "r", ""));
		} catch (Exception e) {
			fail(e);
		}

		String expected = """
				/' T "A" ---> "r" "B" '/
				""";

		assertEquals(expected, sw.toString());
	}

	@Test
	void dependency() {
		StringWriter sw = new StringWriter();

		try (PlantUMLWriter w = new PlantUMLWriter(new PrintWriter(sw))) {
			w.connectDepends("A", "B");
		} catch (Exception e) {
			fail(e);
		}

		String expected = """
				"A" ..> "B"
				""";

		assertEquals(expected, sw.toString());
	}
}
