# Device Masker — LSPosed Privacy Module

> **Advanced Android Xposed/LSPosed module** for spoofing device identifiers and hiding hook injection.
> Kotlin 2.3.0 | Android SDK 36 | Jetpack Compose | YukiHookAPI 1.3.1 | AIDL IPC

**Memory Bank**: The `memory-bank/` directory contains the source of truth for project context, patterns, and progress tracking. Read ALL files for deep project understanding. **NEVER SKIP THIS STEP.**

**RESPECT ALL RULES**: You MUST follow every rule, guideline, principle, coding standard and best practice documented below. No exceptions, no shortcuts, no lazy work, full efforts. Respect project patterns, shared contracts, and existing code style consistency.

---

## Architecture (Hybrid AIDL + XSharedPreferences)

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                        :app (UI Layer — Compose MVVM)                  │
│  MainActivity ──► NavHost (Home │ Groups │ Settings │ Diagnostics)     │
│  ViewModels   ──► SpoofRepository ──► ConfigManager (app-side)        │
│                    ├─ Local JSON file (filesDir/config.json)           │
│                    ├─ ConfigSync ──► XposedPrefs (MODE_WORLD_READABLE) │
│                    └─ ServiceClient ──► AIDL ──► system_server         │
├─────────────────────────────────────────────────────────────────────────┤
│                        :common (Shared Models & IPC)                   │
│  SpoofType(24) │ SpoofGroup │ JsonConfig │ AppConfig │ DeviceIdentifier│
│  DeviceProfilePreset(10) │ Carrier(65+) │ SIMConfig │ LocationConfig   │
│  Generators: IMEI, IMSI, ICCID, MAC, Serial, Phone, UUID             │
│  SharedPrefsKeys (SINGLE SOURCE OF TRUTH for pref keys)               │
│  IDeviceMaskerService.aidl (14 methods)                               │
├─────────────────────────────────────────────────────────────────────────┤
│                        :xposed (Hook Layer — YukiHookAPI)              │
│  XposedEntry: loadSystem { SystemServiceHooker }                      │
│               loadApp   { AntiDetect → Device → Network → Advertising │
│                            → System → Location → Sensor → WebView }   │
│  BaseSpoofHooker: AIDL first ──► XSharedPreferences fallback          │
│  Service: DeviceMaskerService + ConfigManager + ServiceBridge          │
│  Utils: ClassCache (LRU-100) │ DualLog │ HookMetrics │ PrefsHelper    │
└─────────────────────────────────────────────────────────────────────────┘
```

- **Primary (Real-time)**: AIDL service (`DeviceMaskerService`) in `system_server` — config changes apply instantly.
- **Fallback (Cached)**: `XSharedPreferences` via `MODE_WORLD_READABLE` — requires target app restart.
- **Config write path**: UI → ConfigManager → JSON file + XposedPrefs + AIDL service.
- **Config read path**: Hooker → `BaseSpoofHooker.getSpoofValue()` → AIDL first, XSharedPreferences fallback.

---

## Project Structure

```text
devicemasker/
├── app/                    # :app — Main application (UI + MVVM)
│   ├── data/               # ConfigSync, XposedPrefs, DataStore
│   ├── repository/         # SpoofRepository, AppScopeRepository
│   ├── service/            # ServiceClient (AIDL), ConfigManager, LogManager
│   └── ui/                 # MainActivity, navigation/, screens/, theme/
├── common/                 # :common — Shared models, generators, AIDL
│   ├── aidl/               # IDeviceMaskerService.aidl (IPC Contract)
│   └── kotlin/.../common/
│       ├── generators/     # Secure value generation logic
│       ├── models/         # Carrier, SIMConfig, LocationConfig
│       └── [SpoofType, SharedPrefsKeys, JsonConfig, AppConfig]
└── xposed/                 # :xposed — Hook logic (YukiHookAPI)
    ├── hooker/             # 8+ Hookers (Device, Network, System, etc.)
    ├── service/            # DeviceMaskerService (system_server), ConfigManager
    ├── utils/              # ClassCache (LRU), DualLog, HookHelper
    └── XposedEntry.kt      # Module entry: loadSystem + loadApp
