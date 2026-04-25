-- ============================================================
-- 에고무기 미사용 DB 정리 + 레벨 0~10 경험치/말투 보정 SQL
-- 목적: type2 변형/이미지 커스터마이징 제거 후 남은 미사용 컬럼/테이블 삭제
-- 대상: 기존에 구버전 에고 SQL을 이미 적용한 서버
-- 주의: 실행 전 DB 백업 필수
-- 파일 인코딩: UTF-8
-- ============================================================

SET NAMES utf8;

-- ------------------------------------------------------------
-- 1. 미사용 테이블 삭제
-- ------------------------------------------------------------
DROP TABLE IF EXISTS ego_talk;
DROP TABLE IF EXISTS ego_type;
DROP TABLE IF EXISTS ego_view;
DROP TABLE IF EXISTS `에고모양`;

-- ------------------------------------------------------------
-- 2. ego 테이블의 무기변형 잔여 인덱스/컬럼 삭제
-- MySQL 버전별 IF EXISTS 지원 차이가 있으므로 실패하면 해당 줄은 이미 없는 것으로 보면 됩니다.
-- ------------------------------------------------------------

ALTER TABLE ego DROP INDEX ego_form_idx;
ALTER TABLE ego DROP COLUMN form;
ALTER TABLE ego DROP COLUMN prev_shield;

-- ------------------------------------------------------------
-- 3. 말투/레벨/경험치 보정
-- ------------------------------------------------------------
ALTER TABLE ego MODIFY ego_type VARCHAR(30) NOT NULL DEFAULT '예의' COMMENT '실시간 대화 말투: 예의/예의반대';
ALTER TABLE ego MODIFY ego_lv INT NOT NULL DEFAULT 0 COMMENT '에고 레벨 0~10, 0은 전투능력 없음';
ALTER TABLE ego MODIFY need_exp BIGINT NOT NULL DEFAULT 100 COMMENT '현재 레벨에서 다음 레벨 필요 경험치';

-- 기존 수호/광전/냉정/현자/공백 값은 새 정책상 예의로 통합한다.
UPDATE ego SET ego_type = '예의' WHERE ego_type IS NULL OR ego_type = '' OR ego_type NOT IN ('예의', '예의반대');

UPDATE ego SET ego_lv = 0 WHERE ego_lv < 0;
UPDATE ego SET ego_lv = 10 WHERE ego_lv > 10;

-- Lv.0 -> Lv.1  : 100
-- Lv.1 -> Lv.2  : 250
-- Lv.2 -> Lv.3  : 500
-- Lv.3 -> Lv.4  : 900
-- Lv.4 -> Lv.5  : 1500
-- Lv.5 -> Lv.6  : 2400
-- Lv.6 -> Lv.7  : 3600
-- Lv.7 -> Lv.8  : 5200
-- Lv.8 -> Lv.9  : 7500
-- Lv.9 -> Lv.10 : 10000
-- Lv.10          : 0
UPDATE ego SET need_exp = 100 WHERE ego_lv = 0;
UPDATE ego SET need_exp = 250 WHERE ego_lv = 1;
UPDATE ego SET need_exp = 500 WHERE ego_lv = 2;
UPDATE ego SET need_exp = 900 WHERE ego_lv = 3;
UPDATE ego SET need_exp = 1500 WHERE ego_lv = 4;
UPDATE ego SET need_exp = 2400 WHERE ego_lv = 5;
UPDATE ego SET need_exp = 3600 WHERE ego_lv = 6;
UPDATE ego SET need_exp = 5200 WHERE ego_lv = 7;
UPDATE ego SET need_exp = 7500 WHERE ego_lv = 8;
UPDATE ego SET need_exp = 10000 WHERE ego_lv = 9;
UPDATE ego SET need_exp = 0, ego_exp = 0 WHERE ego_lv = 10;

-- ------------------------------------------------------------
-- 4. 레벨별 해금 능력 기본값 추가/보정
-- ------------------------------------------------------------
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

-- ------------------------------------------------------------
-- 5. 확인
-- ------------------------------------------------------------
DESC ego;
SHOW TABLES LIKE 'ego_type';
SHOW TABLES LIKE 'ego_talk';
SHOW TABLES LIKE 'ego_view';
SHOW TABLES LIKE '에고모양';

SELECT ego_type, COUNT(*) AS count
FROM ego
GROUP BY ego_type;

SELECT ego_lv, need_exp, COUNT(*) AS count
FROM ego
GROUP BY ego_lv, need_exp
ORDER BY ego_lv;

SELECT *
FROM ego_skill_base
WHERE skill IN ('EGO_COUNTER', 'EGO_REVENGE', 'CRITICAL_BURST');

SELECT 'EGO_CLEANUP_TONE_EXP_CURVE_0_10_OK' AS result;
