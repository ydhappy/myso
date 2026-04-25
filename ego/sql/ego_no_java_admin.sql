-- ============================================================
-- 에고무기 운영 관리 SQL - EUC-KR 안전 영문 스키마 기준
-- 목적: Java 수정 없이 DB에서 에고 조회/편집/이미지변경/경험치보정/이상데이터점검
-- 전제: ego_install_euckr.sql 적용 완료
-- 파일 인코딩: UTF-8
-- 접속 인코딩: SET NAMES utf8
-- 테이블 문자셋: euckr
-- ============================================================

SET NAMES utf8;

-- ------------------------------------------------------------
-- 0. 기본 변수
-- ------------------------------------------------------------
SET @char_id := 100001;
SET @item_id := 123456789;
SET @ego_name := '카르마';
SET @ego_type := '수호';
SET @form := 'sword';
SET @skill := 'EGO_BALANCE';
SET @inv_gfx := 0;
SET @ground_gfx := 0;
SET @label := '한손검';
SET @memo := '에고 한손검 형태';

-- form: dagger 단검 / sword 한손검 / tohandsword 양손검 / axe 도끼 / spear 창 / bow 활 / staff 지팡이 / wand 완드
-- 이미지: 0이면 원본 아이템 이미지 사용, 1 이상이면 해당 gfx 번호 사용

-- ------------------------------------------------------------
-- 1. 테이블 존재 확인
-- ------------------------------------------------------------
SHOW TABLES LIKE 'ego';
SHOW TABLES LIKE 'ego_skill';
SHOW TABLES LIKE 'ego_view';
SHOW TABLES LIKE 'ego_type';
SHOW TABLES LIKE 'ego_talk';
SHOW TABLES LIKE 'ego_skill_base';
SHOW TABLES LIKE 'ego_log';

-- ------------------------------------------------------------
-- 2. 에고 수동 생성 또는 재활성화
-- 주의: 게임 내 .에고생성은 +0 무기만 허용한다.
-- DB 수동 생성 시에는 item_id가 실제 착용 무기 objectId인지 운영자가 직접 확인해야 한다.
-- ------------------------------------------------------------
INSERT INTO ego
(item_id, char_id, use_yn, ego_name, ego_type, ego_lv, ego_exp, need_exp, talk_lv, ctrl_lv, last_talk, last_warn, form, prev_shield, mod_date)
VALUES
(@item_id, @char_id, 1, @ego_name, @ego_type, 1, 0, 100, 1, 1, 0, 0, @form, 0, NOW())
ON DUPLICATE KEY UPDATE
    char_id = VALUES(char_id),
    use_yn = 1,
    ego_name = VALUES(ego_name),
    ego_type = VALUES(ego_type),
    form = VALUES(form),
    ego_lv = IF(ego_lv < 1, 1, ego_lv),
    ego_exp = IF(ego_exp < 0, 0, ego_exp),
    need_exp = IF(need_exp < 1, 100, need_exp),
    talk_lv = IF(talk_lv < 1, 1, talk_lv),
    ctrl_lv = IF(ctrl_lv < 1, 1, ctrl_lv),
    mod_date = NOW();

-- ------------------------------------------------------------
-- 3. 능력 설정, 한 무기당 활성 능력 1개 권장
-- ------------------------------------------------------------
UPDATE ego_skill
SET use_yn = 0,
    mod_date = NOW()
WHERE item_id = @item_id;

INSERT INTO ego_skill
(item_id, skill, skill_lv, rate_bonus, dmg_bonus, last_proc, use_yn, mod_date)
VALUES
(@item_id, @skill, 1, 0, 0, 0, 1, NOW())
ON DUPLICATE KEY UPDATE
    use_yn = 1,
    skill_lv = IF(skill_lv < 1, 1, skill_lv),
    mod_date = NOW();

-- ------------------------------------------------------------
-- 4. 형태별 인벤토리/바닥 이미지 설정
-- ------------------------------------------------------------
INSERT INTO ego_view
(form, label, inv_gfx, ground_gfx, memo, use_yn)
VALUES
(@form, @label, @inv_gfx, @ground_gfx, @memo, 1)
ON DUPLICATE KEY UPDATE
    label = VALUES(label),
    inv_gfx = VALUES(inv_gfx),
    ground_gfx = VALUES(ground_gfx),
    memo = VALUES(memo),
    use_yn = 1;

-- 예시:
-- UPDATE ego_view SET inv_gfx = 1001, ground_gfx = 1002, memo = '에고 활 전용 이미지' WHERE form = 'bow';
-- UPDATE ego_view SET inv_gfx = 0, ground_gfx = 0 WHERE form = 'bow'; -- 원본 이미지 사용

-- ------------------------------------------------------------
-- 5. 현재 지정 아이템 상태 조회
-- ------------------------------------------------------------
SELECT
    e.item_id,
    e.char_id,
    e.use_yn,
    e.ego_name,
    e.ego_type,
    e.form,
    e.prev_shield,
    e.ego_lv,
    e.ego_exp,
    e.need_exp,
    e.talk_lv,
    e.ctrl_lv,
    s.skill,
    s.skill_lv,
    s.rate_bonus,
    s.dmg_bonus,
    v.label,
    v.inv_gfx,
    v.ground_gfx,
    v.memo,
    e.reg_date,
    e.mod_date
FROM ego e
LEFT JOIN ego_skill s ON s.item_id = e.item_id AND s.use_yn = 1
LEFT JOIN ego_view v ON v.form = e.form AND v.use_yn = 1
WHERE e.item_id = @item_id;

