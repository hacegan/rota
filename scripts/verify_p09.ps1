#requires -Version 7.0
<#
.SYNOPSIS
  Rota P09 verifier: PostgreSQL persistence (compose file, persistence module, Flyway, Testcontainers).

.DESCRIPTION
  Needs Docker running: the Testcontainers tests start throwaway postgres containers (the first run
  pulls the postgres image). This script never starts, stops or deletes the compose stack, and
  never touches the rota-pgdata volume.

  Dot-sources scripts/env.ps1, then checks:
    layout     persistence module registered; pom, migrations V1-V4, compose file, .env.example,
               dev-up / dev-down / run-api-postgres scripts exist
    compose    no top-level "version:" key; image tag = VERSIONS.md; port 127.0.0.1:5432:5432;
               named volume rota-pgdata; healthcheck present; dev-down.ps1 has no -v/--volumes
    versions   Flyway (persistence pom), Testcontainers (persistence + bootstrap poms) and the
               test image constants match VERSIONS.md
    secrets    .env gitignored and not tracked; .env.example has POSTGRES_DB/USER/PASSWORD
    docker     docker info (daemon reachable, needed by Testcontainers)
    config     docker compose --env-file .env.example -f platform/compose/postgres.yml config --quiet
    build      scripts/build.ps1
    test       scripts/test.ps1 (all modules, including Testcontainers tests)
    persistence PostgresRosterRepositoryTest + PersistenceArchitectureTest reports
    wiring     bootstrap PostgresProfileApiTest + DefaultProfileWiringTest reports
    archunit   domain ArchitectureTest + application ApplicationArchitectureTest reports
               (no Spring, no Timefold, no JPA)

.EXAMPLE
  pwsh -NoProfile -File scripts/verify.ps1 p09
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'env.ps1')

$repoRoot = Split-Path -Parent $PSScriptRoot
$composeFile = Join-Path $repoRoot 'platform\compose\postgres.yml'
$envExample = Join-Path $repoRoot '.env.example'
$script:Results = [System.Collections.Generic.List[object]]::new()

function Add-Result {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][ValidateSet('OK', 'WARN', 'FAIL')][string]$Status,
        [string]$Detail = ''
    )
    $script:Results.Add([pscustomobject]@{ Name = $Name; Status = $Status; Detail = $Detail })
    $color = @{ OK = 'Green'; WARN = 'Yellow'; FAIL = 'Red' }[$Status]
    Write-Host ("{0,-4}  {1,-18} {2}" -f $Status, $Name, $Detail) -ForegroundColor $color
}

# Surefire .txt report check: returns $null when OK, otherwise a problem description.
function Test-SurefireReport {
    param(
        [Parameter(Mandatory)][string]$Module,
        [Parameter(Mandatory)][string]$Pattern
    )
    $report = Get-ChildItem (Join-Path $repoRoot "$Module\target\surefire-reports") -Filter $Pattern -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if (-not $report) { return "$Module $Pattern surefire report missing" }
    $text = Get-Content $report.FullName -Raw
    if ($text -notmatch 'Tests run:\s*([0-9]+)' -or [int]$Matches[1] -lt 1) { return "$Module $($report.Name): no tests ran" }
    if ($text -match 'Failures:\s*0' -and $text -match 'Errors:\s*0' -and $text -notmatch 'Skipped:\s*[1-9]') { return $null }
    return "$Module $($report.Name): " + (($text -replace '\r?\n', ' ').Trim())
}

function Get-FileText {
    param([Parameter(Mandatory)][string]$RelativePath)
    $path = Join-Path $repoRoot $RelativePath
    if (Test-Path $path -PathType Leaf) { return Get-Content $path -Raw }
    return $null
}

function Get-VersionsValue {
    param([Parameter(Mandatory)][string]$Pattern)
    foreach ($line in Get-Content (Join-Path $repoRoot 'VERSIONS.md')) {
        if ($line -match $Pattern) { return $Matches[1] }
    }
    return $null
}

Write-Host 'Rota P09 verify' -ForegroundColor Cyan
Write-Host ('Repository: {0}' -f (Get-Location).Path)
Write-Host 'Note: needs Docker running; the first run pulls the postgres image for Testcontainers.' -ForegroundColor Yellow
Write-Host ''

# --- Layout ---
$layoutProblems = @()
$rootPom = Get-FileText 'pom.xml'
foreach ($module in @('domain', 'solver', 'application', 'persistence', 'bootstrap')) {
    if ($rootPom -notmatch "<module>$module</module>") { $layoutProblems += "root pom missing <module>$module</module>" }
}
foreach ($file in @('persistence\pom.xml',
        'persistence\src\main\resources\db\migration\V1__create_roster.sql',
        'persistence\src\main\resources\db\migration\V2__create_assignment.sql',
        'persistence\src\main\resources\db\migration\V3__create_unassigned_seat.sql',
        'persistence\src\main\resources\db\migration\V4__create_roster_change.sql',
        'platform\compose\postgres.yml', '.env.example',
        'scripts\dev-up.ps1', 'scripts\dev-down.ps1', 'scripts\run-api-postgres.ps1')) {
    if (-not (Test-Path (Join-Path $repoRoot $file) -PathType Leaf)) { $layoutProblems += "$($file -replace '\\', '/') missing" }
}
foreach ($pure in @('domain', 'application')) {
    $pomText = Get-FileText "$pure\pom.xml"
    if ($pomText -match 'jakarta\.persistence|hibernate|flyway|spring-data') { $layoutProblems += "$pure/pom.xml references JPA/Flyway" }
}
if ($layoutProblems.Count -gt 0) { Add-Result -Name 'layout' -Status FAIL -Detail ($layoutProblems -join '; ') }
else { Add-Result -Name 'layout' -Status OK -Detail 'persistence module, migrations V1-V4, compose, scripts' }

