/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.deps;

/**
 *
 */
public sealed interface TypeRef permits DeclaredTypeRef, ExternalTypeRef, UnresolvedTypeRef {

	String displayName();
}