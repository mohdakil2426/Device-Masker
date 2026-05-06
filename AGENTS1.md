# Device Masker — Agent Guide

Android LSPosed/libxposed module for per-app device identity spoofing. 3-module Gradle project: `:app` (Compose UI + config), `:common` (shared contracts + generators), `:xposed` (hooks running inside target app processes).

## Build Commands

```powershell
# Full gate (what CI runs, minus assembleRelease):
.\gradlew.bat spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug assembleRelease --no-daemon

# Fast targeted gates:
.\gradlew.bat :xposed:testDebugUnitTest --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :common:testDebugUnitTest --no-daemon
.\gradlew.bat assembleDebug --no-daemon

# CI release (minified, validates ProGuard rules):
.\gradlew.bat :app:assembleCiRelease --no-daemon

# Spotless auto-fix:
.\gradlew.bat spotlessApply --no-daemon

# Single ViewModel test:
.\gradlew.bat :app:testDebugUnitTest --tests com.astrixforge.devicemasker.ui.navigation.DeviceMaskerNavigatorTest --no-daemon
```

CI runs on ubuntu-latest with JDK 17. Jobs: `quality` (spotless, compile, lint, tests) → `assemble-debug` → `assemble-signed-release` (main branch only, requires signing secrets).

## Module Boundaries

| Module | What lives here | Key constraint |
|--------|----------------|----------------|
| `:app` | Compose UI, ViewModels, ConfigManager, ConfigSync, XposedPrefs, diagnostics, root capture | No hook logic. Timber OK here. |
| `:common` | JsonConfig, SpoofType, SharedPrefsKeys, generators, DevicePersona, AIDL | No Android UI deps. Generators run at config time only. |
| `:xposed` | XposedEntry, all hookers, PrefsReader, DualLog, DeviceMaskerService | **No Timber. No Compose. No random generation.** `libxposed:api` is `compileOnly`. |

## Project Structure

```
devicemasker/
├── app/                  :app module — Compose UI, config, diagnostics, root capture
│   └── src/main/kotlin/com/astrixforge/devicemasker/
│       ├── data/             RemotePreferences writer, config sync, repositories
│       ├── service/          ConfigManager, log store, AIDL client, diagnostics
│       ├── ui/               MainActivity, screens, navigation, theme, components
│       └── utils/            Image utilities
│
├── common/               :common module — shared contracts, models, generators
│   └── src/main/
│       ├── kotlin/.../common/
│       │   ├── models/       Carrier, SIMConfig, DeviceHardwareConfig, LocationConfig, Country
│       │   ├── generators/   IMEI, IMSI, ICCID, MAC, Serial, UUID, PhoneNumber, Fingerprint
│       │   ├── diagnostics/  DiagnosticEvent schema, redactor
│       │   └── (root)        JsonConfig, AppConfig, SpoofGroup, SpoofType, SharedPrefsKeys, PersonaGenerator
│       └── aidl/             IDeviceMaskerService (diagnostics-only)
│
├── xposed/               :xposed module — hooks in target app processes
│   └── src/main/
│       ├── kotlin/.../xposed/
│       │   ├── hooker/       11 hookers + BaseSpoofHooker base class
│       │   ├── service/      DeviceMaskerService (system_server AIDL), log buffer
│       │   ├── diagnostics/  Event sink, hook health registry
│       │   └── (root)        XposedEntry, PrefsReader, DualLog, DeoptimizeManager
│       └── resources/META-INF/xposed/   java_init.list, module.prop, scope.list
│
├── gradle/               Version catalog (libs.versions.toml)
├── docs/                 Reports, guides, plans
├── scripts/              Build/verification scripts
├── build.gradle.kts      Root build config + Spotless formatting
├── settings.gradle.kts   Module includes + repositories
├── lint.xml              Lint exclusions
└── gradle.properties     JVM args, R8, build features
```

### File & Boundaries

