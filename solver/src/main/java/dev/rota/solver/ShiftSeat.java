package dev.rota.solver;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import dev.rota.domain.TimeRange;

/**
 * Planning entity: one headcount seat of one shift. The planning variable is the employee;
 * an empty seat is {@code employee == null} (coverage gap, medium level).
 *
 * <p>Mutable class with a public no-arg constructor and setters, as assumed in
 * TIMEFOLD-VERIFIED.md UNVERIFIED #2.
 */
@PlanningEntity
public class ShiftSeat {

    @PlanningId
    private String seatId;
    private String shiftId;
    private int seatIndex;
    private TimeRange time;
    private String requiredSkill;
    /** ISO week-based-year * 100 + ISO week, in the site zone, of the seat start (H5). */
    private int weekKey;

    @PlanningVariable(allowsUnassigned = true)
    private PlanningEmployee employee;

    public ShiftSeat() {
    }

    public ShiftSeat(String shiftId, int seatIndex, TimeRange time, String requiredSkill, int weekKey) {
        this.seatId = shiftId + "#" + seatIndex;
        this.shiftId = shiftId;
        this.seatIndex = seatIndex;
        this.time = time;
        this.requiredSkill = requiredSkill;
        this.weekKey = weekKey;
    }

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public int getSeatIndex() {
        return seatIndex;
    }

    public void setSeatIndex(int seatIndex) {
        this.seatIndex = seatIndex;
    }

    public TimeRange getTime() {
        return time;
    }

    public void setTime(TimeRange time) {
        this.time = time;
    }

    public String getRequiredSkill() {
        return requiredSkill;
    }

    public void setRequiredSkill(String requiredSkill) {
        this.requiredSkill = requiredSkill;
    }

    public int getWeekKey() {
        return weekKey;
    }

    public void setWeekKey(int weekKey) {
        this.weekKey = weekKey;
    }

    public PlanningEmployee getEmployee() {
        return employee;
    }

    public void setEmployee(PlanningEmployee employee) {
        this.employee = employee;
    }

    public int getDurationMinutes() {
        return (int) time.minutes();
    }

    @Override
    public String toString() {
        return seatId + "->" + (employee == null ? "-" : employee.getId());
    }
}
