package dev.rota.solver;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;

/**
 * Unit penalties per score level.
 *
 * <p>UNVERIFIED (see .agent/QUESTIONS.md, A4): built with the record's canonical constructor
 * {@code (structuralScore, hardScore, mediumScore, softScore)}. The component order comes from
 * javap in TIMEFOLD-VERIFIED.md; the constructor is public by JLS record rules. The meaning of
 * {@code structuralScore} is not verified; 0 is assumed neutral. This is the only place the
 * constructor is used.
 */
final class Scores {

    static final HardMediumSoftScore ONE_HARD = new HardMediumSoftScore(0L, 1L, 0L, 0L);
    static final HardMediumSoftScore ONE_MEDIUM = new HardMediumSoftScore(0L, 0L, 1L, 0L);
    static final HardMediumSoftScore ONE_SOFT = new HardMediumSoftScore(0L, 0L, 0L, 1L);

    private Scores() {
    }
}
