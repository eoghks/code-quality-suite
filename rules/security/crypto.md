# 암호화 규칙 (A02 — Cryptographic Failures)

> `security-audit-agent` 가 로드하는 카테고리 파일.
> **약한 알고리즘·Random·하드코딩 인증정보** 감지.

---

## A02.1 약한 해시·암호 알고리즘

| 패턴 | 심각도 | 대체 |
|---|---|---|
| `MessageDigest.getInstance("MD5")` | **Critical** | SHA-256 이상 |
| `MessageDigest.getInstance("SHA-1")` | **High** | SHA-256 이상 |
| `Cipher.getInstance("DES/...")` / `DESede` | **Critical** | AES-256-GCM |
| `Cipher.getInstance("AES/ECB/...")` | **High** | AES-GCM / AES-CBC (IV 랜덤) |

---

## A02.2 보안 맥락 `java.util.Random` **[Critical]**

인증 토큰·세션 ID·비밀번호 리셋 토큰 생성에 `Random`·`Math.random()` 사용.

```java
// ❌ Critical
String token = String.valueOf(new Random().nextLong());

// ✅
String token = new BigInteger(130, new SecureRandom()).toString(32);
```

---

## A02.3 하드코딩 인증정보 (Secret Scanning)

**정규식 패턴 — 소스·설정 파일 전체 스캔:**

| 종류 | 패턴 | 심각도 |
|---|---|---|
| AWS Access Key | `AKIA[0-9A-Z]{16}` | **Critical** |
| AWS Secret | `[A-Za-z0-9/+=]{40}` (context: `aws_secret`·`secretAccessKey`) | **Critical** |
| GitHub PAT | `ghp_[0-9a-zA-Z]{36}` · `github_pat_[0-9a-zA-Z_]{82}` | **Critical** |
| JWT | `eyJ[A-Za-z0-9_-]+\.eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+` | **High** |
| Private Key | `-----BEGIN (RSA\|EC\|OPENSSH) PRIVATE KEY-----` | **Critical** |
| Slack Token | `xox[baprs]-[0-9a-zA-Z-]+` | **Critical** |
| 일반 비밀번호 | `(password\|passwd\|pwd)\s*=\s*"[^"]{6,}"` | **High** |

**화이트리스트:** `src/test/**`, `*Test.java`, `*.example`, `*.sample` 파일 제외 가능.
