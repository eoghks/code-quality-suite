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

---

## 10. 메트릭 기반 복잡도 제한 (v0.2.0+)

메서드 줄 수(§1)와 **별개로** 복잡도·파라미터·필드 개수를 객관적 수치로 제한한다. Agent 는 소스 파싱으로 아래 지표를 카운트한다.

| 항목 | 임계값 | 심각도 | 감지 방식 |
|---|---|---|---|
| **Cyclomatic Complexity** | 10 초과 | Medium | `if`·`else if`·`case`·`for`·`while`·`do`·`catch`·`&&`·`\|\|`·`? :` 분기 수 + 1 |
| **Cognitive Complexity** | 15 초과 | Medium | 중첩 깊이 가중치 (`if` 안의 `for` = 2 누적) 포함 근사 계산 |
| **메서드 파라미터 개수** | **3 초과** → Parameter Object 권장 | Medium | 시그니처 파라미터 수 |
| **클래스 필드 개수** | **10 초과** | Medium | `private`/`protected` 인스턴스 필드 (static 제외) |

### 10.1 Cyclomatic Complexity 10 초과 대응

```java
// ❌ CC = 12 (분기 11개 + 1)
public String classify(int score) {
    if (score >= 90) return "A";
    else if (score >= 80) return "B";
    else if (score >= 70) return "C";
    else if (score >= 60) return "D";
    // ... 7개 분기 더
}

// ✅ 테이블 기반 (CC = 2)
private static final NavigableMap<Integer, String> GRADES = new TreeMap<>(Map.of(
    90, "A", 80, "B", 70, "C", 60, "D"
));
public String classify(int score) {
    return GRADES.floorEntry(score).getValue();
}
```

### 10.2 파라미터 3개 초과 → Parameter Object

```java
// ❌ 파라미터 5개
public Order createOrder(Long userId, Long productId, int quantity,
                         String address, String memo) { ... }

// ✅ Parameter Object
public Order createOrder(OrderCreateRequest request) { ... }
```

**예외:** Spring Controller 메서드의 `@RequestParam`·`@PathVariable`·`HttpServletRequest` 바인딩은 프레임워크 관용이므로 제외 (주석 불필요).

### 10.3 필드 10개 초과 → 클래스 분리

God Class 신호. 책임별 클래스 분리 (§12.4 참조) 또는 Value Object 추출.

---

## 11. 런타임 안전 (예외·Resource) (v0.2.0+)

운영 환경에서 장애를 유발하는 예외·리소스 관리 안티패턴을 **High 수준**으로 차단한다.

### 11.1 예외 처리 금지 패턴

| 패턴 | 심각도 | 대응 |
|---|---|---|
| `catch (Exception e)` / `catch (Throwable t)` | **High** | 구체 예외로 분리 (`IOException`, `SQLException` 등) |
| `catch (RuntimeException e)` 상위 포착 | **High** | 구체 런타임 예외로 분리 (`NullPointerException` 은 catch 금지 — 방어 코드로 해결) |
| `e.printStackTrace()` | **High** | `log.error("...", e)` 로 교체 |
| catch 블록 공백 (swallow) | **High** | 최소 로깅 + 복구 로직 또는 재던지기 (`throw` / `throw new AppException(e)`) |
| `throw new RuntimeException(...)` / `throw new Exception(...)` | **Medium** | 도메인 의미 담은 구체 예외 클래스 정의 |

### 11.2 Resource 관리 강제

**try-with-resources 강제** — `java.lang.AutoCloseable` 구현체는 예외 없이 try-with-resources 사용.

```java
// ❌ 수동 close (close 누락 위험, finally 누락 시 리소스 누수)
Connection conn = dataSource.getConnection();
try {
    // ...
} finally {
    conn.close();
}

// ✅ try-with-resources
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement(SQL)) {
    // ...
}
```

**감지 대상 AutoCloseable (주요):**
- JDBC: `Connection` · `Statement` · `PreparedStatement` · `ResultSet`
- I/O: `InputStream` · `OutputStream` · `Reader` · `Writer` · `FileInputStream` · `FileOutputStream`
- 기타: `Scanner` · `java.util.stream.Stream` · `BufferedReader` · `RandomAccessFile`

**예외:** Spring `JdbcTemplate` · `TransactionTemplate` 등은 프레임워크가 close 관리 → 보고 대상 아님.

---

## 12. 응집도·결합도 (v0.2.0+)

### 12.1 클래스·파일 크기 제한

| 단위 | 임계값 | 심각도 |
|---|---|---|
| 클래스 | **400줄 초과** | Medium |
| 파일 | **500줄 초과** | Medium |
| God Class (메서드 30개 초과 **AND** 필드 15개 초과) | 모두 초과 | **High** |

초과 시 대응:
- 단일 책임 원칙 위반 신호 → 도메인 분리 / Service 레이어 분리
- 유틸 클래스가 커지면 카테고리별 분리 (`StringUtils` / `DateUtils` / `CollectionUtils`)

### 12.2 Law of Demeter (LoD)

메서드 체이닝 **3단계 초과** 경고. 객체 내부 구조 노출·강결합 신호.

```java
// ❌ 4단계 체인 — LoD 위반
String city = user.getAddress().getCity().getName().toUpperCase();

// ✅ 위임 메서드 제공 (User 가 책임)
String city = user.getCityName();   // User 내부에서 체이닝 처리
```

**예외:** Builder 체이닝 (`Builder.a().b().c().build()`), Stream API (`stream().filter().map()`), Fluent API (예: `ResponseEntity.ok().header().body()`) 는 의도된 체이닝 → 보고 제외.

### 12.3 순환 의존 (참고)

패키지 간 순환 참조 (`a.b → a.c → a.b`) 는 **Low 보고**. Agent 는 import 분석으로 감지하되, 확신도 낮으므로 경고 수준.

### 12.4 God Class 분해 가이드

**High 판정 시 제안 순서:**
1. 책임별 메서드 그룹화 → 별도 Service/Component 분리
2. 데이터만 담는 필드들 → 내부 Value Object 추출
3. 분리 후 원본 클래스는 Facade 역할 유지 (외부 호출자 영향 최소화)
