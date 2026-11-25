package dev.rota.solver.fixture;

import dev.rota.domain.Employee;
import dev.rota.domain.Preference;
import dev.rota.domain.PreviousAssignment;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.Shift;
import dev.rota.domain.TimeRange;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test-only fixture reader (P05 owns real ingestion). Reads both fixture formats:
 * <ul>
 *   <li>mini: {@code timezone} field, availability as ISO offset datetimes (D3), optional
 *       {@code contract_hours_per_week}, {@code preferences.json}, {@code previous-roster.json} (D4)</li>
 *   <li>tight (kept unchanged, D1): no timezone, availability as {@code day + HH:mm}, no contract
 *       hours, no preferences, no previous roster</li>
 * </ul>
 * Missing timezone defaults to Europe/Istanbul (D3: fixtures use Europe/Istanbul). Missing
 * contract hours default to 0 (no contract target). A local end time not after its start time is
 * taken as the next day.
 */
public final class FixtureLoader {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Istanbul");

    private FixtureLoader() {
    }

    /** Finds {@code fixtures/<name>} by walking up from the working directory (surefire runs in solver/). */
    public static Path fixture(String name) {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            Path candidate = dir.resolve("fixtures").resolve(name);
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("fixtures/" + name + " not found above " + Path.of("").toAbsolutePath());
    }

    public static SchedulingProblem load(String name) {
        return load(fixture(name));
    }

    @SuppressWarnings("unchecked")
    public static SchedulingProblem load(Path dir) {
        Map<String, Object> employeesJson = (Map<String, Object>) MiniJson.parse(read(dir.resolve("employees.json")));
        Map<String, Object> availabilityJson = (Map<String, Object>) MiniJson.parse(read(dir.resolve("availability.json")));
        ZoneId zone = zone(employeesJson, availabilityJson);

        Map<String, List<TimeRange>> availability = new LinkedHashMap<>();
        for (Object item : (List<Object>) availabilityJson.get("availability")) {
            Map<String, Object> entry = (Map<String, Object>) item;
            String employeeId = (String) entry.get("employee_id");
            availability.computeIfAbsent(employeeId, k -> new ArrayList<>()).add(window(entry, zone));
        }

        List<Employee> employees = new ArrayList<>();
        for (Object item : (List<Object>) employeesJson.get("employees")) {
            Map<String, Object> entry = (Map<String, Object>) item;
            String id = (String) entry.get("id");
            List<String> skills = new ArrayList<>();
            for (Object skill : (List<Object>) entry.get("skills")) {
                skills.add((String) skill);
            }
            int maxHours = ((BigDecimal) entry.get("max_hours_per_week")).intValueExact();
            Object contract = entry.get("contract_hours_per_week");
            int contractHours = contract == null ? 0 : ((BigDecimal) contract).intValueExact();
            employees.add(new Employee(id, (String) entry.get("name"), new HashSet<>(skills), maxHours,
                    contractHours, availability.getOrDefault(id, List.of())));
        }

        return new SchedulingProblem(zone, employees, shifts(dir.resolve("shifts.csv"), zone),
                preferences(dir.resolve("preferences.json")), previousRoster(dir.resolve("previous-roster.json")));
    }

    private static ZoneId zone(Map<String, Object> employeesJson, Map<String, Object> availabilityJson) {
        Object zone = employeesJson.getOrDefault("timezone", availabilityJson.get("timezone"));
        return zone == null ? DEFAULT_ZONE : ZoneId.of((String) zone);
    }

    private static TimeRange window(Map<String, Object> entry, ZoneId zone) {
        if (entry.containsKey("day")) {
            return localRange(LocalDate.parse((String) entry.get("day")),
                    LocalTime.parse((String) entry.get("start")), LocalTime.parse((String) entry.get("end")), zone);
        }
        return new TimeRange(OffsetDateTime.parse((String) entry.get("start")).toInstant(),
                OffsetDateTime.parse((String) entry.get("end")).toInstant());
    }

    /** {@code {"preferences":[{"employee_id","kind":"PREFER|AVOID","target":"yyyy-MM-dd"}]}}; missing file = none. */
    @SuppressWarnings("unchecked")
    private static List<Preference> preferences(Path file) {
        List<Preference> preferences = new ArrayList<>();
        if (!Files.isRegularFile(file)) {
            return preferences;
        }
        Map<String, Object> json = (Map<String, Object>) MiniJson.parse(read(file));
        for (Object item : (List<Object>) json.get("preferences")) {
            Map<String, Object> entry = (Map<String, Object>) item;
            preferences.add(new Preference((String) entry.get("employee_id"),
                    Preference.Kind.valueOf((String) entry.get("kind")),
                    LocalDate.parse((String) entry.get("target"))));
        }
        return preferences;
    }

    /** {@code {"previous_roster":{"assignments":[{"shift_id","employee_id","pinned"}]}}}; missing file = none. */
    @SuppressWarnings("unchecked")
    private static List<PreviousAssignment> previousRoster(Path file) {
        List<PreviousAssignment> assignments = new ArrayList<>();
        if (!Files.isRegularFile(file)) {
            return assignments;
        }
        Map<String, Object> json = (Map<String, Object>) MiniJson.parse(read(file));
        Map<String, Object> roster = (Map<String, Object>) json.get("previous_roster");
        for (Object item : (List<Object>) roster.get("assignments")) {
            Map<String, Object> entry = (Map<String, Object>) item;
            assignments.add(new PreviousAssignment((String) entry.get("shift_id"),
                    (String) entry.get("employee_id"), Boolean.TRUE.equals(entry.get("pinned"))));
        }
        return assignments;
    }

    private static List<Shift> shifts(Path csv, ZoneId zone) {
        List<Shift> shifts = new ArrayList<>();
        List<String> lines = readLines(csv);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (line.isEmpty()) {
                continue;
            }
            String[] f = line.split(",");
            if (f.length != 6) {
                throw new IllegalArgumentException(csv + " line " + (i + 1) + ": expected 6 fields, got " + f.length);
            }
            TimeRange time = localRange(LocalDate.parse(f[1].strip()), LocalTime.parse(f[2].strip()),
                    LocalTime.parse(f[3].strip()), zone);
            shifts.add(new Shift(f[0].strip(), time, f[4].strip(), Integer.parseInt(f[5].strip())));
        }
        return shifts;
    }

    private static TimeRange localRange(LocalDate day, LocalTime start, LocalTime end, ZoneId zone) {
        Instant from = ZonedDateTime.of(day, start, zone).toInstant();
        LocalDate endDay = end.isAfter(start) ? day : day.plusDays(1);
        Instant to = ZonedDateTime.of(endDay, end, zone).toInstant();
        return new TimeRange(from, to);
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
