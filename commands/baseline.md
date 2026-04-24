---
name: baseline
description: Baseline 관리 커맨드. create / update / show / audit / extend 서브커맨드로 .quality-baseline.json 을 생성·갱신·조회·감사·연장. 레거시 프로젝트 점진적 도입 시 사용.
---

# /baseline

## 용도

`.quality-baseline.json` 을 생성·관리하는 커맨드입니다. **레거시 프로젝트**에 Plugin 을 처음 도입하거나 해결된 위반을 정리할 때 사용합니다.

```
/baseline <create|update|show|audit|extend>
```

---

## 서브커맨드

### `/baseline create`

**최초 도입 시 사용.** 현재 전체 소스의 위반을 스캔해 `.quality-baseline.json` 을 생성합니다. 이후 신규 위반만 차단 대상이 됩니다.

**실행 절차:**
1. `security-audit-agent` 와 `code-quality-agent` 를 `--strict` 모드로 호출 (전수 스캔)
2. 발견된 모든 위반 목록 수집
3. 각 위반의 fingerprint 계산 (파일경로 + code + 정규화 메시지 SHA-256)
4. `.quality-baseline.json` 생성 (프로젝트 루트)
5. 생성 결과 요약 출력

**출력 예시:**
```
Baseline 생성 완료 → .quality-baseline.json
- 등록된 위반: 47건
  - METHOD-LEN: 12건
  - N+1: 8건
  - SQL-INJ: 3건 (레거시 매퍼, 주석 명시 필수)
  - NULL-RETURN: 15건
  - LAYER: 9건

다음 단계:
  git add .quality-baseline.json
  git commit -m "chore: 초기 품질 baseline 등록"
  이후 신규 위반만 차단 대상
```

**주의:** `TEST-FAIL` · `HARDCODED-SECRET` 은 Baseline 등록 불가 — 즉시 수정 필요.

---

### `/baseline update`

**해결된 위반 정리.** 개발팀이 기존 Baseline 위반을 수정한 뒤 baseline 에서 제거합니다. 신규 위반은 추가하지 않습니다.

**실행 절차:**
1. 현재 `.quality-baseline.json` 로드
2. `code-quality-agent` + `security-audit-agent` 로 현재 상태 재스캔
3. Baseline 항목 중 현재 스캔에서 **사라진 fingerprint** 자동 제거
4. `updated` 타임스탬프 갱신
5. 제거된 항목 목록 출력 후 파일 저장

**출력 예시:**
```
Baseline 업데이트 완료
- 제거된 위반 (해결): 8건
  - NULL-RETURN: UserService.java (5건)
  - METHOD-LEN: OrderService.java (3건)
- 남은 Baseline 위반: 39건
- .quality-baseline.json updated: 2026-05-10T14:30:00Z
```

---

### `/baseline show`

**현황 조회.** `.quality-baseline.json` 의 현재 상태를 카테고리별로 요약 출력합니다.

**출력 예시:**
```
## Baseline 현황 (.quality-baseline.json)
생성일: 2026-04-21 | 마지막 업데이트: 2026-05-10 | generator: code-quality-suite@0.2.0

### 카테고리별 건수
- METHOD-LEN : 7건
- NULL-RETURN : 10건
- N+1        : 8건
- SQL-INJ    : 3건  ← 레거시 매퍼, 마이그레이션 예정
- LAYER      : 9건
- MAP-PARAM  : 2건
합계: 39건

### 최고령 항목 (오래된 순)
1. OrderService.java:156 — METHOD-LEN (등록: 2026-04-21, 19일 경과)
2. LegacyMapper.xml:45  — SQL-INJ     (등록: 2026-04-21, 19일 경과)

### 권고
- SQL-INJ 3건: 마이그레이션 일정 확인 필요
- 30일 이상 경과 항목이 존재하면 /baseline update 실행 권고
```

---

### `/baseline audit` (v0.5.0+)

**등록된 baseline 의 나이·만료 상태 감사.** Stale (90일 초과) / 만료 임박 (30일 이내) / 만료됨 항목을 분류 보고.

**실행 절차:**
1. `.quality-baseline.json` 로드 (version "1" 이면 자동으로 "2" 마이그레이션)
2. 각 violation 의 `registered_at` · `expires_at` 과 현재 시각 비교
3. 분류:
   - **만료됨 (`now ≥ expires_at`)** → 정상 위반으로 승격, 보고서에서 BLOCK 판정 반영
   - **만료 임박 (`expires_at - 30일 ≤ now`)** → `[BASELINE-EXPIRING]` 경고
   - **Stale (등록 후 90일+ 해결 안됨)** → 재검토 권고
4. `.baseline-audit-report.md` 생성

**출력 예시:**
```
## Baseline Audit — 2026-10-24

### 🔴 만료됨 (3건 — 정상 위반으로 승격)
- [SQL-INJ] LegacyMapper.xml:45 — 등록: 2026-04-21, 만료: 2026-10-18 (경과 6일)
  권고: 즉시 해결 또는 /baseline extend --days 90 --reason "..."

### 🟡 만료 임박 (5건 — 30일 이내)
- [METHOD-LEN] OrderService.java:156 — 만료: 2026-11-10 (17일 남음)
- [N+1] UserService.java:78 — 만료: 2026-11-15 (22일 남음)

### ⏳ Stale (7건 — 90일+ 미해결)
- [LAYER] LegacyController.java:8 — 등록: 2026-04-21 (186일 경과)

### 요약
- 총 baseline: 39건
- 만료됨: 3건 · 만료 임박: 5건 · Stale: 7건 · 정상: 24건

### 권장 조치
1. 만료 3건 → 이번 스프린트에 해결 또는 /baseline extend
2. 만료 임박 5건 → 다음 스프린트 계획 포함
3. Stale 7건 → 리팩토링 우선순위 재검토
```

---

### `/baseline extend <fingerprint> --days N --reason "<사유>"`

**만료 기한 연장.** 해결 불가능한 기술 부채는 사유와 함께 연장.

```bash
/baseline extend a3f8c2d1e5b9704f --days 90 --reason "Q1 2027 레거시 마이그레이션 프로젝트 완료 시 일괄 처리"
```

- `fingerprint` 앞 8자 이상만 입력해도 매칭 (Unique 매칭 필수)
- `--reason` 은 10자 이상 필수
- 연장 이력은 violation 객체의 `extensions` 배열에 누적 기록 (감사 추적)

---

## Baseline 파일 관리

```bash
# 생성 후 반드시 git 커밋
git add .quality-baseline.json
git commit -m "chore: 초기 품질 baseline 등록 (47건)"

# 업데이트 후도 동일
git add .quality-baseline.json
git commit -m "chore: baseline 갱신 — 8건 해결 (NULL-RETURN/METHOD-LEN)"
```

**`.gitignore` 추가 금지** — Baseline 은 팀 공유 파일입니다.

---

## strict 모드와 함께 사용

```bash
# Baseline 무시하고 전수 감사 (정기 감사용)
/run-pipeline --strict

# 또는 보안만
/security-scan --strict
```

strict 모드는 Baseline 을 완전히 무시합니다. 레거시 포함 전체가 차단 대상이므로 정기 감사·마이그레이션 완료 확인 시 사용.
