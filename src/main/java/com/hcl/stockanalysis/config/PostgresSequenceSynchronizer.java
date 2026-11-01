package com.hcl.stockanalysis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;

/**
 * Ensures that PostgreSQL sequence generators match the maximum IDs of seeded records.
 * Runs on application startup whenever connected to a PostgreSQL database.
 */
@Component
public class PostgresSequenceSynchronizer {

    private static final Logger log = LoggerFactory.getLogger(PostgresSequenceSynchronizer.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public PostgresSequenceSynchronizer(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeSequences() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbProduct = metaData.getDatabaseProductName();

            if (dbProduct != null && dbProduct.equalsIgnoreCase("PostgreSQL")) {
                log.info("PostgreSQL database detected. Synchronizing primary key sequences with seeded IDs...");
                List<String> tables = List.of("stock", "stock_price", "app_user", "watchlist");

                for (String table : tables) {
                    try {
                        String sql = "SELECT CASE " +
                                "WHEN pg_get_serial_sequence('" + table + "', 'id') IS NOT NULL " +
                                "THEN setval(pg_get_serial_sequence('" + table + "', 'id'), COALESCE(MAX(id), 1)) " +
                                "END FROM " + table;
                        jdbcTemplate.execute(sql);
                        log.info("Successfully synchronized sequence for table '{}'.", table);
                    } catch (Exception ex) {
                        log.warn("Notice: could not sync sequence for table '{}': {}", table, ex.getMessage());
                    }
                }
            } else {
                log.debug("Database is '{}'. Skipping PostgreSQL-specific sequence synchronization.", dbProduct);
            }
        } catch (Exception ex) {
            log.warn("Unable to check database metadata for sequence synchronization: {}", ex.getMessage());
        }
    }
}
