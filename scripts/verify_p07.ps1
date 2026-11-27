#requires -Version 7.0
<#
.SYNOPSIS
  Rota P07 verifier: calendar demo UI (web/, Next.js App Router + React + TypeScript).

.DESCRIPTION
  NOTE: the FIRST run downloads packages from the npm registry into web/node_modules
  (next, react, react-dom, typescript, @types/react, @types/node and their dependencies; a few
  hundred MB). It needs network access and can take several minutes. Later runs reuse
  web/node_modules. npm also writes web/package-lock.json on the first run; commit it so later
  installs are reproducible.

  Dot-sources scripts/env.ps1, then checks:
    layout    web/package.json, tsconfig.json, next.config.mjs, app/, lib/ exist; web/.gitignore
              ignores node_modules and .next
    versions  web/package.json has exactly next, react, react-dom (dependencies) and typescript,
              @types/react, @types/node (devDependencies), each pinned to the version in the
              VERSIONS.md "Frontend — npm" table
    tools     node and npm on PATH
    install   npm install            (in web/)
    typecheck npm run typecheck      (next typegen && tsc --noEmit)
    build     npm run build          (next build)
  Does not start a server and does not call the API. Never deletes node_modules or .next.

.EXAMPLE
  pwsh -NoProfile -File scripts/verify.ps1 p07
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'env.ps1')

$repoRoot = Split-Path -Parent $PSScriptRoot
$webDir = Join-Path $repoRoot 'web'
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

# npm.cmd on Windows avoids npm.ps1 and the execution policy.
$npm = if ($IsWindows) { 'npm.cmd' } else { 'npm' }

