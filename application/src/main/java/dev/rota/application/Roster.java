package dev.rota.application;

import java.time.ZoneId;
import java.util.Objects;

/** A stored roster: the plan a solve or repair produced for a fixture, under a generated id. */
public record Roster(String id, String fixture, Operation operation, ZoneId zone, RosterPlan plan) {

    public Roster {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fixture, "fixture");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(plan, "plan");
    }

    public enum Operation {
        SOLVE,
        REPAIR
    }
}
