package dev.rota.solver;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;

/**
 * Unit penalties per score level.
 *
 * <p>Built with the record's canonical constructor {@code (structuralScore, hardScore, mediumScore,
 * softScore)}. Component order comes from javap in TIMEFOLD-VERIFIED.md; the constructor compiled and
 * the P03 tests passed on the local run. The meaning of {@code structuralScore} is still not
 * documented; 0 is used. This is the only place the constructor is used.
 */
final class Scores {

    static final HardMediumSoftScore ONE_HARD = new HardMediumSoftScore(0L, 1L, 0L, 0L);
    static final HardMediumSoftScore ONE_MEDIUM = new HardMediumSoftScore(0L, 0L, 1L, 0L);
    static final HardMediumSoftScore ONE_SOFT = soft(1L);

    private Scores() {
    }

    static HardMediumSoftScore soft(long weight) {
        return new HardMediumSoftScore(0L, 0L, 0L, weight);
    }
}
