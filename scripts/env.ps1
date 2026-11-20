#requires -Version 7.0
<#
.SYNOPSIS
  Resolve the machine JDK 27 for the current session.

.DESCRIPTION
  Reads `java_home` from `.agent/MACHINE.md` when present; otherwise scans the
  per-user and machine Adoptium install dirs for `jdk-27*`, picks the highest,
  and records it in `.agent/MACHINE.md`. Sets JAVA_HOME and prepends its bin to
  the current session PATH only. Idempotent; no network; no admin.

.EXAMPLE
  . .\scripts\env.ps1
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$machineFile = Join-Path $repoRoot '.agent/MACHINE.md'

function Get-RecordedJavaHome {
    if (-not (Test-Path $machineFile -PathType Leaf)) { return $null }
    $line = Get-Content $machineFile | Where-Object { $_ -match '^\s*java_home\s*:' } | Select-Object -First 1
    if (-not $line) { return $null }
    if ($line -match '^\s*java_home\s*:\s*(.+?)\s*$') { return $Matches[1].Trim().Trim('"') }
    return $null
}

function Find-Jdk27 {
    $roots = @(
        (Join-Path $env:LOCALAPPDATA 'Programs\Eclipse Adoptium'),
        'C:\Program Files\Eclipse Adoptium'
    )
    $found = foreach ($root in $roots) {
        if (Test-Path $root -PathType Container) {
            Get-ChildItem $root -Directory -Filter 'jdk-27*' -ErrorAction SilentlyContinue
        }
    }
    if (-not $found) { return $null }
    $pick = $found | Sort-Object { [version]($_.Name -replace '^jdk-', '' -replace '[-+].*$', '') } -Descending | Select-Object -First 1
    return $pick.FullName
}

$javaHome = Get-RecordedJavaHome
if (-not $javaHome) {
    $javaHome = Find-Jdk27
    if ($javaHome) {
        Add-Content -Path $machineFile -Value "java_home: $javaHome"
    }
}

if (-not $javaHome -or -not (Test-Path (Join-Path $javaHome 'bin\java.exe') -PathType Leaf)) {
    Write-Host 'No JDK 27 found (checked .agent/MACHINE.md and the Adoptium install dirs).' -ForegroundColor Red
    exit 1
}

$env:JAVA_HOME = $javaHome
$bin = Join-Path $javaHome 'bin'
if (($env:Path -split ';') -notcontains $bin) {
    $env:Path = "$bin;$env:Path"
}

Write-Host ("JAVA_HOME: {0}" -f $env:JAVA_HOME)
& java -version 2>&1 | ForEach-Object { Write-Host $_ }
