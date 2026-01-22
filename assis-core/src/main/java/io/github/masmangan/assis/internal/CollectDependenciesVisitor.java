/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 *
 */
final class CollectDependenciesVisitor extends VoidVisitorAdapter<DependencyContext> {

	/**
	 * Logger used by the generator to report progress and parse/write issues.
	 */
	private static final Logger logger = Logger.getLogger(CollectDependenciesVisitor.class.getName());

	/**
	 *
	 */
	private final Deque<TypeDeclaration<?>> ownerStack = new ArrayDeque<>();

	@Override
	public void visit(ClassOrInterfaceDeclaration n, DependencyContext ctx) {
		logger.log(Level.INFO, () -> "Collecting dependencies for " + n);
		enter(n);
		super.visit(n, ctx);
		exit();
	}

	@Override
	public void visit(ObjectCreationExpr n, DependencyContext ctx) {
		logger.log(Level.INFO, () -> "Object creationg for " + n);
		recordTypeUse(n.getType(), n, ctx);
		super.visit(n, ctx);
	}

	@Override
	public void visit(EnumDeclaration n, DependencyContext ctx) {
		enter(n);
		super.visit(n, ctx);
		exit();
	}

	@Override
	public void visit(RecordDeclaration n, DependencyContext ctx) {
		enter(n);
		super.visit(n, ctx);
		exit();
	}

	@Override
	public void visit(InstanceOfExpr n, DependencyContext ctx) {
		recordTypeUse(n.getType(), n, ctx);
		super.visit(n, ctx);
	}

	@Override
	public void visit(CastExpr n, DependencyContext ctx) {
		recordTypeUse(n.getType(), n, ctx);
		super.visit(n, ctx);
	}

	@Override
	public void visit(ClassExpr n, DependencyContext ctx) {
		recordTypeUse(n.getType(), n, ctx);
		super.visit(n, ctx);
	}

	@Override
	public void visit(MethodCallExpr n, DependencyContext ctx) {
		n.getScope().ifPresent(scope -> {
			if (scope instanceof NameExpr ne) {
				recordScope(ne.getNameAsString(), n, ctx);
			}
		});
		super.visit(n, ctx);
	}

	@Override
	public void visit(MethodDeclaration md, DependencyContext ctx) {
		logger.log(Level.INFO, () -> "Collecting dependencies for " + md);
		super.visit(md, ctx);
		if (!md.getType().isVoidType()) {
			ctx.resolveTarget(md.getType(), md).ifPresent(to -> collect(owner(), to, ctx));
		}
	}

	/**
	 *
	 * @param typeNode
	 * @param site
	 * @param ctx
	 */
	private void recordTypeUse(Type typeNode, Node site, DependencyContext ctx) {
		logger.log(Level.INFO, () -> "Record Type Use for " + typeNode);
		if (ownerStack.isEmpty()) {
			return;
		}

		ctx.resolveTarget(typeNode, site).ifPresent(target -> collect(owner(), target, ctx));

		// unwrap generic arguments
		if (typeNode instanceof ClassOrInterfaceType cit) {
			cit.getTypeArguments().ifPresent(args -> args.forEach(arg -> recordTypeUse(arg, site, ctx)));
		}
	}

	/**
	 *
	 * @param td
	 */
	private void enter(TypeDeclaration<?> td) {
		ownerStack.push(td);
	}

	/**
	 *
	 */
	private void exit() {
		ownerStack.pop();
	}

	/**
	 *
	 * @return
	 */
	private TypeDeclaration<?> owner() {
		return ownerStack.peek();
	}

	/**
	 *
	 * @param simpleName
	 * @param site
	 * @param ctx
	 */
	private void recordScope(String simpleName, Node site, DependencyContext ctx) {
		if (ownerStack.isEmpty()) {
			return;
		}

		ctx.resolveScopeName(simpleName, site).ifPresent(target -> collect(owner(), target, ctx));
	}

	/**
	 *
	 * @param from
	 * @param to
	 * @param ctx
	 */
	private void collect(TypeDeclaration<?> from, TypeRef to, DependencyContext ctx) {
		if (to instanceof DeclaredTypeRef) {
			ctx.addDependency(from, to);
		} else {
			ctx.addCherryPick(from, to);
		}
	}
}