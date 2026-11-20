#requires -Version 7.0
<#
.SYNOPSIS
  Build Rota with the Maven wrapper.

.DESCRIPTION
  Dot-sources scripts/env.ps1 to put the machine JDK 27 on the session PATH, then runs
  the Maven wrapper. Windows only (mvnw.cmd); never bash.

.EXAMPLE
  pwsh -NoProfile -File scripts/build.ps1
#>
[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'env.ps1')

$repoRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $repoRoot 'mvnw.cmd') -q -DskipTests package
exit $LASTEXITCODE
