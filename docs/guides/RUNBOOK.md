# Device Masker — Complete Command Reference

> **Standalone reference** — every command for building, testing, linting, releasing, and auditing the module.
> Reports are in XML/TXT format. No browser required.

---

## 🔨 Build Commands

```bash
# Full debug build (all 3 modules)
./gradlew assembleDebug

# Full release build — R8 full-mode shrinking + obfuscation
./gradlew assembleRelease

# Install debug APK to connected rooted device
./gradlew installDebug

# Clean rebuild (fixes stale Gradle caches)
./gradlew clean assembleDebug

# Module-specific debug builds (faster for targeted changes)
./gradlew :app:assembleDebug
./gradlew :xposed:assembleDebug
./gradlew :common:assembleDebug

# Kotlin-only compile (fastest check — no APK packaging, no dex)
./gradlew :app:compileDebugKotlin
./gradlew :xposed:compileDebugKotlin
./gradlew :common:compileDebugKotlin

# Dry run — show task execution plan without running
./gradlew :app:assembleRelease --dry-run
```

---

## 🎨 Format & Style Commands

```bash
# Auto-fix all Kotlin formatting issues (ALWAYS run before committing)
./gradlew spotlessApply

# Verify formatting — fails build if violations found (used in CI)
./gradlew spotlessCheck

# Check only Kotlin source files (skip Gradle KTS scripts)
./gradlew spotlessKotlinCheck

# Check only Gradle KTS files
./gradlew spotlessKotlinGradleCheck
```

---

## 🔍 Lint Commands

```bash
# Full cross-module lint (checkDependencies=true — recommended)
./gradlew lint

# Module-specific lint (faster for focused changes)
./gradlew :app:lint
./gradlew :xposed:lint
./gradlew :common:lint

# Debug variant lint (generates XML report)
./gradlew :app:lintDebug

# Release variant lint — stricter, catches R8-specific issues
./gradlew :app:lintRelease

# Lint vital checks only (very fast — subset used by R8 pre-release)
./gradlew :app:lintVitalRelease

# Snapshot known issues so future runs show only NEW issues
./gradlew :app:updateLintBaseline
```

> **Report location**: `app/build/reports/lint-results-debug.xml`
> View in IDE or any XML viewer to see issues with code context, severity, and fix suggestions.

---

## 🧪 Test Commands

```bash
# Run all unit tests across all modules
./gradlew test

# Module-specific unit tests
./gradlew :common:test          # Generator tests (29 tests — fastest)
./gradlew :app:test
./gradlew :xposed:test

# Run with verbose output (shows each test case result)
./gradlew :common:test --info

# Run a specific test class
./gradlew :common:test --tests "com.astrixforge.devicemasker.common.generators.IMEIGeneratorTest"
./gradlew :common:test --tests "com.astrixforge.devicemasker.common.generators.MACGeneratorTest"
./gradlew :common:test --tests "com.astrixforge.devicemasker.common.generators.SerialGeneratorTest"
./gradlew :common:test --tests "com.astrixforge.devicemasker.common.generators.AndroidIdGeneratorTest"

# Run on-device instrumented tests (requires connected rooted device)
./gradlew :app:connectedAndroidTest
```

> **Test report location**: `common/build/reports/tests/test/index.html`

---

## 🛡️ Release Validation Commands

```bash
# Full R8 release build — validates ALL ProGuard rules are correct
./gradlew assembleRelease
# Any R8 error = missing -keep rule → fix in proguard-rules.pro or consumer-rules.pro

# Verify critical classes were KEPT (not stripped) by R8
# Expected: each class APPEARS in the mapping file (means it was kept, not stripped)
Select-String -Path 'app\build\outputs\mapping\release\mapping.txt' -Pattern 'DeviceMaskerService|XposedEntry|HookEntry|AntiDetect'

# Full R8 mapping inspection — see all renamed/stripped classes
# File: app/build/outputs/mapping/release/mapping.txt
cat app/build/outputs/mapping/release/mapping.txt | grep 'com.astrixforge'

# APK size after R8 shrinking
Get-Item app\build\outputs\apk\release\*.apk | Select-Object Name, @{N='MB';E={[math]::Round($_.Length/1MB,2)}}

# Compare debug vs release APK sizes
Get-Item app\build\outputs\apk\debug\*.apk, app\build\outputs\apk\release\*.apk |
    Select-Object Name, @{N='MB';E={[math]::Round($_.Length/1MB,2)}}

# Inspect APK contents (lists DEX, assets, manifest — requires Android SDK build-tools)
%ANDROID_HOME%\build-tools\36.0.0\aapt2 dump file app/build/outputs/apk/release/app-release-unsigned.apk

# Verify xposed_init asset exists and has correct entry point (CRITICAL — LSPosed won't load without it)
# Expected content: com.astrixforge.devicemasker.hook.HookEntry
type xposed\src\main\assets\xposed_init
type app\src\main\assets\xposed_init 2>nul || echo "xposed_init not found in app module"
```

