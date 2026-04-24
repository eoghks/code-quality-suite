# /architecture-review

> 자바 프로젝트 아키텍처를 정적 분석으로 검증한다.
> 레이어 역방향 의존·순환 의존·DDD 경계 위반을 감지하고 `.architecture-report.md` 를 생성한다.

---

## 사용법

```
/architecture-review [target] [--full] [--strict]
```

| 인자 | 동작 |
|---|---|
| 없음 | 현재 브랜치 변경 파일 기준 아키텍처 검증 (git diff) |
| `<파일경로>` | 지정 파일만 스캔 |
| `<디렉터리>` | 지정 디렉터리 하위 전수 스캔 |
| `<브랜치명>` | 해당 브랜치의 main 대비 diff 스캔 |
| `--full` | 전체 `src/main/java/` 전수 스캔 |
| `--strict` | Baseline 무시 + 전수 검사 |

---

## 실행 절차

```
1. 인자 파싱
   → target / --full / --strict 플래그 분리

2. architecture-review-agent 호출
   → 스캔 범위 전달 (변경 파일 목록 또는 전체 경로)
   → --strict 플래그 전달

3. .architecture-report.md 생성 확인
   → 마지막 줄 BLOCK 마커 확인
   → [BLOCK: ARCH STOP] → 사용자에게 High 위반 목록 요약 출력

4. 결과 요약 출력
```

---

## 출력 예시

### PASS

```
✅ Architecture Review — PASS

스캔 파일: 5개
High 위반: 0건
Medium: 1건 (UserController.java — 메서드 58줄)
Low: 0건

[PASS: ARCH OK] — 커밋 진행 가능
보고서: .architecture-report.md
```

### BLOCK

```
⛔ Architecture Review — BLOCK

[ARCH-LAYER-01] OrderController.java:12
  → import com.example.repository.OrderRepository
  → Controller 에서 Repository 직접 접근 금지. OrderService 를 통해 위임할 것.

신규 High: 1건 (차단)

보고서: .architecture-report.md
/architecture-review --strict 로 Baseline 무시 전수 스캔 가능
```

---

## /run-pipeline 연동

`/run-pipeline --full` 실행 시 아키텍처 검증이 2번째 Stage 로 자동 삽입된다:

```
Stage 1: code-refactoring-agent   (리팩토링)
Stage 2: architecture-review-agent (아키텍처 검증) ← --full 전용
Stage 3: security-audit-agent      (보안 검증)
Stage 4: code-quality-agent        (품질 검증)
```

기본 `/run-pipeline` (--full 없음) 에서는 아키텍처 검증이 포함되지 않는다.
PR 머지 전·대규모 리팩토링 후에 `/run-pipeline --full` 또는 `/architecture-review --full` 단독 실행 권장.
