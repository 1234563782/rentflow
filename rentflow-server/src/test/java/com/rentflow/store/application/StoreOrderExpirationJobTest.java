package com.rentflow.store.application;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StoreOrderExpirationJobTest {
    @Test
    void delegatesScheduledCleanupToTransactionalService() {
        StoreApplicationService service = mock(StoreApplicationService.class);

        new StoreOrderExpirationJob(service).expireBatch();

        verify(service).closeExpiredOrders();
    }
}
