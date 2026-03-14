@echo off
setlocal ENABLEDELAYEDEXPANSION
color CF

cd /d "%~dp0"

set "JAVA_EXE=%~dp0jre\bin\java.exe"
if not exist "%JAVA_EXE%" (
    set "JAVA_EXE=java"
)

set "PORT=2000"
for /f "usebackq tokens=1,* delims==" %%A in ("socket.conf") do (
    set "K=%%~A"
    set "V=%%~B"
    if /I "!K!"=="port " set "PORT=!V!"
    if /I "!K!"=="port" set "PORT=!V!"
)
set "PORT=%PORT: =%"

for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":%PORT% .*LISTENING"') do (
    set "HOLD_PID=%%P"
)

if defined HOLD_PID (
    echo [ERROR] Port %PORT% is already in use. PID=!HOLD_PID!
    tasklist /FI "PID eq !HOLD_PID!" | findstr /I /V "Image Name" 
    echo Stop the existing process, then run again.
    pause
    exit /b 1
)

set "HOLD_PID="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":13000 .*LISTENING"') do (
    set "HOLD_PID=%%P"
)

if defined HOLD_PID (
    echo [ERROR] Auth port 13000 is already in use. PID=!HOLD_PID!
    tasklist /FI "PID eq !HOLD_PID!" | findstr /I /V "Image Name"
    echo Stop the existing process, then run again.
    pause
    exit /b 1
)

"%JAVA_EXE%" -Xms16g -Xmx32g -Xss1m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./heapdump.hprof -cp ".;lib/*;server.jar" lineage.Main gui

