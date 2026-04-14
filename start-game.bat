@echo off
setlocal

set "PROJECT_DIR=%~dp0demo"
set "GAME_URL=http://localhost:8080/game"

if not exist "%PROJECT_DIR%\mvnw.cmd" (
    echo [ERROR] Could not find mvnw.cmd in "%PROJECT_DIR%"
    pause
    exit /b 1
)

echo [INFO] Starting Spring Boot server...
start "Pokemon Game Server" cmd /k "cd /d "%PROJECT_DIR%" && mvnw.cmd spring-boot:run"

echo [INFO] Waiting for server to become available...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$url = '%GAME_URL%';" ^
    "for ($i = 0; $i -lt 120; $i++) {" ^
    "  try {" ^
    "    $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 2;" ^
    "    if ($resp.StatusCode -ge 200) {" ^
    "      Start-Process $url;" ^
    "      exit 0;" ^
    "    }" ^
    "  } catch {}" ^
    "  Start-Sleep -Seconds 1;" ^
    "}" ^
    "Start-Process $url;"

endlocal
