---
name: baseline
description: Baseline 관리 커맨드. create / update / show 서브커맨드로 .quality-baseline.json 을 생성·갱신·조회. 레거시 프로젝트 점진적 도입 시 사용.
---

# /baseline

## 용도

`.quality-baseline.json` 을 생성·관리하는 커맨드입니다. **레거시 프로젝트**에 Plugin 을 처음 도입하거나 해결된 위반을 정리할 때 사용합니다.

```
/baseline <create|update|show>
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
