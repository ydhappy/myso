-- ============================================================
-- 에고무기 무자바 생성/편집 관리 SQL
-- 목적: Java 소스 수정 없이 DB에서 에고무기 생성/편집/형태변경/조회/비활성화
-- 전제: ego_oneclick_install.sql 적용 완료
-- 주의: 서버코어가 에고 DB를 로드/사용하는 Java 연결은 이미 되어 있어야 실제 게임 기능으로 반영됩니다.
-- ============================================================

SET NAMES utf8;

-- ------------------------------------------------------------
-- 0. 기본 변수
-- ------------------------------------------------------------
SET @CHA_OBJID := 100001;              -- 캐릭터 objectId
SET @ITEM_OBJID := 123456789;          -- 착용 무기 item objectId
SET @EGO_NAME := '카르마';             -- 에고 호출 이름
SET @EGO_PERSONALITY := 'guardian';    -- guardian / berserker / calm / sage
SET @EGO_ABILITY := 'EGO_BALANCE';     -- 능력 코드
SET @EGO_FORM := 'sword';              -- dagger/sword/tohandsword/axe/spear/bow/staff/wand

-- ------------------------------------------------------------
-- 1. 형태변환 컬럼 보정, 기존 DB용
-- ------------------------------------------------------------
SET @db_name := DATABASE();
SET @sql := IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=@db_name AND TABLE_NAME='character_item_ego' AND COLUMN_NAME='ego_form_type') = 0,
    'ALTER TABLE character_item_ego ADD COLUMN ego_form_type VARCHAR(40) NOT NULL DEFAULT '''' COMMENT ''에고 현재 변신 형태'' AFTER ego_last_warning_time',
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

-- ------------------------------------------------------------
-- 2. 현재 에고 상태 조회
-- ------------------------------------------------------------
SELECT
    e.item_objid,
    e.cha_objid,
    e.ego_enabled,
    e.ego_name,
    e.ego_personality,
    e.ego_form_type,
    e.prev_shield_objid,
    e.ego_level,
    e.ego_exp,
    e.ego_max_exp,
    e.ego_talk_level,
    e.ego_control_level,
    e.updated_at
FROM character_item_ego e
WHERE e.item_objid = @ITEM_OBJID;

SELECT
    a.uid,
    a.item_objid,
    a.ability_type,
    a.ability_level,
    a.proc_chance_bonus,
    a.damage_bonus,
    a.enabled,
    a.updated_at
FROM character_item_ego_ability a
WHERE a.item_objid = @ITEM_OBJID
ORDER BY a.enabled DESC, a.uid DESC;

-- ------------------------------------------------------------
-- 3. 에고 생성 또는 재활성화
-- ------------------------------------------------------------
INSERT INTO character_item_ego
(
    item_objid,
    cha_objid,
    ego_enabled,
    ego_name,
    ego_personality,
    ego_level,
    ego_exp,
    ego_max_exp,
    ego_talk_level,
    ego_control_level,
    ego_last_talk_time,
    ego_last_warning_time,
    ego_form_type,
    prev_shield_objid
)
VALUES
(
    @ITEM_OBJID,
    @CHA_OBJID,
    1,
    @EGO_NAME,
    @EGO_PERSONALITY,
    1,
    0,
    100,
    1,
    1,
    0,
    0,
    @EGO_FORM,
    0
)
ON DUPLICATE KEY UPDATE
    cha_objid = VALUES(cha_objid),
    ego_enabled = 1,
    ego_name = VALUES(ego_name),
    ego_personality = VALUES(ego_personality),
    ego_form_type = VALUES(ego_form_type),
    ego_level = IF(ego_level < 1, 1, ego_level),
    ego_max_exp = IF(ego_max_exp < 1, 100, ego_max_exp),
    ego_talk_level = IF(ego_talk_level < 1, 1, ego_talk_level),
    ego_control_level = IF(ego_control_level < 1, 1, ego_control_level);

-- ------------------------------------------------------------
-- 4. 능력 설정
-- ------------------------------------------------------------
UPDATE character_item_ego_ability
SET enabled = 0
WHERE item_objid = @ITEM_OBJID;

INSERT INTO character_item_ego_ability
(
    item_objid,
    ability_type,
    ability_level,
    proc_chance_bonus,
    damage_bonus,
    last_proc_time,
    enabled
)
VALUES
(
    @ITEM_OBJID,
    @EGO_ABILITY,
    1,
    0,
    0,
    0,
    1
)
ON DUPLICATE KEY UPDATE
    enabled = 1,
    ability_level = IF(ability_level < 1, 1, ability_level);

-- ------------------------------------------------------------
-- 5. 형태만 변경
-- ------------------------------------------------------------
-- SET @EGO_FORM := 'bow';         -- 활
-- SET @EGO_FORM := 'tohandsword'; -- 양손검
-- SET @EGO_FORM := 'sword';       -- 한손검
-- SET @EGO_FORM := 'dagger';      -- 단검
-- SET @EGO_FORM := 'spear';       -- 창
-- SET @EGO_FORM := 'axe';         -- 도끼
-- SET @EGO_FORM := 'staff';       -- 지팡이
-- SET @EGO_FORM := 'wand';        -- 완드
-- UPDATE character_item_ego
-- SET ego_form_type = @EGO_FORM
-- WHERE item_objid = @ITEM_OBJID
--   AND ego_enabled = 1;

-- ------------------------------------------------------------
-- 6. 이름/성격 변경
-- ------------------------------------------------------------
-- UPDATE character_item_ego
-- SET ego_name = @EGO_NAME
-- WHERE item_objid = @ITEM_OBJID
--   AND ego_enabled = 1;
--
-- UPDATE character_item_ego
-- SET ego_personality = @EGO_PERSONALITY
-- WHERE item_objid = @ITEM_OBJID
--   AND ego_enabled = 1;

-- ------------------------------------------------------------
-- 7. 레벨/경험치 직접 보정
-- ------------------------------------------------------------
-- UPDATE character_item_ego
-- SET ego_level = 5,
--     ego_exp = 0,
--     ego_max_exp = 500
-- WHERE item_objid = @ITEM_OBJID
--   AND ego_enabled = 1;

-- ------------------------------------------------------------
-- 8. 능력 레벨/확률/피해 보정
-- ------------------------------------------------------------
-- UPDATE character_item_ego_ability
-- SET ability_level = 3,
--     proc_chance_bonus = 2,
--     damage_bonus = 5
-- WHERE item_objid = @ITEM_OBJID
--   AND ability_type = @EGO_ABILITY
--   AND enabled = 1;

-- ------------------------------------------------------------
-- 9. 에고 비활성화
-- ------------------------------------------------------------
-- UPDATE character_item_ego SET ego_enabled = 0 WHERE item_objid = @ITEM_OBJID;
-- UPDATE character_item_ego_ability SET enabled = 0 WHERE item_objid = @ITEM_OBJID;

-- ------------------------------------------------------------
-- 10. 에고 완전 삭제
-- ------------------------------------------------------------
-- DELETE FROM character_item_ego_ability WHERE item_objid = @ITEM_OBJID;
-- DELETE FROM character_item_ego WHERE item_objid = @ITEM_OBJID;

-- ------------------------------------------------------------
-- 11. 전체 에고 목록 조회
-- ------------------------------------------------------------
SELECT
    e.cha_objid,
    e.item_objid,
    e.ego_enabled,
    e.ego_name,
    e.ego_personality,
    e.ego_form_type,
    e.prev_shield_objid,
    e.ego_level,
    e.ego_exp,
    e.ego_max_exp,
    a.ability_type,
    a.ability_level,
    a.proc_chance_bonus,
    a.damage_bonus,
    a.enabled AS ability_enabled,
    e.updated_at
FROM character_item_ego e
LEFT JOIN character_item_ego_ability a
       ON a.item_objid = e.item_objid
      AND a.enabled = 1
ORDER BY e.updated_at DESC, e.item_objid DESC;

-- ------------------------------------------------------------
-- 12. 이상 데이터 점검
-- ------------------------------------------------------------
SELECT e.*
FROM character_item_ego e
LEFT JOIN character_item_ego_ability a
       ON a.item_objid = e.item_objid
      AND a.enabled = 1
WHERE e.ego_enabled = 1
  AND a.uid IS NULL;

SELECT item_objid, COUNT(*) AS enabled_ability_count
FROM character_item_ego_ability
WHERE enabled = 1
GROUP BY item_objid
HAVING COUNT(*) > 1;

SELECT *
FROM character_item_ego
WHERE ego_level < 1
   OR ego_max_exp < 1
   OR ego_exp < 0
   OR ego_form_type NOT IN ('', 'dagger', 'sword', 'tohandsword', 'axe', 'spear', 'bow', 'staff', 'wand');

-- ------------------------------------------------------------
-- 13. 적용 후 확인
-- ------------------------------------------------------------
SELECT
    e.item_objid,
    e.cha_objid,
    e.ego_enabled,
    e.ego_name,
    e.ego_personality,
    e.ego_form_type,
    e.prev_shield_objid,
    e.ego_level,
    e.ego_exp,
    e.ego_max_exp,
    a.ability_type,
    a.ability_level,
    a.proc_chance_bonus,
    a.damage_bonus,
    a.enabled AS ability_enabled
FROM character_item_ego e
LEFT JOIN character_item_ego_ability a
       ON a.item_objid = e.item_objid
      AND a.enabled = 1
WHERE e.item_objid = @ITEM_OBJID;
