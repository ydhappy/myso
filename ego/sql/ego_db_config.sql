-- ============================================================
-- 에고 기능 DB화 보강 SQL - 병합 구조 최종 버전
-- 대상: 신규/기존 서버 공통
-- 목적:
--   1) Java 상수성 설정값을 ego_config로 관리
--   2) 레벨별 필요 경험치 + 전투 보너스를 ego_level로 통합 관리
--   3) 무기 타입/능력 허용 규칙을 ego_weapon_rule로 관리
--   4) .에고리로드 만으로 설정/레벨/무기규칙 즉시 반영
-- 파일 인코딩: UTF-8
-- DB 문자셋: euckr
-- ============================================================

SET NAMES utf8;

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
ON DUPLICATE KEY UPDATE
    memo = VALUES(memo),
    use_yn = VALUES(use_yn);

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
INNER JOIN ego_level l ON e.ego_lv = l.ego_lv
SET e.need_exp = l.need_exp
WHERE l.use_yn = 1;

UPDATE ego SET need_exp = 0, ego_exp = 0 WHERE ego_lv >= 10;

SELECT 'EGO_DB_CONFIG_MERGED_SCHEMA_OK' AS result;
SELECT * FROM ego_config ORDER BY config_key;
SELECT * FROM ego_level ORDER BY ego_lv;
SELECT * FROM ego_weapon_rule ORDER BY type2;
SELECT ego_lv, need_exp, COUNT(*) AS count
FROM ego
GROUP BY ego_lv, need_exp
ORDER BY ego_lv;
