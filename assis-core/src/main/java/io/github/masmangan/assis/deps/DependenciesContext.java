package io.github.masmangan.assis.deps;

import java.util.Optional;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.Type;

import io.github.masmangan.assis.DeclaredIndex;
import io.github.masmangan.assis.PlantUMLWriter;

public class DependenciesContext implements DependencyContext {

	private final DeclaredIndex idx;
	private final PlantUMLWriter pw;

	public DependenciesContext(DeclaredIndex idx, PlantUMLWriter pw) {
	  this.idx = idx;
	  this.pw = pw;
	}

	@Override
	public Optional<TypeRef> resolveTarget(Type typeNode, Node usageSite) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Optional<TypeRef> resolveScopeName(String simpleName, Node usageSite) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public boolean hasDependency(TypeDeclaration<?> from, TypeRef to) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addDependency(TypeDeclaration<?> from, TypeRef to) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addCherryPick(TypeDeclaration<?> from, TypeRef to) {
		// TODO Auto-generated method stub

	}

}
