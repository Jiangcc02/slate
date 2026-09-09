@echo off
rem Module: platform-base / launcher
rem Type: Windows launcher script (ASCII only - cmd parser safe; see quickstart.md)
rem Responsibility: start admin (5173) and student (5174) dev servers in two windows; --check = env self-test only
rem Design doc: docs/design/platform-base/design.md (Chinese docs live in docs/, scripts stay ASCII)
rem Maintainer: coordinator / agent-fffabc
setlocal EnableDelayedExpansion
for %%i in ("%~dp0..") do set "SLATE_ROOT=%%~fi"
set "FE_DIR=%SLATE_ROOT%\frontend"

echo ============================================
echo   slate frontend (admin 5173 / student 5174)
echo ============================================

rem -- Node >= 20 check --
set "NODE_MAJOR="
for /f "tokens=1 delims=." %%v in ('node -v 2^>nul') do set "NODE_RAW=%%v"
if not defined NODE_RAW (
    echo [ERROR] Node.js not found, need 20+.
    echo        Install: https://nodejs.org/en/download
    pause
    exit /b 1
)
set "NODE_MAJOR=%NODE_RAW:v=%"
if %NODE_MAJOR% LSS 20 (
    echo [ERROR] Node too old, current %NODE_RAW%, need 20+.
    pause
    exit /b 1
)

rem -- pnpm check --
where pnpm >nul 2>nul
if errorlevel 1 (
    echo [ERROR] pnpm not found. Install one of:
    echo        corepack enable
    echo        npm install -g pnpm
    pause
    exit /b 1
)

echo [env] Node: %NODE_RAW%   pnpm: OK

if /i "%~1"=="--check" (
    echo [check] environment OK, not starting - --check mode
    exit /b 0
)

rem -- install deps on first run --
if not exist "%FE_DIR%\node_modules" (
    echo [prepare] first run: pnpm install
    pushd "%FE_DIR%"
    call pnpm install
    if errorlevel 1 (
        popd
        echo [ERROR] pnpm install failed.
        pause
        exit /b 1
    )
    popd
)

echo [start] two windows will open, close them or Ctrl+C to stop
start "slate admin (5173)" /D "%FE_DIR%" cmd /k pnpm --filter @slate/admin dev
start "slate student (5174)" /D "%FE_DIR%" cmd /k pnpm --filter @slate/student dev
echo [done] dev servers launched in separate windows.
pause
endlocal
