package dev.rota.domain;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A person who can be scheduled to shifts.
 *
 * @param skills               skill codes the employee holds
 * @param maxHoursPerWeek      hard weekly cap (H5)
 * @param contractHoursPerWeek contracted weekly hours (D4, S4); 0 means no contract target
 * @param availability         datetime windows the employee can work in; may cross midnight (D3)
 */
public record Employee(String id, String name, Set<String> skills, int maxHoursPerWeek,
                       int contractHoursPerWeek, List<TimeRange> availability) {

    public Employee {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        Objects.requireNonNull(name, "name");
        skills = Set.copyOf(Objects.requireNonNull(skills, "skills"));
        if (maxHoursPerWeek < 0) {
            throw new IllegalArgumentException("maxHoursPerWeek must be >= 0: " + maxHoursPerWeek);
        }
        if (contractHoursPerWeek < 0) {
            throw new IllegalArgumentException("contractHoursPerWeek must be >= 0: " + contractHoursPerWeek);
        }
        availability = List.copyOf(Objects.requireNonNull(availability, "availability"));
    }

    public Employee(String id, String name) {
        this(id, name, Set.of(), 0, 0, List.of());
    }

    public boolean hasSkill(String skill) {
        return skills.contains(skill);
    }
}
