param(
    [string]$Version = "2.28.1",
    [string]$OutputPath = "tools\opentelemetry-javaagent.jar"
)

$ErrorActionPreference = "Stop"

$resolvedOutput = Join-Path (Get-Location) $OutputPath
$outputDirectory = Split-Path -Parent $resolvedOutput
if (-not (Test-Path $outputDirectory)) {
    New-Item -ItemType Directory -Path $outputDirectory | Out-Null
}

$url = "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v$Version/opentelemetry-javaagent.jar"
Write-Host "Downloading OpenTelemetry Java Agent $Version"
Write-Host $url
Invoke-WebRequest -Uri $url -OutFile $resolvedOutput
Write-Host "Saved to $resolvedOutput"
