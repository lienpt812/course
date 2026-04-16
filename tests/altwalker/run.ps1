# AltWalker Test Runner
# Usage: .\run.ps1 [-Model Test_Auth] [-Generator "random(vertex_coverage(100))"] [-Rebuild] [-VerifyOnly] [-ResetSeed]
# Example: .\run.ps1 -Model Test_Auth
# Example: .\run.ps1 (runs all models)
#
# Frees TCP ports 5000 (AltWalker .NET host) and 8887 (GraphWalker REST) before run — fixes "Address already in use: bind".

param(
    [string]$Model      = "",
    [string]$Generator  = "random(edge_coverage(100))",
    [switch]$VerifyOnly,
    [switch]$Rebuild,
    [switch]$ResetSeed,
    [string]$JavaHome   = ""
)

$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$DLL        = "$ScriptDir\bin\tests.dll"
$ModelsDir  = "$ScriptDir\models"
$ProjectDir = "$ScriptDir\tests"
$SeedFlag   = "$env:TEMP\altwalker_seed_done.flag"

# Ports used by AltWalker CLI: executor listens on 5000, embedded GraphWalker REST on 8887
$AltWalkerPorts = @(5000, 8887)

function Stop-ListenersOnPorts {
    param([int[]] $Ports)
    $seen = [System.Collections.Generic.HashSet[int]]::new()
    $netstat = netstat -ano 2>$null
    if (-not $netstat) { return }
    foreach ($line in $netstat) {
        if ($line -notmatch 'LISTENING') { continue }
        $hit = $false
        foreach ($p in $Ports) {
            if ($line -match ":$p\s") { $hit = $true; break }
        }
        if (-not $hit) { continue }
        $parts = ($line -split '\s+') | Where-Object { $_ -ne '' }
        if ($parts.Count -lt 2) { continue }
        $procId = $parts[-1]
        if ($procId -match '^\d+$' -and [int]$procId -gt 0) {
            [void]$seen.Add([int]$procId)
        }
    }
    foreach ($id in $seen) {
        Write-Host "  (free port) taskkill /PID $id /F" -ForegroundColor DarkGray
        taskkill /PID $id /F 2>$null | Out-Null
    }
    if ($seen.Count -gt 0) {
        Start-Sleep -Milliseconds 800
    }
}

# ── Step 0: Java 17 ──────────────────────────────────────────────────────────
if ($JavaHome -ne "") {
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
        Write-Host "[0] WARNING: Java 17 not found. GraphWalker may fail." -ForegroundColor Yellow
    }
}

# ── Step 1: Free AltWalker / GraphWalker ports ─────────────────────────────
Write-Host "[1] Clearing ports $($AltWalkerPorts -join ', ') (AltWalker + GraphWalker)..." -ForegroundColor Cyan
Stop-ListenersOnPorts -Ports $AltWalkerPorts

# ── Step 1b: Reset seed flag ──────────────────────────────────────────────────
if ($ResetSeed -or $Rebuild) {
    if (Test-Path $SeedFlag) {
        Remove-Item $SeedFlag -Force
        Write-Host "[1b] Seed flag cleared - data will be re-seeded" -ForegroundColor Yellow
    }
}

# ── Step 2: Build ─────────────────────────────────────────────────────────────
if ($Rebuild -or -not (Test-Path $DLL)) {
    Write-Host "[2] Building project..." -ForegroundColor Cyan
    dotnet publish "$ProjectDir\tests.csproj" -c Release -o "$ScriptDir\bin" --nologo -v minimal
    if ($LASTEXITCODE -ne 0) { Write-Host "Build FAILED" -ForegroundColor Red; exit 1 }
    Write-Host "[2] Build OK" -ForegroundColor Green
} else {
    Write-Host "[2] Using existing build (use -Rebuild to force)" -ForegroundColor Gray
}

# ── Step 2b: Sync and fix model files ─────────────────────────────────────────
Write-Host "[2b] Syncing model files..." -ForegroundColor Cyan
$syncPy = Join-Path $ScriptDir "sync_models.py"
if (Test-Path $syncPy) {
    & python $syncPy
    if ($LASTEXITCODE -ne 0) { Write-Host "[2b] WARNING: sync_models.py failed" -ForegroundColor Yellow }
} else {
    Write-Host "[2b] sync_models.py not found, skip" -ForegroundColor Yellow
}

# ── Step 3: Select models ─────────────────────────────────────────────────────
if ($Model -ne "") {
    $ModelFiles = @(Get-Item "$ModelsDir\$Model.json" -ErrorAction SilentlyContinue)
    if ($ModelFiles.Count -eq 0) {
        $ModelFiles = @(Get-ChildItem "$ModelsDir\*$Model*.json")
    }
    if ($ModelFiles.Count -eq 0) {
        Write-Host "Model '$Model' not found in $ModelsDir" -ForegroundColor Red
        exit 1
    }
} else {
    $ModelFiles = @(Get-ChildItem "$ModelsDir\*.json")
}

Write-Host "[3] Models: $($ModelFiles.Count)" -ForegroundColor Cyan
$ModelFiles | ForEach-Object { Write-Host "    - $($_.Name)" -ForegroundColor Gray }

# ── Step 4: Run ───────────────────────────────────────────────────────────────
$Passed = 0
$Failed = 0

foreach ($mf in $ModelFiles) {
    # Release ports so each `altwalker online` can bind GraphWalker again (stale Java from prior run / other tools)
    Stop-ListenersOnPorts -Ports $AltWalkerPorts

    Write-Host ""
    Write-Host ">>> $($mf.Name)" -ForegroundColor Yellow

    if ($VerifyOnly) {
        $result = altwalker verify $DLL -l dotnet -m $mf.FullName 2>&1
    } else {
        $result = altwalker online $DLL -l dotnet -m $mf.FullName $Generator 2>&1
    }

    $result | ForEach-Object { Write-Host "  $_" }

    if ($LASTEXITCODE -eq 0) {
        Write-Host "  PASS: $($mf.Name)" -ForegroundColor Green
        $Passed++
    } else {
        Write-Host "  FAIL: $($mf.Name)" -ForegroundColor Red
        $Failed++
    }
}

# ── Summary ───────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "================================" -ForegroundColor Cyan
Write-Host "  PASSED: $Passed  FAILED: $Failed  TOTAL: $($Passed+$Failed)" -ForegroundColor $(if ($Failed -eq 0) { "Green" } else { "Red" })
Write-Host "================================" -ForegroundColor Cyan
