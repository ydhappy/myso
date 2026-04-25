-- ============================================================
-- 에고무기 미사용 DB 정리 SQL
-- 목적: 무기변형 제거 후 남은 미사용 컬럼/인덱스/테이블 삭제
-- 대상: 기존에 ego_install_euckr.sql 구버전을 이미 적용한 서버
-- 주의: 실행 전 DB 백업 필수
-- 파일 인코딩: UTF-8
-- ============================================================

SET NAMES utf8;

-- ------------------------------------------------------------
-- 1. 미사용 테이블 삭제
-- ------------------------------------------------------------
DROP TABLE IF EXISTS ego_talk;
DROP TABLE IF EXISTS ego_type;

-- ------------------------------------------------------------
-- 2. ego 테이블의 무기변형 잔여 인덱스/컬럼 삭제
-- MySQL 버전별 IF EXISTS 지원 차이가 있으므로 실패하면 해당 줄은 이미 없는 것으로 보면 됩니다.
-- ------------------------------------------------------------

-- ego.form 인덱스 제거
ALTER TABLE ego DROP INDEX ego_form_idx;

-- 무기변형 잔여 컬럼 제거
ALTER TABLE ego DROP COLUMN form;
ALTER TABLE ego DROP COLUMN prev_shield;

-- ------------------------------------------------------------
-- 3. 레벨별 해금 능력 기본값 추가/보정
-- ------------------------------------------------------------
INSERT INTO ego_skill_base
(skill, label, memo, base_rate, lv_rate, max_rate, min_lv, cool_ms, effect, use_yn)
VALUES
('EGO_COUNTER', '반격', 'Lv.10 해금 피격 반격', 3, 1, 20, 10, 3000, 10710, 1),
('EGO_REVENGE', '복수', 'Lv.20 해금 저체력 피격 특수 반격', 2, 1, 15, 20, 8000, 6321, 1)
ON DUPLICATE KEY UPDATE
    label = VALUES(label),
    memo = VALUES(memo),
    base_rate = VALUES(base_rate),
    lv_rate = VALUES(lv_rate),
    max_rate = VALUES(max_rate),
    min_lv = VALUES(min_lv),
    cool_ms = VALUES(cool_ms),
    effect = VALUES(effect),
    use_yn = VALUES(use_yn);

-- ------------------------------------------------------------
-- 4. 확인
-- ------------------------------------------------------------
DESC ego;
SHOW TABLES LIKE 'ego_type';
SHOW TABLES LIKE 'ego_talk';

SELECT *
FROM ego_skill_base
WHERE skill IN ('EGO_COUNTER', 'EGO_REVENGE');

SELECT 'EGO_CLEANUP_UNUSED_OK' AS result;
