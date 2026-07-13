package com.rentflow.shared.idempotency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;

@Component
public class MySqlIdempotencyMutex {
    private static final Logger LOGGER = LoggerFactory.getLogger(MySqlIdempotencyMutex.class);
    private static final String LOCK_PREFIX = "rentflow-idem:";
    private final DataSource dataSource;
    private final IdempotencyProperties properties;

    public MySqlIdempotencyMutex(DataSource dataSource, IdempotencyProperties properties) {
        this.dataSource = dataSource;
        this.properties = properties;
    }

    public void acquire(String scope) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Idempotency mutex requires an active synchronized transaction");
        }
        String lockName = LOCK_PREFIX + sha256(scope).substring(0, 48);
        Connection connection = DataSourceUtils.getConnection(dataSource);
        int result = getLock(connection, lockName);
        if (result == 0) {
            throw new IdempotencyInProgressException(properties.retryAfterSeconds());
        }
        if (result != 1) {
            throw new DataAccessResourceFailureException("MySQL did not return a valid GET_LOCK result");
        }
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    releaseQuietly(connection, lockName);
                }
            });
        } catch (RuntimeException exception) {
            releaseQuietly(connection, lockName);
            throw exception;
        }
    }

    private int getLock(Connection connection, String lockName) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, lockName);
            statement.setInt(2, properties.waitTimeoutSeconds());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new DataAccessResourceFailureException("MySQL GET_LOCK returned no row");
                }
                int value = resultSet.getInt(1);
                return resultSet.wasNull() ? -1 : value;
            }
        } catch (SQLException exception) {
            throw new DataAccessResourceFailureException("MySQL idempotency lock could not be acquired", exception);
        }
    }

    private void releaseQuietly(Connection connection, String lockName) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, lockName);
            statement.executeQuery().close();
        } catch (SQLException exception) {
            LOGGER.error("MySQL idempotency lock could not be released lockName={}", lockName, exception);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(
                    value.getBytes(StandardCharsets.UTF_8)
            ));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
