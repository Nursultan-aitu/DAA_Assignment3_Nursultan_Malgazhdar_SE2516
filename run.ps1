param([ValidateSet('test','benchmark','plots','all')][string]$Task = 'all')
$ErrorActionPreference = 'Stop'

$jdkCandidates = @()
if ($env:JAVA_HOME) { $jdkCandidates += $env:JAVA_HOME }
$jdkBase = Join-Path $env:USERPROFILE '.jdks'
if (Test-Path -LiteralPath $jdkBase) {
    $jdkCandidates += Get-ChildItem -LiteralPath $jdkBase -Directory |
        Sort-Object @{Expression = { if ($_.Name -match '21') { 0 } else { 1 } }}, Name |
        Select-Object -ExpandProperty FullName
}
$compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
if ($compiler) { $jdkCandidates += Split-Path (Split-Path $compiler.Source -Parent) -Parent }
$selectedJdk = $null
foreach ($candidate in $jdkCandidates) {
    if (!(Test-Path -LiteralPath (Join-Path $candidate 'bin/javac.exe'))) { continue }
    $versionText = (& (Join-Path $candidate 'bin/java.exe') -version 2>&1 | Out-String)
    if ($versionText -match 'version "(\d+)' -and [int]$Matches[1] -ge 17) {
        $selectedJdk = $candidate
        break
    }
}
if (!$selectedJdk) { throw 'Install JDK 17 or newer and set JAVA_HOME.' }
$env:JAVA_HOME = $selectedJdk

function Invoke-Tests {
    & '.\mvnw.cmd' -B clean verify
    if ($LASTEXITCODE -ne 0) { throw "Tests failed with exit code $LASTEXITCODE" }
}

function Invoke-Benchmark {
    & '.\mvnw.cmd' -B compile
    if ($LASTEXITCODE -ne 0) { throw "Compilation failed with exit code $LASTEXITCODE" }
    & (Join-Path $selectedJdk 'bin/java.exe') -Xms512m -Xmx3g -cp 'target/classes' edu.daa.benchmark.Benchmark
    if ($LASTEXITCODE -ne 0) { throw "Benchmark failed with exit code $LASTEXITCODE" }
}

function Invoke-Plots {
    $python = Get-Command python.exe -ErrorAction SilentlyContinue
    if (!$python) { throw 'Python 3 with Pillow is required to regenerate plots.' }
    & $python.Source 'tools/plot_results.py'
    if ($LASTEXITCODE -ne 0) { throw "Plot generation failed with exit code $LASTEXITCODE" }
    & $python.Source 'tools/generate_report.py'
    if ($LASTEXITCODE -ne 0) { throw "Report generation failed with exit code $LASTEXITCODE" }
}

Push-Location $PSScriptRoot
try {
    Write-Host "Using JDK: $selectedJdk"
    switch ($Task) {
        'test' { Invoke-Tests }
        'benchmark' { Invoke-Benchmark }
        'plots' { Invoke-Plots }
        'all' { Invoke-Tests; Invoke-Benchmark; Invoke-Plots }
    }
} finally {
    Pop-Location
}
