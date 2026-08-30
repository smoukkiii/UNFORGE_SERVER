@echo off
setlocal
cd /d "%~dp0rsmod"

echo Stopping any running Project Gielinor server...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='SilentlyContinue';" ^
  "Get-NetTCPConnection -LocalPort 43594 -State Listen | ForEach-Object { Write-Host ('Killing PID ' + $_.OwningProcess + ' on port 43594'); Stop-Process -Id $_.OwningProcess -Force };" ^
  "Get-CimInstance Win32_Process -Filter \"Name = 'java.exe'\" | Where-Object { $_.CommandLine -match 'server:app:run|org\.rsmod\.server\.app\.GameServer' } | ForEach-Object { Write-Host ('Killing Java PID ' + $_.ProcessId); Stop-Process -Id $_.ProcessId -Force }"

if not exist "gradlew.bat" (
  echo ERROR: gradlew.bat not found in "%CD%"
  exit /b 1
)

echo Starting Project Gielinor...
call gradlew.bat :server:app:run --args=--skip-type-verification --console=plain %*
exit /b %ERRORLEVEL%
