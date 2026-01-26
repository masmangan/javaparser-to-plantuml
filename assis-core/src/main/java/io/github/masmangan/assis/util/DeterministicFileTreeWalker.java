/*
 * Copyright (c) 2025-2026, Marco Mangan. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */

package io.github.masmangan.assis.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Deterministic traversal of one or more roots.
 *
 * Contract: - Deterministic: same filesystem snapshot + same roots => same
 * result order - Stable: ties are broken deterministically - Pre-order:
 * directories are visited before their children - Directory children order:
 * directories first, then files; both sorted by name; tie-break by absolute
 * path
 */
public final class DeterministicFileTreeWalker {

	private DeterministicFileTreeWalker() {
	}

	/**
	 * Discovers .java files under the given roots in a deterministic order.
	 *
	 * @param roots          source roots (files or directories)
	 * @param shouldVisitDir predicate to decide whether to traverse into a
	 *                       directory
	 */
	public static DeterministicPathList discoverJavaFiles(Set<Path> roots, Predicate<Path> shouldVisitDir)
			throws IOException {
		Objects.requireNonNull(shouldVisitDir, "shouldVisitDir");

		List<Path> canonRoots = canonicalizeRoots(roots);

		List<Path> out = new ArrayList<>();
		for (Path root : canonRoots) {
			if (!Files.exists(root)) {
				continue;
			}
			if (Files.isRegularFile(root) && root.toString().endsWith(".java")) {
				out.add(root);
				continue;
			}
			if (Files.isDirectory(root)) {
				walkDirForJava(root, out, shouldVisitDir);
			}
		}
		return DeterministicPathList.of(out);
	}

	/* ===================== internals ===================== */

	private static void walkDirForJava(Path dir, List<Path> out, Predicate<Path> shouldVisitDir) throws IOException {
		for (Path child : sortedChildren(dir)) {
			if (Files.isDirectory(child)) {
				if (shouldVisitDir.test(child)) {
					walkDirForJava(child, out, shouldVisitDir);
				}
			} else if (Files.isRegularFile(child) && child.toString().endsWith(".java")) {
				out.add(child);
			}
		}
	}

	private record Child(Path path, boolean isDir, String name, String abs) {
	}

	private static List<Path> sortedChildren(Path dir) throws IOException {
		try (var stream = Files.list(dir)) {
			return stream.map(p -> new Child(p, Files.isDirectory(p), fileName(p), absNorm(p))).sorted(Comparator
					.comparingInt((Child c) -> c.isDir ? 0 : 1).thenComparing(c -> c.name).thenComparing(c -> c.abs))
					.map(Child::path).toList();
		}
	}

	private static String fileName(Path p) {
		Path fn = p.getFileName();
		return fn == null ? "" : fn.toString();
	}

	private static String absNorm(Path p) {
		return p.toAbsolutePath().normalize().toString();
	}

	private static List<Path> canonicalizeRoots(Set<Path> roots) {
		if (roots == null || roots.isEmpty()) {
			return List.of();
		}

		return roots.stream().filter(Objects::nonNull).map(p -> p.toAbsolutePath().normalize()).distinct()
				.sorted(Comparator.comparing(Path::toString)).toList();
	}
}