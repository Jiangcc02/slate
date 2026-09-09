@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion
rem 域/模块: 平台底座/启动脚本
rem 类型: Windows 启动脚本
rem 职责: 一键启动后端 slate-boot（自动探测 JDK17 与 Maven）；参数 --check 仅自检环境不启动
rem 设计文档: docs/design/平台底座/design.md
rem 维护者: 协调者 / agent-fffabc
for %%i in ("%~dp0..") do set "SLATE_ROOT=%%~fi"

echo ============================================
echo   slate 后端启动（slate-boot，端口 8080）
echo ============================================

rem ── JDK 17 探测：优先本机工具目录（确定 17），其次 JAVA_HOME（须自行保证 ≥17）──
set "JDK_HOME="
if exist "%USERPROFILE%\.zcode\tools\jdk-17.0.2\bin\java.exe" (
    set "JDK_HOME=%USERPROFILE%\.zcode\tools\jdk-17.0.2"
    goto :jdk_done
)
if exist "%JAVA_HOME%\bin\java.exe" (
    set "JDK_HOME=%JAVA_HOME%"
    echo [提示] 使用系统 JAVA_HOME，请自行确认为 JDK 17+（蓝图 §十三基线）
    goto :jdk_done
)
echo [错误] 未找到 JDK 17。
echo        安装指引: https://adoptium.net/temurin/releases/?version=17
echo        或解压到 %USERPROFILE%\.zcode\tools\jdk-17.0.2 后重试
exit /b 1
:jdk_done

rem ── Maven 探测：PATH 优先，其次本机工具目录 ──
set "MVN_CMD="
where mvn >nul 2>nul && set "MVN_CMD=mvn" && goto :mvn_done
if exist "%USERPROFILE%\.zcode\tools\apache-maven-3.9.16\bin\mvn.cmd" (
    set "MVN_CMD=%USERPROFILE%\.zcode\tools\apache-maven-3.9.16\bin\mvn.cmd"
    goto :mvn_done
)
echo [错误] 未找到 Maven。
echo        安装指引: https://maven.apache.org/download.cgi
echo        或解压到 %USERPROFILE%\.zcode\tools\apache-maven-3.9.16 后重试
exit /b 1
:mvn_done

echo [环境] JDK:   %JDK_HOME%
echo [环境] Maven: %MVN_CMD%

if /i "%~1"=="--check" (
    echo [自检] 环境就绪，未启动（--check 模式）
    exit /b 0
)

set "JAVA_HOME=%JDK_HOME%"
set "Path=%JDK_HOME%\bin;%Path%"
cd /d "%SLATE_ROOT%\backend"
echo [启动] mvn -pl slate-boot -am spring-boot:run （首次运行需下载依赖，请耐心）
call "%MVN_CMD%" -pl slate-boot -am spring-boot:run
endlocal