```

---

## Critical Rules

### File & Module Boundaries

| Type                  | Correct Location                         | Wrong                                     |
| --------------------- | ---------------------------------------- | ----------------------------------------- |
| Shared models/enums   | `:common` module                         | Duplicating models in `:app` or `:xposed` |
| AIDL interface        | `common/src/main/aidl/`                  | Defining AIDL in `:app` or `:xposed`      |
| Pref key generation   | `SharedPrefsKeys` in `:common`           | Hardcoding keys in `:app` or `:xposed`    |
| Value generators      | `common/generators/`                     | Generating values in hookers directly     |
| Hook logic            | `:xposed/hooker/`                        | Putting hooks in `:app` or `:common`      |
| UI Compose screens    | `:app/ui/screens/`                       | Mixing UI into `:common` or `:xposed`     |
| system_server service | `:xposed/service/`                       | Running AIDL service from `:app`          |
| Config persistence    | `ConfigManager` (both app-side & xposed) | Direct file I/O from hookers              |

### Xposed Safety Rules (⚠️ BOOTLOOP PREVENTION)

1. **ALWAYS wrap system_server hooks in try-catch** — crashes cause bootloop!
2. **Load `SystemServiceHooker` in `loadSystem {}`** — before any app hooks.
3. **Load `AntiDetectHooker` FIRST in `loadApp {}`** — before any spoofing hooks.
4. **Use `optional()` for uncertain methods** — prevents crashes on different Android versions.
5. **Never block essential class loading** — always allow `androidx.*`, `kotlin.*`, `java.*`, `android.*`.
6. **Skip critical packages** — `android`, `com.android.systemui`, `com.android.phone` in loadApp.
7. **Use `runCatching { }` in all hooker callbacks** — never crash target apps.
8. **Use `ThreadLocal` re-entrance guards** — prevent infinite recursion in stack trace hooks.

### Configuration Rules

- **`SharedPrefsKeys`** in `:common` is the **SINGLE SOURCE OF TRUTH** for all preference keys.
- Both `XposedPrefs` (`:app`) and `PrefsKeys` (`:xposed`) **MUST delegate** to `SharedPrefsKeys`.
- **XSharedPreferences CACHES values** — config changes via this path require target app restart.
- **AIDL provides real-time updates** — no restart needed when service is available.
- **Correlation groups MUST be respected** — SIM values must use same carrier, location values same country.

### Data Integrity Rules

- **`SIMConfig` validates on construction** — IMSI must start with carrier MCC/MNC, phone must start with country code.
- **`DeviceProfilePreset` values are applied as a complete set** — never mix fields from different presets.
- **IMEI must pass Luhn validation** — use `IMEIGenerator` which guarantees valid check digits.
- **All generators use `SecureRandom`** — never use `Random()` for security-sensitive values.

---

## Universal Development Principles

1. **Safety First** — every hook callback wrapped in `runCatching { }`, system_server code in try-catch.
2. **Immutable Data** — use `copy()` for all model updates, `val` properties, `@Serializable` data classes.
3. **Fail Gracefully** — return fallback values instead of crashing; use `optional()` for uncertain methods.
4. **Single Source of Truth** — `SharedPrefsKeys` for keys, `SpoofType` for identifiers, `JsonConfig` for config.
5. **Correlate Values** — SIM/Location/Hardware values must be generated together via `CorrelationGroup`.
6. **DRY** — use `BaseSpoofHooker` for common hook patterns, delegates for key generation.
7. **Performance** — use `ClassCache` (LRU) for class lookups, pre-load common classes at init.
8. **Thread Safety** — `AtomicReference`, `ConcurrentHashMap`, `@Volatile`, `synchronized` in system_server.
9. **Document Intent** — comments explain WHY, not WHAT; use KDoc for public APIs.
10. **KISS** — simple, maintainable designs over clever abstractions.

---

## Tech Stack

| Layer          | Stack                                                               |
| -------------- | ------------------------------------------------------------------- |
| **Language**   | Kotlin 2.3.0, Java 25                                               |
| **Platform**   | Android SDK 36 (Android 16 / Baklava), minSdk 26                    |
| **UI**         | Jetpack Compose (BOM 2026.02.01), Material 3 Expressive (1.4.0)     |
| **Hooking**    | YukiHookAPI 1.3.1, LSPosed (API 82), KavaRef                        |
| **IPC**        | AIDL Binder (system_server ↔ app processes), ContentProvider bridge |
| **Arch**       | MVVM, Multi-Module Gradle (`:app`, `:xposed`, `:common`)            |
| **Data**       | kotlinx.serialization (JSON), SharedPreferences, AtomicFile         |
| **Build**      | Gradle (Kotlin DSL), KSP, Spotless (ktfmt 0.54)                     |
| **Logging**    | Timber (app), DualLog (xposed → YLog + buffer), HookMetrics         |
| **Navigation** | Jetpack Navigation Compose, spring-based animated transitions       |

---

## Commands

### 🔨 Build Commands

```bash
# Full debug build (all 3 modules)
./gradlew assembleDebug

