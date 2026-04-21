# 품질 검증 규칙 (quality-rules)

> Quality Agent 가 참조하는 **검증 기준**입니다.
> 공통 표준은 `shared-standards.md` 에서 먼저 로드됩니다.
> Quality Agent 는 **읽기 전용** — 위반 사항을 검출해 **보고**하며, 수정은 Refactor Agent 또는 개발자에게 위임합니다.

---

## 1. 보안 검증 (Security)

### 1.1 SQL Injection — `${}` 직접 치환 **[차단 수준]**

MyBatis 매퍼에서 `${}` 직접 치환은 **Critical 위반**. Quality Agent 는 발견 시 **커밋 중단 권고**.

| 사용 | 판정 | 처리 |
|---|---|---|
| `#{parameter}` | ✅ OK | 바인딩 파라미터 (정상) |
| `${columnName}` (ORDER BY 정렬 컬럼) | ⚠️ 허용 조건부 | 화이트리스트 검증 주석 필수 |
| `${tableName}` (동적 테이블) | ⚠️ 허용 조건부 | 상수 enum / 사전 검증 주석 필수 |
| `${value}` (사용자 입력 → WHERE 값) | ❌ **차단** | SQL Injection 치명적 취약점 |

**예시:**
```xml
<!-- ❌ Critical 차단 -->
<select id="findUser">
  SELECT * FROM user WHERE name = '${name}'
</select>

<!-- ✅ 권장 -->
<select id="findUser">
  SELECT * FROM user WHERE name = #{name}
</select>

<!-- ⚠️ 조건부 허용 (주석으로 사유 명시) -->
<select id="findSorted">
  <!-- sortColumn 은 Controller 에서 화이트리스트 검증 완료 -->
  SELECT * FROM user ORDER BY ${sortColumn}
</select>
```

**Quality Agent 동작:** `${...}` 패턴 스캔 → 주석에 화이트리스트/검증 명시 없으면 **Critical 보고 + 커밋 중단 권고**.

### 1.2 민감정보 로그 출력 금지

**스캔 대상 키워드** (대소문자 무관):

| 카테고리 | 키워드 |
|---|---|
| 기본 3종 | `password`, `token`, `secret` |
| 인증 파생 | `apiKey`, `accessKey`, `privateKey`, `credential`, `authorization`, `bearer` |
| 개인정보 (PII) | `ssn`, `jumin`, `주민등록번호`, `phone`, `연락처`, `email`(로그 맥락) |

**검출 패턴:**
```java
// ❌ 위반 — 로그에 민감정보 노출
log.info("login password: {}", user.getPassword());
log.debug("jwt token=" + token);

// ✅ 권장 — 마스킹 또는 존재 여부만 로깅
log.info("login user: {}", user.getUsername());
log.debug("jwt token present: {}", token != null);
```

**응답 DTO 검증:** 비밀번호·토큰 필드가 Response DTO 에 포함되면 **High 보고**.

### 1.3 입력 검증 (Controller 진입점)

- `@RequestParam`·`@PathVariable` 파라미터는 Bean Validation (`@NotNull`, `@Size`, `@Pattern`) 또는 수동 검증 필요
- 화이트리스트 기반 허용 목록 선호 (블랙리스트 금지)
- 길이·타입·형식 제한 필수

---

## 2. 규칙 준수 검증 (Rule Compliance)

### 2.1 `javax.*` → `jakarta.*` 강제

```java
// ❌ 위반
import javax.servlet.http.HttpServletRequest;
import javax.persistence.Entity;

// ✅ 준수
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.Entity;
```

### 2.2 메서드 50줄 제한 검증

`refactor-rules.md` 의 50줄 제한이 지켜지는지 검증. 초과 시 **Medium 보고** (자동 수정 권고 → Refactor Agent).

### 2.3 레이어 책임 분리 검증

| 레이어 | 금지 패턴 검출 |
|---|---|
| **Controller** | `@Transactional`, `Repository`/`Dao` 직접 주입, 비즈니스 조건 분기 |
| **Service** | `HttpServletRequest`/`HttpServletResponse` 의존, `JdbcTemplate` 직접 사용 |
| **Dao/Repository** | 트랜잭션 조작, 도메인 로직, 외부 API 호출 |

