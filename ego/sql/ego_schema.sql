-- ============================================================
-- EGO SCHEMA SQL
-- File encoding: UTF-8
-- Runtime DB charset target: euckr
-- Policy: no one-click delete, no full reset, no direct item table insert.
-- Java runtime ability names are kept 1:1 with EgoWeaponAbilityController.
-- Simple input aliases are normalized in EgoWeaponDatabase.
-- Ego personality styles: 예의, 예의반대, 싸이코패스.
-- DB reorganization:
--   ego_skill -> merged into ego ability_* columns.
--   ego_bond  -> merged into ego bond/bond_reason columns.
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
    ability_type VARCHAR(40) NOT NULL DEFAULT '',
    ability_lv INT NOT NULL DEFAULT 1,
    ability_rate_bonus INT NOT NULL DEFAULT 0,
    ability_dmg_bonus INT NOT NULL DEFAULT 0,
    ability_last_proc BIGINT NOT NULL DEFAULT 0,
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
    INDEX ego_ability_idx (ability_type),
    INDEX ego_bond_idx (bond)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고무기 통합 정보';

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

DROP TABLE IF EXISTS ego_talk;
DROP TABLE IF EXISTS ego_type;
DROP TABLE IF EXISTS ego_view;
DROP TABLE IF EXISTS ego_weapon_rule;
DROP TABLE IF EXISTS ego_level_exp;
DROP TABLE IF EXISTS ego_level_bonus;
DROP TABLE IF EXISTS `에고모양`;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'bond') = 0,
    'ALTER TABLE ego ADD COLUMN bond INT NOT NULL DEFAULT 0',
    'SELECT ''ego.bond already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'bond_reason') = 0,
    'ALTER TABLE ego ADD COLUMN bond_reason VARCHAR(40) NOT NULL DEFAULT ""',
    'SELECT ''ego.bond_reason already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'ability_type') = 0,
    'ALTER TABLE ego ADD COLUMN ability_type VARCHAR(40) NOT NULL DEFAULT "" AFTER ctrl_lv',
    'SELECT ''ego.ability_type already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'ability_lv') = 0,
    'ALTER TABLE ego ADD COLUMN ability_lv INT NOT NULL DEFAULT 1 AFTER ability_type',
    'SELECT ''ego.ability_lv already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'ability_rate_bonus') = 0,
    'ALTER TABLE ego ADD COLUMN ability_rate_bonus INT NOT NULL DEFAULT 0 AFTER ability_lv',
    'SELECT ''ego.ability_rate_bonus already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'ability_dmg_bonus') = 0,
    'ALTER TABLE ego ADD COLUMN ability_dmg_bonus INT NOT NULL DEFAULT 0 AFTER ability_rate_bonus',
    'SELECT ''ego.ability_dmg_bonus already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'ability_last_proc') = 0,
    'ALTER TABLE ego ADD COLUMN ability_last_proc BIGINT NOT NULL DEFAULT 0 AFTER ability_dmg_bonus',
    'SELECT ''ego.ability_last_proc already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND INDEX_NAME = 'ego_ability_idx') = 0,
    'ALTER TABLE ego ADD INDEX ego_ability_idx (ability_type)',
    'SELECT ''ego_ability_idx already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE ego SET ability_type='EGO_BALANCE' WHERE ability_type='BALANCE';
UPDATE ego SET ability_type='BLOOD_DRAIN' WHERE ability_type='BLOOD';
UPDATE ego SET ability_type='MANA_DRAIN' WHERE ability_type='MANA';
UPDATE ego SET ability_type='CRITICAL_BURST' WHERE ability_type='CRIT';
UPDATE ego SET ability_type='GUARDIAN_SHIELD' WHERE ability_type='SHIELD';
UPDATE ego SET ability_type='AREA_SLASH' WHERE ability_type='AREA';
UPDATE ego SET ability_type='EXECUTION' WHERE ability_type='EXECUTE';
UPDATE ego SET ability_type='FLAME_BRAND' WHERE ability_type='FIRE';
UPDATE ego SET ability_type='FROST_BIND' WHERE ability_type='FROST';
UPDATE ego SET ability_type='EGO_COUNTER' WHERE ability_type='COUNTER';
UPDATE ego SET ability_type='EGO_REVENGE' WHERE ability_type='REVENGE';

