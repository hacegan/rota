package dev.rota.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Half-open time interval {@code [start, end)}. Back-to-back ranges do not overlap.
 */
public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start: " + start + " .. " + end);
        }
    }

    public long minutes() {
        return Duration.between(start, end).toMinutes();
    }

    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public long overlapMinutes(TimeRange other) {
        if (!overlaps(other)) {
            return 0;
        }
        Instant from = start.isAfter(other.start) ? start : other.start;
        Instant to = end.isBefore(other.end) ? end : other.end;
        return Duration.between(from, to).toMinutes();
    }

    /** Minutes from the end of this range to the start of {@code other}; negative if they overlap. */
    public long gapMinutesTo(TimeRange other) {
        return Duration.between(end, other.start).toMinutes();
    }
}
