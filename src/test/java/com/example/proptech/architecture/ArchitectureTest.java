package com.example.proptech.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.example.proptech.domain.AddressMaster;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

    private final JavaClasses importedClasses =
        new ClassFileImporter().importPackages("com.example.proptech");

    private final JavaClasses addressMasterClasses = new ClassFileImporter().importClasses(
        AddressMaster.class);


    @Test
    void controller_should_not_access_repository_directly() {
        noClasses()
            .that().resideInAPackage("..controller..")
            .should().accessClassesThat().resideInAPackage("..repository..")
            .check(importedClasses);
    }

    @Test
    void check_byte_code_fields_not_update() {

        ArchCondition<JavaClass> notSetLegacyFullAddress =
            new ArchCondition<JavaClass>("not set legacyFullAddress") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    for (JavaFieldAccess access : item.getFieldAccessesFromSelf()) {
                        boolean isLegacyFields = access.getTarget().getName()
                            .equals("legacyFullAddress");
                        boolean isWrite = access.getAccessType() == AccessType.SET;

                        if (isLegacyFields && isWrite) {
                            events.add(
                                SimpleConditionEvent.violated(
                                    access,
                                    "change value in " + access.getTarget().getFullName()
                                )
                            );
                        }

                    }
                }
            };


        JavaClasses classes = new ClassFileImporter().importClasses(AddressMaster.class);

        ArchRuleDefinition.classes()
            .that().resideInAPackage("..domain..")
            .should(notSetLegacyFullAddress)
            .check(classes);
    }


    @Test
    @DisplayName("가벼운 제한")
    void practice_cannot_access_controller_to_repository() {
        ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..controller..")
            .should().accessClassesThat().resideInAPackage("..repository..")
            .check(importedClasses);
    }


    @Test
    @DisplayName("무거운 제한 - 필드 자체 선언 금지")
    void practice_cannot_access_controller_to_repository_should_not_have_fields() {
        ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .check(importedClasses);
    }

    @Test
    @DisplayName("domain class setter 금지")
    void practice_cannot_access_domain_not_have_setter() {
        ArchRuleDefinition.noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
            .should().haveNameMatching("set[A-Z].*")
            .allowEmptyShould(true)
            .check(importedClasses);
    }

    @Test
    @DisplayName("특정 필드 getter 메서드 선언 금지")
    void practice_cannot_access_getter() {
        ArchRuleDefinition.noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
            .should().haveName("getLegacyFullAddress")
            .allowEmptyShould(true)
            .check(addressMasterClasses);
    }


    @Test
    @DisplayName("특정 필드 getter 메서드 호출 금지")
    void practice_cannot_call_getter() {
        noClasses()
            .should().callMethod(AddressMaster.class, "getLegacyFullAddress")
            .allowEmptyShould(true)
            .check(importedClasses);
    }

    @Test
    @DisplayName("바이트 코드 수준에서 참조 금지 확인")
    void practice_byte_code_dont_access_field() {

        ArchCondition<JavaClass> noFieldAccessCondition = new ArchCondition<JavaClass>(
            "dont change legacyFields") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (JavaFieldAccess access : item.getFieldAccessesFromSelf()) {
                    boolean legacyFullAddress = access.getTarget().getName()
                        .matches("legacy.*");

                    boolean hasChangeCode = access.getAccessType().equals(AccessType.SET);

                    if (legacyFullAddress && hasChangeCode) {
                        events.add(SimpleConditionEvent.violated(
                            access,
                            "change value in " + access.getTarget().getFullName()
                        ));
                    }

                }
            }
        };

        JavaClasses javaClass = new ClassFileImporter().importClasses(AddressMaster.class);

        ArchRuleDefinition.classes()
            .that().resideInAPackage("..domain..")
            .should(noFieldAccessCondition)
            .check(javaClass);
    }
}
