@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ============================================================
echo 에고무기 원클릭 설치 - Windows
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

set SQL_FILE=%~dp0..\sql\ego_oneclick_install.sql

if not exist "%SQL_FILE%" (
    echo [ERROR] SQL 파일을 찾을 수 없습니다: %SQL_FILE%
    pause
    exit /b 1
)

echo.
echo [INFO] 설치 SQL 실행 중...
echo mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p**** %DB_NAME% ^< %SQL_FILE%
echo.

mysql -h %DB_HOST% -P %DB_PORT% -u %DB_USER% -p%DB_PASS% --default-character-set=utf8 %DB_NAME% < "%SQL_FILE%"

if errorlevel 1 (
    echo.
    echo [ERROR] 설치 실패
    echo 확인사항:
    echo 1. mysql 명령어가 PATH에 등록되어 있는지 확인
    echo 2. DB 접속정보가 맞는지 확인
    echo 3. DB 권한이 있는지 확인
    echo 4. SQL 파일 인코딩이 UTF-8인지 확인
    pause
    exit /b 1
)

echo.
echo [OK] 에고무기 DB 설치 완료
echo 다음 단계:
echo 1. 서버가 실행 중이면 .에고리로드 실행
echo 2. 또는 서버 재시작
echo 3. Java 연결이 없는 서버는 docs\EGO_SYSTEM_MANUAL.md 또는 docs\EGO_PORTING_GUIDE.md 참고
echo.
pause
exit /b 0
