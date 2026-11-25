package dev.rota.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeRangeTest {

    private static TimeRange range(String start, String end) {
        return new TimeRange(Instant.parse(start), Instant.parse(end));
    }

    @Test
    void rejectsEndNotAfterStart() {
        assertThrows(IllegalArgumentException.class,
                () -> range("2026-11-23T08:00:00Z", "2026-11-23T08:00:00Z"));
    }

    @Test
    void backToBackRangesDoNotOverlap() {
        TimeRange morning = range("2026-11-23T05:00:00Z", "2026-11-23T11:00:00Z");
        TimeRange afternoon = range("2026-11-23T11:00:00Z", "2026-11-23T17:00:00Z");
        assertFalse(morning.overlaps(afternoon));
        assertEquals(0, morning.overlapMinutes(afternoon));
        assertEquals(0, morning.gapMinutesTo(afternoon));
    }

    @Test
    void overlapMinutesIsTheIntersection() {
        TimeRange a = range("2026-11-23T05:00:00Z", "2026-11-23T11:00:00Z");
        TimeRange b = range("2026-11-23T09:00:00Z", "2026-11-23T17:00:00Z");
        assertEquals(120, a.overlapMinutes(b));
    }
}
