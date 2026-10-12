@echo off
REM Stop PromptForge (铸语) local services
REM Finds the java process listening on 8080 and node processes on 5173

echo Stopping backend (port 8080)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do taskkill /PID %%a /F

echo Stopping frontend (port 5173)...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') do taskkill /PID %%a /F

echo Done.
pause
