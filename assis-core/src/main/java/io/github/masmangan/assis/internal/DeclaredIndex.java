/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithAccessModifiers;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * Index of declared types (top-level and nested).
 */
public class DeclaredIndex {

	private static final Logger logger = Logger.getLogger(DeclaredIndex.class.getName());

	private static final String EMPTY_STRING = "";

	private static final String PACKAGE_SEPARATOR = ".";

	private static final char CHAR_INNER_TYPE_SEPARATOR = '$';

	private static final char CHAR_PACKAGE_SEPARATOR = '.';

	/**
	 * Type key → declaration
	 */
	private final Map<TypeKey, TypeDeclaration<?>> byKey = new LinkedHashMap<>();

	/**
	 * Type key → declared package (from CompilationUnit)
	 */
	private final Map<TypeKey, String> pkgByKey = new LinkedHashMap<>();

	/**
	 * package → list of type keys
	 */
	private Map<String, List<TypeKey>> keysByPkg = new LinkedHashMap<>();

	/**
	 * simple name → unique type key (only when unambiguous)
	 */
	private final Map<String, TypeKey> uniqueBySimple = new LinkedHashMap<>();

	private static TypeKey key(String fqn) {
		return new TypeKey(fqn);
	}

	private static String text(TypeKey k) {
		return k.text();
	}

	/**
	 * Populates index with declared types from compilation units.
	 *
	 * @param units
	 */
	public void fill(final List<CompilationUnit> units) {

		for (CompilationUnit unit : units) {
			for (TypeDeclaration<?> td : unit.getTypes()) {
				collectTypeRecursive(unit, td, null, PACKAGE_SEPARATOR);
			}
		}

		for (Map.Entry<TypeKey, String> e : pkgByKey.entrySet()) {
			TypeKey key = e.getKey();
			String pkg = e.getValue();
			keysByPkg.computeIfAbsent(pkg, __ -> new ArrayList<>()).add(key);
		}

		keysByPkg = sortPackagesByNameFqn(keysByPkg);
		keysByPkg.values().forEach(list -> list.sort(Comparator.comparing(TypeKey::text)));

		Map<String, TypeKey> seen = new LinkedHashMap<>();
		Set<String> ambiguous = new LinkedHashSet<>();

		for (TypeKey k : byKey.keySet()) {
			String simple = DeclaredIndex.simpleName(text(k));
			TypeKey prior = seen.putIfAbsent(simple, k);
			if (prior != null) {
				ambiguous.add(simple);
			}
		}

		for (var e : seen.entrySet()) {
			if (!ambiguous.contains(e.getKey())) {
				uniqueBySimple.put(e.getKey(), e.getValue());
			}
		}

	}

	private void collectTypeRecursive(CompilationUnit unit, TypeDeclaration<?> td, String ownerFqn, String separator) {
		String name = td.getNameAsString();
		String fqn;
		String pkg = unit.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");

		if (ownerFqn == null) {
			fqn = pkg.isEmpty() ? name : pkg + separator + name;
		} else {
			fqn = ownerFqn + separator + name;
		}

		TypeKey k = key(fqn);

		if (byKey.containsKey(k)) {
			logger.log(Level.WARNING, () -> "Attempt to redefine " + fqn);
			logger.log(Level.WARNING, unit::toString);
			logger.log(Level.WARNING, td::toString);
			logger.log(Level.WARNING, () -> "Keeping first definition.");
			return;
		}

		byKey.put(k, td);
		pkgByKey.put(k, pkg);

		if (td instanceof ClassOrInterfaceDeclaration cid) {
			cid.getMembers().forEach(m -> {
				if (m instanceof TypeDeclaration<?> nested) {
					collectTypeRecursive(unit, nested, fqn, "$");
				}
			});
		} else if (td instanceof EnumDeclaration ed) {
			ed.getMembers().forEach(m -> {
				if (m instanceof TypeDeclaration<?> nested) {
					collectTypeRecursive(unit, nested, fqn, "$");
				}
			});
		}
	}

	public Iterable<String> fqnsInIndexOrder() {
		return Collections.unmodifiableList(byKey.keySet().stream().map(TypeKey::text).toList());
	}

	public boolean containsFqn(String fqn) {
		return byKey.containsKey(key(fqn));
	}

	public TypeDeclaration<?> getByFqn(String fqn) {
		return byKey.get(key(fqn));
	}

