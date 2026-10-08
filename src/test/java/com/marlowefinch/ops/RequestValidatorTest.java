package com.marlowefinch.ops;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Plain unit tests for the TODO-232 validator and DateRange.resolve; no Spring context. */
class RequestValidatorTest {

    private static final Clock CLOCK = Clock.fixed(
            LocalDate.parse("2026-09-21").atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Test
    void resolveDefaultsToTheLast30DaysEndingToday() {
        DateRange range = DateRange.resolve(null, null, CLOCK);
        assertThat(range).isEqualTo(new DateRange(LocalDate.parse("2026-08-22"), LocalDate.parse("2026-09-21")));
    }

    @Test
    void resolveTreatsBlankAsMissing() {
        assertThat(DateRange.resolve(" ", "", CLOCK)).isEqualTo(DateRange.resolve(null, null, CLOCK));
    }

    @Test
    void resolveAcceptsExplicitDatesAndTheInclusiveLimits() {
        assertThat(DateRange.resolve("2026-09-21", "2026-09-21", CLOCK))
                .isEqualTo(new DateRange(LocalDate.parse("2026-09-21"), LocalDate.parse("2026-09-21")));
        assertThat(DateRange.resolve("2026-01-01", "2027-01-01", CLOCK)).isNotNull();
    }

    @Test
    void resolveThrowsWithEveryProblemCollected() {
        assertThatThrownBy(() -> DateRange.resolve("nope", "also-nope", CLOCK))
                .isInstanceOfSatisfying(InvalidRequestException.class, e -> assertThat(e.errors())
                        .containsExactlyInAnyOrder("from must be an ISO date (YYYY-MM-DD)", "to must be an ISO date (YYYY-MM-DD)"));
        assertThatThrownBy(() -> DateRange.resolve("2026-09-21", "2026-09-01", CLOCK))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        e -> assertThat(e.errors()).containsExactly("from must be on or before to"));
        assertThatThrownBy(() -> DateRange.resolve("2026-01-01", "2027-01-02", CLOCK))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        e -> assertThat(e.errors()).containsExactly("the range may span at most 366 days"));
    }

    @Test
    void defaultedBoundCanStillConflictWithAnExplicitOne() {
        // to defaults to today (2026-09-21); from after that is out of order
        assertThatThrownBy(() -> DateRange.resolve("2026-10-01", null, CLOCK))
                .isInstanceOfSatisfying(InvalidRequestException.class,
                        e -> assertThat(e.errors()).containsExactly("from must be on or before to"));
    }

    @Test
    void limitUsesTheDefaultWhenMissingOrBlank() {
        List<String> errors = new ArrayList<>();
        assertThat(RequestValidator.limit(null, 20, errors)).isEqualTo(20);
        assertThat(RequestValidator.limit("  ", 20, errors)).isEqualTo(20);
        assertThat(errors).isEmpty();
    }

    @Test
    void limitAcceptsOneThrough500AndRejectsEverythingElse() {
        List<String> errors = new ArrayList<>();
        assertThat(RequestValidator.limit("1", 20, errors)).isEqualTo(1);
        assertThat(RequestValidator.limit("500", 20, errors)).isEqualTo(500);
        assertThat(errors).isEmpty();
        for (String bad : new String[] {"0", "501", "-1", "abc", "2.5", "99999999999"}) {
            assertThat(RequestValidator.limit(bad, 20, errors)).as(bad).isNull();
        }
        assertThat(errors).hasSize(6).containsOnly("limit must be an integer between 1 and 500");
    }

    @Test
    void throwIfAnyOnlyThrowsWhenThereAreErrors() {
        RequestValidator.throwIfAny(new ArrayList<>());
        assertThatThrownBy(() -> RequestValidator.throwIfAny(List.of("boom")))
                .isInstanceOf(InvalidRequestException.class);
    }
}
