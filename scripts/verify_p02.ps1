#requires -Version 7.0
<#
.SYNOPSIS
  Rota P02 verifier: module layout, build, tests, ArchUnit rule.

.DESCRIPTION
  Checks the aggregator/module layout, runs scripts/build.ps1 and scripts/test.ps1, and
  confirms the ArchUnit rule (domain must not depend on Spring) ran and passed. No
  database, no Docker, no REST; Maven may download dependencies.

.EXAMPLE
  pwsh -NoProfile -File scripts/verify_p02.ps1
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

Write-Host 'Rota P02 verify' -ForegroundColor Cyan
Write-Host ('Repository: {0}' -f (Get-Location).Path)
Write-Host ''

# --- Module layout ---
$modules = @('domain', 'application', 'bootstrap')
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
}
if ($layoutProblems.Count -gt 0) {
    Add-Result -Name 'layout' -Status FAIL -Detail ($layoutProblems -join '; ')
}
else {
    Add-Result -Name 'layout' -Status OK -Detail 'aggregator + domain, application, bootstrap'
}

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

# --- ArchUnit rule ---
$report = Get-ChildItem (Join-Path $repoRoot 'domain\target\surefire-reports') -Filter '*ArchitectureTest*.txt' -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $report) {
    Add-Result -Name 'archunit' -Status FAIL -Detail 'ArchitectureTest surefire report missing'
}
else {
    $text = Get-Content $report.FullName -Raw
    if ($text -match 'Failures:\s*0' -and $text -match 'Errors:\s*0') {
        Add-Result -Name 'archunit' -Status OK -Detail 'domain does not depend on Spring'
    }
    else {
        Add-Result -Name 'archunit' -Status FAIL -Detail (($text -replace '\r?\n', ' ').Trim())
    }
}

# --- Summary ---
$failed = @($script:Results | Where-Object Status -eq 'FAIL')
$warned = @($script:Results | Where-Object Status -eq 'WARN')

Write-Host ''
Write-Host ("P02 verify: {0} OK, {1} WARN, {2} FAIL" -f ($script:Results.Count - $warned.Count - $failed.Count), $warned.Count, $failed.Count)

if ($failed.Count -gt 0) {
    Write-Host 'P02 verify FAILED.' -ForegroundColor Red
    exit 1
}

Write-Host 'P02 verify passed.' -ForegroundColor Green
exit 0
