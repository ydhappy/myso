-- ============================================================
-- 에고무기 특별 능력 SQL 확장
-- 선행 권장: ego/sql/ego_weapon.sql 적용
-- ============================================================

-- 1. 에고 능력 마스터 테이블
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

-- 2. 아이템별 에고 능력 연결 테이블
-- character_item_ego와 분리하여, 한 무기에 여러 능력을 부여할 수 있게 설계합니다.
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

-- 3. 발동 로그 테이블
-- 운영 중 밸런스 확인용. 로그가 너무 많으면 비활성화하거나 주기적으로 정리하세요.
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

-- 4. 테스트용 예시
-- 실제 item_objid는 서버 DB의 아이템 objectId로 바꿔야 합니다.
-- INSERT INTO character_item_ego_ability (item_objid, ability_type, ability_level, enabled)
-- VALUES (123456789, 'BLOOD_DRAIN', 1, 1)
-- ON DUPLICATE KEY UPDATE ability_level=1, enabled=1;

-- 5. 추천 능력 분류
-- 한손검: EGO_BALANCE, FLAME_BRAND
-- 양손검/도끼: CRITICAL_BURST, EXECUTION
-- 창: AREA_SLASH
-- 지팡이/완드: MANA_DRAIN, FROST_BIND
-- 수호/방어 컨셉 무기: GUARDIAN_SHIELD
-- 흡혈 컨셉 무기: BLOOD_DRAIN
