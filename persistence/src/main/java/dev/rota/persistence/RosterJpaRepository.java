package dev.rota.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Spring Data repository for {@link RosterEntity}; used only by {@link PostgresRosterRepository}. */
public interface RosterJpaRepository extends JpaRepository<RosterEntity, String> {

    /** Next value of {@code roster_id_seq} (V1). */
    @Query(value = "select nextval('roster_id_seq')", nativeQuery = true)
    Long nextRosterNumber();
}
