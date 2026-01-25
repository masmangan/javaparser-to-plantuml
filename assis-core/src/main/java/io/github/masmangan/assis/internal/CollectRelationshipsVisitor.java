/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.internal;

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

	private static final String ASSIS_CHERRY_PICK_GHOST = "@assis:cherry-pick ghost";

	private static final Logger logger = Logger.getLogger(CollectRelationshipsVisitor.class.getName());

	private final DeclaredIndex idx;

	private final PlantUMLWriter pw;

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

	private void emitAssociationRelations() {
		for (var td : idx.typesInIndexOrder()) {
			String pkg = DeclaredIndex.derivePkg(td);
			String fqn = DeclaredIndex.deriveFqnDollar(td);
			emitAssociations(pkg, fqn, td);
		}
	}

	private void emitInnerClassRelations() {
		for (String fqn : idx.fqnsInIndexOrder()) {
			emitInnerTypes(fqn);
		}
	}

	private void emitInnerTypes(String fqn) {
		String ownerFqn = DeclaredIndex.ownerFqnOf(fqn);
		if (ownerFqn != null && idx.containsFqn(ownerFqn)) {
			pw.connectInnerType(ownerFqn, fqn);
		}
	}

	private void emitInheritanceRelations() {
		for (var td : idx.typesInIndexOrder()) {
			emitExtendsImplements(td);
		}
	}

	private void emitExtendsImplements(TypeDeclaration<?> td) {
		if (td instanceof ClassOrInterfaceDeclaration cid) {
			for (ClassOrInterfaceType ext : cid.getExtendedTypes()) {
				emitExtends(cid, ext);
			}

			for (ClassOrInterfaceType impl : cid.getImplementedTypes()) {
				emitImplements(cid, impl);
			}
		} else if (td instanceof EnumDeclaration ed) {
			for (ClassOrInterfaceType impl : ed.getImplementedTypes()) {
				emitImplements(ed, impl);
			}
		} else if (td instanceof RecordDeclaration rd) {
			for (ClassOrInterfaceType impl : rd.getImplementedTypes()) {
				emitImplements(rd, impl);
			}
		}
	}

	private void emitImplements(TypeDeclaration<?> td, ClassOrInterfaceType impl) {
		String subFqn = DeclaredIndex.deriveFqnDollar(td);

		Optional<TypeRef> tr = idx.resolveTarget(impl, td);
		logger.log(Level.INFO, () -> "Trying to resolve implements type: " + tr);

		if (tr.isPresent()) {
			TypeRef ref = tr.get();

			if (ref instanceof DeclaredTypeRef dtr) {
				String target = DeclaredIndex.deriveFqnDollar(dtr.declaration());
				pw.connectImplements(subFqn, target);
				return;
			}

			if (ref instanceof ExternalTypeRef etr) {
				pw.connectImplements(subFqn, etr.fqn());
				return;
			}

			pw.withBeforeTag(ASSIS_CHERRY_PICK_GHOST, () -> pw.connectImplements(subFqn, ref.displayName()));
			return;
		}

		pw.withBeforeTag(ASSIS_CHERRY_PICK_GHOST, () -> pw.connectImplements(subFqn, impl.getNameWithScope()));
	}

	private void emitExtends(ClassOrInterfaceDeclaration cid, ClassOrInterfaceType ext) {
		String subFqn = DeclaredIndex.deriveFqnDollar(cid);

		Optional<TypeRef> tr = idx.resolveTarget(ext, cid);
		logger.log(Level.INFO, () -> "Trying to resolve extends type: " + tr);

		if (tr.isPresent()) {
			TypeRef ref = tr.get();
			logger.log(Level.INFO, () -> "IsPresent: " + ref);

			if (ref instanceof DeclaredTypeRef dtr) {
				logger.log(Level.INFO, () -> "DeclaredTypeRef: " + dtr);

				String target = DeclaredIndex.deriveFqnDollar(dtr.declaration());
				pw.connectExtends(subFqn, target);
				return;
			}

			if (ref instanceof ExternalTypeRef etr) {
				logger.log(Level.INFO, () -> "ExternalTypeRef: " + etr);
				pw.connectExtends(subFqn, etr.fqn());
				return;
			}
			logger.log(Level.INFO, () -> "Unresolved: " + ref.displayName());

			// Unresolved (or other TypeRef): ghost
			pw.withBeforeTag(ASSIS_CHERRY_PICK_GHOST, () -> pw.connectExtends(subFqn, ref.displayName()));
			return;
		}
		logger.log(Level.INFO, () -> "Fallback: " + ext.getNameWithScope());

		// Fallback (should be rare for extends)
		pw.withBeforeTag(ASSIS_CHERRY_PICK_GHOST, () -> pw.connectExtends(subFqn, ext.getNameWithScope()));
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
		// add a list of of associations here to avoid deps sobreposition later!
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

	private void emitRecordParameterAssociation(String pkg, String ownerFqn, Parameter p) {
		// FIXME
		String target = idx.resolveAssocTarget(pkg, ownerFqn, p.getType());

		if (target != null) {
			String st = DeclaredIndex.stereotypesToString(p);
			emitAssociation(ownerFqn, target, p.getNameAsString(), st);
		}
	}

	private void emitFieldAssociation(String pkg, String ownerFqn, FieldDeclaration fd) {
		String st = DeclaredIndex.renderStereotypes(DeclaredIndex.stereotypesOf(fd));

		for (VariableDeclarator vd : fd.getVariables()) {
			String target = null;

			// Reuse the central resolver (SymbolSolver + Index authority)
			Optional<TypeRef> tr = idx.resolveTarget(vd.getType(), vd); // usageSite can be vd or fd
			logger.log(Level.INFO, () -> "Trying to resolve type: " + tr);

			if (tr.isPresent()) {
				TypeRef ref = tr.get();
				logger.log(Level.INFO, () -> "Type is present: " + ref);

				if (ref instanceof DeclaredTypeRef dtr) {
					// Prefer canonical $-fqn derived from AST declaration
					String fqnDollar = DeclaredIndex.deriveFqnDollar(dtr.declaration());
					target = fqnDollar;
				} else if (ref instanceof ExternalTypeRef etr) {
					logger.log(Level.INFO, () -> "External: " + etr);

					target = etr.fqn(); // or etr.displayName()/name() depending on your API
					// Wait, no association for externals, field emitted earlier!
					break; // or return or continue
				} else {
					logger.log(Level.INFO, () -> "Unresolved: " + ref);

					// UnresolvedTypeRef or others: keep it ghosted (or ignore) depending on your
					// policy
					target = ref.displayName();
				}
			} else {
				// Non-reference types might come here (primitives, etc.)
				// If you still want previous behavior, keep your old fallback:
				target = idx.resolveAssocTarget(pkg, ownerFqn, vd.getType());
			}
			// idx.resolveAssocTarget() decides if we emit field or association!
			//
			if (target != null) {
				emitAssociation(ownerFqn, target, vd.getNameAsString(), st);
			}
		}
	}

}