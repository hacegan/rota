package dev.rota.application;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Use cases and ports stay pure Java: no Spring, no Timefold. The domain module has the same two
 * rules in its own ArchitectureTest.
 */
class ApplicationArchitectureTest {

    private static final JavaClasses APPLICATION = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("dev.rota.application");

    @Test
    void applicationMustNotDependOnSpring() {
        ArchRule rule = noClasses().should().dependOnClassesThat().resideInAPackage("org.springframework..");
        rule.check(APPLICATION);
    }

    @Test
    void applicationMustNotDependOnTimefold() {
        ArchRule rule = noClasses().should().dependOnClassesThat().resideInAPackage("ai.timefold..");
        rule.check(APPLICATION);
    }

    @Test
    void applicationMustNotDependOnSolverOrBootstrap() {
        ArchRule rule = noClasses().should().dependOnClassesThat()
                .resideInAnyPackage("dev.rota.solver..", "dev.rota.bootstrap..");
        rule.check(APPLICATION);
    }
}
