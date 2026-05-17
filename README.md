# ArchUnit Practice Lab

ArchUnit으로 Java 코드의 아키텍처 규칙을 검사하는 연습용 Gradle 프로젝트입니다.

이 repo는 “테스트를 모두 통과시키는 정답 프로젝트”라기보다, 규칙을 하나씩 깨보고 실패 메시지를 읽으면서 ArchUnit 감각을 익히는 용도입니다.

## 실행

```bash
./gradlew test --no-daemon
```

특정 ArchUnit 테스트 클래스만 실행:

```bash
./gradlew test --tests '*ArchitectureTest' --no-daemon
```

## 프로젝트 구조

```text
src/main/java/com/example/proptech/
  controller/
    AddressController.java
  service/
    AddressService.java
  repository/
    AddressRepository.java
  domain/
    AddressMaster.java
    AddressMasterWithSetter.java
    AddressMasterWithUpdateFieldMethod.java

src/test/java/com/example/proptech/architecture/
  ArchitectureTest.java
```

## Gradle 설정

[build.gradle](build.gradle)에 JUnit 5, ArchUnit, Lombok이 들어 있습니다.

```gradle
compileOnly "org.projectlombok:lombok:1.18.46"
annotationProcessor "org.projectlombok:lombok:1.18.46"

testImplementation "org.junit.jupiter:junit-jupiter:5.10.2"
testRuntimeOnly "org.junit.platform:junit-platform-launcher:1.10.2"
testImplementation "com.tngtech.archunit:archunit-junit5:1.3.0"
```

Lombok은 컴파일 시 getter/setter 같은 코드를 생성합니다. ArchUnit은 컴파일된 class 파일을 분석하므로, Lombok이 생성한 `getXxx()` / `setXxx()` 메서드도 규칙으로 잡을 수 있습니다.

## 핵심 개념

### JavaClasses

`JavaClasses`는 ArchUnit이 bytecode를 읽어서 만든 검사 대상 묶음입니다.

```java
private final JavaClasses importedClasses =
    new ClassFileImporter().importPackages("com.example.proptech");
```

이렇게 하면 `com.example.proptech` 아래 전체 클래스를 검사합니다. 계층 규칙처럼 여러 패키지 관계를 봐야 할 때 사용합니다.

```java
private final JavaClasses addressMasterClasses =
    new ClassFileImporter().importClasses(AddressMaster.class);
```

이렇게 하면 `AddressMaster` 하나만 검사합니다. 특정 클래스의 getter, setter, 필드 규칙을 볼 때 사용합니다.

## 현재 연습 규칙

규칙은 [ArchitectureTest.java](src/test/java/com/example/proptech/architecture/ArchitectureTest.java)에 있습니다.

### 1. controller -> repository 직접 접근 금지

```java
noClasses()
    .that().resideInAPackage("..controller..")
    .should().accessClassesThat().resideInAPackage("..repository..")
    .check(importedClasses);
```

의미:

```text
controller 패키지 클래스가 repository 패키지 클래스를 직접 사용하면 안 된다.
```

주의:

`accessClassesThat()`은 실제 접근을 봅니다. 필드 타입이나 생성자 파라미터 같은 의존성까지 강하게 막고 싶으면 `dependOnClassesThat()`을 씁니다.

연습:

[AddressController.java](src/main/java/com/example/proptech/controller/AddressController.java)의 주석을 풀어보세요.

```java
// private final AddressRepository addressRepository;

// public String accessByRepository(){
//     return addressRepository.findByDong("hello");
// }
```

### 2. controller -> repository 의존성 금지

```java
noClasses()
    .that().resideInAPackage("..controller..")
    .should().dependOnClassesThat().resideInAPackage("..repository..")
    .check(importedClasses);
```

의미:

```text
controller가 repository를 필드 타입, 생성자 파라미터, 메서드 호출 등 어떤 형태로든 의존하면 안 된다.
```

`accessClassesThat()`보다 더 강한 규칙입니다.

### 3. domain setter 메서드 금지

```java
noMethods()
    .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
    .should().haveNameMatching("set[A-Z].*")
    .allowEmptyShould(true)
    .check(importedClasses);
```

의미:

```text
domain 패키지 안에 setXxx 형태의 setter 메서드가 있으면 안 된다.
```

`set[A-Z].*` 정규식:

```text
set      -> set으로 시작
[A-Z]    -> set 다음 글자는 대문자
.*       -> 그 뒤는 아무 문자 0개 이상
```

그래서 `setSido`, `setLegacyFullAddress`는 잡고, `setup` 같은 일반 메서드는 피합니다.

### 4. 특정 legacy getter 선언 금지

```java
noMethods()
    .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
    .should().haveName("getLegacyFullAddress")
    .allowEmptyShould(true)
    .check(addressMasterClasses);
```

의미:

```text
AddressMaster에 getLegacyFullAddress 메서드가 생성되거나 선언되면 안 된다.
```

