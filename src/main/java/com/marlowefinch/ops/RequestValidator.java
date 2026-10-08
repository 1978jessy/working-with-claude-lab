package com.marlowefinch.ops;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * The single place where query parameters are parsed and checked (TODO-232).
 * Problems are collected into the caller's list so one request can report several;
 * call {@link #throwIfAny} once everything has been checked.
 */
public final class RequestValidator {

    public static final int MAX_RANGE_DAYS = 366;
    public static final int MIN_LIMIT = 1;
    public static final int MAX_LIMIT = 500;

    private RequestValidator() {
    }

    /** Blank or null bounds take their defaults. The range is inclusive, so from=to is 1 day. */
    public static DateRange dateRange(String from, String to, Clock clock, List<String> errors) {
        LocalDate today = LocalDate.now(clock);
        LocalDate end = isBlank(to) ? today : parseDate(to, "to", errors);
        LocalDate start = isBlank(from) ? today.minusDays(DateRange.DEFAULT_DAYS) : parseDate(from, "from", errors);
        if (start == null || end == null) {
            return null;
        }
        if (start.isAfter(end)) {
            errors.add("from must be on or before to");
            return null;
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS) {
            errors.add("the range may span at most " + MAX_RANGE_DAYS + " days");
            return null;
        }
        return new DateRange(start, end);
    }

    /** Blank or null gives {@code defaultValue}; otherwise an integer in 1..500. Returns null if invalid. */
    public static Integer limit(String raw, int defaultValue, List<String> errors) {
        if (isBlank(raw)) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw);
            if (value >= MIN_LIMIT && value <= MAX_LIMIT) {
                return value;
            }
        } catch (NumberFormatException ignored) {
            // falls through to the error below
        }
        errors.add("limit must be an integer between " + MIN_LIMIT + " and " + MAX_LIMIT);
        return null;
    }

    public static void throwIfAny(List<String> errors) {
        if (!errors.isEmpty()) {
            throw new InvalidRequestException(errors);
        }
    }

    public static DateRange validDateRange(String from, String to, Clock clock) {
        List<String> errors = new ArrayList<>();
        DateRange range = dateRange(from, to, clock, errors);
        throwIfAny(errors);
        return range;
    }

    private static LocalDate parseDate(String raw, String name, List<String> errors) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            errors.add(name + " must be an ISO date (YYYY-MM-DD)");
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
