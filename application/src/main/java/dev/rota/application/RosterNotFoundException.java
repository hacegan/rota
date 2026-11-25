package dev.rota.application;

/** No stored roster has the requested id. */
public final class RosterNotFoundException extends RuntimeException {

    public RosterNotFoundException(String id) {
        super("roster not found: " + id);
    }
}
