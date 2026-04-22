# 보안 검증 규칙 (security-rules)

> `security-audit-agent` 가 참조하는 **OWASP Top 10 + 추가 항목** 검증 기준.
> 읽기 전용 정적 스캔. 차단 판정 시 `.security-report.md` 에 `[BLOCK: SECURITY STOP]` 마커 출력.

---

## 0. 심각도 체계

| 태그 | 의미 | 차단 여부 |
|---|---|---|
| **Critical** | 즉시 악용 가능, 보안 사고 직결 (SQL Injection, RCE, 하드코딩 인증정보) | ✅ `[BLOCK: SECURITY STOP]` |
| **High** | 악용 경로 명확, 환경 조건에 따라 치명 (Deserialization, SSRF, Auth Failure) | ⚠️ 강력 권고 (차단은 프로젝트 정책) |
| **Medium** | 방어 계층 부재, 단독 악용은 어려움 (CSRF 미설정, Cookie Secure 누락) | 📝 검토 대상 |
| **Low** | 개선 제안, 컨텍스트 의존 (로그 포맷, 난수 생성기 선택) | 💡 참고 |

---

## A01. Broken Access Control

### A01.1 Controller 엔드포인트 인가 누락

`@RestController` / `@Controller` 클래스의 public 핸들러(`@GetMapping`·`@PostMapping`·`@PutMapping`·`@DeleteMapping`·`@RequestMapping`) 에 **인가 어노테이션 부재** 시 **High**.

```java
// ❌ 인가 없음
@GetMapping("/admin/users")
public List<User> listUsers() { ... }

// ✅ Spring Security
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/admin/users")
public List<User> listUsers() { ... }
```

**허용 어노테이션:** `@PreAuthorize` · `@PostAuthorize` · `@Secured` · `@RolesAllowed` (Jakarta) · `@PermitAll` (명시적 공개 표시).

**예외 판단:** 클래스 레벨 `@PreAuthorize` 선언 시 핸들러별 누락 허용. `/public`·`/health`·`/actuator` 등 명백히 공개 경로면 `@PermitAll` 명시 권고.

### A01.2 IDOR (Insecure Direct Object Reference)

`@PathVariable Long id` 를 **소유권 검증 없이** Repository 조회에 바로 전달 → **High**.

```java
// ❌ 다른 사용자의 주문 조회 가능
@GetMapping("/orders/{id}")
public Order getOrder(@PathVariable Long id) {
    return orderRepository.findById(id).orElseThrow();
}

// ✅ 소유권 확인
public Order getOrder(@PathVariable Long id, @AuthenticationPrincipal User user) {
    Order order = orderRepository.findById(id).orElseThrow();
    if (!order.getUserId().equals(user.getId())) throw new ForbiddenException();
    return order;
}
```

---

## A02. Cryptographic Failures

### A02.1 약한 해시·암호 알고리즘

| 패턴 | 판정 | 대체 |
|---|---|---|
| `MessageDigest.getInstance("MD5")` | **Critical** | SHA-256 이상 |
| `MessageDigest.getInstance("SHA-1")` | **High** | SHA-256 이상 |
| `Cipher.getInstance("DES/...")` / `DESede` | **Critical** | AES-256-GCM |
| `Cipher.getInstance("AES/ECB/...")` | **High** | AES-GCM / AES-CBC (IV 랜덤) |

### A02.2 보안 맥락 `java.util.Random`

인증 토큰·세션 ID·패스워드 리셋 토큰 생성에 `Random`·`Math.random()` 사용 → **Critical**.
→ `java.security.SecureRandom` 으로 대체.

### A02.3 하드코딩 인증정보 (Secret Scanning)

**정규식 패턴:**
| 종류 | 패턴 | 심각도 |
|---|---|---|
| AWS Access Key | `AKIA[0-9A-Z]{16}` | **Critical** |
| AWS Secret | `[A-Za-z0-9/+=]{40}` (context: `aws_secret`·`secretAccessKey`) | **Critical** |
| GitHub PAT | `ghp_[0-9a-zA-Z]{36}` · `github_pat_[0-9a-zA-Z_]{82}` | **Critical** |
| JWT | `eyJ[A-Za-z0-9_-]+\.eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+` | **High** (샘플 JWT 제외하려면 주석 명시) |
| Private Key | `-----BEGIN (RSA\|EC\|OPENSSH) PRIVATE KEY-----` | **Critical** |
| Slack Token | `xox[baprs]-[0-9a-zA-Z-]+` | **Critical** |
| 일반 비밀번호 | `(password\|passwd\|pwd)\s*=\s*"[^"]{6,}"` (소스·`application.yml`) | **High** |

**화이트리스트:** `src/test/**`, `*Test.java`, `*.example`·`*.sample` 파일은 제외 가능.

