# ArchUnit Practice Lab

Java 코드의 아키텍처 규칙을 [ArchUnit](https://www.archunit.org/)으로 검사해보는 Gradle 연습 프로젝트입니다.

이 repo는 정답 프로젝트가 아니라, 규칙을 일부러 깨보고 실패 메시지를 읽으면서 ArchUnit 감각을 익히는 용도입니다.

## Quick Start

```bash
./gradlew test --no-daemon
```

ArchUnit 테스트만 실행:

```bash
./gradlew archUnitTest --no-daemon
```

특정 테스트 클래스만 실행:

```bash
./gradlew test --tests '*ArchitectureTest' --no-daemon
```

## Project Layout

```text
src/main/java/com/example/proptech/
  controller/   # API 진입 계층
  service/      # 유스케이스 계층
  repository/   # 데이터 접근 계층
  domain/       # 도메인 모델

src/test/java/com/example/proptech/architecture/
  ArchitectureTest.java
```

## Dependencies

[build.gradle](build.gradle)

```gradle
compileOnly "org.projectlombok:lombok:1.18.46"
annotationProcessor "org.projectlombok:lombok:1.18.46"

testImplementation "org.junit.jupiter:junit-jupiter:5.10.2"
testRuntimeOnly "org.junit.platform:junit-platform-launcher:1.10.2"
testImplementation "com.tngtech.archunit:archunit-junit5:1.3.0"
```

Lombok은 컴파일 시 `getXxx()` / `setXxx()` 같은 메서드를 생성합니다. ArchUnit은 컴파일된 class 파일을 분석하므로 Lombok이 생성한 메서드도 규칙으로 검사할 수 있습니다.

## Core Concepts

### JavaClasses

`JavaClasses`는 ArchUnit이 bytecode를 읽어서 만든 검사 대상 묶음입니다.

```java
private final JavaClasses importedClasses =
    new ClassFileImporter().importPackages("com.example.proptech");
```

패키지 전체를 검사합니다. controller, service, repository 간 의존성처럼 여러 클래스 관계를 볼 때 사용합니다.

```java
private final JavaClasses addressMasterClasses =
    new ClassFileImporter().importClasses(AddressMaster.class);
```

특정 클래스만 검사합니다. 특정 클래스의 getter, setter, 필드 규칙을 볼 때 사용합니다.

### accessClassesThat vs dependOnClassesThat

| API | 보는 것 | 예시 |
| --- | --- | --- |
| `accessClassesThat()` | 실제 코드에서 접근한 클래스 | `addressRepository.findByDong("hello")` |
| `dependOnClassesThat()` | 필드 타입, 생성자 파라미터, 메서드 호출 등 넓은 의존성 | `private final AddressRepository addressRepository` |

controller가 repository를 아예 알면 안 되는 규칙이면 `dependOnClassesThat()`이 더 적합합니다.

### noMethods vs callMethod

| API | 보는 것 | 의미 |
| --- | --- | --- |
| `noMethods()` | 메서드 선언 또는 Lombok이 생성한 메서드 | `getLegacyFullAddress()`가 있으면 실패 |
| `callMethod()` | 누군가 메서드를 호출했는지 | `address.getLegacyFullAddress()` 호출 시 실패 |

## Practice Rules

규칙은 [ArchitectureTest.java](src/test/java/com/example/proptech/architecture/ArchitectureTest.java)에 있습니다.

| Rule | What It Checks | Break It By |
| --- | --- | --- |
| `controller_should_not_use_repository_classes` | controller가 repository 클래스를 직접 사용하지 않음 | `AddressController`에서 `addressRepository.findByDong(...)` 호출 |
| `controller_should_not_depend_on_repository_package` | controller가 repository 패키지에 의존하지 않음 | `AddressController`에 `AddressRepository` 필드 추가 |
| `domain_should_not_declare_setter_methods` | domain에 `setXxx` 메서드가 없음 | domain 클래스에 Lombok `@Setter` 추가 |
| `address_master_should_not_declare_legacy_full_address_getter` | `AddressMaster`에 legacy getter가 없음 | `@Getter(AccessLevel.NONE)` 제거 |
| `legacy_full_address_getter_should_not_be_called` | 어떤 클래스도 legacy getter를 호출하지 않음 | service/repository에서 `getLegacyFullAddress()` 호출 |
| `legacy_fields_should_not_be_reassigned_by_bytecode_access` | legacy 계열 필드를 SET 하지 않음 | 검사 대상을 `AddressMasterWithUpdateFieldMethod.class`로 바꾸기 |

## Rule Examples

### Controller -> Repository 직접 접근 금지

```java
noClasses()
    .that().resideInAPackage("..controller..")
    .should().accessClassesThat().resideInAPackage("..repository..")
    .check(importedClasses);
```

`accessClassesThat()`은 실제 사용을 봅니다. 아래 주석을 풀면 실패합니다.

[AddressController.java](src/main/java/com/example/proptech/controller/AddressController.java)

```java
// public String accessByRepository(){
//     return addressRepository.findByDong("hello");
// }
```

### Controller -> Repository 의존성 금지

```java
noClasses()
    .that().resideInAPackage("..controller..")
    .should().dependOnClassesThat().resideInAPackage("..repository..")
    .check(importedClasses);
```

`dependOnClassesThat()`은 필드 타입도 봅니다. 아래 주석을 풀면 실패합니다.

```java
// private final AddressRepository addressRepository;
```

### Domain Setter 메서드 금지

```java
noMethods()
    .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
    .should().haveNameMatching("set[A-Z].*")
    .allowEmptyShould(true)
    .check(importedClasses);
```

`set[A-Z].*`는 Java setter 관례를 잡는 정규식입니다.

```text
set      -> set으로 시작
[A-Z]    -> set 다음 글자는 대문자
.*       -> 뒤에는 아무 문자 0개 이상
```

`setSido`, `setLegacyFullAddress`는 잡고, `setup` 같은 일반 메서드는 피합니다.

### Legacy Getter 선언 금지

```java
noMethods()
    .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
    .should().haveName("getLegacyFullAddress")
    .allowEmptyShould(true)
    .check(addressMasterClasses);
```

[AddressMaster.java](src/main/java/com/example/proptech/domain/AddressMaster.java)는 클래스 레벨에 `@Getter`가 있지만 legacy 필드에는 예외를 둡니다.

```java
@Deprecated
@Getter(AccessLevel.NONE)
public String legacyFullAddress;
```

`@Getter(AccessLevel.NONE)`을 제거하면 Lombok이 `getLegacyFullAddress()`를 생성하고 규칙이 실패합니다.

### Legacy Getter 호출 금지

```java
noClasses()
    .should().callMethod(AddressMaster.class, "getLegacyFullAddress")
    .allowEmptyShould(true)
    .check(importedClasses);
```

이 규칙은 getter 존재가 아니라 getter 호출을 봅니다. 아래 주석을 풀면 실패합니다.

[AddressService.java](src/main/java/com/example/proptech/service/AddressService.java)

```java
// addressMaster.getLegacyFullAddress();
```

### JavaFieldAccess로 필드 재대입 금지

```java
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
```

이 규칙은 메서드 이름을 보지 않습니다. 바이트코드 수준에서 legacy 계열 필드에 값을 다시 대입했는지만 봅니다.

잡고 싶은 코드:

```java
public void updateLegacyFullAddress(String legacyFullAddress) {
    this.legacyFullAddress = legacyFullAddress;
}
```

현재 `legacy_fields_should_not_be_reassigned_by_bytecode_access`는 `AddressMaster.class`만 import합니다.

```java
JavaClasses javaClass = new ClassFileImporter().importClasses(AddressMaster.class);
```

[AddressMasterWithUpdateFieldMethod.java](src/main/java/com/example/proptech/domain/AddressMasterWithUpdateFieldMethod.java)를 검사하려면 import 대상을 바꿔보세요.

```java
JavaClasses javaClass = new ClassFileImporter()
    .importClasses(AddressMasterWithUpdateFieldMethod.class);
```

## Practice Flow

1. `./gradlew archUnitTest --no-daemon`으로 현재 상태가 통과하는지 확인합니다.
2. `AddressController`에서 repository 직접 접근 주석을 풀고 실패를 확인합니다.
3. `AddressMaster`에서 `@Getter(AccessLevel.NONE)`을 제거하고 getter 생성 금지 규칙 실패를 확인합니다.
4. `AddressService`에서 `getLegacyFullAddress()` 호출 주석을 풀고 호출 금지 규칙 실패를 확인합니다.
5. `legacy_fields_should_not_be_reassigned_by_bytecode_access`의 import 대상을 `AddressMasterWithUpdateFieldMethod.class`로 바꾸고 필드 재대입 규칙 실패를 확인합니다.
6. 각 연습 후에는 코드를 원복하고 다시 테스트가 통과하는지 확인합니다.

## CI

GitHub Actions는 push와 pull request에서 ArchUnit 전용 task를 실행합니다.

[.github/workflows/archunit.yml](.github/workflows/archunit.yml)

```yaml
name: ArchUnit CI

on:
  pull_request:
  push:
    branches:
      - main

jobs:
  archunit:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Run ArchUnit tests
        run: ./gradlew archUnitTest --no-daemon
```

실전에서는 일부러 실패시키는 연습용 규칙과 CI에 넣을 진짜 규칙을 분리하는 편이 좋습니다.

```text
ArchitectureTest.java          # CI에 넣을 규칙
ArchitecturePracticeTest.java  # 일부러 깨보는 연습 규칙
```
