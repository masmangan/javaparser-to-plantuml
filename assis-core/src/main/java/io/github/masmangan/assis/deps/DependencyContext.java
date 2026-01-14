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
import io.github.masmangan.assis.PlantUMLWriter;

record DepKey(TypeDeclaration<?> from, TypeRef to) {
}

public class DependencyContext {
	/**
	 * Logger used by the generator to report progress and parse/write issues.
	 */
	static final Logger logger = Logger.getLogger(DependencyContext.class.getName());

	private final DeclaredIndex idx;
	private final PlantUMLWriter pw;
	Set<DepKey> deps = new HashSet<>();

	public DependencyContext(DeclaredIndex idx, PlantUMLWriter pw) {
		this.idx = idx;
		this.pw = pw;
	}

	public Optional<TypeRef> resolveTarget(Type typeNode, Node usageSite) {
		logger.log(Level.INFO, () -> "Resolving target" + typeNode);

		if (!(typeNode instanceof ClassOrInterfaceType cit)) {
	        return Optional.empty();
	    }

	    String simpleName = cit.getNameAsString();

	    // ask the index for a declared type with this simple name
	    //Optional<TypeDeclaration<?>> decl = idx.findBySimpleName(simpleName, usageSite);
	    TypeDeclaration<?> td = idx.byFqn.get(simpleName);
	    if (td == null)
	        return Optional.of(new ExternalTypeRef(simpleName));
	    else
	    return Optional.of(new DeclaredTypeRef(td));
	}


	public Optional<TypeRef> resolveScopeName(String simpleName, Node usageSite) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	public boolean hasDependency(TypeDeclaration<?> from, TypeRef to) {
		return deps.contains(new DepKey(from, to));
	}

	public void addDependency(TypeDeclaration<?> from, TypeRef to) {
		// TODO: complete reference end fqn
		String fromFqn = DeclaredIndex.deriveFqnDollar(from);
		String toFqn = to.displayName();
		if (to instanceof DeclaredTypeRef dtr) {
			toFqn = DeclaredIndex.deriveFqnDollar(dtr.declaration());
		}
  
		pw.connectDepends(fromFqn, toFqn);
		deps.add(new DepKey(from, to));

	}

	public void addCherryPick(TypeDeclaration<?> from, TypeRef to) {
		// TODO complete reference end fqn
		String fromFqn = DeclaredIndex.deriveFqnDollar(from);
		String toFqn = "__CANARY";
		pw.withBeforeTag("A", () -> pw.connectDepends(fromFqn, toFqn));
		deps.add(new DepKey(from, to));

	}

}
