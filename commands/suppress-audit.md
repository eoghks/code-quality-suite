---
name: suppress-audit
description: 프로젝트 전체 @suppress 주석 사용 현황 감사. 코드별/파일별/사유품질별 집계 후 .suppress-audit-report.md 생성. 남용 방지용.
---

# /suppress-audit

## 사용법

```
/suppress-audit [대상] [옵션]
```

## 인자

| 인자 | 설명 |
|---|---|
| (없음) | `src/main/` + `src/test/` + `db/migration/` 전체 |
| `<경로>` | 특정 파일/디렉터리만 |

## 옵션

| 옵션 | 설명 |
|---|---|
| `--format json` | JSON 형식 리포트 (`suppress-audit.json`) |
| `--min-count N` | 위반 코드당 N건 이상만 리포트 (기본 1) |
| `--stale-days N` | N일 초과 `@suppress` 를 stale 로 표시 (기본 180) |

---

## 동작

### 1. @suppress 주석 수집

화이트리스트 확장자(`.java`, `.xml`, `.sql`, `.yml`, `.properties`, `.kt`) 에서:

```
(//|--|\#|<!--)\s*@suppress\s+<CODE>(\s+—\s+<REASON>)?
```

각 매치에서 추출:
- 파일 경로 · 라인 번호
- 위반 코드 (`SEC-CMD`, `SQL-INJ`, `MIG-DROP` 등)
- 사유 텍스트 (없으면 `(사유 없음)`)
- `git blame` 으로 작성자·커밋 SHA·작성일 추출

### 2. 품질 검사

사유 텍스트 평가:

| 조건 | 품질 등급 |
|---|---|
| 사유 10자 이상 + "승인"·"검증"·"테스트"·"PR #" 키워드 포함 | ✅ 양호 |
| 사유 10자 이상, 근거 키워드 없음 | ⚠️ 보통 |
| 사유 10자 미만 | ❌ 부실 |
| `임시`, `TODO`, `나중에`, `?` 만 있음 | ❌ 불명확 |
| 사유 없음 (사유 필드 공란) | ❌ 누락 |

### 3. 집계

- 위반 코드별 억제 건수 (Top 10)
- 파일별 억제 건수 (Top 10)
- 작성자별 억제 건수
- 사유 품질 분포 (양호 / 보통 / 부실 / 누락 비율)
- stale 항목 (`--stale-days` 초과, 기본 180일)

### 4. `.suppress-audit-report.md` 생성

```markdown
## @suppress Audit — <프로젝트명>

**스캔 일시:** 2026-04-24 14:30
**대상 파일:** 245개 · **@suppress 총 건수:** 47건

### 위반 코드별 집계 (Top 10)

| 코드 | 건수 | 품질 양호 | 부실/누락 |
|---|---|---|---|
| SEC-CMD | 12건 | 3 | 9 ⚠️ |
| SQL-INJ | 8건 | 7 | 1 |
| REF-CC | 7건 | 2 | 5 ⚠️ |
| ...

### 파일별 집계 (Top 10)

| 파일 | 건수 |
|---|---|
| LegacyService.java | 9 |
| OrderMapper.xml | 5 |
| ...

### ❌ 품질 부실 / 누락 (15건)

- LegacyService.java:42 — [REF-CC] 사유: "임시" (불명확)
- OrderMapper.xml:18 — [SQL-INJ] 사유 없음
- ...

### ⏳ Stale 항목 (180일 초과 — 11건)

- UserController.java:85 — [SEC-CMD] 등록일: 2025-09-01 (235일 경과)
  작성자: holdhwan · 커밋: a3b4c5d
- ...

### 작성자별 집계

| 작성자 | 건수 | 품질 양호율 |
|---|---|---|
| holdhwan | 21건 | 45% |
| ...

### 요약
- 총 억제: 47건
- 품질 양호: 18건 (38%)
- 품질 부실/누락: 15건 (32%) ⚠️
- Stale (180일+): 11건 (23%)

### 권장 조치
1. 품질 부실/누락 15건 → 사유 보강 또는 억제 해제
2. Stale 11건 → 재검토 후 해결 또는 사유 갱신
3. LegacyService.java 9건 집중 → 리팩토링 우선순위 검토
```

---

## PR 정책 힌트

`--pr-check` 옵션 (향후 v0.6 CI/CD 통합):

- PR 당신규 `@suppress` 추가 건수 한도 (기본 2건)
- 초과 시 PR 코멘트로 리뷰어 주의 환기
- GitHub Actions `/generate-workflow` 에 포함

---

## 참고

- @suppress 문법: `rules/suppress-policy.md`
- Baseline 과의 차이: @suppress = 인라인 단건 억제, Baseline = 파일 단위 대량 등록
- 남용 방지: 이 커맨드 + `rules/prompt-safety.md` (악성 억제 방어)
