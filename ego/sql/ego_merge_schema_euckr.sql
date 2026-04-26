-- ============================================================
-- 에고 테이블/컬럼 병합 마이그레이션 SQL
-- 파일 인코딩: UTF-8
-- DB 문자셋: euckr
--
-- 병합 내용:
--   1) ego_level_exp + ego_level_bonus -> ego_level
--   2) ego_bond -> ego.bond / ego.bond_reason
--
-- 안전 정책:
--   기존 테이블은 즉시 DROP하지 않습니다.
--   Java 코드는 통합 테이블 우선, 구버전 테이블 fallback 구조입니다.
-- ============================================================

SET NAMES utf8;

-- ------------------------------------------------------------
-- 1. ego 테이블에 유대감 컬럼 병합
-- ------------------------------------------------------------
ALTER TABLE ego ADD COLUMN bond INT NOT NULL DEFAULT 0 COMMENT '에고 유대감 0~1000';
ALTER TABLE ego ADD COLUMN bond_reason VARCHAR(40) NOT NULL DEFAULT '' COMMENT '마지막 유대감 증가 사유';

-- 이미 컬럼이 있는 서버에서는 위 ALTER가 실패할 수 있습니다.
-- 실패 시 아래 UPDATE부터 계속 실행하세요.

UPDATE ego e
INNER JOIN ego_bond b ON e.item_id = b.item_id
SET e.bond = b.bond,
    e.bond_reason = b.last_reason,
    e.mod_date = NOW();

-- ------------------------------------------------------------
-- 2. 레벨 경험치 + 전투 보너스 통합 테이블
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ego_level (
    ego_lv INT NOT NULL COMMENT '에고 레벨 0~10',
    need_exp BIGINT NOT NULL DEFAULT 0 COMMENT '다음 레벨 필요 경험치',
    proc_bonus INT NOT NULL DEFAULT 0 COMMENT '스킬 발동률 추가',
    critical_chance INT NOT NULL DEFAULT 0 COMMENT '치명 발동률 추가',
    critical_damage INT NOT NULL DEFAULT 0 COMMENT '치명 추가 피해',
    counter_chance INT NOT NULL DEFAULT 0 COMMENT '피격 반격 확률',
    counter_power INT NOT NULL DEFAULT 0 COMMENT '반격 피해 비율',
    counter_critical INT NOT NULL DEFAULT 0 COMMENT '반격 치명 확률',
    memo VARCHAR(255) NOT NULL DEFAULT '',
    use_yn TINYINT(1) NOT NULL DEFAULT 1,
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (ego_lv)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 레벨 통합 설정';

-- 구버전 테이블이 있는 경우 데이터 이관
INSERT INTO ego_level
(ego_lv, need_exp, proc_bonus, critical_chance, critical_damage, counter_chance, counter_power, counter_critical, memo, use_yn)
SELECT
    e.ego_lv,
    e.need_exp,
    IFNULL(b.proc_bonus, 0),
    IFNULL(b.critical_chance, 0),
    IFNULL(b.critical_damage, 0),
    IFNULL(b.counter_chance, 0),
    IFNULL(b.counter_power, 0),
    IFNULL(b.counter_critical, 0),
    e.memo,
    e.use_yn
FROM ego_level_exp e
LEFT JOIN ego_level_bonus b ON e.ego_lv = b.ego_lv
ON DUPLICATE KEY UPDATE
    need_exp = VALUES(need_exp),
    proc_bonus = VALUES(proc_bonus),
    critical_chance = VALUES(critical_chance),
    critical_damage = VALUES(critical_damage),
    counter_chance = VALUES(counter_chance),
    counter_power = VALUES(counter_power),
    counter_critical = VALUES(counter_critical),
    memo = VALUES(memo),
    use_yn = VALUES(use_yn),
    mod_date = NOW();

-- 구버전 테이블이 없거나 비어 있는 경우 기본값 보정
INSERT INTO ego_level
(ego_lv, need_exp, proc_bonus, critical_chance, critical_damage, counter_chance, counter_power, counter_critical, memo, use_yn)
VALUES
(0, 100, 0, 0, 0, 0, 0, 0, 'Lv.0 전투능력 없음', 1),
(1, 250, 0, 1, 1, 0, 0, 0, 'Lv.1 기본 치명 시작', 1),
(2, 500, 1, 2, 2, 0, 0, 0, 'Lv.2', 1),
(3, 900, 2, 3, 3, 0, 0, 0, 'Lv.3', 1),
(4, 1500, 3, 4, 4, 0, 0, 0, 'Lv.4', 1),
(5, 2400, 4, 6, 6, 35, 18, 8, 'Lv.5 피격 반격 시작', 1),
(6, 3600, 6, 9, 8, 100, 24, 12, 'Lv.6 자동반격 시작', 1),
(7, 5200, 8, 12, 10, 100, 30, 16, 'Lv.7', 1),
(8, 7500, 10, 15, 12, 100, 38, 20, 'Lv.8', 1),
(9, 10000, 12, 18, 15, 100, 46, 25, 'Lv.9', 1),
(10, 0, 15, 25, 20, 100, 60, 35, 'Lv.10 스턴 연동', 1)
ON DUPLICATE KEY UPDATE
    memo = VALUES(memo),
    use_yn = VALUES(use_yn);

-- ego.need_exp 보정
UPDATE ego e
INNER JOIN ego_level l ON e.ego_lv = l.ego_lv
SET e.need_exp = l.need_exp
WHERE l.use_yn = 1;

UPDATE ego SET need_exp = 0, ego_exp = 0 WHERE ego_lv >= 10;

-- ------------------------------------------------------------
-- 3. 확인
-- ------------------------------------------------------------
SELECT 'EGO_MERGE_SCHEMA_OK' AS result;
DESC ego;
SELECT * FROM ego_level ORDER BY ego_lv;
SELECT item_id, ego_name, ego_lv, ego_exp, need_exp, bond, bond_reason FROM ego ORDER BY item_id DESC LIMIT 20;
