package com.marlowefinch.ops;

import java.time.Clock;
import java.time.LocalDate;

/**
 * A closed date range for the query endpoints.
 *
 * Both bounds default to "the last 30 days ending today". The record itself does not
 * validate (repositories and tests build arbitrary ranges); {@link #resolve} does, via
 * {@link RequestValidator}, and throws {@link InvalidRequestException} (a 400) for a
 * malformed date, {@code from} after {@code to}, or a span over 366 days. See TODO-232.
 */
public record DateRange(LocalDate from, LocalDate to) {

    public static final int DEFAULT_DAYS = 30;

    public static DateRange resolve(String from, String to, Clock clock) {
        return RequestValidator.validDateRange(from, to, clock);
    }
}
