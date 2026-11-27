package dev.rota.persistence;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** The persistence adapter depends on application + domain only: no solver, no Timefold, no web. */
class PersistenceArchitectureTest {

    private static final JavaClasses PERSISTENCE = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("dev.rota.persistence");

    @Test
    void persistenceMustNotDependOnSolverBootstrapOrTimefold() {
        ArchRule rule = noClasses().should().dependOnClassesThat()
                .resideInAnyPackage("dev.rota.solver..", "dev.rota.bootstrap..", "ai.timefold..");
        rule.check(PERSISTENCE);
    }

    @Test
    void persistenceMustNotDependOnSpringWeb() {
        ArchRule rule = noClasses().should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework.web..", "org.springframework.http..");
        rule.check(PERSISTENCE);
    }
}
