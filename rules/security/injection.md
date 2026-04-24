# 인젝션 규칙 (A03 — Injection)

> `security-audit-agent` 가 로드하는 카테고리 파일.
> **MyBatis mapper XML, JdbcTemplate, Command·LDAP·Header Injection** 감지.

---

## A03.1 SQL Injection — `${}` 직접 치환 **[Critical]**

MyBatis 매퍼에서 `${}` 는 문자열 치환. 사용자 입력이 닿으면 즉시 차단.

```xml
<!-- ❌ Critical -->
<select>SELECT * FROM user WHERE name = '${name}'</select>

<!-- ✅ -->
<select>SELECT * FROM user WHERE name = #{name}</select>

<!-- ⚠️ 화이트리스트 주석 명시 시 허용 -->
<!-- sortColumn: Controller 화이트리스트 검증 완료 (NAME, CREATED_AT 만 허용) -->
<select>SELECT * FROM user ORDER BY ${sortColumn}</select>
```

**감지:** XML 파일에서 `${` 포함 라인 → 상위 context 에 사용자 입력 전달 여부 확인.

---

## A03.2 SQL String Concatenation **[Critical]**

`JdbcTemplate`·`EntityManager`·`NamedParameterJdbcTemplate` 호출 시 쿼리에 `+` 연산.

```java
// ❌ Critical
String sql = "SELECT * FROM user WHERE name='" + name + "'";
jdbcTemplate.queryForList(sql);

// ✅
jdbcTemplate.queryForList("SELECT * FROM user WHERE name = ?", name);
```

**감지:** `jdbcTemplate.*For*(` 또는 `em.createNativeQuery(` 인자에 문자열 연결 `+` 패턴.

---

## A03.3 Command Injection **[Critical/High]**

| 패턴 | 심각도 |
|---|---|
| `Runtime.getRuntime().exec(userInput)` | **Critical** |
| `new ProcessBuilder(userInput).start()` | **Critical** |
| `exec(String[])` 배열에 사용자 입력 포함 | **High** |

**완화:** 명령어는 화이트리스트 고정, 인자만 파라미터. `/bin/sh -c` 경유 금지.

---

## A03.4 LDAP Injection **[Critical]**

`DirContext.search(base, "(uid=" + userInput + ")", ...)` → **Critical**.

**완화:** `\`·`*`·`(`·`)` 이스케이프 또는 Apache Commons Lang `LdapUtils.escapeFilterValue`.

---

## A03.5 HTTP Header Injection (Response Splitting) **[High]**

`response.setHeader(name, userInput)` — `\r\n` 포함 시 헤더 주입.

**감지:** `setHeader`·`addHeader`·`HttpHeaders.set` 호출 인자에 사용자 입력 전달.  
**완화:** `\r`·`\n` 제거 검증 필수.
