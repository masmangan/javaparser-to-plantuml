/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.internal;

import com.github.javaparser.ast.body.TypeDeclaration;

/**
 *
 *
 * @author Marco Mangan
 */
record DeclaredTypeRef(TypeDeclaration<?> declaration) implements TypeRef {

	@Override
	public String displayName() {
		return declaration.getNameAsString();
	}

}