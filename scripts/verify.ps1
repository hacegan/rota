#requires -Version 7.0
<#
.SYNOPSIS
  Rota phase verify dispatcher.

.DESCRIPTION
  Dispatches a phase id (p01..p19) to its phase-specific verifier. Unknown or
  unimplemented phases fail clearly with a non-zero exit code.

.EXAMPLE
  pwsh -NoProfile -File scripts/verify.ps1 p01
  pwsh -NoProfile -File scripts/verify.ps1 p99
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Phase
)

$ErrorActionPreference = 'Stop'

$verifiers = @{
    p01 = 'scripts/verify_p01.ps1'
}

$key = $Phase.ToLowerInvariant()
if (-not $verifiers.ContainsKey($key)) {
    Write-Host ("Unknown or unimplemented phase: {0}" -f $Phase) -ForegroundColor Red
    Write-Host ("Supported phases: {0}" -f (($verifiers.Keys | Sort-Object) -join ', ')) -ForegroundColor Yellow
    exit 2
}

$scriptPath = $verifiers[$key]
if (-not (Test-Path $scriptPath -PathType Leaf)) {
    Write-Host ("Verifier not found: {0}" -f $scriptPath) -ForegroundColor Red
    exit 2
}

& pwsh -NoProfile -File $scriptPath
exit $LASTEXITCODE
