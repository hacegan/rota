package dev.rota.solver;

import dev.rota.solver.fixture.FixtureLoader;

/**
 * Test helpers: load a fixture into a planning schedule and look up seats/employees by id.
 */
final class Seats {

    private Seats() {
    }

    static RotaSchedule schedule(String fixture) {
        return PlanningMapper.toSchedule(FixtureLoader.load(fixture));
    }

    /** First seat (index 0) of the shift. */
    static ShiftSeat seat(RotaSchedule schedule, String shiftId) {
        return schedule.getSeats().stream()
                .filter(s -> s.getShiftId().equals(shiftId) && s.getSeatIndex() == 0)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no seat for shift " + shiftId));
    }

    static PlanningEmployee employee(RotaSchedule schedule, String employeeId) {
        return schedule.getEmployees().stream()
                .filter(e -> e.getId().equals(employeeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no employee " + employeeId));
    }

    static ShiftSeat assign(RotaSchedule schedule, String shiftId, String employeeId) {
        ShiftSeat seat = seat(schedule, shiftId);
        seat.setEmployee(employee(schedule, employeeId));
        return seat;
    }
}
