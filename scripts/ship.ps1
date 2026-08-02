# Mahari Automated One-Command Shipping Script
# Usage: .\scripts\ship.ps1 -CommitMessage "type(scope): description" [-Version "3.3.0"]

param (
    [Parameter(Mandatory=$true)]
    [string]$CommitMessage,

    [Parameter(Mandatory=$false)]
    [string]$Version = "3.2.0"
)

$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH += ";C:\Users\ujubu\AppData\Local\Android\Sdk\platform-tools"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "🚀 STARTING AUTOMATED SHIP PIPELINE (v$Version)" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

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
adb -s R7AWC009KWA shell "dumpsys package com.example.mahari | grep -E 'versionName|versionCode'"

# Step 4: Commit & Push Code to GitHub
Write-Host "[4/6] Committing code changes with author identity..." -ForegroundColor Yellow
$ErrorActionPreference = "Continue"
git add .
git commit -m "$CommitMessage"
git push origin main

# Step 5: Tag Release & Push Tag
Write-Host "[5/6] Tagging release v$Version and pushing tag to origin..." -ForegroundColor Yellow
git tag -d "v$Version" 2>$null
git push origin ":refs/tags/v$Version" 2>$null
git tag "v$Version"
git push origin "v$Version"

# Step 6: Verify Live GitHub Release Page
Write-Host "[6/6] Polling GitHub API for live release assets..." -ForegroundColor Yellow
$maxAttempts = 12
$attempt = 1
$releaseUrl = "https://api.github.com/repos/dilithegit/Mahari/releases/tags/v$Version"

while ($attempt -le $maxAttempts) {
    Start-Sleep -Seconds 10
    try {
        $rel = Invoke-RestMethod -Uri $releaseUrl -ErrorAction Stop
        if ($rel.assets.Count -ge 2) {
            Write-Host "=========================================" -ForegroundColor Green
            Write-Host "✅ LIVE GITHUB RELEASE CONFIRMED FOR v$Version" -ForegroundColor Green
            Write-Host "Release Name: $($rel.name)" -ForegroundColor Green
            Write-Host "Release Tag: $($rel.tag_name)" -ForegroundColor Green
            Write-Host "Assets Attached: $($rel.assets.Count)" -ForegroundColor Green
            $rel.assets | Select-Object name, size, browser_download_url | Format-Table -AutoSize
            Write-Host "Release URL: https://github.com/dilithegit/Mahari/releases/tag/v$Version" -ForegroundColor Green
            Write-Host "=========================================" -ForegroundColor Green
            exit 0
        }
    } catch {
        Write-Host "Waiting for GitHub Actions pipeline build... (attempt $attempt/$maxAttempts)" -ForegroundColor Gray
    }
    $attempt++
}

Write-Host "⚠️ Ship complete, but release assets taking longer than expected to attach. Check https://github.com/dilithegit/Mahari/releases/tag/v$Version manually." -ForegroundColor Yellow
