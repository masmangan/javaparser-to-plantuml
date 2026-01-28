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
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
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
 * Visits AST to collect dependency relationships.
 *
 * @author Marco Mangan
 */
final class CollectDependenciesVisitor extends VoidVisitorAdapter<DependencyContext> {

	private static final Logger logger = Logger.getLogger(CollectDependenciesVisitor.class.getName());

	private final Deque<TypeDeclaration<?>> ownerStack = new ArrayDeque<>();

	@Override
	public void visit(ClassOrInterfaceDeclaration n, DependencyContext ctx) {
		logger.log(Level.INFO, () -> "Collecting dependencies for " + n);
		enter(n);
		super.visit(n, ctx);
		exit();
	}

	@Override
	public void visit(FieldDeclaration fd, DependencyContext ctx) {
		if (ownerStack.isEmpty()) {
			super.visit(fd, ctx);
			return;
		}

		for (VariableDeclarator vd : fd.getVariables()) {
			recordTypeUse(vd.getType(), vd, ctx);
		}

		super.visit(fd, ctx);
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

		// Record components are parameters, not fields.
		n.getParameters().forEach(p -> recordTypeUse(p.getType(), p, ctx));

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

		if (ownerStack.isEmpty()) {
			super.visit(md, ctx);
			return;
		}

		// 1) Parameters: m(b : B) -> A ..> B
		md.getParameters().forEach(p -> recordTypeUse(p.getType(), p, ctx));

		// 2) Throws clause: m() throws X -> A ..> X
		md.getThrownExceptions().forEach(t -> recordTypeUse(t, t, ctx));

		// 3) Return type (keep consistent with other type uses)
		if (!md.getType().isVoidType()) {
			recordTypeUse(md.getType(), md, ctx);
		}

		super.visit(md, ctx);
	}

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

	private void enter(TypeDeclaration<?> td) {
		ownerStack.push(td);
	}

	private void exit() {
		ownerStack.pop();
	}

	private TypeDeclaration<?> owner() {
		return ownerStack.peek();
	}

	private void recordScope(String simpleName, Node site, DependencyContext ctx) {
		if (ownerStack.isEmpty()) {
			return;
		}
		ctx.resolveScopeName(simpleName, site).ifPresent(target -> collect(owner(), target, ctx));
	}

	private void collect(TypeDeclaration<?> from, TypeRef to, DependencyContext ctx) {

		if (ctx.hasDependency(from, to)) {
			return;
		}
		if (to instanceof DeclaredTypeRef) {
			ctx.addDependency(from, to);
		} else {
			// External + Unresolved become ghost deps
			ctx.addCherryPick(from, to);
		}
	}

}