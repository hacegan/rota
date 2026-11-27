package dev.rota.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Table {@code roster} (V1) with its seats and changes as ordered element collections
 * ({@code assignment}, {@code unassigned_seat}, {@code roster_change}; V2-V4). Only
 * {@link RosterMapper} creates or reads it; nothing outside this module sees JPA types.
 */
@Entity
@Table(name = "roster")
public class RosterEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "fixture", nullable = false, length = 64)
    private String fixture;

    @Column(name = "operation", nullable = false, length = 16)
    private String operation;

    @Column(name = "zone", nullable = false, length = 64)
    private String zone;

    @Column(name = "hard_score", nullable = false)
    private long hardScore;

    @Column(name = "medium_score", nullable = false)
    private long mediumScore;

    @Column(name = "soft_score", nullable = false)
    private long softScore;

    @Column(name = "pinned_seat_ids", nullable = false)
    private String pinnedSeatIds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "assignment", joinColumns = @JoinColumn(name = "roster_id"))
    @OrderColumn(name = "position")
    private List<AssignmentRow> assignments = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "unassigned_seat", joinColumns = @JoinColumn(name = "roster_id"))
    @OrderColumn(name = "position")
    private List<UnassignedSeatRow> unassigned = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "roster_change", joinColumns = @JoinColumn(name = "roster_id"))
    @OrderColumn(name = "position")
    private List<ChangeRow> changes = new ArrayList<>();

    protected RosterEntity() {
        // for JPA
    }

    RosterEntity(String id, String fixture, String operation, String zone, long hardScore, long mediumScore,
                 long softScore, String pinnedSeatIds, Instant createdAt) {
        this.id = id;
        this.fixture = fixture;
        this.operation = operation;
        this.zone = zone;
        this.hardScore = hardScore;
        this.mediumScore = mediumScore;
        this.softScore = softScore;
        this.pinnedSeatIds = pinnedSeatIds;
        this.createdAt = createdAt;
    }

    String id() {
        return id;
    }

    String fixture() {
        return fixture;
    }

    String operation() {
        return operation;
    }

    String zone() {
        return zone;
    }

    long hardScore() {
        return hardScore;
    }

    long mediumScore() {
        return mediumScore;
    }

    long softScore() {
        return softScore;
    }

    String pinnedSeatIds() {
        return pinnedSeatIds;
    }

    List<AssignmentRow> assignments() {
        return assignments;
    }

    List<UnassignedSeatRow> unassigned() {
        return unassigned;
    }

    List<ChangeRow> changes() {
        return changes;
    }
}
