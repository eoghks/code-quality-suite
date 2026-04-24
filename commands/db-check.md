---
name: db-check
description: Flyway/Liquibase 마이그레이션 스크립트 정적 분석. db-migration-agent 를 호출해 DROP·NOT NULL·버전 연속성을 검증한다.
---

# /db-check

## 사용법

```
/db-check [대상] [옵션]
```

## 인자

| 인자 | 설명 | 예시 |
|---|---|---|
| (없음) | `git diff HEAD~1..HEAD` 기준 변경 마이그레이션 파일 | `/db-check` |
| `<파일경로>` | 특정 SQL/XML/YAML 파일 | `/db-check db/migration/V5__add_payment.sql` |
| `<디렉터리>` | 하위 전체 스캔 | `/db-check db/migration/` |
| `--all` | `db/migration/` 및 `resources/db/migration/` 전체 | `/db-check --all` |

## 옵션

| 옵션 | 설명 |
|---|---|
| `--strict` | `@suppress` 주석 무시, 모든 위반 보고 |
| `--version-only` | MIG-VERSION 버전 연속성 검사만 실행 |

## 동작

1. **`db-migration-agent`** 호출
2. 대상 마이그레이션 파일 정적 분석:
   - `DROP TABLE` / `TRUNCATE` → **Critical**
   - `NOT NULL ADD COLUMN` without `DEFAULT` → **High**
   - `ADD COLUMN` without `DEFAULT` → **High**
   - `ADD INDEX` (대용량 Lock 경고) → **Medium**
   - Undo 스크립트 부재 → **Low**
   - 버전 번호 불연속 → **High (BLOCK)**
   - 인코딩 선언 누락 → **Low**
3. `.migration-report.md` 생성
4. BLOCK 조건(Critical 또는 MIG-VERSION) 시 커밋 차단 안내

## BLOCK 조건

| 조건 | 결과 |
|---|---|
| Critical 1건 이상 | `[BLOCK: MIGRATION STOP]` |
| MIG-VERSION 감지 | `[BLOCK: MIGRATION STOP]` |
| High 만 존재 | `[BLOCK: MIGRATION STOP]` |
| Medium/Low 만 존재 | `[PASS: MIGRATION OK]` + 경고 |
| 위반 없음 | `[PASS: MIGRATION OK]` |

## 출력 예시

```
## DB Migration Report — V5__add_payment.sql

### Critical
- [MIG-DROP] V5__add_payment.sql:12 — DROP TABLE old_payment (데이터 손실 위험)

### High
- [MIG-ALTER-NULL] V5__add_payment.sql:18 — ADD COLUMN amount DECIMAL NOT NULL (DEFAULT 없음)

### 요약
- Critical: 1건 · High: 1건 · Medium: 0건 · Low: 0건

[BLOCK: MIGRATION STOP]
```

## 참고

- 분석 규칙 상세: `rules/migration-rules.md`
- 억제 주석: `rules/suppress-policy.md`
- Hook 통합: `hooks/pre-commit-pipeline.sh` 에서 `.migration-report.md` BLOCK 마커 자동 감지
