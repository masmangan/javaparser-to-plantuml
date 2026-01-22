/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.internal;

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
import com.github.javaparser.resolution.UnsolvedSymbolException;
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
	private static final Logger logger = Logger.getLogger(CollectRelationshipsVisitor.class.getName());

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
		for (var td : idx.typesInIndexOrder()) {
			String pkg = DeclaredIndex.derivePkg(td);
			String fqn = DeclaredIndex.deriveFqnDollar(td);
			emitAssociations(pkg, fqn, td);
		}
	}

	/**
	 *
	 */
	private void emitInnerClassRelations() {
		for (String fqn : idx.fqnsInIndexOrder()) {
			emitInnerTypes(fqn);
		}
	}

	/**
	 *
	 * @param fqn
	 */
	private void emitInnerTypes(String fqn) {
		String ownerFqn = DeclaredIndex.ownerFqnOf(fqn);
		if (ownerFqn != null && idx.containsFqn(ownerFqn)) {
			pw.connectInnerType(ownerFqn, fqn);
		}
	}

	/**
	 *
	 */
	private void emitInheritanceRelations() {
		for (var td : idx.typesInIndexOrder()) {
			String pkg = DeclaredIndex.derivePkg(td);
			String fqn = DeclaredIndex.deriveFqnDollar(td);
			emitExtendsImplements(pkg, fqn, td);
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

		String raw = DeclaredIndex.simpleName(nameWithScope);
		String target = idx.resolveTypeName(pkg, raw);
		if (target != null) {
			pw.connectImplements(subFqn, target);
		} else {
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
		String raw = DeclaredIndex.simpleName(nameWithScope);
		String target = idx.resolveTypeName(pkg, raw);
		if (target != null) {
			pw.connectExtends(subFqn, target);
		} else {
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
		// log here!
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
		String target = idx.resolveAssocTarget(pkg, ownerFqn, p.getType());
		if (target != null) {
			String st = DeclaredIndex.stereotypesToString(p);

			emitAssociation(ownerFqn, target, p.getNameAsString(), st);
		}
	}

	/**
	 *
	 * @param pkg
	 * @param ownerFqn
	 * @param fd
	 */
	private void emitFieldAssociation(String pkg, String ownerFqn, FieldDeclaration fd) {
		String st = DeclaredIndex.renderStereotypes(DeclaredIndex.stereotypesOf(fd));
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
						/// 
						target = rt.asReferenceType().getQualifiedName();
					}
				} catch (UnsolvedSymbolException e) {
					logger.log(Level.INFO, () -> "UNSOLVED: " + e.getName());
				}
			} else {
				target = idx.resolveAssocTarget(pkg, ownerFqn, vd.getType());

			}
			//
			if (target != null) {
				emitAssociation(ownerFqn, target, vd.getNameAsString(), st);
			}
		}
	}

}