-- ------------------------------------------------------------
-- 6. 자주 쓰는 편집 SQL
-- ------------------------------------------------------------
-- 이름 변경
-- UPDATE ego SET ego_name = '루나', mod_date = NOW() WHERE item_id = @item_id AND use_yn = 1;

-- 성격 변경
-- UPDATE ego SET ego_type = '현자', mod_date = NOW() WHERE item_id = @item_id AND use_yn = 1;

-- 표시형태 변경, 실제 item.type2는 바뀌지 않는다.
-- UPDATE ego SET form = 'bow', mod_date = NOW() WHERE item_id = @item_id AND use_yn = 1;
-- UPDATE ego SET form = 'tohandsword', mod_date = NOW() WHERE item_id = @item_id AND use_yn = 1;
-- UPDATE ego SET form = 'sword', mod_date = NOW() WHERE item_id = @item_id AND use_yn = 1;

-- 형태별 이미지 변경
-- UPDATE ego_view SET inv_gfx = 1001, ground_gfx = 1002, memo = '에고 활 전용 이미지' WHERE form = 'bow';

-- 레벨/경험치 보정
-- UPDATE ego SET ego_lv = 5, ego_exp = 0, need_exp = 1000, mod_date = NOW() WHERE item_id = @item_id AND use_yn = 1;

-- 능력 보정
-- UPDATE ego_skill SET skill_lv = 3, rate_bonus = 2, dmg_bonus = 5, mod_date = NOW() WHERE item_id = @item_id AND skill = @skill AND use_yn = 1;

-- 비활성화
-- UPDATE ego SET use_yn = 0, mod_date = NOW() WHERE item_id = @item_id;
-- UPDATE ego_skill SET use_yn = 0, mod_date = NOW() WHERE item_id = @item_id;

-- 삭제
-- DELETE FROM ego_skill WHERE item_id = @item_id;
-- DELETE FROM ego WHERE item_id = @item_id;

-- ------------------------------------------------------------
-- 7. 전체 목록
-- ------------------------------------------------------------
SELECT
    e.char_id,
    e.item_id,
    e.use_yn,
    e.ego_name,
    e.ego_type,
    e.form,
    e.ego_lv,
    e.ego_exp,
    e.need_exp,
    s.skill,
    s.skill_lv,
    s.use_yn AS skill_use_yn,
    v.label,
    v.inv_gfx,
    v.ground_gfx,
    e.reg_date,
    e.mod_date
FROM ego e
LEFT JOIN ego_skill s ON s.item_id = e.item_id AND s.use_yn = 1
LEFT JOIN ego_view v ON v.form = e.form AND v.use_yn = 1
ORDER BY IFNULL(e.mod_date, e.reg_date) DESC, e.item_id DESC;

-- ------------------------------------------------------------
-- 8. 이상 데이터 점검
-- ------------------------------------------------------------
-- 활성 에고인데 활성 능력이 없는 경우
SELECT e.*
FROM ego e
LEFT JOIN ego_skill s ON s.item_id = e.item_id AND s.use_yn = 1
WHERE e.use_yn = 1 AND s.id IS NULL;

-- 한 무기에 활성 능력이 2개 이상인 경우
SELECT item_id, COUNT(*) AS active_skill_count
FROM ego_skill
WHERE use_yn = 1
GROUP BY item_id
HAVING COUNT(*) > 1;

-- 레벨/경험치 이상값
SELECT *
FROM ego
WHERE ego_lv < 1
   OR ego_lv > 30
   OR need_exp < 1
   OR ego_exp < 0
   OR form NOT IN ('', 'dagger', 'sword', 'tohandsword', 'axe', 'spear', 'bow', 'staff', 'wand');

-- 표시형태가 ego_view에 없는 경우
SELECT e.*
FROM ego e
LEFT JOIN ego_view v ON v.form = e.form
WHERE e.use_yn = 1
  AND e.form <> ''
  AND v.form IS NULL;

-- ------------------------------------------------------------
-- 9. 이상 데이터 자동 보정
-- 필요할 때만 주석 해제해서 사용
-- ------------------------------------------------------------
-- UPDATE ego SET ego_lv = 1 WHERE ego_lv < 1;
-- UPDATE ego SET ego_lv = 30 WHERE ego_lv > 30;
-- UPDATE ego SET ego_exp = 0 WHERE ego_exp < 0;
-- UPDATE ego SET need_exp = 100 WHERE need_exp < 1;
-- UPDATE ego SET form = '' WHERE form NOT IN ('', 'dagger', 'sword', 'tohandsword', 'axe', 'spear', 'bow', 'staff', 'wand');

-- 활성 능력이 여러 개인 경우, 가장 작은 id만 남기고 나머지 비활성화
-- UPDATE ego_skill s
-- JOIN (
--     SELECT item_id, MIN(id) AS keep_id
--     FROM ego_skill
--     WHERE use_yn = 1
--     GROUP BY item_id
--     HAVING COUNT(*) > 1
-- ) x ON x.item_id = s.item_id
-- SET s.use_yn = IF(s.id = x.keep_id, 1, 0),
--     s.mod_date = NOW()
-- WHERE s.use_yn = 1;

-- ------------------------------------------------------------
-- 10. 운영 후 반영
-- ------------------------------------------------------------
-- 게임 안에서 .에고리로드 실행
-- 또는 서버 재시작
