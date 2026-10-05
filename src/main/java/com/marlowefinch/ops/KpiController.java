package com.marlowefinch.ops;

import java.time.Clock;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KpiController {

    private final DashboardRepository repository;
    private final Clock clock;

    public KpiController(DashboardRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @GetMapping("/api/kpis")
    public Kpis kpis(@RequestParam(required = false) String from,
                     @RequestParam(required = false) String to) {
        return repository.kpis(DateRange.resolve(from, to, clock));
    }
}
