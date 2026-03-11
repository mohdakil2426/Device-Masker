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

| Layer          | Stack                                                                   |
| -------------- | ----------------------------------------------------------------------- |
| **Language**   | Kotlin 2.3.0, Java 25                                                   |
| **Platform**   | Android SDK 36 (Android 16 / Baklava), minSdk 26                        |
| **UI**         | Jetpack Compose (BOM 2025.12.00), Material 3 Expressive (1.5.0-alpha11) |
| **Hooking**    | YukiHookAPI 1.3.1, LSPosed (API 82), KavaRef                            |
| **IPC**        | AIDL Binder (system_server ↔ app processes), ContentProvider bridge     |
| **Arch**       | MVVM, Multi-Module Gradle (`:app`, `:xposed`, `:common`)                |
| **Data**       | kotlinx.serialization (JSON), SharedPreferences, AtomicFile             |
| **Build**      | Gradle (Kotlin DSL), KSP, Spotless (ktfmt 0.54)                         |
| **Logging**    | Timber (app), DualLog (xposed → YLog + buffer), HookMetrics             |
| **Navigation** | Jetpack Navigation Compose, spring-based animated transitions           |

---

## Commands

### Build Commands

```bash
./gradlew assembleDebug              # Build debug APK
./gradlew assembleRelease            # Build release APK (requires signing)
./gradlew installDebug               # Build and install to connected device
./gradlew :app:assembleDebug         # Build only :app module
./gradlew :common:assembleDebug      # Build only :common module
./gradlew :xposed:assembleDebug      # Build only :xposed module
```

### Lint & Format Commands

```bash
./gradlew spotlessApply              # Auto-format all Kotlin files (ktfmt)
./gradlew spotlessCheck              # Check formatting without fixing
./gradlew lint                       # Run Android lint on all modules
./gradlew :app:lint                  # Run lint on :app only
./gradlew :common:lint               # Run lint on :common only
./gradlew :xposed:lint               # Run lint on :xposed only
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

Run ALL commands, in order, every time you touch any source file:

```bash
# 1. Format — MUST complete with no changes needed
./gradlew spotlessApply

# 2. Format check — MUST show BUILD SUCCESSFUL
./gradlew spotlessCheck

# 3. Lint — MUST show BUILD SUCCESSFUL, zero errors
./gradlew lint

# 4. Build — MUST produce APK with zero compile errors
./gradlew assembleDebug
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

_Last Updated: 2026-03-12 (AIDL hybrid architecture complete, 24 SpoofTypes, 8 active hookers, 10 device presets)_
