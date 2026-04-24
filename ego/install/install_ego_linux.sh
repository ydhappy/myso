#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SQL_FILE="$SCRIPT_DIR/../sql/ego_oneclick_install.sql"

echo "============================================================"
echo "에고무기 원클릭 설치 - Linux/macOS"
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
echo "[INFO] 설치 SQL 실행 중..."
echo "mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p**** $DB_NAME < $SQL_FILE"
echo

MYSQL_PWD="$DB_PASS" mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" --default-character-set=utf8 "$DB_NAME" < "$SQL_FILE"

echo
echo "[OK] 에고무기 DB 설치 완료"
echo "다음 단계:"
echo "1. 서버가 실행 중이면 .에고리로드 실행"
echo "2. 또는 서버 재시작"
echo "3. Java 연결이 없는 서버는 docs/EGO_SYSTEM_MANUAL.md 또는 docs/EGO_PORTING_GUIDE.md 참고"
echo
