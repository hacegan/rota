package dev.rota.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.Instant;

/** One row of table {@code assignment} (V2); position and roster_id come from the owning collection. */
@Embeddable
public class AssignmentRow {

    @Column(name = "seat_id", nullable = false, length = 64)
    private String seatId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "skill", length = 64)
    private String skill;

    @Column(name = "employee_id", nullable = false, length = 64)
    private String employeeId;

    protected AssignmentRow() {
        // for JPA
    }

    AssignmentRow(String seatId, Instant startAt, Instant endAt, String skill, String employeeId) {
        this.seatId = seatId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.skill = skill;
        this.employeeId = employeeId;
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

    String employeeId() {
        return employeeId;
    }
}
