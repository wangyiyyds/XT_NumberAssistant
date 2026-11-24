$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
# 如需抖音 Cookie，请在启动前设置 YTDLP_COOKIES_FILE 指向 Netscape cookies.txt。
# 不默认直读 Edge/Chrome，因为新版浏览器的 App-Bound 加密可能导致 DPAPI 解密失败。
& "$PSScriptRoot\.venv\Scripts\python.exe" "$PSScriptRoot\server.py"
