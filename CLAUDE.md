# Device Masker — LSPosed Privacy Module

> **Advanced Android Xposed/LSPosed module** for spoofing device identifiers and hiding hook injection.
> Kotlin 2.3.0 | Android SDK 36 | Jetpack Compose | YukiHookAPI 1.3.1 | AIDL IPC

**Memory Bank**: The `memory-bank/` directory contains the source of truth for project context, patterns, and progress tracking. Read ALL files for deep project understanding. **NEVER SKIP THIS STEP.**

**RESPECT ALL RULES**: You MUST follow every rule, guideline, principle, coding standard and best practice documented below. No exceptions, no shortcuts, no lazy work, full efforts. Respect project patterns, shared contracts, and existing code style consistency.

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
| Pref key generation   | `SharedPrefsKeys` in `:common`           | Hardcoding keys in `:app` or `:xposed`    |
| Value generators      | `common/generators/`                     | Generating values in hookers directly     |
| Hook logic            | `:xposed/hooker/`                        | Putting hooks in `:app` or `:common`      |
| UI Compose screens    | `:app/ui/screens/`                       | Mixing UI into `:common` or `:xposed`     |
| system_server service | `:xposed/service/`                       | Running AIDL service from `:app`          |
| Config persistence    | `ConfigManager` (both app-side & xposed) | Direct file I/O from hookers              |

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

### 🔨 Build

```bash
# Quality gate — run in this order before every commit
./gradlew spotlessApply && ./gradlew spotlessCheck && ./gradlew lint && ./gradlew test && ./gradlew assembleDebug

./gradlew assembleDebug          # Full debug build (all 3 modules)
./gradlew assembleRelease        # Release build (R8 shrinking + obfuscation)
./gradlew installDebug           # Build + install to connected rooted device
./gradlew clean assembleDebug    # Clean rebuild (fixes stale caches)

# Module-specific (faster for targeted changes)
./gradlew :app:assembleDebug
./gradlew :xposed:assembleDebug
./gradlew :common:assembleDebug
```

### 🎨 Format & Lint

```bash
./gradlew spotlessApply          # Auto-fix formatting (run before committing)
./gradlew spotlessCheck          # Verify formatting (CI)

./gradlew lint                   # Full cross-module lint
./gradlew :app:lint              # App-only lint
./gradlew :app:lintRelease       # Stricter — catches R8-specific issues
./gradlew :app:updateLintBaseline  # Snapshot known issues
```

> Reports: `app/build/reports/lint-results-debug.xml`

### 🧪 Tests

```bash
./gradlew test                   # All unit tests
./gradlew :common:test           # Generator tests (fastest — 29 tests)
./gradlew :common:test --tests "com.astrixforge.devicemasker.common.generators.IMEIGeneratorTest"
./gradlew :app:connectedAndroidTest  # On-device tests
```

### 🛡️ Release Validation

```bash
./gradlew assembleRelease

# Verify critical classes were KEPT (not stripped) by R8
Select-String -Path 'app\build\outputs\mapping\release\mapping.txt' -Pattern 'DeviceMaskerService|XposedEntry|HookEntry|AntiDetect'

# APK size
Get-Item app\build\outputs\apk\release\*.apk | Select-Object Name, @{N='MB';E={[math]::Round($_.Length/1MB,2)}}

# Verify xposed_init exists (CRITICAL — LSPosed won't load without it)
type xposed\src\main\assets\xposed_init
```

### 🔎 Xposed Safety Checks (All must return 0 results)

```bash
# 1. Unprotected hook callbacks
grep -rn 'after {\|before {\|replaceAny {' xposed/src --include='*.kt' | grep -v 'runCatching'

# 2. Hardcoded pref keys (must use SharedPrefsKeys)
grep -rn '"module_enabled"\|"app_enabled_"\|"spoof_value_"\|"spoof_enabled_"' app/src xposed/src --include='*.kt'

# 3. Non-secure random in generators
grep -rn 'Random()\b' common/src --include='*.kt' | grep -v 'SecureRandom'

# 4. Timber in :xposed (must use DualLog)
grep -rn 'Timber\.' xposed/src --include='*.kt'

# 5. Compose imports in :common or :xposed
grep -rn 'import androidx.compose' common/src xposed/src --include='*.kt'
```

### 🗂️ PowerShell One-Liners

