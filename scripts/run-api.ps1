#requires -Version 7.0
<#
.SYNOPSIS
  Start the Rota REST API (bootstrap Spring Boot app) in the foreground.

.DESCRIPTION
  Dot-sources scripts/env.ps1 (machine JDK 27 on the session PATH), packages the modules with the
  Maven wrapper (tests skipped; never 'clean'), then runs the bootstrap jar with the repository's
  fixtures folder. Stop with Ctrl+C. Rosters are kept in memory and are lost when it stops.

  Endpoints (docs/contracts/openapi/rota.yaml):
    GET  /api/health
    POST /api/rosters/solve?fixture=mini|tight
    POST /api/rosters/repair?fixture=sick
    GET  /api/rosters/{id}

.PARAMETER Port
  HTTP port. Default 8080.

.PARAMETER SkipBuild
  Run the existing bootstrap jar without packaging first.

.EXAMPLE
  pwsh -NoProfile -File scripts/run-api.ps1
  Invoke-RestMethod -Method Post 'http://localhost:8080/api/rosters/solve?fixture=mini'
#>
[CmdletBinding()]
param(
    [int]$Port = 8080,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'env.ps1')

$repoRoot = Split-Path -Parent $PSScriptRoot

if (-not $SkipBuild) {
    & (Join-Path $repoRoot 'mvnw.cmd') -q -DskipTests package
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Build failed (exit $LASTEXITCODE)." -ForegroundColor Red
        exit $LASTEXITCODE
    }
}

$jar = Get-ChildItem (Join-Path $repoRoot 'bootstrap\target') -Filter 'rota-bootstrap-*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike '*.original' } |
    Select-Object -First 1
if (-not $jar) {
    Write-Host 'bootstrap\target\rota-bootstrap-*.jar not found; run without -SkipBuild.' -ForegroundColor Red
    exit 2
}

$fixtures = Join-Path $repoRoot 'fixtures'
Write-Host ("Starting {0} on http://localhost:{1} (fixtures: {2})" -f $jar.Name, $Port, $fixtures) -ForegroundColor Cyan
& java -jar $jar.FullName "--server.port=$Port" "--rota.fixtures-dir=$fixtures"
exit $LASTEXITCODE
