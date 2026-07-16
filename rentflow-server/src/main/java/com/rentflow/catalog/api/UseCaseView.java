package com.rentflow.catalog.api;

import java.util.List;

public record UseCaseView(
        String id,
        String code,
        String name,
        String description,
        List<String> aliases
) {
    public UseCaseView {
        aliases = List.copyOf(aliases);
    }
}
