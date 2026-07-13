package com.rentflow.identity.infrastructure;

import com.rentflow.identity.domain.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface UserMapper {
    @Select("""
            SELECT id, username, password_hash, nickname, role, timezone, enabled
            FROM users
            WHERE username = #{username}
            LIMIT 1
            """)
    Optional<UserAccount> findByUsername(@Param("username") String username);

    @Insert("""
            INSERT INTO users (id, username, password_hash, nickname, role, timezone, enabled)
            VALUES (#{id}, #{username}, #{passwordHash}, #{nickname}, #{role}, #{timezone}, TRUE)
            """)
    int insert(UserAccount account);
}
