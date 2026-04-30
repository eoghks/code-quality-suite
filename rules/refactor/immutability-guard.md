# Immutability·Guard Clause 규칙 (refactor/immutability-guard)

> §13~14: Immutability 우선 정책·Guard Clause/Early Return (v0.3.0+)

---

## 13. Immutability 우선 정책 (v0.3.0+)

**원칙:** 상태 변경 가능성 최소화 → 멀티스레드 안전성 확보 및 사이드 이펙트 제거.

| 항목 | 규칙 | 심각도 |
|---|---|---|
| 인스턴스 필드 `final` 누락 | 재할당되지 않는 필드는 `final` 강제 | Medium |
| DTO/Value Object `record` 전환 | 불변 필드 3개 이상인 DTO → `record` 변환 권고 | Low |
| 컬렉션 반환 | 직접 반환 → `Collections.unmodifiableXxx` 또는 `List.copyOf()` | Medium |
| 방어적 복사 | 생성자·setter 에서 받은 컬렉션·배열 → 복사 후 저장 | Medium |

### 13.1 `final` 필드 강제

```java
// ❌ 재할당 없는데 final 미적용
private String name;
private int age;
// ✅ final 선언
private final String name;
private final int age;
```

**예외:** Lombok `@Setter`, JPA `@Entity` 기본 생성자 요구 필드, Spring `@Autowired` 필드 (→ 생성자 주입 전환 권장).

### 13.2 DTO → `record` 전환

```java
// ❌ 불변 DTO 클래스
public class UserResponse {
    private final Long id;
    private final String name;
    private final String email;
    // getter, constructor...
}
// ✅ record (Java 16+)
public record UserResponse(Long id, String name, String email) {}
```

**적용 신호:** `@Getter` + 모든 필드 `final` + 기본 생성자 없음 + 필드 3개 이상.

### 13.3 컬렉션 불변 반환

```java
// ❌ 내부 리스트 직접 반환 — 외부에서 수정 가능
public List<String> getTags() { return this.tags; }
// ✅ 불변 뷰 반환
public List<String> getTags() {
    return Collections.unmodifiableList(this.tags);
    // 또는: return List.copyOf(this.tags);
}
```

### 13.4 방어적 복사

```java
// ❌ 외부 리스트 참조 그대로 저장
public Order(List<Item> items) { this.items = items; }
// ✅ 방어적 복사
public Order(List<Item> items) { this.items = new ArrayList<>(items); }
```

---

## 14. Guard Clause / Early Return (v0.3.0+)

**원칙:** 전제 조건 실패를 메서드 진입 직후 조기 반환으로 처리 → 중첩 깊이 감소.

| 항목 | 규칙 | 심각도 |
|---|---|---|
| 중첩 `if` 3레벨 초과 | Guard clause 로 조기 return/throw | Medium |
| 전제 조건 후 `else` 블록 | `if (invalid) return` 패턴으로 `else` 제거 | Low |

### 14.1 Guard Clause 전환

```java
// ❌ 중첩 if 4레벨
public String process(Order order) {
    if (order != null) {
        if (order.isValid()) {
            if (order.hasItems()) {
                if (order.getUser() != null) {
                    return doProcess(order);
                }
            }
        }
    }
    return null;
}

// ✅ Guard clause — 실패 조건 먼저 처리
public String process(Order order) {
    if (order == null)            return null;
    if (!order.isValid())         return null;
    if (!order.hasItems())        return null;
    if (order.getUser() == null)  return null;
    return doProcess(order);
}
```

### 14.2 `else` 제거

```java
// ❌ 불필요한 else
public void validate(String input) {
    if (input == null || input.isBlank()) {
        throw new IllegalArgumentException("입력 필수");
    } else {
        doValidate(input);  // else 불필요
    }
}
// ✅ else 제거
public void validate(String input) {
    if (input == null || input.isBlank()) {
        throw new IllegalArgumentException("입력 필수");
    }
    doValidate(input);
}
```

**Agent 감지 기준:**
- `if ... return/throw` 블록 이후 오는 `else` → Low 경고
- 중첩 if 깊이 3 초과 → Medium 경고
