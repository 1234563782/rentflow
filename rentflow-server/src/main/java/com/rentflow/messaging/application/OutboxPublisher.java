package com.rentflow.messaging.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.messaging.infrastructure.DomainEventEnvelope;
import com.rentflow.messaging.infrastructure.OutboxEventRow;
import com.rentflow.messaging.infrastructure.OutboxMapper;
import com.rentflow.messaging.infrastructure.RabbitTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(prefix = "rentflow.messaging", name = "enabled", havingValue = "true")
public class OutboxPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxMapper outboxMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final MessagingProperties properties;

    public OutboxPublisher(
            OutboxMapper outboxMapper,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            MessagingProperties properties
    ) {
        this.outboxMapper = outboxMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.rabbitTemplate.setMandatory(true);
    }

    @Scheduled(fixedDelayString = "${rentflow.messaging.publish-fixed-delay-millis:1000}")
    @Transactional
    public void publishBatch() {
        List<OutboxEventRow> events = outboxMapper.lockPendingBatch(properties.publishBatchSize());
        for (OutboxEventRow event : events) {
            try {
                publish(event);
                if (outboxMapper.markPublished(event.id()) != 1) {
                    throw new IllegalStateException("Published outbox event could not be marked");
                }
            } catch (Exception exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                int delaySeconds = Math.min(300, 1 << Math.min(event.retryCount(), 8));
                String message = exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage();
                if (outboxMapper.markRetry(event.id(), message, delaySeconds) != 1) {
                    throw new IllegalStateException("Failed outbox event could not be rescheduled", exception);
                }
                LOGGER.warn("Outbox publish failed eventId={}, retryInSeconds={}", event.id(), delaySeconds, exception);
                break;
            }
        }
    }

    private void publish(OutboxEventRow row) throws Exception {
        DomainEventEnvelope event = new DomainEventEnvelope(
                row.id(),
                row.aggregateType(),
                row.aggregateId(),
                row.eventType(),
                readPayload(row.payload()),
                row.correlationId(),
                row.createdAt()
        );
        CorrelationData correlation = new CorrelationData(row.id());
        rabbitTemplate.convertAndSend(
                RabbitTopology.DOMAIN_EXCHANGE,
                row.eventType(),
                event,
                correlation
        );
        CorrelationData.Confirm confirm = correlation.getFuture()
                .get(properties.confirmTimeoutMillis(), TimeUnit.MILLISECONDS);
        if (!confirm.isAck()) {
            throw new AmqpException("Broker rejected event: " + confirm.getReason());
        }
        if (correlation.getReturned() != null) {
            throw new AmqpException("Event was not routed to a queue");
        }
    }

    private com.fasterxml.jackson.databind.JsonNode readPayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored outbox payload cannot be read", exception);
        }
    }
}