| Type | Correct Location | Wrong |
| --- | --- | --- |
| User-facing docs | `docs/public/` | `docs/internal/` |
| Agent investigation reports/audits | `docs/internal/reports/` | project root |
| Implementation plans | `docs/superpowers/plans/` | `docs/reports/` or project root |
| Build logs and command output | `logs/build/` | project root, docs, module dirs |
| Device testing logs, logcat, screenshots, captures, exported evidence | `logs/device/` | project root or docs |
| Agent/user temporary check artifacts | `logs/tmp/` | project root or source folders |

All agent-created and user-created build logs, device logs, temporary captures, smoke-test exports, and scratch evidence must stay under `logs/` using the closest matching subfolder. Do not scatter temporary evidence files in the project root.


## Architecture — Config Delivery

- Complete Architecture Detailes Read This First `docs\public\ARCHITECTURE.md`

```
UI → SpoofRepository → ConfigManager (config.json) → ConfigSync → XposedPrefs → RemotePreferences
                                                                                        ↓ (LSPosed bridge)
                                                                              Hookers read in target process
```

- **RemotePreferences is the sole config channel.** AIDL is diagnostics-only — never deliver spoof config through it.
- Config writes use `commit()` (sync), not `apply()` (async), so hooks see values immediately.
- `JsonConfig.appConfigs` is canonical for app assignment. `SpoofGroup.assignedApps` is legacy/display-only.
- `SharedPrefsKeys` in `:common` is the only source for preference key strings. Never hardcode keys.

## Architecture — Hook System

10 hookers registered from `XposedEntry.onPackageReady()` in order: AntiDetect → Device → Subscription → Network → System → Location → Sensor → Advertising → WebView → PackageManager. `SystemServiceHooker` registered separately in `onSystemServerStarting()`.

**Every hook must:**
- Return original value when config is disabled, missing, blank, malformed, or unsafe
- Register callbacks with `intercept(stableHooker { ... })` or explicit named `XposedInterface.Hooker` implementations
- Call `xi.deoptimize(m)` after hooking (prevents ART/JIT inlining from bypassing hook)
- Use `safeHook()` isolation — one failed method never blocks other hooks
- Skip abstract methods before calling `xi.hook(m)`

**Never do in `:xposed`:**
- Generate random/fallback identifiers
- Use Timber (use `DualLog` instead)
- Read app-private JSON files
- Hardcode RemotePreferences key strings
- Mutate `chain.args` in place — it's immutable. Copy and call `chain.proceed(Object[])`
- Use direct Kotlin SAM callbacks like `xi.hook(m).intercept { ... }`; release R8 caused `AbstractMethodError` from that callback shape
- Re-enable global `Class.forName` / `ClassLoader.loadClass` hooks without per-app kill switch

**libxposed API 101 specifics:**
- Lifecycle: `onModuleLoaded` → `onPackageReady` (app hooks) / `onSystemServerStarting` (system_server setup)
- `chain.thisObject` can be `null` for static methods
- `HookFailedError` extends `XposedFrameworkError` extends `Error` — rethrow before generic `Throwable` catches
- Legacy XposedBridge patterns compile but fail silently at runtime — always load the libxposed skill first (`.agents/skills/libxposed/SKILL.md`)

## Anti-Detection State

**Active:** Stack trace filtering, `/proc/self/maps` filtering, package visibility hiding.
**Disabled by default:** Global `Class.forName` and `ClassLoader.loadClass` hooks — destabilized target app startup. Reintroduce only behind per-app kill switch.
**Rule:** Intentional package/class hiding throws must use `ExceptionMode.PASSTHROUGH`.

## Runtime Validation

Before claiming hooks work:
1. Run unit tests
2. Build and install the APK variant being validated. For release/R8 validation, use the minified release APK and keep R8 enabled
3. LSPosed scope must include `android`, `system`, and target app
4. Force-stop and relaunch target app after any scope/module/config change
5. Check LSPosed logs for `XposedEntry loaded`, `All hooks registered`, and `Spoof event`
6. Verify actual spoofed values in target app, not just log events
7. After hook safety or R8 callback changes, run `R8HookerAbiTest`, retest `com.mantle.verify`, and validate at least one more target such as `flar2.devcheck`