---

## A03. Injection

### A03.1 SQL Injection (`${}` 직접 치환) **Critical**

MyBatis 매퍼에서 `${}` 는 바인딩 아닌 **문자열 치환**. 사용자 입력이 닿는 모든 위치에서 **Critical 차단**.

```xml
<!-- ❌ Critical -->
<select>SELECT * FROM user WHERE name = '${name}'</select>

<!-- ✅ -->
<select>SELECT * FROM user WHERE name = #{name}</select>

<!-- ⚠️ 조건부 허용 — 주석으로 화이트리스트 검증 명시 필수 -->
<!-- sortColumn: Controller 화이트리스트 검증 완료 (NAME, CREATED_AT 만 허용) -->
<select>SELECT * FROM user ORDER BY ${sortColumn}</select>
```

### A03.2 SQL String Concatenation

`JdbcTemplate`·`EntityManager`·`NamedParameterJdbcTemplate` 호출 시 쿼리 문자열에 `+` 연산 → **Critical**.

```java
// ❌ Critical — PreparedStatement 파라미터 미사용
String sql = "SELECT * FROM user WHERE name='" + name + "'";
jdbcTemplate.queryForList(sql);

// ✅
jdbcTemplate.queryForList("SELECT * FROM user WHERE name = ?", name);
```

### A03.3 Command Injection

| 패턴 | 심각도 |
|---|---|
| `Runtime.getRuntime().exec(userInput)` | **Critical** |
| `new ProcessBuilder(userInput).start()` | **Critical** |
| `exec(String[])` 배열에 사용자 입력 포함 | **High** (쉘 메타문자 검증 필수) |

**완화:** 명령어는 화이트리스트, 인자만 파라미터 + 쉘 경유 없이(`/bin/sh -c` 사용 금지).

### A03.4 LDAP Injection

