/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.cli;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.github.masmangan.assis.GenerateClassDiagram;

/**
 * The {@code AssisApp} class is the PlantUML diagram generator entry point.
 */
public final class AssisApp {

	private static final String MAVEN_POM_PROPS =
	        "META-INF/maven/%s/%s/pom.properties";
	
	private static final String VERSION_UNKNOWN = "unknown";

	private static final Logger LOG = Logger.getLogger(AssisApp.class.getName());

	/**
	 * Default output directory (PlantUML/Jebbs convention).
	 */
	static final String DEFAULT_OUT_DIR = "docs/diagrams/src";

	/**
	 *
	 */
	private AssisApp() {
	}

	/**
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		System.exit(run(args));
	}

	/**
	 * Test-friendly runner.
	 *
	 * @return exit code (0 success, non-zero failure)
	 */
	static int run(String[] args) {
		final CliArgs cli;
		try {
			cli = CliArgs.parse(args);
		} catch (IllegalArgumentException e) {
			LOG.log(Level.SEVERE, e::getMessage);
			return 1;
		}

		if (cli.mode == CliArgs.Mode.HELP) {
			LOG.log(Level.INFO, () -> CliArgs.usage);
			return 0;
		}

		if (cli.mode == CliArgs.Mode.VERSION) {
		    logVersion();
		    return 0;
		}

		final Set<Path> sourceRoots;
		try {
			sourceRoots = SourceLocator.resolve(cli.sourceRoots);
		} catch (Exception e) {
			LOG.log(Level.SEVERE, e::getMessage);
			return 2;
		}

		final Path outDir = (cli.outDir != null) ? cli.outDir.toAbsolutePath().normalize()
				: Path.of(DEFAULT_OUT_DIR).toAbsolutePath().normalize();

		try {
			if (Files.exists(outDir) && !Files.isDirectory(outDir)) {
				throw new IllegalArgumentException("-d must be a directory: " + outDir);
			}
			Files.createDirectories(outDir);

			LOG.info(() -> "Generating diagrams from source roots:");
			for (Path r : sourceRoots) {
				LOG.info(() -> "  - " + r);
			}
			LOG.info(() -> "Writing outputs to: " + outDir);

			GenerateClassDiagram.generate(sourceRoots, outDir);

			return 0;
		} catch (Exception e) {
			LOG.log(Level.SEVERE, e::getMessage);
			return 3;
		}
	}
	
	private static void logVersion() {
	    LOG.log(Level.INFO, () -> "ASSIS " + io.github.masmangan.assis.GenerateClassDiagram.versionOrDev());

	    // JavaParser version
	    LOG.log(Level.INFO, () -> "JavaParser: " + mavenPomVersion("com.github.javaparser", "javaparser-core"));

	    // Java runtime
	    final String javaVersion = System.getProperty("java.version");
	    final String javaVendor  = System.getProperty("java.vendor");
	    final String runtimeName = System.getProperty("java.runtime.name");
	    final String runtimeVer  = System.getProperty("java.runtime.version");

	    final String runtime = (runtimeName != null ? runtimeName : "Java Runtime")
	            + (runtimeVer != null ? " " + runtimeVer : "");

	    LOG.log(Level.INFO, () -> "Java: " + javaVersion + " (" + javaVendor + ") / " + runtime);

	    // OS
	    final String os = System.getProperty("os.name") + " "
	            + System.getProperty("os.version") + " "
	            + System.getProperty("os.arch");

	    LOG.log(Level.INFO, () -> "OS: " + os);
	}

	private static String mavenPomVersion(String groupId, String artifactId) {


		String path = MAVEN_POM_PROPS.formatted(groupId, artifactId);
		try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
	        if (in == null) return VERSION_UNKNOWN;
	        Properties p = new Properties();
	        p.load(in);
	        return p.getProperty("version", VERSION_UNKNOWN);
	    } catch (Exception e) {
	        return VERSION_UNKNOWN;
	    }
	}


}