# Full release build with R8 full-mode shrinking + obfuscation
./gradlew assembleRelease

# Install debug APK directly to connected rooted device
./gradlew installDebug

# Module-specific builds (faster for targeted changes)
./gradlew :app:assembleDebug
./gradlew :common:assembleDebug
./gradlew :xposed:assembleDebug

# Compile Kotlin only (fastest check — no APK packaging)
./gradlew :app:compileDebugKotlin
./gradlew :xposed:compileDebugKotlin
./gradlew :common:compileDebugKotlin

# Clean + full rebuild (fixes stale caches)
./gradlew clean assembleDebug
```

---

### 🎨 Format & Style Commands

```bash
# Auto-fix all Kotlin formatting issues (run FIRST before committing)
./gradlew spotlessApply

# Check formatting without modifying files (use in CI)
./gradlew spotlessCheck

# Check only Kotlin source files (skip Gradle scripts)
./gradlew spotlessKotlinCheck

# Check only Gradle KTS files
./gradlew spotlessKotlinGradleCheck
```

---

### 🔍 Lint Commands — Android Lint

```bash
# Run lint across ALL modules (recommended — enables cross-module analysis)
./gradlew lint

# Module-specific lint (faster for focused changes)
./gradlew :app:lint
./gradlew :common:lint
./gradlew :xposed:lint

# Lint with HTML report (opens in browser at app/build/reports/lint-results.html)
./gradlew :app:lintDebug

# Lint the release variant (stricter — catches R8-specific issues)
./gradlew :app:lintRelease

# Lint vital checks only (subset, very fast — same as what R8 runs pre-release)
./gradlew :app:lintVitalRelease

# Generate lint baseline (snapshot known issues so future runs show only NEW issues)
./gradlew :app:updateLintBaseline
```

> **Reports Location**: `app/build/reports/lint-results-debug.html`  
> Open in browser to see issues with code context, severity, and fix suggestions.

---

### 🔬 Deep Analysis Commands

```bash
# Full quality gate pipeline — run ALL of these in order before every commit:
./gradlew spotlessApply && ./gradlew spotlessCheck && ./gradlew lint && ./gradlew assembleDebug

# Dependency insight — see why a transitive dependency is pulled in
./gradlew :app:dependencyInsight --dependency <group:artifact>
# Example: check why coroutines is included
./gradlew :app:dependencyInsight --dependency kotlinx-coroutines-core

# Full dependency tree for each module
./gradlew :app:dependencies
./gradlew :common:dependencies
./gradlew :xposed:dependencies

# Only runtime dependencies tree (cleaner output)
./gradlew :app:dependencies --configuration releaseRuntimeClasspath

# Check for outdated dependencies (requires gradle-versions-plugin if added)
# ./gradlew dependencyUpdates

# Build scan — full profiling and analysis (uploads to scans.gradle.com)
./gradlew assembleDebug --scan

# Profile build performance (local, no upload)
./gradlew assembleDebug --profile
# Report: build/reports/profile/profile-*.html

# Task dependency graph for a specific task
./gradlew :app:assembleRelease --dry-run
```

---

### 🧪 Test Commands

```bash
# Run all unit tests across all modules
./gradlew test

# Module-specific unit tests
./gradlew :common:test
./gradlew :app:test
./gradlew :xposed:test

# Run tests with verbose output (shows each test case result)
./gradlew :common:test --info

