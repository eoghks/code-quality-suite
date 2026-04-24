---
name: generate-tests
description: 신규/변경 public 메서드에 JUnit 5 + Mockito 테스트 스켈레톤 자동 생성. test-generation-agent 를 호출한다.
---

# /generate-tests

## 사용법

```
/generate-tests [대상] [옵션]
```

## 인자

| 인자 | 설명 | 예시 |
|---|---|---|
| (없음) | `git diff HEAD~1..HEAD` 기준 변경 파일 | `/generate-tests` |
| `<파일경로>` | 특정 파일 지정 | `/generate-tests src/main/java/com/example/UserService.java` |
| `<디렉터리>` | 하위 전체 스캔 | `/generate-tests src/main/java/com/example/service/` |
| `<브랜치명>` | 해당 브랜치 변경분 | `/generate-tests feature/new-feature` |
| `--all` | `src/main/java/` 전체 | `/generate-tests --all` |

## 옵션

| 옵션 | 설명 |
|---|---|
| `--dry-run` | 테스트 파일 생성 없이 대상 메서드 목록만 출력 |
| `--no-run` | 테스트 파일 생성 후 `mvn test` / `./gradlew test` 실행 건너뜀 |
| `--strict` | `@suppress` 무시, 모든 위반 그대로 보고 |

## 동작

1. **`test-generation-agent`** 호출
2. 변경된 비-테스트 `.java` 파일에서 `public` 메서드 추출
3. 대응 테스트 파일 생성 (`src/test/java/...Test.java`) 또는 갱신
4. Given/When/Then 스켈레톤 + `@DisplayName` 자동 작성
5. `mvn test` / `./gradlew test` 실행
6. 실패 시 최대 2회 수정 시도 → 계속 실패 시 `@Disabled` 처리
7. 결과 커밋

## 브랜치 규칙

- 현재 브랜치가 `main` / `master` 이면 `test/<클래스명-lower>` 브랜치 생성 후 커밋
- 기존 `feature/` / `bugfix/` / `refactor/` 브랜치이면 해당 브랜치에 추가 커밋

## 출력 예시

```
Test Generation Report
- 대상 메서드: 5개
- 스켈레톤 생성: 5개
- @Disabled (수동 완성 필요): 1개
- 테스트 실행: 통과 8건 / 실패 0건
- 커밋: abc1234
```

## 참고

- 생성 규칙 상세: `agents/test-generation-agent.md`
- 프로덕션 코드(`src/main/`)는 수정하지 않음