# Runs npm in web/; npm's output goes to the console, only the exit code is returned.
function Invoke-Npm {
    param([Parameter(Mandatory)][string[]]$Arguments)
    Push-Location $webDir
    try {
        & $npm @Arguments | Out-Host
        return $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
}

Write-Host 'Rota P07 verify' -ForegroundColor Cyan
Write-Host ('Repository: {0}' -f (Get-Location).Path)
Write-Host 'Note: the first run downloads npm packages into web/node_modules (network, several minutes).' -ForegroundColor Yellow
Write-Host ''

# --- Layout ---
$layoutProblems = @()
foreach ($file in @('package.json', 'tsconfig.json', 'next.config.mjs', '.gitignore', 'app\layout.tsx', 'app\page.tsx',
        'app\roster-demo.tsx', 'app\globals.css', 'lib\api.ts', 'lib\format.ts', 'lib\grid.ts', 'lib\types.ts')) {
    if (-not (Test-Path (Join-Path $webDir $file) -PathType Leaf)) { $layoutProblems += "web/$($file -replace '\\', '/') missing" }
}
$gitignore = Join-Path $webDir '.gitignore'
if (Test-Path $gitignore -PathType Leaf) {
    $ignoreText = Get-Content $gitignore -Raw
    if ($ignoreText -notmatch '(?m)^/?node_modules/?\s*$') { $layoutProblems += 'web/.gitignore does not ignore node_modules' }
    if ($ignoreText -notmatch '(?m)^/?\.next/?\s*$') { $layoutProblems += 'web/.gitignore does not ignore .next' }
}
if ($layoutProblems.Count -gt 0) { Add-Result -Name 'layout' -Status FAIL -Detail ($layoutProblems -join '; ') }
else { Add-Result -Name 'layout' -Status OK -Detail 'web/ app, lib, config, .gitignore' }

# --- Versions: package.json must match VERSIONS.md exactly ---
$expectedDeps = @('next', 'react', 'react-dom')
$expectedDevDeps = @('typescript', '@types/react', '@types/node')
$versionProblems = @()
$pinned = @{}
foreach ($line in Get-Content (Join-Path $repoRoot 'VERSIONS.md')) {
    if ($line -match '^\|\s*`([^`]+)`\s*\|\s*([^|\s]+)\s*\|') {
        if (($expectedDeps + $expectedDevDeps) -contains $Matches[1]) { $pinned[$Matches[1]] = $Matches[2] }
    }
}
$packagePath = Join-Path $webDir 'package.json'
if (-not (Test-Path $packagePath -PathType Leaf)) {
    $versionProblems += 'web/package.json missing'
}
else {
    $package = Get-Content $packagePath -Raw | ConvertFrom-Json
    foreach ($section in @(@{ Name = 'dependencies'; Expected = $expectedDeps }, @{ Name = 'devDependencies'; Expected = $expectedDevDeps })) {
        $declared = @{}
        if ($package.($section.Name)) {
            foreach ($property in $package.($section.Name).PSObject.Properties) { $declared[$property.Name] = [string]$property.Value }
        }
        foreach ($name in $declared.Keys) {
            if ($section.Expected -notcontains $name) { $versionProblems += "$($section.Name) has unexpected '$name'" }
        }
        foreach ($name in $section.Expected) {
            if (-not $pinned.ContainsKey($name)) { $versionProblems += "VERSIONS.md has no version for '$name'" }
            elseif (-not $declared.ContainsKey($name)) { $versionProblems += "$($section.Name) missing '$name'" }
            elseif ($declared[$name] -ne $pinned[$name]) { $versionProblems += "$name is '$($declared[$name])', VERSIONS.md says '$($pinned[$name])'" }
        }
    }
}
if ($versionProblems.Count -gt 0) { Add-Result -Name 'versions' -Status FAIL -Detail ($versionProblems -join '; ') }
else { Add-Result -Name 'versions' -Status OK -Detail 'six packages, exact pins from VERSIONS.md' }

# --- Tools ---
$missingTools = @(@('node', $npm) | Where-Object { -not (Get-Command $_ -ErrorAction SilentlyContinue) })
if ($missingTools.Count -gt 0) { Add-Result -Name 'tools' -Status FAIL -Detail ("not on PATH: {0}" -f ($missingTools -join ', ')) }
else { Add-Result -Name 'tools' -Status OK -Detail ("node {0}" -f (& node --version)) }

# --- npm install, typecheck, build (each needs the previous one) ---
$canRun = ($missingTools.Count -eq 0) -and (Test-Path $packagePath -PathType Leaf)
if (-not $canRun) {
    Add-Result -Name 'install' -Status FAIL -Detail 'skipped (tools or package.json missing)'
    Add-Result -Name 'typecheck' -Status FAIL -Detail 'skipped'
    Add-Result -Name 'build' -Status FAIL -Detail 'skipped'
}
else {
    $installExit = Invoke-Npm -Arguments @('install', '--no-audit', '--no-fund')
    if ($installExit -ne 0) {
        Add-Result -Name 'install' -Status FAIL -Detail "npm install exit $installExit"
        Add-Result -Name 'typecheck' -Status FAIL -Detail 'skipped (install failed)'
        Add-Result -Name 'build' -Status FAIL -Detail 'skipped (install failed)'
    }
    else {
        Add-Result -Name 'install' -Status OK -Detail 'npm install'
        $typecheckExit = Invoke-Npm -Arguments @('run', 'typecheck')
        if ($typecheckExit -eq 0) { Add-Result -Name 'typecheck' -Status OK -Detail 'next typegen && tsc --noEmit' }
        else { Add-Result -Name 'typecheck' -Status FAIL -Detail "npm run typecheck exit $typecheckExit" }

        $buildExit = Invoke-Npm -Arguments @('run', 'build')
        if ($buildExit -eq 0) { Add-Result -Name 'build' -Status OK -Detail 'next build' }
        else { Add-Result -Name 'build' -Status FAIL -Detail "npm run build exit $buildExit" }
    }
}

# --- Summary ---
$failed = @($script:Results | Where-Object Status -eq 'FAIL')
$warned = @($script:Results | Where-Object Status -eq 'WARN')

Write-Host ''
Write-Host ("P07 verify: {0} OK, {1} WARN, {2} FAIL" -f ($script:Results.Count - $warned.Count - $failed.Count), $warned.Count, $failed.Count)

if ($failed.Count -gt 0) {
    Write-Host 'P07 verify FAILED.' -ForegroundColor Red
    exit 1
}

Write-Host 'P07 verify passed.' -ForegroundColor Green
exit 0
