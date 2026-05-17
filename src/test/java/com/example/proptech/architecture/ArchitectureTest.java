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
    @DisplayName("legacyFullAddress 필드는 재대입하지 않는다")
    void legacy_full_address_field_should_not_be_reassigned() {

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
    @DisplayName("controller는 repository 클래스를 직접 사용하지 않는다")
    void controller_should_not_use_repository_classes() {
        ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..controller..")
            .should().accessClassesThat().resideInAPackage("..repository..")
            .check(importedClasses);
    }


    @Test
    @DisplayName("controller는 repository 패키지에 의존하지 않는다")
    void controller_should_not_depend_on_repository_package() {
        ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .check(importedClasses);
    }

    @Test
    @DisplayName("domain에는 setter 메서드를 선언하지 않는다")
    void domain_should_not_declare_setter_methods() {
        ArchRuleDefinition.noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
            .should().haveNameMatching("set[A-Z].*")
            .allowEmptyShould(true)
            .check(importedClasses);
    }

    @Test
    @DisplayName("특정 필드 getter 메서드 선언 금지")
    void address_master_should_not_declare_legacy_full_address_getter() {
        ArchRuleDefinition.noMethods()
            .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
            .should().haveName("getLegacyFullAddress")
            .allowEmptyShould(true)
            .check(addressMasterClasses);
    }


    @Test
    @DisplayName("특정 필드 getter 메서드 호출 금지")
    void legacy_full_address_getter_should_not_be_called() {
        noClasses()
            .should().callMethod(AddressMaster.class, "getLegacyFullAddress")
            .allowEmptyShould(true)
            .check(importedClasses);
    }

    @Test
    @DisplayName("legacy 계열 필드는 바이트코드 수준에서 재대입하지 않는다")
    void legacy_fields_should_not_be_reassigned_by_bytecode_access() {

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