---

## ⚡ Quality Gate Pipeline

Run ALL gates in this order before every commit. A single failure = task is NOT done.

```bash
# Gate 1 — Format
./gradlew spotlessApply
./gradlew spotlessCheck
# Expected: BUILD SUCCESSFUL — zero formatting violations

# Gate 2 — Compile
./gradlew :app:compileDebugKotlin :common:compileDebugKotlin :xposed:compileDebugKotlin
# Expected: BUILD SUCCESSFUL — zero compilation errors

# Gate 3 — Lint
./gradlew lint
# Expected: BUILD SUCCESSFUL, 0 errors

# Gate 4 — Unit Tests
./gradlew test
# Expected: All tests GREEN — 0 failures, 0 errors

# Gate 5 — Debug Build
./gradlew assembleDebug
# Expected: BUILD SUCCESSFUL — APK in app/build/outputs/apk/debug/

# Gate 6 — Release Build (R8 Validation)
./gradlew assembleRelease
Select-String -Path 'app\build\outputs\mapping\release\mapping.txt' -Pattern 'DeviceMaskerService|XposedEntry|HookEntry|AntiDetect'
# Expected: each critical class in mapping (kept, not stripped)

# Gate 7 — Xposed Safety (see full section below — all 5 checks must return 0 results)

# Full pipeline one-liner (Gates 1-5)
./gradlew spotlessApply && ./gradlew spotlessCheck && ./gradlew lint && ./gradlew test && ./gradlew assembleDebug
```

---

## 🔎 Xposed Safety Checks (Grep)

**All checks must return 0 results.** Any result is a violation that must be fixed.

```bash
# 1. SAFETY: Unprotected hook callbacks — any after/before/replaceAny without runCatching is a crash risk
grep -rn 'after {\|before {\|replaceAny {' xposed/src --include='*.kt' | grep -v 'runCatching'

# 2. KEY MISMATCH: Hardcoded pref key strings (must use SharedPrefsKeys — any match = silent config failure)
grep -rn '"module_enabled"\|"app_enabled_"\|"spoof_value_"\|"spoof_enabled_"' app/src xposed/src --include='*.kt'

# 3. SECURITY: Non-secure random in generators (must use SecureRandom for all identifier generation)
grep -rn 'Random()\b' common/src --include='*.kt' | grep -v 'SecureRandom'

# 4. DEPRECATED: java.util.Random anywhere in xposed or common
grep -rn 'java.util.Random\b' xposed/src common/src --include='*.kt'

# 5. ANTI-PATTERN: Timber in :xposed (must use DualLog instead)
grep -rn 'Timber\.' xposed/src --include='*.kt'

# 6. MODULE BOUNDARY: Compose imports leaking into :common or :xposed
grep -rn 'import androidx.compose' common/src xposed/src --include='*.kt'

# 7. MODULE BOUNDARY: Hook logic leaking into :app or :common
grep -rn 'YukiBaseHooker\|BaseSpoofHooker\|onHook()' app/src common/src --include='*.kt'

# 8. BOOTLOOP RISK: system_server service code (verify all funs are wrapped in try-catch)
grep -rn 'fun ' xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/service/ --include='*.kt'

# 9. RECURSION RISK: Stack trace hooks without ThreadLocal re-entrance guards
grep -rn 'getStackTrace\|fillInStackTrace' xposed/src --include='*.kt' | grep -v 'ThreadLocal\|reentrant\|guard'

# 10. AIDL SAFETY: Direct Binder calls without null-check (service may be null at any time)
grep -rn 'service\.' xposed/src --include='*.kt' | grep -v '?\.' | grep -v 'runCatching'

# 11. ANTI-PATTERN: System.out / println in any module
grep -rn 'println\|System.out\|System.err' app/src xposed/src common/src --include='*.kt'

# 12. SERIALIZATION: @Serializable classes outside :common (must only be in :common)
grep -rn '@Serializable' app/src xposed/src --include='*.kt'

# 13. DEAD CODE: TODO/FIXME/HACK comments that need resolution
grep -rn 'TODO\|FIXME\|HACK\|XXX' app/src xposed/src common/src --include='*.kt'

# 14. VERIFY: xposed_init contains correct entry point
# Expected line: com.astrixforge.devicemasker.hook.HookEntry
type xposed\src\main\assets\xposed_init 2>nul || echo "xposed_init not found in xposed module"
type app\src\main\assets\xposed_init 2>nul || echo "xposed_init not found in app module"
```

---

## 🔬 Dependency & Analysis Commands

