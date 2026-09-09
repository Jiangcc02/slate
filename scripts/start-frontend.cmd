@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
rem 域/模块: 平台底座/启动脚本
rem 类型: Windows 启动脚本
rem 职责: 一键启动前端管理端(5173)与学生端(5174)两个开发服务器；参数 --check 仅自检环境不启动
rem 设计文档: docs/design/平台底座/design.md
rem 维护者: 协调者 / agent-fffabc
for %%i in ("%~dp0..") do set "SLATE_ROOT=%%~fi"
set "FE_DIR=%SLATE_ROOT%\frontend"

echo ============================================
echo   slate 前端启动（管理端 5173 / 学生端 5174）
echo ============================================

rem ── Node ≥ 20 检查 ──
set "NODE_MAJOR="
for /f "tokens=1 delims=." %%v in ('node -v 2^>nul') do set "NODE_RAW=%%v"
if not defined NODE_RAW (
    echo [错误] 未找到 Node.js（需 20+）。
    echo        安装指引: https://nodejs.org/zh-cn/download
    exit /b 1
)
set "NODE_MAJOR=%NODE_RAW:v=%"
if %NODE_MAJOR% LSS 20 (
    echo [错误] Node 版本过低（当前 %NODE_RAW%，需 20+）
    exit /b 1
)

rem ── pnpm 检查 ──
where pnpm >nul 2>nul
if errorlevel 1 (
    echo [错误] 未找到 pnpm。安装任选其一：
    echo        corepack enable
    echo        npm install -g pnpm
    exit /b 1
)

echo [环境] Node: %NODE_RAW%   pnpm: 已安装

if /i "%~1"=="--check" (
    echo [自检] 环境就绪，未启动（--check 模式）
    exit /b 0
)

rem ── 依赖安装（首次）──
if not exist "%FE_DIR%\node_modules" (
    echo [准备] 首次运行：安装依赖 pnpm install
    pushd "%FE_DIR%" || exit /b 1
    call pnpm install || (popd & exit /b 1)
    popd
)

echo [启动] 两个独立窗口分别运行管理端与学生端，关闭窗口或 Ctrl+C 停止
start "slate 管理端 (5173)" /D "%FE_DIR%" cmd /k chcp 65001 ^>nul ^& pnpm --filter @slate/admin dev
start "slate 学生端 (5174)" /D "%FE_DIR%" cmd /k chcp 65001 ^>nul ^& pnpm --filter @slate/student dev
endlocal
