#requires -Version 7.0
<#
.SYNOPSIS
  Rota P00 preflight: toolchain versions, free RAM, and resource budget.

.DESCRIPTION
  Reads actual versions from the environment (never invents them), reports free
  physical memory and free disk space, and checks them against a configurable
  budget. Exits 1 when a required check fails.

.PARAMETER BudgetMB
  Memory budget in MB reserved for build/test workloads. Default: 2048.

.PARAMETER MinDiskGB
  Minimum free disk space in GB on the repository drive. Default: 1.

.PARAMETER Require
  Tools that must be present. Default: pwsh,git,java,node,npm,docker,gh,opencode.

.EXAMPLE
  pwsh -File scripts/preflight.ps1
  pwsh -File scripts/preflight.ps1 -BudgetMB 4096 -Require git,dotnet
#>
[CmdletBinding()]
param(
    [int]$BudgetMB = 2048,
    [double]$MinDiskGB = 1,
    [string[]]$Require = @('pwsh', 'git', 'java', 'node', 'npm', 'docker', 'gh', 'opencode')
)

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

function Get-ToolVersion {
    param([Parameter(Mandatory)][string]$Name)

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $command) { return $null }

    $probeArgs = @($versionArgs[$Name])
    $job = Start-Job -ScriptBlock {
        param($tool, $toolArgs)
        $output = & $tool @toolArgs 2>&1 | Out-String
        [pscustomobject]@{ Output = $output; ExitCode = $LASTEXITCODE }
    } -ArgumentList $Name, $probeArgs

    $null = Wait-Job $job -Timeout 5
    if ($job.State -ne 'Completed') {
        $detail = if ($job.State -eq 'Running') { 'version probe timed out after 5s' } else { 'version probe failed' }
        Stop-Job $job -ErrorAction SilentlyContinue
        Remove-Job $job -Force -ErrorAction SilentlyContinue
        return [pscustomobject]@{ Ok = $false; Detail = $detail }
    }

    $result = Receive-Job $job
    Remove-Job $job -Force -ErrorAction SilentlyContinue

    if ($result.ExitCode -ne 0) {
        return [pscustomobject]@{ Ok = $false; Detail = "version probe exited with code $($result.ExitCode)" }
    }

    $firstLine = ([string]$result.Output) -split '\r?\n' | Where-Object { $_.Trim() } | Select-Object -First 1
    if (-not $firstLine) {
        return [pscustomobject]@{ Ok = $false; Detail = 'version probe produced no output' }
    }
    return [pscustomobject]@{ Ok = $true; Detail = $firstLine.Trim() }
}

Write-Host 'Rota preflight' -ForegroundColor Cyan
Write-Host ('Repository: {0}' -f (Get-Location).Path)
Write-Host ''

# --- Tool versions ---
$versionArgs = @{
    pwsh     = @('-Version')
    git      = @('--version')
    java     = @('-version')
    node     = @('--version')
    npm      = @('--version')
    docker   = @('--version')
    gh       = @('--version')
    opencode = @('--version')
    dotnet   = @('--version')
    python   = @('--version')
    kubectl  = @('--version')
    helm     = @('--version')
    kind     = @('--version')
}

$optionalTools = @('dotnet', 'python', 'kubectl', 'helm', 'kind')
$allTools = @($Require) + @($optionalTools | Where-Object { $_ -notin $Require })

foreach ($tool in ($allTools | Select-Object -Unique)) {
    $probe = Get-ToolVersion -Name $tool
    if ($null -eq $probe) {
        $status = if ($tool -in $Require) { 'FAIL' } else { 'WARN' }
        Add-Result -Name $tool -Status $status -Detail 'not found'
    }
    elseif ($probe.Ok) {
        Add-Result -Name $tool -Status OK -Detail $probe.Detail
    }
    else {
        $status = if ($tool -in $Require) { 'FAIL' } else { 'WARN' }
        Add-Result -Name $tool -Status $status -Detail $probe.Detail
    }
}

# --- Free RAM ---
$os = Get-CimInstance Win32_OperatingSystem
$freeRamMB = [math]::Round($os.FreePhysicalMemory / 1KB)
$totalRamMB = [math]::Round($os.TotalVisibleMemorySize / 1KB)
$ramDetail = "{0} MB free / {1} MB total" -f $freeRamMB, $totalRamMB
if ($freeRamMB -ge $BudgetMB) {
    Add-Result -Name 'ram' -Status OK -Detail $ramDetail
}
else {
    Add-Result -Name 'ram' -Status FAIL -Detail "$ramDetail (budget $BudgetMB MB)"
}

# --- Disk budget ---
$repoDrive = [System.IO.DriveInfo]::new((Get-Location).Path.Substring(0, 2))
$freeDiskGB = [math]::Round($repoDrive.AvailableFreeSpace / 1GB, 2)
$diskDetail = "{0} GB free on {1}" -f $freeDiskGB, $repoDrive.Name
if ($freeDiskGB -ge $MinDiskGB) {
    Add-Result -Name 'disk' -Status OK -Detail $diskDetail
}
else {
    Add-Result -Name 'disk' -Status FAIL -Detail "$diskDetail (min $MinDiskGB GB)"
}

# --- Summary ---
$failed = @($script:Results | Where-Object Status -eq 'FAIL')
$warned = @($script:Results | Where-Object Status -eq 'WARN')

Write-Host ''
Write-Host ("Preflight: {0} OK, {1} WARN, {2} FAIL" -f ($script:Results.Count - $warned.Count - $failed.Count), $warned.Count, $failed.Count)

if ($failed.Count -gt 0) {
    Write-Host 'Preflight FAILED.' -ForegroundColor Red
    exit 1
}

Write-Host 'Preflight passed.' -ForegroundColor Green
exit 0
