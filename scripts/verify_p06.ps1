#requires -Version 7.0
<#
.SYNOPSIS
  Rota P06 verifier: runnable REST API around the solver.

.DESCRIPTION
  Dot-sources scripts/env.ps1, then checks:
    layout    aggregator lists domain, solver, application, bootstrap; domain and application poms
              have no Timefold or Spring; contract docs/contracts/openapi/rota.yaml and
              scripts/run-api.ps1 exist
    build     scripts/build.ps1
    test      scripts/test.ps1 (all modules: P03-P05 solver tests, application UseCasesTest,
              bootstrap FixtureFilesTest and RosterApiTest)
    mockmvc   bootstrap RosterApiTest surefire report: tests ran, 0 failures, 0 errors
    archunit  domain ArchitectureTest and application ApplicationArchitectureTest: no Spring,
              no Timefold (application: also no solver/bootstrap)
  All endpoint assertions live in JUnit (MockMvc) so they run on any OS. No server is started.

.EXAMPLE
  pwsh -NoProfile -File scripts/verify.ps1 p06
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'env.ps1')

$repoRoot = Split-Path -Parent $PSScriptRoot
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
    if ($text -match 'Failures:\s*0' -and $text -match 'Errors:\s*0') { return $null }
    return "$Module $($report.Name): " + (($text -replace '\r?\n', ' ').Trim())
}

Write-Host 'Rota P06 verify' -ForegroundColor Cyan
Write-Host ('Repository: {0}' -f (Get-Location).Path)
Write-Host ''

# --- Module layout ---
$modules = @('domain', 'solver', 'application', 'bootstrap')
$rootPom = Join-Path $repoRoot 'pom.xml'
$layoutProblems = @()
if (-not (Test-Path $rootPom -PathType Leaf)) {
    $layoutProblems += 'root pom.xml missing'
}
else {
    $rootText = Get-Content $rootPom -Raw
    foreach ($module in $modules) {
        if ($rootText -notmatch "<module>$module</module>") { $layoutProblems += "root pom missing <module>$module</module>" }
        if (-not (Test-Path (Join-Path $repoRoot "$module\pom.xml") -PathType Leaf)) { $layoutProblems += "$module/pom.xml missing" }
    }
    foreach ($pure in @('domain', 'application')) {
        $pom = Join-Path $repoRoot "$pure\pom.xml"
        if (Test-Path $pom -PathType Leaf) {
            $pomText = Get-Content $pom -Raw
            if ($pomText -match 'ai\.timefold') { $layoutProblems += "$pure/pom.xml references timefold" }
            if ($pomText -match 'springframework') { $layoutProblems += "$pure/pom.xml references Spring" }
        }
    }
}
foreach ($file in @('docs\contracts\openapi\rota.yaml', 'scripts\run-api.ps1')) {
    if (-not (Test-Path (Join-Path $repoRoot $file) -PathType Leaf)) { $layoutProblems += "$file missing" }
}
if ($layoutProblems.Count -gt 0) { Add-Result -Name 'layout' -Status FAIL -Detail ($layoutProblems -join '; ') }
else { Add-Result -Name 'layout' -Status OK -Detail 'modules; pure domain/application poms; contract; run-api' }

# --- Build ---
& (Join-Path $PSScriptRoot 'build.ps1')
$buildExit = $LASTEXITCODE
if ($buildExit -eq 0) { Add-Result -Name 'build' -Status OK -Detail 'mvnw -q -DskipTests package' }
else { Add-Result -Name 'build' -Status FAIL -Detail "exit $buildExit" }

# --- Test ---
& (Join-Path $PSScriptRoot 'test.ps1')
$testExit = $LASTEXITCODE
if ($testExit -eq 0) { Add-Result -Name 'test' -Status OK -Detail 'mvnw -q test' }
else { Add-Result -Name 'test' -Status FAIL -Detail "exit $testExit" }

# --- MockMvc endpoint tests ---
$mockMvcProblem = Test-SurefireReport -Module 'bootstrap' -Pattern '*RosterApiTest*.txt'
if ($null -eq $mockMvcProblem) { Add-Result -Name 'mockmvc' -Status OK -Detail 'health, solve mini/tight, repair sick, get, 400/404' }
else { Add-Result -Name 'mockmvc' -Status FAIL -Detail $mockMvcProblem }

# --- ArchUnit ---
$archProblems = @()
foreach ($check in @(@('domain', '*ArchitectureTest*.txt'), @('application', '*ApplicationArchitectureTest*.txt'))) {
    $problem = Test-SurefireReport -Module $check[0] -Pattern $check[1]
    if ($null -ne $problem) { $archProblems += $problem }
}
if ($archProblems.Count -eq 0) { Add-Result -Name 'archunit' -Status OK -Detail 'domain and application do not depend on Spring or Timefold' }
else { Add-Result -Name 'archunit' -Status FAIL -Detail ($archProblems -join '; ') }

# --- Summary ---
$failed = @($script:Results | Where-Object Status -eq 'FAIL')
$warned = @($script:Results | Where-Object Status -eq 'WARN')

Write-Host ''
Write-Host ("P06 verify: {0} OK, {1} WARN, {2} FAIL" -f ($script:Results.Count - $warned.Count - $failed.Count), $warned.Count, $failed.Count)

if ($failed.Count -gt 0) {
    Write-Host 'P06 verify FAILED.' -ForegroundColor Red
    exit 1
}

Write-Host 'P06 verify passed.' -ForegroundColor Green
exit 0
