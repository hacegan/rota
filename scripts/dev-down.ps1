#requires -Version 7.0
<#
.SYNOPSIS
  Stop the local PostgreSQL started by scripts/dev-up.ps1. Keeps the data.

.DESCRIPTION
  Runs docker compose down WITHOUT -v: the named volume rota-pgdata is never deleted, so the
  database survives. Deleting the volume is a destructive action and needs Samet's explicit ONAY
  (AGENTS.md); this script never does it.

  The compose file needs its variables only to parse; .env is used when present, otherwise
  .env.example (values are not used by "down").

.EXAMPLE
  pwsh -NoProfile -File scripts/dev-down.ps1
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'env.ps1')

$repoRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repoRoot 'platform\compose\postgres.yml'
$envFile = Join-Path $repoRoot '.env'
if (-not (Test-Path $envFile -PathType Leaf)) { $envFile = Join-Path $repoRoot '.env.example' }

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host 'docker not found on PATH.' -ForegroundColor Red
    exit 2
}

# No -v / --volumes: keep rota-pgdata.
& docker compose --env-file $envFile -f $composeFile down
if ($LASTEXITCODE -ne 0) {
    Write-Host "docker compose down failed (exit $LASTEXITCODE)." -ForegroundColor Red
    exit $LASTEXITCODE
}
Write-Host 'PostgreSQL stopped; volume rota-pgdata kept.' -ForegroundColor Green
exit 0
