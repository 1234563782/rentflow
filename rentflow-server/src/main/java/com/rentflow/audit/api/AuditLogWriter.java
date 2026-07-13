package com.rentflow.audit.api;

public interface AuditLogWriter {
    void write(AuditCommand command);
}
