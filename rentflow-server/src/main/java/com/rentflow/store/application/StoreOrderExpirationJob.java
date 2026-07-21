package com.rentflow.store.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StoreOrderExpirationJob {
    private final StoreApplicationService service;

    public StoreOrderExpirationJob(StoreApplicationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${rentflow.store.cleanup-fixed-delay-millis:60000}")
    public void expireBatch() {
        service.closeExpiredOrders();
    }
}
