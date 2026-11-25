package dev.rota.bootstrap;

import dev.rota.application.Roster;
import dev.rota.application.RosterRepository;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** P06 roster store: process memory only, lost on restart. No database (P13 adds persistence). */
public final class InMemoryRosterRepository implements RosterRepository {

    private final Map<String, Roster> rosters = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public String nextId() {
        return "r" + sequence.incrementAndGet();
    }

    @Override
    public void save(Roster roster) {
        Objects.requireNonNull(roster, "roster");
        rosters.put(roster.id(), roster);
    }

    @Override
    public Optional<Roster> findById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(rosters.get(id));
    }
}
