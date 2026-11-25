package dev.rota.application;

import java.util.Optional;

/** Stored rosters. P06 implementation is in memory (bootstrap); persistence comes later. */
public interface RosterRepository {

    /** A new id, unique within this repository. */
    String nextId();

    void save(Roster roster);

    Optional<Roster> findById(String id);
}
