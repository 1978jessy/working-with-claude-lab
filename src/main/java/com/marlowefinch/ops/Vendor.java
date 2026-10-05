package com.marlowefinch.ops;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Vendor(
        long id,
        String name,
        String category,
        BigDecimal annualSpend,
        LocalDate contractEnd,
        int noticeDays,
        String owner,
        long daysUntilContractEnd,
        boolean inNoticeWindow) {
}
