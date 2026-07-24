param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(
        "workflow-service",
        "order-service",
        "inventory-service",
        "payment-service",
        "invoice-service",
        "notification-service"
    )]
    [string]$Service,

    [string]$AgentPath = "tools\opentelemetry-javaagent.jar",
    [string]$OtlpEndpoint = "http://localhost:4318",
    [string]$Environment = "local"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $AgentPath)) {
    throw "OpenTelemetry Java Agent not found at '$AgentPath'. Run .\scripts\download-otel-java-agent.ps1 first."
}

$resolvedAgent = Resolve-Path $AgentPath
$workspacePath = (Get-Location).Path
$agentPathValue = $resolvedAgent.Path
if ($agentPathValue.StartsWith($workspacePath, [System.StringComparison]::OrdinalIgnoreCase)) {
    $agentForJvm = $agentPathValue.Substring($workspacePath.Length).TrimStart("\", "/").Replace("\", "/")
} else {
    $agentForJvm = $agentPathValue.Replace("\", "/")
}

if (-not $agentForJvm.StartsWith("..") -and -not [System.IO.Path]::IsPathRooted($agentForJvm)) {
    $agentForJvm = "../$agentForJvm"
}
$env:OTEL_SERVICE_NAME = $Service
$env:OTEL_RESOURCE_ATTRIBUTES = "deployment.environment=$Environment"
$env:OTEL_EXPORTER_OTLP_ENDPOINT = $OtlpEndpoint
$env:OTEL_EXPORTER_OTLP_PROTOCOL = "http/protobuf"
$env:OTEL_TRACES_EXPORTER = "otlp"
$env:OTEL_METRICS_EXPORTER = "otlp"
$env:OTEL_LOGS_EXPORTER = "none"
$env:OTEL_PROPAGATORS = "tracecontext,baggage"
$env:OTEL_INSTRUMENTATION_LOGBACK_MDC_ENABLED = "true"
$env:OTEL_INSTRUMENTATION_HTTP_SERVER_EXCLUDED_URLS = "/actuator/.*,/prometheus,/favicon.ico,/error,/camunda/app/.*,/camunda/api/.*,/webjars/.*,/.*\.(css|js|png|ico|map|woff|woff2)"

$jvmArguments = "-javaagent:$agentForJvm"
Write-Host "Starting $Service with OpenTelemetry Java Agent"
Write-Host "Java Agent: $agentForJvm"
Write-Host "OTLP endpoint: $OtlpEndpoint"
Write-Host "Excluded server URLs: $env:OTEL_INSTRUMENTATION_HTTP_SERVER_EXCLUDED_URLS"
.\mvnw.cmd -pl $Service spring-boot:run "-Dspring-boot.run.jvmArguments=$jvmArguments"
