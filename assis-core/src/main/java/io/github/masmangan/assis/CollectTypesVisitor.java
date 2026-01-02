/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;

/**
 * 
 */
class CollectTypesVisitor {

	/**
	 * 
	 */
	private final DeclaredIndex idx;

	/**
	 * 
	 */
	private final String pkg;
	
	/**
	 * 
	 */
	private final PlantUMLWriter pw;

	/**
	 * 
	 * @param idx
	 * @param pkg
	 */
	CollectTypesVisitor(final DeclaredIndex idx, final String pkg, final PlantUMLWriter pw) {
		this.idx = idx;
		this.pkg = (pkg == null) ? "" : pkg;
		this.pw = pw;
	}	

	/**
	 * 
	 * @param fqn
	 * @param td
	 */
	void emitType(String fqn, TypeDeclaration<?> td) {
		String stereotypes = GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(td));
		String pumlName = idx.pumlName(fqn);

		pw.println();

		if (td instanceof ClassOrInterfaceDeclaration cid) {
			if (cid.isInterface()) {
				pw.beginInterface(pumlName, stereotypes);
			} else if (cid.isAbstract()) {
				pw.beginAbstractClass(pumlName, stereotypes);
			} else if (cid.isFinal()) {
				pw.beginClass(pumlName, "<<final>>" + (stereotypes.isBlank() ? "" : " " + stereotypes.trim()));
			} else {
				pw.beginClass(pumlName, stereotypes);
			}

			emitFields(fqn, cid.getFields());
			emitConstructors(cid.getConstructors());
			emitMethods(cid.getMethods());

			pw.endType();
			return;
		}

		if (td instanceof RecordDeclaration rd) {
			pw.beginRecord(pumlName, stereotypes);

			emitRecordComponents(fqn, rd);

			var componentNames = rd.getParameters().stream().map(p -> p.getNameAsString())
					.collect(java.util.stream.Collectors.toSet());

			var extraFields = rd.getFields().stream().filter(
					fd -> fd.getVariables().stream().noneMatch(vd -> componentNames.contains(vd.getNameAsString())))
					.toList();

			emitFields(fqn, extraFields);

			emitConstructors(rd.getConstructors());
			emitMethods(rd.getMethods());

			pw.endType();
			return;
		}

		if (td instanceof EnumDeclaration ed) {
			pw.beginEnum(pumlName, stereotypes);
			emitEnumConstants(ed);
			emitFields(fqn, ed.getFields());
			emitConstructors(ed.getConstructors());
			emitMethods(ed.getMethods());
			pw.endType();
			return;
		}

		if (td instanceof AnnotationDeclaration) {
			pw.beginAnnotation(pumlName, stereotypes);
			pw.endType();
			return;
		}

		GenerateClassDiagram.logger.log(Level.WARNING, () -> "Unexpected type: " + td);
	}

	/**
	 * 
	 * @param ed
	 */
	private void emitEnumConstants(EnumDeclaration ed) {
		for (EnumConstantDeclaration c : ed.getEntries()) {
			pw.println(c.getNameAsString());
		}
	}

	/**
	 * 
	 * @param ownerFqn
	 * @param rd
	 */
	private void emitRecordComponents(String ownerFqn, RecordDeclaration rd) {
		for (Parameter p : rd.getParameters()) {
			List<String> ss = GenerateClassDiagram.stereotypesOf(p);
			String raw = p.getType().asString().replaceAll("<.*>", "").replace("[]", "").trim();
			String resolved = idx.resolveTypeName(pkg, raw);

			if (resolved != null && !resolved.equals(ownerFqn)) {
				continue;
			}
			pw.println(
					p.getNameAsString() + " : " + p.getType().asString() + GenerateClassDiagram.renderStereotypes(ss));
		}
	}

	/**
	 * 
	 * @param ownerFqn
	 * @param fields
	 */
	private void emitFields(String ownerFqn, List<FieldDeclaration> fields) {

		List<FieldDeclaration> sorted = new ArrayList<>(fields);
		sorted.sort((a, b) -> {
			String an = a.getVariables().isEmpty() ? "" : a.getVariable(0).getNameAsString();
			String bn = b.getVariables().isEmpty() ? "" : b.getVariable(0).getNameAsString();
			return an.compareTo(bn);
		});

		for (FieldDeclaration fd : sorted) {
			for (VariableDeclarator vd : fd.getVariables()) {
				emitVariableDeclarator(ownerFqn, fd, vd);
			}
		}
	}

	/**
	 * 
	 * @param ownerFqn
	 * @param fd
	 * @param vd
	 */
	private void emitVariableDeclarator(String ownerFqn, FieldDeclaration fd,
			VariableDeclarator vd) {
		String assoc = assocTypeFrom(ownerFqn, vd);
		if (assoc != null) {
			return;
		}

		String name = vd.getNameAsString();
		String type = vd.getType().asString();
		String staticPrefix = fd.isStatic() ? "{static} " : "";
		String vis = GenerateClassDiagram.visibility(fd);

		List<String> mods = new ArrayList<>();
		if (fd.isFinal()) {
			mods.add("final");
		}
		if (fd.isTransient()) {
			mods.add("transient");
		}
		if (fd.isVolatile()) {
			mods.add("volatile");
		}
		String modBlock = mods.isEmpty() ? "" : " {" + String.join(", ", mods) + "}";

		pw.println(vis + " " + staticPrefix + name + " : " + type + modBlock
				+ GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(fd)));
	}

	/**
	 * 
	 * @param ctors
	 */
	private void emitConstructors(List<ConstructorDeclaration> ctors) {
		List<ConstructorDeclaration> sorted = new ArrayList<>(ctors);
		sorted.sort((a, b) -> a.getDeclarationAsString(false, false, false)
				.compareTo(b.getDeclarationAsString(false, false, false)));

		for (ConstructorDeclaration c : sorted) {
			String name = c.getNameAsString();
			String params = c.getParameters().stream().map(p -> p.getNameAsString() + " : " + p.getType().asString())
					.collect(Collectors.joining(", "));
			String vis = GenerateClassDiagram.visibility(c);
			pw.println(vis + " <<create>> " + name + "(" + params + ")"
					+ GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(c)));
		}
	}

	/**
	 * 
	 * @param methods
	 */
	private void emitMethods(List<MethodDeclaration> methods) {
		List<MethodDeclaration> sorted = new ArrayList<>(methods);
		sorted.sort((a, b) -> a.getDeclarationAsString(false, false, false)
				.compareTo(b.getDeclarationAsString(false, false, false)));

		for (MethodDeclaration m : sorted) {
			String returnType = m.getType().asString();
			String name = m.getNameAsString();
			String params = m.getParameters().stream().map(p -> {
				String anns = GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(p));
				return (anns + " " + p.getNameAsString() + " : " + p.getType().asString()).trim();
			}).collect(Collectors.joining(", "));
			String flags = GenerateClassDiagram.getFlags(m);
			String vis = GenerateClassDiagram.visibility(m);
			pw.println(vis + " " + name + "(" + params + ") : " + returnType + flags
					+ GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(m)));
		}
	}

	/**
	 * 
	 * @param ownerFqn
	 * @param vd
	 * @return
	 */
	private String assocTypeFrom(String ownerFqn, VariableDeclarator vd) {
		String raw = vd.getType().asString().replaceAll("<.*>", "").replace("[]", "").trim();
		String resolved = idx.resolveTypeName(pkg, raw);
		if (resolved == null) {
			return null;
		}
		if (resolved.equals(ownerFqn)) {
			return null;
		}
		return resolved;
	}
}