package dev.rota.solver;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;

/**
 * Soft-level constraint weights (P04). PLACEHOLDERS, not calibrated: the order follows
 * CONSTRAINT-MODEL.md §2 (stability above contract above fairness above preferences), the
 * numbers do not. Weighers return natural units (seats, minutes, scaled squared counts), so
 * ConstraintVerifier match weights stay readable; the weight lives only here.
 */
final class SoftWeights {

    /** S1: per avoided seat, and per preferred date with no seat. */
    static final HardMediumSoftScore PREFERENCE = Scores.soft(50);
    /** S2n / S2w: weigher is already scaled by {@link #FAIRNESS_SCALE}. */
    static final HardMediumSoftScore FAIRNESS = Scores.soft(1);
    /** S3: per seat whose employee differs from previous-roster.json. */
    static final HardMediumSoftScore STABILITY = Scores.soft(1000);
    /** S4: per minute of deviation from contract hours. */
    static final HardMediumSoftScore CONTRACT = Scores.soft(1);

    /** S2n / S2w: penalty = count² × FAIRNESS_SCALE / max hours per week (integer division). */
    static final long FAIRNESS_SCALE = 1000L;

    private SoftWeights() {
    }
}
