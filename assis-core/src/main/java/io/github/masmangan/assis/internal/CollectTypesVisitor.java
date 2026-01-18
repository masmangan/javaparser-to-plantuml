/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
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

import io.github.masmangan.assis.io.PlantUMLWriter;

/**
 * Emits PlantUML type blocks (classes, interfaces, records, enums, annotations)
 * for a set of declared Java types.
 *
 * <p>
 * This visitor writes <em>only</em> the structural contents that belong inside
 * a type block: members such as fields, constructors, methods, enum constants,
 * and record components. It deliberately does not emit
 * inheritance/implementation, nesting edges, or association edges; those are
 * emitted by {@link CollectRelationshipsVisitor}.
 *
 * <h2>Type naming</h2>
 * <p>
 * The type name to be emitted is provided as an ASSIS "PlantUML identifier" via
 * {@link DeclaredIndex#pumlName(String)}. Callers must provide the correct
 * fully-qualified name (FQN) and any nested-type convention (e.g., {@code $})
 * consistently with {@link DeclaredIndex}.
 *
 * <h2>Members vs. associations</h2>
 * <p>
 * ASSIS chooses to render either:
 * <ul>
 * <li>a field/record-component line inside the type block, <em>or</em></li>
 * <li>an association edge between types,</li>
 * </ul>
 * but not both. When a field (or record component) type resolves to another
 * declared type, the member line is suppressed here so that
 * {@link CollectRelationshipsVisitor} can render the association edge.
 *
 * <p>
 * This class performs no PlantUML validation; it relies on
 * {@link PlantUMLWriter} for block emission and on upstream logic for indexing
 * and name resolution.
 */
class CollectTypesVisitor {

	/**
	 *
	 */
	private static final String SPACE_STRING = " ";

	/**
	 *
	 */
	private static final String EMPTY_STRING = "";

	/**
	 *
	 */
	private static final String FINAL_MODIFIER = "<<final>>";

	/**
	 * Index of declared types and name-resolution helpers.
	 */
	private final DeclaredIndex idx;

	/**
	 * Package context used to resolve simple type names.
	 *
	 * <p>
	 * This is the declared package of the type currently being emitted (as read
	 * from the compilation unit). It is used as a hint for
	 * {@link DeclaredIndex#resolveTypeName(String, String)}.
	 */
	private final String pkg;

	/**
	 * Target writer used to emit PlantUML lines and blocks.
	 */
	private final PlantUMLWriter pw;

	/**
	 * Creates a visitor that emits PlantUML type blocks for a given package
	 * context.
	 *
	 * @param idx index used for declared type lookup and name resolution; must not
	 *            be {@code null}
	 * @param pkg package of the owner type being emitted; may be {@code null}
	 *            (treated as empty)
	 * @param pw  PlantUML writer to receive emitted lines; must not be {@code null}
	 * @throws NullPointerException if {@code idx} or {@code pw} is {@code null}
	 */
	CollectTypesVisitor(final DeclaredIndex idx, final String pkg, final PlantUMLWriter pw) {
		this.idx = idx;
		this.pkg = (pkg == null) ? EMPTY_STRING : pkg;
		this.pw = pw;
	}

	/**
	 * Emits a single type block for the given declaration.
	 *
	 * <p>
	 * This method selects the appropriate PlantUML keyword (class, interface,
	 * abstract class, record, enum, annotation) and then emits the members that
	 * belong inside the block.
	 *
	 * <p>
	 * The caller must provide {@code fqn} that matches the key used in
	 * {@link DeclaredIndex#byFqn}.
	 *
	 * @param fqn fully-qualified name for the type (ASSIS convention for nested
	 *            types applies); must not be {@code null}
	 * @param td  JavaParser type declaration; must not be {@code null}
	 * @throws NullPointerException if {@code fqn} or {@code td} is {@code null}
	 */
	void emitType(String fqn, TypeDeclaration<?> td) {
		String stereotypes = typeStereotypes(td);
		String vis = GenerateClassDiagram.visibility(td);

		pw.println();

		if (td instanceof ClassOrInterfaceDeclaration cid) {
			if (cid.isInterface()) {
				pw.beginInterface(fqn, vis, stereotypes);
				emitFields(fqn, cid.getFields());
				emitConstructors(cid.getConstructors());
				emitMethods(cid.getMethods());

				pw.endInterface(fqn);
			} else if (cid.isAbstract()) {
				pw.beginAbstractClass(fqn, vis, stereotypes);
				emitFields(fqn, cid.getFields());
				emitConstructors(cid.getConstructors());
				emitMethods(cid.getMethods());

				pw.endAbstractClass(fqn);
			} else if (cid.isFinal()) {
				pw.beginClass(fqn, vis, finalClassStereotypes(td));
				emitFields(fqn, cid.getFields());
				emitConstructors(cid.getConstructors());
				emitMethods(cid.getMethods());

				pw.endClass(fqn);
			} else {
				pw.beginClass(fqn, vis, stereotypes);
				emitFields(fqn, cid.getFields());
				emitConstructors(cid.getConstructors());
				emitMethods(cid.getMethods());

				pw.endClass(fqn);
			}

			return;
		}

		if (td instanceof RecordDeclaration rd) {
			pw.beginRecord(fqn, vis, stereotypes);

			emitRecordComponents(fqn, rd);

			var componentNames = rd.getParameters().stream().map(p -> p.getNameAsString())
					.collect(java.util.stream.Collectors.toSet());

			var extraFields = rd.getFields().stream().filter(
					fd -> fd.getVariables().stream().noneMatch(vd -> componentNames.contains(vd.getNameAsString())))
					.toList();

			emitFields(fqn, extraFields);

			emitConstructors(rd.getConstructors());
			emitMethods(rd.getMethods());

			pw.endRecord(fqn);
			return;
		}

		if (td instanceof EnumDeclaration ed) {
			pw.beginEnum(fqn, vis, stereotypes);
			emitEnumConstants(ed);
			emitFields(fqn, ed.getFields());
			emitConstructors(ed.getConstructors());
			emitMethods(ed.getMethods());
			pw.endEnum(fqn);
			return;
		}

		if (td instanceof AnnotationDeclaration ad) {
			pw.beginAnnotation(fqn, vis, stereotypes);
			emitAnnotationMembers(ad);
			pw.endAnnotation(fqn);
			return;
		}

		GenerateClassDiagram.logger.log(Level.WARNING, () -> "Unexpected type: " + td);
	}

