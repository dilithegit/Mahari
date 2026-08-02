# Mahari Automated One-Command Shipping Script
# Usage: .\scripts\ship.ps1 -CommitMessage "type(scope): description" [-Version "3.6.0"]

param (
    [Parameter(Mandatory=$true)]
    [string]$CommitMessage,

    [Parameter(Mandatory=$false)]
    [string]$Version = ""
)

$ErrorActionPreference = "Stop"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:PATH += ";C:\Users\ujubu\AppData\Local\Android\Sdk\platform-tools"

# Step 0: Mandatory ADB Device Connection Verification
Write-Host "[0/6] Checking connected ADB devices..." -ForegroundColor Yellow
$devicesOutput = adb devices
Write-Host $devicesOutput -ForegroundColor Gray

if (($devicesOutput -join " ") -notmatch "R7AWC009KWA") {
    Write-Error "no device connected, phone did not receive this update. Aborting ship pipeline."
    exit 1
}

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
    # Auto-bump minor version (e.g. 3.5.0 -> 3.6.0) and versionCode (12 -> 13)
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
    Write-Host "Tag v$targetName already exists on remote! Bumping version to avoid collision..." -ForegroundColor Yellow
    $versionParts = $targetName.Split('.')
    $major = [int]$versionParts[0]
    $minor = [int]$versionParts[1] + 1
    $targetName = "$major.$minor.0"
    $targetCode = $targetCode + 1
}

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "STARTING AUTOMATED SHIP PIPELINE" -ForegroundColor Cyan
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

# Step 2: Build Debug APK (Consistently debug-over-debug variant)
Write-Host "[2/6] Assembling debug APK..." -ForegroundColor Yellow
.\gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed! Aborting ship."
    exit 1
}

# Step 3: In-place Phone Update (FORBIDDEN: adb uninstall)
Write-Host "[3/6] Performing in-place phone update (adb install -r)..." -ForegroundColor Yellow
$installOutput = adb -s R7AWC009KWA install -r app/build/outputs/apk/debug/app-debug.apk 2>&1
Write-Host $installOutput -ForegroundColor Gray

if ($installOutput -match "INSTALL_FAILED_UPDATE_INCOMPATIBLE|INSTALL_FAILED_SHARED_USER_INCOMPATIBLE") {
    Write-Error "CRITICAL: Signature mismatch detected! Do NOT uninstall. Aborting ship."
    exit 1
}

# Launch App & Verify Installed Package Version
adb -s R7AWC009KWA shell am start -n com.example.mahari/.MainActivity | Out-Null
Write-Host "Verifying installed package status on device R7AWC009KWA..." -ForegroundColor Green
$installedDump = adb -s R7AWC009KWA shell "dumpsys package com.example.mahari | grep -E 'versionName|versionCode'"
Write-Host $installedDump -ForegroundColor Green

if (($installedDump -join " ") -match "versionName=$targetName") {
    Write-Host "=========================================" -ForegroundColor Green
    Write-Host "Installed version matches the new build: YES" -ForegroundColor Green
    Write-Host "=========================================" -ForegroundColor Green
} else {
    Write-Error "Installed version does NOT match - update did not reach the phone! Aborting ship."
    exit 1
}

# Step 4: Commit & Push Code to GitHub (using native Git CLI only)
Write-Host "[4/6] Committing code changes with author identity (native Git CLI)..." -ForegroundColor Yellow
$ErrorActionPreference = "Stop"
git add .
$fullCommitMsg = "$CommitMessage (v$targetName)"
git commit -m $fullCommitMsg
if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne 1) {
    Write-Error "Git commit failed! Aborting ship."
    exit 1
}

git push origin main
if ($LASTEXITCODE -ne 0) {
    Write-Error "Git push origin main failed! Aborting ship."
    exit 1
}

# Step 5: Tag Release & Push Tag (native Git CLI only)
Write-Host "[5/6] Tagging release v$targetName and pushing tag to origin..." -ForegroundColor Yellow
$tagName = "v$targetName"
git tag $tagName
if ($LASTEXITCODE -ne 0) {
    Write-Error "Git tag creation failed! Aborting ship."
    exit 1
}

git push origin $tagName
if ($LASTEXITCODE -ne 0) {
    Write-Error "Git push tag failed! Aborting ship."
    exit 1
}

# Step 6: Verify Live GitHub Release Page (native PowerShell API call only)
Write-Host "[6/6] Polling GitHub REST API for live release assets..." -ForegroundColor Yellow
$maxAttempts = 12
$attempt = 1
$releaseUrl = "https://api.github.com/repos/dilithegit/Mahari/releases/tags/$tagName"

while ($attempt -le $maxAttempts) {
    Start-Sleep -Seconds 10
    try {
        $rel = Invoke-RestMethod -Uri $releaseUrl -ErrorAction Stop
        if ($rel.assets.Count -ge 2) {
            Write-Host "=========================================" -ForegroundColor Green
            Write-Host "LIVE GITHUB RELEASE CONFIRMED FOR $tagName" -ForegroundColor Green
            Write-Host "Release Name: $($rel.name)" -ForegroundColor Green
            Write-Host "Release Tag: $($rel.tag_name)" -ForegroundColor Green
            Write-Host "Assets Attached: $($rel.assets.Count)" -ForegroundColor Green
            $rel.assets | Select-Object name, size, browser_download_url | Format-Table -AutoSize
            Write-Host "Release URL: https://github.com/dilithegit/Mahari/releases/tag/$tagName" -ForegroundColor Green
            Write-Host "=========================================" -ForegroundColor Green
            exit 0
        }
    } catch {
        Write-Host "Waiting for GitHub Actions pipeline build... (attempt $attempt/$maxAttempts)" -ForegroundColor Gray
    }
    $attempt++
}

Write-Host "Ship complete, but release assets taking longer than expected to attach. Check https://github.com/dilithegit/Mahari/releases/tag/$tagName manually." -ForegroundColor Yellow
