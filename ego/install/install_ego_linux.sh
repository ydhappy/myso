#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SQL_FILE="$SCRIPT_DIR/../sql/ego_install_euckr.sql"

echo "============================================================"
echo "에고무기 원클릭 설치 - Linux/macOS / EUC-KR DB 안전버전"
echo "============================================================"
echo

read -r -p "DB HOST 입력 [localhost]: " DB_HOST
DB_HOST=${DB_HOST:-localhost}

read -r -p "DB PORT 입력 [3306]: " DB_PORT
DB_PORT=${DB_PORT:-3306}

read -r -p "DB NAME 입력: " DB_NAME
if [ -z "$DB_NAME" ]; then
  echo "[ERROR] DB NAME은 필수입니다."
  exit 1
fi

read -r -p "DB USER 입력 [root]: " DB_USER
DB_USER=${DB_USER:-root}

read -r -s -p "DB PASSWORD 입력: " DB_PASS
echo

if [ ! -f "$SQL_FILE" ]; then
  echo "[ERROR] SQL 파일을 찾을 수 없습니다: $SQL_FILE"
  exit 1
fi

if ! command -v mysql >/dev/null 2>&1; then
  echo "[ERROR] mysql 명령어를 찾을 수 없습니다. mysql client 설치 또는 PATH 등록이 필요합니다."
  exit 1
fi

echo
echo "[INFO] EUC-KR DB 안전 에고 테이블 설치 SQL 실행 중..."
echo "[INFO] 파일 인코딩 UTF-8 / 접속 인코딩 UTF-8 / 테이블 문자셋 EUC-KR"
echo "[INFO] 테이블명은 ego, ego_skill, ego_view 등 영문 단순명으로 생성됩니다."
echo "mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p**** --default-character-set=utf8 $DB_NAME < $SQL_FILE"
echo

MYSQL_PWD="$DB_PASS" mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" --default-character-set=utf8 "$DB_NAME" < "$SQL_FILE"

echo
echo "[OK] 에고무기 DB 설치 완료"
echo "생성 테이블: ego, ego_skill, ego_view, ego_type, ego_talk, ego_skill_base, ego_log"
echo "다음 단계:"
echo "1. 서버가 실행 중이면 .에고리로드 실행"
echo "2. 또는 서버 재시작"
echo "3. docs/EGO_SYSTEM_MANUAL.md 확인"
echo
