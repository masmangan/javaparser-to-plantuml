package io.github.masmangan.assis.deps;

/**
 * 
 */
public sealed interface TypeRef permits DeclaredTypeRef, ExternalTypeRef, UnresolvedTypeRef {

	String displayName();
}