# 리팩토링 규칙 (refactor-rules)

> Refactor Agent 가 참조하는 **리팩토링 기준**입니다.
> 공통 표준은 `shared-standards.md` 에서 먼저 로드됩니다.

---

## 1. 메서드 크기 제한

- **최대 50줄** (공백·주석·중괄호 포함)
- **테스트 메서드에도 동일 적용** (given/when/then 구조라도 50줄 이내)
- 50줄 초과 시:
  1. 논리 블록별 private 메서드 분리
  2. 전역 변수 공유가 과하면 Parameter Object 적용
  3. 분리 후에도 50줄 초과하는 "주 메서드"가 남으면 클래스 분리 검토

---

## 2. null 반환 정책

| 반환 타입 | 정책 |
|---|---|
| 단일 객체 | `Optional<T>` 사용 |
| `List`, `Set`, `Map` | 빈 컬렉션 (`Collections.emptyList()`, `List.of()` 등) |
| 배열 | 길이 0 배열 (`new T[0]`) |
| `String` | 빈 문자열 `""` 또는 `Optional<String>` 상황별 |

**금지:** `return null;` — Refactor Agent 발견 시 위 정책으로 자동 교체 제안

---

## 3. `Map<String, Object>` 사용 정책

**원칙: 최대한 금지.** 대신 DTO/VO 사용.

- **파라미터**: `Map<String, Object>` → DTO 변환 (필드 의미 명확화)
- **반환값**: `Map<String, Object>` → DTO/VO 또는 Record
- **불가피한 예외** (MyBatis 다중 파라미터 등): 주석으로 사유 명시 필수

**예시:**
```java
// ❌ 금지
public void createUser(Map<String, Object> userMap) { ... }

// ✅ 권장
public void createUser(UserCreateDto dto) { ... }
```

---

## 4. 매직넘버 상수화

**예외 허용:** `0`, `1`, `-1` (관용적 의미 명확한 경우)

**상수화 필수:**
- HTTP 상태 코드 (`200`, `404` 등 → `HttpStatus.OK`)
- 타임아웃·제한값 (`30`, `1000` 등 → `TIMEOUT_SECONDS`, `MAX_RETRY_COUNT`)
- 비즈니스 임계값 (`1000000`, `0.15` 등 → 의미 있는 상수명)
- 반복되는 문자열 리터럴 (매직 문자열도 상수화)

**명명:**
- `static final` 상수는 UPPER_SNAKE_CASE
- 공개 범위는 최소화 (private 우선, 필요 시 package-private)

---

## 5. 설계 패턴 적극 적용

리팩토링 시 **디자인 패턴을 최대한 활용**한다. Refactor Agent 는 다음 패턴들을 우선 고려:

### 5.1 Strategy — 조건 분기 → 전략 클래스

```java
// Before
if (type.equals("A")) { ... }
else if (type.equals("B")) { ... }

// After
Map<String, Strategy> strategies = ...;
strategies.get(type).execute();
```

**적용 신호:** `if/else if` 체인이 3개 이상, `switch` 가 동작 분기

### 5.2 Factory — 객체 생성 집중화

**적용 신호:** `new` 가 여러 곳에 분산, 생성 로직에 조건부 분기

### 5.3 Builder — 복잡한 생성자

**적용 신호:** 생성자 파라미터 4개 이상, 선택적 파라미터 존재 → Lombok `@Builder` 적극 활용

### 5.4 Template Method — 중복 구조 → 추상 메서드

**적용 신호:** 여러 클래스가 동일한 단계 순서를 갖되 일부 단계만 다름

### 5.5 기타 권장 패턴

- **Facade** — 복잡한 서브시스템 단순화
- **Adapter** — 기존 인터페이스 호환
- **Decorator** — 동적 책임 추가
- **Observer** — 이벤트 기반 (Spring `@EventListener` 활용)

---

## 6. 중복 제거 원칙 (DRY)

- 3회 이상 반복되는 코드 → 메서드 추출
- 2개 이상 클래스에서 동일 로직 → 공통 유틸/상속/컴포지션
- 상수·설정값 중복 → 상수 클래스 또는 `@ConfigurationProperties`

---

## 7. 네이밍 개선

- **동사 + 명사** 형태 메서드명: `findUserById`, `calculateTotalPrice`
- 불리언 반환: `is*`, `has*`, `can*`, `should*` 접두사
- 약어 남용 금지: `usr` → `user`, `calc` → `calculate`
- 한글 변수명 금지 (식별자는 영어)

---

## 8. 테스트 코드 병행 수정

**Refactor Agent 책임:**
- 구현 메서드 수정 시 대응 테스트 메서드도 갱신
- 메서드 분리 시 새 메서드 단위 테스트 추가 고려
- 리팩토링 전후 테스트 통과 확인 (`mvn test` / `./gradlew test`)
- 테스트 깨짐 → 리팩토링 잘못된 것 (롤백 후 재검토)

---

## 9. 브랜치·커밋 전략 (조직 규칙 준수)

**브랜치 강제:**
- `main`/`master` 직접 커밋 금지
- 작업 유형 감지:
  - 신규 기능 → `feature/<요약>` (예: `feature/user-login`)
  - 버그 수정 → `bugfix/<요약>` (예: `bugfix/npe-user-service`)
  - 순수 리팩토링 → `refactor/<요약>`

**커밋 단위:**
- 리팩토링 단위로 아토믹 커밋 분리
- 한 커밋에 여러 리팩토링 섞지 않음

**커밋 메시지 형식:**
```
<type>: <한국어 요약>

- 변경 내용 1
- 변경 내용 2
```

타입: `feat` (신규) · `fix` (버그) · `refactor` (리팩토링) · `test` (테스트) · `docs` (문서) · `chore` (기타)
