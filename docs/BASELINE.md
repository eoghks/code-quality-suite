# BASELINE 가이드

레거시 프로젝트에 `code-quality-suite` v0.2.0 을 **점진적으로 도입**하기 위한 Baseline 시스템 사용법.

---

## 1. Baseline 이란?

기존에 이미 존재하는 위반을 `.quality-baseline.json` 에 기록해 두면:
- **신규·재발 위반** → 정상 차단 대상
- **기존 등록 위반** → `[BASELINE]` 태그로 분리 보고, 차단 안 함

레거시 코드의 기술 부채가 수백 건이어도 **신규 코드만 빡세게** 관리할 수 있습니다.

---

## 2. 최초 도입 순서

### Step 1. 전수 스캔 + Baseline 생성

```bash
/baseline create
```

현재 소스 전체를 스캔해 모든 위반을 `.quality-baseline.json` 에 등록합니다.

출력 예:
```
Baseline 생성 완료 → .quality-baseline.json
- 등록된 위반: 47건 (METHOD-LEN: 12, N+1: 8, SQL-INJ: 3, ...)
```

### Step 2. Baseline 파일 커밋

```bash
git add .quality-baseline.json
git commit -m "chore: 초기 품질 baseline 등록 (47건)"
```

팀 전체가 동일한 baseline 을 공유합니다.

### Step 3. 이후 개발 — 신규 위반만 차단

```bash
/run-pipeline          # baseline 자동 적용, 신규 위반만 차단
```

---

## 3. Baseline 해제 (해결된 위반 제거)

레거시 위반을 수정했으면 baseline 에서 제거합니다.

```bash
/baseline update
```

현재 소스를 재스캔해 사라진 fingerprint 를 자동 제거합니다.

```
Baseline 업데이트 완료
- 제거된 위반 (해결): 8건 (NULL-RETURN 5건, METHOD-LEN 3건)
- 남은 Baseline 위반: 39건
```

```bash
git add .quality-baseline.json
git commit -m "chore: baseline 갱신 — 8건 해결"
```

---

## 4. 현황 조회

```bash
/baseline show
```

```
## Baseline 현황
생성일: 2026-04-21 | 마지막 업데이트: 2026-05-10

카테고리별 건수:
- METHOD-LEN  7건
- NULL-RETURN 10건
- N+1         8건
- SQL-INJ     3건  ← 마이그레이션 예정
합계: 39건

최고령 항목 (30일 이상):
- LegacyMapper.xml:45 — SQL-INJ (등록: 2026-04-21)
```

---

## 5. Strict 모드 (레거시 포함 전수 감사)

```bash
/run-pipeline --strict      # Baseline 무시, 모든 위반 차단 대상
/security-scan --strict     # 보안만 전수 스캔
```

정기 감사나 마이그레이션 완료 확인 시 사용.

---

## 6. Baseline 파일 포맷

```json
{
  "version": "1",
  "created": "2026-04-21T10:00:00Z",
  "updated": "2026-05-10T14:30:00Z",
  "generator": "code-quality-suite@0.2.0",
  "violations": [
    {
      "file": "src/main/java/com/example/LegacyService.java",
      "line": 200,
      "code": "METHOD-LEN",
      "message": "73줄 (50줄 초과)",
      "fingerprint": "a3f8c2d1e5b9704f"
    }
  ]
}
```

`fingerprint` = SHA-256(파일경로 + code + 정규화 메시지) 앞 16자. 라인 번호가 바뀌어도 같은 위반으로 인식.

---

## 7. 팀 운영 원칙

| 원칙 | 설명 |
|---|---|
| **git 커밋 필수** | `.quality-baseline.json` 은 팀 공유 파일. 항상 커밋. |
| **PR 리뷰 대상** | 신규 위반을 baseline 에 추가하는 PR 은 팀 합의 필요. |
| **해제 일정 명시** | `message` 필드에 해제 예정일·조건 기재 권장. |
| **TEST-FAIL·Secret 불허** | 테스트 실패·하드코딩 인증정보는 baseline 등록 불가. |
| **정기 업데이트** | 스프린트 종료 시 `/baseline update` 로 해결 항목 정리. |

---

## 8. 주의사항

- `.quality-baseline.json` 을 `.gitignore` 에 추가하면 팀 공유가 안 됩니다. **절대 추가 금지**.
- baseline 이 너무 많이 쌓이면 `/run-pipeline --strict` 로 현실을 파악하고 마이그레이션 계획 수립.
- strict 모드는 CI/CD 파이프라인에 정기적으로 연결하면 유용 (예: 월 1회 전수 감사).
