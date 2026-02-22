# Run this script once to download the Gradle wrapper JAR so you can build without installing Gradle.
# Usage: powershell -ExecutionPolicy Bypass -File setup-gradle-wrapper.ps1

$ErrorActionPreference = "Stop"
$wrapperDir = Join-Path $PSScriptRoot "gradle\wrapper"
$jarPath = Join-Path $wrapperDir "gradle-wrapper.jar"

if (Test-Path $jarPath) {
    Write-Host "gradle-wrapper.jar already exists. Nothing to do."
    exit 0
}

if (-not (Test-Path $wrapperDir)) {
    New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
}

# Download from jsDelivr CDN (reliable, no redirects)
$wrapperUrl = "https://cdn.jsdelivr.net/gh/gradle/gradle@v8.2.0/gradle/wrapper/gradle-wrapper.jar"
Write-Host "Downloading gradle-wrapper.jar..."
Invoke-WebRequest -Uri $wrapperUrl -OutFile $jarPath -UseBasicParsing

if (-not (Test-Path $jarPath) -or (Get-Item $jarPath).Length -lt 50000) {
    Write-Error "Download failed. Run: winget install Gradle.Gradle   then: gradle wrapper"
    exit 1
}

Write-Host "Done. You can now run: .\gradlew.bat assembleDebug"
exit 0