SET @has_ego_skill := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego_skill');
SET @sql := IF(@has_ego_skill > 0,
    'UPDATE ego_skill SET skill=''EGO_BALANCE'' WHERE skill=''BALANCE''',
    'SELECT ''ego_skill not exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@has_ego_skill > 0,
    'UPDATE ego_skill SET skill=''BLOOD_DRAIN'' WHERE skill=''BLOOD''',
    'SELECT ''ego_skill not exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@has_ego_skill > 0,
    'UPDATE ego_skill SET skill=''MANA_DRAIN'' WHERE skill=''MANA''',
    'SELECT ''ego_skill not exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@has_ego_skill > 0,
    'UPDATE ego_skill SET skill=''CRITICAL_BURST'' WHERE skill=''CRIT''',
    'SELECT ''ego_skill not exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@has_ego_skill > 0,
    'UPDATE ego_skill SET skill=''GUARDIAN_SHIELD'' WHERE skill=''SHIELD''',
    'SELECT ''ego_skill not exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@has_ego_skill > 0,
    'UPDATE ego_skill SET skill=''AREA_SLASH'' WHERE skill=''AREA''',
    'SELECT ''ego_skill not exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@has_ego_skill > 0,
    'UPDATE ego_skill SET skill=''EXECUTION'' WHERE skill=''EXECUTE''',
    'SELECT ''ego_skill not exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@has_ego_skill > 0,
    'UPDATE ego_skill SET skill=''FLAME_BRAND'' WHERE skill=''FIRE''',
    'SELECT ''ego_skill not exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@has_ego_skill > 0,
    'UPDATE ego_skill SET skill=''FROST_BIND'' WHERE skill=''FROST''',
    'SELECT ''ego_skill not exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@has_ego_skill > 0,
    'UPDATE ego_skill SET skill=''EGO_COUNTER'' WHERE skill=''COUNTER''',
    'SELECT ''ego_skill not exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql := IF(@has_ego_skill > 0,
    'UPDATE ego_skill SET skill=''EGO_REVENGE'' WHERE skill=''REVENGE''',
    'SELECT ''ego_skill not exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(@has_ego_skill > 0,
    'UPDATE ego e SET ability_type=(SELECT s.skill FROM ego_skill s WHERE s.item_id=e.item_id AND s.use_yn=1 ORDER BY s.id ASC LIMIT 1), ability_lv=IFNULL((SELECT s.skill_lv FROM ego_skill s WHERE s.item_id=e.item_id AND s.use_yn=1 ORDER BY s.id ASC LIMIT 1), 1), ability_rate_bonus=IFNULL((SELECT s.rate_bonus FROM ego_skill s WHERE s.item_id=e.item_id AND s.use_yn=1 ORDER BY s.id ASC LIMIT 1), 0), ability_dmg_bonus=IFNULL((SELECT s.dmg_bonus FROM ego_skill s WHERE s.item_id=e.item_id AND s.use_yn=1 ORDER BY s.id ASC LIMIT 1), 0), ability_last_proc=IFNULL((SELECT s.last_proc FROM ego_skill s WHERE s.item_id=e.item_id AND s.use_yn=1 ORDER BY s.id ASC LIMIT 1), 0), mod_date=NOW() WHERE EXISTS (SELECT 1 FROM ego_skill s WHERE s.item_id=e.item_id AND s.use_yn=1)',
    'SELECT ''ego_skill migration skipped'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_ego_bond := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego_bond');
SET @sql := IF(@has_ego_bond > 0,
    'UPDATE ego e SET bond=IFNULL((SELECT b.bond FROM ego_bond b WHERE b.item_id=e.item_id LIMIT 1), e.bond), bond_reason=IFNULL((SELECT b.last_reason FROM ego_bond b WHERE b.item_id=e.item_id LIMIT 1), e.bond_reason), mod_date=NOW() WHERE EXISTS (SELECT 1 FROM ego_bond b WHERE b.item_id=e.item_id)',
    'SELECT ''ego_bond migration skipped'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE ego SET ability_type='' WHERE ability_type IS NULL;
UPDATE ego SET ability_lv=1 WHERE ability_lv IS NULL OR ability_lv < 1;
UPDATE ego SET ability_rate_bonus=0 WHERE ability_rate_bonus IS NULL;
UPDATE ego SET ability_dmg_bonus=0 WHERE ability_dmg_bonus IS NULL OR ability_dmg_bonus < 0;
UPDATE ego SET ability_last_proc=0 WHERE ability_last_proc IS NULL OR ability_last_proc < 0;

INSERT INTO ego_skill_base (skill, label, memo, base_rate, lv_rate, max_rate, min_lv, cool_ms, effect, use_yn) VALUES
('EGO_BALANCE', '공명', '균형형 추가 피해', 3, 1, 25, 1, 0, 3940, 1),
('BLOOD_DRAIN', '흡혈', 'HP 흡수', 3, 1, 20, 1, 0, 8150, 1),
('MANA_DRAIN', '흡마', 'MP 흡수', 3, 1, 20, 1, 0, 7300, 1),
('CRITICAL_BURST', '치명', '치명 추가 피해', 2, 1, 35, 1, 0, 12487, 1),
('GUARDIAN_SHIELD', '수호', '위험 시 HP 흡수', 3, 1, 25, 1, 3000, 6321, 1),
('AREA_SLASH', '광역', '주변 몬스터 피해', 2, 1, 20, 5, 3000, 12248, 1),
('EXECUTION', '처형', '낮은 HP 대상 추가 피해', 2, 1, 20, 7, 0, 8683, 1),
('FLAME_BRAND', '화염', '화염 추가 피해', 3, 1, 25, 1, 0, 1811, 1),
('FROST_BIND', '서리', '서리 추가 피해', 3, 1, 25, 1, 0, 3684, 1),
('EGO_COUNTER', '반격', '피격/자동 반격', 35, 5, 100, 5, 2500, 10710, 1),
('EGO_REVENGE', '복수', 'Lv.10 스턴 연동 반격', 50, 0, 50, 10, 6000, 4183, 1)
ON DUPLICATE KEY UPDATE label=VALUES(label), memo=VALUES(memo), base_rate=VALUES(base_rate), lv_rate=VALUES(lv_rate), max_rate=VALUES(max_rate), min_lv=VALUES(min_lv), cool_ms=VALUES(cool_ms), effect=VALUES(effect), use_yn=VALUES(use_yn);

DELETE FROM ego_skill_base WHERE skill IN ('BALANCE','BLOOD','MANA','CRIT','SHIELD','AREA','EXECUTE','FIRE','FROST','COUNTER','REVENGE');

INSERT INTO ego_config (config_key, config_value, memo, use_yn) VALUES
('ego_orb_item_code', '900001', '에고 구슬 아이템코드', 1),
('ego_orb_item_name', '에고 구슬', '에고 구슬 아이템명', 1),
('genre_talk_delay_ms', '1200', '장르대화 연속 입력 방지 딜레이 ms', 1),
('auto_talk_hp_warn_rate', '25', 'HP 자동 위험 대사 기준', 1),
('auto_talk_mp_warn_rate', '15', 'MP 자동 안내 대사 기준', 1),
('auto_talk_idle_hp_rate', '80', '자동 안정 대사 HP 기준', 1),
('auto_talk_idle_mp_rate', '50', '자동 안정 대사 MP 기준', 1),
('auto_talk_hp_warn_delay_ms', '15000', 'HP 자동 경고 딜레이 ms', 1),
('auto_talk_mp_warn_delay_ms', '20000', 'MP 자동 경고 딜레이 ms', 1),
('auto_talk_boss_warn_delay_ms', '30000', '보스 자동 경고 딜레이 ms', 1),
('auto_talk_idle_delay_ms', '180000', '안정 상태 자동 대사 딜레이 ms', 1),
('attack_ego_exp', '1', '공격 중 획득 경험치', 1),
('attack_exp_delay_ms', '3000', '공격 경험치 획득 딜레이 ms', 1),
('kill_ego_exp', '5', '몬스터 처치 경험치', 1),
('boss_kill_ego_exp', '50', '보스 처치 추가 경험치', 1),
('exp_message_rate', '3', '경험치 메시지 확률', 1),
('proc_message_delay_ms', '1500', '능력 발동 메시지 딜레이 ms', 1),
('combo_add_damage_rate', '12', '공통 에고 발동 추가 피해율', 1),
('combo_add_damage_level_rate', '2', '에고 레벨당 공통 추가 피해율', 1),
('combo_hp_absorb_rate', '5', '공통 에고 발동 HP 흡수율', 1),
('combo_hp_absorb_max_rate', '12', '공통 에고 발동 HP 흡수율 상한', 1),
('combo_mp_absorb_rate', '3', '공통 에고 발동 MP 흡수율', 1),
('combo_mp_absorb_max_rate', '8', '공통 에고 발동 MP 흡수율 상한', 1),
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
('guardian_shield_hp_rate', '40', '수호 흡수 발동 HP 기준', 1),
('execution_target_hp_rate', '20', '처형 발동 대상 HP 기준', 1),
('area_range', '2', '광역 능력 범위', 1),
('area_max_target', '4', '광역 능력 최대 대상 수', 1)
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value), memo=VALUES(memo), use_yn=VALUES(use_yn);

DELETE FROM ego_config WHERE config_key IN ('change_orb_item_code', 'change_orb_item_name', 'combo_heal_rate', 'combo_heal_max_rate');
DELETE FROM ego_item_template WHERE item_name='에고 변경구슬' AND item_code<>900001;
INSERT INTO ego_item_template (item_code, item_name, java_class, item_type1, item_type2, name_id, inv_gfx, ground_gfx, stackable, memo, use_yn) VALUES
(900001, '에고 구슬', 'lineage.world.object.item.EgoOrb', 'etc', 'normal', '$900001', 4038, 4038, 1, '착용 무기에 에고를 최초 생성한다. 이미 에고무기면 능력/대화/레벨 변경 없이 주인만 재인식한다.', 1)
ON DUPLICATE KEY UPDATE item_name=VALUES(item_name), java_class=VALUES(java_class), memo=VALUES(memo), use_yn=VALUES(use_yn);

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
ON DUPLICATE KEY UPDATE memo=VALUES(memo), need_exp=VALUES(need_exp), proc_bonus=VALUES(proc_bonus), critical_chance=VALUES(critical_chance), critical_damage=VALUES(critical_damage), counter_chance=VALUES(counter_chance), counter_power=VALUES(counter_power), counter_critical=VALUES(counter_critical), use_yn=VALUES(use_yn);

INSERT INTO ego_talk_pack (genre, tone, keyword, message, use_yn) VALUES
('드라마','예의','','오늘의 전투는 조용히 시작됐지만, 끝은 분명 주인님의 선택으로 기록될 것입니다.',1),
('드라마','예의반대','','드라마 찍냐? 그래도 주인공이면 끝까지 서 있어야지.',1),
('드라마','싸이코패스','','좋아. 지금 장면은 조용히 금이 가고 있어. 네가 무너지지 않으면 더 아름답겠지.',1),
('영화','예의','','지금은 예고편이 아닙니다. 주인님의 선택이 곧 본편입니다.',1),
('영화','예의반대','','엔딩 크레딧 보고 싶으면 지금 죽지 마라.',1),
('영화','싸이코패스','','카메라가 있다면 지금 네 손끝을 찍었을 거야. 떨림까지 전부 기록되니까.',1),
('웹툰','예의','','한 컷 한 컷 쌓이면 성장 서사가 됩니다. 지금의 작은 경험치도 의미 있습니다.',1),
('웹툰','예의반대','','이번 회차 제목은 물약 안 먹다 큰일 날 뻔함이냐?',1),
('웹툰','싸이코패스','','이번 컷은 마음에 들어. 네가 버티면 다음 컷은 더 잔인하게 빛날 거야.',1),
('무협','예의','','강호에서 오래 살아남는 이는 먼저 베는 자가 아니라 먼저 읽는 자입니다.',1),
('무협','예의반대','','강호였으면 너 지금 하수 티 난다. 자세 고쳐.',1),
('무협','싸이코패스','','검끝이 조용할수록 속은 더 시끄럽지. 좋아, 그 소리를 따라 베어.',1),
('공포','싸이코패스','','뒤를 봐. 아무것도 없다면 더 좋아. 공포는 보이지 않을 때 가장 오래 남거든.',1),
('빌런','싸이코패스','','정답은 간단해. 망설임을 버려. 다만 네가 어디까지 버릴 수 있는지 보자.',1),
('아무','예의','','살아남은 자만이 다음 대사를 말할 수 있습니다.',1),
('아무','예의반대','','대사는 내가 해줄 테니 전투는 네가 해.',1),
('아무','싸이코패스','','아직 살아 있네. 좋아. 더 보여줘. 네 한계가 어디서 부서지는지 궁금하거든.',1)
ON DUPLICATE KEY UPDATE keyword=VALUES(keyword), use_yn=VALUES(use_yn);

UPDATE ego SET ego_type='싸이코패스' WHERE ego_type IN ('사이코패스','psycho','psychopath');
UPDATE ego SET ego_type='예의반대' WHERE ego_type IN ('반말','막말','싸가지');
UPDATE ego SET ego_type='예의' WHERE ego_type IS NULL OR ego_type='' OR ego_type NOT IN ('예의','예의반대','싸이코패스');
UPDATE ego SET ego_lv=0 WHERE ego_lv < 0;
UPDATE ego SET ego_lv=10 WHERE ego_lv > 10;
UPDATE ego e INNER JOIN ego_level l ON e.ego_lv=l.ego_lv SET e.need_exp=l.need_exp WHERE l.use_yn=1;
UPDATE ego SET need_exp=0, ego_exp=0 WHERE ego_lv >= 10;

DROP TABLE IF EXISTS ego_skill;
DROP TABLE IF EXISTS ego_bond;

SELECT 'EGO_SCHEMA_SQL_OK' AS result;
SHOW TABLES LIKE 'ego%';
SELECT item_id, char_id, ego_name, ego_type, ego_lv, ego_exp, need_exp, ability_type, ability_lv, ability_rate_bonus, ability_dmg_bonus, ability_last_proc, bond, bond_reason FROM ego;
SELECT * FROM ego_level ORDER BY ego_lv;
SELECT * FROM ego_config ORDER BY config_key;
SELECT * FROM ego_skill_base ORDER BY skill;
SELECT * FROM ego_item_template ORDER BY item_code;
SELECT genre, tone, message FROM ego_talk_pack WHERE tone='싸이코패스' ORDER BY genre, id;
