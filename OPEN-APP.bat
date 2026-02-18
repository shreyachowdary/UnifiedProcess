@echo off
echo Opening Notification Platform in your browser...
start http://localhost:8080
echo.
echo If the page doesn't load:
echo 1. Make sure you ran START.bat first
echo 2. Wait 1-2 minutes for the API to start
echo 3. Try opening: http://localhost:8080
echo.
timeout /t 3 >nul
