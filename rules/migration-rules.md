# DB 마이그레이션 규칙 (migration-rules)

> `db-migration-agent` 가 참조하는 **Flyway / Liquibase 스크립트 정적 분석 기준**.
> 읽기 전용. 차단 판정 시 `.migration-report.md` 에 `[BLOCK: MIGRATION STOP]` 마커 출력.

---

## 0. 심각도 체계

| 태그 | 의미 | BLOCK |
|---|---|---|
| **Critical** | 데이터 손실 직결 (`DROP`, `TRUNCATE`) | ✅ |
| **High** | 기존 데이터 NULL·오류 위험 (NOT NULL without DEFAULT) | ✅ |
| **Medium** | 운영 중단 위험 (장시간 Lock) | ❌ |
| **Low** | 운영 편의·안전망 부재 | ❌ |

**BLOCK 조건:** Critical 1건 이상 또는 MIG-VERSION(버전 불연속).

---

## 1. MIG-DROP — 테이블 삭제·초기화 [Critical]

### 감지 패턴

```sql
DROP TABLE <table_name>;          -- ❌ Critical
DROP TABLE IF EXISTS <table_name>; -- ❌ Critical
TRUNCATE TABLE <table_name>;       -- ❌ Critical
TRUNCATE <table_name>;             -- ❌ Critical
```

### 허용 패턴 (주석 명시 시)

```sql
-- @suppress MIG-DROP — 개발 환경 전용, 프로덕션 적용 불가
DROP TABLE temp_migration_log;
```

### 대응 방법

- 컬럼 삭제는 `ALTER TABLE DROP COLUMN` (전체 테이블 DROP 대신)
- 데이터 초기화는 `DELETE FROM` + 조건부 (트랜잭션 내)
- 테이블 제거가 진짜 필요하면 `@suppress` + 사유 + DBA 승인 이력 명시

---

## 2. MIG-ALTER-NULL — NOT NULL 컬럼 추가 시 DEFAULT 누락 [High]

### 감지 패턴

```sql
-- ❌ High — 기존 행에 NULL 삽입 시도 → DB 오류
ALTER TABLE users ADD COLUMN age INT NOT NULL;

-- ✅ DEFAULT 제공
ALTER TABLE users ADD COLUMN age INT NOT NULL DEFAULT 0;

-- ✅ 또는 2단계: nullable 추가 → 백필 → NOT NULL 변경
ALTER TABLE users ADD COLUMN age INT;
UPDATE users SET age = 0 WHERE age IS NULL;
ALTER TABLE users MODIFY COLUMN age INT NOT NULL;
```

**감지 기준:** `ALTER TABLE ... ADD COLUMN ... NOT NULL` 구문에서 `DEFAULT` 키워드 부재.

---

## 3. MIG-ALTER-DEF — DEFAULT 없는 컬럼 추가 (기존 데이터 NULL) [High]

### 감지 패턴

```sql
-- ❌ High — NULL 허용 컬럼이지만 기존 데이터가 예기치 않게 NULL
ALTER TABLE orders ADD COLUMN memo VARCHAR(500);

-- ✅ DEFAULT 명시
ALTER TABLE orders ADD COLUMN memo VARCHAR(500) DEFAULT '';
```

**적용 범위:** `VARCHAR`, `TEXT`, `INT`, `BIGINT`, `BOOLEAN` 등 모든 타입.
**예외:** `NULLABLE` 임을 의도적으로 명시한 주석 존재 시 Low 로 완화.

---

## 4. MIG-INDEX — 대용량 테이블 인덱스 추가 (Lock 경고) [Medium]

### 감지 패턴

```sql
-- ⚠️ Medium — 운영 중 Lock 발생 가능
ALTER TABLE orders ADD INDEX idx_user_id (user_id);
CREATE INDEX idx_created_at ON orders (created_at);
```

### 대응 방법

- **MySQL 5.6+**: `ALTER TABLE ... ADD INDEX` 는 Online DDL 지원 (pt-online-schema-change 불필요)
- **PostgreSQL**: `CREATE INDEX CONCURRENTLY` 사용 → Lock 없음
- 대용량 테이블 기준은 컨텍스트 의존 → Medium 경고 + 배포 시간 검토 권고

---

## 5. MIG-NO-UNDO — 롤백 스크립트 부재 [Low]

### 감지 패턴

Flyway 사용 시 `V{version}__*.sql` 파일에 대응하는 `U{version}__*.sql` (undo) 파일 부재.

```
db/migration/V3__add_user_age.sql    ← 존재
db/migration/U3__add_user_age.sql    ← ❌ 없음 → Low 경고
```

**예외:** Flyway Community Edition 은 undo 미지원 → Low 대신 정보성 메시지.

---

## 6. MIG-VERSION — 버전 번호 불연속 [High]

### 감지 패턴

```
V1__init.sql
V2__add_users.sql
V4__add_orders.sql   ← ❌ V3 누락 → High
```

### 감지 방법

1. `db/migration/` (또는 `resources/db/migration/`) 하위 `V*.sql` 파일 목록 수집
2. 버전 번호 추출 → 정렬 → 연속성 확인
3. 누락 버전 목록 보고

**예외:** 의도적 버전 건너뜀은 `-- @suppress MIG-VERSION — V3 삭제됨, V4 부터 재시작` 주석 명시.

---

## 7. MIG-ENCODING — 인코딩 선언 누락 [Low]

```sql
-- ✅ 파일 상단 인코딩 주석 권장 (MySQL)
-- charset: utf8mb4
ALTER TABLE users MODIFY COLUMN name VARCHAR(100) CHARACTER SET utf8mb4;
```

한글 데이터를 포함하는 컬럼 변경 시 `CHARACTER SET` / `COLLATE` 명시 권고.

---

## 8. 보고서 형식

```
## DB Migration Report — V5__add_payment.sql

### Critical
- [MIG-DROP] V5__add_payment.sql:12 — DROP TABLE old_payment (데이터 손실 위험)

### High
- [MIG-ALTER-NULL] V5__add_payment.sql:18 — ADD COLUMN amount DECIMAL NOT NULL (DEFAULT 없음)
- [MIG-VERSION] db/migration/ — V3 누락 (V2 → V4 불연속)

### Medium
- [MIG-INDEX] V5__add_payment.sql:25 — CREATE INDEX idx_payment_user ON payment (user_id)

### Low
- [MIG-NO-UNDO] U5__add_payment.sql 미존재

### 요약
- Critical: 1건 · High: 2건 · Medium: 1건 · Low: 1건

[BLOCK: MIGRATION STOP]
```
