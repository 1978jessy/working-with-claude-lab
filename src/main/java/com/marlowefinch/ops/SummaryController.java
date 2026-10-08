package com.marlowefinch.ops;

import java.time.Clock;
import java.util.Comparator;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SummaryController {

    private final DashboardRepository repository;
    private final Clock clock;

    public SummaryController(DashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @GetMapping("/api/summary")
    public Summary summary(@RequestParam(required = false) String from,
                           @RequestParam(required = false) String to) {
        DateRange range = DateRange.resolve(from, to, clock);
        Kpis kpis = repository.kpis(range);

        // Ties are broken by name so the answer is stable.
        String worstCarrier = repository.onTimeByCarrier(range).stream()
                .filter(c -> c.rate() != null)
                .min(Comparator.comparing(CarrierOnTime::rate).thenComparing(CarrierOnTime::carrier))
                .map(CarrierOnTime::carrier)
                .orElse(null);

        String busiestCategory = repository.ticketsByCategory(range).stream()
                .filter(t -> t.total() > 0)
                .max(Comparator.comparingLong(TicketCategoryCount::total)
                        .thenComparing(TicketCategoryCount::category, Comparator.reverseOrder()))
                .map(TicketCategoryCount::category)
                .orElse(null);

        return new Summary(kpis.from(), kpis.to(), kpis.onTimeRate(), kpis.openTickets(),
                kpis.revenue(), kpis.orders(), worstCarrier, busiestCategory);
    }
}
