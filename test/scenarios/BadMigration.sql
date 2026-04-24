-- [시나리오 테스트] DB Migration Agent 검증 대상
--
-- 포함된 의도적 위반:
--  - DROP TABLE (Critical)                           ← MIG-DROP
--  - NOT NULL ADD COLUMN without DEFAULT (High)      ← MIG-ALTER-NULL
--  - ADD COLUMN without DEFAULT (High)               ← MIG-ALTER-DEF
--  - CREATE INDEX without CONCURRENTLY (Medium)      ← MIG-INDEX
--  - 버전 번호 불연속: V4 → V6 (V5 누락) (High)     ← MIG-VERSION
--
-- 이 파일은 V6__bad_migration.sql 로 가정 (V5 가 없는 상태)
-- charset: utf8mb4

-- ============================================================
-- ❌ Critical — MIG-DROP: 전체 테이블 삭제
-- ============================================================
DROP TABLE old_payment_log;

-- ============================================================
-- ❌ Critical — MIG-DROP: TRUNCATE (데이터 초기화)
-- ============================================================
TRUNCATE TABLE temp_user_import;

-- ============================================================
-- ❌ High — MIG-ALTER-NULL: NOT NULL 컬럼 추가 + DEFAULT 없음
--   기존 행에 NULL 삽입 시도 → DB 오류 발생
-- ============================================================
ALTER TABLE users ADD COLUMN age INT NOT NULL;

-- ============================================================
-- ❌ High — MIG-ALTER-NULL: NOT NULL + 다른 타입도 동일
-- ============================================================
ALTER TABLE orders ADD COLUMN status VARCHAR(20) NOT NULL;

-- ============================================================
-- ❌ High — MIG-ALTER-DEF: ADD COLUMN DEFAULT 없음
--   기존 데이터가 예기치 않게 NULL 이 됨
-- ============================================================
ALTER TABLE orders ADD COLUMN memo VARCHAR(500);

-- ============================================================
-- ❌ Medium — MIG-INDEX: 대용량 테이블 인덱스 추가 (Lock 경고)
--   PostgreSQL 에서는 CONCURRENTLY 사용 권장
-- ============================================================
CREATE INDEX idx_orders_user_id ON orders (user_id);
ALTER TABLE payments ADD INDEX idx_payment_created (created_at);

-- ============================================================
-- ✅ 올바른 패턴 — NOT NULL + DEFAULT 제공
-- ============================================================
ALTER TABLE products ADD COLUMN stock INT NOT NULL DEFAULT 0;

-- ============================================================
-- ✅ 올바른 패턴 — DEFAULT 명시
-- ============================================================
ALTER TABLE users ADD COLUMN nickname VARCHAR(50) DEFAULT '';

-- ============================================================
-- ✅ 올바른 패턴 — @suppress MIG-DROP + 사유 명시
-- ============================================================
-- @suppress MIG-DROP — 개발 환경 전용 임시 테이블, 프로덕션 적용 불가. DBA 승인: 2026-04-10
DROP TABLE dev_temp_log;

-- ============================================================
-- ❌ @suppress 사유 없음 → [SUPPRESS-NO-REASON] Medium 경고
-- ============================================================
-- @suppress MIG-DROP
DROP TABLE another_temp;