```powershell
# Files modified in last 24h
Get-ChildItem -Recurse -Filter '*.kt' | Where-Object { $_.LastWriteTime -gt (Get-Date).AddHours(-24) }

# Search across all modules
Select-String -Path 'app/src','xposed/src','common/src' -Recurse -Pattern 'BaseSpoofHooker' -Include '*.kt'

# Kotlin line count per module
Get-ChildItem -Recurse -Filter '*.kt' app/src,xposed/src,common/src | Measure-Object -Sum -Property Length
```

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
./gradlew :app:compileDebugKotlin :common:compileDebugKotlin :xposed:compileDebugKotlin
# Expected: BUILD SUCCESSFUL — zero compilation errors or warnings
```

#### 🟠 Gate 3 — Lint

```bash
./gradlew lint
# Expected: BUILD SUCCESSFUL, 0 errors
```

#### 🔴 Gate 4 — Unit Tests

```bash
./gradlew test
# Expected: All tests GREEN — 0 failures, 0 errors
```

#### 🔵 Gate 5 — Debug Build

```bash
./gradlew assembleDebug
# Expected: BUILD SUCCESSFUL — APK in app/build/outputs/apk/debug/
```

#### ⚫ Gate 6 — Release Build (R8 Validation)

```bash
./gradlew assembleRelease
Select-String -Path 'app\build\outputs\mapping\release\mapping.txt' -Pattern 'DeviceMaskerService|XposedEntry|HookEntry|AntiDetect'
# Expected: each critical class appears in mapping (kept, not stripped)
```

#### 🔎 Gate 7 — Xposed Safety Checks (Grep)

```bash
grep -rn 'after {\|before {\|replaceAny {' xposed/src --include='*.kt' | grep -v 'runCatching'
grep -rn '"module_enabled"\|"app_enabled_"\|"spoof_value_"\|"spoof_enabled_"' app/src xposed/src --include='*.kt'
grep -rn 'Random()' common/src --include='*.kt' | grep -v 'SecureRandom'
grep -rn 'Timber\.' xposed/src --include='*.kt'
grep -rn 'import androidx.compose' common/src xposed/src --include='*.kt'
# All 5 checks must return 0 results
```

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

## More Commands

`docs/guides/RUNBOOK.md`

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

> **⛔ MANDATORY — Query BEFORE writing any code, every time, no exceptions.**
> **Priority 1: `google-developer-knowledge`** — Android, Compose, Jetpack, Google APIs (official Google sources).
> **Priority 2: `context7`** — All other libraries (YukiHookAPI, kotlinx, third-party). Use only if google-developer-knowledge has no result.

| Server                         | Priority | Covers                                                                              |
| ------------------------------ | -------- | ----------------------------------------------------------------------------------- |
| **google-developer-knowledge** | **1st**  | Android SDK, Jetpack Compose, Material 3, Firebase, Google APIs, Chrome, TensorFlow |
| **context7**                   | 2nd      | YukiHookAPI, kotlinx, all non-Google libraries                                      |

### Official Documentation & Repositories (`context7`)

- **LibXposed**:
  - [API Reference](https://libxposed.github.io/api/)
  - [API](https://github.com/libxposed/api)
  - [Helper](https://github.com/libxposed/helper)
  - [Service](https://github.com/libxposed/service)
  - [Example](https://github.com/libxposed/example)

---

## Skills

**⚠️ MANDATORY: Read relevant skills BEFORE generating any code.**

Skills are located in `.claude/skills/` — read the **SKILL.md** file inside each skill folder.

### Design & Planning

| Skill                | When to Use                                                       | Path                               |
| :------------------- | :---------------------------------------------------------------- | :--------------------------------- |
| **mermaid-diagrams** | Creating software diagrams (class, sequence, flow, C4, ERD, Git). | `.claude/skills/mermaid-diagrams/` |

### Android & Kotlin

| Skill                     | When to Use                                                                      | Path                                    |
| :------------------------ | :------------------------------------------------------------------------------- | :-------------------------------------- |
| **android-ninja**         | Core Android architectural guidance (Kotlin, Compose, MVVM, Hilt, Multi-module). | `.claude/skills/claude-android-ninja/`  |
| **material-3-expressive** | Material 3 Expressive UI design, review, token specification, and motion.        | `.claude/skills/material-3-expressive/` |

### Tooling & Maintenance

| Skill                | When to Use                                                               | Path                                     |
| :------------------- | :------------------------------------------------------------------------ | :--------------------------------------- |
| **skill-creator**    | Creating, modifying, and optimizing agentic skills via data-driven evals. | `.claude/skills/skill-creator/`          |
| **coding-standards** | Generating project-specific style guides from existing source code.       | `.claude/skills/write-coding-standards/` |

---

_Last Updated: 2026-03-13 (Skill reference update, AIDL hybrid architecture, R8 full-mode release build, 7-gate quality pipeline, Xposed safety grep checks, 24 SpoofTypes)_
