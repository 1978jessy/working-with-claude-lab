package com.marlowefinch.ops;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The dashboard's notion of "today".
 *
 * The seed data covers the 90 days before 2026-09-21, so the clock is pinned to that
 * date (property {@code ops.today}) rather than read from the machine. Every default
 * date range and every "days until" figure is computed from this bean, which keeps the
 * numbers stable in the tests and identical on every laptop in the workshop.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${ops.today}") String today) {
        LocalDate date = LocalDate.parse(today);
        return Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    }
}
