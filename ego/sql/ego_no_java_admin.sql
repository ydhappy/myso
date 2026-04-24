-- ============================================================
-- 에고무기 무자바 생성/편집 관리 SQL
-- 목적: Java 소스 수정 없이 DB에서 에고무기 생성/편집/조회/비활성화
-- 전제: ego_weapon.sql, ego_weapon_ability.sql 적용 완료
-- 주의: 서버코어가 에고 DB를 로드/사용하는 Java 연결은 이미 되어 있어야 실제 게임 기능으로 반영됩니다.
-- ============================================================

-- ------------------------------------------------------------
-- 0. 기본 사용법
-- ------------------------------------------------------------
-- 1) 아래 변수만 실제 값으로 변경합니다.
-- 2) 필요한 섹션만 실행합니다.
-- 3) 운영 DB에서는 실행 전 반드시 백업합니다.

SET @CHA_OBJID := 100001;              -- 캐릭터 objectId
SET @ITEM_OBJID := 123456789;          -- 착용 무기 item objectId
SET @EGO_NAME := '카르마';             -- 에고 호출 이름
SET @EGO_PERSONALITY := 'guardian';    -- guardian / berserker / calm / sage
SET @EGO_ABILITY := 'EGO_BALANCE';     -- 능력 코드

-- 능력 코드 목록
-- EGO_BALANCE, BLOOD_DRAIN, MANA_DRAIN, CRITICAL_BURST,
-- GUARDIAN_SHIELD, AREA_SLASH, EXECUTION, FLAME_BRAND, FROST_BIND

-- ------------------------------------------------------------
-- 1. 현재 에고 상태 조회
-- ------------------------------------------------------------
SELECT
    e.item_objid,
    e.cha_objid,
    e.ego_enabled,
    e.ego_name,
    e.ego_personality,
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
-- 2. 에고 생성 또는 재활성화
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
    ego_last_warning_time
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
    0
)
ON DUPLICATE KEY UPDATE
    cha_objid = VALUES(cha_objid),
    ego_enabled = 1,
    ego_name = VALUES(ego_name),
    ego_personality = VALUES(ego_personality),
    ego_level = IF(ego_level < 1, 1, ego_level),
    ego_max_exp = IF(ego_max_exp < 1, 100, ego_max_exp),
    ego_talk_level = IF(ego_talk_level < 1, 1, ego_talk_level),
    ego_control_level = IF(ego_control_level < 1, 1, ego_control_level);

-- ------------------------------------------------------------
-- 3. 능력 설정
-- 기본 정책: 한 무기에는 활성 능력 1개만 둡니다.
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
-- 4. 이름만 변경
-- ------------------------------------------------------------
-- SET @EGO_NAME := '루나';
-- UPDATE character_item_ego
-- SET ego_name = @EGO_NAME
-- WHERE item_objid = @ITEM_OBJID
--   AND ego_enabled = 1;

-- ------------------------------------------------------------
-- 5. 성격만 변경
-- ------------------------------------------------------------
-- SET @EGO_PERSONALITY := 'sage';
-- UPDATE character_item_ego
-- SET ego_personality = @EGO_PERSONALITY
-- WHERE item_objid = @ITEM_OBJID
--   AND ego_enabled = 1;

-- ------------------------------------------------------------
-- 6. 레벨/경험치 직접 보정
-- ------------------------------------------------------------
-- UPDATE character_item_ego
-- SET ego_level = 5,
--     ego_exp = 0,
--     ego_max_exp = 500
-- WHERE item_objid = @ITEM_OBJID
--   AND ego_enabled = 1;

-- ------------------------------------------------------------
-- 7. 능력 레벨/확률/피해 보정
-- ------------------------------------------------------------
-- UPDATE character_item_ego_ability
-- SET ability_level = 3,
--     proc_chance_bonus = 2,
--     damage_bonus = 5
-- WHERE item_objid = @ITEM_OBJID
--   AND ability_type = @EGO_ABILITY
--   AND enabled = 1;

-- ------------------------------------------------------------
-- 8. 에고 비활성화
-- 데이터는 남기고 기능만 끕니다.
-- ------------------------------------------------------------
-- UPDATE character_item_ego
-- SET ego_enabled = 0
-- WHERE item_objid = @ITEM_OBJID;
--
-- UPDATE character_item_ego_ability
-- SET enabled = 0
-- WHERE item_objid = @ITEM_OBJID;

-- ------------------------------------------------------------
-- 9. 에고 완전 삭제
-- 신중히 사용하세요.
-- ------------------------------------------------------------
-- DELETE FROM character_item_ego_ability
-- WHERE item_objid = @ITEM_OBJID;
--
-- DELETE FROM character_item_ego
-- WHERE item_objid = @ITEM_OBJID;

-- ------------------------------------------------------------
-- 10. 전체 에고 목록 조회
-- ------------------------------------------------------------
SELECT
    e.cha_objid,
    e.item_objid,
    e.ego_enabled,
    e.ego_name,
    e.ego_personality,
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
-- 11. 이상 데이터 점검
-- ------------------------------------------------------------

-- 에고는 켜져 있는데 능력이 없는 아이템
SELECT e.*
FROM character_item_ego e
LEFT JOIN character_item_ego_ability a
       ON a.item_objid = e.item_objid
      AND a.enabled = 1
WHERE e.ego_enabled = 1
  AND a.uid IS NULL;

-- 한 아이템에 활성 능력이 2개 이상인 데이터
SELECT item_objid, COUNT(*) AS enabled_ability_count
FROM character_item_ego_ability
WHERE enabled = 1
GROUP BY item_objid
HAVING COUNT(*) > 1;

-- 레벨/경험치 비정상 데이터
SELECT *
FROM character_item_ego
WHERE ego_level < 1
   OR ego_max_exp < 1
   OR ego_exp < 0;

-- ------------------------------------------------------------
-- 12. 이상 데이터 자동 보정
-- ------------------------------------------------------------
-- 레벨/경험치 최소값 보정
-- UPDATE character_item_ego
-- SET ego_level = IF(ego_level < 1, 1, ego_level),
--     ego_max_exp = IF(ego_max_exp < 1, 100, ego_max_exp),
--     ego_exp = IF(ego_exp < 0, 0, ego_exp),
--     ego_talk_level = IF(ego_talk_level < 1, 1, ego_talk_level),
--     ego_control_level = IF(ego_control_level < 1, 1, ego_control_level);

-- 같은 아이템의 활성 능력이 여러 개면 가장 최신 uid만 남기기
-- UPDATE character_item_ego_ability a
-- JOIN (
--     SELECT item_objid, MAX(uid) AS keep_uid
--     FROM character_item_ego_ability
--     WHERE enabled = 1
--     GROUP BY item_objid
--     HAVING COUNT(*) > 1
-- ) x ON x.item_objid = a.item_objid
-- SET a.enabled = IF(a.uid = x.keep_uid, 1, 0)
-- WHERE a.item_objid = x.item_objid;

-- ------------------------------------------------------------
-- 13. 적용 후 확인
-- ------------------------------------------------------------
SELECT
    e.item_objid,
    e.cha_objid,
    e.ego_enabled,
    e.ego_name,
    e.ego_personality,
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
