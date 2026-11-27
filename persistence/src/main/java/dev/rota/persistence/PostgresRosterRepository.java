package dev.rota.persistence;

import dev.rota.application.Roster;
import dev.rota.application.RosterRepository;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link RosterRepository} over PostgreSQL (profile {@code postgres}). Each call is one
 * transaction (programmatic, so no proxying of this class is needed); entities never leave it.
 */
public final class PostgresRosterRepository implements RosterRepository {

    private final RosterJpaRepository rows;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public PostgresRosterRepository(RosterJpaRepository rows, TransactionTemplate transactions, Clock clock) {
        this.rows = Objects.requireNonNull(rows, "rows");
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public String nextId() {
        Long number = transactions.execute(status -> rows.nextRosterNumber());
        return "r" + Objects.requireNonNull(number, "roster_id_seq returned no value");
    }

    /** Inserts, or replaces an existing roster with the same id (all its seats and changes). */
    @Override
    public void save(Roster roster) {
        Objects.requireNonNull(roster, "roster");
        RosterEntity entity = RosterMapper.toEntity(roster, clock.instant());
        transactions.executeWithoutResult(status -> rows.save(entity));
    }

    @Override
    public Optional<Roster> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        Optional<Roster> found = transactions.execute(status -> rows.findById(id).map(RosterMapper::toDomain));
        return found == null ? Optional.empty() : found;
    }
}
