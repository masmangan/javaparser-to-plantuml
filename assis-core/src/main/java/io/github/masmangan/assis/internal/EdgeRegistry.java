/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.internal;

import java.util.HashSet;
import java.util.Set;

record RelKey(String fromFqn, String toFqn) {
}

/**
 *
 * @since 0.9.5
 * @author Marco Mangan
 */
public class EdgeRegistry {

	private final Set<RelKey> seen = new HashSet<>();

	public void registerAssociation(String from, String to) {
		register(from, to);
	}

	public boolean isRegistered(String from, String to) {
		return seen.contains(new RelKey(from, to));
	}

	public void registerDependency(String from, String to) {
		register(from, to);
	}

	private void register(String from, String to) {
		seen.add(new RelKey(from, to));
	}

}
