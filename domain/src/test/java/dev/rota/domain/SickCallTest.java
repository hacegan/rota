package dev.rota.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SickCallTest {

    private static TimeRange range(String start, String end) {
        return new TimeRange(Instant.parse(start), Instant.parse(end));
    }

    @Test
    void effectivePeriodStartsAtTheReportWhenReportedDuringThePeriod() {
        SickCall call = new SickCall("E2", range("2026-11-24T21:00:00Z", "2026-11-25T21:00:00Z"),
                Instant.parse("2026-11-25T04:00:00Z"));
        assertEquals(Optional.of(range("2026-11-25T04:00:00Z", "2026-11-25T21:00:00Z")), call.effectivePeriod());
    }

    @Test
    void effectivePeriodIsWholePeriodWhenReportedBefore() {
        TimeRange period = range("2026-11-25T21:00:00Z", "2026-11-26T21:00:00Z");
        SickCall call = new SickCall("E2", period, Instant.parse("2026-11-25T04:00:00Z"));
        assertEquals(Optional.of(period), call.effectivePeriod());
    }

    @Test
    void effectivePeriodIsEmptyWhenReportedAfterThePeriod() {
        SickCall call = new SickCall("E2", range("2026-11-23T21:00:00Z", "2026-11-24T21:00:00Z"),
                Instant.parse("2026-11-25T04:00:00Z"));
        assertEquals(Optional.empty(), call.effectivePeriod());
    }

    @Test
    void withoutAvailabilityRemovesThePeriodFromEveryWindow() {
        Employee e2 = new Employee("E2", "Two", Set.of("cashier"), 30, 30, List.of(
                range("2026-11-25T05:00:00Z", "2026-11-25T17:00:00Z"),
                range("2026-11-26T05:00:00Z", "2026-11-26T17:00:00Z")));
        Employee sick = e2.withoutAvailability(range("2026-11-24T21:00:00Z", "2026-11-25T21:00:00Z"));
        assertEquals(List.of(range("2026-11-26T05:00:00Z", "2026-11-26T17:00:00Z")), sick.availability());
        assertEquals(e2.id(), sick.id());
        assertEquals(e2.contractHoursPerWeek(), sick.contractHoursPerWeek());
    }

    @Test
    void rosterAssignmentRejectsNegativeSeat() {
        assertThrows(IllegalArgumentException.class, () -> new RosterAssignment("S01", -1, "E1"));
        assertEquals("S01#0", new RosterAssignment("S01", 0, "E1").seatId());
    }
}
