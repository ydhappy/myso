-- ============================================================
-- 에고 DB 대사팩 중복 제거 SQL
-- 대상: ego_talk_pack 기본 대사를 여러 번 삽입한 기존 서버
-- 주의: 실행 전 DB 백업 권장
-- 파일 인코딩: UTF-8
-- ============================================================

SET NAMES utf8;

CREATE TABLE IF NOT EXISTS ego_talk_pack_tmp LIKE ego_talk_pack;

ALTER TABLE ego_talk_pack_tmp ADD UNIQUE KEY ego_talk_pack_uk (genre, tone, message);

INSERT IGNORE INTO ego_talk_pack_tmp
SELECT * FROM ego_talk_pack
ORDER BY id ASC;

RENAME TABLE ego_talk_pack TO ego_talk_pack_old,
             ego_talk_pack_tmp TO ego_talk_pack;

DROP TABLE ego_talk_pack_old;

SELECT 'EGO_TALK_PACK_DEDUPE_OK' AS result;
SELECT genre, tone, COUNT(*) AS count
FROM ego_talk_pack
GROUP BY genre, tone
ORDER BY genre, tone;
