@echo off
setlocal EnableExtensions

cd /d "%~dp0"

set "FORCE_REINSTALL="
if /I "%~1"=="--reinstall" set "FORCE_REINSTALL=1"

set "LOCAL_PROPERTIES=%CD%\local.properties"
set "SDK_DIR="
set "BACKEND_BASE_URL="

if exist "%LOCAL_PROPERTIES%" (
  for /f "usebackq tokens=1,* delims==" %%A in ("%LOCAL_PROPERTIES%") do (
    if /I "%%~A"=="sdk.dir" set "SDK_DIR=%%~B"
    if /I "%%~A"=="BACKEND_BASE_URL" set "BACKEND_BASE_URL=%%~B"
  )
)

if not defined SDK_DIR if defined ANDROID_SDK_ROOT set "SDK_DIR=%ANDROID_SDK_ROOT%"
if not defined SDK_DIR if defined ANDROID_HOME set "SDK_DIR=%ANDROID_HOME%"

if defined SDK_DIR (
  set "SDK_DIR=%SDK_DIR:\\=\%"
  set "SDK_DIR=%SDK_DIR:\:=:%"
)

if not defined SDK_DIR (
  echo Could not find Android SDK path. Set sdk.dir in local.properties or ANDROID_SDK_ROOT.
  exit /b 1
)

set "ADB_EXE=%SDK_DIR%\platform-tools\adb.exe"
set "APP_ID=com.athar.accessibilitymapping"
set "APP_ACTIVITY=com.athar.accessibilitymapping/.MainActivity"
set "BACKEND_PORT="

if not exist "%ADB_EXE%" (
  echo adb.exe was not found at "%ADB_EXE%".
  exit /b 1
)

if not defined BACKEND_BASE_URL (
  set "BACKEND_BASE_URL=http://127.0.0.1:8080/"
)

for /f "tokens=2 delims=:" %%P in ("%BACKEND_BASE_URL%") do (
  set "BACKEND_PORT=%%~P"
)
if defined BACKEND_PORT (
  for /f "tokens=1 delims=/" %%P in ("%BACKEND_PORT%") do set "BACKEND_PORT=%%~P"
)
if not defined BACKEND_PORT set "BACKEND_PORT=8080"

if defined FORCE_REINSTALL (
  echo Uninstalling existing %APP_ID% before reinstalling...
  "%ADB_EXE%" uninstall %APP_ID%
)

call .\gradlew.bat installDebug --console=plain
if errorlevel 1 (
  if not defined FORCE_REINSTALL (
    echo.
    echo Install failed. If you see INSTALL_FAILED_UPDATE_INCOMPATIBLE above, rerun with:
    echo   %~nx0 --reinstall
    echo.
    echo That will uninstall the existing %APP_ID% app from the device first and will clear its local app data.
  )
  exit /b %errorlevel%
)

echo %BACKEND_BASE_URL% | findstr /I /C:"127.0.0.1" /C:"localhost" >nul
if not errorlevel 1 (
  "%ADB_EXE%" reverse tcp:%BACKEND_PORT% tcp:%BACKEND_PORT%
  if errorlevel 1 exit /b %errorlevel%
)

"%ADB_EXE%" devices
"%ADB_EXE%" shell am start -n %APP_ACTIVITY%
if errorlevel 1 exit /b %errorlevel%

echo.
echo App installed and launched successfully.
endlocal
