package io.github.masmangan.assis.deps;

/**
 *
 */
public record UnresolvedTypeRef(String name) implements TypeRef {

	@Override
	public String displayName() {
		return name;
	}
}