-- ============================================================
-- 에고무기 설치 SQL - EUC-KR 안전 버전
-- 파일 인코딩: UTF-8
-- DB/테이블 문자셋: euckr
-- MySQL 구버전 호환: 한 테이블에 CURRENT_TIMESTAMP 자동 컬럼 1개만 사용
-- ============================================================

SET NAMES utf8;

CREATE TABLE IF NOT EXISTS ego (
    item_id BIGINT NOT NULL COMMENT '에고무기 아이템 objectId',
    char_id BIGINT NOT NULL DEFAULT 0 COMMENT '소유 캐릭터 objectId',
    use_yn TINYINT(1) NOT NULL DEFAULT 0 COMMENT '사용 여부',
    ego_name VARCHAR(50) NOT NULL DEFAULT '에고' COMMENT '호출 이름',
    ego_type VARCHAR(30) NOT NULL DEFAULT '수호' COMMENT '성격/분류',
    ego_lv INT NOT NULL DEFAULT 1 COMMENT '에고 레벨',
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

CREATE TABLE IF NOT EXISTS ego_view (
    form VARCHAR(40) NOT NULL COMMENT '원본 type2: dagger/sword/tohandsword/axe/spear/bow/staff/wand',
    label VARCHAR(50) NOT NULL DEFAULT '' COMMENT '표시 이름',
    inv_gfx INT NOT NULL DEFAULT 0 COMMENT '인벤토리 아이콘 gfx, 0이면 원본 사용',
    ground_gfx INT NOT NULL DEFAULT 0 COMMENT '바닥 드랍 gfx, 0이면 원본 사용',
    memo VARCHAR(255) NOT NULL DEFAULT '' COMMENT '아이템정보 보조 설명',
    use_yn TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    PRIMARY KEY (form)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 원본 무기타입별 인벤토리/바닥 표시';

INSERT INTO ego_view
(form, label, inv_gfx, ground_gfx, memo, use_yn)
VALUES
('dagger', '단검', 0, 0, '에고 단검 원본 타입', 1),
('sword', '한손검', 0, 0, '에고 한손검 원본 타입', 1),
('tohandsword', '양손검', 0, 0, '에고 양손검 원본 타입', 1),
('axe', '도끼', 0, 0, '에고 도끼 원본 타입', 1),
('spear', '창', 0, 0, '에고 창 원본 타입', 1),
('bow', '활', 0, 0, '에고 활 원본 타입', 1),
('staff', '지팡이', 0, 0, '에고 지팡이 원본 타입', 1),
('wand', '완드', 0, 0, '에고 완드 원본 타입', 1)
ON DUPLICATE KEY UPDATE
    label = VALUES(label),
    memo = VALUES(memo),
    use_yn = VALUES(use_yn);

CREATE TABLE IF NOT EXISTS ego_skill_base (
    skill VARCHAR(40) NOT NULL COMMENT '능력 코드',
    label VARCHAR(50) NOT NULL COMMENT '표시명',
    memo VARCHAR(255) NOT NULL DEFAULT '',
    base_rate INT NOT NULL DEFAULT 3 COMMENT '기본 발동률',
    lv_rate INT NOT NULL DEFAULT 1 COMMENT '레벨당 발동률',
    max_rate INT NOT NULL DEFAULT 25 COMMENT '최대 발동률',
    min_lv INT NOT NULL DEFAULT 1 COMMENT '최소 에고 실질 레벨',
    cool_ms INT NOT NULL DEFAULT 0 COMMENT '능력별 쿨타임 ms',
    effect INT NOT NULL DEFAULT 0 COMMENT 'S_ObjectEffect 이펙트 번호',
    use_yn TINYINT(1) NOT NULL DEFAULT 1 COMMENT '사용 여부',
    PRIMARY KEY (skill)
) ENGINE=InnoDB DEFAULT CHARSET=euckr COLLATE=euckr_korean_ci COMMENT='에고 능력 기본값';

INSERT INTO ego_skill_base
(skill, label, memo, base_rate, lv_rate, max_rate, min_lv, cool_ms, effect, use_yn)
VALUES
('EGO_BALANCE', '공명', '균형형 추가 피해', 3, 1, 25, 1, 0, 3940, 1),
('BLOOD_DRAIN', '흡혈', 'HP 회복', 3, 1, 20, 1, 0, 8150, 1),
('MANA_DRAIN', '흡마', 'MP 회복', 3, 1, 20, 1, 0, 7300, 1),
('CRITICAL_BURST', '치명', '강한 추가 피해', 2, 1, 20, 3, 0, 12487, 1),
('GUARDIAN_SHIELD', '수호', '위험 시 HP 회복', 3, 1, 20, 2, 3000, 6321, 1),
('AREA_SLASH', '광역', '주변 몬스터 피해', 2, 1, 15, 5, 3000, 12248, 1),
('EXECUTION', '처형', '약한 대상 추가 피해', 2, 1, 15, 7, 0, 8683, 1),
('FLAME_BRAND', '화염', '화염 추가 피해', 3, 1, 20, 1, 0, 1811, 1),
('FROST_BIND', '서리', '서리 추가 피해', 3, 1, 20, 1, 0, 3684, 1),
('EGO_COUNTER', '반격', 'Lv.10 해금 피격 반격', 3, 1, 20, 10, 3000, 10710, 1),
('EGO_REVENGE', '복수', 'Lv.20 해금 저체력 피격 특수 반격', 2, 1, 15, 20, 8000, 6321, 1)
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

SELECT 'EGO_INSTALL_OK_EUCKR_CLEAN' AS result;
SHOW TABLES LIKE 'ego';
SHOW TABLES LIKE 'ego_skill';
SHOW TABLES LIKE 'ego_view';
SHOW TABLES LIKE 'ego_skill_base';
SHOW TABLES LIKE 'ego_log';
