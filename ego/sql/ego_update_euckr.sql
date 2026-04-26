-- ============================================================
-- 에고무기 기존 서버 업데이트 SQL - 최소 실행 파일
-- 파일 인코딩: UTF-8
-- DB/테이블 문자셋: euckr
--
-- 목적:
--   기존 구버전 에고 DB를 현재 DB화 구조로 한번에 보정합니다.
--
-- 실행 순서:
--   SOURCE ego/sql/ego_update_euckr.sql;
--
-- 내부 실행:
--   1) ego_cleanup_unused.sql      구버전 미사용 테이블/컬럼 정리 + 기본 보정
--   2) ego_db_config.sql           설정/경험치/전투보너스/무기규칙 DB화
--   3) ego_talk_pack_dedupe.sql    대사팩 중복 정리
--
-- 주의:
--   SOURCE 문은 mysql CLI, HeidiSQL, Navicat 등에서 동작 방식이 다를 수 있습니다.
--   SOURCE가 안 되는 툴이면 아래 3개 파일을 순서대로 직접 실행하세요.
-- ============================================================

SET NAMES utf8;

SOURCE ego/sql/ego_cleanup_unused.sql;
SOURCE ego/sql/ego_db_config.sql;
SOURCE ego/sql/ego_talk_pack_dedupe.sql;

SELECT 'EGO_UPDATE_EUCKR_ALL_OK' AS result;
