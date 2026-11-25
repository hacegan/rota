package dev.rota.application;

import dev.rota.domain.RosterAssignment;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.SickCall;

import java.util.List;

/**
 * Source of named fixtures ({@code fixtures/<name>}). Implemented in bootstrap over the files.
 * Callers pass only names they have already allowed; implementations still reject unsafe names.
 */
public interface FixturePort {

    SchedulingProblem problem(String fixture);

    /** {@code published-roster.json} of the fixture. */
    List<RosterAssignment> publishedRoster(String fixture);

    /** {@code sick-call.json} of the fixture. */
    SickCall sickCall(String fixture);
}
