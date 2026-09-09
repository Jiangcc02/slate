@echo off
rem Module: platform-base / launcher
rem Type: Windows launcher script (ASCII only - cmd parser safe; see quickstart.md)
rem Responsibility: start backend slate-boot (auto-detect JDK17 and Maven); --check = env self-test only
rem Design doc: docs/design/platform-base/design.md (Chinese docs live in docs/, scripts stay ASCII)
rem Maintainer: coordinator / agent-fffabc
setlocal EnableDelayedExpansion
for %%i in ("%~dp0..") do set "SLATE_ROOT=%%~fi"

echo ============================================
echo   slate backend (slate-boot, port 8080)
echo ============================================

rem -- detect JDK 17: prefer local tools dir, fallback JAVA_HOME (must be 17+) --
set "JDK_HOME="
if exist "%USERPROFILE%\.zcode\tools\jdk-17.0.2\bin\java.exe" (
    set "JDK_HOME=%USERPROFILE%\.zcode\tools\jdk-17.0.2"
    goto jdk_done
)
if exist "%JAVA_HOME%\bin\java.exe" (
    set "JDK_HOME=%JAVA_HOME%"
    echo [hint] using system JAVA_HOME, make sure it is JDK 17+
    goto jdk_done
)
echo [ERROR] JDK 17 not found.
echo        Install: https://adoptium.net/temurin/releases/?version=17
echo        Or unzip it to: %USERPROFILE%\.zcode\tools\jdk-17.0.2
pause
exit /b 1
:jdk_done

rem -- detect Maven: PATH first, then local tools dir --
set "MVN_CMD="
where mvn >nul 2>nul
if not errorlevel 1 (
    set "MVN_CMD=mvn"
    goto mvn_done
)
if exist "%USERPROFILE%\.zcode\tools\apache-maven-3.9.16\bin\mvn.cmd" (
    set "MVN_CMD=%USERPROFILE%\.zcode\tools\apache-maven-3.9.16\bin\mvn.cmd"
    goto mvn_done
)
echo [ERROR] Maven not found.
echo        Install: https://maven.apache.org/download.cgi
echo        Or unzip it to: %USERPROFILE%\.zcode\tools\apache-maven-3.9.16
pause
exit /b 1
:mvn_done

echo [env] JDK:   %JDK_HOME%
echo [env] Maven: %MVN_CMD%

if /i "%~1"=="--check" (
    echo [check] environment OK, not starting - --check mode
    exit /b 0
)

set "JAVA_HOME=%JDK_HOME%"
set "Path=%JDK_HOME%\bin;%Path%"
cd /d "%SLATE_ROOT%\backend"
echo [build] mvn -pl slate-boot -am package -DskipTests  (first run downloads dependencies)
call "%MVN_CMD%" -q -pl slate-boot -am package -DskipTests
if errorlevel 1 (
    echo [ERROR] build failed, see maven output above.
    pause
    exit /b 1
)

set "BOOT_JAR="
for %%f in ("%SLATE_ROOT%\backend\slate-boot\target\slate-boot-*.jar") do set "BOOT_JAR=%%f"
if not defined BOOT_JAR (
    echo [ERROR] slate-boot jar not found, build may have failed.
    pause
    exit /b 1
)

echo [start] java -jar %BOOT_JAR%
"%JDK_HOME%\bin\java" -jar "%BOOT_JAR%"
echo.
echo [exit] backend stopped. Check the output above if this was unexpected.
pause
endlocal
