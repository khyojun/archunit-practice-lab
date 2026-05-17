# ArchUnit Practice Lab

ArchUnit을 실전 규칙처럼 연습하는 작은 Gradle 프로젝트입니다.

## 실행

```bash
./gradlew test --no-daemon
```

현재 테스트 중 일부는 의도적으로 실패할 수 있습니다. 실패 메시지를 보면서 규칙의 의미를 익히는 연습용 repo입니다.

## 연습 주제

- controller가 repository에 직접 의존하지 않기
- domain setter 메서드 금지
- 특정 legacy getter 선언/호출 금지
- `JavaFieldAccess`로 특정 필드 재대입 감지
- `importPackages`와 `importClasses` 차이 이해

## ArchUnit 테스트만 실행

```bash
./gradlew test --tests '*ArchitectureTest' --no-daemon
```

## 다음 정리 과제

실전 CI에 올릴 규칙과 일부러 깨보는 연습 규칙을 분리합니다.

```text
ArchitectureTest.java          # CI에 넣을 규칙
ArchitecturePracticeTest.java  # 일부러 깨보는 연습 규칙
```
