-- ============================================================
-- 에고무기 시스템 SQL
-- 대상 DB: myso lineage DB
-- 적용 전 반드시 DB 백업을 권장합니다.
-- ============================================================

-- 1. 아이템 개별 에고 정보 저장 테이블
-- characters_inventory 원본 테이블을 직접 크게 변경하지 않기 위한 별도 테이블입니다.
-- item_objid는 ItemInstance.objectId 또는 characters_inventory.item_objid에 대응시키는 용도입니다.

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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (item_objid),
    INDEX idx_character_item_ego_cha_objid (cha_objid),
    INDEX idx_character_item_ego_name (ego_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='에고무기 개별 성장/대화 정보';

-- 2. 에고 성격 기본값 테이블
-- 자바에서 하드코딩하지 않고 DB로 말투/정책을 바꾸고 싶을 때 사용합니다.

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

-- 3. 에고 명령어/멘트 확장 테이블
-- 1차 자바 파일은 규칙형 하드코딩이지만, 이후 DB 멘트화할 때 사용합니다.

CREATE TABLE IF NOT EXISTS ego_talk_template (
    uid INT NOT NULL AUTO_INCREMENT,
    personality VARCHAR(30) NOT NULL DEFAULT 'guardian',
    keyword VARCHAR(100) NOT NULL COMMENT '반응 키워드',
    ment TEXT NOT NULL COMMENT '멘트. ] 구분으로 랜덤 후보 확장 가능',
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
('berserker', '공격', '\\fY[에고] 검이 굶주렸습니다. 적을 베겠습니다.', 1, 1),
('calm', '선공', '\\fY[에고] 선공 몬스터 감지.', 1, 1),
('sage', '조언', '\\fY[에고] 현재 전투 상황을 분석했습니다.', 1, 1);

-- 4. 빠른 테스트용 예시
-- 실제 item_objid와 cha_objid는 서버 DB의 값에 맞게 수정해야 합니다.
-- INSERT INTO character_item_ego (item_objid, cha_objid, ego_enabled, ego_name, ego_personality)
-- VALUES (123456789, 100001, 1, '카르마', 'guardian')
-- ON DUPLICATE KEY UPDATE ego_enabled=1, ego_name='카르마', ego_personality='guardian';

-- 5. 참고: 기존 characters_inventory에 직접 컬럼 추가하는 방식은 비추천입니다.
-- 이유: 기존 저장/로드 SQL 전체를 수정해야 하며, 실수 시 인벤토리 로딩 오류 가능성이 커집니다.
-- 별도 character_item_ego 테이블 방식이 안전합니다.
