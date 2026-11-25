package dev.rota.solver;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import dev.rota.domain.TimeRange;

import java.time.LocalDate;

/**
 * Planning entity: one headcount seat of one shift. The planning variable is the employee;
 * an empty seat is {@code employee == null} (coverage gap, medium level).
 *
 * <p>Calendar facts (week, local date, night, weekend) are computed once by {@link PlanningMapper}
 * in the site zone, so constraint streams never touch time zones.
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
    /** ISO week-based-year * 100 + ISO week, in the site zone, of the seat start (H5, S4). */
    private int weekKey;
    /** Site-zone date of the seat start (S1). Null only for seats built with the short constructor. */
    private LocalDate localDate;
    /** D3: the seat overlaps 00:00-06:00 local on some day (S2n). */
    private boolean night;
    /** D3: ISO week key of the Saturday/Sunday the seat touches; 0 when not a weekend seat (S2w). */
    private int weekendKey;
    /** Employee id on this seat in previous-roster.json, or null (S3). */
    private String previousEmployeeId;

    @PlanningVariable(allowsUnassigned = true)
    private PlanningEmployee employee;

    public ShiftSeat() {
    }

    public ShiftSeat(String shiftId, int seatIndex, TimeRange time, String requiredSkill, int weekKey) {
        this(shiftId, seatIndex, time, requiredSkill, weekKey, null, false, 0, null);
    }

    public ShiftSeat(String shiftId, int seatIndex, TimeRange time, String requiredSkill, int weekKey,
                     LocalDate localDate, boolean night, int weekendKey, String previousEmployeeId) {
        this.seatId = shiftId + "#" + seatIndex;
        this.shiftId = shiftId;
        this.seatIndex = seatIndex;
        this.time = time;
        this.requiredSkill = requiredSkill;
        this.weekKey = weekKey;
        this.localDate = localDate;
        this.night = night;
        this.weekendKey = weekendKey;
        this.previousEmployeeId = previousEmployeeId;
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

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
    }

    public boolean isNight() {
        return night;
    }

    public void setNight(boolean night) {
        this.night = night;
    }

    /** 1 for a night seat, else 0; summed per employee for S2n. */
    public int getNightUnits() {
        return night ? 1 : 0;
    }

    public int getWeekendKey() {
        return weekendKey;
    }

    public void setWeekendKey(int weekendKey) {
        this.weekendKey = weekendKey;
    }

    public String getPreviousEmployeeId() {
        return previousEmployeeId;
    }

    public void setPreviousEmployeeId(String previousEmployeeId) {
        this.previousEmployeeId = previousEmployeeId;
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
