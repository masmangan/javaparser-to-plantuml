/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

import io.github.masmangan.assis.io.PlantUMLWriter;

/**
 * Emits PlantUML relationship edges for all declared types in a
 * {@link DeclaredIndex}.
 *
 * <p>
 * This visitor is responsible only for relationships (edges). Type declarations
 * (nodes) are emitted by {@link CollectTypesVisitor}.
 *
 * <h2>Relationships</h2>
 * <ul>
 * <li><b>Inheritance</b>: {@code "Sub" --|> "Super"}</li>
 * <li><b>Implementation</b>: {@code "Sub" ..|> "Interface"}</li>
 * <li><b>Nesting</b> (inner/nested types):
 * {@code "Outer" +-- "Outer$Inner"}</li>
 * <li><b>Association</b> (has-a):
 * {@code "Owner" ---> "Target" : role <<Stereo>>}</li>
 * </ul>
 *
 * <h2>No duplicate rendering</h2>
 * <p>
 * ASSIS emits a field/record component either as an attribute/component (in
 * {@link CollectTypesVisitor}) or as an association here, never both. The
 * decision is made by resolving the declared type name using
 * {@link DeclaredIndex#resolveTypeName(String, String)}.
 */
class CollectRelationshipsVisitor {

	/**
	 * Logger used by the generator to report progress and parse/write issues.
	 */
	static final Logger logger = Logger.getLogger(CollectRelationshipsVisitor.class.getName());

	/**
	 *
	 */
	private static final char CHAR_INNER_TYPE_SEPARATOR = '$';

	/**
	 *
	 */
	private static final char CHAR_PACKAGE_SEPARATOR = '.';

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
	 * Emits all relationship edges in three passes:
	 * <ol>
	 * <li>extends/implements edges</li>
	 * <li>nesting edges for inner/nested types</li>
	 * <li>association edges for fields and record components</li>
	 * </ol>
	 *
	 * <p>
	 * Ordering is chosen to keep the output stable and readable.
	 */
	void emitAll() {
		emitInheritanceRelations();

		emitInnerClassRelations();

		emitAssociationRelations();
	}

	/**
	 *
	 */
	private void emitAssociationRelations() {
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
	 */
	private void emitInnerClassRelations() {
		for (String fqn : idx.byFqn.keySet()) {
			emitInnerTypes(fqn);
		}
	}

	/**
	 *
	 * @param fqn
	 */
	private void emitInnerTypes(String fqn) {
		String ownerFqn = ownerFqnOf(fqn);
		if (ownerFqn != null && idx.byFqn.containsKey(ownerFqn)) {
			pw.connectInnerType(ownerFqn, fqn);
		}
	}

	/**
	 *
	 */
	private void emitInheritanceRelations() {
		for (var entry : idx.fqnsByPkg.entrySet()) {
			String pkg = entry.getKey();
			for (String fqn : entry.getValue()) {
				TypeDeclaration<?> td = idx.byFqn.get(fqn);
				emitExtendsImplements(pkg, fqn, td);
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
		String nameWithScope = impl.getNameWithScope();

		String raw = GenerateClassDiagram.simpleName(nameWithScope);
		String target = idx.resolveTypeName(pkg, raw);
		if (target != null) {
			// emit active implements to a know type
			pw.connectImplements(subFqn, target);
		} else {
			// emit commented out, tagged implements for user review
			pw.withBeforeTag("@assis:cherry-pick ghost", () -> pw.connectImplements(subFqn, nameWithScope));
		}
	}

	/**
	 *
	 * @param pkg
	 * @param subFqn
	 * @param ext
	 */
	private void emitExtends(String pkg, String subFqn, ClassOrInterfaceType ext) {
		String nameWithScope = ext.getNameWithScope();
		String raw = GenerateClassDiagram.simpleName(nameWithScope);
		String target = idx.resolveTypeName(pkg, raw);
		if (target != null) {
			// emit active extends to a know type
			pw.connectExtends(subFqn, target);
		} else {
			// emit commented out, tagged extends for user review
			pw.withBeforeTag("@assis:cherry-pick ghost", () -> pw.connectExtends(subFqn, nameWithScope));
		}
	}

	/**
	 * Emits an association edge in PlantUML "has-a" form:
	 * {@code "Owner" --> "Target" : role <<Stereo>>}.
	 *
	 * @param ownerFqn    owner type FQN
	 * @param targetFqn   target type FQN
	 * @param role        role label (field name or record component name)
	 * @param stereotypes already rendered stereotype block (may be {@code null})
	 */
	private void emitAssociation(String ownerFqn, String targetFqn, String role, String stereotypes) {
		pw.connectAssociation(ownerFqn, targetFqn, role, stereotypes);
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
				emitRecordParameterAssociation(pkg, ownerFqn, p);
			}
		}
	}

	/**
	 *
	 * @param pkg
	 * @param ownerFqn
	 * @param p
	 */
	private void emitRecordParameterAssociation(String pkg, String ownerFqn, Parameter p) {
		String target = resolveAssocTarget(pkg, ownerFqn, p.getType());
		if (target != null) {
			String st = stereotypesToString(p);

			emitAssociation(ownerFqn, target, p.getNameAsString(), st);
		}
	}

	/**
	 *
	 * @param p
	 * @return
	 */
	private String stereotypesToString(Parameter p) {
		return GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(p));
	}

