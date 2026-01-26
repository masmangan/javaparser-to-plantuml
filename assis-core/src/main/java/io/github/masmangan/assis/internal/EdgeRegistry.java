package io.github.masmangan.assis.internal;

import java.util.HashSet;
import java.util.Set;

record RelKey(String fromFqn, String toFqn) {
}

public class EdgeRegistry {

	private final Set<RelKey> seen = new HashSet<>();

	public void registerAssociation(String from, String to) {
		register(from, to);
	}

	public boolean isRegistered(String from, String to) {
		return seen.contains(new RelKey(from, to));
	}

	public void registerDependency(String from, String to) {
		register(from, to);
	}

	private void register(String from, String to) {
		seen.add(new RelKey(from, to));
	}

}
