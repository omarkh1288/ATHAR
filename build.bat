@echo off
REM Build script for AccessibilityMappingApp with Windows file locking workarounds

echo.
echo ========================================
echo  AccessibilityMappingApp Build Script
echo ========================================
echo.

REM Stop any running Gradle daemons
echo Stopping Gradle daemons...
call gradlew --stop >nul 2>&1

REM Kill any Java processes that might hold file locks
echo Killing Java processes...
taskkill /IM java.exe /F >nul 2>&1

REM Wait a moment for processes to terminate
timeout /t 2 /nobreak >nul

REM Run the build
echo.
echo Starting clean build...
echo.
call gradlew assembleDebug --console=plain

if %ERRORLEVEL% EQU 0 (
    echo.
    echo BUILD SUCCESSFUL!
    echo.
) else (
    echo.
    echo BUILD FAILED. Attempting recovery...
    echo.
    echo - Stopping daemons again
    call gradlew --stop >nul 2>&1
    taskkill /IM java.exe /F >nul 2>&1
    timeout /t 3 /nobreak >nul

    echo - Removing build directory
    rmdir /s /q "%USERPROFILE%\.gradle-local-build\AccessibilityMappingApp" >nul 2>&1
    timeout /t 2 /nobreak >nul

    echo - Retrying build
    call gradlew assembleDebug --console=plain
)

pause

