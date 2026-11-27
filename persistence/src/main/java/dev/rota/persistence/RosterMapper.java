package dev.rota.persistence;

import dev.rota.application.Roster;
import dev.rota.application.RosterPlan;
import dev.rota.domain.TimeRange;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

/**
 * Application {@link Roster} &lt;-&gt; {@link RosterEntity}. Lists keep their order (OrderColumn
 * {@code position}). Reasons and pinned seat ids are stored as newline-separated text; a value
 * that itself contains a newline is rejected instead of being silently split later.
 */
final class RosterMapper {

    private static final String SEPARATOR = "\n";

    private RosterMapper() {
    }

    static RosterEntity toEntity(Roster roster, Instant createdAt) {
        RosterPlan plan = roster.plan();
        RosterPlan.ScoreSummary score = plan.score();
        RosterEntity entity = new RosterEntity(roster.id(), roster.fixture(), roster.operation().name(),
                roster.zone().getId(), score.hard(), score.medium(), score.soft(),
                join(plan.pinnedSeatIds(), "pinned seat id"), createdAt);
        for (RosterPlan.Assignment a : plan.assignments()) {
            entity.assignments().add(new AssignmentRow(a.seatId(), a.time().start(), a.time().end(), a.skill(),
                    a.employeeId()));
        }
        for (RosterPlan.UnassignedSeat u : plan.unassigned()) {
            entity.unassigned().add(new UnassignedSeatRow(u.seatId(), u.time().start(), u.time().end(), u.skill(),
                    join(u.reasons(), "reason")));
        }
        for (RosterPlan.Change c : plan.changes()) {
            entity.changes().add(new ChangeRow(c.seatId(), c.before(), c.after(), c.kind().name()));
        }
        return entity;
    }

    static Roster toDomain(RosterEntity entity) {
        List<RosterPlan.Assignment> assignments = entity.assignments().stream()
                .map(r -> new RosterPlan.Assignment(r.seatId(), new TimeRange(r.startAt(), r.endAt()), r.skill(),
                        r.employeeId()))
                .toList();
        List<RosterPlan.UnassignedSeat> unassigned = entity.unassigned().stream()
                .map(r -> new RosterPlan.UnassignedSeat(r.seatId(), new TimeRange(r.startAt(), r.endAt()), r.skill(),
                        split(r.reasons())))
                .toList();
        List<RosterPlan.Change> changes = entity.changes().stream()
                .map(r -> new RosterPlan.Change(r.seatId(), r.beforeEmployeeId(), r.afterEmployeeId(),
                        RosterPlan.Change.Kind.valueOf(r.kind())))
                .toList();
        RosterPlan plan = new RosterPlan(
                new RosterPlan.ScoreSummary(entity.hardScore(), entity.mediumScore(), entity.softScore()),
                assignments, unassigned, changes, split(entity.pinnedSeatIds()));
        return new Roster(entity.id(), entity.fixture(), Roster.Operation.valueOf(entity.operation()),
                ZoneId.of(entity.zone()), plan);
    }

    static String join(List<String> values, String what) {
        for (String value : values) {
            if (value.isEmpty() || value.contains(SEPARATOR) || value.contains("\r")) {
                throw new IllegalArgumentException(what + " must be a non-empty single line: '" + value + "'");
            }
        }
        return String.join(SEPARATOR, values);
    }

    static List<String> split(String stored) {
        return stored.isEmpty() ? List.of() : Arrays.asList(stored.split(SEPARATOR, -1));
    }
}
