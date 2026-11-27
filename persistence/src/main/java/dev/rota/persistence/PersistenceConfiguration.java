package dev.rota.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.time.Clock;
import java.util.Map;

/**
 * PostgreSQL persistence, active only under Spring profile {@code postgres}. Wired by hand with
 * plain Spring/Hibernate/Flyway classes on purpose: this module brings no Spring Boot
 * auto-configuration, so the default (in-memory) profile never looks for a database
 * (QUESTIONS.md P09 P2).
 *
 * <p>Properties: {@code rota.datasource.url}, {@code rota.datasource.username},
 * {@code rota.datasource.password}. Migrations: {@code classpath:db/migration} (V1-V4), run at
 * startup before the EntityManagerFactory is built. Hibernate never changes the schema.
 */
@Configuration(proxyBeanMethods = false)
@Profile("postgres")
@EnableJpaRepositories(basePackageClasses = RosterJpaRepository.class,
        entityManagerFactoryRef = "rotaEntityManagerFactory",
        transactionManagerRef = "rotaTransactionManager")
public class PersistenceConfiguration {

    @Bean(destroyMethod = "close")
    HikariDataSource rotaDataSource(@Value("${rota.datasource.url}") String url,
                                    @Value("${rota.datasource.username}") String username,
                                    @Value("${rota.datasource.password:}") String password) {
        HikariConfig config = new HikariConfig();
        config.setPoolName("rota");
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }

    @Bean
    Flyway rotaFlyway(DataSource rotaDataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(rotaDataSource)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
        return flyway;
    }

    /** Takes the Flyway bean so the schema is migrated before Hibernate starts. */
    @Bean
    LocalContainerEntityManagerFactoryBean rotaEntityManagerFactory(DataSource rotaDataSource, Flyway rotaFlyway) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setPersistenceUnitName("rota");
        factory.setDataSource(rotaDataSource);
        factory.setPackagesToScan(RosterEntity.class.getPackageName());
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "none"));
        return factory;
    }

    @Bean
    PlatformTransactionManager rotaTransactionManager(EntityManagerFactory rotaEntityManagerFactory) {
        return new JpaTransactionManager(rotaEntityManagerFactory);
    }

    @Bean
    PostgresRosterRepository postgresRosterRepository(RosterJpaRepository rows,
                                                      PlatformTransactionManager rotaTransactionManager) {
        return new PostgresRosterRepository(rows, new TransactionTemplate(rotaTransactionManager), Clock.systemUTC());
    }
}
