# @suppress 인라인 억제 주석 정책 (suppress-policy)

> 모든 Agent 가 참조하는 **위반 인라인 억제 규칙**.
> Baseline JSON 없이 코드 라인에 직접 억제 사유를 명시한다.

---

## 1. 문법

### Java / Kotlin / Groovy

```java
// @suppress <위반코드> — <사유>
Runtime.getRuntime().exec(cmd);
```

```java
// @suppress REF-CC — 레거시 분기 로직, 리팩토링 일정 2026-Q3
public String processLegacy(String type) { ... }
```

### XML (MyBatis, Liquibase)

```xml
<!-- @suppress SQL-INJ — sortColumn Enum 화이트리스트 검증 완료 (CREATED_AT, NAME 만 허용) -->
<select id="findAllSorted" resultType="User">
    SELECT * FROM user ORDER BY ${sortColumn}
</select>
```

### SQL (Flyway 마이그레이션)

```sql
-- @suppress MIG-DROP — 개발 환경 전용 임시 테이블, 프로덕션 적용 불가. DBA 승인: 2026-04-10
DROP TABLE temp_migration_log;
```

### YAML / Properties

```yaml
# @suppress ARCH-SEC-01 — 개발 프로파일 전용, 프로덕션 SecurityFilterChain 별도 적용
security:
  permit-all: true
```

---

## 2. 위치 규칙

억제 주석은 **위반 라인의 직전 1~2 라인**에 위치해야 한다.

```java
// ✅ 직전 1라인 (유효)
// @suppress SEC-CMD — 화이트리스트 검증 후 실행
Runtime.getRuntime().exec(sanitizedCmd);

// ✅ 직전 2라인 (어노테이션 등 개입 시 허용)
// @suppress ARCH-TX-01 — 배치 전용 Controller, 트랜잭션 경계 의도적 선언
@Transactional
@PostMapping("/batch")
public ResponseEntity<Void> runBatch() { ... }

// ❌ 3라인 이상 떨어진 경우 — 억제 무시
// @suppress SEC-CMD
// 중간 주석
// 또 다른 줄
Runtime.getRuntime().exec(cmd);  // ← 억제 무시됨
```

---

## 3. 동작 규칙

| 조건 | 동작 |
|---|---|
| `@suppress <코드> — <사유>` | 위반 억제, 보고서에 `[SUPPRESSED:<코드>]` 태그로 기록 |
| `@suppress <코드>` (사유 없음) | 억제는 되지만 `[SUPPRESS-NO-REASON]` **Medium** 경고 추가 |
| `@suppress <잘못된코드>` (존재하지 않는 코드) | 억제 무시, 원래 위반 그대로 보고, `[SUPPRESS-INVALID-CODE]` **Low** 경고 |
| `--strict` 모드 실행 | 모든 `@suppress` 무시, 위반 전체 보고 |

---

## 4. 지원 위반 코드 목록

### Refactor (code-refactoring-agent)

| 코드 | 설명 |
|---|---|
| `REF-LEN` | 메서드 길이 초과 |
| `REF-CC` | Cyclomatic Complexity 초과 |
| `REF-COG` | Cognitive Complexity 초과 |
| `REF-PARAM` | 파라미터 개수 초과 |
| `REF-GOD` | God Class |
| `REF-LOD` | Law of Demeter 위반 |
| `REF-FINAL` | 불변 필드 final 누락 |
| `REF-GUARD` | Guard Clause 적용 필요 (중첩 if) |

### Security (security-audit-agent)

| 코드 | 설명 |
|---|---|
| `SQL-INJ` | SQL Injection (MyBatis ${} 직접 치환) |
| `CMD-INJ` | Command Injection |
| `SEC-CRYPTO` | 취약 암호화 (MD5, SHA1 등) |
| `SEC-RAND` | 취약 난수 (`java.util.Random`) |
| `SEC-SECRET` | 하드코딩된 비밀값 |
| `SEC-DESER` | 안전하지 않은 역직렬화 |
| `SEC-XXE` | XXE 취약점 |

### Architecture (architecture-review-agent)

| 코드 | 설명 |
|---|---|
| `ARCH-LAYER` | 레이어 간 직접 의존 |
| `ARCH-CYCLE` | 순환 의존 |
| `ARCH-DDD` | DDD 경계 위반 (Entity 노출) |
| `ARCH-TX-01` | Controller @Transactional |
| `ARCH-TX-02` | Controller @Transactional(readOnly) |
| `ARCH-SEC-01` | SecurityFilterChain anyRequest 누락 |
| `ARCH-SEC-02` | permitAll 과다 적용 |
| `ARCH-SEC-03` | httpBasic 활성화 |

### Migration (db-migration-agent)

| 코드 | 설명 |
|---|---|
| `MIG-DROP` | DROP TABLE / TRUNCATE |
| `MIG-ALTER-NULL` | NOT NULL 컬럼 DEFAULT 없음 |
| `MIG-ALTER-DEF` | ADD COLUMN DEFAULT 없음 |
| `MIG-INDEX` | 대용량 테이블 인덱스 Lock |
| `MIG-NO-UNDO` | Undo 스크립트 부재 |
| `MIG-VERSION` | 버전 번호 불연속 |

---

## 5. Agent 감지 로직 (공통)

모든 Agent 는 위반 라인 감지 시 다음 순서로 처리한다:

```
1. 위반 라인 번호(N) 확인
2. N-1, N-2 라인에 @suppress 패턴 검색
   패턴: (//|--|\#|<!--)\s*@suppress\s+<코드>(\s+—\s+.+)?
3. 매칭 시:
   a. 사유 있음 → [SUPPRESSED:<코드>] 처리, 보고서에 억제 섹션 기록
   b. 사유 없음 → [SUPPRESS-NO-REASON] Medium 추가
4. 매칭 없음 → 원래 심각도로 보고
5. --strict 옵션 전달 시 → 2~3 단계 스킵, 항상 원래 심각도
```

---

## 6. 남용 방지

- `@suppress` 사유 텍스트 **필수** — 없으면 Medium 경고 (억제는 허용)
- 억제된 위반도 보고서 `### Suppressed` 섹션에 기록해 감사 추적 유지
- `--strict` 모드로 전체 위반 재확인 가능
- PR 코드리뷰 시 `[SUPPRESSED]` 항목을 리뷰어가 검토할 수 있도록 보고서 공유 권장
