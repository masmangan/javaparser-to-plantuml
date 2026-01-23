package io.github.masmangan.assis.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Deterministic, immutable list of source root paths.
 *
 * Contract: - Input is a Set of source roots (must be non-null and non-empty).
 * - Paths are normalized to absolute normalized paths. - Order is deterministic
 * (sorted by Path::toString after normalization). - Iteration order is the
 * deterministic order.
 *
 * Non-goals: - This is NOT semantic sorting (packages/types/etc). It's only
 * deterministic root ordering.
 */
public final class DeterministicPathList implements Iterable<Path> {

	private static final Logger logger = Logger.getLogger(DeterministicPathList.class.getName());

	private final List<Path> paths;

	private DeterministicPathList(final List<Path> paths) {
		this.paths = List.copyOf(paths);
	}

	/** Build from a set of roots with validation + deterministic ordering. */
	public static DeterministicPathList fromSourceRoots(final Set<Path> sourceRoots) {
		checkSourceRoots(sourceRoots);
		return new DeterministicPathList(sortRootsByPath(sourceRoots));
	}

	public static DeterministicPathList of(List<Path> paths) {
		return new DeterministicPathList(paths == null ? List.of() : paths);
	}

	@Override
	public Iterator<Path> iterator() {
		return paths.iterator();
	}

	@Override
	public String toString() {
		return paths.toString();
	}

	/* ===================== internals ===================== */

	private static List<Path> sortRootsByPath(final Set<Path> sourceRoots) {
		// Track collapses: multiple raw roots -> same normalized absolute path
		Map<String, List<String>> occurrences = new LinkedHashMap<>();

		boolean sawNull = false;

		for (Path raw : sourceRoots) {
			if (raw == null) {
				sawNull = true;
				continue;
			}
			Path norm = raw.toAbsolutePath().normalize();
			String key = norm.toString();

			occurrences.computeIfAbsent(key, k -> new ArrayList<>()).add(raw.toString());
		}

		if (sawNull) {
			logger.log(Level.WARNING, "Ignoring null source root entry.");
		}

		for (Map.Entry<String, List<String>> e : occurrences.entrySet()) {
			List<String> rawList = e.getValue();
			if (rawList.size() > 1) {

				// @formatter:off
                String locations = rawList.stream()
                		.distinct()
                		.sorted()
                		.collect(Collectors.joining(", "));
                // @formatter:on

				logger.log(Level.WARNING, () -> "Duplicate source root detected after normalization for \"" + e.getKey()
						+ "\"; inputs collapse to the same root. Roots: " + locations);
			}
		}

		// @formatter:off
        List<Path> sorted = occurrences.keySet().stream()
                .map(Path::of)
                .sorted(Comparator.comparing(Path::toString))
                .toList();
        // @formatter:on

		return sorted;
	}

	private static void checkSourceRoots(final Set<Path> sourceRoots) {
		Objects.requireNonNull(sourceRoots, "sourceRoots");
		if (sourceRoots.isEmpty()) {
			throw new IllegalArgumentException("sourceRoots must not be empty.");
		}
	}
}