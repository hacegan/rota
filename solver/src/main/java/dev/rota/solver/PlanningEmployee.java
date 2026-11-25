package dev.rota.solver;

import dev.rota.domain.TimeRange;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Problem fact: an employee as the solver sees it. Plain final class (not a record) so the
 * open question about records as facts (TIMEFOLD-VERIFIED.md UNVERIFIED #2) does not apply.
 * Equality is identity.
 */
public final class PlanningEmployee {

    private final String id;
    private final Set<String> skills;
    private final int maxMinutesPerWeek;
    private final int contractMinutesPerWeek;
    private final int horizonWeeks;
    private final Set<LocalDate> avoidDates;
    private final List<TimeRange> availability;

    public PlanningEmployee(String id, Set<String> skills, int maxMinutesPerWeek, List<TimeRange> availability) {
        this(id, skills, maxMinutesPerWeek, 0, 1, Set.of(), availability);
    }

    /**
     * @param contractMinutesPerWeek contracted minutes per week; 0 means no contract target (S4 off)
     * @param horizonWeeks           ISO weeks covered by the planning horizon (S4 unused-contract penalty)
     * @param avoidDates             site-zone dates the employee asked not to work (S1 AVOID)
     */
    public PlanningEmployee(String id, Set<String> skills, int maxMinutesPerWeek, int contractMinutesPerWeek,
                            int horizonWeeks, Set<LocalDate> avoidDates, List<TimeRange> availability) {
        this.id = Objects.requireNonNull(id, "id");
        this.skills = Set.copyOf(skills);
        this.maxMinutesPerWeek = maxMinutesPerWeek;
        this.contractMinutesPerWeek = contractMinutesPerWeek;
        this.horizonWeeks = horizonWeeks;
        this.avoidDates = Set.copyOf(avoidDates);
        this.availability = merge(availability);
    }

    public String getId() {
        return id;
    }

    public Set<String> getSkills() {
        return skills;
    }

    public int getMaxMinutesPerWeek() {
        return maxMinutesPerWeek;
    }

    public int getContractMinutesPerWeek() {
        return contractMinutesPerWeek;
    }

    /** Contract minutes over the whole horizon: penalty when the employee gets no seat at all (S4). */
    public int getContractMinutesForHorizon() {
        return Math.multiplyExact(contractMinutesPerWeek, horizonWeeks);
    }

    public boolean avoids(LocalDate date) {
        return date != null && avoidDates.contains(date);
    }

    /** Sorted, merged availability windows (overlapping or touching windows joined). */
    public List<TimeRange> getAvailability() {
        return availability;
    }

    public boolean hasSkill(String skill) {
        return skills.contains(skill);
    }

    /** Minutes of {@code time} not covered by any availability window. */
    public int unavailableMinutes(TimeRange time) {
        long covered = 0;
        for (TimeRange window : availability) {
            covered += window.overlapMinutes(time);
        }
        return (int) (time.minutes() - covered);
    }

    private static List<TimeRange> merge(List<TimeRange> windows) {
        List<TimeRange> sorted = new ArrayList<>(windows);
        sorted.sort(Comparator.comparing(TimeRange::start));
        List<TimeRange> merged = new ArrayList<>();
        for (TimeRange window : sorted) {
            if (!merged.isEmpty()) {
                TimeRange last = merged.get(merged.size() - 1);
                if (!window.start().isAfter(last.end())) {
                    if (window.end().isAfter(last.end())) {
                        merged.set(merged.size() - 1, new TimeRange(last.start(), window.end()));
                    }
                    continue;
                }
            }
            merged.add(window);
        }
        return List.copyOf(merged);
    }

    @Override
    public String toString() {
        return id;
    }
}