# --- Compose file (static) ---
$postgresTag = Get-VersionsValue '^\|\s*`postgres`[^|]*\|\s*([^|\s]+)\s*\|'
$composeProblems = @()
$composeText = Get-FileText 'platform\compose\postgres.yml'
if ($null -eq $composeText) { $composeProblems += 'compose file missing' }
else {
    if ($composeText -match '(?m)^version\s*:') { $composeProblems += 'top-level "version:" key present (Compose v2: remove it)' }
    if (-not $postgresTag) { $composeProblems += 'VERSIONS.md has no postgres image tag' }
    elseif ($composeText -notmatch "(?m)^\s*image:\s*postgres:$([regex]::Escape($postgresTag))\s*$") { $composeProblems += "image is not postgres:$postgresTag" }
    if ($composeText -notmatch '127\.0\.0\.1:5432:5432') { $composeProblems += 'port is not 127.0.0.1:5432:5432' }
    if ($composeText -notmatch 'rota-pgdata:') { $composeProblems += 'named volume rota-pgdata missing' }
    if ($composeText -notmatch '(?m)^\s*healthcheck:') { $composeProblems += 'healthcheck missing' }
}
$downText = Get-FileText 'scripts\dev-down.ps1'
if ($downText) {
    $downCode = ($downText -split '\r?\n' | Where-Object { $_ -notmatch '^\s*#' -and $_ -notmatch '^\s*\.' }) -join "`n"
    if ($downCode -match 'compose[^\n]*\bdown\b[^\n]*(\s-v\b|--volumes)') { $composeProblems += 'dev-down.ps1 removes volumes' }
}
if ($composeProblems.Count -gt 0) { Add-Result -Name 'compose' -Status FAIL -Detail ($composeProblems -join '; ') }
else { Add-Result -Name 'compose' -Status OK -Detail "postgres:$postgresTag, 127.0.0.1:5432, rota-pgdata, healthcheck, no version key" }

