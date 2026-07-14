package com.rentflow.messaging.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OutboxMapper {
    int insert(
            @Param("id") String id,
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") String aggregateId,
            @Param("eventType") String eventType,
            @Param("payload") String payload,
            @Param("correlationId") String correlationId
    );

    List<OutboxEventRow> lockPendingBatch(@Param("batchSize") int batchSize);

    int markPublished(@Param("id") String id);

    int markRetry(
            @Param("id") String id,
            @Param("lastError") String lastError,
            @Param("delaySeconds") int delaySeconds
    );

    int insertInbox(@Param("consumerName") String consumerName, @Param("eventId") String eventId);
}
