package com.marlowefinch.ops;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VendorRepository {

    private final JdbcTemplate jdbc;
    private final Clock clock;

    public VendorRepository(JdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Every vendor, soonest contract end first, with the days left computed from the fixed clock. */
    public List<Vendor> findAll() {
        LocalDate today = LocalDate.now(clock);
        return jdbc.query("""
                SELECT id, name, category, annual_spend, contract_end, notice_days, owner
                FROM vendors
                ORDER BY contract_end, name
                """, (rs, i) -> {
            LocalDate contractEnd = rs.getObject("contract_end", LocalDate.class);
            int noticeDays = rs.getInt("notice_days");
            long daysUntil = ChronoUnit.DAYS.between(today, contractEnd);
            return new Vendor(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getBigDecimal("annual_spend"),
                    contractEnd,
                    noticeDays,
                    rs.getString("owner"),
                    daysUntil,
                    daysUntil <= noticeDays);
        });
    }
}
