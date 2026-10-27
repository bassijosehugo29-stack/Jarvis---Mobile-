$ErrorActionPreference = "Stop"
$projectRoot = Split-Path $PSScriptRoot -Parent
$destination = Join-Path $projectRoot "gradle/wrapper/gradle-wrapper.jar"
$tempFile = [System.IO.Path]::GetTempFileName()
try {
    Invoke-WebRequest -UseBasicParsing -Uri "https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar" -OutFile $tempFile
    $checksum = (Invoke-WebRequest -UseBasicParsing -Uri "https://services.gradle.org/distributions/gradle-8.11.1-wrapper.jar.sha256").Content.Trim()
    $actual = (Get-FileHash -Algorithm SHA256 $tempFile).Hash
    if ($checksum.Length -ne 64 -or $actual -ine $checksum) { throw "Checksum do wrapper inválido." }
    New-Item -ItemType Directory -Force (Split-Path $destination) | Out-Null
    Move-Item -Force $tempFile $destination
} finally {
    if (Test-Path $tempFile) { Remove-Item $tempFile }
}
