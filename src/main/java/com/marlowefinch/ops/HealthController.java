package com.marlowefinch.ops;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final Clock clock;

    public HealthController(Clock clock) {
        this.clock = clock;
    }

    /** Liveness, plus the date the dashboard treats as today so the frontend can build its default range. */
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "today", LocalDate.now(clock).toString());
    }
}
