package com.marlowefinch.ops;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The four headline numbers for a date range. {@code onTimeRate} is null when nothing was delivered. */
public record Kpis(
        LocalDate from,
        LocalDate to,
        Double onTimeRate,
        long deliveries,
        long onTimeDeliveries,
        long openTickets,
        BigDecimal revenue,
        long orders) {
}
