-- ============================================================
-- 에고무기 미사용 DB 정리 SQL - 반복 실행 안전 버전
-- 목적:
--   1) 구버전 type2 변형/이미지 커스터마이징 잔여 테이블 제거
--   2) 구버전 ego 컬럼/인덱스가 있을 때만 제거
--   3) 말투/레벨/경험치 기본값 보정
--   4) 구버전 fallback 테이블 및 대사팩 보장
-- 대상: 기존에 구버전 에고 SQL을 이미 적용한 서버
-- 주의: 실행 전 DB 백업 권장
-- 파일 인코딩: UTF-8
-- DB 문자셋: euckr
-- ============================================================

SET NAMES utf8;

-- ------------------------------------------------------------
-- 1. 구버전 미사용 테이블 삭제
-- ------------------------------------------------------------
DROP TABLE IF EXISTS ego_talk;
DROP TABLE IF EXISTS ego_type;
DROP TABLE IF EXISTS ego_view;
DROP TABLE IF EXISTS `에고모양`;

-- ------------------------------------------------------------
-- 2. ego 테이블의 무기변형 잔여 인덱스/컬럼 삭제 - 반복 실행 안전
-- ------------------------------------------------------------
SET @drop_ego_form_idx := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND INDEX_NAME = 'ego_form_idx') > 0,
    'ALTER TABLE ego DROP INDEX ego_form_idx',
    'SELECT ''ego_form_idx not exists'' AS info'
);
PREPARE stmt_drop_ego_form_idx FROM @drop_ego_form_idx;
EXECUTE stmt_drop_ego_form_idx;
DEALLOCATE PREPARE stmt_drop_ego_form_idx;

SET @drop_ego_form_col := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'form') > 0,
    'ALTER TABLE ego DROP COLUMN form',
    'SELECT ''ego.form not exists'' AS info'
);
PREPARE stmt_drop_ego_form_col FROM @drop_ego_form_col;
EXECUTE stmt_drop_ego_form_col;
DEALLOCATE PREPARE stmt_drop_ego_form_col;

SET @drop_ego_prev_shield_col := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'prev_shield') > 0,
    'ALTER TABLE ego DROP COLUMN prev_shield',
    'SELECT ''ego.prev_shield not exists'' AS info'
);
PREPARE stmt_drop_ego_prev_shield_col FROM @drop_ego_prev_shield_col;
EXECUTE stmt_drop_ego_prev_shield_col;
DEALLOCATE PREPARE stmt_drop_ego_prev_shield_col;

