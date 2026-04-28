-- ============================================================
-- EGO SCHEMA SQL
-- File encoding: UTF-8
-- Runtime DB charset target: euckr
-- Purpose: one consolidated install/update/migration SQL for the ego system.
-- Policy: no one-click full delete, no full data purge, no table drop reset script.
-- ============================================================

SET NAMES utf8;

-- ------------------------------------------------------------
-- Core tables
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ego (
    item_id BIGINT NOT NULL COMMENT '에고무기 아이템 objectId',
    char_id BIGINT NOT NULL DEFAULT 0 COMMENT '소유 캐릭터 objectId',
    use_yn TINYINT(1) NOT NULL DEFAULT 0 COMMENT '사용 여부',
    ego_name VARCHAR(50) NOT NULL DEFAULT '에고' COMMENT '호출 이름',
    ego_type VARCHAR(30) NOT NULL DEFAULT '예의' COMMENT '말투: 예의/예의반대',
    ego_lv INT NOT NULL DEFAULT 0 COMMENT '레벨 0~10',
    ego_exp BIGINT NOT NULL DEFAULT 0 COMMENT '현재 경험치',
    need_exp BIGINT NOT NULL DEFAULT 100 COMMENT '다음 레벨 필요 경험치',
    talk_lv INT NOT NULL DEFAULT 1 COMMENT '대화 단계',
    ctrl_lv INT NOT NULL DEFAULT 1 COMMENT '제어 단계',
    last_talk BIGINT NOT NULL DEFAULT 0 COMMENT '마지막 대화 시간',
    last_warn BIGINT NOT NULL DEFAULT 0 COMMENT '마지막 경고 시간',
    bond INT NOT NULL DEFAULT 0 COMMENT '유대감 0~1000',
    bond_reason VARCHAR(40) NOT NULL DEFAULT '' COMMENT '마지막 유대감 사유',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    mod_date DATETIME NULL DEFAULT NULL COMMENT '수정일',
    PRIMARY KEY (item_id),
    INDEX ego_char_idx (char_id),
    INDEX ego_name_idx (ego_name),
    INDEX ego_lv_idx (ego_lv),
    INDEX ego_bond_idx (bond)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고무기 기본 정보';

CREATE TABLE IF NOT EXISTS ego_skill (
    id BIGINT NOT NULL AUTO_INCREMENT,
    item_id BIGINT NOT NULL COMMENT '에고무기 아이템 objectId',
    skill VARCHAR(40) NOT NULL COMMENT '능력 코드',
    skill_lv INT NOT NULL DEFAULT 1 COMMENT '능력 레벨',
    rate_bonus INT NOT NULL DEFAULT 0 COMMENT '추가 확률',
    dmg_bonus INT NOT NULL DEFAULT 0 COMMENT '추가 피해',
    last_proc BIGINT NOT NULL DEFAULT 0 COMMENT '마지막 발동 시간',
    use_yn TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    mod_date DATETIME NULL DEFAULT NULL COMMENT '수정일',
    PRIMARY KEY (id),
    UNIQUE KEY ego_skill_uk (item_id, skill),
    INDEX ego_skill_item_idx (item_id),
    INDEX ego_skill_idx (skill)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고무기 능력 정보';

CREATE TABLE IF NOT EXISTS ego_skill_base (
    skill VARCHAR(40) NOT NULL COMMENT '능력 코드',
    label VARCHAR(50) NOT NULL COMMENT '표시명',
    memo VARCHAR(255) NOT NULL DEFAULT '',
    base_rate INT NOT NULL DEFAULT 3 COMMENT '기본 발동률',
    lv_rate INT NOT NULL DEFAULT 1 COMMENT '레벨당 발동률',
    max_rate INT NOT NULL DEFAULT 25 COMMENT '최대 발동률',
    min_lv INT NOT NULL DEFAULT 1 COMMENT '최소 에고 레벨',
    cool_ms INT NOT NULL DEFAULT 0 COMMENT '쿨타임 ms',
    effect INT NOT NULL DEFAULT 0 COMMENT 'S_ObjectEffect 이펙트 번호',
    use_yn TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
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
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '기록일',
    PRIMARY KEY (id),
    INDEX ego_log_item_idx (item_id),
    INDEX ego_log_char_idx (char_id),
    INDEX ego_log_skill_idx (skill),
    INDEX ego_log_date_idx (reg_date)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 능력 발동 기록';

CREATE TABLE IF NOT EXISTS ego_talk_pack (
    id BIGINT NOT NULL AUTO_INCREMENT,
    genre VARCHAR(30) NOT NULL COMMENT '장르',
    tone VARCHAR(30) NOT NULL DEFAULT '예의' COMMENT '말투',
    keyword VARCHAR(80) NOT NULL DEFAULT '' COMMENT '예비 키워드',
    message VARCHAR(255) NOT NULL COMMENT '대사',
    use_yn TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    mod_date DATETIME NULL DEFAULT NULL COMMENT '수정일',
    PRIMARY KEY (id),
    INDEX ego_talk_pack_idx (genre, tone, use_yn)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 DB 대사팩';

CREATE TABLE IF NOT EXISTS ego_config (
    config_key VARCHAR(80) NOT NULL COMMENT '설정 키',
    config_value VARCHAR(255) NOT NULL DEFAULT '' COMMENT '설정 값',
    memo VARCHAR(255) NOT NULL DEFAULT '' COMMENT '설명',
    use_yn TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    mod_date DATETIME NULL DEFAULT NULL COMMENT '수정일',
    PRIMARY KEY (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 공통 설정';

CREATE TABLE IF NOT EXISTS ego_level (
    ego_lv INT NOT NULL COMMENT '레벨 0~10',
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

CREATE TABLE IF NOT EXISTS ego_weapon_rule (
    type2 VARCHAR(40) NOT NULL COMMENT 'item.type2 원본 값',
    display_name VARCHAR(50) NOT NULL DEFAULT '' COMMENT '표시명',
    default_ability VARCHAR(40) NOT NULL DEFAULT 'EGO_BALANCE' COMMENT '기본 능력',
    allowed_abilities VARCHAR(255) NOT NULL DEFAULT '' COMMENT '허용 능력 콤마 구분',
    use_yn TINYINT(1) NOT NULL DEFAULT 1 COMMENT '에고 생성 허용 여부',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (type2)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 무기 타입/능력 허용 규칙';

-- Legacy fallback tables. Java uses the merged tables first and falls back only for old servers.
CREATE TABLE IF NOT EXISTS ego_bond (
    item_id BIGINT NOT NULL COMMENT '에고무기 아이템 objectId',
    bond INT NOT NULL DEFAULT 0 COMMENT '유대감 0~1000',
    last_reason VARCHAR(40) NOT NULL DEFAULT '' COMMENT '마지막 증가 사유',
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

-- ------------------------------------------------------------
-- Safe migration: old structure cleanup and merged-column patch
-- ------------------------------------------------------------
DROP TABLE IF EXISTS ego_talk;
DROP TABLE IF EXISTS ego_type;
DROP TABLE IF EXISTS ego_view;
DROP TABLE IF EXISTS `에고모양`;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'bond') = 0,
    'ALTER TABLE ego ADD COLUMN bond INT NOT NULL DEFAULT 0 COMMENT ''유대감 0~1000''',
    'SELECT ''ego.bond already exists'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ego' AND COLUMN_NAME = 'bond_reason') = 0,
    'ALTER TABLE ego ADD COLUMN bond_reason VARCHAR(40) NOT NULL DEFAULT '''' COMMENT ''마지막 유대감 사유''',
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

UPDATE ego e
INNER JOIN ego_bond b ON e.item_id = b.item_id
SET e.bond = b.bond, e.bond_reason = b.last_reason, e.mod_date = NOW();

-- ------------------------------------------------------------
-- Default config data
-- ------------------------------------------------------------
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
ON DUPLICATE KEY UPDATE label=VALUES(label), memo=VALUES(memo), base_rate=VALUES(base_rate), lv_rate=VALUES(lv_rate), max_rate=VALUES(max_rate), min_lv=VALUES(min_lv), cool_ms=VALUES(cool_ms), effect=VALUES(effect), use_yn=VALUES(use_yn);

INSERT INTO ego_config (config_key, config_value, memo, use_yn) VALUES
('genre_talk_delay_ms', '1200', '장르대화 연속 입력 방지 딜레이 ms', 1),
('auto_talk_hp_warn_rate', '25', 'HP 자동 위험 대사 기준', 1),
('auto_talk_mp_warn_rate', '15', 'MP 자동 안내 대사 기준', 1),
('attack_ego_exp', '1', '공격 중 획득 경험치', 1),
('attack_exp_delay_ms', '3000', '공격 경험치 획득 딜레이 ms', 1),
('kill_ego_exp', '5', '몬스터 처치 경험치', 1),
('boss_kill_ego_exp', '50', '보스 처치 추가 경험치', 1),
('exp_message_rate', '3', '경험치 메시지 확률', 1),
('proc_message_delay_ms', '1500', '능력 발동 메시지 딜레이 ms', 1),
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
ON DUPLICATE KEY UPDATE need_exp=VALUES(need_exp), proc_bonus=VALUES(proc_bonus), critical_chance=VALUES(critical_chance), critical_damage=VALUES(critical_damage), counter_chance=VALUES(counter_chance), counter_power=VALUES(counter_power), counter_critical=VALUES(counter_critical), memo=VALUES(memo), use_yn=VALUES(use_yn);

INSERT INTO ego_weapon_rule (type2, display_name, default_ability, allowed_abilities, use_yn) VALUES
('dagger','단검','EGO_BALANCE','EGO_BALANCE,BLOOD_DRAIN,MANA_DRAIN,CRITICAL_BURST,GUARDIAN_SHIELD,EXECUTION,FLAME_BRAND,EGO_COUNTER,EGO_REVENGE',1),
('sword','한손검','EGO_BALANCE','EGO_BALANCE,BLOOD_DRAIN,MANA_DRAIN,CRITICAL_BURST,GUARDIAN_SHIELD,EXECUTION,FLAME_BRAND,EGO_COUNTER,EGO_REVENGE',1),
('tohandsword','양손검','CRITICAL_BURST','EGO_BALANCE,BLOOD_DRAIN,CRITICAL_BURST,GUARDIAN_SHIELD,AREA_SLASH,EXECUTION,FLAME_BRAND,EGO_COUNTER,EGO_REVENGE',1),
('axe','도끼','CRITICAL_BURST','EGO_BALANCE,BLOOD_DRAIN,CRITICAL_BURST,GUARDIAN_SHIELD,AREA_SLASH,EXECUTION,FLAME_BRAND,EGO_COUNTER,EGO_REVENGE',1),
('spear','창','AREA_SLASH','EGO_BALANCE,BLOOD_DRAIN,CRITICAL_BURST,GUARDIAN_SHIELD,AREA_SLASH,FLAME_BRAND,FROST_BIND,EGO_COUNTER,EGO_REVENGE',1),
('bow','활','EGO_BALANCE','EGO_BALANCE,CRITICAL_BURST,GUARDIAN_SHIELD,FROST_BIND,EGO_COUNTER,EGO_REVENGE',1),
('staff','지팡이','MANA_DRAIN','EGO_BALANCE,MANA_DRAIN,GUARDIAN_SHIELD,FLAME_BRAND,FROST_BIND,EGO_COUNTER,EGO_REVENGE',1),
('wand','완드','MANA_DRAIN','EGO_BALANCE,MANA_DRAIN,GUARDIAN_SHIELD,FLAME_BRAND,FROST_BIND,EGO_COUNTER,EGO_REVENGE',1),
('fishing_rod','낚싯대','EGO_BALANCE','',0)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), default_ability=VALUES(default_ability), allowed_abilities=VALUES(allowed_abilities), use_yn=VALUES(use_yn);

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
('아무','예의반대','','대사는 내가 해줄 테니 전투는 네가 해.',1);

-- ------------------------------------------------------------
-- Data normalization
-- ------------------------------------------------------------
UPDATE ego SET ego_type='예의' WHERE ego_type IS NULL OR ego_type='' OR ego_type NOT IN ('예의','예의반대');
UPDATE ego SET ego_lv=0 WHERE ego_lv < 0;
UPDATE ego SET ego_lv=10 WHERE ego_lv > 10;
UPDATE ego e INNER JOIN ego_level l ON e.ego_lv=l.ego_lv SET e.need_exp=l.need_exp WHERE l.use_yn=1;
UPDATE ego SET need_exp=0, ego_exp=0 WHERE ego_lv >= 10;

SELECT 'EGO_SCHEMA_SQL_OK' AS result;
SHOW TABLES LIKE 'ego%';
SELECT * FROM ego_level ORDER BY ego_lv;
SELECT * FROM ego_config ORDER BY config_key;
SELECT * FROM ego_weapon_rule ORDER BY type2;
