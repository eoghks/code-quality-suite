---
description: Refactor → Quality 파이프라인 수동 실행 (커밋 직전이 아니어도 중간 점검용)
argument-hint: "[파일경로 | 브랜치명]"
---

# /run-pipeline

**역할:** Refactor Agent → Quality Agent 를 순차 실행합니다. `git commit` Hook 이 발화하지 않는 상황(중간 점검, 탐색적 리팩토링)에서 수동으로 호출합니다.

---

## 인자 규칙

- `/run-pipeline` — **인자 없음** → 현재 브랜치의 모든 미커밋 변경 + 최근 커밋 대상
- `/run-pipeline src/main/java/Foo.java` — 지정 파일만 대상
- `/run-pipeline feature/user-login` — 지정 브랜치의 main 대비 diff 대상

---

## 실행 절차

아래 순서대로 수행하세요:

### 1. 대상 범위 확정

$ARGUMENTS

위 인자를 해석해 대상 범위를 결정:
- 인자 없음 → `git status` + `git log main..HEAD --name-only` 로 후보 수집
- 파일 경로 → 해당 파일 + 연관 테스트 파일
- 브랜치명 → `git diff main...<branch> --name-only`

### 2. Refactor Agent 호출

```
/agent code-refactoring-agent <대상 범위>
```

Refactor 완료 후 보고서를 컨텍스트에 유지한 채 다음 단계로 이동.

### 3. Quality Agent 호출

```
/agent code-quality-agent <대상 범위>
```

Quality 완료 시 **결과를 `.quality-report.md` 파일로 저장**하도록 메시지 말미에 명시:

> "보고서 전체를 `.quality-report.md` 파일로 저장해 주세요. 마지막 줄에 `[BLOCK: COMMIT STOP]` 또는 `[PASS: COMMIT READY]` 마커를 정확히 포함해야 합니다."

### 4. 결과 요약

파이프라인 종료 후 아래 형식으로 사용자에게 요약 출력:

```
## Pipeline Complete

### Refactor
- 변경 파일: N개
- 커밋: M건
- 테스트: 통과 X건 / 실패 0건

### Quality
- Critical: A건 · High: B건 · Medium: C건 · Low: D건
- 커밋 상태: [PASS: COMMIT READY] | [BLOCK: COMMIT STOP]

### 다음 액션
- PASS → 개발자가 `git commit` 진행
- BLOCK → Critical 해결 후 재실행
```

---

## 주의

- 이 커맨드는 **자동 커밋을 수행하지 않음** — Refactor Agent 가 내부적으로 리팩토링 단위 아토믹 커밋을 생성할 수 있으나, 최종 "기능 완료 커밋" 은 개발자가 결정
- Hook 과 달리 이 커맨드는 **Agent 를 실제 호출** 함 (Hook 은 힌트만)
- 실패 시 (Agent 오류, 테스트 실행 불가 등) 어느 단계에서 멈췄는지 명시 후 종료
