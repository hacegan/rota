#requires -Version 7.0
<#
.SYNOPSIS
  Rota P08 verifier: browser E2E demo smoke (Playwright, Chromium) against the real API and web app.

.DESCRIPTION
  NOTE: the FIRST run downloads packages and a browser:
    - npm packages into web/node_modules (if not there yet) and e2e/node_modules (@playwright/test);
    - the Chromium build for Playwright (about 150-200 MB) into the user's Playwright cache
      (%LOCALAPPDATA%\ms-playwright on Windows). Not into the repository.
  It needs network access and can take several minutes. Later runs reuse both.

  Dot-sources scripts/env.ps1, then:
    layout    e2e/package.json, playwright.config.ts, tests/demo.spec.ts, .gitignore exist
    versions  e2e/package.json has exactly @playwright/test, pinned to the VERSIONS.md version
    tools     node, npm, npx on PATH
    ports     8080 (API) and 3000 (web) are free, so the run uses freshly built servers
    build     scripts/build.ps1 (bootstrap jar for run-api.ps1 -SkipBuild)
    web-npm   npm ci in web/
    e2e-npm   npm ci in e2e/
    browser   npx playwright install chromium
    e2e       npx playwright test (CI=1). playwright.config.ts starts the API
              (scripts/run-api.ps1 -SkipBuild) and the web app (next build + next start), runs
              3 tests, and stops both
    screenshot docs/demo/rota-demo.png written by this run
  Never deletes node_modules, .next, target or the browser cache.

.EXAMPLE
  pwsh -NoProfile -File scripts/verify.ps1 p08
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'env.ps1')

$repoRoot = Split-Path -Parent $PSScriptRoot
$webDir = Join-Path $repoRoot 'web'
$e2eDir = Join-Path $repoRoot 'e2e'
$screenshot = Join-Path $repoRoot 'docs\demo\rota-demo.png'
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

# *.cmd on Windows avoids npm.ps1/npx.ps1 and the execution policy.
$npm = if ($IsWindows) { 'npm.cmd' } else { 'npm' }
$npx = if ($IsWindows) { 'npx.cmd' } else { 'npx' }

