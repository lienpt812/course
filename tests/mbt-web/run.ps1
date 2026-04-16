#Requires -Version 5.1
# MBT Web Test Runner — AltWalker + Selenium + C#/.NET
# Usage:
#   .\run.ps1                         # chạy tất cả 8 model
#   .\run.ps1 -Model Auth             # chạy một model
#   .\run.ps1 -Rebuild                # build lại trước khi chạy
#   .\run.ps1 -VerifyOnly             # chỉ verify model vs code (không chạy browser)
#   .\run.ps1 -Headless               # chạy Chrome headless
#   .\run.ps1 -Model Auth -Headless -Rebuild

param(
    [string]  $Model       = "",
    [string]  $Generator   = "quick_random(edge_coverage(100))",
    [switch]  $Rebuild,
    [switch]  $VerifyOnly,
    [switch]  $Headless,
    [string]  $JavaHome    = "",
    [string]  $AppBaseUrl  = "",
    [string]  $ApiBaseUrl  = "",
    [string]  $HealthUrl   = ""
)

$ErrorActionPreference = "Stop"
# Force UTF-8 cho Python/AltWalker reporter (tranh loi UnicodeEncodeError tren Windows CP1252)
$env:PYTHONUTF8    = "1"
$env:PYTHONIOENCODING = "utf-8"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
chcp 65001 | Out-Null
$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$ModelsDir  = Join-Path $ScriptDir "models"
$ProjectDir = Join-Path $ScriptDir "MbtWeb.csproj"
$BinDir     = Join-Path $ScriptDir "bin"
$DLL        = Join-Path $BinDir "mbt-web.dll"

# ── Env vars ──────────────────────────────────────────────────────────────────
if ($AppBaseUrl)  { $env:APP_BASE_URL  = $AppBaseUrl }
if ($ApiBaseUrl)  { $env:API_BASE_URL  = $ApiBaseUrl }
if ($HealthUrl)   { $env:API_HEALTH_URL = $HealthUrl }
if ($Headless)    { $env:HEADLESS       = "1" }

# ── Java 17 ───────────────────────────────────────────────────────────────────
if ($JavaHome) {
    $env:JAVA_HOME = $JavaHome
    $env:PATH = "$JavaHome\bin;" + $env:PATH
    Write-Host "[0] Using Java: $JavaHome" -ForegroundColor Cyan
} else {
    $found = Get-Item "C:\Program Files\Eclipse Adoptium\jdk-17*" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) {
        $env:JAVA_HOME = $found.FullName
        $env:PATH = "$($found.FullName)\bin;" + $env:PATH
        Write-Host "[0] Java 17: $($found.FullName)" -ForegroundColor Green
    } else {
        Write-Host "[0] WARNING: Java 17 not found in default path." -ForegroundColor Yellow
    }
}

# ── Clear ports ───────────────────────────────────────────────────────────────
function Clear-Ports {
    param([int[]] $Ports)
    $seen = [System.Collections.Generic.HashSet[int]]::new()
    $lines = netstat -ano 2>$null
    if (-not $lines) { return }
    foreach ($line in $lines) {
        if ($line -notmatch 'LISTENING') { continue }
        $hit = $false
        foreach ($p in $Ports) { if ($line -match ":$p\s") { $hit = $true; break } }
        if (-not $hit) { continue }
        $parts = ($line -split '\s+') | Where-Object { $_ -ne '' }
        $pid   = $parts[-1]
        if ($pid -match '^\d+$' -and [int]$pid -gt 0) { [void]$seen.Add([int]$pid) }
    }
    foreach ($id in $seen) {
        Write-Host "  Kill PID $id (port $Ports)" -ForegroundColor DarkGray
        taskkill /PID $id /F 2>$null | Out-Null
    }
    if ($seen.Count -gt 0) { Start-Sleep -Milliseconds 800 }
}

Write-Host "[1] Clearing AltWalker ports (5000, 8887)..." -ForegroundColor Cyan
Clear-Ports -Ports @(5000, 8887)

# ── Build ─────────────────────────────────────────────────────────────────────
if ($Rebuild -or -not (Test-Path $DLL)) {
    Write-Host "[2] Building project..." -ForegroundColor Cyan
    dotnet publish $ProjectDir -c Release -o $BinDir --nologo -v minimal
    if ($LASTEXITCODE -ne 0) { Write-Host "BUILD FAILED" -ForegroundColor Red; exit 1 }
    Write-Host "[2] Build OK" -ForegroundColor Green
} else {
    Write-Host "[2] Using existing build (use -Rebuild to force)" -ForegroundColor Gray
}

# ── Select models ─────────────────────────────────────────────────────────────
if ($Model) {
    $ModelFiles = @(Get-Item (Join-Path $ModelsDir "$Model.json") -ErrorAction SilentlyContinue)
    if ($ModelFiles.Count -eq 0) {
        $ModelFiles = @(Get-ChildItem (Join-Path $ModelsDir "*$Model*.json"))
    }
    if ($ModelFiles.Count -eq 0) {
        Write-Host "Model '$Model' not found in $ModelsDir" -ForegroundColor Red; exit 1
    }
} else {
    $ModelFiles = @(Get-ChildItem (Join-Path $ModelsDir "*.json"))
}

Write-Host "[3] Models: $($ModelFiles.Count)" -ForegroundColor Cyan
$ModelFiles | ForEach-Object { Write-Host "    - $($_.Name)" -ForegroundColor Gray }

# ── Run ───────────────────────────────────────────────────────────────────────
$Passed = 0
$Failed = 0

foreach ($mf in $ModelFiles) {
    Clear-Ports -Ports @(5000, 8887)

    Write-Host ""
    Write-Host ">>> $($mf.Name)" -ForegroundColor Yellow

    if ($VerifyOnly) {
        $out = altwalker verify $DLL -l dotnet -m $mf.FullName 2>&1
    } else {
        $out = altwalker online $DLL -l dotnet -m $mf.FullName $Generator 2>&1
    }
    $out | ForEach-Object { Write-Host "  $_" }

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  PASS: $($mf.Name)" -ForegroundColor Green
        $Passed++
    } else {
        Write-Host "  FAIL: $($mf.Name)" -ForegroundColor Red
        $Failed++
    }
}

Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "  PASSED: $Passed  FAILED: $Failed  TOTAL: $($Passed + $Failed)" `
    -ForegroundColor $(if ($Failed -eq 0) { "Green" } else { "Red" })
Write-Host "================================" -ForegroundColor Cyan

if ($Failed -gt 0) { exit 1 }
