package com.hcl.stockanalysis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

/**
 * Ensures identity counters and auto-increment sequences for all seeded tables
 * are dynamically synchronized to MAX(id) + 1 on every application startup.
 * 
 * Works across both H2 (local / tests) and PostgreSQL (production).
 */
@Component
@Order(1)
public class DatabaseSequenceSynchronizer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSequenceSynchronizer.class);

    private static final List<String> TABLES = List.of("stock", "stock_price", "app_user", "watchlist");

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public DatabaseSequenceSynchronizer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        syncDatabaseSequences();
    }

    /**
     * Synchronizes identity counters for all entity tables to MAX(id) + 1.
     * Safe to invoke multiple times on startup or during tests.
     */
    public void syncDatabaseSequences() {
        try (Connection conn = dataSource.getConnection()) {
            String dbProduct = conn.getMetaData().getDatabaseProductName();
            if (dbProduct == null) {
                dbProduct = "UNKNOWN";
            }
            log.info("Synchronizing database identity sequences for product: {}", dbProduct);

            boolean isH2 = dbProduct.toLowerCase().contains("h2");
            boolean isPostgres = dbProduct.toLowerCase().contains("postgres");

            for (String table : TABLES) {
                syncTableSequence(table, isH2, isPostgres);
            }
            log.info("Database identity sequences synchronized successfully.");
        } catch (Exception e) {
            log.warn("Failed to synchronize database sequences: {}. Startup will continue.", e.getMessage(), e);
        }
    }

    private void syncTableSequence(String table, boolean isH2, boolean isPostgres) {
        try {
            Long maxId = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM " + table, Long.class);
            if (maxId == null) {
                maxId = 0L;
            }

            if (isH2) {
                long nextVal = maxId + 1;
                jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH " + nextVal);
                log.info("H2: Table '{}' sequence restarted with next value {}", table, nextVal);
            } else if (isPostgres) {
                try {
                    if (maxId > 0) {
                        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('" + table + "', 'id'), " + maxId + ", true)");
                    } else {
                        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('" + table + "', 'id'), 1, false)");
                    }
                    log.info("PostgreSQL: Table '{}' sequence synced to MAX(id)={}", table, maxId);
                } catch (Exception ex) {
                    // Fallback for IDENTITY columns in PostgreSQL 10+
                    long nextVal = maxId + 1;
                    jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH " + nextVal);
                    log.info("PostgreSQL (identity restart fallback): Table '{}' restarted with {}", table, nextVal);
                }
            } else {
                long nextVal = maxId + 1;
                jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH " + nextVal);
                log.info("Generic SQL: Table '{}' sequence restarted with {}", table, nextVal);
            }
        } catch (Exception e) {
            log.warn("Could not synchronize sequence for table '{}': {}", table, e.getMessage());
        }
    }
}
