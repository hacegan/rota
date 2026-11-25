package dev.rota.domain;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    @Test
    void domainMustNotDependOnSpring() {
        JavaClasses domainClasses = new ClassFileImporter().importPackages("dev.rota.domain");
        ArchRule rule = noClasses().should().dependOnClassesThat()
                .resideInAPackage("..org.springframework..");
        rule.check(domainClasses);
    }

    @Test
    void domainMustNotDependOnTimefold() {
        JavaClasses domainClasses = new ClassFileImporter().importPackages("dev.rota.domain");
        ArchRule rule = noClasses().should().dependOnClassesThat()
                .resideInAPackage("ai.timefold..");
        rule.check(domainClasses);
    }
}
