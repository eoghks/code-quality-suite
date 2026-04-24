# 기타 보안 규칙 (A04·A05·A06·A07·A09·A10·X01·X03)

> `security-audit-agent` 가 로드하는 카테고리 파일.
> **설계 결함·설정 오류·취약 의존성·인증 실패·로깅·SSRF·쿠키·Path Traversal** 감지.

---

## A04. Insecure Design

### A04.1 CSRF 비활성 **[High]**

`http.csrf(csrf -> csrf.disable())` 또는 `.csrf().disable()` — 주석 사유 없으면 High.

허용:
- Stateless REST + JWT → 주석 필수: `// stateless JWT, CSRF 불필요`

### A04.2 Rate Limiting 부재 **[Medium]**

`/login`·`/register`·`/password/reset` 에 Rate Limit 어노테이션·필터 부재.

### A04.3 Mass Assignment **[High]**

`@RequestBody UserEntity user` — Entity 직접 바인딩. `isAdmin`·`role` 등 민감 필드 조작 가능.
→ DTO 분리 또는 `@JsonIgnore`.

---

## A05. Security Misconfiguration

### A05.1 Spring Boot 설정 위험값

| 키 | 값 | 심각도 |
|---|---|---|
| `spring.jpa.show-sql` | `true` (prod) | **Medium** |
| `management.endpoints.web.exposure.include` | `*` | **High** |
| `management.endpoint.env.show-values` | `ALWAYS` | **High** |
| `debug` / `trace` | `true` (prod) | **Medium** |
| `server.error.include-stacktrace` | `ALWAYS` | **High** |

### A05.2 CORS 와일드카드 **[High]**

`@CrossOrigin(origins = "*")` + `allowCredentials = true` → 화이트리스트 도메인 명시.

---

## A06. Vulnerable Components

**`pom.xml` / `build.gradle` 파싱 후 아래 목록 대조:**

| 라이브러리 | 취약 버전 | 심각도 | 권장 버전 |
|---|---|---|---|
| `log4j-core` | < 2.17.1 | **Critical** | 2.17.1+ |
| `spring-core` / `spring-web` | < 5.3.20 | **High** | 5.3.20+ |
| `spring-boot` | < 2.6.7 | **High** | 2.6.7+ |
| `jackson-databind` | < 2.13.4.2 | **High** | 2.13.4.2+ |
| `commons-text` | < 1.10.0 | **Critical** | 1.10.0+ |
| `snakeyaml` | < 2.0 | **High** | 2.0+ |
| `mybatis` | < 3.5.6 | **Medium** | 3.5.6+ |
| `h2database` | < 2.1.210 | **High** | 2.1.210+ |

---

## A07. Auth Failures

### A07.1 Timing Attack **[High]**

```java
// ❌ 타이밍 공격 가능
if (user.getPassword().equals(inputPassword)) { ... }

// ✅ 상수 시간 비교
if (MessageDigest.isEqual(hash1, hash2)) { ... }
```

### A07.2 Session Fixation **[Medium]**

로그인 성공 후 세션 재생성 (`invalidate()` + 신규 세션) 누락.

### A07.3 기본 비밀번호 하드코딩 **[Critical]**

`admin/admin`, `root/root`, `password`, `123456` 등 소스·설정에 하드코딩.

---

## A09. Logging Failures

### A09.1 민감정보 로그 출력 **[Critical]**

**스캔 키워드:**

| 카테고리 | 키워드 |
|---|---|
| 인증 | `password` · `passwd` · `token` · `secret` · `apiKey` · `accessKey` · `credential` · `authorization` · `bearer` · `sessionId` |
| PII | `ssn` · `jumin` · `주민등록번호` · `phone` · `연락처` · `cardNumber` · `계좌번호` · `accountNumber` |

```java
// ❌ Critical
log.info("login: {}", password);
log.debug("token=" + token);
```

### A09.2 인증 실패 로깅 부재 **[Low]**

`/login` 실패 시 보안 이벤트 로그 없음 → IDS/SIEM 탐지 불가.

---

## A10. SSRF

| 패턴 | 심각도 |
|---|---|
| `restTemplate.getForObject(userInput, ...)` | **High** |
| `webClient.get().uri(userInput).retrieve()` | **High** |
| `new URL(userInput).openConnection()` | **High** |

**완화:** URL 화이트리스트, 내부 IP 대역 차단 (`127.0.0.0/8`, `169.254.169.254` 포함).

---

## X01. Cookie 보안 속성

| 속성 | 요구 | 심각도 |
|---|---|---|
| `HttpOnly` | `true` | **Medium** 누락 시 |
| `Secure` | `true` | **High** 누락 시 |
| `SameSite` | `Strict` 또는 `Lax` | **Medium** 누락 시 |

---

## X03. Path Traversal **[High]**

```java
// ❌
File f = new File(baseDir, userInput);

// ✅
Path base = Paths.get(baseDir).toAbsolutePath().normalize();
Path target = base.resolve(userInput).normalize();
if (!target.startsWith(base)) throw new SecurityException("path traversal");
```

**감지:** `new File(userInput)`, `Paths.get(userInput)`, `FileInputStream(userInput)` — 검증 없이 직접 전달.
