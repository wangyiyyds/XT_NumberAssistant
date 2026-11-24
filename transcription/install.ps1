$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
py -3.11 -m venv .venv
if ($LASTEXITCODE -ne 0) { throw 'Python 3.11 环境创建失败' }
& "$PSScriptRoot\.venv\Scripts\python.exe" -m pip install -r requirements.txt --cache-dir "$PSScriptRoot\.pip-cache" --timeout 120 -i https://mirrors.aliyun.com/pypi/simple/
if ($LASTEXITCODE -ne 0) { throw 'FunASR 依赖安装失败' }
