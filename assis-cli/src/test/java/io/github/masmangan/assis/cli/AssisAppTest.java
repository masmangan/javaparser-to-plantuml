/*
 * Copyright (c) 2025-2026, Marco Mangan.
 * All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.cli;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

/**
 * 
 */
class AssisAppTest {

	private static final String PUML_FILE = "class-diagram.puml";

	@TempDir
	Path tempDir;

	@Test
	void generatesDiagramFromHelloUsingSourcepathAndOutputDirectory() throws Exception {
		Path srcMainJava = tempDir.resolve("src/somewhere");
		Files.createDirectories(srcMainJava);

		Files.writeString(srcMainJava.resolve("Hello.java"), """
				public class Hello {
				}
				""", UTF_8);

		Path outputDirectory = tempDir.resolve("anotherdoc");

		int code = AssisApp
				.run(new String[] { "-sourcepath", srcMainJava.toString(), "-d", outputDirectory.toString() });

		assertEquals(0, code);
		assertTrue(Files.exists(outputDirectory), "Expected output directory to exist: " + outputDirectory + "\n");

		Path pumlPath = outputDirectory.resolve(PUML_FILE);
		assertTrue(Files.exists(pumlPath), "Expected output PUML file to exist: " + pumlPath + "\n");

		String puml = Files.readString(pumlPath, UTF_8);
		assertTrue(puml.contains("Hello") || puml.contains("\"Hello\""),
				"Expected diagram to mention Hello. Content:\n" + puml);
	}

	@ResourceLock("ASSIS_DEFAULT_OUTPUT")
	@Test
	void generatesDiagramFromHelloUsingSourcepathOnly() throws Exception {
		Path sourcePath = tempDir.resolve("src");
		Files.createDirectories(sourcePath);

		Files.writeString(sourcePath.resolve("Hello.java"), """
				public class Hello {
				}
				""", UTF_8);

		Path expectedOut = Path.of("docs/diagrams/src").toAbsolutePath().normalize();
		Path pumlPath = expectedOut.resolve(PUML_FILE);

		Files.deleteIfExists(pumlPath);

		try (var outBuf = new ByteArrayOutputStream(); var errBuf = new ByteArrayOutputStream();

		) {
			int code = AssisApp.run(new String[] { "-sourcepath", sourcePath.toString() });

			assertEquals(0, code);
			assertTrue(Files.exists(expectedOut), "Expected default output directory to exist: " + expectedOut + "\n");
			assertTrue(Files.exists(pumlPath), "Expected output PUML file to exist: " + pumlPath + "\n");

			String puml = Files.readString(pumlPath, UTF_8);
			assertTrue(puml.contains("Hello"), "Expected diagram to mention Hello. Content:\n" + puml);
		} finally {
			try {
				Files.deleteIfExists(pumlPath);
			} catch (Exception ignored) {
				throw new IllegalStateException("Clean up failed.");
			}
		}
	}

	@ResourceLock("ASSIS_DEFAULT_OUTPUT")
	@Test
	void generatesDiagramFromDefaults() throws Exception {
		Path expectedOut = Path.of("docs/diagrams/src").toAbsolutePath().normalize();
		Path pumlPath = expectedOut.resolve(PUML_FILE);

		Files.deleteIfExists(pumlPath);

		try {
			int code = AssisApp.run(new String[] {});

			assertEquals(0, code);
			assertTrue(Files.exists(expectedOut), "Expected default output directory to exist: " + expectedOut + "\n");
			assertTrue(Files.exists(pumlPath), "Expected output PUML file to exist: " + pumlPath + "\n");

			String puml = Files.readString(pumlPath, UTF_8);
			assertTrue(puml.contains("AssisApp"), "Expected diagram to mention Hello. Content:\n" + puml);
		} finally {
			try {
				Files.deleteIfExists(pumlPath);
			} catch (Exception ignored) {
				throw new IllegalStateException("Clean up failed.");
			}
		}
	}

	@Test
	void generatesDiagramFromCompositeSourcepathAndOutputDirectory() throws Exception {
		Path sp1 = tempDir.resolve("p1");
		Path sp2 = tempDir.resolve("p2");
		Files.createDirectories(sp1);
		Files.createDirectories(sp2);

		Files.writeString(sp1.resolve("Hello.java"), """
				public class Hello {
				}
				""", UTF_8);

		Files.writeString(sp2.resolve("World.java"), """
				public class World {
				}
				""", UTF_8);

		Path out = tempDir.resolve("anotherdoc");

		var outBuf = new ByteArrayOutputStream();
		var errBuf = new ByteArrayOutputStream();

		String compositeSourcepath = sp1.toString() + File.pathSeparator + sp2.toString();
		int code = AssisApp.run(new String[] { "-sourcepath", compositeSourcepath, "-d", out.toString() });

		assertEquals(0, code, debugBuffers(outBuf, errBuf));
		assertTrue(Files.exists(out),
				"Expected output directory to exist: " + out + "\n" + debugBuffers(outBuf, errBuf));

		Path pumlPath = out.resolve(PUML_FILE);
		assertTrue(Files.exists(pumlPath),
				"Expected output PUML file to exist: " + pumlPath + "\n" + debugBuffers(outBuf, errBuf));

		String puml = Files.readString(pumlPath, UTF_8);
		assertTrue(puml.contains("Hello") || puml.contains("\"Hello\""),
				"Expected diagram to mention Hello. Content:\n" + puml);
		assertTrue(puml.contains("World") || puml.contains("\"World\""),
				"Expected diagram to mention World (composite sourcepath). Content:\n" + puml);
	}

	@Test
	void invalidArgumentReturnsNonZero() {
		try (var logs = new JulLogCaptor(AssisApp.class)) {
			int code = AssisApp.run(new String[] { "-nope" });

			assertNotEquals(0, code, "Expected non-zero exit code for invalid arg.\nLogs:\n" + logs.dump());

			assertTrue(logs.any(Level.SEVERE, "unknown") || logs.any(Level.SEVERE, "option"),
					"Expected a SEVERE message mentioning unknown/invalid option.\nLogs:\n" + logs.dump());
		}
	}

	@Test
	void checkHelpKnobExists() {
		try (var logs = new JulLogCaptor(AssisApp.class)) {
			int code = AssisApp.run(new String[] { "--help" });

			assertEquals(0, code, "Expected zero exit code for --help.\nLogs:\n" + logs.dump());

			assertTrue(logs.any(Level.INFO, "usage"),
					"Expected help text to contain 'usage' (logged at CONFIG).\nLogs:\n" + logs.dump());
		}
	}

	@Test
	void checkVersionExists() {
		try (var logs = new JulLogCaptor(AssisApp.class)) {
			int code = AssisApp.run(new String[] { "--version" });

			assertEquals(0, code, "Expected zero exit code for --version.\nLogs:\n" + logs.dump());

			assertTrue(logs.any(Level.INFO, "ASSIS"),
					"Expected version output to mention ASSIS (logged at CONFIG).\nLogs:\n" + logs.dump());
		}
	}

	private static String debugBuffers(ByteArrayOutputStream outBuf, ByteArrayOutputStream errBuf) {
		return "stdout:\n" + outBuf.toString(UTF_8) + "\n\nstderr:\n" + errBuf.toString(UTF_8);
	}
}