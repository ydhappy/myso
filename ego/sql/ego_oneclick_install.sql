-- ============================================================
-- 에고무기 원클릭 통합 설치 SQL
-- Java 8 서버 / MySQL 또는 MariaDB 기준
-- 적용 전 반드시 DB 백업 권장
-- ============================================================

SET NAMES utf8;

-- ============================================================
-- 1. 에고무기 기본 정보
-- ============================================================
CREATE TABLE IF NOT EXISTS character_item_ego (
    item_objid BIGINT NOT NULL COMMENT '아이템 고유 objectId',
    cha_objid BIGINT NOT NULL DEFAULT 0 COMMENT '소유 캐릭터 objID',
    ego_enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '에고 활성화 여부',
    ego_name VARCHAR(50) NOT NULL DEFAULT '에고' COMMENT '에고 호출 이름',
    ego_personality VARCHAR(30) NOT NULL DEFAULT 'guardian' COMMENT '성격: guardian/berserker/calm/sage',
    ego_level INT NOT NULL DEFAULT 1 COMMENT '에고 레벨',
    ego_exp BIGINT NOT NULL DEFAULT 0 COMMENT '에고 경험치',
    ego_max_exp BIGINT NOT NULL DEFAULT 100 COMMENT '다음 레벨 필요 경험치',
    ego_talk_level INT NOT NULL DEFAULT 1 COMMENT '대화 지능 단계',
    ego_control_level INT NOT NULL DEFAULT 1 COMMENT '제어 가능 단계',
    ego_last_talk_time BIGINT NOT NULL DEFAULT 0 COMMENT '마지막 대화 시간',
    ego_last_warning_time BIGINT NOT NULL DEFAULT 0 COMMENT '마지막 자동 경고 시간',
    ego_form_type VARCHAR(40) NOT NULL DEFAULT '' COMMENT '에고 현재 변신 형태: sword/bow/tohandsword 등',
    prev_shield_objid BIGINT NOT NULL DEFAULT 0 COMMENT '형태변신 때문에 해제한 방패 objectId',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (item_objid),
    INDEX idx_character_item_ego_cha_objid (cha_objid),
    INDEX idx_character_item_ego_name (ego_name),
    INDEX idx_character_item_ego_form (ego_form_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='에고무기 개별 성장/대화/형태변환 정보';

-- 기존 설치 DB에 컬럼이 없을 때만 자동 추가한다.
SET @db_name := DATABASE();
SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=@db_name AND TABLE_NAME='character_item_ego' AND COLUMN_NAME='ego_form_type') = 0,
    'ALTER TABLE character_item_ego ADD COLUMN ego_form_type VARCHAR(40) NOT NULL DEFAULT '''' COMMENT ''에고 현재 변신 형태: sword/bow/tohandsword 등'' AFTER ego_last_warning_time',
    'SELECT ''ego_form_type column exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=@db_name AND TABLE_NAME='character_item_ego' AND COLUMN_NAME='prev_shield_objid') = 0,
    'ALTER TABLE character_item_ego ADD COLUMN prev_shield_objid BIGINT NOT NULL DEFAULT 0 COMMENT ''형태변신 때문에 해제한 방패 objectId'' AFTER ego_form_type',
    'SELECT ''prev_shield_objid column exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 인덱스는 중복 생성 시 실패할 수 있으므로 존재하지 않을 때만 생성한다.
SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_SCHEMA=@db_name AND TABLE_NAME='character_item_ego' AND INDEX_NAME='idx_character_item_ego_form') = 0,
    'ALTER TABLE character_item_ego ADD INDEX idx_character_item_ego_form (ego_form_type)',
    'SELECT ''idx_character_item_ego_form index exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 2. 에고 성격 템플릿
-- ============================================================
CREATE TABLE IF NOT EXISTS ego_personality_template (
    personality VARCHAR(30) NOT NULL COMMENT '성격 코드',
    display_name VARCHAR(50) NOT NULL COMMENT '표시명',
    description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '설명',
    hp_danger_rate INT NOT NULL DEFAULT 30 COMMENT '위험 HP 기준',
    allow_auto_attack TINYINT(1) NOT NULL DEFAULT 1 COMMENT '공격 제어 허용',
    allow_auto_warning TINYINT(1) NOT NULL DEFAULT 1 COMMENT '자동 경고 허용',
    PRIMARY KEY (personality)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='에고 성격 템플릿';

INSERT INTO ego_personality_template
(personality, display_name, description, hp_danger_rate, allow_auto_attack, allow_auto_warning)
VALUES
('guardian', '수호자형', '체력과 생존을 우선하는 안정형 에고', 35, 1, 1),
('berserker', '광전사형', '공격을 선호하지만 낮은 체력에서는 제어를 제한하는 전투형 에고', 25, 1, 1),
('calm', '냉정형', '짧고 정확하게 위험만 알려주는 침착형 에고', 30, 1, 1),
('sage', '현자형', '상태 분석과 조언을 자세히 제공하는 분석형 에고', 40, 1, 1)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    description = VALUES(description),
    hp_danger_rate = VALUES(hp_danger_rate),
    allow_auto_attack = VALUES(allow_auto_attack),
    allow_auto_warning = VALUES(allow_auto_warning);

-- ============================================================
-- 3. 에고 대화 템플릿
-- ============================================================
CREATE TABLE IF NOT EXISTS ego_talk_template (
    uid INT NOT NULL AUTO_INCREMENT,
    personality VARCHAR(30) NOT NULL DEFAULT 'guardian',
    keyword VARCHAR(100) NOT NULL COMMENT '반응 키워드',
    ment TEXT NOT NULL COMMENT '멘트',
    min_ego_level INT NOT NULL DEFAULT 1,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (uid),
    INDEX idx_ego_talk_template_personality (personality),
    INDEX idx_ego_talk_template_keyword (keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='에고 대화 템플릿';

INSERT INTO ego_talk_template
(personality, keyword, ment, min_ego_level, enabled)
VALUES
('guardian', '상태', '\\fY[에고] 주인님의 상태를 확인했습니다.', 1, 1),
('guardian', '위험', '\\fR[에고] 위험합니다. 회복과 후퇴를 우선하십시오.', 1, 1),
('guardian', '활', '\\fY[에고] 활 형태로 변신합니다.', 1, 1),
('guardian', '양검', '\\fY[에고] 양손검 형태로 변신합니다.', 1, 1),
('guardian', '한검', '\\fY[에고] 한손검 형태로 변신합니다.', 1, 1),
('berserker', '공격', '\\fY[에고] 검이 굶주렸습니다. 적을 베겠습니다.', 1, 1),
('calm', '선공', '\\fY[에고] 선공 몬스터 감지.', 1, 1),
('sage', '조언', '\\fY[에고] 현재 전투 상황을 분석했습니다.', 1, 1)
ON DUPLICATE KEY UPDATE
    personality = VALUES(personality),
    keyword = VALUES(keyword),
    ment = VALUES(ment),
    min_ego_level = VALUES(min_ego_level),
    enabled = VALUES(enabled);

-- ============================================================
-- 4. 에고 능력 템플릿
-- ============================================================
CREATE TABLE IF NOT EXISTS ego_ability_template (
    ability_type VARCHAR(40) NOT NULL COMMENT '능력 코드',
    display_name VARCHAR(50) NOT NULL COMMENT '표시명',
    description VARCHAR(255) NOT NULL DEFAULT '' COMMENT '설명',
    base_proc_chance INT NOT NULL DEFAULT 3 COMMENT '기본 발동 확률',
    add_proc_chance_per_level INT NOT NULL DEFAULT 1 COMMENT '레벨당 추가 확률',
    max_proc_chance INT NOT NULL DEFAULT 25 COMMENT '발동 확률 상한',
    min_ego_level INT NOT NULL DEFAULT 1 COMMENT '최소 에고 레벨',
    cooldown_ms INT NOT NULL DEFAULT 0 COMMENT '능력별 쿨타임 ms',
    effect_id INT NOT NULL DEFAULT 0 COMMENT '기본 이펙트 ID',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    PRIMARY KEY (ability_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='에고무기 특별 능력 템플릿';

INSERT INTO ego_ability_template
(ability_type, display_name, description, base_proc_chance, add_proc_chance_per_level, max_proc_chance, min_ego_level, cooldown_ms, effect_id, enabled)
VALUES
('EGO_BALANCE', '공명 타격', '균형형 에고 능력. 소량 추가 피해를 준다.', 3, 1, 25, 1, 0, 3940, 1),
('BLOOD_DRAIN', '생명 흡수', '피해 일부만큼 HP를 회복한다.', 3, 1, 20, 1, 0, 8150, 1),
('MANA_DRAIN', '정신 흡수', '공격 시 MP를 회복한다.', 3, 1, 20, 1, 0, 7300, 1),
('CRITICAL_BURST', '치명 폭발', '강한 추가 피해를 준다.', 2, 1, 20, 3, 0, 12487, 1),
('GUARDIAN_SHIELD', '수호 의지', '체력이 낮을 때 HP를 회복한다.', 3, 1, 20, 2, 3000, 6321, 1),
('AREA_SLASH', '공명 베기', '주변 몬스터에게 광역 피해를 준다.', 2, 1, 15, 5, 3000, 12248, 1),
('EXECUTION', '처형', 'HP가 낮은 적에게 추가 피해를 준다.', 2, 1, 15, 7, 0, 8683, 1),
('FLAME_BRAND', '화염 각인', '화염 이펙트와 함께 추가 피해를 준다.', 3, 1, 20, 1, 0, 1811, 1),
('FROST_BIND', '서리 충격', '서리 이펙트와 함께 소량 추가 피해를 준다.', 3, 1, 20, 1, 0, 3684, 1)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    description = VALUES(description),
    base_proc_chance = VALUES(base_proc_chance),
    add_proc_chance_per_level = VALUES(add_proc_chance_per_level),
    max_proc_chance = VALUES(max_proc_chance),
    min_ego_level = VALUES(min_ego_level),
    cooldown_ms = VALUES(cooldown_ms),
    effect_id = VALUES(effect_id),
    enabled = VALUES(enabled);

-- ============================================================
-- 5. 아이템별 에고 능력
-- ============================================================
CREATE TABLE IF NOT EXISTS character_item_ego_ability (
    uid BIGINT NOT NULL AUTO_INCREMENT,
    item_objid BIGINT NOT NULL COMMENT '아이템 고유 objectId',
    ability_type VARCHAR(40) NOT NULL COMMENT '능력 코드',
    ability_level INT NOT NULL DEFAULT 1 COMMENT '능력 레벨',
    proc_chance_bonus INT NOT NULL DEFAULT 0 COMMENT '추가 발동 확률',
    damage_bonus INT NOT NULL DEFAULT 0 COMMENT '추가 피해 보정',
    last_proc_time BIGINT NOT NULL DEFAULT 0 COMMENT '마지막 발동 시간',
    enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (uid),
    UNIQUE KEY uk_ego_ability_item_type (item_objid, ability_type),
    INDEX idx_ego_ability_item_objid (item_objid),
    INDEX idx_ego_ability_type (ability_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='아이템별 에고 특별 능력';

-- ============================================================
-- 6. 발동 로그
-- ============================================================
CREATE TABLE IF NOT EXISTS ego_ability_proc_log (
    uid BIGINT NOT NULL AUTO_INCREMENT,
    item_objid BIGINT NOT NULL DEFAULT 0,
    cha_objid BIGINT NOT NULL DEFAULT 0,
    cha_name VARCHAR(50) NOT NULL DEFAULT '',
    target_name VARCHAR(80) NOT NULL DEFAULT '',
    ability_type VARCHAR(40) NOT NULL DEFAULT '',
    base_damage INT NOT NULL DEFAULT 0,
    final_damage INT NOT NULL DEFAULT 0,
    add_damage INT NOT NULL DEFAULT 0,
    proc_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uid),
    INDEX idx_ego_proc_log_item (item_objid),
    INDEX idx_ego_proc_log_cha (cha_objid),
    INDEX idx_ego_proc_log_time (proc_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='에고 능력 발동 로그';

-- ============================================================
-- 7. 설치 검증
-- ============================================================
SELECT 'EGO_INSTALL_OK' AS result;

SHOW TABLES LIKE 'character_item_ego';
SHOW TABLES LIKE 'character_item_ego_ability';
SHOW TABLES LIKE 'ego_personality_template';
SHOW TABLES LIKE 'ego_talk_template';
SHOW TABLES LIKE 'ego_ability_template';
SHOW TABLES LIKE 'ego_ability_proc_log';

SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'character_item_ego'
  AND COLUMN_NAME IN ('ego_form_type', 'prev_shield_objid');
