package dev.rota.bootstrap;

import dev.rota.application.Roster;
import dev.rota.application.RosterPlan;
import dev.rota.domain.TimeRange;

import java.time.ZoneId;
import java.util.List;

/**
 * JSON body of a roster (docs/contracts/openapi/rota.yaml, schema {@code Roster}). Plain records
 * with String/long/boolean fields only, so the JSON shape does not depend on Jackson's java.time
 * settings. Times are ISO-8601 offset date-times in the roster's zone, e.g. {@code 2026-11-23T07:00+03:00}.
 */
public record RosterResponse(String id, String fixture, String operation, String zone, Score score,
                             List<Assignment> assignments, List<Unassigned> unassigned, List<Change> changes,
                             List<String> pinnedSeatIds) {

    public record Score(long hard, long medium, long soft, boolean feasible) {
    }

    public record Assignment(String seatId, String start, String end, String skill, String employeeId) {
    }

    public record Unassigned(String seatId, String start, String end, String skill, List<String> reasons) {
    }

    /** {@code before}/{@code after} are null for an empty seat (serialized as JSON null). */
    public record Change(String seatId, String before, String after, String kind) {
    }

    public static RosterResponse from(Roster roster) {
        ZoneId zone = roster.zone();
        RosterPlan plan = roster.plan();
        RosterPlan.ScoreSummary score = plan.score();
        return new RosterResponse(
                roster.id(),
                roster.fixture(),
                roster.operation().name(),
                zone.getId(),
                new Score(score.hard(), score.medium(), score.soft(), score.feasible()),
                plan.assignments().stream()
                        .map(a -> new Assignment(a.seatId(), start(a.time(), zone), end(a.time(), zone), a.skill(),
                                a.employeeId()))
                        .toList(),
                plan.unassigned().stream()
                        .map(u -> new Unassigned(u.seatId(), start(u.time(), zone), end(u.time(), zone), u.skill(),
                                u.reasons()))
                        .toList(),
                plan.changes().stream()
                        .map(c -> new Change(c.seatId(), c.before(), c.after(), c.kind().name()))
                        .toList(),
                plan.pinnedSeatIds());
    }

    private static String start(TimeRange time, ZoneId zone) {
        return time.start().atZone(zone).toOffsetDateTime().toString();
    }

    private static String end(TimeRange time, ZoneId zone) {
        return time.end().atZone(zone).toOffsetDateTime().toString();
    }
}
