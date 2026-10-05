package com.marlowefinch.ops;

import java.time.LocalDate;

public record LateDelivery(String orderRef, String carrier, LocalDate promisedDate, LocalDate deliveredDate, int daysLate) {
}
