---
description: Refactor → Security → Quality 3-stage 파이프라인 수동 실행. --full 로 Architecture 검증 추가, --strict 로 Baseline 무시 전수 검사 가능.
argument-hint: "[파일경로 | 브랜치명 | --full] [--strict]"
---

# /run-pipeline

**역할:** `code-refactoring-agent` → `security-audit-agent` → `code-quality-agent` 를 순차 실행합니다.
`--full` 플래그 추가 시 `architecture-review-agent` 가 2번째 Stage 로 삽입됩니다.

---

## 인자 규칙

| 인자 | 동작 |
|---|---|
| 없음 | 현재 브랜치 미커밋 변경 + 최근 커밋, Baseline 적용 |
| `src/main/java/Foo.java` | 지정 파일만 대상 |
| `feature/user-login` | 해당 브랜치의 main 대비 diff |
| `--full` | Architecture Agent 삽입 (4-stage). PR 전·대규모 리팩토링 후 권장 |
| `--strict` | Baseline 무시 + 전체 `src/main/**` 전수 검사 |

---

## 실행 절차 — 기본 (3-stage)

### 1. 대상 범위 확정 + Baseline 로드

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

### 3. Security Agent 호출

```
/agent security-audit-agent <대상 범위> [--strict]
```

결과 → `.security-report.md`. 마지막 줄: `[BLOCK: SECURITY STOP]` 또는 `[PASS: SECURITY OK]`.

### 4. Quality Agent 호출

```
/agent code-quality-agent <대상 범위> [--strict]
```

결과 → `.quality-report.md`. 마지막 줄: `[BLOCK: COMMIT STOP]` 또는 `[PASS: COMMIT READY]`.

### 5. 결과 요약

```
## Pipeline Complete — <대상>

### Stage 1: Refactor
- 변경 파일: N개 · 커밋: M건 · 테스트: 통과 X건 / 실패 0건

### Stage 2: Security
- Critical: A건 · High: B건 · Medium: C건 · Low: D건
- 상태: [PASS: SECURITY OK] | [BLOCK: SECURITY STOP]

### Stage 3: Quality
- Critical: A건 · High: B건 · SpotBugs: G건 · JaCoCo: H%
- 상태: [PASS: COMMIT READY] | [BLOCK: COMMIT STOP]

### 최종 판정
- ✅ PASS → git commit 진행 가능
- ❌ BLOCK → Critical 해결 후 /run-pipeline 재실행
```

---

## 실행 절차 — `--full` 옵션 (4-stage)

`--full` 인자 감지 시 Refactor 이후 Architecture Agent 를 삽입한다.

```
Stage 1: code-refactoring-agent    (리팩토링)
Stage 2: architecture-review-agent (아키텍처 검증) ← --full 전용
Stage 3: security-audit-agent      (보안 검증)
Stage 4: code-quality-agent        (품질 검증)
```

### Architecture Agent 호출

```
/agent architecture-review-agent <대상 범위> [--strict]
```

결과 → `.architecture-report.md`. 마지막 줄: `[BLOCK: ARCH STOP]` 또는 `[PASS: ARCH OK]`.

**ARCH STOP 발생 시 Security·Quality Stage 진행 중단.** 레이어 위반 해결 후 재실행.

### `--full` 결과 요약

```
## Pipeline Complete — <대상> [FULL MODE]

### Stage 1: Refactor
- 변경 파일: N개 · 커밋: M건 · 테스트: 통과 X건 / 실패 0건

### Stage 2: Architecture
- High: A건 · Medium: B건 · Low: C건
- 상태: [PASS: ARCH OK] | [BLOCK: ARCH STOP]

### Stage 3: Security
- Critical: A건 · High: B건 · Medium: C건 · Low: D건
- 상태: [PASS: SECURITY OK] | [BLOCK: SECURITY STOP]

### Stage 4: Quality
- Critical: A건 · High: B건 · SpotBugs: G건 · JaCoCo: H%
- 상태: [PASS: COMMIT READY] | [BLOCK: COMMIT STOP]

### 최종 판정
- ✅ PASS → git commit 진행 가능
- ❌ BLOCK → 해당 보고서 확인 후 재실행
```

---

## 주의

- 기본 `/run-pipeline` 은 3-stage (Architecture 미포함). PR 전·대규모 변경 후엔 `--full` 권장.
- Hook 은 **힌트만** 주입. 실제 Agent 호출은 이 커맨드가 담당.
- BLOCK 발생 시 해당 단계 이후 중단. 보고서 확인 후 재실행.
- `--strict` 사용 시 레거시 전체 스캔 → 다수 위반 예상. 처음엔 `/baseline create` 선행 권장.
