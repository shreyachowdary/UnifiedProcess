@echo off
echo Stopping Unified Notification Platform...
cd /d "%~dp0infra"
docker compose down
echo Done.
pause
