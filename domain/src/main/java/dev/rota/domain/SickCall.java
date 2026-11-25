package dev.rota.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * An employee reports sick for {@code period} at {@code reportedAt} (P05). {@code reportedAt} is
 * also "now" for the repair: seats that started before it are history and never move.
 */
public record SickCall(String employeeId, TimeRange period, Instant reportedAt) {

    public SickCall {
        Objects.requireNonNull(employeeId, "employeeId");
        if (employeeId.isBlank()) {
            throw new IllegalArgumentException("employeeId must not be blank");
        }
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(reportedAt, "reportedAt");
    }

    /**
     * The part of the sick period from the report onward. The past is not rewritten, so the
     * employee stays available (for the record) before {@code reportedAt}. Empty if the report
     * came after the period ended.
     */
    public Optional<TimeRange> effectivePeriod() {
        Instant from = period.start().isAfter(reportedAt) ? period.start() : reportedAt;
        return from.isBefore(period.end()) ? Optional.of(new TimeRange(from, period.end())) : Optional.empty();
    }
}