-- ------------------------------------------------------------
-- 3. 필수 보조 테이블 보장
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ego_skill_base (
    skill VARCHAR(40) NOT NULL COMMENT '능력 코드',
    label VARCHAR(50) NOT NULL COMMENT '표시명',
    memo VARCHAR(255) NOT NULL DEFAULT '',
    base_rate INT NOT NULL DEFAULT 3 COMMENT '기본 발동률',
    lv_rate INT NOT NULL DEFAULT 1 COMMENT '레벨당 발동률',
    max_rate INT NOT NULL DEFAULT 25 COMMENT '최대 발동률',
    min_lv INT NOT NULL DEFAULT 1 COMMENT '최소 에고 레벨',
    cool_ms INT NOT NULL DEFAULT 0 COMMENT '능력별 쿨타임 ms',
    effect INT NOT NULL DEFAULT 0 COMMENT 'S_ObjectEffect 이펙트 번호',
    use_yn TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    PRIMARY KEY (skill)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 능력 기본값';

CREATE TABLE IF NOT EXISTS ego_bond (
    item_id BIGINT NOT NULL COMMENT '에고무기 아이템 objectId',
    bond INT NOT NULL DEFAULT 0 COMMENT '유대감 0~1000',
    last_reason VARCHAR(40) NOT NULL DEFAULT '' COMMENT '마지막 증가 사유',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    mod_date DATETIME NULL DEFAULT NULL COMMENT '수정일',
    PRIMARY KEY (item_id),
    INDEX ego_bond_idx (bond)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 유대감 fallback';

CREATE TABLE IF NOT EXISTS ego_talk_pack (
    id BIGINT NOT NULL AUTO_INCREMENT,
    genre VARCHAR(30) NOT NULL COMMENT '장르',
    tone VARCHAR(30) NOT NULL DEFAULT '예의' COMMENT '말투: 예의/예의반대',
    keyword VARCHAR(80) NOT NULL DEFAULT '' COMMENT '예비 키워드',
    message VARCHAR(255) NOT NULL COMMENT '대사',
    use_yn TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    mod_date DATETIME NULL DEFAULT NULL COMMENT '수정일',
    PRIMARY KEY (id),
    INDEX ego_talk_pack_idx (genre, tone, use_yn)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 DB 대사팩';

-- ------------------------------------------------------------
-- 4. 말투/레벨/경험치 보정
-- ------------------------------------------------------------
ALTER TABLE ego MODIFY ego_type VARCHAR(30) NOT NULL DEFAULT '예의' COMMENT '실시간 대화 말투: 예의/예의반대';
ALTER TABLE ego MODIFY ego_lv INT NOT NULL DEFAULT 0 COMMENT '에고 레벨 0~10, 0은 전투능력 없음';
ALTER TABLE ego MODIFY need_exp BIGINT NOT NULL DEFAULT 100 COMMENT '현재 레벨에서 다음 레벨 필요 경험치';

UPDATE ego SET ego_type = '예의' WHERE ego_type IS NULL OR ego_type = '' OR ego_type NOT IN ('예의', '예의반대');
UPDATE ego SET ego_lv = 0 WHERE ego_lv < 0;
UPDATE ego SET ego_lv = 10 WHERE ego_lv > 10;
UPDATE ego SET need_exp = 100 WHERE ego_lv = 0;
UPDATE ego SET need_exp = 250 WHERE ego_lv = 1;
UPDATE ego SET need_exp = 500 WHERE ego_lv = 2;
UPDATE ego SET need_exp = 900 WHERE ego_lv = 3;
UPDATE ego SET need_exp = 1500 WHERE ego_lv = 4;
UPDATE ego SET need_exp = 2400 WHERE ego_lv = 5;
UPDATE ego SET need_exp = 3600 WHERE ego_lv = 6;
UPDATE ego SET need_exp = 5200 WHERE ego_lv = 7;
UPDATE ego SET need_exp = 7500 WHERE ego_lv = 8;
UPDATE ego SET need_exp = 10000 WHERE ego_lv = 9;
UPDATE ego SET need_exp = 0, ego_exp = 0 WHERE ego_lv = 10;

-- ------------------------------------------------------------
-- 5. 스킬베이스 기본값 보장
-- ------------------------------------------------------------
INSERT INTO ego_skill_base
(skill, label, memo, base_rate, lv_rate, max_rate, min_lv, cool_ms, effect, use_yn)
VALUES
('EGO_BALANCE', '공명', 'Lv.1부터 균형형 추가 피해', 3, 1, 25, 1, 0, 3940, 1),
('BLOOD_DRAIN', '흡혈', 'Lv.1부터 HP 회복', 3, 1, 20, 1, 0, 8150, 1),
('MANA_DRAIN', '흡마', 'Lv.1부터 MP 회복', 3, 1, 20, 1, 0, 7300, 1),
('CRITICAL_BURST', '치명', 'Lv.1부터 치명 추가 피해, 레벨별 치명률 증가', 2, 1, 35, 1, 0, 12487, 1),
('GUARDIAN_SHIELD', '수호', 'Lv.1부터 위험 시 HP 회복', 3, 1, 25, 1, 3000, 6321, 1),
('AREA_SLASH', '광역', 'Lv.5부터 주변 몬스터 피해', 2, 1, 20, 5, 3000, 12248, 1),
('EXECUTION', '처형', 'Lv.7부터 약한 대상 추가 피해', 2, 1, 20, 7, 0, 8683, 1),
('FLAME_BRAND', '화염', 'Lv.1부터 화염 추가 피해', 3, 1, 25, 1, 0, 1811, 1),
('FROST_BIND', '서리', 'Lv.1부터 서리 추가 피해', 3, 1, 25, 1, 0, 3684, 1),
('EGO_COUNTER', '반격', 'Lv.5부터 피격 반격, Lv.6부터 자동반격', 35, 5, 100, 5, 2500, 10710, 1),
('EGO_REVENGE', '복수', 'Lv.10 스턴 연동 특수 반격', 50, 0, 50, 10, 6000, 4183, 1)
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
-- 6. fallback 유대감/대사팩 기본값 보장
-- ------------------------------------------------------------
INSERT INTO ego_bond (item_id, bond, last_reason)
SELECT item_id, 0, 'INIT'
FROM ego
WHERE use_yn=1
ON DUPLICATE KEY UPDATE bond=bond;

INSERT INTO ego_talk_pack (genre, tone, keyword, message, use_yn) VALUES
('드라마', '예의', '', '오늘의 전투는 조용히 시작됐지만, 끝은 분명 주인님의 선택으로 기록될 것입니다.', 1),
('드라마', '예의반대', '', '드라마 찍냐? 그래도 주인공이면 끝까지 서 있어야지.', 1),
('영화', '예의', '', '지금은 예고편이 아닙니다. 주인님의 선택이 곧 본편입니다.', 1),
('영화', '예의반대', '', '엔딩 크레딧 보고 싶으면 지금 죽지 마라.', 1),
('웹툰', '예의', '', '한 컷 한 컷 쌓이면 성장 서사가 됩니다. 지금의 작은 경험치도 의미 있습니다.', 1),
('웹툰', '예의반대', '', '이번 회차 제목은 물약 안 먹다 큰일 날 뻔함이냐?', 1),
('무협', '예의', '', '강호에서 오래 살아남는 이는 먼저 베는 자가 아니라 먼저 읽는 자입니다.', 1),
('무협', '예의반대', '', '강호였으면 너 지금 하수 티 난다. 자세 고쳐.', 1),
('아무', '예의', '', '살아남은 자만이 다음 대사를 말할 수 있습니다.', 1),
('아무', '예의반대', '', '대사는 내가 해줄 테니 전투는 네가 해.', 1)
ON DUPLICATE KEY UPDATE message = message;

-- ------------------------------------------------------------
-- 7. 확인
-- ------------------------------------------------------------
SELECT 'EGO_CLEANUP_REPEAT_SAFE_OK' AS result;
DESC ego;
SHOW TABLES LIKE 'ego_bond';
SHOW TABLES LIKE 'ego_talk_pack';
SELECT ego_type, COUNT(*) AS count FROM ego GROUP BY ego_type;
SELECT ego_lv, need_exp, COUNT(*) AS count FROM ego GROUP BY ego_lv, need_exp ORDER BY ego_lv;
SELECT COUNT(*) AS ego_talk_pack_count FROM ego_talk_pack;