	/**
	 * Deterministic: package order, then FQN order inside each package. Read-only,
	 * no lambdas needed at call sites (Writer + IOException friendly).
	 */
	public Iterable<TypeDeclaration<?>> typesInIndexOrder() {
		List<TypeDeclaration<?>> out = new ArrayList<>();
		for (var entry : keysByPkg.entrySet()) {
			for (TypeKey key : entry.getValue()) {
				TypeDeclaration<?> td = byKey.get(key);
				if (td != null) {
					out.add(td);
				}
			}
		}
		return Collections.unmodifiableList(out);
	}

	/** Deterministic package iteration order (read-only). */
	public Iterable<String> packagesInIndexOrder() {
		return Collections.unmodifiableSet(keysByPkg.keySet());
	}

	/** Deterministic type order inside the package (read-only). */
	public Iterable<TypeDeclaration<?>> typesInPackageOrder(String pkg) {
		List<TypeKey> keys = keysByPkg.get(pkg);
		if (keys == null || keys.isEmpty()) {
			return List.of();
		}

		List<TypeDeclaration<?>> out = new ArrayList<>(keys.size());
		for (TypeKey key : keys) {
			TypeDeclaration<?> td = byKey.get(key);
			if (td != null) {
				out.add(td);
			}
		}
		return Collections.unmodifiableList(out);
	}

	public Optional<TypeRef> resolveTarget(Type typeNode, Node usageSite) {
		logger.log(Level.INFO, () -> "Resolving target: " + typeNode);

		// If we want arrays like Foo[] to count as dependency on Foo:
		if (typeNode instanceof ArrayType at) {
			return resolveTarget(at.getComponentType(), usageSite);
		}

		if (typeNode instanceof TypeParameter tp) {
			// resolve its first bound if present
			if (!tp.getTypeBound().isEmpty()) {
				return resolveTarget(tp.getTypeBound().get(0), usageSite);
			}
			return Optional.empty();
		}
		if (typeNode instanceof WildcardType wt) {
			return wt.getExtendedType().map(t -> resolveTarget(t, usageSite)).orElse(Optional.empty());
		}
		if (!(typeNode instanceof ClassOrInterfaceType cit)) {
			return Optional.empty();
		}

		logger.log(Level.INFO, () -> "Trying to resolve type: " + cit);

		Optional<TypeRef> solved = tryResolveWithSolver(cit);
		if (solved.isPresent()) {
			return solved;
		}

		// 2) Best-effort textual fallback (ghost/external)
		// getNameWithScope keeps Outer.Inner if present in source, which is better than
		// simple name.
		logger.log(Level.INFO, () -> "Textual: " + cit.getNameWithScope());

		String fallbackName = cit.getNameWithScope();
		TypeDeclaration<?> td = getByFqn(fallbackName);
		if (td != null) {
			logger.log(Level.SEVERE, () -> "QualifiedName dot-dot succeeded on index (2): " + fallbackName);
			return Optional.of(new DeclaredTypeRef(td));
		}
		logger.log(Level.INFO, () -> "UNSOLVED: " + cit.getNameWithScope());
		return Optional.of(new UnresolvedTypeRef(fallbackName));

	}