# Runs a tool in a folder; its output goes to the console, only the exit code is returned.
function Invoke-In {
    param(
        [Parameter(Mandatory)][string]$Directory,
        [Parameter(Mandatory)][string]$Tool,
        [Parameter(Mandatory)][string[]]$Arguments
    )
    Push-Location $Directory
    try {
        & $Tool @Arguments | Out-Host
        return $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
}

function Test-PortInUse {
    param([Parameter(Mandatory)][int]$Port)
    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $connect = $client.ConnectAsync('127.0.0.1', $Port)
        return $connect.Wait(500) -and $client.Connected
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

Write-Host 'Rota P08 verify' -ForegroundColor Cyan
Write-Host ('Repository: {0}' -f (Get-Location).Path)
Write-Host 'Note: the first run downloads npm packages and Chromium for Playwright (network, several minutes).' -ForegroundColor Yellow
Write-Host ''

$failedEarly = $false

# --- Layout ---
$layoutProblems = @()
foreach ($file in @('package.json', 'playwright.config.ts', 'tests\demo.spec.ts', '.gitignore')) {
    if (-not (Test-Path (Join-Path $e2eDir $file) -PathType Leaf)) { $layoutProblems += "e2e/$($file -replace '\\', '/') missing" }
}
if (-not (Test-Path (Join-Path $webDir 'package.json') -PathType Leaf)) { $layoutProblems += 'web/package.json missing' }
if ($layoutProblems.Count -gt 0) { Add-Result -Name 'layout' -Status FAIL -Detail ($layoutProblems -join '; '); $failedEarly = $true }
else { Add-Result -Name 'layout' -Status OK -Detail 'e2e/ config, tests, .gitignore; web/' }

# --- Versions: e2e/package.json must match VERSIONS.md exactly ---
$versionProblems = @()
$pinned = $null
foreach ($line in Get-Content (Join-Path $repoRoot 'VERSIONS.md')) {
    if ($line -match '^\|\s*`@playwright/test`\s*\|\s*([^|\s]+)\s*\|') { $pinned = $Matches[1] }
}
$packagePath = Join-Path $e2eDir 'package.json'
if (-not $pinned) { $versionProblems += 'VERSIONS.md has no version for @playwright/test' }
if (Test-Path $packagePath -PathType Leaf) {
    $package = Get-Content $packagePath -Raw | ConvertFrom-Json
    $declared = @{}
    foreach ($section in @('dependencies', 'devDependencies')) {
        if ($package.$section) {
            foreach ($property in $package.$section.PSObject.Properties) { $declared[$property.Name] = [string]$property.Value }
        }
    }
    foreach ($name in $declared.Keys) {
        if ($name -ne '@playwright/test') { $versionProblems += "e2e/package.json has unexpected '$name'" }
    }
    if (-not $declared.ContainsKey('@playwright/test')) { $versionProblems += "e2e/package.json missing '@playwright/test'" }
    elseif ($pinned -and $declared['@playwright/test'] -ne $pinned) {
        $versionProblems += "@playwright/test is '$($declared['@playwright/test'])', VERSIONS.md says '$pinned'"
    }
}
if ($versionProblems.Count -gt 0) { Add-Result -Name 'versions' -Status FAIL -Detail ($versionProblems -join '; '); $failedEarly = $true }
else { Add-Result -Name 'versions' -Status OK -Detail "@playwright/test $pinned (VERSIONS.md)" }

# --- Tools ---
$missingTools = @(@('node', $npm, $npx) | Where-Object { -not (Get-Command $_ -ErrorAction SilentlyContinue) })
if ($missingTools.Count -gt 0) { Add-Result -Name 'tools' -Status FAIL -Detail ("not on PATH: {0}" -f ($missingTools -join ', ')); $failedEarly = $true }
else { Add-Result -Name 'tools' -Status OK -Detail ("node {0}" -f (& node --version)) }

# --- Ports ---
$busy = @(@(8080, 3000) | Where-Object { Test-PortInUse -Port $_ })
if ($busy.Count -gt 0) {
    Add-Result -Name 'ports' -Status FAIL -Detail ("in use: {0}; stop the running API/web first" -f ($busy -join ', '))
    $failedEarly = $true
}
else { Add-Result -Name 'ports' -Status OK -Detail '8080 and 3000 free' }

# --- Steps (each needs the previous ones) ---
$steps = @(
    @{ Name = 'build'; Detail = 'scripts/build.ps1 (API jar)'; Run = { & (Join-Path $PSScriptRoot 'build.ps1') | Out-Host; $LASTEXITCODE } },
    @{ Name = 'web-npm'; Detail = 'npm ci (web)'; Run = { Invoke-In -Directory $webDir -Tool $npm -Arguments @('ci', '--no-audit', '--no-fund') } },
    @{ Name = 'e2e-npm'; Detail = 'npm ci (e2e)'; Run = { Invoke-In -Directory $e2eDir -Tool $npm -Arguments @('ci', '--no-audit', '--no-fund') } },
    @{ Name = 'browser'; Detail = 'npx playwright install chromium'; Run = { Invoke-In -Directory $e2eDir -Tool $npx -Arguments @('playwright', 'install', 'chromium') } }
)

$runStart = Get-Date
$blocked = $failedEarly
foreach ($step in $steps) {
    if ($blocked) { Add-Result -Name $step.Name -Status FAIL -Detail 'skipped (earlier check failed)'; continue }
    $exit = & $step.Run
    if ($exit -eq 0) { Add-Result -Name $step.Name -Status OK -Detail $step.Detail }
    else { Add-Result -Name $step.Name -Status FAIL -Detail "$($step.Detail) exit $exit"; $blocked = $true }
}

# --- E2E ---
if ($blocked) {
    Add-Result -Name 'e2e' -Status FAIL -Detail 'skipped (earlier check failed)'
    Add-Result -Name 'screenshot' -Status FAIL -Detail 'skipped'
}
else {
    $previousCi = $env:CI
    $env:CI = '1'
    try {
        $e2eExit = Invoke-In -Directory $e2eDir -Tool $npx -Arguments @('playwright', 'test')
    }
    finally {
        $env:CI = $previousCi
    }
    if ($e2eExit -eq 0) { Add-Result -Name 'e2e' -Status OK -Detail 'playwright test: mini, tight, sick repair' }
    else { Add-Result -Name 'e2e' -Status FAIL -Detail "playwright test exit $e2eExit (traces in e2e/test-results)" }

    $shot = Get-Item $screenshot -ErrorAction SilentlyContinue
    if ($shot -and $shot.LastWriteTime -ge $runStart -and $shot.Length -gt 0) {
        Add-Result -Name 'screenshot' -Status OK -Detail ("docs/demo/rota-demo.png ({0:N0} bytes)" -f $shot.Length)
    }
    else { Add-Result -Name 'screenshot' -Status FAIL -Detail 'docs/demo/rota-demo.png not written by this run' }
}

# --- Summary ---
$failed = @($script:Results | Where-Object Status -eq 'FAIL')
$warned = @($script:Results | Where-Object Status -eq 'WARN')

Write-Host ''
Write-Host ("P08 verify: {0} OK, {1} WARN, {2} FAIL" -f ($script:Results.Count - $warned.Count - $failed.Count), $warned.Count, $failed.Count)

if ($failed.Count -gt 0) {
    Write-Host 'P08 verify FAILED.' -ForegroundColor Red
    exit 1
}

Write-Host 'P08 verify passed.' -ForegroundColor Green
exit 0