# Run a specific test class
./gradlew :common:test --tests "com.astrixforge.devicemasker.common.generators.IMEIGeneratorTest"

# Run tests and generate HTML report
./gradlew :common:test
# Report: common/build/reports/tests/test/index.html

# Run instrumented (on-device) tests
./gradlew :app:connectedAndroidTest
```

---

### 🛡️ Release Validation Commands

```bash
# Full release build with R8 (validates ProGuard rules are complete)
./gradlew assembleRelease

# Inspect the R8 mapping file (see what was renamed/stripped)
# File: app/build/outputs/mapping/release/mapping.txt
cat app/build/outputs/mapping/release/mapping.txt | grep 'com.astrixforge'

# Verify the release APK was produced
ls app/build/outputs/apk/release/

# Check APK size after R8 shrinking
# Windows:
Get-Item app\build\outputs\apk\release\*.apk | Select-Object Name, @{N='SizeMB';E={[math]::Round($_.Length/1MB,2)}}

# Inspect APK contents (lists all DEX, assets, manifest entries)
# Requires Android SDK build-tools:
%ANDROID_HOME%\build-tools\36.0.0\aapt2 dump file app/build/outputs/apk/release/app-release-unsigned.apk

# Check that xposed_init asset is present (CRITICAL for LSPosed to load module)
# Must contain: com.astrixforge.devicemasker.hook.HookEntry
%ANDROID_HOME%\build-tools\36.0.0\aapt dump file app/build/outputs/apk/release/app-release-unsigned.apk assets/xposed_init
```

---

### 🔎 Xposed-Specific Issue Finder Commands (Grep)

These grep commands scan the codebase for common Xposed anti-patterns and safety violations.

```bash
# ⚠️  SAFETY: Find hook callbacks NOT wrapped in runCatching { }
# Any 'after {' or 'before {' or 'replaceAny {' without runCatching is a crash risk
grep -rn 'after {\|before {\|replaceAny {' xposed/src --include='*.kt' | grep -v 'runCatching'

# ⚠️  BOOTLOOP RISK: Find system_server code NOT wrapped in try-catch
# All code in DeviceMaskerService, ConfigManager, SystemServiceHooker must be in try-catch
grep -rn 'fun ' xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/service/ --include='*.kt'

# 🔑 KEY MISMATCH: Find hardcoded preference key strings (not using SharedPrefsKeys)
# Should return 0 results. Any match = silent config failure risk.
grep -rn '"module_enabled"\|"app_enabled_"\|"spoof_value_"\|"spoof_enabled_"' app/src xposed/src --include='*.kt'

# 🎲 SECURITY: Find usage of Random() instead of SecureRandom in generators
# All generators MUST use SecureRandom for security-critical values
grep -rn 'Random()\b' common/src --include='*.kt' | grep -v 'SecureRandom'

# 📦 MODULE BOUNDARY: Find UI/Compose code leaking into :common or :xposed modules
grep -rn 'import androidx.compose' common/src xposed/src --include='*.kt'

# 📦 MODULE BOUNDARY: Find hook logic leaking into :app or :common modules
grep -rn 'YukiBaseHooker\|BaseSpoofHooker\|onHook()' app/src common/src --include='*.kt'

# 🔁 RECURSION RISK: Find stack trace hooks without ThreadLocal re-entrance guards
grep -rn 'getStackTrace\|fillInStackTrace' xposed/src --include='*.kt' | grep -v 'ThreadLocal\|reentrant\|guard'

# 📍 DEPRECATED: Find any usage of the old Random() instead of SecureRandom
grep -rn 'java.util.Random\b' xposed/src common/src --include='*.kt'

# 🔗 AIDL SAFETY: Find direct Binder calls without null-check (service may be null)
grep -rn 'service\.' xposed/src --include='*.kt' | grep -v '?\.' | grep -v 'runCatching'

# 🧹 DEAD CODE: Find TODO/FIXME/HACK comments that need resolution
grep -rn 'TODO\|FIXME\|HACK\|XXX' app/src xposed/src common/src --include='*.kt'

