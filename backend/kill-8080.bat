@echo off
REM 强制结束占用 8080 端口的进程（ IDEA Before launch 兜底用，孤儿进程也杀得掉）
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080 " ^| findstr "LISTENING"') do (
    taskkill /F /PID %%a >nul 2>&1
)
exit /b 0
