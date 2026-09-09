@echo off
rem Module: platform-base / launcher
rem Type: Windows launcher script (ASCII only - cmd parser safe; see quickstart.md)
rem Responsibility: start backend slate-boot (auto-detect JDK17 and Maven); --check = env self-test only
rem Design doc: docs/design/platform-base/design.md (Chinese docs live in docs/, scripts stay ASCII)
rem Maintainer: coordinator / agent-fffabc
rem Note: no EnableDelayedExpansion on purpose - "!" chars in user PATH would be eaten
setlocal
for %%i in ("%~dp0..") do set "SLATE_ROOT=%%~fi"

echo ============================================
echo   slate backend (slate-boot, port 8080)
echo ============================================

rem -- detect JDK 17 (installed on YOUR computer, not in this repo) --
rem order: SLATE_JDK17 env var (any unzip dir) > JAVA_HOME > machine-local convention dir (optional)
rem whatever is picked is then checked to actually BE major version 17+ (see jdk_picked)
set "JDK_HOME="
if defined SLATE_JDK17 (
    if exist "%SLATE_JDK17%\bin\java.exe" (
        set "JDK_HOME=%SLATE_JDK17%"
        goto jdk_picked
    )
)
if exist "%JAVA_HOME%\bin\java.exe" (
    set "JDK_HOME=%JAVA_HOME%"
    goto jdk_picked
)
if exist "%USERPROFILE%\.zcode\tools\jdk-17.0.2\bin\java.exe" (
    rem machine-local convenience dir, exists only on the coordinator PC - harmless elsewhere
    set "JDK_HOME=%USERPROFILE%\.zcode\tools\jdk-17.0.2"
    goto jdk_picked
)
echo [ERROR] JDK 17 not found on this computer.
echo        1) Install Temurin 17: https://adoptium.net/temurin/releases/?version=17
echo        2) Or unzip JDK 17 to any dir on your PC and set env var SLATE_JDK17 to it
pause
exit /b 1

:jdk_picked
rem a found java.exe is not enough (JDK 8 on JAVA_HOME would pass) - verify the version
for /f "usebackq tokens=3" %%v in (`"%JDK_HOME%\bin\java.exe" -version 2^>^&1 ^| findstr /i version`) do (
    for /f "tokens=1,2 delims=." %%x in ("%%~v") do (
        rem 17.0.20.1 style: first number is the major; 1.8.0_202 style: second number is
        if %%x geq 17 goto jdk_done
        if "%%x"=="1" if %%y geq 17 goto jdk_done
    )
)
echo [ERROR] java found but it is not JDK 17+ : %JDK_HOME%
echo        1) Install Temurin 17: https://adoptium.net/temurin/releases/?version=17
echo        2) Or unzip JDK 17 to any dir on your PC and set env var SLATE_JDK17 to it
pause
exit /b 1
:jdk_done

rem -- detect Maven (installed on YOUR computer, not in this repo) --
rem order: SLATE_M2 env var (any unzip dir) > machine-local convention dir (this PC only)
rem        > PATH (each candidate verified runnable; first working one wins)
rem the winner is stored as a FULL PATH - the build below must never re-resolve "mvn"
set "MVN_CMD="
if defined SLATE_M2 (
    if exist "%SLATE_M2%\bin\mvn.cmd" (
        set "MVN_CMD=%SLATE_M2%\bin\mvn.cmd"
        goto mvn_done
    )
)
if exist "%USERPROFILE%\.zcode\tools\apache-maven-3.9.16\bin\mvn.cmd" (
    rem machine-local convenience dir, exists only on the coordinator PC - harmless elsewhere
    set "MVN_CMD=%USERPROFILE%\.zcode\tools\apache-maven-3.9.16\bin\mvn.cmd"
    goto mvn_done
)
rem only executable extensions qualify - the extensionless "mvn" shell script must be skipped
for %%x in (mvn.cmd mvn.exe mvn.bat) do (
    if not defined MVN_CMD (
        for /f "delims=" %%f in ('where %%x 2^>nul') do (
            if not defined MVN_CMD (
                call "%%f" -version >nul 2>nul
                if not errorlevel 1 set "MVN_CMD=%%f"
            )
        )
    )
)
if defined MVN_CMD goto mvn_done
echo [ERROR] Maven not found on this computer.
echo        1) Install Maven and add its bin to PATH: https://maven.apache.org/download.cgi
echo        2) Or unzip Maven to any dir on your PC and set env var SLATE_M2 to it
pause
exit /b 1
:mvn_done

echo [env] JDK:   %JDK_HOME%
echo [env] Maven: %MVN_CMD%

if /i "%~1"=="--check" (
    echo [check] environment OK, not starting - --check mode
    exit /b 0
)

rem fail early when the port is taken - a running instance locks the jar and breaks repackage
netstat -ano | findstr /r /c:":8080 .*LISTENING" >nul 2>nul
if not errorlevel 1 (
    echo [ERROR] port 8080 is already in use - a previous slate backend is probably still running.
    echo        stop that instance first, then run this script again.
    pause
    exit /b 1
)

set "JAVA_HOME=%JDK_HOME%"
set "Path=%JDK_HOME%\bin;%Path%"
cd /d "%SLATE_ROOT%\backend"
echo [build] mvn -B -ntp -pl slate-boot -am package -DskipTests  (first run downloads dependencies)
call "%MVN_CMD%" -B -ntp -pl slate-boot -am package -DskipTests
if errorlevel 1 goto build_failed

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

:build_failed
echo [ERROR] build failed, mvn exit code %errorlevel%. See the maven output above.
echo [hint] maven used: %MVN_CMD%
for /f "delims=" %%f in ('where mvn 2^>nul') do echo [hint] where mvn also finds: %%f
echo [hint] JAVA_HOME is: %JAVA_HOME%
echo [hint] if there is no maven output above, report this whole window text.
pause
exit /b 1