	/**
	 *
	 * @param td
	 * @return
	 */
	private static String typeStereotypes(TypeDeclaration<?> td) {
		return GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(td));
	}

	/**
	 *
	 * @param td
	 * @return
	 */
	private static String finalClassStereotypes(TypeDeclaration<?> td) {
		String stereotypes = typeStereotypes(td);
		return FINAL_MODIFIER + (stereotypes.isBlank() ? EMPTY_STRING : SPACE_STRING + stereotypes.trim());
	}

	/**
	 * Emits annotation members as lines inside the record block.
	 *
	 * @param ad annotation declaration
	 */
	private void emitAnnotationMembers(AnnotationDeclaration ad) {
		ad.getMembers().stream().filter(AnnotationMemberDeclaration.class::isInstance)
				.map(AnnotationMemberDeclaration.class::cast)
				.sorted(Comparator.comparing(AnnotationMemberDeclaration::getNameAsString)).forEach(amd -> {
					String name = amd.getNameAsString();
					String type = amd.getType().asString();

					String defaultValue = amd.getDefaultValue().map(v -> " = " + v).orElse(EMPTY_STRING);

					pw.println(name + "() : " + type + defaultValue
							+ GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(amd)));
				});
	}

	/**
	 * Emits enum constants (one per line) inside the current enum block.
	 *
	 * @param ed enum declaration; must not be {@code null}
	 * @throws NullPointerException if {@code ed} is {@code null}
	 */
	private void emitEnumConstants(EnumDeclaration ed) {
		for (EnumConstantDeclaration c : ed.getEntries()) {
			pw.println(c.getNameAsString());
		}
	}

	/**
	 * Extracts a "raw" type name for association/name-resolution heuristics.
	 *
	 * <p>
	 * This method removes:
	 * <ul>
	 * <li>generic arguments (e.g., {@code List<Foo>} → {@code List})</li>
	 * <li>array brackets (e.g., {@code Foo[]} → {@code Foo})</li>
	 * </ul>
	 *
	 * <p>
	 * It is a string-based heuristic used to decide whether a member likely refers
	 * to another declared type.
	 *
	 * @param typeAsString JavaParser type string; must not be {@code null}
	 * @return simplified type name
	 * @throws NullPointerException if {@code typeAsString} is {@code null}
	 */
	private static String rawTypeName(String typeAsString) {
		return typeAsString.replaceAll("<[^>]*>", EMPTY_STRING).replace("[]", EMPTY_STRING).trim();
	}

	/**
	 * Emits record components as lines inside the record block.
	 *
	 * <p>
	 * Components whose type resolves to another declared type are suppressed so
	 * that an association edge can be emitted instead.
	 *
	 * @param ownerFqn fully-qualified name of the record being emitted; must not be
	 *                 {@code null}
	 * @param rd       record declaration; must not be {@code null}
	 * @throws NullPointerException if {@code ownerFqn} or {@code rd} is
	 *                              {@code null}
	 */
	private void emitRecordComponents(String ownerFqn, RecordDeclaration rd) {
		for (Parameter p : rd.getParameters()) {
			String raw = rawTypeName(p.getType().asString());
			String target = idx.resolveTypeName(pkg, raw);

			boolean becomesAssociation = target != null && !target.equals(ownerFqn);
			if (becomesAssociation) {
				continue;
			}

			pw.println(p.getNameAsString() + " : " + p.getType().asString()
					+ GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(p)));
		}
	}

	/**
	 * Emits field declarations as member lines inside the current type block.
	 *
	 * <p>
	 * Field declarations are sorted by the first variable name for deterministic
	 * output. Each variable declaration is handled independently.
	 *
	 * <p>
	 * Fields whose type resolves to another declared type are suppressed here so
	 * that associations can be rendered separately.
	 *
	 * @param ownerFqn fully-qualified name of the owning type; must not be
	 *                 {@code null}
	 * @param fields   field declarations; must not be {@code null}
	 * @throws NullPointerException if {@code ownerFqn} or {@code fields} is
	 *                              {@code null}
	 */
	private void emitFields(String ownerFqn, List<FieldDeclaration> fields) {

		List<FieldDeclaration> sorted = new ArrayList<>(fields);
		sorted.sort((a, b) -> {
			String an = a.getVariables().isEmpty() ? EMPTY_STRING : a.getVariable(0).getNameAsString();
			String bn = b.getVariables().isEmpty() ? EMPTY_STRING : b.getVariable(0).getNameAsString();
			return an.compareTo(bn);
		});

		for (FieldDeclaration fd : sorted) {
			for (VariableDeclarator vd : fd.getVariables()) {
				emitVariableDeclarator(ownerFqn, fd, vd);
			}
		}
	}

	/**
	 * Emits a single variable declarator as a PlantUML field line.
	 *
	 * <p>
	 * If the declarator's type resolves to another declared type, the field line is
	 * omitted (association will be emitted elsewhere).
	 *
	 * @param ownerFqn fully-qualified name of the owning type; must not be
	 *                 {@code null}
	 * @param fd       field declaration; must not be {@code null}
	 * @param vd       variable declarator; must not be {@code null}
	 * @throws NullPointerException if {@code ownerFqn}, {@code fd}, or {@code vd}
	 *                              is {@code null}
	 */
	private void emitVariableDeclarator(String ownerFqn, FieldDeclaration fd, VariableDeclarator vd) {
		String assoc = assocTypeFrom(ownerFqn, vd);
		if (assoc != null) {
			return;
		}

		String name = vd.getNameAsString();
		String type = vd.getType().asString();
		String staticPrefix = fd.isStatic() ? "{static}" : EMPTY_STRING;
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
		String modBlock = mods.isEmpty() ? EMPTY_STRING : " {" + String.join(", ", mods) + "}";

		pw.println(vis + SPACE_STRING + staticPrefix + SPACE_STRING + name + " : " + type + modBlock
				+ GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(fd)));
	}

	/**
	 * Emits constructors as PlantUML operation lines.
	 *
	 * <p>
	 * Constructors are sorted by a stable textual signature for deterministic
	 * output.
	 *
	 * @param ctors constructors to emit; must not be {@code null}
	 * @throws NullPointerException if {@code ctors} is {@code null}
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
	 * Returns PlantUML method flags derived from Java modifiers.
	 *
	 * <p>
	 * Current flags:
	 * <ul>
	 * <li>{@code {static}}</li>
	 * <li>{@code {abstract}}</li>
	 * <li>{@code {final}}</li>
	 * </ul>
	 *
	 * @param method method declaration; must not be {@code null}
	 * @return flags string (possibly empty)
	 * @throws NullPointerException if {@code method} is {@code null}
	 */
	private static String getFlags(MethodDeclaration method) {
		String flags = "";

		if (method.isStatic()) {
			flags += " {static}";
		}

		if (method.isAbstract()) {
			flags += " {abstract}";
		}

		if (method.isFinal()) {
			flags += " {final}";
		}
		return flags;
	}

	/**
	 * Emits methods as PlantUML operation lines.
	 *
	 * <p>
	 * Methods are sorted by a stable textual signature for deterministic output.
	 * Parameter annotations are rendered as stereotypes preceding each parameter.
	 *
	 * @param methods methods to emit; must not be {@code null}
	 * @throws NullPointerException if {@code methods} is {@code null}
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
				return (anns + SPACE_STRING + p.getNameAsString() + " : " + p.getType().asString()).trim();
			}).collect(Collectors.joining(", "));
			String flags = getFlags(m);
			String vis = GenerateClassDiagram.visibility(m);

			pw.println(vis + SPACE_STRING + name + "(" + params + ") : " + returnType + flags
					+ GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(m)));
		}
	}

	/**
	 * Determines whether a field should become an association instead of a member
	 * line.
	 *
	 * <p>
	 * If the variable declarator's (raw) type resolves to another declared type and
	 * is not a self-reference, the association target FQN is returned.
	 *
	 * @param ownerFqn fully-qualified name of the owning type; must not be
	 *                 {@code null}
	 * @param vd       variable declarator; must not be {@code null}
	 * @return resolved target FQN when this field should become an association;
	 *         otherwise {@code null}
	 * @throws NullPointerException if {@code ownerFqn} or {@code vd} is
	 *                              {@code null}
	 */
	private String assocTypeFrom(String ownerFqn, VariableDeclarator vd) {
		String raw = rawTypeName(vd.getType().asString());
		String resolved = idx.resolveTypeName(pkg, raw);
		if ((resolved == null) || resolved.equals(ownerFqn)) {
			return null;
		}
		return resolved;
	}
}