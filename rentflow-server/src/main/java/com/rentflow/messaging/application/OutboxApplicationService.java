package com.rentflow.messaging.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.messaging.api.DomainEventPublisher;
import com.rentflow.messaging.infrastructure.OutboxMapper;
import com.rentflow.shared.id.Ulid;
import com.rentflow.shared.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class OutboxApplicationService implements DomainEventPublisher {
    private final OutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;

    public OutboxApplicationService(OutboxMapper outboxMapper, ObjectMapper objectMapper) {
        this.outboxMapper = outboxMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
        if (correlationId == null) {
            correlationId = Ulid.next();
        }
        if (outboxMapper.insert(
                Ulid.next(),
                aggregateType,
                aggregateId,
                eventType,
                serialize(payload),
                correlationId
        ) != 1) {
            throw new IllegalStateException("Outbox insert did not affect exactly one row");
        }
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Outbox payload cannot be serialized", exception);
        }
    }
}
