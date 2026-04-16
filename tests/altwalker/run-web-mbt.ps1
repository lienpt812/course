#Requires -Version 5.1
# AltWalker + Selenium targeting SPA at http://localhost:3000 (see fe_new/vite.config.ts).
# Prereq: API on :8000, Vite dev on :3000, pip install altwalker, Java 17+, Chrome.
param(
    [string] $Model = "",
    [switch] $Rebuild,
    [switch] $VerifyOnly
)

$ErrorActionPreference = "Stop"
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

if (-not $env:APP_BASE_URL) { $env:APP_BASE_URL = "http://localhost:3000" }
if (-not $env:API_BASE_URL) { $env:API_BASE_URL = "http://localhost:8000/api/v1" }
if (-not $env:API_HEALTH_URL) { $env:API_HEALTH_URL = "http://localhost:8000/health" }

Write-Host "APP_BASE_URL     = $($env:APP_BASE_URL)"
Write-Host "API_BASE_URL     = $($env:API_BASE_URL)"
Write-Host "API_HEALTH_URL   = $($env:API_HEALTH_URL)"

try {
    $r = Invoke-WebRequest -Uri $env:APP_BASE_URL -UseBasicParsing -TimeoutSec 5
    Write-Host "[OK] Web $($env:APP_BASE_URL) status $($r.StatusCode)"
} catch {
    Write-Host "[WARN] Web not reachable at $($env:APP_BASE_URL) - run: cd fe_new; npm run dev"
}

try {
    $h = Invoke-WebRequest -Uri $env:API_HEALTH_URL -UseBasicParsing -TimeoutSec 5
    Write-Host "[OK] API $($env:API_HEALTH_URL) status $($h.StatusCode)"
} catch {
    Write-Host "[WARN] API not reachable at $($env:API_HEALTH_URL)"
}

$splat = @{}
if ($Model) { $splat["Model"] = $Model }
if ($Rebuild) { $splat["Rebuild"] = $true }
if ($VerifyOnly) { $splat["VerifyOnly"] = $true }
& "$here\run.ps1" @splat
