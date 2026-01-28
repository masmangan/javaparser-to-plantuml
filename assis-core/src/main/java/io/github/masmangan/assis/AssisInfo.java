/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

/**
 * Placeholder for ASSIS CORE information.
 *
 * @author Marco Mangan
 */
public class AssisInfo {

	private AssisInfo() {

	}

	/**
	 * Returns the implementation version of the running package, or {@code "dev"}
	 * when the version metadata is not available (e.g., during local development).
	 *
	 * @return the implementation version from the JAR manifest, or {@code "dev"}
	 */
	public static String versionOrDev() {
		String v = GenerateClassDiagram.class.getPackage().getImplementationVersion();
		return (v == null || v.isBlank()) ? "dev" : v;
	}

}
