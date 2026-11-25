package dev.rota.application;

import java.util.Objects;

/** Use case: read a stored roster by id. */
public final class GetRoster {

    private final RosterRepository rosters;

    public GetRoster(RosterRepository rosters) {
        this.rosters = Objects.requireNonNull(rosters, "rosters");
    }

    public Roster get(String id) {
        if (id == null) {
            throw new RosterNotFoundException("null");
        }
        return rosters.findById(id).orElseThrow(() -> new RosterNotFoundException(id));
    }
}
