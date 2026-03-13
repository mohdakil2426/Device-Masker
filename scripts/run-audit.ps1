# =============================================================================
# Device Masker — All-in-One Quality Audit
# =============================================================================
# Runs:
#   [A] 10 Xposed safety grep checks  (must all return 0 results)
#   [B]  5 Gradle quality gates       (spotlessCheck, compile, lint, test, build)
#
# Output: docs/audit-report.txt  (UTF-8, structured for AI agent consumption)
# Usage:  .\docs\run-audit.ps1   (run from project root)
# =============================================================================

Set-StrictMode -Version Latest
$ErrorActionPreference = "Continue"

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------
$ReportPath  = "scripts\logs\audit-report.txt"
$ProjectRoot = $PSScriptRoot | Split-Path -Parent
Set-Location $ProjectRoot

$Timestamp   = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$lines       = [System.Collections.Generic.List[string]]::new()
$totalFail   = 0
$totalPass   = 0

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
function Write-Section($title) {
    $bar = "=" * 72
    $script:lines.Add("")
    $script:lines.Add($bar)
    $script:lines.Add("  $title")
    $script:lines.Add($bar)
}

function Write-SubSection($title) {
    $script:lines.Add("")
    $script:lines.Add("--- $title ---")
}

function Grep-Check($id, $description, [string[]]$paths, $pattern, $excludePattern) {
    $hits = @()
    foreach ($p in $paths) {
        $fullPath = Join-Path $ProjectRoot $p
        if (Test-Path $fullPath) {
            $found = Get-ChildItem -Path $fullPath -Recurse -Filter "*.kt" -ErrorAction SilentlyContinue |
                     Select-String -Pattern $pattern -ErrorAction SilentlyContinue
            if ($excludePattern) {
                $found = $found | Where-Object { $_ -notmatch $excludePattern }
            }
            $hits += $found
        }
    }

    if ($hits.Count -gt 0) {
        $script:lines.Add("[FAIL] $id : $description")
        $script:lines.Add("       Violations ($($hits.Count) total):")
        $hits | ForEach-Object {
            $script:lines.Add("         $($_.Filename):$($_.LineNumber): $($_.Line.Trim())")
        }
        $script:totalFail++
    } else {
        $script:lines.Add("[PASS] $id : $description")
        $script:totalPass++
    }
}

function Gradle-Gate($id, $description, $gradleTask) {
    $script:lines.Add("")
    $script:lines.Add(">>> Running: ./gradlew $gradleTask")
    $output = & cmd /c "gradlew.bat $gradleTask 2>&1"
    $exitCode = $LASTEXITCODE
    $outputText = $output -join "`n"

    # Capture last 30 lines of output for context
    $tail = ($output | Select-Object -Last 30) -join "`n"
    $script:lines.Add($tail)

    if ($exitCode -eq 0 -and ($outputText -match "BUILD SUCCESSFUL")) {
        $script:lines.Add("")
        $script:lines.Add("[PASS] $id : $description")
        $script:totalPass++
    } else {
        $script:lines.Add("")
        $script:lines.Add("[FAIL] $id : $description  (exit code: $exitCode)")
        $script:totalFail++
    }
}

# =============================================================================
# HEADER
# =============================================================================
$lines.Add("DEVICE MASKER — QUALITY AUDIT REPORT")
$lines.Add("Generated : $Timestamp")
$lines.Add("Project   : $ProjectRoot")
$lines.Add("Format    : Structured TXT — safe for AI agent consumption")
$lines.Add("=" * 72)

# =============================================================================
# SECTION A — XPOSED SAFETY GREP CHECKS
# =============================================================================
Write-Section "SECTION A : XPOSED SAFETY GREP CHECKS  (all must be [PASS])"
$lines.Add("")
$lines.Add("Rule: Every check must return 0 matches.")
$lines.Add("      Any [FAIL] = violation that must be fixed before release.")
$lines.Add("")

Grep-Check "A01" "Unprotected hook callbacks (after/before/replaceAny without runCatching)" `
    @("xposed/src") `
    "^\s+(after|before|replaceAny)\s*\{" `
    "runCatching"

Grep-Check "A02" "Hardcoded pref key strings (must delegate to SharedPrefsKeys)" `
    @("app/src", "xposed/src") `
    '"module_enabled"|"app_enabled_"|"spoof_value_"|"spoof_enabled_"' `
    $null

Grep-Check "A03" "Non-secure Random() in generators (must use SecureRandom)" `
    @("common/src") `
    "Random\(\)" `
    "SecureRandom"

Grep-Check "A04" "java.util.Random usage (deprecated — use SecureRandom)" `
    @("xposed/src", "common/src") `
    "java\.util\.Random" `
    $null

