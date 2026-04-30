# 리팩토링 메트릭·브랜치 규칙 (refactor/metrics-git)

> §8~10: 테스트 병행·브랜치/커밋 전략·복잡도 메트릭

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
- 신규 기능 → `feature/<요약>` · 버그 수정 → `bugfix/<요약>` · 리팩토링 → `refactor/<요약>`

**커밋 단위:**
- 리팩토링 단위로 아토믹 커밋 분리 (여러 리팩토링 섞지 않음)

**커밋 메시지 형식:**
```
<type>: <한국어 요약>

- 변경 내용 1
- 변경 내용 2
```

타입: `feat` · `fix` · `refactor` · `test` · `docs` · `chore`

---

## 10. 메트릭 기반 복잡도 제한 (v0.2.0+)

| 항목 | 임계값 | 심각도 | 감지 방식 |
|---|---|---|---|
| **Cyclomatic Complexity** | 10 초과 | Medium | `if`·`else if`·`case`·`for`·`while`·`catch`·`&&`·`\|\|`·`?:` 분기 수 + 1 |
| **Cognitive Complexity** | 15 초과 | Medium | 중첩 깊이 가중치 포함 근사 계산 |
| **메서드 파라미터 개수** | 3 초과 | Medium | 시그니처 파라미터 수 |
| **클래스 필드 개수** | 10 초과 | Medium | `private`/`protected` 인스턴스 필드 (static 제외) |

### 10.1 Cyclomatic Complexity 10 초과 → 테이블 기반 리팩토링

```java
// ❌ CC = 12
public String classify(int score) {
    if (score >= 90) return "A";
    else if (score >= 80) return "B";
    // ...11개 분기
}

// ✅ CC = 2
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
public Order createOrder(Long userId, Long productId, int quantity, String address, String memo) { ... }
// ✅
public Order createOrder(OrderCreateRequest request) { ... }
```

**예외:** Spring Controller `@RequestParam`·`@PathVariable`·`HttpServletRequest` 바인딩 제외.

### 10.3 필드 10개 초과 → 클래스 분리

God Class 신호. 책임별 클래스 분리 또는 Value Object 추출.
