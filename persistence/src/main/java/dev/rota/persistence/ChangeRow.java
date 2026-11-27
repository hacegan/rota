package dev.rota.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One row of table {@code roster_change} (V4); null employee = empty seat. */
@Embeddable
public class ChangeRow {

    @Column(name = "seat_id", nullable = false, length = 64)
    private String seatId;

    @Column(name = "before_employee_id", length = 64)
    private String beforeEmployeeId;

    @Column(name = "after_employee_id", length = 64)
    private String afterEmployeeId;

    @Column(name = "kind", nullable = false, length = 16)
    private String kind;

    protected ChangeRow() {
        // for JPA
    }

    ChangeRow(String seatId, String beforeEmployeeId, String afterEmployeeId, String kind) {
        this.seatId = seatId;
        this.beforeEmployeeId = beforeEmployeeId;
        this.afterEmployeeId = afterEmployeeId;
        this.kind = kind;
    }

    String seatId() {
        return seatId;
    }

    String beforeEmployeeId() {
        return beforeEmployeeId;
    }

    String afterEmployeeId() {
        return afterEmployeeId;
    }

    String kind() {
        return kind;
    }
}