Grep-Check "A05" "Timber logging in :xposed module (must use DualLog)" `
    @("xposed/src") `
    "Timber\." `
    $null

Grep-Check "A06" "Compose imports leaking into :common or :xposed" `
    @("common/src", "xposed/src") `
    "import androidx\.compose" `
    $null

Grep-Check "A07" "Hook logic leaking into :app or :common (YukiBaseHooker/BaseSpoofHooker)" `
    @("app/src", "common/src") `
    "YukiBaseHooker|BaseSpoofHooker" `
    $null

Grep-Check "A08" "System.out / println in any module" `
    @("app/src", "xposed/src", "common/src") `
    "println\b|System\.out\.|System\.err\." `
    $null

Grep-Check "A09" "@Serializable data classes outside :common module" `
    @("app/src", "xposed/src") `
    "@Serializable" `
    $null

Grep-Check "A10" "Unguarded stack trace hooks (ThreadLocal re-entrance missing)" `
    @("xposed/src") `
    "getStackTrace|fillInStackTrace" `
    "ThreadLocal|reentrant|guard"

# A section subtotal
$lines.Add("")
$lines.Add("Section A Result: $($totalPass) passed, $($totalFail) failed so far")

# =============================================================================
# SECTION B — GRADLE QUALITY GATES
# =============================================================================
$gradleStart = $totalFail
Write-Section "SECTION B : GRADLE QUALITY GATES  (all must be [PASS])"
$lines.Add("")
$lines.Add("Gates run in mandatory order. A single failure means code is NOT shippable.")
$lines.Add("")

Write-SubSection "Gate 1 — Spotless Format Check"
Gradle-Gate "B01" "spotlessCheck — zero formatting violations" "spotlessCheck"

Write-SubSection "Gate 2 — Kotlin Compile (all 3 modules)"
Gradle-Gate "B02" "compileDebugKotlin — zero compile errors" ":app:compileDebugKotlin :common:compileDebugKotlin :xposed:compileDebugKotlin"

Write-SubSection "Gate 3 — Android Lint"
Gradle-Gate "B03" "lint — 0 errors across all modules" "lint"

Write-SubSection "Gate 4 — Unit Tests"
Gradle-Gate "B04" "test — all tests GREEN, 0 failures" "test"

Write-SubSection "Gate 5 — Debug Build"
Gradle-Gate "B05" "assembleDebug — APK produced successfully" "assembleDebug"

# =============================================================================
# SUMMARY
# =============================================================================
Write-Section "SUMMARY"
$lines.Add("")
$lines.Add("  Total checks run : $($totalPass + $totalFail)")
$lines.Add("  PASSED           : $totalPass")
$lines.Add("  FAILED           : $totalFail")
$lines.Add("")

if ($totalFail -eq 0) {
    $lines.Add("  OVERALL RESULT   : *** PASS — All quality gates green. Safe to commit. ***")
} else {
    $lines.Add("  OVERALL RESULT   : *** FAIL — $totalFail check(s) failed. Do NOT commit. ***")
    $lines.Add("")
    $lines.Add("  ACTION REQUIRED:")
    $lines.Add("    Search this file for '[FAIL]' to find each violation.")
    $lines.Add("    Fix the root cause. Re-run .\scripts\run-audit.ps1 until all checks pass.")
}

$lines.Add("")
$lines.Add("  Lint XML report  : app\build\reports\lint-results-debug.xml")
$lines.Add("  Test XML reports : common\build\reports\tests\test\")
$lines.Add("  R8 mapping       : app\build\outputs\mapping\release\mapping.txt")
$lines.Add("")
$lines.Add("=" * 72)
$lines.Add("END OF REPORT — $Timestamp")
$lines.Add("=" * 72)

# =============================================================================
# WRITE REPORT
# =============================================================================
$outPath = Join-Path $ProjectRoot $ReportPath
$lines | Set-Content -Path $outPath -Encoding UTF8

Write-Host ""
Write-Host "Audit complete." -ForegroundColor Cyan
Write-Host "  Passed : $totalPass" -ForegroundColor Green
if ($totalFail -gt 0) {
    Write-Host "  Failed : $totalFail" -ForegroundColor Red
    Write-Host "  Result : FAIL" -ForegroundColor Red
} else {
    Write-Host "  Failed : 0" -ForegroundColor Green
    Write-Host "  Result : PASS" -ForegroundColor Green
}
Write-Host ""
Write-Host "  Report saved to: $ReportPath" -ForegroundColor Cyan
Write-Host "  Open it in any editor or pass scripts\logs\audit-report.txt to an AI agent." -ForegroundColor DarkGray
Write-Host ""
