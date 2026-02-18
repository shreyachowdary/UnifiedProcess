@echo off
echo ============================================
echo   Unified Notification Platform - Starter
echo ============================================
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not running!
    echo.
    echo Please:
    echo 1. Open Docker Desktop from the Start menu
    echo 2. Wait until it shows "Docker Desktop is running"
    echo 3. Run this script again
    echo.
    pause
    exit /b 1
)

echo [OK] Docker is running.
echo.

cd /d "%~dp0infra"

echo Starting all services (this may take a few minutes on first run)...
echo.

docker compose up -d

if errorlevel 1 (
    echo.
    echo [ERROR] Failed to start. Check the output above.
    pause
    exit /b 1
)

echo.
echo ============================================
echo   SUCCESS! Services are starting.
echo ============================================
echo.
echo Wait 2-3 minutes for everything to be ready.
echo Then open in your browser:
echo.
echo    http://localhost:8080
echo.
echo ============================================
pause
