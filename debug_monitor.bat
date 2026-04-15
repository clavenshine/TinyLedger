@echo off
chcp 65001 >nul
echo ========================================
echo TinyLedger 调试监控工具
echo ========================================
echo.
echo 正在启动实时日志监控...
echo 按 Ctrl+C 停止监控
echo.
echo 监控内容：
echo - 通知监听服务 (TxNotifService)
echo - 短信接收器 (SmsReceiver)
echo - 短信解析器 (SmsTransactionParser)
echo - 错误和崩溃 (ERROR/FATAL)
echo ========================================
echo.

adb logcat ^| findstr /i "TxNotifService SmsReceiver SmsTransactionParser ERROR FATAL"

pause
