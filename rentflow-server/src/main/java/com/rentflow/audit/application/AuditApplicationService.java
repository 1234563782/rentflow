package com.rentflow.audit.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.audit.api.AuditCommand;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.audit.infrastructure.AuditMapper;
import com.rentflow.shared.id.Ulid;
import com.rentflow.shared.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditApplicationService implements AuditLogWriter {
    private final AuditMapper auditMapper;
    private final ObjectMapper objectMapper;

    public AuditApplicationService(AuditMapper auditMapper, ObjectMapper objectMapper) {
        this.auditMapper = auditMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void write(AuditCommand command) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId == null) {
            correlationId = Ulid.next();
        }
        if (auditMapper.insert(
                Ulid.next(),
                correlationId,
                command.userId(),
                command.action(),
                command.aggregateType(),
                command.aggregateId(),
                command.outcome(),
                serialize(command)
        ) != 1) {
            throw new IllegalStateException("Audit insert did not affect exactly one row");
        }
    }

    private String serialize(AuditCommand command) {
        try {
            return objectMapper.writeValueAsString(command.metadata());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Audit metadata cannot be serialized", exception);
        }
    }
}
