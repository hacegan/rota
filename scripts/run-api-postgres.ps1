#requires -Version 7.0
<#
.SYNOPSIS
  Start the Rota REST API with Spring profile "postgres" (rosters stored in PostgreSQL).

.DESCRIPTION
  Dot-sources scripts/env.ps1, loads POSTGRES_* from the repo-root .env into this process only
  (never printed, never written anywhere), packages the modules with the Maven wrapper (tests
  skipped; never 'clean') unless -SkipBuild, then runs the bootstrap jar with
  --spring.profiles.active=postgres. Start the database first: scripts/dev-up.ps1.
  Flyway migrates the schema on startup. Stop with Ctrl+C.

.PARAMETER Port
  HTTP port. Default 8080.

.PARAMETER SkipBuild
  Run the existing bootstrap jar without packaging first.

.EXAMPLE
  pwsh -NoProfile -File scripts/dev-up.ps1
  pwsh -NoProfile -File scripts/run-api-postgres.ps1
#>
[CmdletBinding()]
param(
    [int]$Port = 8080,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'env.ps1')

$repoRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repoRoot '.env'

if (-not (Test-Path $envFile -PathType Leaf)) {
    Write-Host '.env not found in the repo root. Copy .env.example to .env and set POSTGRES_PASSWORD.' -ForegroundColor Red
    exit 2
}

# KEY=VALUE lines; blank lines and # comments ignored. Process scope only.
foreach ($line in Get-Content $envFile) {
    $trimmed = $line.Trim()
    if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }
    $eq = $trimmed.IndexOf('=')
    if ($eq -lt 1) { continue }
    $name = $trimmed.Substring(0, $eq).Trim()
    $value = $trimmed.Substring($eq + 1).Trim().Trim('"')
    if ($name -like 'POSTGRES_*') {
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
}
foreach ($required in @('POSTGRES_DB', 'POSTGRES_USER', 'POSTGRES_PASSWORD')) {
    if (-not [Environment]::GetEnvironmentVariable($required, 'Process')) {
        Write-Host "$required is not set in .env." -ForegroundColor Red
        exit 2
    }
}

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
Write-Host ("Starting {0} (profile postgres, database {1} on localhost:5432) on http://localhost:{2}" -f `
        $jar.Name, $env:POSTGRES_DB, $Port) -ForegroundColor Cyan
& java -jar $jar.FullName "--server.port=$Port" "--rota.fixtures-dir=$fixtures" '--spring.profiles.active=postgres'
exit $LASTEXITCODE
