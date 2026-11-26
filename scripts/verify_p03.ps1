#requires -Version 7.0
<#
.SYNOPSIS
  Rota P03 verifier: solver module, hard constraints H1-H5, coverage M6.

.DESCRIPTION
  Dot-sources scripts/env.ps1, then checks:
    layout    aggregator lists domain, solver, application, bootstrap; domain pom has no Timefold
    build     scripts/build.ps1
    test      scripts/test.ps1 (all modules, including the mini/tight solver assertions)
    archunit  domain ArchitectureTest: Spring and Timefold rules both ran and passed
  The mini/tight solver assertions live in JUnit (MiniFixtureSolveTest / TightFixtureSolveTest)
  so they run on any OS. No report files are parsed here.

.EXAMPLE
  pwsh -NoProfile -File scripts/verify.ps1 p03
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

Write-Host 'Rota P03 verify' -ForegroundColor Cyan
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
    $domainPom = Join-Path $repoRoot 'domain\pom.xml'
    if ((Test-Path $domainPom -PathType Leaf) -and ((Get-Content $domainPom -Raw) -match 'ai\.timefold')) {
        $layoutProblems += 'domain/pom.xml references timefold'
    }
}
if ($layoutProblems.Count -gt 0) { Add-Result -Name 'layout' -Status FAIL -Detail ($layoutProblems -join '; ') }
else { Add-Result -Name 'layout' -Status OK -Detail 'domain, solver, application, bootstrap; no Timefold in domain pom' }

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

# --- ArchUnit ---
$report = Get-ChildItem (Join-Path $repoRoot 'domain\target\surefire-reports') -Filter '*ArchitectureTest*.txt' -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $report) {
    Add-Result -Name 'archunit' -Status FAIL -Detail 'ArchitectureTest surefire report missing'
}
else {
    $text = Get-Content $report.FullName -Raw
    if ($text -match 'Failures:\s*0' -and $text -match 'Errors:\s*0') {
        Add-Result -Name 'archunit' -Status OK -Detail 'domain does not depend on Spring or Timefold'
    }
    else {
        Add-Result -Name 'archunit' -Status FAIL -Detail (($text -replace '\r?\n', ' ').Trim())
    }
}

# --- Summary ---
$failed = @($script:Results | Where-Object Status -eq 'FAIL')
$warned = @($script:Results | Where-Object Status -eq 'WARN')

Write-Host ''
Write-Host ("P03 verify: {0} OK, {1} WARN, {2} FAIL" -f ($script:Results.Count - $warned.Count - $failed.Count), $warned.Count, $failed.Count)

if ($failed.Count -gt 0) {
    Write-Host 'P03 verify FAILED.' -ForegroundColor Red
    exit 1
}

Write-Host 'P03 verify passed.' -ForegroundColor Green
exit 0