Smoke targets: `com.mantle.verify`, `flar2.devcheck`.

```powershell
# Install and smoke-test:
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am force-stop com.mantle.verify
adb logcat -c
adb shell monkey -p com.mantle.verify -c android.intent.category.LAUNCHER 1
adb shell pidof com.mantle.verify
adb logcat -d -t 1200
```

## Crash Signatures To Watch

These must remain absent after hook changes:
- `androidx.work.WorkManagerInitializer`
- WebView regex `PatternSyntaxException`
- `Cannot hook abstract methods`
- `AbstractMethodError` from minified hooker lambdas
- `FATAL EXCEPTION` in target process


## Code Style

- Kotlin 2.3.0, JVM 17 toolchain
- Spotless with ktfmt `0.54` kotlinlangStyle — run `spotlessApply` before committing
- Compose opt-ins: `ExperimentalMaterial3Api`, `ExperimentalMaterial3ExpressiveApi`, `ExperimentalAnimationApi`, `ExperimentalFoundationApi`, `ExperimentalMaterial3AdaptiveApi`
- Lint is fail-fast (`abortOnError = true`)
- No hardcoded `Color(0x...)` outside `ui/theme/Color.kt`

## Key Gotchas

- Release R8 is enabled. Runtime hookers must use `StableHooker`/named `XposedInterface.Hooker` callbacks; direct Kotlin SAM `.intercept { ... }` callbacks caused `AbstractMethodError` in target processes.
- `:xposed` uses `resources.srcDirs("src/main/resources")` for `META-INF/xposed/` files — these are Java resources, not Android assets.
- `XposedProvider` in manifest is required for RemotePreferences bridge — do not remove.
- Global module toggle: `module_enabled` in RemotePreferences. Per-app: `{sanitized_pkg}_app_enabled`.
- Some hooks read config at registration time, not at callback time. Don't claim live update without verifying.
- `gradle.properties`: config-cache + parallel builds enabled, R8 strict mode on, 4GB heap, `useAndroidX=true`.

## Testing

- `MainDispatcherRule` swaps `Dispatchers.Main` for coroutine tests
- Hand-written fakes in `app/src/test/.../testing/` — 7 fakes: `FakeSpoofRepository`, `FakeConfigManager`, `FakeSettingsDataStore`, `FakeServiceClient`, `FakeLogManager`, `FakeAppScopeRepository`, `FakeSharedPreferences`
- Turbine for Flow emission testing
- MockK only for Navigation 3 framework types
- `advanceUntilIdle()` required after async ops in `runTest`
- `:xposed` tests can reference `libxposed:api` at test runtime only

## Module Guides

Detailed per-module guides with folder structures, APIs, and constraints:

- `app/AGENTS1.md` — Compose UI, ViewModels, navigation, diagnostics, build config
- `common/AGENTS1.md` — data models, generators, SharedPrefsKeys, AIDL contract
- `xposed/AGENTS1.md` — hookers, hook patterns, anti-detection, ProGuard, metadata

## Skills

Load the `libxposed` skill before any Xposed work for all the information about it its have: `.agents/skills/libxposed/SKILL.md`

Other available skills: `claude-android-ninja`, `edge-to-edge`, `material-3-expressive`, `navigation-3`, `r8-analyzer`.

## graphify

This project has a graphify knowledge graph at graphify-out/.

Rules:

- Before answering architecture or codebase questions, read graphify-out/GRAPH_REPORT.md for god nodes and community structure
- If graphify-out/wiki/index.md exists, navigate it instead of reading raw files
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` over grep — these traverse the graph's EXTRACTED + INFERRED edges instead of scanning files
- After modifying code files in this session, run `graphify update .` to keep the graph current (AST-only, no API cost
