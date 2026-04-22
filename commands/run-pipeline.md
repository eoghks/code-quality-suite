---
description: Refactor → Security → Quality 3-stage 파이프라인 수동 실행. --strict 로 Baseline 무시 전수 검사 가능.
argument-hint: "[파일경로 | 브랜치명 | --strict]"
---

# /run-pipeline

**역할:** `code-refactoring-agent` → `security-audit-agent` → `code-quality-agent` 를 순차 실행합니다. `git commit` Hook 이 발화하지 않는 상황(중간 점검, 탐색적 리팩토링)이나 커밋 전 전수 검증 시 사용합니다.

---

## 인자 규칙

| 인자 | 동작 |
|---|---|
| 없음 | 현재 브랜치 미커밋 변경 + 최근 커밋, Baseline 적용 |
| `src/main/java/Foo.java` | 지정 파일만 대상 |
| `feature/user-login` | 해당 브랜치의 main 대비 diff |
| `--strict` | Baseline 무시 + 전체 `src/main/**` 전수 검사 |

---

## 실행 절차 (3-stage)

### 1. 대상 범위 확정 + Baseline 로드

```
$ARGUMENTS
```

인자 해석:
- 없음 → `git status` + `git log main..HEAD --name-only` 로 변경 파일 수집
- 파일/디렉터리 → 해당 경로 + 연관 테스트
- 브랜치명 → `git diff main...<branch> --name-only`
- `--strict` → Baseline 로드 건너뜀, 대상 = 전체 소스

**Baseline 로드:** `--strict` 아닌 경우 `.quality-baseline.json` 존재 시 fingerprint Set 로드.

### 2. Refactor Agent 호출

```
/agent code-refactoring-agent <대상 범위>
```

완료 후 변경 파일 목록을 컨텍스트에 보존.

### 3. Security Agent 호출

```
/agent security-audit-agent <대상 범위> [--strict]
```

결과를 `.security-report.md` 로 저장. 마지막 줄에 `[BLOCK: SECURITY STOP]` 또는 `[PASS: SECURITY OK]` 포함.

### 4. Quality Agent 호출

```
/agent code-quality-agent <대상 범위> [--strict]
```

결과를 `.quality-report.md` 로 저장. 마지막 줄에 `[BLOCK: COMMIT STOP]` 또는 `[PASS: COMMIT READY]` 포함.

### 5. 결과 요약

```
## Pipeline Complete — <대상> [STRICT 모드]

### Stage 1: Refactor
- 변경 파일: N개 · 커밋: M건
- 테스트: 통과 X건 / 실패 0건

### Stage 2: Security
- Critical: A건 · High: B건 · Medium: C건 · Low: D건
- Baseline 제외: E건
- 상태: [PASS: SECURITY OK] | [BLOCK: SECURITY STOP]

### Stage 3: Quality
- Critical: A건 · High: B건 · Medium: C건 · Low: D건
- Baseline 제외: F건
- SpotBugs: G건 · JaCoCo: H% (변경 메서드 평균)
- 상태: [PASS: COMMIT READY] | [BLOCK: COMMIT STOP]

### 최종 판정
- ✅ PASS → `git commit` 진행 가능
- ❌ BLOCK → Critical 해결 후 `/run-pipeline` 재실행
```

---

## 주의

- 최종 커밋은 개발자가 결정. Refactor Agent 가 생성한 리팩토링 단위 커밋과 별개.
- Hook 은 **힌트만** 주입. 실제 Agent 호출은 이 커맨드가 담당.
- Security 또는 Quality 에서 BLOCK 발생 시 해당 단계 이후 진행 중단. 보고서 참조 후 재실행.
- `--strict` 사용 시 레거시 전체 스캔 → 다수 위반 예상. 처음엔 `/baseline create` 선행 권장.
