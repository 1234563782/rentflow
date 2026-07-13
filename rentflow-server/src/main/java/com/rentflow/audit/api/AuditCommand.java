package com.rentflow.audit.api;

import java.util.Map;

public record AuditCommand(
        String userId,
        String action,
        String aggregateType,
        String aggregateId,
        String outcome,
        Map<String, Object> metadata
) {
    public AuditCommand {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