	private Optional<TypeRef> tryResolveWithSolver(ClassOrInterfaceType cit) {
		// 1) Prefer SymbolSolver
		try {
			ResolvedType rt = cit.resolve();
			logger.log(Level.INFO, () -> "ResolvedType.describe(): " + rt.describe());

			if (rt.isReferenceType()) {
				ResolvedReferenceType rrt = rt.asReferenceType();

				// 1a) If solver can give us the declaring AST node, prefer that
				Optional<TypeRef> refNode = tryResolveUsingNode(rrt);
				if (refNode.isPresent()) {
					return refNode;
				}
				// 1b) Otherwise, use solver’s qualified name (dot form)
				Optional<TypeRef> refName = tryResolveUsingQualifiedName(rrt);
				if (refName.isPresent()) {
					return refName;
				}
			}
		} catch (UnsolvedSymbolException e) {
			logger.log(Level.INFO, () -> "UNSOLVED: " + e.getName());
		} catch (RuntimeException e) {
			// Keep best-effort behavior (SymbolSolver sometimes throws other runtime
			// exceptions)
			logger.log(Level.INFO, () -> "Resolve failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
		}
		return Optional.empty();
	}

	private Optional<TypeRef> tryResolveUsingQualifiedName(ResolvedReferenceType rrt) {
		try {
			String qualifiedName = rrt.getQualifiedName();
			logger.log(Level.INFO, () -> "QualifiedName dot-dot: " + qualifiedName);

			// If your index expects $ for nested, you may want a normalizer.
			// But staying “less design change”: just try both if you can.
			TypeDeclaration<?> td = getByFqn(qualifiedName);
			if (td != null) {
				logger.log(Level.SEVERE, () -> "QualifiedName dot-dot succeeded on index (1): " + qualifiedName);
				return Optional.of(new DeclaredTypeRef(td));
			}
			return Optional.of(new ExternalTypeRef(qualifiedName));
		} catch (RuntimeException ex) {
			logger.log(Level.INFO, () -> "Could not read qualified name: " + ex.getClass().getSimpleName());
			// fall through to textual fallback below
		}
		return Optional.empty();
	}

	private Optional<TypeRef> tryResolveUsingNode(ResolvedReferenceType rrt) {
		try {
			Optional<ResolvedReferenceTypeDeclaration> optRtd = rrt.getTypeDeclaration();
			if (optRtd.isPresent()) {
				ResolvedReferenceTypeDeclaration rtd = optRtd.get();

				Optional<Node> astNode = rtd.toAst();
				if (astNode.isPresent() && astNode.get() instanceof TypeDeclaration<?> td) {
					String fqnDollar = DeclaredIndex.deriveFqnDollar(td);

					TypeDeclaration<?> indexed = getByFqn(fqnDollar);
					if (indexed != null) {
						return Optional.of(new DeclaredTypeRef(indexed));
					} else {
						return Optional.of(new ExternalTypeRef(fqnDollar));
					}
				}
			}

		} catch (RuntimeException ex) {
			// Some solvers/declarations may throw UnsupportedOperationException, etc.
			logger.log(Level.INFO, () -> "TypeDeclaration/toAst not available: " + ex.getClass().getSimpleName());
		}
		return Optional.empty();

	}

	/**
	 *
	 * @param ownerPkg
	 * @param rawName
	 * @return
	 */
	String resolveTypeName(String ownerPkg, String rawName) {
		if (rawName == null) {
			return null;
		}
		String raw = rawName.trim();
		if (raw.isEmpty()) {
			return null;
		}

		if (raw.contains(PACKAGE_SEPARATOR) && byKey.containsKey(key(raw))) {
			return raw;
		}

		String simple = DeclaredIndex.simpleName(raw);

		String samePkg = (ownerPkg == null || ownerPkg.isEmpty()) ? simple : ownerPkg + PACKAGE_SEPARATOR + simple;

		if (byKey.containsKey(key(samePkg))) {
			return samePkg;
		}

		TypeKey unique = uniqueBySimple.get(simple);
		return unique == null ? null : unique.text();
	}

	/**
	 *
	 * @param pkg
	 * @param ownerFqn
	 * @param type
	 * @return
	 */
	String resolveAssocTarget(String pkg, String ownerFqn, Type type) {
		String raw = DeclaredIndex.rawNameOf(type);
		String target = resolveTypeName(pkg, raw);
		if (target == null || target.equals(ownerFqn)) {
			return null;
		}
		return target;
	}

	/**
	 *
	 * @param td
	 * @return
	 */
	static String deriveFqnDollar(TypeDeclaration<?> td) {
		String pkg = derivePkg(td);

		Deque<String> names = new ArrayDeque<>();
		Node cur = td;
		while (cur != null) {
			if (cur instanceof TypeDeclaration<?> t) {
				names.push(t.getNameAsString());
			}
			cur = cur.getParentNode().orElse(null);
		}
		String chain = String.join("$", names);

		return pkg.isEmpty() ? chain : pkg + "." + chain;
	}

	/**
	 *
	 * @param td
	 * @return
	 */
	static String derivePkg(TypeDeclaration<?> td) {
		return td.findCompilationUnit().flatMap(u -> u.getPackageDeclaration().map(pd -> pd.getNameAsString()))
				.orElse("");
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
	static String rawTypeName(String typeAsString) {
		return typeAsString.replaceAll("<[^>]*>", EMPTY_STRING).replace("[]", EMPTY_STRING).trim();
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
	static String ownerFqnOf(String fqn) {
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
	static String rawNameOf(Type type) {
		Type t = peelArrays(type);

		if (t.isClassOrInterfaceType()) {
			return t.asClassOrInterfaceType().getNameWithScope();
		}
		return t.asString();
	}

	/**
	 *
	 * @param p
	 * @return
	 */
	static String stereotypesToString(Parameter p) {
		return DeclaredIndex.renderStereotypes(DeclaredIndex.stereotypesOf(p));
	}

	/**
	 *
	 * @param keysByPkg2
	 * @return
	 */
	private static Map<String, List<TypeKey>> sortPackagesByNameFqn(Map<String, List<TypeKey>> keysByPkg2) {
		return keysByPkg2.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	/**
	 *
	 * @param td
	 * @return
	 */
	static boolean isTopLevel(TypeDeclaration<?> td) {
		return td.getParentNode().map(CompilationUnit.class::isInstance).orElse(false);
	}

	// Old GenerateClassDiagram helpers
	//

	/**
	 * Converts a JavaParser access specifier to PlantUML visibility notation.
	 *
	 * <p>
	 * Mapping:
	 * <ul>
	 * <li>{@code public} → {@code +}</li>
	 * <li>{@code protected} → {@code #}</li>
	 * <li>{@code private} → {@code -}</li>
	 * <li>package-private → {@code ~}</li>
	 * </ul>
	 *
	 * @param n node providing access modifiers; must not be {@code null}
	 * @return PlantUML visibility character
	 * @throws NullPointerException if {@code n} is {@code null}
	 */
	static String visibility(NodeWithAccessModifiers<?> n) {
		AccessSpecifier a = n.getAccessSpecifier();
		return switch (a) {
		case PUBLIC -> "+";
		case PROTECTED -> "#";
		case PRIVATE -> "-";
		default -> "~";
		};
	}

	// Shared helpers for visitors and DeclaredIndex.

	/**
	 * Returns the stereotypes (annotation simple names) declared on a node.
	 *
	 * <p>
	 * This method returns annotation identifiers only (simple names), not fully
	 * qualified names.
	 *
	 * @param n annotated node; must not be {@code null}
	 * @return list of annotation identifiers (possibly empty)
	 * @throws NullPointerException if {@code n} is {@code null}
	 */
	static List<String> stereotypesOf(NodeWithAnnotations<?> n) {
		return n.getAnnotations().stream().map(a -> a.getName().getIdentifier()).sorted().toList();
	}

	/**
	 * Renders a list of stereotype names into PlantUML stereotype syntax.
	 *
	 * <p>
	 * Example: {@code ["Entity","Deprecated"]} becomes
	 * {@code " <<Entity>> <<Deprecated>>"}.
	 *
	 * @param ss stereotype names; may be {@code null} or empty
	 * @return a leading-space-prefixed stereotype block, or {@code ""} when none
	 */
	static String renderStereotypes(List<String> ss) {
		if (ss == null || ss.isEmpty()) {
			return "";
		}
		return " " + ss.stream().map(s -> "<<" + s + ">>").collect(Collectors.joining(" "));
	}

	/**
	 * Returns the simple name of a type name that may be package-qualified and/or
	 * nested.
	 *
	 * <p>
	 * This method strips:
	 * <ul>
	 * <li>package qualification using {@code '.'}</li>
	 * <li>nesting qualification using {@code '$'}</li>
	 * </ul>
	 *
	 * <p>
	 * Example: {@code "p.Outer$Inner"} becomes {@code "Inner"}.
	 *
	 * @param qname fully-qualified and/or nested type name; must not be
	 *              {@code null}
	 * @return the simple name component
	 * @throws NullPointerException if {@code qname} is {@code null}
	 */
	static String simpleName(String qname) {
		String s = qname;
		int lt = s.lastIndexOf('.');
		if (lt >= 0) {
			s = s.substring(lt + 1);
		}
		lt = s.lastIndexOf('$');
		if (lt >= 0) {
			s = s.substring(lt + 1);
		}
		return s;
	}

	// Scope simple name is not solver-confirmed; unresolved unless indexed.
	public Optional<TypeRef> resolveScopeName(String simpleName, Node usageSite) {
		TypeDeclaration<?> indexed = getByFqn(simpleName);
		if (indexed != null) {
			return Optional.of(new DeclaredTypeRef(indexed));
		}
		return Optional.of(new UnresolvedTypeRef(simpleName));
	}

}