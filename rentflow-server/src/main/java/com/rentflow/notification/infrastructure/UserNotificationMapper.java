package com.rentflow.notification.infrastructure;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserNotificationMapper {
    int insert(
            @Param("id") String id,
            @Param("userId") String userId,
            @Param("type") String type,
            @Param("uniqueKey") String uniqueKey,
            @Param("title") String title,
            @Param("content") String content,
            @Param("aggregateType") String aggregateType,
            @Param("aggregateId") String aggregateId
    );

    List<UserNotificationRow> listForUser(
            @Param("userId") String userId,
            @Param("offset") long offset,
            @Param("size") int size
    );

    long countForUser(@Param("userId") String userId);

    long countUnreadForUser(@Param("userId") String userId);

    int markRead(@Param("id") String id, @Param("userId") String userId);
}
