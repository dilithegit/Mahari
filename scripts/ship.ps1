# Mahari Automated One-Command Shipping Script
# Usage: .\scripts\ship.ps1 -CommitMessage "type(scope): description" [-Version "3.3.0"]

param (
    [Parameter(Mandatory=$true)]
    [string]$CommitMessage,

    [Parameter(Mandatory=$false)]
    [string]$Version = ""
)

$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH += ";C:\Users\ujubu\AppData\Local\Android\Sdk\platform-tools"

$gradleFile = "app/build.gradle.kts"
$gradleContent = Get-Content $gradleFile -Raw

# Parse current versionCode and versionName from build.gradle.kts
if ($gradleContent -match 'versionCode\s*=\s*(\d+)') {
    $currentCode = [int]$matches[1]
} else {
    $currentCode = 1
}

if ($gradleContent -match 'versionName\s*=\s*"([^"]+)"') {
    $currentName = $matches[1]
} else {
    $currentName = "1.0.0"
}

# Determine target version
if ([string]::IsNullOrWhiteSpace($Version) -or $Version -eq $currentName) {
    # Auto-bump minor version (e.g. 3.2.0 -> 3.3.0) and versionCode (9 -> 10)
    $versionParts = $currentName.Split('.')
    $major = [int]$versionParts[0]
    $minor = [int]$versionParts[1] + 1
    $targetName = "$major.$minor.0"
    $targetCode = $currentCode + 1
} else {
    $targetName = $Version
    $targetCode = $currentCode + 1
}

# Hard Guard: Check if target tag already exists on remote origin
$remoteTags = git ls-remote --tags origin
if ($remoteTags -match "refs/tags/v$targetName") {
    Write-Host "⚠️ Tag v$targetName already exists on remote! Bumping version to avoid collision..." -ForegroundColor Yellow
    $versionParts = $targetName.Split('.')
    $major = [int]$versionParts[0]
    $minor = [int]$versionParts[1] + 1
    $targetName = "$major.$minor.0"
    $targetCode = $targetCode + 1
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "🚀 STARTING AUTOMATED SHIP PIPELINE" -ForegroundColor Cyan
Write-Host "Previous Version: $currentName (code: $currentCode)" -ForegroundColor Cyan
Write-Host "New Target Version: $targetName (code: $targetCode)" -ForegroundColor Green
Write-Host "=========================================" -ForegroundColor Cyan

# Update build.gradle.kts automatically
$newGradleContent = $gradleContent -replace 'versionCode\s*=\s*\d+', "versionCode = $targetCode"
$newGradleContent = $newGradleContent -replace 'versionName\s*=\s*"[^"]+"', "versionName = `"$targetName`""
Set-Content $gradleFile -Value $newGradleContent -NoNewline

# Step 1: Run Unit Tests
Write-Host "[1/6] Running unit tests..." -ForegroundColor Yellow
.\gradlew.bat testDebugUnitTest
if ($LASTEXITCODE -ne 0) {
    Write-Error "Unit tests failed! Aborting ship."
    exit 1
}

# Step 2: Build Debug APK
Write-Host "[2/6] Assembling debug APK..." -ForegroundColor Yellow
.\gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed! Aborting ship."
    exit 1
}

# Step 3: In-place Phone Update (FORBIDDEN: adb uninstall)
Write-Host "[3/6] Performing in-place phone update (adb install -r)..." -ForegroundColor Yellow
adb -s R7AWC009KWA install -r app/build/outputs/apk/debug/app-debug.apk
if ($LASTEXITCODE -ne 0) {
    Write-Error "In-place installation failed! Aborting ship."
    exit 1
}

# Launch App & Verify Package Status
adb -s R7AWC009KWA shell am start -n com.example.mahari/.MainActivity
Write-Host "Verifying installed package status on device R7AWC009KWA..." -ForegroundColor Green
$installedDump = adb -s R7AWC009KWA shell "dumpsys package com.example.mahari | grep -E 'versionName|versionCode'"
Write-Host $installedDump -ForegroundColor Green

# Confirm version mismatch does not exist
if (($installedDump -join " ") -notmatch "versionName=$targetName") {
    Write-Error "Installed version ($installedDump) does not match target version ($targetName)! Aborting."
    exit 1
}

# Step 4: Commit & Push Code to GitHub (using native Git CLI only)
Write-Host "[4/6] Committing code changes with author identity (native Git CLI)..." -ForegroundColor Yellow
$ErrorActionPreference = "Continue"
git add .
git commit -m "$CommitMessage (v$targetName)"
git push origin main

# Step 5: Tag Release & Push Tag (native Git CLI only)
Write-Host "[5/6] Tagging release v$targetName and pushing tag to origin..." -ForegroundColor Yellow
git tag "v$targetName"
git push origin "v$targetName"

# Step 6: Verify Live GitHub Release Page (native PowerShell API call only)
Write-Host "[6/6] Polling GitHub REST API for live release assets..." -ForegroundColor Yellow
$maxAttempts = 12
$attempt = 1
$releaseUrl = "https://api.github.com/repos/dilithegit/Mahari/releases/tags/v$targetName"

while ($attempt -le $maxAttempts) {
    Start-Sleep -Seconds 10
    try {
        $rel = Invoke-RestMethod -Uri $releaseUrl -ErrorAction Stop
        if ($rel.assets.Count -ge 2) {
            Write-Host "=========================================" -ForegroundColor Green
            Write-Host "✅ LIVE GITHUB RELEASE CONFIRMED FOR v$targetName" -ForegroundColor Green
            Write-Host "Release Name: $($rel.name)" -ForegroundColor Green
            Write-Host "Release Tag: $($rel.tag_name)" -ForegroundColor Green
            Write-Host "Assets Attached: $($rel.assets.Count)" -ForegroundColor Green
            $rel.assets | Select-Object name, size, browser_download_url | Format-Table -AutoSize
            Write-Host "Release URL: https://github.com/dilithegit/Mahari/releases/tag/v$targetName" -ForegroundColor Green
            Write-Host "=========================================" -ForegroundColor Green
            exit 0
        }
    } catch {
        Write-Host "Waiting for GitHub Actions pipeline build... (attempt $attempt/$maxAttempts)" -ForegroundColor Gray
    }
    $attempt++
}

Write-Host "⚠️ Ship complete, but release assets taking longer than expected to attach. Check https://github.com/dilithegit/Mahari/releases/tag/v$targetName manually." -ForegroundColor Yellow
