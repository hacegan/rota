#requires -Version 7.0
<#
.SYNOPSIS
  Rota P01 verifier: glossary, requirements, and mini fixtures.

.DESCRIPTION
  Checks that the P01 deliverables exist and that every JSON fixture parses.
  No network access and no destructive commands.

.EXAMPLE
  pwsh -NoProfile -File scripts/verify_p01.ps1
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

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

Write-Host 'Rota P01 verify' -ForegroundColor Cyan
Write-Host ('Repository: {0}' -f (Get-Location).Path)
Write-Host ''

# --- Glossary ---
$glossaryPath = 'docs/domain/GLOSSARY.md'
if (-not (Test-Path $glossaryPath -PathType Leaf)) {
    Add-Result -Name 'glossary' -Status FAIL -Detail 'missing'
}
else {
    $text = Get-Content $glossaryPath -Raw
    $terms = @('shift', 'employee', 'skill', 'availability', 'roster', 'constraint', 'penalty', 'publication')
    $missing = @($terms | Where-Object { $text -notmatch [regex]::Escape($_) })
    if ($missing.Count -eq 0) {
        Add-Result -Name 'glossary' -Status OK -Detail ("all {0} terms present" -f $terms.Count)
    }
    else {
        Add-Result -Name 'glossary' -Status FAIL -Detail ("missing: " + ($missing -join ', '))
    }
}

# --- Requirements ---
$requirementsPath = 'docs/domain/REQUIREMENTS.md'
if (-not (Test-Path $requirementsPath -PathType Leaf)) {
    Add-Result -Name 'requirements' -Status FAIL -Detail 'missing'
}
else {
    $size = (Get-Item $requirementsPath).Length
    if ($size -gt 0) {
        Add-Result -Name 'requirements' -Status OK -Detail ("{0} bytes" -f $size)
    }
    else {
        Add-Result -Name 'requirements' -Status FAIL -Detail 'empty'
    }
}

# --- Fixtures ---
$fixturesDir = 'fixtures/mini'
if (-not (Test-Path $fixturesDir -PathType Container)) {
    Add-Result -Name 'fixtures' -Status FAIL -Detail 'missing'
}
else {
    $files = @(Get-ChildItem $fixturesDir -File)
    if ($files.Count -eq 0) {
        Add-Result -Name 'fixtures' -Status FAIL -Detail 'empty'
    }
    else {
        Add-Result -Name 'fixtures' -Status OK -Detail ("{0} files" -f $files.Count)
    }
}

# --- JSON parse ---
$jsonFiles = @(Get-ChildItem $fixturesDir -File -Filter *.json -ErrorAction SilentlyContinue)
if ($jsonFiles.Count -eq 0) {
    Add-Result -Name 'json-parse' -Status FAIL -Detail 'no JSON fixtures'
}
else {
    $failed = @()
    foreach ($file in $jsonFiles) {
        try {
            $null = Get-Content $file.FullName -Raw | ConvertFrom-Json
        }
        catch {
            $failed += ("{0}: {1}" -f $file.Name, $_.Exception.Message)
        }
    }
    if ($failed.Count -eq 0) {
        Add-Result -Name 'json-parse' -Status OK -Detail ("{0} JSON files parsed" -f $jsonFiles.Count)
    }
    else {
        Add-Result -Name 'json-parse' -Status FAIL -Detail ($failed -join '; ')
    }
}

# --- Summary ---
$failed = @($script:Results | Where-Object Status -eq 'FAIL')
$warned = @($script:Results | Where-Object Status -eq 'WARN')

Write-Host ''
Write-Host ("P01 verify: {0} OK, {1} WARN, {2} FAIL" -f ($script:Results.Count - $warned.Count - $failed.Count), $warned.Count, $failed.Count)

if ($failed.Count -gt 0) {
    Write-Host 'P01 verify FAILED.' -ForegroundColor Red
    exit 1
}

Write-Host 'P01 verify passed.' -ForegroundColor Green
exit 0
