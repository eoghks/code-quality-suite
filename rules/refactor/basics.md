# 리팩토링 기초 규칙 (refactor/basics)

> §1~7: 메서드 크기·null·Map·매직넘버·설계 패턴·DRY·네이밍

---

## 1. 메서드 크기 제한

- **최대 50줄** (공백·주석·중괄호 포함, 테스트 메서드 포함)
- 50줄 초과 시:
  1. 논리 블록별 private 메서드 분리
  2. 전역 변수 공유 과하면 Parameter Object 적용
  3. 분리 후에도 초과하면 클래스 분리 검토

---

## 2. null 반환 정책

| 반환 타입 | 정책 |
|---|---|
| 단일 객체 | `Optional<T>` 사용 |
| `List`/`Set`/`Map` | 빈 컬렉션 (`Collections.emptyList()` 등) |
| 배열 | 길이 0 배열 (`new T[0]`) |
| `String` | 빈 문자열 `""` 또는 `Optional<String>` |

**금지:** `return null;`

---

## 3. `Map<String, Object>` 사용 정책

원칙: 최대한 금지. 대신 DTO/VO 사용.

```java
// ❌ 금지
public void createUser(Map<String, Object> userMap) { ... }
// ✅ 권장
public void createUser(UserCreateDto dto) { ... }
```

불가피한 예외(MyBatis 다중 파라미터 등): 주석으로 사유 명시 필수.

---

## 4. 매직넘버 상수화

**예외 허용:** `0`, `1`, `-1` (관용적 의미 명확한 경우)

**상수화 필수:** HTTP 상태코드 · 타임아웃·제한값 · 비즈니스 임계값 · 반복 문자열 리터럴

- `static final` 상수는 `UPPER_SNAKE_CASE`
- 공개 범위 최소화 (private 우선)

---

## 5. 설계 패턴 적극 적용

### 5.1 Strategy — 조건 분기 → 전략 클래스

```java
// Before: if/else if 체인 3개 이상, switch 동작 분기
// After:
Map<String, Strategy> strategies = ...;
strategies.get(type).execute();
```

### 5.2 Factory — 객체 생성 집중화

**적용 신호:** `new` 가 여러 곳에 분산, 생성 로직에 조건부 분기

### 5.3 Builder — 복잡한 생성자

**적용 신호:** 생성자 파라미터 4개 이상, 선택적 파라미터 → Lombok `@Builder` 활용

### 5.4 Template Method — 중복 구조 → 추상 메서드

**적용 신호:** 여러 클래스가 동일 단계 순서를 갖되 일부 단계만 다름

### 5.5 기타 권장 패턴

- **Facade** — 복잡한 서브시스템 단순화
- **Adapter** — 기존 인터페이스 호환
- **Decorator** — 동적 책임 추가
- **Observer** — 이벤트 기반 (Spring `@EventListener` 활용)

---

## 6. 중복 제거 원칙 (DRY)

- 3회 이상 반복 코드 → 메서드 추출
- 2개 이상 클래스에서 동일 로직 → 공통 유틸/상속/컴포지션
- 상수·설정값 중복 → 상수 클래스 또는 `@ConfigurationProperties`

---

## 7. 네이밍 개선

- **동사 + 명사** 형태: `findUserById`, `calculateTotalPrice`
- 불리언 반환: `is*`, `has*`, `can*`, `should*` 접두사
- 약어 남용 금지: `usr` → `user`, `calc` → `calculate`
- 한글 변수명 금지 (식별자는 영어)
