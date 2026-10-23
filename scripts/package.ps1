# scripts/package.ps1 — Windows (.exe) jpackage wrapper
# Usage: pwsh scripts/package.ps1  (from the repo root)
# Requires: JDK 21+, WiX Toolset (pre-installed on GitHub windows-latest runners),
#           ./gradlew assemble already run (build/jpackage-input/ must exist)

$ErrorActionPreference = "Stop"

$ScriptDir   = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir  = (Get-Item (Join-Path $ScriptDir "..")).FullName
$InputDir    = Join-Path $ProjectDir "desktop" "build" "jpackage-input"
$OutputDir   = Join-Path $ProjectDir "desktop" "build" "installer"
$MainClass   = "com.example.app.AppLauncher"
$AppName     = "BanRoulette"

# Auto-detect built jar and version from jpackage-input
$JarFile = Get-ChildItem -Path $InputDir -Filter "*1.0.0.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $JarFile) {
    $JarFile = Get-ChildItem -Path $InputDir -Filter "*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch '^(javafx|h2|core)' } | Select-Object -First 1
}
if ($JarFile) {
    $MainJar = $JarFile.Name
    $AppVersion = $MainJar -replace '^.*-', '' -replace '\.jar$', ''
} else {
    $MainJar = "desktop-1.0.0.jar"
    $AppVersion = "1.0.0"
}

Write-Host "==> Packaging $AppName as .exe ..."
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

# Icon path (optional — add desktop/src/main/resources/icon.ico later)
$IconArgs = @()
$IconIco = Join-Path $ProjectDir "desktop" "src" "main" "resources" "icon.ico"
if (Test-Path $IconIco) {
    $IconArgs = @("--icon", $IconIco)
}

$jpackageArgs = @(
    "--type",         "exe",
    "--name",         $AppName,
    "--app-version",  $AppVersion,
    "--input",        $InputDir,
    "--main-jar",     $MainJar,
    "--main-class",   $MainClass,
    "--dest",         $OutputDir,
    "--java-options", "--enable-preview",
    "--win-dir-chooser",
    "--win-menu",
    "--win-shortcut"
) + $IconArgs

Write-Host "jpackage $jpackageArgs"
& jpackage @jpackageArgs

Write-Host "==> Installer written to: $OutputDir"
Get-ChildItem $OutputDir | Format-Table Name, Length -AutoSize