### 2.4 Lombok 사용 검증

- 금지: `@Data`, 무분별한 `@EqualsAndHashCode` → **High 보고**
- 허용 외 Lombok 어노테이션 발견 시 **Low 보고** (팀 합의 확인 필요)

### 2.5 null 반환 검증

`return null;` 검출 → `refactor-rules.md` 3절 null 정책 적용 권고 (**Medium 보고**).

### 2.6 `Map<String,Object>` 인자·반환 검증

함수 시그니처에서 `Map<String, Object>` 검출 시 DTO 전환 권고 (**Medium 보고**). 주석으로 사유 명시된 경우 제외.

---

## 3. 테스트 커버리지 검증 (신규/변경 메서드 기준)

**검증 범위:** 레거시 전체가 아니라 **현재 diff 의 신규·변경 메서드**에 한정.

### 3.1 검증 절차

1. `git diff --cached` 또는 `git diff HEAD~1 HEAD` 로 변경 파일 수집
2. 변경된 `public`·`protected` 메서드 추출
3. 대응하는 테스트 클래스·메서드 존재 여부 확인
   - 관례: `src/main/java/**/Foo.java` → `src/test/java/**/FooTest.java`
   - 테스트 메서드명 힌트: `testXxx`, `xxxTest`, `should...` 패턴
4. 누락된 경우 **High 보고**

### 3.2 테스트 실행

빌드 도구 자동 감지:

| 감지 파일 | 실행 명령 |
|---|---|
| `pom.xml` 존재 | `mvn test` |
| `build.gradle` / `build.gradle.kts` 존재 | `./gradlew test` (Windows: `gradlew.bat test`) |
| 둘 다 | `pom.xml` 우선 |
| 둘 다 없음 | 테스트 실행 스킵 + **Low 보고** (빌드 도구 미감지) |

**결과 보고 형식** (전역 CLAUDE.md 규칙 준수):
```
통과 N건 / 실패 M건
```

실패 시 실패 테스트 목록 출력 → **Critical 보고**.

---

## 4. 성능 규칙 (Performance)

정적 분석으로 감지 가능한 안티패턴만 검증. 런타임 프로파일링은 범위 외.

### 4.1 N+1 쿼리 감지 **[High 보고]**

**JPA 패턴:**
```java
// ❌ N+1 유발
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    order.getItems().size(); // lazy loading → 쿼리 N번 추가 발생
}

// ✅ 권장
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items")
List<Order> findAllWithItems();
```

**감지 신호:**
- `@OneToMany`·`@ManyToOne` + lazy 기본 + 컬렉션 루프
- `findAll()` 결과를 `for`·`stream()` 에서 연관 객체 접근

**MyBatis 패턴:**
- 루프 내 `mapper.findById(id)` 반복 호출 → `findByIds(List<Long>)` 일괄 조회 권고

### 4.2 인덱스 힌트 **[Low 보고]**

**한계:** Quality Agent 는 DDL 에 접근 불가능. 정적 분석만으로 인덱스 유무 판정 불가.

**대신 제공:** JPA `@Query` / MyBatis `<select>` 의 `WHERE` / `ORDER BY` / `JOIN` 절 컬럼 목록을 **힌트 형태로 보고** → 개발자가 DBA 와 검토.

```
[인덱스 검토 권고]
- OrderMapper.findByStatusAndCreatedAt → WHERE status, created_at
- UserRepository.findByEmail → WHERE email
```

### 4.3 반복 문자열 연산 **[Medium 보고]**

```java
// ❌ 위반
String result = "";
for (String s : list) {
    result += s + ",";
}

// ✅ 권장
String result = String.join(",", list);
// 또는
StringBuilder sb = new StringBuilder();
for (String s : list) sb.append(s).append(",");
```

**감지 신호:** `for`·`while` 내부에 `+=` 문자열 연산, `+` 문자열 조립.

### 4.4 로거 파라미터 방식 강제 **[Medium 보고]**

```java
// ❌ 위반 — debug 레벨 꺼져도 문자열 조립 비용 발생
log.debug("user info: " + user.toString() + ", id=" + id);

// ✅ 권장 — 파라미터 방식 (debug off 시 조립 안 함)
log.debug("user info: {}, id={}", user, id);
```

