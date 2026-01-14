/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

/**
 * Index of declared types (top-level and nested).
 */
public class DeclaredIndex {

	/**
	 * Logger used by index to warn about type redefinition.
	 */
	static final Logger logger = Logger.getLogger(DeclaredIndex.class.getName());

	/**
	 *
	 */
	private static final String PACKAGE_SEPARATOR = ".";

	/** 
	 * FQN → declaration 
	 * */
	final Map<String, TypeDeclaration<?>> byFqn = new LinkedHashMap<>();

	/** 
	 * FQN → declared package (from CompilationUnit) 
	 * */
	final Map<String, String> pkgByFqn = new LinkedHashMap<>();

	/** 
	 * package → list of FQNs 
	 * */
	Map<String, List<String>> fqnsByPkg = new LinkedHashMap<>();

	/** 
	 * simple name → unique FQN (only when unambiguous) 
	 * 
	 */
	final Map<String, String> uniqueBySimple = new LinkedHashMap<>();

	/**
	 * Populates idx with declared types from compilation units.
	 *
	 * @param cus
	 * @return
	 */
	static void fill(DeclaredIndex idx, List<CompilationUnit> cus) {

		for (CompilationUnit cu : cus) {

			for (TypeDeclaration<?> td : cu.getTypes()) {
				collectTypeRecursive(idx, cu, td, null, PACKAGE_SEPARATOR);
			}
		}

		for (Map.Entry<String, String> e : idx.pkgByFqn.entrySet()) {
			String fqn = e.getKey();
			String pkg = e.getValue();
			idx.fqnsByPkg.computeIfAbsent(pkg, k -> new ArrayList<>()).add(fqn);
		}

		idx.fqnsByPkg = sortPackagesByNameFqn(idx.fqnsByPkg);
		idx.fqnsByPkg.values().forEach(list -> list.sort(String::compareTo));

		Map<String, String> seen = new LinkedHashMap<>();
		Set<String> ambiguous = new LinkedHashSet<>();

		for (String fqn : idx.byFqn.keySet()) {
			String simple = GenerateClassDiagram.simpleName(fqn);
			if (!seen.containsKey(simple)) {
				seen.put(simple, fqn);
			} else {
				ambiguous.add(simple);
			}
		}

		for (var e : seen.entrySet()) {
			if (!ambiguous.contains(e.getKey())) {
				idx.uniqueBySimple.put(e.getKey(), e.getValue());
			}
		}

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

		if (raw.contains(PACKAGE_SEPARATOR) && byFqn.containsKey(raw)) {
			return raw;
		}

		String simple = GenerateClassDiagram.simpleName(raw);

		String samePkg = (ownerPkg == null || ownerPkg.isEmpty()) ? simple : ownerPkg + PACKAGE_SEPARATOR + simple;

		if (byFqn.containsKey(samePkg)) {
			return samePkg;
		}

		return uniqueBySimple.get(simple);
	}

	static String deriveFqnDollar(TypeDeclaration<?> td) {
	  String pkg = derivePkg(td);
	
	  // walk up TypeDeclaration parents to build nested chain
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

	static String derivePkg(TypeDeclaration<?> td) {
	  return td.findCompilationUnit()
	      .flatMap(cu -> cu.getPackageDeclaration().map(pd -> pd.getNameAsString()))
	      .orElse(""); // default package
	}

	/**
	 *
	 * @param idx
	 * @param cu
	 * @param td
	 * @param ownerFqn
	 * @param separator
	 */
	private static void collectTypeRecursive(DeclaredIndex idx, CompilationUnit cu, TypeDeclaration<?> td,
			String ownerFqn, String separator) {
		String name = td.getNameAsString();
		String fqn;
		String pkg = cu.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");

		if (ownerFqn == null) {
			fqn = pkg.isEmpty() ? name : pkg + separator + name;
		} else {
			fqn = ownerFqn + separator + name;
		}
		if (idx.byFqn.containsKey(fqn)) {
			logger.log(Level.WARNING, () -> "Attempt to redefine " + fqn);
			logger.log(Level.WARNING, cu::toString);
			logger.log(Level.WARNING, td::toString);
			logger.log(Level.WARNING, () -> "Keeping first definition.");
			return;
		}

		idx.byFqn.put(fqn, td);
		idx.pkgByFqn.put(fqn, pkg);

		if (td instanceof ClassOrInterfaceDeclaration cid) {
			cid.getMembers().forEach(m -> {
				if (m instanceof TypeDeclaration<?> nested) {
					collectTypeRecursive(idx, cu, nested, fqn, "$");
				}
			});
		} else if (td instanceof EnumDeclaration ed) {
			ed.getMembers().forEach(m -> {
				if (m instanceof TypeDeclaration<?> nested) {
					collectTypeRecursive(idx, cu, nested, fqn, "$");
				}
			});
		}
	}

	/**
	 *
	 * @param byPkg
	 * @return
	 */
	static Map<String, List<String>> sortPackagesByNameFqn(Map<String, List<String>> byPkg) {
		return byPkg.entrySet().stream().sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
	}

	/**
	 *
	 * @param fqn
	 * @return
	 */
	static String pumlName(String fqn) {
		return fqn;
	}

	/**
	 *
	 * @param fqn
	 * @return
	 */
	static String qPuml(String fqn) {
		return "\"" + pumlName(fqn) + "\"";
	}
	
	
	static boolean isTopLevel(TypeDeclaration<?> td) {
		return td.getParentNode().isPresent()
			    && td.getParentNode().get() instanceof com.github.javaparser.ast.CompilationUnit;


	}
}