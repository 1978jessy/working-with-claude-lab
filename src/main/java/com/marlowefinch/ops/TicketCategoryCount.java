package com.marlowefinch.ops;

/** Tickets opened in the range for one category: how many are still open, and how many in total. */
public record TicketCategoryCount(String category, long open, long total) {
}
