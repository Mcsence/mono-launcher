# Builds the Mono launcher APK on this PC.
# Usage:  .\build.ps1            -> debug APK
#         .\build.ps1 -Install   -> also installs it on a phone connected over USB (adb)
param([switch]$Install)

$ErrorActionPreference = 'Stop'

$env:JAVA_HOME = "$env:LOCALAPPDATA\Java\jdk-17.0.21.1+2"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

# This PC blocks Unix-domain socket connections, which Java 17 uses internally on Windows.
# The patched class in nio-patch makes Java fall back to TCP loopback instead.
$patch = "$env:LOCALAPPDATA\Java\nio-patch"
if (Test-Path $patch) { $env:JAVA_TOOL_OPTIONS = "--patch-module=java.base=$patch" }

if (-not (Test-Path "$PSScriptRoot\local.properties")) {
    "sdk.dir=$($env:ANDROID_HOME -replace '\\','/')" | Set-Content "$PSScriptRoot\local.properties" -Encoding ascii
}

Push-Location $PSScriptRoot
try {
    & .\gradlew.bat assembleDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Build failed" }
    $apk = "$PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk"
    Write-Host "`nAPK : $apk"
    if ($Install) { & adb install -r $apk }
} finally {
    Pop-Location
}
