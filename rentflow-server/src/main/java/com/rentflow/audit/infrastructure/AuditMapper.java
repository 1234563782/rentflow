package com.rentflow.audit.infrastructure;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuditMapper {
    @Insert("""
            INSERT INTO audit_logs (
                id, correlation_id, user_id, action, aggregate_type,
                aggregate_id, outcome, metadata
            ) VALUES (
                #{id}, #{correlationId}, #{userId}, #{action}, #{aggregateType},
                #{aggregateId}, #{outcome}, #{metadata}
            )
            """)
    int insert(
            @Param("id") String id,
            @Param("correlationId") String correlationId,
            @Param("userId") String userId,
            @Param("action") String action,
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") String aggregateId,
            @Param("outcome") String outcome,
            @Param("metadata") String metadata
    );
}
