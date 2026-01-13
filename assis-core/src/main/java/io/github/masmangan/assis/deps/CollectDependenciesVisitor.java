package io.github.masmangan.assis.deps;

import java.util.ArrayDeque;
import java.util.Deque;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
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

public final class CollectDependenciesVisitor extends VoidVisitorAdapter<DependencyContext> {

	private final Deque<TypeDeclaration<?>> ownerStack = new ArrayDeque<>();

	/* ===== owner handling ===== */

	@Override
	public void visit(ClassOrInterfaceDeclaration n, DependencyContext ctx) {
		enter(n);
		super.visit(n, ctx);
		exit();
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

	private void enter(TypeDeclaration<?> td) {
		ownerStack.push(td);
	}

	private void exit() {
		ownerStack.pop();
	}

	private TypeDeclaration<?> owner() {
		return ownerStack.peek();
	}

	/* ===== dependency sources ===== */

	@Override
	public void visit(ObjectCreationExpr n, DependencyContext ctx) {
		recordTypeUse(n.getType(), n, ctx);
		super.visit(n, ctx);
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

	/* ===== helpers ===== */

	private void recordTypeUse(Type typeNode, Node site, DependencyContext ctx) {
		if (ownerStack.isEmpty()) {
			return;
		}

		ctx.resolveTarget(typeNode, site).ifPresent(target -> collect(owner(), target, ctx));

		// unwrap generic arguments
		if (typeNode instanceof ClassOrInterfaceType cit) {
			cit.getTypeArguments().ifPresent(args -> args.forEach(arg -> recordTypeUse(arg, site, ctx)));
		}
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
			ctx.addCherryPick(from, to);
		}
	}
}