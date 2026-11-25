package dev.rota.solver;

import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.test.ConstraintVerifier;
import org.junit.jupiter.api.Test;

import static dev.rota.solver.Seats.assign;
import static dev.rota.solver.Seats.schedule;
import static dev.rota.solver.Seats.seat;

/**
 * Probes for open Timefold questions. Each test states the expected answer; the local run
 * proves or refutes it, and the result goes into TIMEFOLD-VERIFIED.md.
 */
class TimefoldBehaviourTest {

    /** TIMEFOLD-VERIFIED.md UNVERIFIED #3: does forEach skip seats whose variable is empty? Expected: yes. */
    @Test
    void forEachSkipsUnassignedSeats() {
        ConstraintVerifier<ProbeConstraintProvider, RotaSchedule> verifier =
                ConstraintVerifier.build(new ProbeConstraintProvider(), RotaSchedule.class, ShiftSeat.class);
        RotaSchedule mini = schedule("mini");
        ShiftSeat assigned = assign(mini, "S01", "E1");
        ShiftSeat empty = seat(mini, "S02");
        verifier.verifyThat(ProbeConstraintProvider::countForEach).given(assigned, empty).penalizesBy(1);
    }

    /** Companion: forEachIncludingUnassigned sees both. */
    @Test
    void forEachIncludingUnassignedSeesEmptySeats() {
        ConstraintVerifier<ProbeConstraintProvider, RotaSchedule> verifier =
                ConstraintVerifier.build(new ProbeConstraintProvider(), RotaSchedule.class, ShiftSeat.class);
        RotaSchedule mini = schedule("mini");
        ShiftSeat assigned = assign(mini, "S01", "E1");
        ShiftSeat empty = seat(mini, "S02");
        verifier.verifyThat(ProbeConstraintProvider::countForEachIncludingUnassigned)
                .given(assigned, empty).penalizesBy(2);
    }

    static final class ProbeConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory factory) {
            return new Constraint[] {countForEach(factory), countForEachIncludingUnassigned(factory)};
        }

        Constraint countForEach(ConstraintFactory factory) {
            return factory.forEach(ShiftSeat.class)
                    .penalize(Scores.ONE_SOFT)
                    .asConstraint("probe forEach");
        }

        Constraint countForEachIncludingUnassigned(ConstraintFactory factory) {
            return factory.forEachIncludingUnassigned(ShiftSeat.class)
                    .penalize(Scores.ONE_SOFT)
                    .asConstraint("probe forEachIncludingUnassigned");
        }
    }
}
