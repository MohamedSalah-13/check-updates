@echo off
echo Deleting backup files starting with "main_account" older than 30 days...
echo Path: C:\Users\Mohamed\Desktop\accounts-backup
echo.

forfiles /P "C:\Users\Mohamed\Desktop\accounts-backup" /S /M main-account*.* /D -30 /C "cmd /c del @path" 2>nul

if %errorlevel% equ 0 (
    echo Cleanup completed successfully!
) else (
    echo No files found older than 30 days or an error occurred.
)

pause
