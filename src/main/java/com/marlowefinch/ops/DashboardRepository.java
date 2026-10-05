package com.marlowefinch.ops;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

/**
 * Read-only queries behind the dashboard. Plain SQL through Spring JDBC; no JPA.
 *
 * Conventions shared with the seed data and the answer key:
 * - delivery figures are by the date the parcel arrived ({@code delivered_date}),
 *   so deliveries still in transit are never counted;
 * - a delivery is on time when {@code delivered_date <= promised_date};
 * - orders are counted by {@code ordered_at}; revenue excludes cancelled orders;
 * - tickets are counted by {@code opened_at}.
 */
@Repository
public class DashboardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DashboardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static SqlParameterSource range(DateRange range) {
        return new MapSqlParameterSource()
                .addValue("from", range.from())
                .addValue("to", range.to());
    }

    public Kpis kpis(DateRange range) {
        Map<String, Object> delivery = jdbc.queryForMap("""
                SELECT COUNT(*) AS delivered,
                       COALESCE(SUM(CASE WHEN delivered_date <= promised_date THEN 1 ELSE 0 END), 0) AS on_time
                FROM deliveries
                WHERE delivered_date BETWEEN :from AND :to
                """, range(range));
        long delivered = ((Number) delivery.get("delivered")).longValue();
        long onTime = ((Number) delivery.get("on_time")).longValue();

        Long openTickets = jdbc.queryForObject("""
                SELECT COUNT(*) FROM tickets
                WHERE status = 'open' AND opened_at BETWEEN :from AND :to
                """, range(range), Long.class);

        Map<String, Object> orders = jdbc.queryForMap("""
                SELECT COUNT(*) AS orders,
                       COALESCE(SUM(CASE WHEN status <> 'cancelled' THEN amount ELSE 0 END), 0) AS revenue
                FROM orders
                WHERE ordered_at BETWEEN :from AND :to
                """, range(range));
        long orderCount = ((Number) orders.get("orders")).longValue();
        BigDecimal revenue = new BigDecimal(orders.get("revenue").toString()).setScale(2);

        return new Kpis(range.from(), range.to(), CarrierOnTime.rateOf(onTime, delivered),
                delivered, onTime, openTickets == null ? 0 : openTickets, revenue, orderCount);
    }

    public List<CarrierOnTime> onTimeByCarrier(DateRange range) {
        return jdbc.query("""
                SELECT c.name AS carrier,
                       COUNT(d.id) AS delivered,
                       COALESCE(SUM(CASE WHEN d.delivered_date <= d.promised_date THEN 1 ELSE 0 END), 0) AS on_time
                FROM carriers c
                LEFT JOIN deliveries d
                       ON d.carrier_id = c.id AND d.delivered_date BETWEEN :from AND :to
                GROUP BY c.name
                ORDER BY c.name
                """, range(range), (rs, i) -> {
            long delivered = rs.getLong("delivered");
            long onTime = rs.getLong("on_time");
            return new CarrierOnTime(rs.getString("carrier"), delivered, onTime, CarrierOnTime.rateOf(onTime, delivered));
        });
    }

    public List<LateDelivery> lateDeliveries(DateRange range, int limit) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", range.from())
                .addValue("to", range.to())
                .addValue("limit", limit);
        return jdbc.query("""
                SELECT o.order_ref, c.name AS carrier, d.promised_date, d.delivered_date, d.days_late
                FROM deliveries d
                JOIN orders o ON o.id = d.order_id
                JOIN carriers c ON c.id = d.carrier_id
                WHERE d.delivered_date BETWEEN :from AND :to
                  AND d.delivered_date > d.promised_date
                ORDER BY d.days_late DESC, d.delivered_date DESC, o.order_ref
                LIMIT :limit
                """, params, (rs, i) -> new LateDelivery(
                rs.getString("order_ref"),
                rs.getString("carrier"),
                rs.getObject("promised_date", LocalDate.class),
                rs.getObject("delivered_date", LocalDate.class),
                rs.getInt("days_late")));
    }

    public List<TicketCategoryCount> ticketsByCategory(DateRange range) {
        return jdbc.query("""
                SELECT category,
                       COALESCE(SUM(CASE WHEN status = 'open' THEN 1 ELSE 0 END), 0) AS open_count,
                       COUNT(*) AS total
                FROM tickets
                WHERE opened_at BETWEEN :from AND :to
                GROUP BY category
                ORDER BY open_count DESC, category
                """, range(range), (rs, i) -> new TicketCategoryCount(
                rs.getString("category"), rs.getLong("open_count"), rs.getLong("total")));
    }
}
