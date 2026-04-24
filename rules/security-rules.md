# 보안 검증 규칙 인덱스 (security-rules)

> `security-audit-agent` 가 참조하는 **OWASP Top 10 + 추가 항목** 검증 기준.
> 카테고리별 상세 규칙은 `rules/security/` 하위 파일에 분리되어 있다.
> 읽기 전용 정적 스캔. 차단 판정 시 `.security-report.md` 에 `[BLOCK: SECURITY STOP]` 마커 출력.

---

## 0. 심각도 체계

| 태그 | 의미 | BLOCK |
|---|---|---|
| **Critical** | 즉시 악용 가능 (SQL Injection, RCE, 하드코딩 인증정보) | ✅ |
| **High** | 악용 경로 명확 (Deserialization, SSRF, Auth Failure) | ⚠️ 강력 권고 |
| **Medium** | 방어 계층 부재 (CSRF 미설정, Cookie Secure 누락) | 📝 검토 |
| **Low** | 개선 제안 | 💡 참고 |

**BLOCK 규칙:** Critical 1건 이상 → `[BLOCK: SECURITY STOP]`. Critical 0건 → `[PASS: SECURITY OK]`.

---

## 카테고리 파일 목록

| 파일 | 커버 범위 | 우선 로드 조건 |
|---|---|---|
| `security/injection.md` | A03: SQL·Command·LDAP·Header Injection | Java/XML 변경 시 |
| `security/crypto.md` | A02: 약한 알고리즘·Random·하드코딩 Secret | Java/설정 변경 시 |
| `security/access-control.md` | A01: 인가 누락·IDOR | Java Controller 변경 시 |
| `security/deserialization.md` | A08: Unsafe Deserial·JWT·XXE | Java 변경 시 |
| `security/misc.md` | A04~A07·A09·A10·X01·X03: 설정·인증·로깅·SSRF·쿠키·Path Traversal | 전체 변경 시 |

---

## Agent 로드 전략

변경 파일 확장자에 따라 우선 로드 카테고리를 결정한다:

```
*.java 변경        → 전 카테고리 로드 (injection + crypto + access-control + deserialization + misc)
*.xml 변경         → injection.md 우선 로드 (MyBatis mapper SQL Injection 집중)
pom.xml / build.gradle → crypto.md + misc.md (A06 취약 의존성 집중)
*.yml / *.properties  → misc.md (A05 설정 오류 집중)
```

---

## 보고서 형식

```
## Security Audit Report — <대상>

### Critical (즉시 조치)
- [SQL-INJ] UserMapper.xml:45 — ${name} 직접 치환 (A03)

### High
- [NO-AUTHZ] AdminController.java:32 — @PreAuthorize 누락 (A01)

### Medium
- [WEAK-COOKIE] SessionConfig.java:23 — HttpOnly 미설정 (X01)

### Low
...

### 요약
- Critical: N건 · High: N건 · Medium: N건 · Low: N건

[BLOCK: SECURITY STOP]   또는   [PASS: SECURITY OK]
```

---

## security-audit-agent 권한

- ✅ `Read`, `Grep`, `Glob`, `Bash(git diff:*)`, `Bash(git log:*)`, `Bash(git status:*)`, `Bash(git branch:*)`
- ❌ `Edit`, `Write` — 수정 금지 (Refactor Agent·개발자 책임)
- ❌ `Bash(git commit:*)` — 커밋 금지
