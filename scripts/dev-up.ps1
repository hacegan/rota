#requires -Version 7.0
<#
.SYNOPSIS
  Start the local PostgreSQL (platform/compose/postgres.yml) and wait until it is healthy.

.DESCRIPTION
  Uses the repo-root .env (copy .env.example to .env first; never commit .env) through
  docker compose --env-file. Data lives in the named volume rota-pgdata and survives
  scripts/dev-down.ps1. The first run pulls the postgres image.

.PARAMETER TimeoutSeconds
  How long to wait for the container healthcheck. Default 120.

.EXAMPLE
  pwsh -NoProfile -File scripts/dev-up.ps1
#>
[CmdletBinding()]
param(
    [int]$TimeoutSeconds = 120
)

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'env.ps1')

$repoRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repoRoot 'platform\compose\postgres.yml'
$envFile = Join-Path $repoRoot '.env'
$container = 'rota-postgres'

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host 'docker not found on PATH.' -ForegroundColor Red
    exit 2
}
if (-not (Test-Path $envFile -PathType Leaf)) {
    Write-Host '.env not found in the repo root. Copy .env.example to .env and set POSTGRES_PASSWORD.' -ForegroundColor Red
    exit 2
}

& docker compose --env-file $envFile -f $composeFile up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "docker compose up failed (exit $LASTEXITCODE)." -ForegroundColor Red
    exit $LASTEXITCODE
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$health = ''
while ((Get-Date) -lt $deadline) {
    $health = (& docker inspect --format '{{.State.Health.Status}}' $container 2>$null | Out-String).Trim()
    if ($health -eq 'healthy') {
        Write-Host "$container is healthy on 127.0.0.1:5432." -ForegroundColor Green
        exit 0
    }
    Start-Sleep -Seconds 2
}

Write-Host "$container not healthy after $TimeoutSeconds s (last status: '$health'). See: docker logs $container" -ForegroundColor Red
exit 1
