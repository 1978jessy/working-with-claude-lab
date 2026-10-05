package com.marlowefinch.ops;

import java.time.Clock;
import java.time.LocalDate;

/**
 * A closed date range for the query endpoints.
 *
 * Both bounds default to "the last 30 days ending today". There is deliberately no
 * validation here: a malformed date throws from {@link LocalDate#parse} and surfaces as
 * a 500, and {@code from} after {@code to} simply matches nothing. See TODO-232.
 */
public record DateRange(LocalDate from, LocalDate to) {

    public static final int DEFAULT_DAYS = 30;

    public static DateRange resolve(String from, String to, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        LocalDate end = to == null || to.isBlank() ? today : LocalDate.parse(to);
        LocalDate start = from == null || from.isBlank() ? today.minusDays(DEFAULT_DAYS) : LocalDate.parse(from);
        return new DateRange(start, end);
    }
}
