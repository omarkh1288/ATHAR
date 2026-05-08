@echo off
setlocal EnableExtensions

cd /d "%~dp0"

call .\gradlew.bat :backend:run --console=plain

endlocal