**감지 신호:** `log.debug/info/warn/error(` 의 첫 인자에 문자열 연산 (`+`) 존재.

### 4.5 `findAll()` 후 필터링 안티패턴 **[High 보고]**

```java
// ❌ 위반 — DB 에서 전체 로드 후 메모리 필터 (대용량 시 OOM 위험)
List<User> all = userRepository.findAll();
List<User> active = all.stream()
    .filter(u -> u.getStatus() == Status.ACTIVE)
    .collect(Collectors.toList());

// ✅ 권장 — 쿼리 조건으로 위임
List<User> active = userRepository.findByStatus(Status.ACTIVE);
```

**감지 신호:** `findAll()`·`selectAll()` 결과에 `stream().filter()` / `for` 조건 필터.

---

## 5. 보고 형식 (Report Format)

Quality Agent 는 검증 결과를 **체크리스트 + 심각도 태그** 로 출력.

### 5.1 심각도 (Severity)

| 태그 | 의미 | 커밋 차단 여부 |
|---|---|---|
| **Critical** | 보안 취약점·테스트 실패 | ✅ 커밋 중단 권고 |
| **High** | 중요 규칙 위반 (레이어 위반, 민감정보 노출, N+1) | ⚠️ 커밋 전 검토 강력 권고 |
| **Medium** | 품질 저하 (메서드 50줄 초과, null 반환, 로거 방식) | 📝 다음 리팩토링 대상 |
| **Low** | 개선 제안 (인덱스 힌트, 미확정 Lombok) | 💡 참고 사항 |

### 5.2 출력 템플릿

```
## Quality Report — <대상>

### Critical (차단 권고)
- [SQL-INJ] UserMapper.xml:45 — ${name} 직접 치환 검출
- [TEST-FAIL] UserServiceTest.findById — 통과 0건 / 실패 1건

### High
- [LAYER] UserController.java:32 — 비즈니스 분기 직접 구현 (Service 이관 권고)
- [N+1] OrderService.java:78 — 루프 내 lazy 접근 감지

### Medium
- [METHOD-LEN] UserService.java:102 — 73줄 (50줄 초과)
- [LOGGER] UserService.java:55 — `log.debug("x=" + obj)` → 파라미터 방식 권고

### Low
- [INDEX-HINT] OrderMapper.findByStatus → WHERE status 컬럼 인덱스 검토

### 요약
- Critical: 2건 · High: 2건 · Medium: 2건 · Low: 1건
- 권고: **커밋 중단** (Critical 해결 필요)
```

### 5.3 BLOCK 마커 (커밋 자동 차단 신호)

Quality Agent 자체는 권한상 프로세스 종료 코드를 직접 제어하지 못하므로, **보고서 마지막 줄에 마커 문자열**을 출력해 Hook 스크립트가 해석하도록 합니다.

| 상황 | 마지막 줄 마커 | Hook 동작 |
|---|---|---|
| Critical 1건 이상 | `[BLOCK: COMMIT STOP]` | Hook 이 `exit 2` → Claude 가 git commit 자동 중단 |
| Critical 0건 | `[PASS: COMMIT READY]` | Hook 이 `exit 0` → 커밋 정상 진행 |

**High / Medium / Low 는 차단하지 않음** — 보고만 하고 개발자 판단에 위임. Critical 만 자동 차단 대상.

### 5.4 최종 결정은 개발자

자동 차단(Critical)도 개발자가 마커를 무시하고 강제 커밋할 수 있는 여지(예: `--no-verify` 는 금지지만 별도 슬래시 커맨드 / 직접 git 명령) 존재. Quality Agent 는 신호자, 결정권자는 개발자.

---

## 6. Quality Agent 동작 범위 (권한)

- ✅ `Read`, `Grep`, `Glob` — 소스 스캔
- ✅ `Bash(git diff:*)`, `Bash(git log:*)`, `Bash(git status:*)` — 변경 파일 식별
- ✅ `Bash(mvn test:*)`, `Bash(./gradlew test:*)` — 테스트 실행
- ❌ `Edit`, `Write` — 수정 금지 (Refactor Agent 책임)
- ❌ `Bash(git add:*)`, `Bash(git commit:*)` — 커밋 금지
