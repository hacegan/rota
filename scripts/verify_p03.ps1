#requires -Version 7.0
<#
.SYNOPSIS
  Rota P03 verifier: solver module, hard constraints H1-H5, coverage M6.

.DESCRIPTION
  Dot-sources scripts/env.ps1, then checks:
    layout        aggregator lists domain, solver, application, bootstrap; domain pom has no Timefold
    build         scripts/build.ps1
    test          scripts/test.ps1 (all modules)
    archunit      domain ArchitectureTest: Spring and Timefold rules both ran and passed
    constraints   solver RotaConstraintProviderTest (ConstraintVerifier, H1-H5 + M6) passed
    timefold      solver TimefoldBehaviourTest (forEach / unassigned probe) passed
    mini          solver run on fixtures/mini: every seat assigned, 0 hard (D1)
    tight         solver run on fixtures/tight: 0 hard, unassigned seats listed with reasons (D1)
  The two solver runs are the MiniFixtureSolveTest / TightFixtureSolveTest test classes. They
  write solver/target/p03/<fixture>-report.txt, which this script re-parses and prints.
  No database, no Docker, no Spring wiring.

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

function Get-Suite {
    param([string]$Module, [string]$Class)
    $path = Join-Path $repoRoot "$Module\target\surefire-reports\TEST-$Class.xml"
    if (-not (Test-Path $path -PathType Leaf)) { return $null }
    return ([xml](Get-Content $path -Raw)).testsuite
}

# Returns $null when the suite passed and contains every required case, otherwise a reason string.
function Test-Suite {
    param([string]$Module, [string]$Class, [string[]]$RequiredCases = @())
    $suite = Get-Suite -Module $Module -Class $Class
    if (-not $suite) { return "surefire report missing: $Module TEST-$Class.xml" }
    $cases = @($suite.testcase)
    $names = @($cases | ForEach-Object { $_.name })
    $missing = @($RequiredCases | Where-Object { $req = $_; -not ($names | Where-Object { $_ -like "$req*" }) })
    if ($missing.Count -gt 0) { return "missing test(s): $($missing -join ', ')" }
    $bad = @($cases | Where-Object { $_.failure -or $_.error } | ForEach-Object { $_.name })
    if ($bad.Count -gt 0) { return "failed: $($bad -join ', ')" }
    if ([int]$suite.skipped -gt 0) { return "skipped: $($suite.skipped)" }
    if ([int]$suite.tests -eq 0) { return 'no tests ran' }
    return $null
}

