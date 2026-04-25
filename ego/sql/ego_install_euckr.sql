-- ============================================================
-- 에고무기 설치 SQL - EUC-KR 안전 버전
-- 파일 인코딩: UTF-8
-- DB/테이블 문자셋: euckr
-- MySQL 구버전 호환: 한 테이블에 CURRENT_TIMESTAMP 자동 컬럼 1개만 사용
-- ============================================================

SET NAMES utf8;

-- ============================================================
-- 에고 레벨별 필요 경험치
-- Lv.0 -> Lv.1  : 100
-- Lv.1 -> Lv.2  : 250
-- Lv.2 -> Lv.3  : 500
-- Lv.3 -> Lv.4  : 900
-- Lv.4 -> Lv.5  : 1500  -- 피격 반격 개방
-- Lv.5 -> Lv.6  : 2400  -- 자동반격 개방
-- Lv.6 -> Lv.7  : 3600
-- Lv.7 -> Lv.8  : 5200
-- Lv.8 -> Lv.9  : 7500
-- Lv.9 -> Lv.10 : 10000 -- 스턴 50% 개방
-- Lv.10          : 0     -- 만렙
-- ============================================================

CREATE TABLE IF NOT EXISTS ego (
    item_id BIGINT NOT NULL COMMENT '에고무기 아이템 objectId',
    char_id BIGINT NOT NULL DEFAULT 0 COMMENT '소유 캐릭터 objectId',
    use_yn TINYINT(1) NOT NULL DEFAULT 0 COMMENT '사용 여부',
    ego_name VARCHAR(50) NOT NULL DEFAULT '에고' COMMENT '호출 이름',
    ego_type VARCHAR(30) NOT NULL DEFAULT '수호' COMMENT '성격/분류',
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

SELECT 'EGO_INSTALL_OK_EUCKR_EXP_CURVE_0_10' AS result;
SHOW TABLES LIKE 'ego';
SHOW TABLES LIKE 'ego_skill';
SHOW TABLES LIKE 'ego_skill_base';
SHOW TABLES LIKE 'ego_log';
