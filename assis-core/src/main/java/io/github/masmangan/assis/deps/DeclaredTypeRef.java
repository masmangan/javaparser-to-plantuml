package io.github.masmangan.assis.deps;

import com.github.javaparser.ast.body.TypeDeclaration;

public record DeclaredTypeRef(TypeDeclaration<?> declaration) implements TypeRef {

	@Override
	public String displayName() {
		return declaration.getNameAsString();
	}
}