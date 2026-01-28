/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.internal;

/**
 *
 *
 * @author Marco Mangan
 */
record ExternalTypeRef(String fqn) implements TypeRef {

	@Override
	public String displayName() {
		return fqn;
	}

}