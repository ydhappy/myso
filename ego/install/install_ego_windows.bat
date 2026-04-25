@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ============================================================
echo 에고무기 원클릭 설치 - Windows / EUC-KR DB 안전버전
echo ============================================================
echo.

set /p DB_HOST=DB HOST 입력 [localhost]: 
if "%DB_HOST%"=="" set DB_HOST=localhost

set /p DB_PORT=DB PORT 입력 [3306]: 
if "%DB_PORT%"=="" set DB_PORT=3306

set /p DB_NAME=DB NAME 입력: 
if "%DB_NAME%"=="" (
    echo [ERROR] DB NAME은 필수입니다.
    pause
    exit /b 1
)

set /p DB_USER=DB USER 입력 [root]: 
if "%DB_USER%"=="" set DB_USER=root

set /p DB_PASS=DB PASSWORD 입력: 

set SQL_FILE=%~dp0..\sql\ego_install_euckr.sql

if not exist "%SQL_FILE%" (
    echo [ERROR] SQL 파일을 찾을 수 없습니다: %SQL_FILE%
    pause
    exit /b 1
)

echo.
echo [INFO] EUC-KR DB 안전 에고 테이블 설치 SQL 실행 중...
echo [INFO] 파일 인코딩 UTF-8 / 접속 인코딩 UTF-8 / 테이블 문자셋 EUC-KR
echo [INFO] 테이블명은 ego, ego_skill, ego_view 등 영문 단순명으로 생성됩니다.
echo mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p**** --default-character-set=utf8 %DB_NAME% ^< %SQL_FILE%
echo.

mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% --default-character-set=utf8 %DB_NAME% < "%SQL_FILE%"

if errorlevel 1 (
    echo.
    echo [ERROR] 설치 실패
    echo 확인사항:
    echo 1. mysql 명령어가 PATH에 등록되어 있는지 확인
    echo 2. DB 접속정보가 맞는지 확인
    echo 3. DB 권한이 있는지 확인
    echo 4. SQL 파일은 UTF-8로 저장되어 있어야 합니다.
    echo 5. DB가 EUC-KR이어도 접속은 --default-character-set=utf8 로 실행합니다.
    pause
    exit /b 1
)

echo.
echo [OK] 에고무기 DB 설치 완료
echo 생성 테이블: ego, ego_skill, ego_view, ego_type, ego_talk, ego_skill_base, ego_log
echo 다음 단계:
echo 1. 서버가 실행 중이면 .에고리로드 실행
echo 2. 또는 서버 재시작
echo 3. docs\EGO_SYSTEM_MANUAL.md 확인
echo.
pause
exit /b 0
