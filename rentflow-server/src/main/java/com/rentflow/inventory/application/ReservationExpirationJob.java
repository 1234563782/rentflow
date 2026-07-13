package com.rentflow.inventory.application;

import com.rentflow.audit.api.AuditCommand;
import com.rentflow.audit.api.AuditLogWriter;
import com.rentflow.inventory.infrastructure.ExpiredReservation;
import com.rentflow.inventory.infrastructure.ReservationMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class ReservationExpirationJob {
    private final ReservationMapper reservationMapper;
    private final ReservationProperties properties;
    private final AuditLogWriter auditLogWriter;

    public ReservationExpirationJob(
            ReservationMapper reservationMapper,
            ReservationProperties properties,
            AuditLogWriter auditLogWriter
    ) {
        this.reservationMapper = reservationMapper;
        this.properties = properties;
        this.auditLogWriter = auditLogWriter;
    }

    @Scheduled(fixedDelayString = "${rentflow.reservation.cleanup-fixed-delay-millis:60000}")
    @Transactional
    public void expireBatch() {
        List<ExpiredReservation> expired = reservationMapper.lockExpiredBatch(properties.cleanupBatchSize());
        if (expired.isEmpty()) {
            return;
        }
        List<String> ids = expired.stream().map(ExpiredReservation::id).toList();
        if (reservationMapper.markExpired(ids) != ids.size()) {
            throw new IllegalStateException("Expired reservation update count did not match the locked batch");
        }
        expired.forEach(reservation -> auditLogWriter.write(new AuditCommand(
                reservation.userId(),
                "RESERVATION_EXPIRED",
                "RESERVATION",
                reservation.id(),
                "SUCCESS",
                Map.of("source", "scheduled-cleanup")
        )));
    }
}
