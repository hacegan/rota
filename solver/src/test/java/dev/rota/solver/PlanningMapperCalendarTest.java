package dev.rota.solver;

import dev.rota.domain.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static dev.rota.solver.Seats.employee;
import static dev.rota.solver.Seats.schedule;
import static dev.rota.solver.Seats.seat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * D3 calendar facts computed by {@link PlanningMapper} (night, weekend) and the P04 inputs it maps
 * from fixtures/mini (contract hours, preferences, previous roster).
 */
class PlanningMapperCalendarTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Istanbul");

    @Test
    void nightIsAnyOverlapWithMidnightToSix() {
        assertTrue(PlanningMapper.isNight(local("2026-11-23", "22:00", "2026-11-24", "06:00"), ZONE));
        assertTrue(PlanningMapper.isNight(local("2026-11-24", "00:00", "2026-11-24", "06:00"), ZONE));
        assertTrue(PlanningMapper.isNight(local("2026-11-24", "05:00", "2026-11-24", "09:00"), ZONE));
        assertFalse(PlanningMapper.isNight(local("2026-11-24", "06:00", "2026-11-24", "14:00"), ZONE));
        assertFalse(PlanningMapper.isNight(local("2026-11-24", "08:00", "2026-11-24", "14:00"), ZONE));
    }

    @Test
    void weekendKeyIsTheIsoWeekOfTheSaturdayOrSundayTouched() {
        assertEquals(202648, PlanningMapper.weekendKey(local("2026-11-28", "08:00", "2026-11-28", "14:00"), ZONE));
        assertEquals(202648, PlanningMapper.weekendKey(local("2026-11-29", "08:00", "2026-11-29", "14:00"), ZONE));
        // Friday night into Saturday touches the weekend.
        assertEquals(202648, PlanningMapper.weekendKey(local("2026-11-27", "22:00", "2026-11-28", "02:00"), ZONE));
        // Friday ending exactly at Saturday 00:00 does not.
        assertEquals(0, PlanningMapper.weekendKey(local("2026-11-27", "18:00", "2026-11-28", "00:00"), ZONE));
        assertEquals(0, PlanningMapper.weekendKey(local("2026-11-23", "08:00", "2026-11-23", "14:00"), ZONE));
    }

    @Test
    void mapsMiniP04Inputs() {
        RotaSchedule mini = schedule("mini");
        assertEquals(40 * 60, employee(mini, "E1").getContractMinutesPerWeek());
        assertEquals(24 * 60, employee(mini, "E3").getContractMinutesForHorizon()); // 1-week horizon
        assertTrue(employee(mini, "E2").avoids(LocalDate.parse("2026-11-27")));
        assertEquals(1, mini.getPreferences().size());
        assertEquals("E1", mini.getPreferences().get(0).getEmployee().getId());
        assertEquals(LocalDate.parse("2026-11-23"), mini.getPreferences().get(0).getDate());
        assertEquals("E2", seat(mini, "S04").getPreviousEmployeeId());
        assertNull(seat(mini, "S01").getPreviousEmployeeId());
        assertEquals(202648, seat(mini, "S11").getWeekendKey());
        assertEquals(0, seat(mini, "S01").getWeekendKey());
        assertFalse(seat(mini, "S01").isNight());
        assertEquals(LocalDate.parse("2026-11-27"), seat(mini, "S10").getLocalDate());
    }

    private static TimeRange local(String fromDay, String fromTime, String toDay, String toTime) {
        return new TimeRange(
                LocalDate.parse(fromDay).atTime(LocalTime.parse(fromTime)).atZone(ZONE).toInstant(),
                LocalDate.parse(toDay).atTime(LocalTime.parse(toTime)).atZone(ZONE).toInstant());
    }
}
