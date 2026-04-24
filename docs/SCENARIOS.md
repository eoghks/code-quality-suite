# 시나리오 테스트 가이드

> `test/scenarios/` 의 더미 파일로 각 Agent 가 의도한 위반을 올바르게 감지하는지 검증한다.
> 실제 프로덕션 코드가 아니므로 빌드·실행 대상에서 제외한다.

---

## 파일 목록 및 위반 항목

### BadService.java — Refactor Agent 대상

| 위반 | 규칙 | 심각도 |
|---|---|---|
| `processOrder` 파라미터 4개 | refactor-rules §10.2 | Medium |
| `processOrder` CC = 12 (분기 11개) | refactor-rules §10.1 | Medium |
| `catch (Exception e)` 광범위 포착 | refactor-rules §11.1 | High |
| `e.printStackTrace()` | refactor-rules §11.1 | High |
| `FileInputStream` try-finally close | refactor-rules §11.2 | High |
| `PreparedStatement` try-finally close | refactor-rules §11.2 | High |

**검증 방법:**
```
/agent code-refactoring-agent test/scenarios/BadService.java
```
예상 결과: CC·파라미터·예외·Resource 위반 보고 + Parameter Object·try-with-resources 리팩토링 제안.

---

### GodClass.java — Refactor Agent 대상

| 위반 | 규칙 | 심각도 |
|---|---|---|
| 인스턴스 필드 16개 (10개 초과) | refactor-rules §10 | Medium |
| 메서드 31개 + 필드 16개 → God Class | refactor-rules §12.1 | **High** |
| `user.getAddress().getCity().getName().toUpperCase()` — 4단계 체인 | refactor-rules §12.2 | Medium |
| `order.getUser().getAddress().getCity().getZip().getCode()` — 5단계 체인 | refactor-rules §12.2 | Medium |

**검증 방법:**
```
/agent code-refactoring-agent test/scenarios/GodClass.java
```
예상 결과: God Class High 감지 + 클래스 분리 가이드 제안, LoD 체인 위임 메서드 제안.

---

### MutableDto.java — Refactor Agent 대상 (v0.3.0 Immutability)

| 위반 | 규칙 | 심각도 |
|---|---|---|
| `name`·`age`·`email`·`tags` — `final` 누락 | refactor-rules §13.1 | Medium |
| 생성자에서 `tags` 방어적 복사 없이 저장 | refactor-rules §13.4 | Medium |
| `getTags()` 내부 리스트 직접 반환 | refactor-rules §13.3 | Medium |
| 불변 DTO 3개 이상 → `record` 전환 권고 | refactor-rules §13.2 | Low |

**검증 방법:**
```
/agent code-refactoring-agent test/scenarios/MutableDto.java
```
예상 결과: final 추가, `new ArrayList<>(tags)` 방어적 복사, `Collections.unmodifiableList` 반환, `record` 전환 권고.

---

### NestedIfController.java — Refactor Agent 대상 (v0.3.0 Guard Clause)

| 위반 | 규칙 | 심각도 |
|---|---|---|
| `processOrder` — 중첩 if 4레벨 | refactor-rules §14.1 | Medium |
| `validate` — throw 후 else 블록 | refactor-rules §14.2 | Low |
| `categorize` — 중첩 if 4레벨 | refactor-rules §14.1 | Medium |

**검증 방법:**
```
/agent code-refactoring-agent test/scenarios/NestedIfController.java
```
예상 결과: Guard clause 패턴으로 조기 return 변환 제안, else 제거 제안.

---

### VulnerableMapper.xml — Security Agent 대상

| 위반 | 규칙 | 심각도 |
|---|---|---|
| `findByName` — `${name}` 직접 치환 | security/injection.md A03.1 | **Critical** |
| `findAllSorted` — 주석 없는 `${sortColumn}` | security/injection.md A03.1 | **Critical** |
| `search` — `${condition}` 동적 WHERE | security/injection.md A03.1 | **Critical** |

**검증 방법:**
```
/security-scan test/scenarios/VulnerableMapper.xml
```
예상 결과: Critical 3건 감지, `findAllSortedSafe` 는 주석 화이트리스트로 통과, BLOCK 마커 출력.

---

### InsecureService.java — Security Agent 대상

| 위반 | 규칙 | 심각도 |
|---|---|---|
| `MessageDigest.getInstance("MD5")` | security/crypto.md A02.1 | **Critical** |
| `new Random()` 토큰 생성 | security/crypto.md A02.2 | **Critical** |
| `Runtime.getRuntime().exec(userInput)` | security/injection.md A03.3 | **Critical** |
| `stored.equals(input)` 비밀번호 비교 | security/misc.md A07.1 | High |
| `password` 로그 출력 | security/misc.md A09.1 | **Critical** |
| AWS Key `AKIA...` 하드코딩 | security/crypto.md A02.3 | **Critical** |

**검증 방법:**
```
/security-scan test/scenarios/InsecureService.java
```
예상 결과: Critical 5건, High 1건 감지, BLOCK 마커 출력.

---

### LayerViolation.java — Architecture Agent 대상

| 위반 | 규칙 | 심각도 |
|---|---|---|
| `import com.example.repository.OrderRepository` (Controller 클래스) | architecture-rules §1.2 ARCH-LAYER-01 | **High** |
| `import com.example.repository.UserRepository` (Controller 클래스) | architecture-rules §1.2 ARCH-LAYER-01 | **High** |
| `import JdbcTemplate` (Controller 클래스) | architecture-rules §1.2 ARCH-LAYER-04 | **High** |

**검증 방법:**
```
/architecture-review test/scenarios/LayerViolation.java
```
예상 결과: ARCH-LAYER High 3건 감지, BLOCK 마커 출력.

---

### EntityExposed.java — Architecture Agent 대상

| 위반 | 규칙 | 심각도 |
|---|---|---|
| `@RequestBody UserEntity` — Entity 직접 수신 | architecture-rules §3.1 ARCH-DDD-01 | **High** |
| 반환 타입 `UserEntity` — Entity 직접 노출 | architecture-rules §3.1 ARCH-DDD-02 | **High** |

**검증 방법:**
```
/architecture-review test/scenarios/EntityExposed.java
```
예상 결과: ARCH-DDD High 2건 감지, BLOCK 마커 출력.

---

## 전체 시나리오 한 번에 실행

```
/run-pipeline --full test/scenarios/
```

예상 순서:
1. Refactor Agent: BadService·GodClass·MutableDto·NestedIfController 위반 보고
2. Architecture Agent: LayerViolation·EntityExposed BLOCK
3. Security Agent: VulnerableMapper·InsecureService BLOCK
4. Quality Agent: 전체 요약

---

## Baseline 연동 테스트

```
# 1. 현재 위반 전체 Baseline 등록
/baseline create test/scenarios/

# 2. 신규 위반 파일 추가 후 재스캔
# → 기존 위반은 [BASELINE], 신규 위반만 차단

# 3. Baseline 현황 확인
/baseline show
```

---

## 주의

- `test/scenarios/` 파일은 **컴파일·실행 대상 아님**. 의존 import 는 stub 으로 작성.
- `.gitignore` 에 포함하지 않음 — 팀 공유 시나리오 기준으로 유지.
- 신규 규칙 추가 시 대응 시나리오 파일을 이 디렉터리에 추가하고 이 문서를 함께 갱신.
