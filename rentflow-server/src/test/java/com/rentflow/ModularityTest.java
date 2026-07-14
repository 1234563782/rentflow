package com.rentflow;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ModularityTest {
    @Test
    void verifiesModulithStructure() {
        ApplicationModules.of(RentFlowApplication.class).verify();
    }

    @Test
    void modulesDoNotAccessOtherModulesInfrastructure() {
        String[] modules = {"identity", "catalog", "pricing", "inventory", "ordering", "audit", "messaging"};
        var classes = new ClassFileImporter().importPackages("com.rentflow");
        for (String module : modules) {
            String[] forbiddenPackages = Arrays.stream(modules)
                    .filter(candidate -> !candidate.equals(module))
                    .map(candidate -> "com.rentflow." + candidate + ".infrastructure..")
                    .toArray(String[]::new);
            noClasses()
                    .that().resideInAPackage("com.rentflow." + module + "..")
                    .should().dependOnClassesThat().resideInAnyPackage(forbiddenPackages)
                    .check(classes);
        }
    }
}
