package com.marlowefinch.ops;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * The Flyway migrations against H2 in PostgreSQL mode: schema, seed volumes and the
 * invariants the dashboard queries rely on. The expected counts come from
 * tools/make_seed.py.
 */
@SpringBootTest
@ActiveProfiles("demo")
class MigrationsTest {

    @Autowired
    private JdbcTemplate jdbc;

    private long count(String sql) {
        Long n = jdbc.queryForObject(sql, Long.class);
        return n == null ? 0 : n;
    }

    @Test
    void flywayAppliedTheSchemaAndTheSeed() {
        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT version, success FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank");
        assertThat(history).hasSize(2);
        assertThat(history.get(0).get("version")).hasToString("1");
        assertThat(history.get(1).get("version")).hasToString("2");
        assertThat(history).allSatisfy(row -> assertThat(row.get("success")).isEqualTo(true));
    }

    @Test
    void allFiveTablesExistAndAreSeeded() {
        assertThat(count("SELECT COUNT(*) FROM carriers")).isEqualTo(4);
        assertThat(count("SELECT COUNT(*) FROM vendors")).isEqualTo(8);
        assertThat(count("SELECT COUNT(*) FROM orders")).isEqualTo(1934);
        assertThat(count("SELECT COUNT(*) FROM deliveries")).isEqualTo(1859);
        assertThat(count("SELECT COUNT(*) FROM tickets")).isEqualTo(600);
    }

    @Test
    void ordersCoverTheNinetyDaysBeforeToday() {
        Map<String, Object> span = jdbc.queryForMap("SELECT MIN(ordered_at) AS first, MAX(ordered_at) AS last FROM orders");
        assertThat(span.get("first")).hasToString("2026-06-23");
        assertThat(span.get("last")).hasToString("2026-09-20");
    }

    @Test
    void everyShippedOrderHasExactlyOneDeliveryAndNoOtherOrderHasAny() {
        long shipped = count("SELECT COUNT(*) FROM orders WHERE status = 'shipped'");
        long withDelivery = count("SELECT COUNT(DISTINCT order_id) FROM deliveries");
        assertThat(withDelivery).isEqualTo(shipped);
        assertThat(count("""
                SELECT COUNT(*) FROM deliveries d JOIN orders o ON o.id = d.order_id
                WHERE o.status <> 'shipped'
                """)).isZero();
    }

    @Test
    void daysLateAgreesWithTheDates() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT promised_date, delivered_date, days_late FROM deliveries");
        long inTransit = 0;
        for (Map<String, Object> row : rows) {
            LocalDate promised = ((java.sql.Date) row.get("promised_date")).toLocalDate();
            java.sql.Date delivered = (java.sql.Date) row.get("delivered_date");
            Object daysLate = row.get("days_late");
            if (delivered == null) {
                inTransit++;
                assertThat(daysLate).isNull();
                continue;
            }
            long expected = Math.max(0, ChronoUnit.DAYS.between(promised, delivered.toLocalDate()));
            assertThat(((Number) daysLate).longValue()).isEqualTo(expected);
        }
        assertThat(inTransit).isEqualTo(54);
    }

    @Test
    void everyTicketPointsAtAnOrderAndUsesOneOfTheFiveCategories() {
        assertThat(count("""
                SELECT COUNT(*) FROM tickets t LEFT JOIN orders o ON o.id = t.order_id WHERE o.id IS NULL
                """)).isZero();
        assertThat(count("SELECT COUNT(DISTINCT category) FROM tickets")).isEqualTo(5);
        assertThat(count("SELECT COUNT(*) FROM tickets WHERE status = 'open' AND closed_at IS NOT NULL")).isZero();
    }
}
