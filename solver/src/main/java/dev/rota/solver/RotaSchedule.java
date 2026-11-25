package dev.rota.solver;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.HardMediumSoftScore;

import java.util.List;

/**
 * Planning solution: employees (facts + value range for {@link ShiftSeat#getEmployee()}) and
 * seats (entities). Score = HardMediumSoftScore (D2).
 *
 * <p>The value range is linked to the planning variable by type, without ids. UNVERIFIED (see
 * .agent/QUESTIONS.md, A8).
 */
@PlanningSolution
public class RotaSchedule {

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<PlanningEmployee> employees;

    @PlanningEntityCollectionProperty
    private List<ShiftSeat> seats;

    @PlanningScore
    private HardMediumSoftScore score;

    public RotaSchedule() {
    }

    public RotaSchedule(List<PlanningEmployee> employees, List<ShiftSeat> seats) {
        this.employees = employees;
        this.seats = seats;
    }

    public List<PlanningEmployee> getEmployees() {
        return employees;
    }

    public void setEmployees(List<PlanningEmployee> employees) {
        this.employees = employees;
    }

    public List<ShiftSeat> getSeats() {
        return seats;
    }

    public void setSeats(List<ShiftSeat> seats) {
        this.seats = seats;
    }

    public HardMediumSoftScore getScore() {
        return score;
    }

    public void setScore(HardMediumSoftScore score) {
        this.score = score;
    }
}
