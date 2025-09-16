@echo off
chcp 65001 >nul
cd /d "%~dp0target"
if exist installer rmdir /s /q installer
cd /d "%~dp0target\installer\KeyMouseShare"
echo 正在启动 KeyMouseShare 应用程序...
KeyMouseShare.exe
echo 程序已退出，退出代码: %ERRORLEVEL%
pause