	/**
	 *
	 * @param pkg
	 * @param ownerFqn
	 * @param fd
	 */
	private void emitFieldAssociation(String pkg, String ownerFqn, FieldDeclaration fd) {
		String st = GenerateClassDiagram.renderStereotypes(GenerateClassDiagram.stereotypesOf(fd));
		String target = null;

		for (VariableDeclarator vd : fd.getVariables()) {
			// Symbol solver
			if (vd.getType().isClassOrInterfaceType()) {
				ClassOrInterfaceType cit = vd.getType().asClassOrInterfaceType();
				logger.log(Level.INFO, () -> "Trying to resolve type: " + cit);
				try {
					ResolvedType rt = cit.resolve();
					logger.log(Level.INFO, () -> "ResolvedType.describe(): " + rt.describe());
					if (rt.isReferenceType()) {
						logger.log(Level.INFO, () -> "QualifiedName: " + rt.asReferenceType().getQualifiedName());
						// FIXME: must get dot dollar name
						target = rt.asReferenceType().getQualifiedName();
					}
				} catch (UnsolvedSymbolException e) {
					logger.log(Level.INFO, () -> "UNSOLVED: " + e.getName());
				}
			} else {
				target = resolveAssocTarget(pkg, ownerFqn, vd.getType());

			}
			//
			if (target != null) {
				emitAssociation(ownerFqn, target, vd.getNameAsString(), st);
			}
		}
	}

	/**
	 * Returns the immediate lexical owner FQN for a nested type name.
	 *
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>{@code "p.Outer$Inner"} -> {@code "p.Outer"}</li>
	 * <li>{@code "p.A$B$C"} -> {@code "p.A$B"}</li>
	 * </ul>
	 *
	 * <p>
	 * This method uses the last {@code '$'} to support multi-level nesting.
	 *
	 * @param fqn fully qualified name (may include {@code '$'} for nesting)
	 * @return owner FQN, or {@code null} when the type is top-level
	 */
	private static String ownerFqnOf(String fqn) {
		int lastDot = fqn.lastIndexOf(CHAR_PACKAGE_SEPARATOR);
		int lastDollar = fqn.lastIndexOf(CHAR_INNER_TYPE_SEPARATOR);
		if (lastDot < 0 && lastDollar < 0) {
			return null;
		}
		return fqn.substring(0, Math.max(lastDot, lastDollar));
	}

	/**
	 *
	 * @param type
	 * @return
	 */
	private static Type peelArrays(Type type) {
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
			// return t.asClassOrInterfaceType().getNameAsString();
			return t.asClassOrInterfaceType().getNameWithScope();
		}
		return t.asString();
	}

	private Optional<String> debugResolve(Type type) {
		try {
			if (!(type instanceof ClassOrInterfaceType cit)) {
				logger.fine(() -> "Skip non-reference type: " + type);
				return Optional.empty();
			}

			ResolvedType rt = cit.resolve();
			logger.info(() -> "Resolved '" + cit + "' -> " + rt.describe());

			if (!rt.isReferenceType()) {
				logger.fine(() -> "Not a reference type: " + rt);
				return Optional.empty();
			}

			ResolvedReferenceType rrt = rt.asReferenceType();
			String qname = rrt.getQualifiedName(); // dot form
			logger.info(() -> "Qualified name: " + qname);

			return Optional.of(qname);

		} catch (UnsolvedSymbolException e) {
			logger.info(() -> "Unsolved symbol: " + e.getName());
			return Optional.empty();

		} catch (IllegalStateException e) {
			logger.info(() -> "SymbolSolver not configured at node");
			return Optional.empty();

		} catch (Exception e) {
			logger.log(Level.WARNING, "Unexpected resolution error", e);
			return Optional.empty();
		}
	}

	/**
	 *
	 * @param pkg
	 * @param ownerFqn
	 * @param type
	 * @return
	 */
	private String resolveAssocTarget(String pkg, String ownerFqn, Type type) {
		// FIXME: solving type
		logger.log(Level.INFO, () -> "Type: " + type.toString());
		Optional<String> op = debugResolve(type);

		logger.log(Level.INFO, () -> "Name: " + op.orElse("NOPE"));

		//
		String raw = rawNameOf(type);
		String target = idx.resolveTypeName(pkg, raw);
		if (target == null || target.equals(ownerFqn)) {
			return null;
		}
		return target;
	}

}