```bash
# Why is a transitive dependency included?
./gradlew :app:dependencyInsight --dependency <group:artifact>
./gradlew :app:dependencyInsight --dependency kotlinx-coroutines-core
./gradlew :app:dependencyInsight --dependency yukihookapi

# Full dependency tree per module
./gradlew :app:dependencies
./gradlew :xposed:dependencies
./gradlew :common:dependencies

# Runtime-only dependency tree (cleaner, no test dependencies)
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
./gradlew :xposed:dependencies --configuration releaseRuntimeClasspath

# Build scan — full profiling uploaded to scans.gradle.com
./gradlew assembleDebug --scan

# Local build performance profile (no upload)
./gradlew assembleDebug --profile
# Report: build/reports/profile/profile-*.html

# Check for dependency version conflicts
./gradlew :app:dependencies | grep -i 'conflict\|->.*FAILED'
```

---

## 🗂️ PowerShell One-Liners

```powershell
# Kotlin line count per module
Get-ChildItem -Recurse -Filter '*.kt' app/src | Measure-Object -Sum -Property Length
Get-ChildItem -Recurse -Filter '*.kt' xposed/src | Measure-Object -Sum -Property Length
Get-ChildItem -Recurse -Filter '*.kt' common/src | Measure-Object -Sum -Property Length

# Total across all modules
Get-ChildItem -Recurse -Filter '*.kt' app/src,xposed/src,common/src | Measure-Object -Sum -Property Length

# File count per module
Get-ChildItem -Recurse -Filter '*.kt' app/src | Measure-Object | Select-Object Count
Get-ChildItem -Recurse -Filter '*.kt' xposed/src | Measure-Object | Select-Object Count
Get-ChildItem -Recurse -Filter '*.kt' common/src | Measure-Object | Select-Object Count

# Find Kotlin files modified in the last 24 hours
Get-ChildItem -Recurse -Filter '*.kt' | Where-Object { $_.LastWriteTime -gt (Get-Date).AddHours(-24) }

# Find files modified in last N hours (change -24 to any number)
Get-ChildItem -Recurse -Filter '*.kt' | Where-Object { $_.LastWriteTime -gt (Get-Date).AddHours(-2) } | Select-Object Name, LastWriteTime, DirectoryName

# Search for a class/function/pattern across all modules
Select-String -Path 'app/src','xposed/src','common/src' -Recurse -Pattern 'BaseSpoofHooker' -Include '*.kt'
Select-String -Path 'app/src','xposed/src','common/src' -Recurse -Pattern 'getSpoofValue' -Include '*.kt'

# Find the largest Kotlin files (potential refactoring candidates)
Get-ChildItem -Recurse -Filter '*.kt' app/src,xposed/src,common/src |
    Sort-Object Length -Descending | Select-Object -First 10 Name, @{N='Lines';E={($_ | Get-Content).Count}}, DirectoryName

# APK size comparison (debug vs release)
Get-Item app\build\outputs\apk\debug\*.apk, app\build\outputs\apk\release\*.apk |
    Select-Object Name, @{N='MB';E={[math]::Round($_.Length/1MB,2)}}

# Check lint XML report for errors only
Select-String -Path 'app\build\reports\lint-results-debug.xml' -Pattern 'severity="Error"' | Measure-Object | Select-Object Count

# View R8 mapping for a specific class
Select-String -Path 'app\build\outputs\mapping\release\mapping.txt' -Pattern 'DeviceMaskerService'
Select-String -Path 'app\build\outputs\mapping\release\mapping.txt' -Pattern 'com.astrixforge'
```

---

## 📋 Report Locations (XML / TXT)

| Report               | Path                                                  | Format         |
| -------------------- | ----------------------------------------------------- | -------------- |
| Lint (debug)         | `app/build/reports/lint-results-debug.xml`            | XML            |
| Lint (release)       | `app/build/reports/lint-results-release.xml`          | XML            |
| Unit tests           | `common/build/reports/tests/test/`                    | XML (per test) |
| Build profile        | `build/reports/profile/profile-*.txt`                 | TXT            |
| R8 mapping           | `app/build/outputs/mapping/release/mapping.txt`       | TXT            |
| R8 seeds             | `app/build/outputs/mapping/release/seeds.txt`         | TXT            |
| R8 usage             | `app/build/outputs/mapping/release/usage.txt`         | TXT            |
| ProGuard config dump | `app/build/outputs/mapping/release/configuration.txt` | TXT            |

---

## 🚀 All-in-One Audit Script

Runs **every** check — all 10 safety grep checks + all 7 Gradle quality gates — and saves a
structured TXT report to `scripts/logs/audit-report.txt`. Designed to be read by both humans and AI agents.

### Run the Audit

```powershell
.\scripts\run-audit.ps1
```

> Output saved to: `scripts/logs/audit-report.txt`
> Encoding: UTF-8, structured sections, clear PASS/FAIL labels — safe for AI agent consumption.

### What the Script Does

1. Runs all 10 Xposed safety grep checks (0-result checks)
2. Runs Gate 1 (spotlessCheck)
3. Runs Gate 2 (compileDebugKotlin — all 3 modules)
4. Runs Gate 3 (lint)
5. Runs Gate 4 (test)
6. Runs Gate 5 (assembleDebug)
7. Summarises overall PASS / FAIL with a count of violations

---

_Last Updated: 2026-03-13_