function Read-Report {
    param([string]$Fixture)
    $path = Join-Path $repoRoot "solver\target\p03\$Fixture-report.txt"
    if (-not (Test-Path $path -PathType Leaf)) { return $null }
    $lines = @(Get-Content $path)
    $value = {
        param([string]$Key)
        $pattern = '^' + [regex]::Escape($Key) + ':\s*(-?\d+)\s*$'
        $hit = $lines | Where-Object { $_ -match $pattern } | Select-Object -First 1
        if ($null -eq $hit) { return $null }
        $null = $hit -match $pattern
        return [long]$Matches[1]
    }
    return [pscustomobject]@{
        Path            = $path
        Lines           = $lines
        Hard            = & $value 'hard'
        Medium          = & $value 'medium'
        Unassigned      = & $value 'unassigned'
        UnassignedLines = @($lines | Where-Object { $_ -like 'UNASSIGNED *' })
    }
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
    if ((Test-Path $domainPom -PathType Leaf) -and ((Get-Content $domainPom -Raw) -match 'timefold')) {
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
$why = Test-Suite -Module 'domain' -Class 'dev.rota.domain.ArchitectureTest' -RequiredCases @('domainMustNotDependOnSpring', 'domainMustNotDependOnTimefold')
if ($why) { Add-Result -Name 'archunit' -Status FAIL -Detail $why }
else { Add-Result -Name 'archunit' -Status OK -Detail 'domain does not depend on Spring or Timefold' }

# --- Constraint tests (ConstraintVerifier) ---
$constraintCases = @(
    'h1_penalizesUnqualifiedEmployee', 'h2_penalizesSeatOutsideAvailability', 'h2_penalizesSeatOutsideAvailabilityInTightDayFormat',
    'h3_penalizesOverlap', 'h4_penalizesBackToBackWithoutRest', 'h5_penalizesWeeklyExcess', 'm6_penalizesEmptySeat'
)
$why = Test-Suite -Module 'solver' -Class 'dev.rota.solver.RotaConstraintProviderTest' -RequiredCases $constraintCases
if ($why) { Add-Result -Name 'constraints' -Status FAIL -Detail $why }
else { Add-Result -Name 'constraints' -Status OK -Detail 'H1-H5 + M6 ConstraintVerifier tests passed' }

# --- Timefold behaviour probe ---
$why = Test-Suite -Module 'solver' -Class 'dev.rota.solver.TimefoldBehaviourTest' -RequiredCases @('forEachSkipsUnassignedSeats', 'forEachIncludingUnassignedSeesEmptySeats')
if ($why) { Add-Result -Name 'timefold-probe' -Status FAIL -Detail $why }
else { Add-Result -Name 'timefold-probe' -Status OK -Detail 'forEach skips empty seats; forEachIncludingUnassigned sees them' }

# --- Solver run: mini ---
$mini = Read-Report -Fixture 'mini'
$why = Test-Suite -Module 'solver' -Class 'dev.rota.solver.MiniFixtureSolveTest'
if (-not $mini) { Add-Result -Name 'solve-mini' -Status FAIL -Detail 'solver/target/p03/mini-report.txt missing' }
elseif ($mini.Hard -ne 0) { Add-Result -Name 'solve-mini' -Status FAIL -Detail "hard $($mini.Hard) (expected 0)" }
elseif ($mini.Unassigned -ne 0) { Add-Result -Name 'solve-mini' -Status FAIL -Detail "$($mini.Unassigned) unassigned seat(s) (expected 0)" }
elseif ($why) { Add-Result -Name 'solve-mini' -Status FAIL -Detail $why }
else { Add-Result -Name 'solve-mini' -Status OK -Detail 'every seat assigned, 0 hard' }

# --- Solver run: tight ---
$tight = Read-Report -Fixture 'tight'
$why = Test-Suite -Module 'solver' -Class 'dev.rota.solver.TightFixtureSolveTest'
if (-not $tight) { Add-Result -Name 'solve-tight' -Status FAIL -Detail 'solver/target/p03/tight-report.txt missing' }
elseif ($tight.Hard -ne 0) { Add-Result -Name 'solve-tight' -Status FAIL -Detail "hard $($tight.Hard) (expected 0)" }
elseif ($tight.Unassigned -lt 1) { Add-Result -Name 'solve-tight' -Status FAIL -Detail 'no unassigned seats reported (tight is infeasible by design)' }
elseif ($tight.UnassignedLines.Count -ne $tight.Unassigned) { Add-Result -Name 'solve-tight' -Status FAIL -Detail "unassigned: $($tight.Unassigned) but $($tight.UnassignedLines.Count) UNASSIGNED line(s)" }
elseif (@($tight.UnassignedLines | Where-Object { $_ -notmatch '\|\s*\S' }).Count -gt 0) { Add-Result -Name 'solve-tight' -Status FAIL -Detail 'unassigned seat without reasons' }
elseif ($why) { Add-Result -Name 'solve-tight' -Status FAIL -Detail $why }
else { Add-Result -Name 'solve-tight' -Status OK -Detail ("0 hard, medium {0}, {1} unassigned with reasons" -f $tight.Medium, $tight.Unassigned) }

# --- Reports ---
foreach ($report in @($mini, $tight)) {
    if (-not $report) { continue }
    Write-Host ''
    Write-Host ('--- {0} ---' -f $report.Path) -ForegroundColor Cyan
    $report.Lines | ForEach-Object { Write-Host $_ }
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
