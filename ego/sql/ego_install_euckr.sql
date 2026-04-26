-- ============================================================
-- 에고무기 설치 SQL - EUC-KR 안전 통합 버전
-- 파일 인코딩: UTF-8
-- DB/테이블 문자셋: euckr
-- MySQL 구버전 호환: 한 테이블에 CURRENT_TIMESTAMP 자동 컬럼 1개만 사용
-- 포함:
--   ego / ego_skill / ego_skill_base / ego_log
--   ego_bond / ego_talk_pack
--   ego_config / ego_level_exp / ego_level_bonus / ego_weapon_rule
-- ============================================================

SET NAMES utf8;

-- ============================================================
-- 1. 에고 기본 정보
-- ============================================================
CREATE TABLE IF NOT EXISTS ego (
    item_id BIGINT NOT NULL COMMENT '에고무기 아이템 objectId',
    char_id BIGINT NOT NULL DEFAULT 0 COMMENT '소유 캐릭터 objectId',
    use_yn TINYINT(1) NOT NULL DEFAULT 0 COMMENT '사용 여부',
    ego_name VARCHAR(50) NOT NULL DEFAULT '에고' COMMENT '호출 이름',
    ego_type VARCHAR(30) NOT NULL DEFAULT '예의' COMMENT '실시간 대화 말투: 예의/예의반대',
    ego_lv INT NOT NULL DEFAULT 0 COMMENT '에고 레벨 0~10, 0은 전투능력 없음',
    ego_exp BIGINT NOT NULL DEFAULT 0 COMMENT '현재 경험치',
    need_exp BIGINT NOT NULL DEFAULT 100 COMMENT '다음 레벨 필요 경험치',
    talk_lv INT NOT NULL DEFAULT 1 COMMENT '대화 단계',
    ctrl_lv INT NOT NULL DEFAULT 1 COMMENT '제어 단계',
    last_talk BIGINT NOT NULL DEFAULT 0 COMMENT '마지막 대화 시간',
    last_warn BIGINT NOT NULL DEFAULT 0 COMMENT '마지막 경고 시간',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    mod_date DATETIME NULL DEFAULT NULL COMMENT '수정일',
    PRIMARY KEY (item_id),
    INDEX ego_char_idx (char_id),
    INDEX ego_name_idx (ego_name),
    INDEX ego_lv_idx (ego_lv)
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
    cool_ms INT NOT NULL DEFAULT 0 COMMENT '능력별 쿨타임 ms',
    effect INT NOT NULL DEFAULT 0 COMMENT 'S_ObjectEffect 이펙트 번호',
    use_yn TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    PRIMARY KEY (skill)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 능력 기본값';

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

-- ============================================================
-- 2. 유대감 / 대사팩
-- ============================================================
CREATE TABLE IF NOT EXISTS ego_bond (
    item_id BIGINT NOT NULL COMMENT '에고무기 아이템 objectId',
    bond INT NOT NULL DEFAULT 0 COMMENT '유대감 0~1000',
    last_reason VARCHAR(40) NOT NULL DEFAULT '' COMMENT '마지막 증가 사유',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일',
    mod_date DATETIME NULL DEFAULT NULL COMMENT '수정일',
    PRIMARY KEY (item_id),
    INDEX ego_bond_idx (bond)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 유대감';

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

-- ============================================================
-- 3. 공통 설정 / 경험치 / 전투 보너스 / 무기 규칙
-- ============================================================
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
('auto_talk_idle_delay_ms', '180000', '안정 상태 자동 대사 재출력 딜레이 ms', 1),
('attack_ego_exp', '1', '공격 중 주기적으로 획득하는 에고 경험치', 1),
('attack_exp_delay_ms', '3000', '공격 경험치 획득 딜레이 ms', 1),
('kill_ego_exp', '5', '몬스터 처치 시 에고 경험치', 1),
('boss_kill_ego_exp', '50', '보스 처치 시 추가 에고 경험치', 1),
('exp_message_rate', '3', '일반 경험치 획득 메시지 출력 확률', 1),
('proc_message_delay_ms', '1500', '능력 발동 메시지 출력 딜레이 ms', 1),
('counter_unlock_level', '5', '피격 반격 해금 레벨', 1),
('auto_counter_unlock_level', '6', '자동반격 해금 레벨', 1),
('auto_counter_cool_ms', '2500', '자동반격 쿨타임 ms', 1),
('auto_counter_chance', '100', '자동반격 발동 확률', 1),
('stun_level', '10', '에고 스턴 해금 레벨', 1),
('stun_success_rate', '50', '에고 스턴 성공 확률', 1),
('stun_time', '2', '에고 스턴 시간 초', 1),
('stun_effect', '4183', '에고 스턴 이펙트 번호', 1),
('stun_cool_ms', '6000', '에고 스턴 쿨타임 ms', 1),
('guardian_shield_hp_rate', '40', '수호 의지 발동 HP 기준 이하', 1),
('execution_target_hp_rate', '20', '처형 발동 대상 HP 기준 이하', 1),
('area_range', '2', '광역 능력 범위', 1),
('area_max_target', '4', '광역 능력 최대 대상 수', 1)
ON DUPLICATE KEY UPDATE memo = VALUES(memo), use_yn = VALUES(use_yn);

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
ON DUPLICATE KEY UPDATE memo = VALUES(memo), use_yn = VALUES(use_yn);

CREATE TABLE IF NOT EXISTS ego_level_bonus (
    ego_lv INT NOT NULL COMMENT '에고 레벨 0~10',
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
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 레벨별 전투 보너스';

INSERT INTO ego_level_bonus
(ego_lv, proc_bonus, critical_chance, critical_damage, counter_chance, counter_power, counter_critical, memo, use_yn)
VALUES
(0, 0, 0, 0, 0, 0, 0, 'Lv.0 전투능력 없음', 1),
(1, 0, 1, 1, 0, 0, 0, 'Lv.1 기본 치명 시작', 1),
(2, 1, 2, 2, 0, 0, 0, 'Lv.2', 1),
(3, 2, 3, 3, 0, 0, 0, 'Lv.3', 1),
(4, 3, 4, 4, 0, 0, 0, 'Lv.4', 1),
(5, 4, 6, 6, 35, 18, 8, 'Lv.5 피격 반격 시작', 1),
(6, 6, 9, 8, 100, 24, 12, 'Lv.6 자동반격 시작', 1),
(7, 8, 12, 10, 100, 30, 16, 'Lv.7', 1),
(8, 10, 15, 12, 100, 38, 20, 'Lv.8', 1),
(9, 12, 18, 15, 100, 46, 25, 'Lv.9', 1),
(10, 15, 25, 20, 100, 60, 35, 'Lv.10 스턴 연동', 1)
ON DUPLICATE KEY UPDATE memo = VALUES(memo), use_yn = VALUES(use_yn);

CREATE TABLE IF NOT EXISTS ego_weapon_rule (
    type2 VARCHAR(40) NOT NULL COMMENT 'item.type2 원본 값',
    display_name VARCHAR(50) NOT NULL DEFAULT '' COMMENT '표시명',
    default_ability VARCHAR(40) NOT NULL DEFAULT 'EGO_BALANCE' COMMENT '기본 에고 능력',
    allowed_abilities VARCHAR(255) NOT NULL DEFAULT '' COMMENT '허용 능력 콤마 구분',
    use_yn TINYINT(1) NOT NULL DEFAULT 1 COMMENT '에고 생성 허용 여부',
    reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mod_date DATETIME NULL DEFAULT NULL,
    PRIMARY KEY (type2)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 무기 타입/능력 허용 규칙';

INSERT INTO ego_weapon_rule
(type2, display_name, default_ability, allowed_abilities, use_yn)
VALUES
('dagger', '단검', 'EGO_BALANCE', 'EGO_BALANCE,BLOOD_DRAIN,MANA_DRAIN,CRITICAL_BURST,GUARDIAN_SHIELD,EXECUTION,FLAME_BRAND,EGO_COUNTER,EGO_REVENGE', 1),
('sword', '한손검', 'EGO_BALANCE', 'EGO_BALANCE,BLOOD_DRAIN,MANA_DRAIN,CRITICAL_BURST,GUARDIAN_SHIELD,EXECUTION,FLAME_BRAND,EGO_COUNTER,EGO_REVENGE', 1),
('tohandsword', '양손검', 'CRITICAL_BURST', 'EGO_BALANCE,BLOOD_DRAIN,CRITICAL_BURST,GUARDIAN_SHIELD,AREA_SLASH,EXECUTION,FLAME_BRAND,EGO_COUNTER,EGO_REVENGE', 1),
('axe', '도끼', 'CRITICAL_BURST', 'EGO_BALANCE,BLOOD_DRAIN,CRITICAL_BURST,GUARDIAN_SHIELD,AREA_SLASH,EXECUTION,FLAME_BRAND,EGO_COUNTER,EGO_REVENGE', 1),
('spear', '창', 'AREA_SLASH', 'EGO_BALANCE,BLOOD_DRAIN,CRITICAL_BURST,GUARDIAN_SHIELD,AREA_SLASH,FLAME_BRAND,FROST_BIND,EGO_COUNTER,EGO_REVENGE', 1),
('bow', '활', 'EGO_BALANCE', 'EGO_BALANCE,CRITICAL_BURST,GUARDIAN_SHIELD,FROST_BIND,EGO_COUNTER,EGO_REVENGE', 1),
('staff', '지팡이', 'MANA_DRAIN', 'EGO_BALANCE,MANA_DRAIN,GUARDIAN_SHIELD,FLAME_BRAND,FROST_BIND,EGO_COUNTER,EGO_REVENGE', 1),
('wand', '완드', 'MANA_DRAIN', 'EGO_BALANCE,MANA_DRAIN,GUARDIAN_SHIELD,FLAME_BRAND,FROST_BIND,EGO_COUNTER,EGO_REVENGE', 1),
('fishing_rod', '낚싯대', 'EGO_BALANCE', '', 0)
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    default_ability = VALUES(default_ability),
    allowed_abilities = VALUES(allowed_abilities),
    use_yn = VALUES(use_yn);

UPDATE ego e
INNER JOIN ego_level_exp x ON e.ego_lv = x.ego_lv
SET e.need_exp = x.need_exp
WHERE x.use_yn = 1;

UPDATE ego SET need_exp = 0, ego_exp = 0 WHERE ego_lv >= 10;

SELECT 'EGO_INSTALL_OK_EUCKR_FULL_DBIZED' AS result;
SHOW TABLES LIKE 'ego';
SHOW TABLES LIKE 'ego_skill';
SHOW TABLES LIKE 'ego_skill_base';
SHOW TABLES LIKE 'ego_log';
SHOW TABLES LIKE 'ego_bond';
SHOW TABLES LIKE 'ego_talk_pack';
SHOW TABLES LIKE 'ego_config';
SHOW TABLES LIKE 'ego_level_exp';
SHOW TABLES LIKE 'ego_level_bonus';
SHOW TABLES LIKE 'ego_weapon_rule';
