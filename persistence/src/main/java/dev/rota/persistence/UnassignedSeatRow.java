package dev.rota.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

/** One row of table {@code unassigned_seat} (V3); reasons are newline-separated (see RosterMapper). */
@Embeddable
public class UnassignedSeatRow {

    @Column(name = "seat_id", nullable = false, length = 64)
    private String seatId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "skill", length = 64)
    private String skill;

    @Column(name = "reasons", nullable = false)
    private String reasons;

    protected UnassignedSeatRow() {
        // for JPA
    }

    UnassignedSeatRow(String seatId, Instant startAt, Instant endAt, String skill, String reasons) {
        this.seatId = seatId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.skill = skill;
        this.reasons = reasons;
    }

    String seatId() {
        return seatId;
    }

    Instant startAt() {
        return startAt;
    }

    Instant endAt() {
        return endAt;
    }

    String skill() {
        return skill;
    }

    String reasons() {
        return reasons;
    }
}
