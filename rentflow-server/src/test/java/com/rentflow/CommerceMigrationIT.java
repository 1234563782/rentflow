package com.rentflow;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class CommerceMigrationIT {
    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.5")
            .withDatabaseName("rentflow")
            .withUsername("rentflow")
            .withPassword("rentflow-test");

    @Test
    void freshDatabaseEndsWithCommerceSchemaOnly() throws SQLException {
        Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = MYSQL.createConnection("")) {
            Set<String> tables = tables(connection);
            assertThat(tables).contains(
                    "products",
                    "product_skus",
                    "commerce_orders",
                    "commerce_order_items",
                    "commerce_product_reviews"
            );
            assertThat(tables).doesNotContain(
                    "equipment_units",
                    "rental_quotes",
                    "inventory_reservations",
                    "rental_orders",
                    "product_capacity_days",
                    "reservation_capacity_claims",
                    "product_reviews"
            );
            assertThat(columns(connection, "products"))
                    .doesNotContain("daily_rate", "fixed_deposit", "pricing_version");
        }
    }

    private static Set<String> tables(Connection connection) throws SQLException {
        Set<String> result = new HashSet<>();
        try (ResultSet rows = connection.getMetaData().getTables(
                connection.getCatalog(), null, "%", new String[]{"TABLE"}
        )) {
            while (rows.next()) {
                result.add(rows.getString("TABLE_NAME"));
            }
        }
        return result;
    }

    private static Set<String> columns(Connection connection, String table) throws SQLException {
        Set<String> result = new HashSet<>();
        try (ResultSet rows = connection.getMetaData().getColumns(
                connection.getCatalog(), null, table, "%"
        )) {
            while (rows.next()) {
                result.add(rows.getString("COLUMN_NAME"));
            }
        }
        return result;
    }
}
