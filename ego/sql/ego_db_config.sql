-- ============================================================
-- 에고 기능 DB화 보강 SQL
-- 대상: 신규/기존 서버 공통
-- 목적:
--   1) Java 상수성 설정값을 ego_config로 이동
--   2) 레벨별 필요 경험치를 ego_level_exp로 이동
--   3) .에고리로드 만으로 설정/경험치표 즉시 반영
-- 파일 인코딩: UTF-8
-- DB 문자셋: euckr
-- ============================================================

SET NAMES utf8;

-- ------------------------------------------------------------
-- 1. 에고 공통 설정값
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ego_config (
    config_key VARCHAR(80) NOT NULL COMMENT '설정 키',
    config_value VARCHAR(255) NOT NULL DEFAULT '' COMMENT '설정 값',
    memo VARCHAR(255) NOT NULL DEFAULT '' COMMENT '설명',
    use_yn TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    mod_date DATETIME NULL DEFAULT NULL COMMENT '수정일',
    PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 공통 설정';

INSERT INTO ego_config (config_key, config_value, memo, use_yn) VALUES
('genre_talk_delay_ms', '1200', '장르대화 연속 입력 방지 딜레이 ms', 1),
('auto_talk_hp_warn_rate', '25', 'HP 자동 위험 대사 발동 기준 퍼센트 이하', 1),
('auto_talk_mp_warn_rate', '15', 'MP 자동 안내 대사 발동 기준 퍼센트 이하', 1),
('auto_talk_idle_hp_rate', '80', '안정 상태 자동 대사 HP 기준 퍼센트 이상', 1),
('auto_talk_idle_mp_rate', '50', '안정 상태 자동 대사 MP 기준 퍼센트 이상', 1),
('auto_talk_hp_warn_delay_ms', '15000', 'HP 자동 위험 대사 재출력 딜레이 ms', 1),
('auto_talk_mp_warn_delay_ms', '20000', 'MP 자동 안내 대사 재출력 딜레이 ms', 1),
('auto_talk_boss_warn_delay_ms', '30000', '보스 감지 자동 대사 재출력 딜레이 ms', 1),
('auto_talk_idle_delay_ms', '180000', '안정 상태 자동 대사 재출력 딜레이 ms', 1)
ON DUPLICATE KEY UPDATE
    memo = VALUES(memo),
    use_yn = VALUES(use_yn);

-- ------------------------------------------------------------
-- 2. 에고 레벨별 필요 경험치
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ego_level_exp (
    ego_lv INT NOT NULL COMMENT '현재 에고 레벨 0~10',
    need_exp BIGINT NOT NULL DEFAULT 0 COMMENT '다음 레벨 필요 경험치, Lv.10은 0',
    memo VARCHAR(255) NOT NULL DEFAULT '' COMMENT '설명',
    use_yn TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    mod_date DATETIME NULL DEFAULT NULL COMMENT '수정일',
    PRIMARY KEY (ego_lv)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 레벨별 필요 경험치';

INSERT INTO ego_level_exp (ego_lv, need_exp, memo, use_yn) VALUES
(0, 100, 'Lv.0 -> Lv.1 기본 스킬/치명 개방', 1),
(1, 250, 'Lv.1 -> Lv.2', 1),
(2, 500, 'Lv.2 -> Lv.3', 1),
(3, 900, 'Lv.3 -> Lv.4', 1),
(4, 1500, 'Lv.4 -> Lv.5 피격 반격 개방', 1),
(5, 2400, 'Lv.5 -> Lv.6 자동반격 개방', 1),
(6, 3600, 'Lv.6 -> Lv.7', 1),
(7, 5200, 'Lv.7 -> Lv.8', 1),
(8, 7500, 'Lv.8 -> Lv.9', 1),
(9, 10000, 'Lv.9 -> Lv.10 스턴 50% 개방', 1),
(10, 0, 'Lv.10 만렙', 1)
ON DUPLICATE KEY UPDATE
    memo = VALUES(memo),
    use_yn = VALUES(use_yn);

-- ------------------------------------------------------------
-- 3. 기존 ego.need_exp를 DB 경험치표 기준으로 보정
-- ------------------------------------------------------------
UPDATE ego e
INNER JOIN ego_level_exp x ON e.ego_lv = x.ego_lv
SET e.need_exp = x.need_exp
WHERE x.use_yn = 1;

UPDATE ego SET need_exp = 0, ego_exp = 0 WHERE ego_lv >= 10;

-- ------------------------------------------------------------
-- 4. 확인
-- ------------------------------------------------------------
SELECT 'EGO_DB_CONFIG_OK' AS result;
SELECT * FROM ego_config ORDER BY config_key;
SELECT * FROM ego_level_exp ORDER BY ego_lv;
SELECT ego_lv, need_exp, COUNT(*) AS count
FROM ego
GROUP BY ego_lv, need_exp
ORDER BY ego_lv;
