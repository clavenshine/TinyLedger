@echo off
cd /d C:\MyProject\opencode-xiaoxiaojizhangben\TinyLedger_Project_20260406_211505
call gradlew.bat assembleDebug 2>&1
echo BUILD_EXIT_CODE=%ERRORLEVEL%
