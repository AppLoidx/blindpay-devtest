package com.example.blindpay.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.example.blindpay");
    }

    // Rule 1: Controllers must not access Repositories directly
    @Test
    void controllers_must_not_access_repositories() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .check(importedClasses);
    }

    // Rule 2: Controllers must not contain @Service annotation
    @Test
    void controllers_must_not_be_services() {
        noClasses().that().resideInAPackage("..controller..")
                .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
                .check(importedClasses);
    }

    // Rule 3: @Transactional must not be on Controllers or Repositories
    @Test
    void transactional_not_on_controllers() {
        noClasses().that().resideInAPackage("..controller..")
                .should().beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
                .check(importedClasses);
    }

    @Test
    void transactional_not_on_repositories() {
        noClasses().that().resideInAPackage("..repository..")
                .should().beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
                .check(importedClasses);
    }

    // Rule 4: JPA @Entity classes must not appear in Controller dependencies
    @Test
    void controllers_must_not_depend_on_entities() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().areAnnotatedWith(jakarta.persistence.Entity.class)
                .check(importedClasses);
    }

    // Rule 5: All service classes must implement an interface
    @Test
    void services_should_have_interfaces() {
        classes().that().resideInAPackage("..service..")
                .and().areNotInterfaces()
                .and().doNotHaveSimpleName("CurlHttpClient")
                .should().implement(JavaClass.Predicates.INTERFACES)
                .check(importedClasses);
    }

    // Rule 6: No @Autowired on fields — constructor injection only
    @Test
    void no_field_injection() {
        noFields().should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                .as("Field injection is not allowed — use constructor injection via @RequiredArgsConstructor")
                .check(importedClasses);
    }

    // Rule 7: Classes in ..controller.. must be annotated with @RestController or @Controller
    @Test
    void controllers_must_be_annotated() {
        classes().that().resideInAPackage("..controller..")
                .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .orShould().beAnnotatedWith(org.springframework.stereotype.Controller.class)
                .check(importedClasses);
    }

    // Rule 8: Classes in ..repository.. must extend JpaRepository or CrudRepository
    @Test
    void repositories_must_extend_jpa_or_crud() {
        classes().that().resideInAPackage("..repository..")
                .should().beAssignableTo(org.springframework.data.repository.CrudRepository.class)
                .check(importedClasses);
    }

    // Rule 9: No class should depend on more than 8 other project classes (SRP check)
    @Test
    void single_responsibility_check() {
        classes().that().resideInAPackage("com.example.blindpay..")
                .should(new ArchCondition<JavaClass>("not depend on more than 8 other project classes") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                        long count = javaClass.getDirectDependenciesFromSelf().stream()
                                .map(dep -> dep.getTargetClass().getFullName())
                                .filter(name -> name.startsWith("com.example.blindpay"))
                                .distinct()
                                .count();
                        if (count > 8) {
                            events.add(SimpleConditionEvent.violated(
                                    javaClass,
                                    String.format("%s depends on %d project classes (max 8)", javaClass.getName(), count)));
                        }
                    }
                })
                .check(importedClasses);
    }

    // Rule 10: Classes in ..service.impl.. must implement an interface from ..service..
    @Test
    void service_impl_must_implement_service_interface() {
        classes().that().resideInAPackage("..service.impl..")
                .should().implement(JavaClass.Predicates.resideInAPackage("..service.."))
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    // Additional: @Transactional methods should not be in controllers
    @Test
    void no_transactional_methods_in_controllers() {
        noMethods().that().areDeclaredInClassesThat().resideInAPackage("..controller..")
                .should().beAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
                .check(importedClasses);
    }
}