[AddressMaster.java](src/main/java/com/example/proptech/domain/AddressMaster.java)는 클래스 레벨에 `@Getter`가 있지만, legacy 필드에는 예외를 둡니다.

```java
@Deprecated
@Getter(AccessLevel.NONE)
public String legacyFullAddress;
```

`@Getter(AccessLevel.NONE)`을 주석 처리하면 Lombok이 `getLegacyFullAddress()`를 만들고, 이 규칙이 실패합니다.

### 5. 특정 legacy getter 호출 금지

```java
noClasses()
    .should().callMethod(AddressMaster.class, "getLegacyFullAddress")
    .allowEmptyShould(true)
    .check(importedClasses);
```

의미:

```text
importedClasses 안의 어떤 클래스도 AddressMaster.getLegacyFullAddress()를 호출하면 안 된다.
```

이 규칙은 getter가 존재하는지만 보지 않습니다. 실제 호출을 봅니다.

연습:

[AddressService.java](src/main/java/com/example/proptech/service/AddressService.java)나 [AddressRepository.java](src/main/java/com/example/proptech/repository/AddressRepository.java)의 주석을 풀어보세요.

```java
// addressMaster.getLegacyFullAddress();
```

### 6. JavaFieldAccess로 legacy 필드 재대입 금지

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

의미:

```text
메서드 이름과 상관없이 legacy로 시작하는 필드에 값을 다시 대입하면 실패한다.
```

이 규칙은 `setLegacyFullAddress`, `updateLegacyFullAddress`, `refresh`처럼 메서드 이름이 달라도 필드 대입 자체를 봅니다.

잡고 싶은 코드:

```java
public void updateLegacyFullAddress(String legacyFullAddress) {
    this.legacyFullAddress = legacyFullAddress;
}
```

주의:

현재 `legacy_fields_should_not_be_reassigned_by_bytecode_access`는 `AddressMaster.class`만 import합니다.

```java
JavaClasses javaClass = new ClassFileImporter().importClasses(AddressMaster.class);
```

[AddressMasterWithUpdateFieldMethod.java](src/main/java/com/example/proptech/domain/AddressMasterWithUpdateFieldMethod.java)를 검사하려면 import 대상을 바꿔야 합니다.

```java
JavaClasses javaClass = new ClassFileImporter()
    .importClasses(AddressMasterWithUpdateFieldMethod.class);
```

## accessClassesThat vs dependOnClassesThat

### accessClassesThat

실제 코드에서 클래스에 접근하는지 봅니다.

예:

```java
addressRepository.findByDong("hello");
```

### dependOnClassesThat

필드 타입, 생성자 파라미터, 메서드 호출 등 더 넓은 의존성을 봅니다.

예:

```java
private final AddressRepository addressRepository;
```

controller가 repository를 아예 알면 안 되는 규칙이면 `dependOnClassesThat()`이 더 적합합니다.

## noMethods vs callMethod

### noMethods

메서드 선언 또는 Lombok이 생성한 메서드의 존재를 검사합니다.

```java
noMethods()
    .that().areDeclaredInClassesThat().resideInAPackage("..domain..")
    .should().haveName("getLegacyFullAddress")
```

의미:

```text
getLegacyFullAddress 메서드가 있으면 안 된다.
```

### callMethod

누군가 그 메서드를 호출했는지 검사합니다.

```java
noClasses()
    .should().callMethod(AddressMaster.class, "getLegacyFullAddress")
```

의미:

```text
getLegacyFullAddress를 호출하면 안 된다.
```

## 연습 순서

1. `./gradlew test --no-daemon`으로 현재 상태가 통과하는지 확인합니다.
2. `AddressController`에서 repository 직접 접근 주석을 풀고 실패를 확인합니다.
3. `AddressMaster`에서 `@Getter(AccessLevel.NONE)`을 제거하고 getter 생성 금지 규칙 실패를 확인합니다.
4. `AddressService`에서 `getLegacyFullAddress()` 호출 주석을 풀고 호출 금지 규칙 실패를 확인합니다.
5. `legacy_fields_should_not_be_reassigned_by_bytecode_access`의 import 대상을 `AddressMasterWithUpdateFieldMethod.class`로 바꾸고 필드 재대입 규칙 실패를 확인합니다.

각 연습 후에는 코드를 원복하고 다시 테스트가 통과하는지 확인하세요.

## CI에서 쓰는 방식

ArchUnit 테스트도 일반 테스트처럼 CI에서 실행합니다.

```yaml
name: Java CI

on:
  pull_request:
  push:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Run tests
        run: ./gradlew test --no-daemon
```

실전에서는 일부러 실패시키는 연습용 규칙과 CI에 넣을 진짜 규칙을 분리하는 편이 좋습니다.

```text
ArchitectureTest.java          # CI에 넣을 규칙
ArchitecturePracticeTest.java  # 일부러 깨보는 연습 규칙
```
