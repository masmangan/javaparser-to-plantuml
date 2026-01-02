/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;

/**
 * 
 */
class CollectRelationshipsVisitor {
	
	/**
	 * 
	 */
	private static final String HAS_A_INNER = " +-- ";

	/**
	 * 
	 */
	private static final String HAS_A = " --> ";

	/**
	 * 
	 */
	private static final String IS_A_IMPLEMENTS = " ..|> ";

	/**
	 * 
	 */
	private static final String IS_A_EXTENDS = " --|> ";

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
	 * @param idx
	 */
	CollectRelationshipsVisitor(final DeclaredIndex idx, final PlantUMLWriter pw) {
		this.idx = idx;
		this.pw = pw;
	}

	/**
	 * 
	 */
	void emitAll() {
		for (var entry : idx.fqnsByPkg.entrySet()) {
			String pkg = entry.getKey();
			for (String fqn : entry.getValue()) {
				TypeDeclaration<?> td = idx.byFqn.get(fqn);
				emitExtendsImplements(pkg, fqn, td);
			}
		}

		for (String fqn : idx.byFqn.keySet()) {
			String ownerFqn = ownerFqnOf(fqn);
			if (ownerFqn != null && idx.byFqn.containsKey(ownerFqn)) {
				pw.println(idx.qPuml(ownerFqn) + HAS_A_INNER + idx.qPuml(fqn));
			}
		}

		for (var entry : idx.fqnsByPkg.entrySet()) {
			String pkg = entry.getKey();
			for (String fqn : entry.getValue()) {
				TypeDeclaration<?> td = idx.byFqn.get(fqn);
				emitAssociations(pkg, fqn, td);
			}
		}
	}

	/**
	 * 
	 * @param pkg
	 * @param subFqn
	 * @param td
	 */
	private void emitExtendsImplements(String pkg, String subFqn, TypeDeclaration<?> td) {
		if (td instanceof ClassOrInterfaceDeclaration cid) {
			for (ClassOrInterfaceType ext : cid.getExtendedTypes()) {
				emitExtends(pkg, subFqn, ext);
			}

			for (ClassOrInterfaceType impl : cid.getImplementedTypes()) {
				emitImplements(pkg, subFqn, impl);
			}
		} else if (td instanceof EnumDeclaration ed) {
			for (ClassOrInterfaceType impl : ed.getImplementedTypes()) {
				emitImplements(pkg, subFqn, impl);
			}
		}
	}

	/**
	 * 
	 * @param pkg
	 * @param subFqn
	 * @param impl
	 */
	private void emitImplements(String pkg, String subFqn, ClassOrInterfaceType impl) {
		String raw = GenerateClassDiagram.simpleName(impl.getNameWithScope());
		String target = idx.resolveTypeName(pkg, raw);
		if (target != null) {
			pw.println(idx.qPuml(subFqn) + IS_A_IMPLEMENTS + idx.qPuml(target));
		}
	}

	/**
	 * 
	 * @param pkg
	 * @param subFqn
	 * @param ext
	 */
	private void emitExtends(String pkg, String subFqn, ClassOrInterfaceType ext) {
		String raw = GenerateClassDiagram.simpleName(ext.getNameWithScope());
		String target = idx.resolveTypeName(pkg, raw);
		if (target != null) {
			pw.println(idx.qPuml(subFqn) + IS_A_EXTENDS + idx.qPuml(target));
		}
	}

	/**
	 * 
	 * @param ownerFqn
	 * @param targetFqn
	 * @param role
	 * @param stereotypes
	 */
	private void emitAssociation(String ownerFqn, String targetFqn, String role,
			String stereotypes) {
		pw.println(idx.qPuml(ownerFqn) + HAS_A + idx.qPuml(targetFqn) + " : " + role
				+ (stereotypes != null ? stereotypes : ""));
	}

	/**
	 * 
	 * @param pkg
	 * @param ownerFqn
	 * @param td
	 */
	private void emitAssociations(String pkg, String ownerFqn, TypeDeclaration<?> td) {
		if (td instanceof ClassOrInterfaceDeclaration || td instanceof EnumDeclaration) {
			for (FieldDeclaration fd : td.getFields()) {
				emitFieldAssociation(pkg, ownerFqn, fd);
			}
		} else if (td instanceof RecordDeclaration rd) {
			for (Parameter p : rd.getParameters()) {
				emitRecordParameterAssociation(pw, pkg, ownerFqn, p);
			}
		}
	}

	private void emitRecordParameterAssociation(PlantUMLWriter pw, String pkg, String ownerFqn, Parameter p) {
		String target = resolveAssocTarget(pkg, ownerFqn, p.getType());
		if (target != null) {
			String st = GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(p));

			emitAssociation(ownerFqn, target, p.getNameAsString(), st);
		}
	}

	private void emitFieldAssociation(String pkg, String ownerFqn, FieldDeclaration fd) {
		String st = GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(fd));

		for (VariableDeclarator vd : fd.getVariables()) {
			String target = resolveAssocTarget(pkg, ownerFqn, vd.getType());
			if (target != null) {
				emitAssociation(ownerFqn, target, vd.getNameAsString(), st);
			}
		}
	}

	/**
	 * 
	 * @param fqn
	 * @return
	 */
	private static String ownerFqnOf(String fqn) {
		int lastDot = fqn.lastIndexOf('.');
		int firstDollar = fqn.indexOf('$');
		if (lastDot < 0 && firstDollar < 0) {
			return null;
		}

		return fqn.substring(0, Math.max(lastDot, firstDollar));
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	private Type peelArrays(Type type) {
		Type t = type;
		while (t.isArrayType()) {
			t = t.asArrayType().getComponentType();
		}
		return t;
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	private String rawNameOf(Type type) {
		Type t = peelArrays(type);

		if (t.isClassOrInterfaceType()) {
			return t.asClassOrInterfaceType().getNameAsString(); // raw type: List, Optional, Foo
		}
		return t.asString(); // primitives etc.
	}

	/**
	 * 
	 * @param pkg
	 * @param ownerFqn
	 * @param type
	 * @return
	 */
	private String resolveAssocTarget(String pkg, String ownerFqn, Type type) {
		String raw = rawNameOf(type);
		String target = idx.resolveTypeName(pkg, raw);
		if (target == null || target.equals(ownerFqn)) {
			return null;
		}
		return target;
	}

}