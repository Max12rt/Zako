# Loads .env from the project root, sets JAVA_HOME to JDK 21, and starts the monolith.
# Run from C:\Users\crulp\Zako:
#   .\run-monolith.ps1

$ErrorActionPreference = 'Stop'

$envFile = Join-Path $PSScriptRoot '.env'
if (-not (Test-Path $envFile)) {
    Write-Error ".env not found at $envFile"
    exit 1
}

Write-Host "Loading $envFile ..." -ForegroundColor Cyan
foreach ($line in Get-Content $envFile) {
    if ($line -match '^\s*([^#=][^=]*)=(.*)$') {
        $name  = $Matches[1].Trim()
        $value = $Matches[2].Trim()
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        if ($name -like '*PASSWORD*' -or $name -like '*SECRET*') {
            Write-Host "  $name = ***" -ForegroundColor DarkGray
        } else {
            Write-Host "  $name = $value" -ForegroundColor DarkGray
        }
    }
}

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:MAVEN_OPTS = "-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.http.ssl.ignore.validity.dates=true"
Write-Host "JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Cyan

Push-Location (Join-Path $PSScriptRoot 'monolith')
try {
    Write-Host "Starting Spring Boot ..." -ForegroundColor Green
    & .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
}
