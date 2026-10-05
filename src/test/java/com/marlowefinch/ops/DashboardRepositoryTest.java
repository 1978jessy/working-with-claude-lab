package com.marlowefinch.ops;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** The queries against the seed data on H2. Expected figures come from the answer key written by tools/make_seed.py. */
@SpringBootTest
@ActiveProfiles("demo")
class DashboardRepositoryTest {

    private static final DateRange LAST_30_DAYS = new DateRange(LocalDate.parse("2026-08-22"), LocalDate.parse("2026-09-21"));
    private static final DateRange LAST_7_DAYS = new DateRange(LocalDate.parse("2026-09-14"), LocalDate.parse("2026-09-21"));
    private static final DateRange BACKWARDS = new DateRange(LocalDate.parse("2026-09-21"), LocalDate.parse("2026-08-22"));

    @Autowired
    private DashboardRepository repository;

    @Autowired
    private VendorRepository vendors;

    @Test
    void kpisForTheLast30DaysMatchTheAnswerKey() {
        Kpis kpis = repository.kpis(LAST_30_DAYS);
        assertThat(kpis.deliveries()).isEqualTo(651);
        assertThat(kpis.onTimeDeliveries()).isEqualTo(610);
        assertThat(kpis.onTimeRate()).isEqualTo(0.937);
        assertThat(kpis.openTickets()).isEqualTo(114);
        assertThat(kpis.revenue()).isEqualByComparingTo(new BigDecimal("360095.50"));
        assertThat(kpis.orders()).isEqualTo(624);
    }

    @Test
    void kpisForABackwardsRangeAreEmptyRatherThanAnError() {
        Kpis kpis = repository.kpis(BACKWARDS);
        assertThat(kpis.deliveries()).isZero();
        assertThat(kpis.onTimeRate()).isNull();
        assertThat(kpis.openTickets()).isZero();
        assertThat(kpis.revenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(kpis.orders()).isZero();
    }

    @Test
    void onTimeByCarrierListsAllFourCarriersAlphabetically() {
        List<CarrierOnTime> rows = repository.onTimeByCarrier(LAST_30_DAYS);
        assertThat(rows).extracting(CarrierOnTime::carrier)
                .containsExactly("Harbour Express", "Kessler Logistics", "Northwind Freight", "Redwood Couriers");
        CarrierOnTime kessler = rows.get(1);
        assertThat(kessler.delivered()).isEqualTo(265);
        assertThat(kessler.onTime()).isEqualTo(238);
        assertThat(kessler.rate()).isEqualTo(0.8981);
    }

    @Test
    void kesslerHasTheLowestOnTimeRateAndTheOthersAreAbove95Percent() {
        List<CarrierOnTime> rows = repository.onTimeByCarrier(LAST_30_DAYS);
        CarrierOnTime worst = rows.stream().min(Comparator.comparing(CarrierOnTime::rate)).orElseThrow();
        assertThat(worst.carrier()).isEqualTo("Kessler Logistics");
        assertThat(rows.stream().filter(r -> !r.carrier().equals("Kessler Logistics")))
                .allSatisfy(r -> assertThat(r.rate()).isGreaterThan(0.95));
    }

    @Test
    void onTimeByCarrierForAnEmptyRangeStillListsEveryCarrierWithNoRate() {
        List<CarrierOnTime> rows = repository.onTimeByCarrier(BACKWARDS);
        assertThat(rows).hasSize(4);
        assertThat(rows).allSatisfy(r -> {
            assertThat(r.delivered()).isZero();
            assertThat(r.rate()).isNull();
        });
    }

    @Test
    void lateDeliveriesLastWeekWereMostlyKessler() {
        List<LateDelivery> late = repository.lateDeliveries(LAST_7_DAYS, 100);
        assertThat(late).hasSize(9);
        assertThat(late.stream().filter(d -> d.carrier().equals("Kessler Logistics")).count()).isEqualTo(6);
        assertThat(late).allSatisfy(d -> {
            assertThat(d.daysLate()).isPositive();
            assertThat(d.deliveredDate()).isAfter(d.promisedDate());
        });
    }

    @Test
    void lateDeliveriesAreWorstFirstAndRespectTheLimit() {
        List<LateDelivery> late = repository.lateDeliveries(LAST_30_DAYS, 5);
        assertThat(late).hasSize(5);
        for (int i = 1; i < late.size(); i++) {
            assertThat(late.get(i).daysLate()).isLessThanOrEqualTo(late.get(i - 1).daysLate());
        }
        assertThat(repository.lateDeliveries(BACKWARDS, 5)).isEmpty();
    }

    @Test
    void ticketsByCategoryCountsOpenAndTotalForTheRange() {
        List<TicketCategoryCount> rows = repository.ticketsByCategory(LAST_30_DAYS);
        assertThat(rows).hasSize(5);
        assertThat(rows.get(0).category()).isEqualTo("Delivery delay");
        assertThat(rows.get(0).open()).isEqualTo(41);
        assertThat(rows.get(0).total()).isEqualTo(90);
        assertThat(rows.stream().mapToLong(TicketCategoryCount::open).sum()).isEqualTo(114);
        for (int i = 1; i < rows.size(); i++) {
            assertThat(rows.get(i).open()).isLessThanOrEqualTo(rows.get(i - 1).open());
        }
    }

    @Test
    void vendorsAreSoonestContractEndFirstWithDaysUntilFromTheFixedClock() {
        List<Vendor> all = vendors.findAll();
        assertThat(all).hasSize(8);
        Vendor first = all.get(0);
        assertThat(first.name()).isEqualTo("Volta Parts GmbH");
        assertThat(first.daysUntilContractEnd()).isEqualTo(24);
        assertThat(first.inNoticeWindow()).isTrue();
        assertThat(all).extracting(Vendor::name).containsExactlyInAnyOrder(
                "Kessler Logistics", "BrightLeaf Packaging", "Volta Parts GmbH", "Northgate Insurance",
                "HelpSpark", "Pinecrest Staffing", "Lumen Creative", "CloudCart");
        assertThat(all.stream().filter(Vendor::inNoticeWindow)).extracting(Vendor::name)
                .containsExactly("Volta Parts GmbH", "Lumen Creative", "Kessler Logistics");
    }
}
