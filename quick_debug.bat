@echo off
chcp 65001 >nul
echo ========================================
echo TinyLedger 快速调试工具
echo ========================================
echo.

:menu
echo 请选择操作：
echo.
echo 1. 查看应用状态
echo 2. 启动实时日志监控
echo 3. 重启应用
echo 4. 强制停止应用
echo 5. 查看通知服务状态
echo 6. 导出完整日志
echo 7. 清除应用数据
echo 0. 退出
echo.
set /p choice=请输入选项 (0-7): 

if "%choice%"=="1" goto status
if "%choice%"=="2" goto monitor
if "%choice%"=="3" goto restart
if "%choice%"=="4" goto stop
if "%choice%"=="5" goto notifstatus
if "%choice%"=="6" goto exportlog
if "%choice%"=="7" goto cleardata
if "%choice%"=="0" goto end
echo 无效选项，请重新输入
echo.
goto menu

:status
echo.
echo ========================================
echo 应用状态
echo ========================================
adb shell ps ^| findstr tinyledger
echo.
adb shell dumpsys package com.tinyledger.app ^| findstr "versionName versionCode"
echo.
goto menu

:monitor
echo.
echo 启动实时日志监控...
echo 按 Ctrl+C 返回菜单
echo.
adb logcat ^| findstr /i "TxNotifService SmsReceiver SmsTransactionParser ERROR FATAL"
goto menu

:restart
echo.
echo 正在重启应用...
adb shell am force-stop com.tinyledger.app
timeout /t 2 /nobreak >nul
adb shell am start -n com.tinyledger.app/.MainActivity
echo 应用已重启
echo.
goto menu

:stop
echo.
echo 正在停止应用...
adb shell am force-stop com.tinyledger.app
echo 应用已停止
echo.
goto menu

:notifstatus
echo.
echo ========================================
echo 通知监听服务状态
echo ========================================
adb shell dumpsys notification ^| findstr /i "tinyledger"
echo.
goto menu

:exportlog
echo.
echo 正在导出日志...
set timestamp=%date:~0,4%%date:~5,2%%date:~8,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set timestamp=%timestamp: =0%
set logfile=logcat_%timestamp%.txt
adb logcat -d > "%logfile%"
echo 日志已导出到: %logfile%
echo.
goto menu

:cleardata
echo.
echo 警告：这将清除应用的所有数据！
set /p confirm=确认清除？(y/n): 
if /i "%confirm%"=="y" (
    adb shell pm clear com.tinyledger.app
    echo 应用数据已清除
    echo 需要重新配置设置
) else (
    echo 操作已取消
)
echo.
goto menu

:end
echo.
echo 感谢使用！
echo.
exit /b 0
