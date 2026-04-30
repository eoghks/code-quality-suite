# 성능·정적 분석 도구 규칙 (quality/performance)

> §4~4A: 성능 안티패턴·SpotBugs/JaCoCo 리포트 파싱

---

## 4. 성능 규칙 (Performance)

정적 분석으로 감지 가능한 안티패턴만 검증. 런타임 프로파일링은 범위 외.

### 4.1 N+1 쿼리 감지 [High]

```java
// ❌ N+1 유발 (JPA)
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    order.getItems().size(); // lazy loading → 쿼리 N번 추가
}
// ✅ 권장
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items")
List<Order> findAllWithItems();
```

**감지 신호:** `@OneToMany`/`@ManyToOne` + lazy 기본 + 컬렉션 루프
**MyBatis:** 루프 내 `mapper.findById(id)` 반복 → `findByIds(List<Long>)` 일괄 조회 권고

### 4.2 인덱스 힌트 [Low]

DDL 접근 불가로 인덱스 유무 판정 불가. `WHERE`/`ORDER BY`/`JOIN` 절 컬럼을 힌트 형태로 보고.

```
[인덱스 검토 권고]
- OrderMapper.findByStatusAndCreatedAt → WHERE status, created_at
```

### 4.3 반복 문자열 연산 [Medium]

```java
// ❌ for 루프 내 += 문자열 연산
String result = "";
for (String s : list) { result += s + ","; }
// ✅
String result = String.join(",", list);
```

### 4.4 로거 파라미터 방식 강제 [Medium]

```java
// ❌ 문자열 조립 비용 발생
log.debug("user info: " + user.toString() + ", id=" + id);
// ✅ 파라미터 방식
log.debug("user info: {}, id={}", user, id);
```

### 4.5 `findAll()` 후 필터링 안티패턴 [High]

```java
// ❌ DB 전체 로드 후 메모리 필터 (대용량 OOM 위험)
List<User> all = userRepository.findAll();
List<User> active = all.stream().filter(u -> u.getStatus() == Status.ACTIVE).collect(...);
// ✅ 쿼리 조건으로 위임
List<User> active = userRepository.findByStatus(Status.ACTIVE);
```

---

## 4A. SpotBugs / JaCoCo 리포트 파싱 (v0.2.0+)

### 4A.1 SpotBugs 리포트 경로

| 빌드 도구 | 리포트 경로 |
|---|---|
| Maven | `target/spotbugsXml.xml` |
| Gradle | `build/reports/spotbugs/main/spotbugs.xml` |

**카테고리 → 심각도 매핑:**

| SpotBugs 카테고리 | 심각도 | 코드 |
|---|---|---|
| `MALICIOUS_CODE`·`SECURITY` | **Critical** | `[SB-SEC]` |
| `CORRECTNESS` | **High** | `[SB-BUG]` |
| `PERFORMANCE`·`MT_CORRECTNESS` | **High** | `[SB-PERF]`·`[SB-THREAD]` |
| `BAD_PRACTICE` | **Medium** | `[SB-BAD]` |
| `DODGY_CODE`·`I18N` | **Medium** | `[SB-DODGY]` |
| `STYLE` | **Low** | `[SB-STYLE]` |

출력 형식: `[SB-BUG] UserService.java:78 — NP_NULL_ON_SOME_PATH (SpotBugs)`

**우선순위 필터:** SpotBugs `priority` 1(High) 만 Critical/High. 2(Normal), 3(Low) 는 Medium/Low.

### 4A.2 JaCoCo 커버리지 경로

| 빌드 도구 | 리포트 경로 |
|---|---|
| Maven | `target/site/jacoco/jacoco.xml` |
| Gradle | `build/reports/jacoco/test/jacocoTestReport.xml` |

**커버리지 기준:**

| 지표 | 임계값 | 심각도 |
|---|---|---|
| 라인 커버리지 | 80% 미만 | **High** |
| 브랜치 커버리지 | 70% 미만 | **Medium** |
| 전체 프로젝트 | 참고용 | Low |

계산: JaCoCo XML `<counter type="LINE" covered="N" missed="M"/>` 로 % 계산. 80% 미만 → `[COV-LOW]` High.

### 4A.3 Graceful Fallback (리포트 부재)

- SpotBugs 미발견: `[SB-MISSING]` Low + `mvn spotbugs:spotbugs` 실행 안내
- JaCoCo 미발견: `[COV-MISSING]` Low + `mvn test` 실행 안내
- 설정 가이드: `docs/INSTALL.md#spotbugs-jacoco-설정`
