# 품질 준수·테스트 규칙 (quality/compliance-testing)

> §1~3: 보안 smoke check·규칙 준수 검증·테스트 커버리지 검증

---

## 1. 보안 검증 (보조) — `security-rules.md` 참조

보안 주 책임은 `security-audit-agent`. Quality Agent 는 **최소 스모크 테스트**로 아래 2개만 유지.

### 1.1 SQL Injection `${}` 최소 체크 [Critical]

`${}` 직접 치환 발견 + 주석에 `화이트리스트`·`검증` 단어 없음 → **Critical + BLOCK 마커**.
상세 규칙은 `security-rules.md §A03.1` 참조.

### 1.2 민감정보 로그 출력 최소 체크 [High]

로그 메시지에 `password`·`token`·`secret` 3개 키워드 동반 시 **High 보고**.

### 1.3 입력 검증 지침 요약

Controller 진입점 Bean Validation (`@Valid`·`@NotNull`·`@Size`·`@Pattern`) 부재 → **Medium 보고**.

---

## 2. 규칙 준수 검증 (Rule Compliance)

### 2.1 `javax.*` → `jakarta.*` 강제

```java
// ❌ import javax.servlet.http.HttpServletRequest;
// ✅ import jakarta.servlet.http.HttpServletRequest;
```

### 2.2 메서드 50줄 제한 검증

초과 시 **Medium 보고** (자동 수정 권고 → Refactor Agent).

### 2.3 레이어 책임 분리 검증

| 레이어 | 금지 패턴 |
|---|---|
| **Controller** | `@Transactional`, `Repository`/`Dao` 직접 주입, 비즈니스 조건 분기 |
| **Service** | `HttpServletRequest`/`HttpServletResponse` 의존, `JdbcTemplate` 직접 사용 |
| **Dao/Repository** | 트랜잭션 조작, 도메인 로직, 외부 API 호출 |

### 2.4 Lombok 사용 검증

- 금지: `@Data`, 무분별한 `@EqualsAndHashCode` → **High 보고**
- 허용 외 Lombok 어노테이션 → **Low 보고**

### 2.5 null 반환 검증

`return null;` 검출 → null 정책 적용 권고 **Medium 보고**.

### 2.6 `Map<String,Object>` 인자·반환 검증

함수 시그니처에서 `Map<String, Object>` 검출 시 DTO 전환 권고 **Medium 보고**. 주석 사유 명시 시 제외.

---

## 3. 테스트 커버리지 검증 (신규/변경 메서드 기준)

**검증 범위:** 현재 diff 의 신규·변경 메서드에 한정.

### 3.1 검증 절차

1. `git diff --cached` 로 변경 파일 수집
2. 변경된 `public`·`protected` 메서드 추출
3. 대응 테스트 클래스·메서드 존재 여부 확인
   - 관례: `src/main/java/**/Foo.java` → `src/test/java/**/FooTest.java`
4. 누락된 경우 **High 보고**

### 3.2 테스트 실행

| 감지 파일 | 실행 명령 |
|---|---|
| `pom.xml` | `mvn test` |
| `build.gradle` / `build.gradle.kts` | `./gradlew test` (Windows: `gradlew.bat test`) |
| 둘 다 없음 | 테스트 실행 스킵 + Low 보고 |

결과 형식: `통과 N건 / 실패 M건`  
실패 시 실패 테스트 목록 출력 → **Critical 보고**.
