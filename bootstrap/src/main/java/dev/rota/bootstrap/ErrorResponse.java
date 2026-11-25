package dev.rota.bootstrap;

/** JSON error body (rota.yaml schema {@code Error}): a stable code plus a readable message. */
public record ErrorResponse(String error, String message) {

    public static final String UNKNOWN_FIXTURE = "unknown_fixture";
    public static final String MISSING_PARAMETER = "missing_parameter";
    public static final String ROSTER_NOT_FOUND = "roster_not_found";
}
