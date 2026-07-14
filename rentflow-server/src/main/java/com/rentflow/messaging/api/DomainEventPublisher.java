package com.rentflow.messaging.api;

import java.util.Map;

public interface DomainEventPublisher {
    void record(String aggregateType, String aggregateId, String eventType, Map<String, Object> payload);
}
