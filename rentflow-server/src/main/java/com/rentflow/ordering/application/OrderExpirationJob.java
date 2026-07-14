package com.rentflow.ordering.application;

import com.rentflow.audit.api.AuditCommand;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.inventory.api.ReservationOrderAccess;
import com.rentflow.messaging.api.DomainEventPublisher;
import com.rentflow.ordering.infrastructure.ExpiredOrder;
import com.rentflow.ordering.infrastructure.OrderMapper;
import com.rentflow.shared.id.Ulid;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class OrderExpirationJob {
    private final OrderMapper orderMapper;
    private final ReservationOrderAccess reservationAccess;
    private final AuditLogWriter auditLogWriter;
    private final DomainEventPublisher eventPublisher;
    private final OrderProperties properties;

    public OrderExpirationJob(
            OrderMapper orderMapper,
            ReservationOrderAccess reservationAccess,
            AuditLogWriter auditLogWriter,
            DomainEventPublisher eventPublisher,
            OrderProperties properties
    ) {
        this.orderMapper = orderMapper;
        this.reservationAccess = reservationAccess;
        this.auditLogWriter = auditLogWriter;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${rentflow.order.cleanup-fixed-delay-millis:60000}")
    @Transactional
    public void expireBatch() {
        List<ExpiredOrder> expired = orderMapper.lockExpiredBatch(properties.cleanupBatchSize());
        for (ExpiredOrder order : expired) {
            if (orderMapper.expirePending(order.id()) != 1) {
                throw new IllegalStateException("Locked pending order could not be expired");
            }
            reservationAccess.expireForOrder(order.sourceReservationId());
            if (orderMapper.insertHistory(
                    Ulid.next(),
                    order.id(),
                    "PENDING_CONFIRMATION",
                    "EXPIRED",
                    "CONFIRMATION_TIMEOUT"
            ) != 1) {
                throw new IllegalStateException("Expired order history could not be inserted");
            }
            eventPublisher.record("ORDER", order.id(), "order.expired", Map.of(
                    "orderId", order.id(),
                    "reservationId", order.sourceReservationId(),
                    "source", "scheduled-cleanup"
            ));
            auditLogWriter.write(new AuditCommand(
                    order.userId(),
                    "ORDER_EXPIRED",
                    "ORDER",
                    order.id(),
                    "SUCCESS",
                    Map.of("source", "scheduled-cleanup")
            ));
        }
    }
}
