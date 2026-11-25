package dev.rota.bootstrap;

import dev.rota.application.FixturePort;
import dev.rota.domain.Employee;
import dev.rota.domain.Preference;
import dev.rota.domain.PreviousAssignment;
import dev.rota.domain.RosterAssignment;
import dev.rota.domain.SchedulingProblem;
import dev.rota.domain.Shift;
import dev.rota.domain.SickCall;
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
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * {@link FixturePort} over the repository's {@code fixtures/} folder (P06 seeding).
 *
 * <p>Same formats and defaults as the solver's test-only {@code FixtureLoader} (P03-P05): mini-style
 * (timezone, ISO offset availability, contract hours, preferences, previous roster) and tight-style
 * (no timezone, {@code day + HH:mm} availability). Missing timezone defaults to Europe/Istanbul (D3);
 * missing contract hours default to 0; a local end time not after its start is the next day.
 * Duplicated on purpose: main code cannot use test classes. Production ingestion with readable
 * parse errors is a later phase (ROADMAP P05 note); it replaces both copies.
 */
public final class FixtureFiles implements FixturePort {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Istanbul");

    /** Fixture names are plain folder names; nothing that could leave {@code fixtures/}. */
    private static final Pattern SAFE_NAME = Pattern.compile("[a-z0-9][a-z0-9-]*");

    private final Path fixturesDir;

    public FixtureFiles(Path fixturesDir) {
        this.fixturesDir = Objects.requireNonNull(fixturesDir, "fixturesDir").toAbsolutePath().normalize();
    }

    /**
     * The fixtures folder: {@code configured} if not blank, otherwise the first {@code fixtures/}
     * folder found walking up from the working directory (repo root or a module folder).
     */
    public static Path locate(String configured) {
        if (configured != null && !configured.isBlank()) {
            Path dir = Path.of(configured).toAbsolutePath().normalize();
            if (!Files.isDirectory(dir)) {
                throw new IllegalStateException("rota.fixtures-dir is not a directory: " + dir);
            }
            return dir;
        }
        Path start = Path.of("").toAbsolutePath();
        for (Path dir = start; dir != null; dir = dir.getParent()) {
            Path candidate = dir.resolve("fixtures");
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("fixtures/ not found above " + start + "; set rota.fixtures-dir");
    }

    public Path fixturesDir() {
        return fixturesDir;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SchedulingProblem problem(String fixture) {
        Path dir = dir(fixture);
        Map<String, Object> employeesJson = (Map<String, Object>) FixtureJson.parse(read(dir.resolve("employees.json")));
        Map<String, Object> availabilityJson = (Map<String, Object>) FixtureJson.parse(read(dir.resolve("availability.json")));
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

    /** {@code published-roster.json}: {@code {"assignments":[{"shift_id","seat","employee_id"}]}}. */
    @Override
    @SuppressWarnings("unchecked")
    public List<RosterAssignment> publishedRoster(String fixture) {
        Map<String, Object> json = (Map<String, Object>) FixtureJson.parse(read(dir(fixture).resolve("published-roster.json")));
        List<RosterAssignment> assignments = new ArrayList<>();
        for (Object item : (List<Object>) json.get("assignments")) {
            Map<String, Object> entry = (Map<String, Object>) item;
            Object seat = entry.get("seat");
            assignments.add(new RosterAssignment((String) entry.get("shift_id"),
                    seat == null ? 0 : ((BigDecimal) seat).intValueExact(), (String) entry.get("employee_id")));
        }
        return assignments;
    }

    /**
     * {@code sick-call.json}: {@code employee_id}, {@code reported_at} (ISO offset), and either
     * {@code date} (whole local day in the file's timezone) or {@code start}/{@code end} (ISO offset).
     */
    @Override
    @SuppressWarnings("unchecked")
    public SickCall sickCall(String fixture) {
        Map<String, Object> json = (Map<String, Object>) FixtureJson.parse(read(dir(fixture).resolve("sick-call.json")));
        Object zoneName = json.get("timezone");
        ZoneId zone = zoneName == null ? DEFAULT_ZONE : ZoneId.of((String) zoneName);
        TimeRange period;
        if (json.containsKey("date")) {
            LocalDate day = LocalDate.parse((String) json.get("date"));
            period = new TimeRange(day.atStartOfDay(zone).toInstant(), day.plusDays(1).atStartOfDay(zone).toInstant());
        } else {
            period = new TimeRange(OffsetDateTime.parse((String) json.get("start")).toInstant(),
                    OffsetDateTime.parse((String) json.get("end")).toInstant());
        }
        Instant reportedAt = OffsetDateTime.parse((String) json.get("reported_at")).toInstant();
        return new SickCall((String) json.get("employee_id"), period, reportedAt);
    }

    private Path dir(String fixture) {
        if (fixture == null || !SAFE_NAME.matcher(fixture).matches()) {
            throw new IllegalArgumentException("invalid fixture name: " + fixture);
        }
        Path dir = fixturesDir.resolve(fixture).normalize();
        if (!dir.startsWith(fixturesDir) || !Files.isDirectory(dir)) {
            throw new IllegalArgumentException("fixture not found: " + fixture);
        }
        return dir;
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
        Map<String, Object> json = (Map<String, Object>) FixtureJson.parse(read(file));
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
        Map<String, Object> json = (Map<String, Object>) FixtureJson.parse(read(file));
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