`DirContext.search(base, "(uid=" + userInput + ")", ...)` → **Critical**.
→ `SearchControls` + `encode` 유틸 (Apache Commons Lang `StringEscapeUtils` 또는 수동 `\`·`*`·`(`·`)` 이스케이프).

### A03.5 HTTP Header Injection (Response Splitting)

`response.setHeader(name, userInput)` / `HttpHeaders.set(k, userInput)` — 사용자 입력에 `\r\n` 포함 시 헤더 주입 → **High**. `\r`·`\n` 제거 검증 필수.

---

## A04. Insecure Design

### A04.1 CSRF 비활성 검증

`http.csrf(csrf -> csrf.disable())` 또는 `.csrf().disable()` 호출 시 **주석으로 사유 명시 없으면 High**.

허용 패턴:
- Stateless REST API + JWT → 주석 필수 (`// stateless JWT, CSRF 불필요`)
- 공개 읽기 전용 엔드포인트만 존재 → 주석 필수

### A04.2 Rate Limiting 부재 (참고)

인증 엔드포인트 (`/login`, `/register`, `/password/reset`) 에 Rate Limit 어노테이션·필터 부재 → **Medium**. 감지 근거가 약하므로 보고만.

### A04.3 Mass Assignment

`@RequestBody User user` 로 Entity 직접 바인딩 → **High**. Entity 에 `isAdmin`·`role` 같은 민감 필드가 있으면 요청 바디로 조작 가능.
→ DTO 분리, `@JsonIgnore` 또는 Jackson `@JsonView`.

---

## A05. Security Misconfiguration

### A05.1 Spring Boot 설정 위험값

**`application.yml` / `application.properties`:**

| 키 | 값 | 심각도 | 사유 |
|---|---|---|---|
| `spring.jpa.show-sql` | `true` (prod 프로파일) | **Medium** | 로그에 SQL + 파라미터 노출 |
| `spring.jpa.properties.hibernate.format_sql` | `true` + show-sql | **Low** | show-sql 과 동반 시만 |
| `management.endpoints.web.exposure.include` | `*` | **High** | Actuator 전면 공개 (heapdump/env/beans 등) |
| `management.endpoint.env.show-values` | `ALWAYS` | **High** | 환경 변수 노출 |
| `debug` / `trace` | `true` (prod) | **Medium** | 디버그 정보 누출 |
| `server.error.include-stacktrace` | `ALWAYS` | **High** | 스택트레이스 클라이언트 노출 |
| `server.error.include-message` | `ALWAYS` (prod) | **Low** | 내부 메시지 노출 |

### A05.2 CORS 와일드카드

`@CrossOrigin(origins = "*")` + `allowCredentials = true` → **High** (브라우저 차단되지만 설정 자체가 위험 신호).
→ 화이트리스트 도메인 명시.

### A05.3 기본 에러 페이지 (Whitelabel)

커스텀 `ErrorController`·`@ControllerAdvice` 없이 기본 Whitelabel Error Page 노출 → **Low**. 스택트레이스 · Spring 버전 정보 유출 가능.

---

## A06. Vulnerable and Outdated Components

### A06.1 알려진 취약 버전 (수동 목록)

**Agent 가 `pom.xml` / `build.gradle` 파싱 후 아래 목록 대조. 완전한 CVE DB 는 v0.5+ OWASP Dependency-Check 통합 예정.**

| 라이브러리 | 취약 버전 | 심각도 | 최소 권장 |
|---|---|---|---|
| `log4j-core` | < 2.17.1 | **Critical** (Log4Shell CVE-2021-44228) | 2.17.1+ |
| `spring-core` / `spring-web` | < 5.3.20 | **High** (Spring4Shell CVE-2022-22965) | 5.3.20+ (5.x) / 6.x |
| `spring-boot` | < 2.6.7 | **High** | 2.6.7+ |
| `jackson-databind` | < 2.13.4.2 | **High** (CVE-2022-42003 등) | 2.13.4.2+ |
| `commons-text` | < 1.10.0 | **Critical** (Text4Shell CVE-2022-42889) | 1.10.0+ |
| `snakeyaml` | < 2.0 | **High** (기본 constructor) | 2.0+ 또는 `SafeConstructor` |
| `mybatis` | < 3.5.6 | **Medium** (CVE-2020-26945) | 3.5.6+ |
| `h2database` | < 2.1.210 | **High** | 2.1.210+ |

**보고 형식:** `[DEP-VUL] pom.xml — log4j-core 2.14.1 → 2.17.1+ (CVE-2021-44228)`

---

## A07. Identification and Authentication Failures

### A07.1 Timing Attack (`String.equals` 비밀번호 비교)

```java
// ❌ 비교 길이별로 시간 차이 발생 → 타이밍 공격 가능
if (user.getPassword().equals(inputPassword)) { ... }

// ✅ 상수 시간 비교
if (MessageDigest.isEqual(
        user.getPassword().getBytes(), inputPassword.getBytes())) { ... }
```

→ **High**. 비밀번호·토큰·서명 비교는 `MessageDigest.isEqual` 또는 `java.security.MessageDigest` 기반 유틸.

### A07.2 Session Fixation

로그인 성공 시 `request.getSession().invalidate()` → 새 세션 생성 또는 `session.changeSessionId()` 호출 없으면 **Medium**.
Spring Security 기본값은 `migrateSession` 이므로 대부분 자동 처리. 수동 세션 관리 코드에서만 검증.

### A07.3 기본 비밀번호 패턴

소스·설정에 `admin/admin`, `root/root`, `password`, `123456` 등 기본 크레덴셜 하드코딩 → **Critical**.

---

## A08. Software and Data Integrity Failures

### A08.1 Unsafe Deserialization

| 패턴 | 심각도 | 대응 |
|---|---|---|
| `ObjectMapper.enableDefaultTyping()` (Jackson) | **Critical** | 제거, polymorphic type 은 `@JsonTypeInfo(use = NAME)` + `PolymorphicTypeValidator` |
| `new ObjectInputStream(...)` | **Critical** | 화이트리스트 `ObjectInputFilter` 설정 필수 |
| `XMLDecoder` (`java.beans.XMLDecoder`) | **Critical** | XML 파서 대체 (JAXB 등) |
| `new Yaml()` (SnakeYAML, 기본 constructor) | **Critical** | `new Yaml(new SafeConstructor(...))` |
| `SerializationUtils.deserialize` (Apache Commons) | **High** | 신뢰 경계 밖 데이터면 사용 금지 |

### A08.2 서명 검증 누락

JWT `parse` 시 `parseClaimsJwt` (서명 미검증) 사용 → **Critical**. 반드시 `parseClaimsJws` + 서명 키 제공.

---

## A09. Security Logging and Monitoring Failures

### A09.1 민감정보 로그 출력 **[Critical]**

**스캔 키워드** (대소문자 무관, 변수명·메시지 모두):

| 카테고리 | 키워드 |
|---|---|
| 인증 | `password` · `passwd` · `pwd` · `token` · `secret` · `apiKey` · `accessKey` · `privateKey` · `credential` · `authorization` · `bearer` · `sessionId` |
| PII (국내) | `ssn` · `jumin` · `주민등록번호` · `resident` · `phone` · `연락처` · `전화번호` · `mobile` · `email`(로그 맥락) · `cardNumber` · `카드번호` · `accountNumber` · `계좌번호` |

**감지 패턴:**
```java
// ❌ 모두 Critical
log.info("login: {}", password);
log.debug("token=" + token);
log.error("user info: " + user.toString());   // user.toString() 에 password 포함 가능
```

**응답 DTO 민감 필드:** Response DTO 에 `password`·`token` 등 포함 → **High**.

### A09.2 실패한 인증 로깅 부재

`/login` 실패 시 로그 출력 없으면 **Low** (IDS/SIEM 탐지 불가). 보안 이벤트 로깅 권고.

---

## A10. Server-Side Request Forgery (SSRF)

사용자 입력 URL 을 서버 측 HTTP 호출에 그대로 전달 → **High**.

| 패턴 | 심각도 |
|---|---|
| `restTemplate.getForObject(userInput, ...)` | **High** |
| `webClient.get().uri(userInput).retrieve()...` | **High** |
| `new URL(userInput).openConnection()` | **High** |
| `HttpClient.send(HttpRequest.newBuilder().uri(URI.create(userInput)))` | **High** |

**완화:**
- URL 화이트리스트 (도메인 레벨)
- 내부 IP 대역 (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `127.0.0.0/8`, `169.254.169.254` AWS metadata) 차단
- DNS rebinding 방어 (조회 결과 IP 검증 후 재호출)

---

## 추가 항목 (OWASP 외 자주 누락)

### X01. Cookie 보안 속성

`Cookie` 객체 또는 Spring `ResponseCookie`·`CookieBuilder` 생성 시:

| 속성 | 요구 | 심각도 |
|---|---|---|
| `HttpOnly` | `true` (세션·토큰 쿠키) | **Medium** 누락 시 |
| `Secure` | `true` (HTTPS 환경) | **High** 누락 시 |
| `SameSite` | `Strict` 또는 `Lax` | **Medium** 누락 시 |
| 만료 | 세션 쿠키 외 `Max-Age` 명시 | **Low** |

### X02. XXE (XML External Entity)

XML 파서 생성 시 외부 엔티티 비활성화 누락 → **Critical**.

```java
// ❌
DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();

// ✅
DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
f.setFeature("http://xml.org/sax/features/external-general-entities", false);
f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
f.setXIncludeAware(false);
f.setExpandEntityReferences(false);
```

**대상 클래스:** `DocumentBuilderFactory` · `SAXParserFactory` · `XMLInputFactory` · `TransformerFactory` · `SchemaFactory` · `XPathFactory`.

### X03. Path Traversal

```java
// ❌
File f = new File(baseDir, userInput);   // userInput = "../../etc/passwd"

// ✅
Path base = Paths.get(baseDir).toAbsolutePath().normalize();
Path target = base.resolve(userInput).normalize();
if (!target.startsWith(base)) throw new SecurityException("path traversal");
```

**감지 패턴:** `new File(userInput)`, `Paths.get(userInput)`, `FileInputStream(userInput)` — 사용자 입력이 파일명에 그대로 전달되고 `normalize`·`startsWith` 검증 부재 시 **High**.

---

## 보고 형식

Quality 보고서와 **분리된** `.security-report.md` 파일에 출력.

```
## Security Audit Report — <대상>

### Critical (즉시 조치)
- [SQL-INJ] UserMapper.xml:45 — ${name} 직접 치환 (A03)
- [HARDCODED-SECRET] application.yml:12 — AWS Access Key 패턴 검출 (A02)
- [UNSAFE-DESERIAL] ObjectMapperConfig.java:18 — enableDefaultTyping 호출 (A08)

### High
- [NO-AUTHZ] AdminController.java:32 — @PreAuthorize 누락 (A01)
- [SSRF] ProxyService.java:55 — restTemplate.getForObject(userInput) (A10)
- [TIMING-ATTACK] LoginService.java:78 — password.equals() 사용 (A07)

### Medium
- [WEAK-COOKIE] SessionConfig.java:23 — HttpOnly 미설정 (X01)
- [CSRF-DISABLED] SecurityConfig.java:40 — csrf().disable() 사유 주석 없음 (A04)

### Low
- [WHITELABEL] 커스텀 ErrorController 부재 (A05)

### 요약
- Critical: 3건 · High: 3건 · Medium: 2건 · Low: 1건

[BLOCK: SECURITY STOP]
```

**BLOCK 규칙:** Critical 1건 이상 → `[BLOCK: SECURITY STOP]`. Critical 0건 → `[PASS: SECURITY OK]`.

---

## security-audit-agent 권한

- ✅ `Read`, `Grep`, `Glob`
- ✅ `Bash(git diff:*)`, `Bash(git log:*)`, `Bash(git status:*)`, `Bash(git branch:*)`
- ❌ `Edit`, `Write` — 수정 금지 (Refactor Agent·개발자 책임)
- ❌ `Bash(git add:*)`, `Bash(git commit:*)` — 커밋 금지
- ❌ 테스트 실행 권한 없음 (Quality Agent 책임)
