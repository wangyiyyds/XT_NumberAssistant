#!/usr/bin/env pwsh
<#
.SYNOPSIS
    本地开发一键启动：数据库 -> .env -> 依赖 -> 后端（8080）。

.EXAMPLE
    ./scripts/dev.ps1

.EXAMPLE
    ./scripts/dev.ps1 -SkipDb -SkipInstall -NoReload
#>
[CmdletBinding()]
param(
    [switch]$SkipDb,
    [switch]$SkipInstall,
    [switch]$NoReload,
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Wait-Database {
    param([int]$Attempts = 30)

    for ($i = 1; $i -le $Attempts; $i++) {
        docker compose exec -T postgres pg_isready -U xianting -d xianting_chat *> $null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "数据库已就绪。" -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "数据库在 $($Attempts * 2) 秒内未就绪，请检查: docker compose logs postgres"
}

if (-not (Get-Command uv -ErrorAction SilentlyContinue)) {
    throw "未找到 uv，请先安装: https://docs.astral.sh/uv/"
}

if (-not $SkipDb) {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "未找到 docker，请先安装 Docker Desktop。"
    }
    Write-Host "启动 PostgreSQL ..." -ForegroundColor Cyan
    docker compose up -d
    Wait-Database
}

if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    Write-Host "已从 .env.example 生成 .env，请填写 DEEPSEEK_API_KEY（聊天接口需要）。" -ForegroundColor Yellow
}

if (-not $SkipInstall) {
    Write-Host "同步 Python 依赖 ..." -ForegroundColor Cyan
    uv sync
}

$uvicornArgs = @(
    "run", "uvicorn", "app.main:app",
    "--host", "0.0.0.0",
    "--port", "$Port"
)
if (-not $NoReload) {
    $uvicornArgs += "--reload"
}

Write-Host "启动后端 http://localhost:$Port （文档: /docs，别名: /swagger-ui.html）" -ForegroundColor Cyan
uv @uvicornArgs