@echo off
REM PromptForge (铸语) - local dev service starter
REM Backend: Spring Boot on 8080 | Frontend: Vite dev server on 5173
REM
REM Secrets are loaded from .env.local (git-ignored). Create it first:
REM   copy .env.example .env.local   then fill in your own values.

setlocal enabledelayedexpansion

set "BASE=%~dp0"

if not exist "%BASE%.env.local" (
  echo [ERROR] .env.local not found.
  echo         Run:  copy .env.example .env.local   and fill in your values.
  pause
  exit /b 1
)

REM Load KEY=VALUE pairs from .env.local into environment
for /f "usebackq eol=# tokens=1,* delims==" %%a in ("%BASE%.env.local") do (
  set "%%a=%%b"
)

REM Local-only overrides (adjust to your machine)
set "JAVA_HOME=D:\jcc\jdk\jdk-21"
if not defined DB_HOST set DB_HOST=127.0.0.1
if not defined DB_PORT set DB_PORT=5432

echo [1/2] Starting backend (port 8080)...
start "PromptForge-backend" /MIN cmd /c ""%JAVA_HOME%\bin\java.exe" -jar "%BASE%backend\target\ai-assistant-1.0.0.jar" --server.port=8080 > "%BASE%backend\backend-run.log" 2>&1"

echo [2/2] Starting frontend (port 5173)...
start "PromptForge-frontend" /MIN cmd /c "cd /d "%BASE%frontend" && npm run dev > "%BASE%frontend\frontend-run.log" 2>&1"

echo.
echo Done. Open http://localhost:5173 in your browser (wait ~30s for backend boot).
echo Admin: %ADMIN_USERNAME% / the password you set in .env.local
echo Backend log: backend\backend-run.log   Frontend log: frontend\frontend-run.log
echo To stop: run stop-services.bat
pause
