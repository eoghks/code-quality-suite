---
name: security-scan
description: OWASP Top 10 기반 보안 취약점 단독 스캔. security-audit-agent 를 직접 호출해 .security-report.md 생성. 리팩토링 없이 보안 점검만 실행할 때 사용.
---

# /security-scan

## 용도

`security-audit-agent` 를 **단독 호출**하는 슬래시 커맨드입니다. 리팩토링이나 품질 검증 없이 **보안 스캔만** 빠르게 실행할 때 사용합니다.

```
/security-scan [대상]
```

## 인자 (`$ARGUMENTS`)

| 인자 형식 | 동작 |
|---|---|
| 없음 | 현재 브랜치 스테이징 변경 (`git diff --cached`) |
| `src/main/java/com/example/UserService.java` | 지정 파일 단독 스캔 |
| `src/main/java/com/example/` | 지정 디렉터리 재귀 스캔 |
| `feature/user-login` | 해당 브랜치의 변경 사항 (main 과 diff) |
| `--all` | 전체 소스 디렉터리 (`src/main/**`) — 초기 전수 감사 |
| `--strict` | Baseline 무시 + 전수 스캔 |

## 실행 절차

1. **`security-audit-agent` 호출** — `$ARGUMENTS` 를 검증 범위 컨텍스트로 전달
2. Agent 가 OWASP A01~A10 + Cookie/XXE/PathTraversal 스캔 수행
3. 결과가 `.security-report.md` 에 저장됨
4. 보고서 요약을 메인 세션에 출력

## 출력 예시

```
Security scan 완료 → .security-report.md
- Critical: 1건 [BLOCK: SECURITY STOP]
- High: 2건  · Medium: 1건  · Low: 0건
- Baseline 제외: 3건

주요 발견:
  [SQL-INJ] LegacyMapper.xml:45 — ${name} 직접 치환 (A03)
  [NO-AUTHZ] AdminController.java:32 — @PreAuthorize 누락 (A01)

커밋 전 반드시 수정 필요. Refactor Agent 또는 개발자가 수정 후 재스캔 권고.
```

## 주의사항

- 보안 스캔만 수행합니다 — 리팩토링 또는 테스트 실행 **미포함**
- 전체 파이프라인은 `/run-pipeline` 사용
- `--strict` 는 레거시 포함 전수 감사용 (시간이 더 걸림)
