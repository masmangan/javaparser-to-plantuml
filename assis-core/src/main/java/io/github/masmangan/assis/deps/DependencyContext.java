package io.github.masmangan.assis.deps;

import java.util.Optional;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.type.Type;

public interface DependencyContext {

    /* ===== owner tracking ===== */

    TypeDeclaration<?> currentOwner();

    /* ===== resolution ===== */

    Optional<TypeRef> resolveTarget(Type typeNode, Node usageSite);

    Optional<TypeRef> resolveScopeName(String simpleName, Node usageSite);

    /* ===== bookkeeping ===== */

    boolean hasDependency(TypeDeclaration<?> from, TypeRef to);

    void addDependency(TypeDeclaration<?> from, TypeRef to);

    void addCherryPick(TypeDeclaration<?> from, TypeRef to);
}