# 📏 COMPLEXITY: Find functions over 50 lines (may need refactoring)
# Counts non-blank lines per function — report manually if any file is too large
wc -l xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/*.kt

# 🚫 ANTI-PATTERN: Find Timber.log usage in xposed module (should use DualLog)
grep -rn 'Timber\.' xposed/src --include='*.kt'

# 🚫 ANTI-PATTERN: Find System.out.println in any module
grep -rn 'println\|System.out\|System.err' app/src xposed/src common/src --include='*.kt'

# 🔐 SERIALIZATION: Find @Serializable data classes missing @Keep or not in common module
grep -rn '@Serializable' app/src xposed/src --include='*.kt'

# 📋 VERIFY: Confirm xposed_init asset file contains the correct entry point
# Expected: com.astrixforge.devicemasker.hook.HookEntry
type xposed\src\main\assets\xposed_init 2>nul || echo 'xposed_init not found in xposed module'
type app\src\main\assets\xposed_init 2>nul || echo 'xposed_init not found in app module'
```

---

### 🗂️ Useful One-Liners (Windows PowerShell)

```powershell
# Count total lines of Kotlin code per module
Get-ChildItem -Recurse -Filter '*.kt' app/src | Measure-Object -Sum -Property Length
Get-ChildItem -Recurse -Filter '*.kt' xposed/src | Measure-Object -Sum -Property Length
Get-ChildItem -Recurse -Filter '*.kt' common/src | Measure-Object -Sum -Property Length

# Find all Kotlin files modified in the last 24 hours
Get-ChildItem -Recurse -Filter '*.kt' | Where-Object { $_.LastWriteTime -gt (Get-Date).AddHours(-24) }

# Search for a class/function across all modules
Select-String -Path 'app/src','xposed/src','common/src' -Recurse -Pattern 'BaseSpoofHooker' -Include '*.kt'

# Show APK size before and after release build
Get-Item app\build\outputs\apk\debug\*.apk, app\build\outputs\apk\release\*.apk |
    Select-Object Name, @{N='MB';E={[math]::Round($_.Length/1MB,2)}}
```

---

## Coding Standards — Kotlin

### Formatting & Style

| Setting             | Value              | Enforced By        |
| ------------------- | ------------------ | ------------------ |
| Formatter           | ktfmt 0.54         | Spotless           |
| Style               | kotlinlangStyle    | Spotless config    |
| Indent              | 4 spaces (no tabs) | ktfmt              |
| Line length         | ~100 characters    | ktfmt              |
| Trailing whitespace | None               | Spotless auto-trim |
| End of file         | Single newline     | Spotless           |

### Imports

- No wildcard imports (`import foo.*`)
- Group order: Android → Compose → third-party → project modules
- Remove unused imports (handled by ktfmt)

### Naming Conventions

| Type            | Convention                   | Example                          |
| --------------- | ---------------------------- | -------------------------------- |
| Classes/Objects | PascalCase                   | `DeviceHooker`, `SpoofGroup`     |
| Functions       | camelCase                    | `getSpoofValue()`, `onHook()`    |
| Properties      | camelCase                    | `isEnabled`, `packageName`       |
| Constants       | SCREAMING_SNAKE_CASE         | `KEY_MODULE_ENABLED`, `TAG`      |
| Composables     | PascalCase                   | `HomeScreen()`, `BottomNavBar()` |
| State classes   | PascalCase + `State` suffix  | `HomeState`, `SettingsState`     |
| ViewModels      | PascalCase + `ViewModel`     | `HomeViewModel`                  |
| AIDL Interfaces | `I` + PascalCase + `Service` | `IDeviceMaskerService`           |
| Services        | PascalCase + `Service`       | `DeviceMaskerService`            |
| Hookers         | PascalCase + `Hooker`        | `AntiDetectHooker`               |
| Generators      | PascalCase + `Generator`     | `IMEIGenerator`                  |

### Kotlin Coding Patterns

```kotlin
// Immutable data updates — always copy()
val updated = group.copy(isEnabled = true, updatedAt = System.currentTimeMillis())

// Null safety — ?.let or ?: return
val group = config.getGroupForApp(packageName) ?: return null

// Exception handling in hooks — runCatching
runCatching {
    method { name = "getDeviceId" }.hook {
        after { result = spoofedValue }
    }
}.onFailure { e -> DualLog.warn(TAG, "Hook failed", e) }

// Hybrid config lookup (BaseSpoofHooker pattern)
val value = getSpoofValue(SpoofType.IMEI) { IMEIGenerator.generate() }

// Thread-safe state in system_server
private val config = AtomicReference<JsonConfig>(JsonConfig.createDefault())
private val filterCounts = ConcurrentHashMap<String, AtomicInteger>()
```

### Compose Guidelines

- Use `collectAsStateWithLifecycle()` for `StateFlow` in UI
- Prefer `derivedStateOf` for expensive computations
- Add stable `key` to all `LazyColumn` items
- Use `MaterialTheme.colorScheme.*` — never hardcode colors
- Use `AppMotion.*` springs for animations
- Mark data classes with `@Immutable` or `@Stable` for Compose stability

### Error Handling

- **In Hookers**: Wrap uncertain methods with `runCatching { }` to prevent crashes
- **In system_server**: ALWAYS wrap in try-catch (crashes cause bootloop!)
- Use `optional()` for methods that may not exist on all Android versions
- Log errors with `DualLog.warn(TAG, message, throwable)`
- Never crash target apps — fail gracefully with fallback values
- Use `hookClassSafe()` for optional class hooking

---

## Key Patterns

| Pattern                   | Implementation                                                                  |
| ------------------------- | ------------------------------------------------------------------------------- |
| **Hook order**            | `loadSystem { SystemService }` → `loadApp { AntiDetect → spoof hooks }`         |
| **Config cascade**        | AIDL service → XSharedPreferences → lazy fallback → hardcoded default           |
| **Value correlation**     | `CorrelationGroup` enum — SIM_CARD, LOCATION, DEVICE_HARDWARE, NONE             |
| **Carrier sync**          | `updateGroupWithCarrier()` auto-syncs SIM + Location to match country           |
| **Device profiles**       | `DeviceProfilePreset` applies ALL Build.\* fields as a consistent set           |
| **Class caching**         | `ClassCache` LRU (max 100) with negative cache for not-found classes            |
| **Dual logging**          | `DualLog` → YLog (Logcat/LSPosed) + internal buffer (AIDL export)               |
| **Pref key delegation**   | `:app` `XposedPrefs` + `:xposed` `PrefsKeys` both delegate to `SharedPrefsKeys` |
| **Anti-detection layers** | Stack trace filter + `/proc/self/maps` hiding + PackageManager hiding           |
| **Thread safety**         | `AtomicReference`, `ConcurrentHashMap`, `ConcurrentLinkedDeque`, `@Volatile`    |
| **UI state**              | `StateFlow` + `collectAsStateWithLifecycle()` + immutable state classes         |
| **Navigation**            | 3-tab BottomNav (Home, Groups, Settings) + detail screens                       |
| **Config write path**     | UI → SpoofRepo → ConfigManager → JSON + XPrefs + AIDL service                   |

---

## Key Architecture Notes

### AIDL Service Components

| Component                   | Location          | Purpose                                     |
| --------------------------- | ----------------- | ------------------------------------------- |
| `IDeviceMaskerService.aidl` | `:common/aidl`    | AIDL interface (14 methods)                 |
| `DeviceMaskerService.kt`    | `:xposed/service` | Singleton service in system_server          |
| `ConfigManager.kt`          | `:xposed/service` | Atomic file config (`/data/misc/`)          |
| `ServiceBridge.kt`          | `:xposed/service` | ContentProvider for binder discovery        |
| `SystemServiceHooker.kt`    | `:xposed/hooker`  | Boot-time service init (AMS + SystemServer) |
| `ServiceClient.kt`          | `:app/service`    | UI client with retry + exponential backoff  |

### Hooker Inventory (8 active hookers)

| Hooker              | Base Class        | Targets                                                    |
| ------------------- | ----------------- | ---------------------------------------------------------- |
| `AntiDetectHooker`  | `YukiBaseHooker`  | Stack traces, /proc/maps, PackageManager                   |
| `DeviceHooker`      | `BaseSpoofHooker` | TelephonyManager, Build, Settings.Secure, SubscriptionInfo |
| `NetworkHooker`     | `BaseSpoofHooker` | WifiInfo, NetworkInterface, BluetoothAdapter, carrier      |
| `AdvertisingHooker` | `BaseSpoofHooker` | AdvertisingIdClient, Gservices, MediaDrm                   |
| `SystemHooker`      | `BaseSpoofHooker` | Build.\* fields, SystemProperties                          |
| `LocationHooker`    | `BaseSpoofHooker` | Location, LocationManager, TimeZone, Locale                |
| `SensorHooker`      | `BaseSpoofHooker` | SensorManager (list filter), Sensor (metadata)             |
| `WebViewHooker`     | `BaseSpoofHooker` | WebSettings (User-Agent)                                   |

### SpoofType Categories (24 total)

| Category        | Types                                                                                                                |
| --------------- | -------------------------------------------------------------------------------------------------------------------- |
| **Device**      | IMEI, IMSI, SERIAL, ICCID, PHONE_NUMBER, SIM_COUNTRY_ISO, SIM_OPERATOR_NAME                                          |
| **Network**     | WIFI_MAC, BLUETOOTH_MAC, WIFI_SSID, WIFI_BSSID, CARRIER_NAME, CARRIER_MCC_MNC, NETWORK_COUNTRY_ISO, NETWORK_OPERATOR |
| **Advertising** | ANDROID_ID, GSF_ID, ADVERTISING_ID, MEDIA_DRM_ID                                                                     |
| **System**      | DEVICE_PROFILE                                                                                                       |
| **Location**    | LOCATION_LATITUDE, LOCATION_LONGITUDE, TIMEZONE, LOCALE                                                              |

---

## Pre-Commit Checklist

> **⛔ ZERO-TOLERANCE**: Every gate below MUST pass before considering any task complete.
> Running these checks is NOT optional. A single failure means the task is NOT done.
> Fix the root cause — never suppress or ignore warnings/errors.

---

### Quality Gates

> Run ALL gates **in the order listed** every time you touch any source file.
> A single FAILED gate means the task is **NOT done** — fix root cause, do not suppress.

#### 🟢 Gate 1 — Format (Always First)

```bash
# Auto-fix formatting — run before everything else
./gradlew spotlessApply

# Verify no formatting issues remain (used in CI)
./gradlew spotlessCheck
# Expected: BUILD SUCCESSFUL — zero formatting violations
```

#### 🟡 Gate 2 — Compile

```bash
# Fast Kotlin-only compile check (no APK, very fast)
./gradlew :app:compileDebugKotlin :common:compileDebugKotlin :xposed:compileDebugKotlin
# Expected: BUILD SUCCESSFUL — zero compilation errors or warnings
```

#### 🟠 Gate 3 — Lint

```bash
# Full cross-module lint (checkDependencies=true is set in app/build.gradle.kts)
./gradlew lint
# Expected: BUILD SUCCESSFUL, 0 errors (warnings are acceptable with justification)

# For focused changes: run only the affected module's lint
./gradlew :app:lint        # for UI changes
./gradlew :xposed:lint     # for hook changes
./gradlew :common:lint     # for model/generator changes
```

#### 🔴 Gate 4 — Unit Tests

```bash
# Run all tests (29 generator tests + any new tests)
./gradlew test
# Expected: All tests GREEN — 0 failures, 0 errors

# Run only common module tests (fastest, covers generators)
./gradlew :common:test
```

#### 🔵 Gate 5 — Debug Build

```bash
# Full debug APK must build without errors
./gradlew assembleDebug
# Expected: BUILD SUCCESSFUL — APK produced in app/build/outputs/apk/debug/
```

#### ⚫ Gate 6 — Release Build (R8 Validation) — Run Before Every Release

```bash
# Full R8 release build — validates ALL ProGuard rules are correct
./gradlew assembleRelease
# Expected: BUILD SUCCESSFUL — release APK in app/build/outputs/apk/release/
# Any R8 error = missing -keep rule → fix in proguard-rules.pro or consumer-rules.pro

# After release build, check the R8 mapping for suspicious stripping:
Select-String -Path 'app\build\outputs\mapping\release\mapping.txt' -Pattern 'DeviceMaskerService|XposedEntry|HookEntry|AntiDetect'
# Expected: Each critical class APPEARS in the mapping (means it was kept, not stripped)
```

#### 🔎 Gate 7 — Xposed Safety Checks (Grep)

```bash
# Check 1: No hook callbacks missing runCatching
# Expected: 0 results (all hooks must be wrapped)
grep -rn 'after {\|before {\|replaceAny {' xposed/src --include='*.kt' | grep -v 'runCatching'

# Check 2: No hardcoded pref keys (must use SharedPrefsKeys)
# Expected: 0 results
grep -rn '"module_enabled"\|"app_enabled_"\|"spoof_value_"\|"spoof_enabled_"' app/src xposed/src --include='*.kt'

# Check 3: No Random() usage in generators (must be SecureRandom)
# Expected: 0 results
grep -rn 'Random()' common/src --include='*.kt' | grep -v 'SecureRandom'

# Check 4: No Timber usage in :xposed module (must use DualLog)
# Expected: 0 results
grep -rn 'Timber\.' xposed/src --include='*.kt'

# Check 5: No Compose imports in :common or :xposed
# Expected: 0 results
grep -rn 'import androidx.compose' common/src xposed/src --include='*.kt'
```

---

### Manual Verification Checklist

Before closing any task, confirm ALL of the following:

- [ ] All pref keys delegate to `SharedPrefsKeys` — no hardcoded key strings
- [ ] New `SpoofType` entries added to all 3 layers: `:common` enum, hooker, and UI screen
- [ ] `runCatching { }` wraps all hook callbacks — zero crash risk in target apps
- [ ] system_server code wrapped in try-catch — zero bootloop risk
- [ ] Correlated values generated together via `CorrelationGroup` — no mismatched SIM/carrier/location
- [ ] `DeviceProfilePreset` applied as complete set — no mixed Build.\* fields
- [ ] `@Serializable` on all cross-process data models
- [ ] Immutable patterns used — `val` properties, `copy()` for updates
- [ ] `ClassCache` used for class lookups in hookers — no raw `loadClass()` calls
- [ ] Memory bank updated if the change affects architecture, patterns, or project state

---

### Hard Failure Rules

| Gate                    | Rule                                                                            |
| ----------------------- | ------------------------------------------------------------------------------- |
| **spotlessCheck fails** | Run `spotlessApply`, then verify `spotlessCheck` is clean.                      |
| **lint errors**         | Fix the code. Never suppress lint warnings without a written justification.     |
| **build fails**         | Fix the build. Do not hand off a broken build under any circumstances.          |
| **hook crash**          | Wrap in `runCatching { }`. Never let a hook crash the target app.               |
| **system_server crash** | Wrap in try-catch. A system_server crash = device bootloop.                     |
| **key mismatch**        | Both sides MUST use `SharedPrefsKeys`. Key mismatches = silent config failures. |
| **correlation broken**  | SIM values MUST share carrier. Location values MUST share country.              |

---

## MCP Tools

| Server                         | Purpose                                  |
| ------------------------------ | ---------------------------------------- |
| **context7**                   | Query library docs & code examples       |
| **google-developer-knowledge** | Official Google developer docs & samples |

---

## Skills

**⚠️ MANDATORY: Read relevant skills BEFORE generating any code.**

Skills are located in `.agents/skills/` — read the **SKILL.md** file inside each skill folder.

### Android & Kotlin

| Skill                     | When to Use                            | Path                                    |
| ------------------------- | -------------------------------------- | --------------------------------------- |
| **kotlin-coroutines**     | Coroutines, suspend, Flow, channels    | `.agents/skills/kotlin-coroutines/`     |
| **kotlin-specialist**     | Idiomatic Kotlin, DSLs, sealed classes | `.agents/skills/kotlin-specialist/`     |
| **material-3-expressive** | M3 Expressive UI design & review       | `.agents/skills/material-3-expressive/` |
| **mobile-design**         | Mobile-first UX, touch patterns        | `.agents/skills/mobile-design/`         |
| **android-agent-skills**  | Android platform patterns              | `.agents/skills/android-agent-skills/`  |

---

_Last Updated: 2026-03-12 (AIDL hybrid architecture, R8 full-mode release build, 7-gate quality pipeline, Xposed safety grep checks, 24 SpoofTypes, 8 active hookers, 10 device presets)_
