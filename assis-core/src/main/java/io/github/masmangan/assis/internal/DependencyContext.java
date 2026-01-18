/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.deps;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

import io.github.masmangan.assis.DeclaredIndex;
import io.github.masmangan.assis.io.PlantUMLWriter;

/**
 *
 */
public class DependencyContext {

	/**
	 * Logger used by the generator to report progress and parse/write issues.
	 */
	static final Logger logger = Logger.getLogger(DependencyContext.class.getName());

	/**
	 *
	 */
	private final DeclaredIndex idx;

	/**
	 *
	 */
	private final PlantUMLWriter pw;

	/**
	 *
	 */
	private final Set<DepKey> deps = new HashSet<>();

	/**
	 *
	 * @param idx
	 * @param pw
	 */
	public DependencyContext(DeclaredIndex idx, PlantUMLWriter pw) {
		this.idx = idx;
		this.pw = pw;
	}

	/**
	 *
	 * @param typeNode
	 * @param usageSite
	 * @return
	 */
	public Optional<TypeRef> resolveTarget(Type typeNode, Node usageSite) {
		logger.log(Level.INFO, () -> "Resolving target" + typeNode);

		if (!(typeNode instanceof ClassOrInterfaceType cit)) {
			return Optional.empty();
		}

		// FIXME: Symbol Solver here...

		String simpleName = cit.getNameAsString();
		TypeDeclaration<?> td = idx.byFqn.get(simpleName);
		if (td == null) {
			return Optional.of(new ExternalTypeRef(simpleName));
		} else {
			return Optional.of(new DeclaredTypeRef(td));
		}
	}

	/**
	 *
	 * @param simpleName
	 * @param usageSite
	 * @return
	 */
	public Optional<TypeRef> resolveScopeName(String simpleName, Node usageSite) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	/**
	 *
	 * @param from
	 * @param to
	 * @return
	 */
	public boolean hasDependency(TypeDeclaration<?> from, TypeRef to) {
		return deps.contains(new DepKey(from, to));
	}

	/**
	 *
	 * @param from
	 * @param to
	 */
	public void addDependency(TypeDeclaration<?> from, TypeRef to) {
		String fromFqn = DeclaredIndex.deriveFqnDollar(from);
		String toFqn = to.displayName();
		if (to instanceof DeclaredTypeRef dtr) {
			toFqn = DeclaredIndex.deriveFqnDollar(dtr.declaration());
		}
		pw.connectDepends(fromFqn, toFqn);
		deps.add(new DepKey(from, to));
	}

	/**
	 *
	 * @param from
	 * @param to
	 */
	public void addCherryPick(TypeDeclaration<?> from, TypeRef to) {
		String fromFqn = DeclaredIndex.deriveFqnDollar(from);
		final String toFqn;
		if (to instanceof DeclaredTypeRef dtr) {
			toFqn = DeclaredIndex.deriveFqnDollar(dtr.declaration());
		} else {
			toFqn = to.displayName();
		}
		pw.withBeforeTag("@assis:cherry-pick ghost", () -> pw.connectDepends(fromFqn, toFqn));
		deps.add(new DepKey(from, to));
	}

}

/**
 *
 */
record DepKey(TypeDeclaration<?> from, TypeRef to) {
}
