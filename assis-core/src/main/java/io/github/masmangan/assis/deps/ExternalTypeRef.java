package io.github.masmangan.assis.deps;

public record ExternalTypeRef(String fqn) implements TypeRef {

    @Override
    public String displayName() {
        return fqn;
    }
}