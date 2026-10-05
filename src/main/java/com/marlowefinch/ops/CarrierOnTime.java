package com.marlowefinch.ops;

/** On-time delivery figures for one carrier. {@code rate} is on-time divided by delivered, 4 decimals. */
public record CarrierOnTime(String carrier, long delivered, long onTime, Double rate) {

    static Double rateOf(long onTime, long delivered) {
        if (delivered == 0) {
            return null;
        }
        return Math.round(10000.0 * onTime / delivered) / 10000.0;
    }
}
