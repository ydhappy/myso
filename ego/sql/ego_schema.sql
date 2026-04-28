-- ============================================================
-- EGO SCHEMA SQL
-- File encoding: UTF-8
-- Runtime DB charset target: euckr
-- Purpose: consolidated install/update SQL for the ego system.
-- Policy: no one-click delete, no full reset, no direct item table insert.
-- Final policy: no weapon type rule table; weapon slot items are eligible except fishing_rod.
-- ============================================================

SET NAMES utf8;

CREATE TABLE IF NOT EXISTS ego (
    item_id BIGINT NOT NULL,
    char_id BIGINT NOT NULL DEFAULT 0,
    use_yn TINYINT(1) NOT NULL DEFAULT 0,
    ego_name VARCHAR(50) NOT NULL DEFAULT '에고',
    ego_type VARCHAR(30) NOT NULL DEFAULT '예의',
    ego_lv INT NOT NULL DEFAULT 0,
    ego_exp BIGINT NOT NULL DEFAULT 0,
    need_exp BIGINT NOT NULL DEFAULT 100,
    talk_lv INT NOT NULL DEFAULT 1,
    ctrl_lv INT NOT NULL DEFAULT 1,
    last_talk BIGINT NOT NULL DEFAULT 0,
    last_warn BIGINT NOT NULL DEFAULT 0,
    bond INT NOT NULL DEFAULT 0,
    bond_reason VARCHAR(40) NOT NULL DEFAULT '',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (item_id),
    INDEX ego_char_idx (char_id),
    INDEX ego_name_idx (ego_name),
    INDEX ego_lv_idx (ego_lv),
    INDEX ego_bond_idx (bond)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고무기 기본 정보';

CREATE TABLE IF NOT EXISTS ego_skill (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    skill VARCHAR(40) NOT NULL,
    skill_lv INT NOT NULL DEFAULT 1,
    rate_bonus INT NOT NULL DEFAULT 0,
    dmg_bonus INT NOT NULL DEFAULT 0,
    last_proc BIGINT NOT NULL DEFAULT 0,
    use_yn TINYINT(1) NOT NULL DEFAULT 1,
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY ego_skill_uk (item_id, skill),
    INDEX ego_skill_item_idx (item_id),
    INDEX ego_skill_idx (skill)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고무기 능력 정보';

CREATE TABLE IF NOT EXISTS ego_skill_base (
    skill VARCHAR(40) NOT NULL,
    label VARCHAR(50) NOT NULL,
    memo VARCHAR(255) NOT NULL DEFAULT '',
    base_rate INT NOT NULL DEFAULT 3,
    lv_rate INT NOT NULL DEFAULT 1,
    max_rate INT NOT NULL DEFAULT 25,
    min_lv INT NOT NULL DEFAULT 1,
    cool_ms INT NOT NULL DEFAULT 0,
    effect INT NOT NULL DEFAULT 0,
    use_yn TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (skill)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 능력 기본값';

CREATE TABLE IF NOT EXISTS ego_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL DEFAULT 0,
    char_id BIGINT NOT NULL DEFAULT 0,
    char_name VARCHAR(50) NOT NULL DEFAULT '',
    target_name VARCHAR(80) NOT NULL DEFAULT '',
    skill VARCHAR(40) NOT NULL DEFAULT '',
    base_dmg INT NOT NULL DEFAULT 0,
    final_dmg INT NOT NULL DEFAULT 0,
    add_dmg INT NOT NULL DEFAULT 0,
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX ego_log_item_idx (item_id),
    INDEX ego_log_char_idx (char_id),
    INDEX ego_log_skill_idx (skill),
    INDEX ego_log_date_idx (reg_date)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 능력 발동 기록';

CREATE TABLE IF NOT EXISTS ego_talk_pack (
    id BIGINT NOT NULL AUTO_INCREMENT,
    genre VARCHAR(30) NOT NULL,
    tone VARCHAR(30) NOT NULL DEFAULT '예의',
    keyword VARCHAR(80) NOT NULL DEFAULT '',
    message VARCHAR(255) NOT NULL,
    use_yn TINYINT(1) NOT NULL DEFAULT 1,
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX ego_talk_pack_idx (genre, tone, use_yn),
    UNIQUE KEY ego_talk_pack_uk (genre, tone, message)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 DB 대사팩';

CREATE TABLE IF NOT EXISTS ego_config (
    config_key VARCHAR(80) NOT NULL,
    config_value VARCHAR(255) NOT NULL DEFAULT '',
    memo VARCHAR(255) NOT NULL DEFAULT '',
    use_yn TINYINT(1) NOT NULL DEFAULT 1,
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 공통 설정';

CREATE TABLE IF NOT EXISTS ego_level (
    ego_lv INT NOT NULL,
    need_exp BIGINT NOT NULL DEFAULT 0,
    proc_bonus INT NOT NULL DEFAULT 0,
    critical_chance INT NOT NULL DEFAULT 0,
    critical_damage INT NOT NULL DEFAULT 0,
    counter_chance INT NOT NULL DEFAULT 0,
    counter_power INT NOT NULL DEFAULT 0,
    counter_critical INT NOT NULL DEFAULT 0,
    memo VARCHAR(255) NOT NULL DEFAULT '',
    use_yn TINYINT(1) NOT NULL DEFAULT 1,
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (ego_lv)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 레벨 통합 설정';

CREATE TABLE IF NOT EXISTS ego_item_template (
    item_code INT NOT NULL,
    item_name VARCHAR(80) NOT NULL,
    java_class VARCHAR(120) NOT NULL DEFAULT 'lineage.world.object.item.EgoOrb',
    item_type1 VARCHAR(40) NOT NULL DEFAULT 'etc',
    item_type2 VARCHAR(40) NOT NULL DEFAULT 'normal',
    name_id VARCHAR(80) NOT NULL DEFAULT '$900001',
    inv_gfx INT NOT NULL DEFAULT 4038,
    ground_gfx INT NOT NULL DEFAULT 4038,
    stackable TINYINT(1) NOT NULL DEFAULT 1,
    memo VARCHAR(255) NOT NULL DEFAULT '',
    use_yn TINYINT(1) NOT NULL DEFAULT 1,
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (item_code),
    UNIQUE KEY ego_item_template_name_uk (item_name)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 전용 아이템 DB 템플릿';

CREATE TABLE IF NOT EXISTS ego_bond (
    item_id BIGINT NOT NULL,
    bond INT NOT NULL DEFAULT 0,
    last_reason VARCHAR(40) NOT NULL DEFAULT '',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (item_id),
    INDEX ego_bond_idx (bond)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='구버전 유대감 fallback';

CREATE TABLE IF NOT EXISTS ego_level_exp (
    ego_lv INT NOT NULL,
    need_exp BIGINT NOT NULL DEFAULT 0,
    memo VARCHAR(255) NOT NULL DEFAULT '',
    use_yn TINYINT(1) NOT NULL DEFAULT 1,
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (ego_lv)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='구버전 레벨 경험치 fallback';

CREATE TABLE IF NOT EXISTS ego_level_bonus (
    ego_lv INT NOT NULL,
    proc_bonus INT NOT NULL DEFAULT 0,
    critical_chance INT NOT NULL DEFAULT 0,
    critical_damage INT NOT NULL DEFAULT 0,
    counter_chance INT NOT NULL DEFAULT 0,
    counter_power INT NOT NULL DEFAULT 0,
    counter_critical INT NOT NULL DEFAULT 0,
    memo VARCHAR(255) NOT NULL DEFAULT '',
    use_yn TINYINT(1) NOT NULL DEFAULT 1,
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (ego_lv)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='구버전 레벨 보너스 fallback';

DROP TABLE IF EXISTS ego_talk;
DROP TABLE IF EXISTS ego_type;
DROP TABLE IF EXISTS ego_view;
DROP TABLE IF EXISTS ego_weapon_rule;
DROP TABLE IF EXISTS `에고모양`;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'bond') = 0,
    'ALTER TABLE ego ADD COLUMN bond INT NOT NULL DEFAULT 0',
    'SELECT ''ego.bond already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'bond_reason') = 0,
    'ALTER TABLE ego ADD COLUMN bond_reason VARCHAR(40) NOT NULL DEFAULT ""',
    'SELECT ''ego.bond_reason already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND INDEX_NAME = 'ego_bond_idx') = 0,
    'ALTER TABLE ego ADD INDEX ego_bond_idx (bond)',
    'SELECT ''ego_bond_idx already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'form') > 0,
    'ALTER TABLE ego DROP COLUMN form',
    'SELECT ''ego.form not exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'prev_shield') > 0,
    'ALTER TABLE ego DROP COLUMN prev_shield',
    'SELECT ''ego.prev_shield not exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE ego e INNER JOIN ego_bond b ON e.item_id = b.item_id
SET e.bond = b.bond, e.bond_reason = b.last_reason, e.mod_date = NOW();

DELETE t1 FROM ego_talk_pack t1
INNER JOIN ego_talk_pack t2
    ON t1.id > t2.id AND t1.genre = t2.genre AND t1.tone = t2.tone AND t1.message = t2.message;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego_talk_pack' AND INDEX_NAME = 'ego_talk_pack_uk') = 0,
    'ALTER TABLE ego_talk_pack ADD UNIQUE KEY ego_talk_pack_uk (genre, tone, message)',
    'SELECT ''ego_talk_pack_uk already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO ego_skill_base (skill, label, memo, base_rate, lv_rate, max_rate, min_lv, cool_ms, effect, use_yn) VALUES
('EGO_BALANCE', '공명', '균형형 추가 피해', 3, 1, 25, 1, 0, 3940, 1),
('BLOOD_DRAIN', '흡혈', 'HP 회복', 3, 1, 20, 1, 0, 8150, 1),
('MANA_DRAIN', '흡마', 'MP 회복', 3, 1, 20, 1, 0, 7300, 1),
('CRITICAL_BURST', '치명', '치명 추가 피해', 2, 1, 35, 1, 0, 12487, 1),
('GUARDIAN_SHIELD', '수호', '위험 시 HP 회복', 3, 1, 25, 1, 3000, 6321, 1),
('AREA_SLASH', '광역', '주변 몬스터 피해', 2, 1, 20, 5, 3000, 12248, 1),
('EXECUTION', '처형', '낮은 HP 대상 추가 피해', 2, 1, 20, 7, 0, 8683, 1),
('FLAME_BRAND', '화염', '화염 추가 피해', 3, 1, 25, 1, 0, 1811, 1),
('FROST_BIND', '서리', '서리 추가 피해', 3, 1, 25, 1, 0, 3684, 1),
('EGO_COUNTER', '반격', '피격/자동 반격', 35, 5, 100, 5, 2500, 10710, 1),
('EGO_REVENGE', '복수', 'Lv.10 스턴 연동 반격', 50, 0, 50, 10, 6000, 4183, 1)
ON DUPLICATE KEY UPDATE label=VALUES(label), memo=VALUES(memo);

INSERT INTO ego_config (config_key, config_value, memo, use_yn) VALUES
('ego_orb_item_code', '900001', '에고 구슬 아이템코드', 1),
('ego_orb_item_name', '에고 구슬', '에고 구슬 아이템명', 1),
('genre_talk_delay_ms', '1200', '장르대화 연속 입력 방지 딜레이 ms', 1),
('auto_talk_hp_warn_rate', '25', 'HP 자동 위험 대사 기준', 1),
('auto_talk_mp_warn_rate', '15', 'MP 자동 안내 대사 기준', 1),
('attack_ego_exp', '1', '공격 중 획득 경험치', 1),
('attack_exp_delay_ms', '3000', '공격 경험치 획득 딜레이 ms', 1),
('kill_ego_exp', '5', '몬스터 처치 경험치', 1),
('boss_kill_ego_exp', '50', '보스 처치 추가 경험치', 1),
('exp_message_rate', '3', '경험치 메시지 확률', 1),
('proc_message_delay_ms', '1500', '능력 발동 메시지 딜레이 ms', 1),
('combo_add_damage_rate', '12', '공통 에고 발동 추가 피해율', 1),
('combo_add_damage_level_rate', '2', '에고 레벨당 공통 추가 피해율', 1),
('combo_heal_rate', '5', '공통 에고 발동 HP 회복율', 1),
('combo_heal_max_rate', '12', '공통 에고 발동 HP 회복율 상한', 1),
('combo_stun_chance', '4', '공통 에고 발동 스턴 기본 확률', 1),
('combo_stun_level_bonus', '1', '에고 레벨당 공통 스턴 확률 보너스', 1),
('combo_slow_chance', '9', '공통 에고 발동 슬로우 기본 확률', 1),
('combo_slow_level_bonus', '1', '에고 레벨당 공통 슬로우 확률 보너스', 1),
('combo_slow_time', '4', '공통 에고 슬로우 지속 시간 초', 1),
('combo_slow_cool_ms', '4000', '공통 에고 슬로우 쿨타임 ms', 1),
('combo_slow_effect', '3684', '공통 에고 슬로우 이펙트', 1),
('counter_unlock_level', '5', '피격 반격 해금 레벨', 1),
('auto_counter_unlock_level', '6', '자동반격 해금 레벨', 1),
('auto_counter_cool_ms', '2500', '자동반격 쿨타임 ms', 1),
('auto_counter_chance', '100', '자동반격 발동 확률', 1),
('stun_level', '10', '에고 스턴 해금 레벨', 1),
('stun_success_rate', '50', '에고 스턴 성공 확률', 1),
('stun_time', '2', '에고 스턴 시간 초', 1),
('stun_effect', '4183', '에고 스턴 이펙트 번호', 1),
('stun_cool_ms', '6000', '에고 스턴 쿨타임 ms', 1),
('guardian_shield_hp_rate', '40', '수호 의지 발동 HP 기준', 1),
('execution_target_hp_rate', '20', '처형 발동 대상 HP 기준', 1),
('area_range', '2', '광역 능력 범위', 1),
('area_max_target', '4', '광역 능력 최대 대상 수', 1)
ON DUPLICATE KEY UPDATE memo=VALUES(memo), use_yn=VALUES(use_yn);

DELETE FROM ego_config WHERE config_key IN ('change_orb_item_code', 'change_orb_item_name');
DELETE FROM ego_item_template WHERE item_name='에고 변경구슬' AND item_code<>900001;
INSERT INTO ego_item_template (item_code, item_name, java_class, item_type1, item_type2, name_id, inv_gfx, ground_gfx, stackable, memo, use_yn) VALUES
(900001, '에고 구슬', 'lineage.world.object.item.EgoOrb', 'etc', 'normal', '$900001', 4038, 4038, 1, '착용 무기에 에고를 최초 생성한다. 이미 에고무기면 능력/대화/레벨 변경 없이 주인만 재인식한다.', 1)
ON DUPLICATE KEY UPDATE item_name=VALUES(item_name), java_class=VALUES(java_class), memo=VALUES(memo), use_yn=VALUES(use_yn);

SELECT 'REGISTER_EGO_ORB_MANUALLY' AS guide,
       item_code,
       item_name,
       java_class,
       item_type1,
       item_type2,
       name_id,
       inv_gfx,
       ground_gfx,
       stackable
FROM ego_item_template
WHERE item_name='에고 구슬';

INSERT INTO ego_level (ego_lv, need_exp, proc_bonus, critical_chance, critical_damage, counter_chance, counter_power, counter_critical, memo, use_yn) VALUES
(0,100,0,0,0,0,0,0,'Lv.0 전투능력 없음',1),
(1,250,0,1,1,0,0,0,'Lv.1 기본 발동 시작',1),
(2,500,1,2,2,0,0,0,'Lv.2',1),
(3,900,2,3,3,0,0,0,'Lv.3',1),
(4,1500,3,4,4,0,0,0,'Lv.4',1),
(5,2400,4,6,6,35,18,8,'Lv.5 피격 반격 시작',1),
(6,3600,6,9,8,100,24,12,'Lv.6 자동반격 시작',1),
(7,5200,8,12,10,100,30,16,'Lv.7',1),
(8,7500,10,15,12,100,38,20,'Lv.8',1),
(9,10000,12,18,15,100,46,25,'Lv.9',1),
(10,0,15,25,20,100,60,35,'Lv.10 스턴 연동',1)
ON DUPLICATE KEY UPDATE memo=VALUES(memo);

INSERT INTO ego_talk_pack (genre, tone, keyword, message, use_yn) VALUES
('드라마','예의','','오늘의 전투는 조용히 시작됐지만, 끝은 분명 주인님의 선택으로 기록될 것입니다.',1),
('드라마','예의반대','','드라마 찍냐? 그래도 주인공이면 끝까지 서 있어야지.',1),
('영화','예의','','지금은 예고편이 아닙니다. 주인님의 선택이 곧 본편입니다.',1),
('영화','예의반대','','엔딩 크레딧 보고 싶으면 지금 죽지 마라.',1),
('웹툰','예의','','한 컷 한 컷 쌓이면 성장 서사가 됩니다. 지금의 작은 경험치도 의미 있습니다.',1),
('웹툰','예의반대','','이번 회차 제목은 물약 안 먹다 큰일 날 뻔함이냐?',1),
('무협','예의','','강호에서 오래 살아남는 이는 먼저 베는 자가 아니라 먼저 읽는 자입니다.',1),
('무협','예의반대','','강호였으면 너 지금 하수 티 난다. 자세 고쳐.',1),
('아무','예의','','살아남은 자만이 다음 대사를 말할 수 있습니다.',1),
('아무','예의반대','','대사는 내가 해줄 테니 전투는 네가 해.',1)
ON DUPLICATE KEY UPDATE keyword=VALUES(keyword);

UPDATE ego SET ego_type='예의' WHERE ego_type IS NULL OR ego_type='' OR ego_type NOT IN ('예의','예의반대');
UPDATE ego SET ego_lv=0 WHERE ego_lv < 0;
UPDATE ego SET ego_lv=10 WHERE ego_lv > 10;
UPDATE ego e INNER JOIN ego_level l ON e.ego_lv=l.ego_lv SET e.need_exp=l.need_exp WHERE l.use_yn=1;
UPDATE ego SET need_exp=0, ego_exp=0 WHERE ego_lv >= 10;

SELECT 'EGO_SCHEMA_SQL_OK' AS result;
SHOW TABLES LIKE 'ego%';
SELECT * FROM ego_level ORDER BY ego_lv;
SELECT * FROM ego_config ORDER BY config_key;
SELECT * FROM ego_item_template ORDER BY item_code;
