package io.github.masmangan.assis.internal;

import java.util.Objects;

/**
 * A strongly-typed wrapper around a {@link String} representing a type-related
 * name or identifier within ASSIS.
 *
 * <p>
 * {@code TypeKey} exists to make the compiler aware that a given {@code String}
 * is intended to represent a type reference or type identity, rather than an
 * arbitrary textual value. At this stage, {@code TypeKey} carries no semantic
 * guarantees beyond non-nullity.
 * </p>
 *
 * <p>
 * In particular, {@code TypeKey}:
 * <ul>
 * <li>does <strong>not</strong> imply that the type is declared, external, or
 * resolved</li>
 * <li>does <strong>not</strong> enforce any naming convention (e.g. dot vs
 * dollar)</li>
 * <li>does <strong>not</strong> perform validation or normalization</li>
 * </ul>
 * </p>
 *
 * <p>
 * Future refactorings may evolve {@code TypeKey} to carry stronger invariants
 * or additional structure. For now, it should be understood as a
 * compiler-visible marker distinguishing type-related strings from other
 * textual values.
 * </p>
 */
public record TypeKey(String text) {

	public TypeKey {
		Objects.requireNonNull(text, "text");
	}

	@Override
	public String toString() {
		return text;
	}
}