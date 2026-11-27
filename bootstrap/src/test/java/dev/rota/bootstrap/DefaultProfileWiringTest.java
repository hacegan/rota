package dev.rota.bootstrap;

import dev.rota.application.RosterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** Without profile "postgres" the API keeps the P06-P08 in-memory store and needs no database. */
@SpringBootTest
class DefaultProfileWiringTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void rosterRepositoryIsInMemory() {
        assertInstanceOf(InMemoryRosterRepository.class, context.getBean(RosterRepository.class));
    }

    @Test
    void noDataSourceInDefaultProfile() {
        assertEquals(0, context.getBeanNamesForType(DataSource.class).length);
    }
}
