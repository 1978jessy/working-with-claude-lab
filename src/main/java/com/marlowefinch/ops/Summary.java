package com.marlowefinch.ops;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The four KPIs plus the weakest carrier and the busiest ticket category. Both names are null for an empty range. */
public record Summary(
        LocalDate from,
        LocalDate to,
        Double onTimeRate,
        long openTickets,
        BigDecimal revenue,
        long orders,
        String worstCarrier,
        String busiestTicketCategory) {
}
