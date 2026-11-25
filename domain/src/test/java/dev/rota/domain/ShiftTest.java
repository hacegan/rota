package dev.rota.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ShiftTest {

    private static final TimeRange MORNING =
            new TimeRange(Instant.parse("2026-11-23T05:00:00Z"), Instant.parse("2026-11-23T11:00:00Z"));

    @Test
    void rejectsZeroHeadcount() {
        assertThrows(IllegalArgumentException.class, () -> new Shift("S01", MORNING, "barista", 0));
    }

    @Test
    void rejectsBlankSkill() {
        assertThrows(IllegalArgumentException.class, () -> new Shift("S01", MORNING, " ", 1));
    }
}
