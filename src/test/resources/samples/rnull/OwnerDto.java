package samples.rnull;

import jakarta.validation.constraints.NotNull;

public record OwnerDto(
    int id,
    @NotNull String name
) {
}