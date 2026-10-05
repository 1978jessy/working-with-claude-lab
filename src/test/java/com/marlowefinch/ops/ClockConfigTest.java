package com.marlowefinch.ops;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("demo")
class ClockConfigTest {

    @Autowired
    private Clock clock;

    @Test
    void todayIsPinnedToTheSeedDate() {
        assertThat(LocalDate.now(clock)).isEqualTo(LocalDate.parse("2026-09-21"));
    }
}