# --- Versions ---
$flywayVersion = Get-VersionsValue '^\|\s*`org\.flywaydb:flyway-core`\s*\|\s*([^|\s]+)\s*\|'
$flywayPgVersion = Get-VersionsValue '^\|\s*`org\.flywaydb:flyway-database-postgresql`\s*\|\s*([^|\s]+)\s*\|'
$tcVersion = Get-VersionsValue '^\|\s*`org\.testcontainers:testcontainers`\s*\|\s*([^|\s]+)\s*\|'
$versionProblems = @()
$persistencePom = Get-FileText 'persistence\pom.xml'
$bootstrapPom = Get-FileText 'bootstrap\pom.xml'
function Test-PomVersion {
    param([string]$PomText, [string]$PomName, [string]$Artifact, [string]$Expected)
    if (-not $Expected) { return "VERSIONS.md has no version for $Artifact" }
    if ($PomText -notmatch "<artifactId>$([regex]::Escape($Artifact))</artifactId>\s*<version>([^<]+)</version>") { return "$PomName has no explicit $Artifact version" }
    if ($Matches[1] -ne $Expected) { return "$PomName $Artifact is $($Matches[1]), VERSIONS.md says $Expected" }
    return $null
}
foreach ($check in @(
        @($persistencePom, 'persistence/pom.xml', 'flyway-core', $flywayVersion),
        @($persistencePom, 'persistence/pom.xml', 'flyway-database-postgresql', $flywayPgVersion),
        @($persistencePom, 'persistence/pom.xml', 'testcontainers', $tcVersion),
        @($bootstrapPom, 'bootstrap/pom.xml', 'testcontainers', $tcVersion))) {
    $problem = Test-PomVersion -PomText $check[0] -PomName $check[1] -Artifact $check[2] -Expected $check[3]
    if ($null -ne $problem) { $versionProblems += $problem }
}
foreach ($testFile in @('persistence\src\test\java\dev\rota\persistence\PostgresRosterRepositoryTest.java',
        'bootstrap\src\test\java\dev\rota\bootstrap\PostgresProfileApiTest.java')) {
    $text = Get-FileText $testFile
    if ($null -eq $text) { $versionProblems += "$($testFile -replace '\\', '/') missing" }
    elseif ($postgresTag -and $text -notmatch "`"postgres:$([regex]::Escape($postgresTag))`"") {
        $versionProblems += "$(Split-Path $testFile -Leaf) does not use postgres:$postgresTag"
    }
}
if ($versionProblems.Count -gt 0) { Add-Result -Name 'versions' -Status FAIL -Detail ($versionProblems -join '; ') }
else { Add-Result -Name 'versions' -Status OK -Detail "flyway $flywayVersion, testcontainers $tcVersion, test image postgres:$postgresTag" }

# --- Secrets ---
$secretProblems = @()
$gitignore = Get-FileText '.gitignore'
if ($gitignore -notmatch '(?m)^/?\.env\s*$') { $secretProblems += '.env is not gitignored' }
$exampleText = Get-FileText '.env.example'
foreach ($key in @('POSTGRES_DB', 'POSTGRES_USER', 'POSTGRES_PASSWORD')) {
    if ($exampleText -notmatch "(?m)^$key=") { $secretProblems += ".env.example missing $key" }
}
if (Get-Command git -ErrorAction SilentlyContinue) {
    Push-Location $repoRoot
    try {
        & git ls-files --error-unmatch .env *> $null
        if ($LASTEXITCODE -eq 0) { $secretProblems += '.env is tracked by git' }
    }
    finally { Pop-Location }
}
if ($secretProblems.Count -gt 0) { Add-Result -Name 'secrets' -Status FAIL -Detail ($secretProblems -join '; ') }
else { Add-Result -Name 'secrets' -Status OK -Detail '.env ignored and untracked; .env.example has the 3 keys' }

# --- Docker ---
$dockerOk = $false
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Add-Result -Name 'docker' -Status FAIL -Detail 'docker not on PATH'
}
else {
    & docker info *> $null
    if ($LASTEXITCODE -eq 0) { $dockerOk = $true; Add-Result -Name 'docker' -Status OK -Detail 'daemon reachable' }
    else { Add-Result -Name 'docker' -Status FAIL -Detail "docker info exit $LASTEXITCODE (start Docker Desktop)" }
}

# --- docker compose config ---
if (Get-Command docker -ErrorAction SilentlyContinue) {
    & docker compose --env-file $envExample -f $composeFile config --quiet
    if ($LASTEXITCODE -eq 0) { Add-Result -Name 'config' -Status OK -Detail 'docker compose config --quiet (.env.example)' }
    else { Add-Result -Name 'config' -Status FAIL -Detail "docker compose config exit $LASTEXITCODE" }
}
else { Add-Result -Name 'config' -Status FAIL -Detail 'skipped (docker not on PATH)' }

# --- Build ---
& (Join-Path $PSScriptRoot 'build.ps1')
$buildExit = $LASTEXITCODE
if ($buildExit -eq 0) { Add-Result -Name 'build' -Status OK -Detail 'mvnw -q -DskipTests package' }
else { Add-Result -Name 'build' -Status FAIL -Detail "exit $buildExit" }

# --- Test (Testcontainers need Docker) ---
if ($buildExit -eq 0 -and $dockerOk) {
    & (Join-Path $PSScriptRoot 'test.ps1')
    $testExit = $LASTEXITCODE
    if ($testExit -eq 0) { Add-Result -Name 'test' -Status OK -Detail 'mvnw -q test' }
    else { Add-Result -Name 'test' -Status FAIL -Detail "exit $testExit" }
}
else { Add-Result -Name 'test' -Status FAIL -Detail 'skipped (build failed or Docker not reachable)' }

# --- Reports ---
$reportGroups = @(
    @{ Name = 'persistence'; Detail = 'Testcontainers round trips + architecture'; Checks = @(
            @('persistence', '*PostgresRosterRepositoryTest*.txt'), @('persistence', '*PersistenceArchitectureTest*.txt')) },
    @{ Name = 'wiring'; Detail = 'profile postgres via API; default stays in-memory'; Checks = @(
            @('bootstrap', '*PostgresProfileApiTest*.txt'), @('bootstrap', '*DefaultProfileWiringTest*.txt')) },
    @{ Name = 'archunit'; Detail = 'domain/application: no Spring, Timefold, JPA'; Checks = @(
            @('domain', '*.ArchitectureTest.txt'), @('application', '*ApplicationArchitectureTest*.txt')) }
)
foreach ($group in $reportGroups) {
    $problems = @()
    foreach ($check in $group.Checks) {
        $problem = Test-SurefireReport -Module $check[0] -Pattern $check[1]
        if ($null -ne $problem) { $problems += $problem }
    }
    if ($problems.Count -eq 0) { Add-Result -Name $group.Name -Status OK -Detail $group.Detail }
    else { Add-Result -Name $group.Name -Status FAIL -Detail ($problems -join '; ') }
}

# --- Summary ---
$failed = @($script:Results | Where-Object Status -eq 'FAIL')
$warned = @($script:Results | Where-Object Status -eq 'WARN')

Write-Host ''
Write-Host ("P09 verify: {0} OK, {1} WARN, {2} FAIL" -f ($script:Results.Count - $warned.Count - $failed.Count), $warned.Count, $failed.Count)

if ($failed.Count -gt 0) {
    Write-Host 'P09 verify FAILED.' -ForegroundColor Red
    exit 1
}

Write-Host 'P09 verify passed.' -ForegroundColor Green
exit 0
