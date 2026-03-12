## Why

Device Masker currently uses YukiHookAPI 1.3.1 (wrapping legacy Xposed API 82) for all hook operations. This architecture has three critical, unfixable limitations:

1. **ART Inlining Bypass Impossible**: Short methods like `getImei()`, `Build.MODEL` getters are inlined by ART's JIT/AOT compiler, causing hooks to silently never fire. The only fix — `deoptimize(method)` — is exclusive to libxposed API 100.
2. **Stale Configuration**: `XSharedPreferences` caches values at process load. Config changes require target app restart. The current AIDL service mitigates this but introduces bootloop risk and 50-200μs binder overhead per hook call. libxposed's `RemotePreferences` provides live config with ~5μs latency and zero bootloop risk.
3. **10 Spoofing Gaps Detected**: `SubscriptionManager` is completely unhooked (banking apps bypass `TelephonyManager`), IMEI TAC doesn't match device profiles (detectable by fraud SDKs), `Build.*` fields are incompletely covered, `WebView.getDefaultUserAgent()` is missed, and `PackageManager.hasSystemFeature()` leaks real hardware capabilities.

**Why now?** libxposed API 100 artifacts (`io.github.libxposed:api:100`, `io.github.libxposed:service:100-1.0.0`) are confirmed published on Maven Central. LSPosed 1.10.2+ (already required by this project) fully supports the modern API. The migration unblocks all three issues simultaneously.

**Reference Documents:**

- `docs/planning/DEVICE_MASKER_LIBXPOSED_API100_PLAN.md` — 16-phase implementation plan (2157 lines)
- `docs/planning/DEVICE_MASKER_STORAGE_ARCHITECTURE_OPTIONS.md` — Option A vs B storage/IPC analysis (1495 lines)

## What Changes

### Dependency & Build System

- **BREAKING**: Remove YukiHookAPI 1.3.1, KavaRef 1.0.2, KSP plugin, legacy `de.robv.android.xposed:api:82`
- Add `io.github.libxposed:api:100` (compileOnly) to `:xposed`
- Add `io.github.libxposed:service:100-1.0.0` (implementation) to `:app`
- Update `libs.versions.toml` — remove 5 old entries, add 2 new entries

### Entry Point Migration

- **BREAKING**: Delete `assets/xposed_init` (legacy entry)
- Create `META-INF/xposed/java_init.list`, `module.prop`, `scope.list` (modern entry)
- **BREAKING**: Rewrite `XposedEntry.kt` — from `IXposedHookLoadPackage` to `XposedModule` subclass with `onPackageLoaded()` + `onSystemServerLoaded()`

### Hook API Migration (All 8+ Hookers)

- **BREAKING**: Rewrite all hookers from YukiHookAPI DSL (`method { name = "..." }.hook { after { } }`) to libxposed static hooker pattern (`@XposedHooker` classes with `@BeforeInvocation`/`@AfterInvocation`)
- **BREAKING**: Rewrite `BaseSpoofHooker` — remove `YukiBaseHooker` inheritance, plain Kotlin object with `safeHook()` pattern
- Add `DeoptimizeManager` — call `deoptimize()` on every hooked method to bypass ART inlining

### Storage/IPC Architecture (Option B — AIDL Demoted to Diagnostics)

- Config delivery moves from hybrid AIDL+XSharedPreferences to `RemotePreferences` (live, LSPosed-managed)
- AIDL service stripped to diagnostics-only (8 methods, down from 15) — keeps hook event counting, log aggregation, health checks
- All hook→service calls become `oneway` (fire-and-forget, non-blocking)
- Remove `ConfigManager.kt` from `:xposed` (no more `/data/misc/` file config)
- `XposedPrefs.kt` in `:app` changes from `MODE_WORLD_READABLE` to `ModulePreferences.from()`

### New Hookers (Closing Spoofing Gaps)

- **NEW**: `SubscriptionHooker` — hooks `SubscriptionManager` + `SubscriptionInfo` getters (ICCID, number, carrier, country)
- **NEW**: `PackageManagerHooker` — hooks `hasSystemFeature()` to match device profile capabilities
- Expand `DeviceHooker` — add `getLine1Number()`, `getPhoneCount()`, `getNetworkType()`, `getDataNetworkType()`
- Expand `NetworkHooker` — add BSSID, `NetworkInterface.getHardwareAddress()`, `ConnectivityManager`
- Expand `WebViewHooker` — add `WebView.getDefaultUserAgent()`, `System.getProperty("http.agent")`
- Expand `AntiDetectHooker` — add `ClassLoader.loadClass()`, `Runtime.exec()`, `ActivityManager.getRunningServices()`
- Expand `SystemHooker` — hook `Build` class initializer, add `Build.TIME`, `SECURITY_PATCH`, `SUPPORTED_ABIS`, `ID`, `TAGS`, `TYPE`

### Value Generation & Correlation

- TAC-aware IMEI generation — each `DeviceProfilePreset` carries valid TAC prefixes
- Enrich `DeviceProfilePreset` with `buildTime`, `securityPatch`, `buildId`, `incremental`, `supportedAbis`, `tacPrefixes`, `simCount`, `hasNfc`, `has5G`
- `NetworkTypeMapper` utility — maps carrier MCC/MNC to expected network type (LTE/NR)
- Full correlation matrix enforcement across all 24 spoof types

### ProGuard & Release Build

- **BREAKING**: Rewrite `xposed/consumer-rules.pro` for API 100 patterns (`@XposedHooker`, `XposedInterface.Hooker`)
- Remove all YukiHookAPI keep rules
- Add libxposed API/Module keep rules

## Capabilities

### New Capabilities

- `libxposed-entry-point`: Modern module entry via `META-INF/xposed/java_init.list` + `XposedModule` subclass replacing legacy `xposed_init` + `IXposedHookLoadPackage`
- `remote-preferences`: Live config delivery via libxposed `RemotePreferences` replacing XSharedPreferences + AIDL config methods
- `art-deoptimization`: ART inline bypass via `deoptimize()` ensuring hooks fire on JIT/AOT-compiled methods
- `subscription-hooker`: SubscriptionManager/SubscriptionInfo hooking to close the biggest spoofing gap
- `package-manager-hooker`: `hasSystemFeature()` hooking to match device profile hardware capabilities
- `tac-aware-imei`: TAC-prefix-correlated IMEI generation matching device profile model
- `enriched-device-profiles`: Extended DeviceProfilePreset with build metadata, ABIs, TAC prefixes, hardware flags
- `network-type-mapping`: Carrier MCC/MNC to network type correlation for consistent spoofing
- `diagnostics-only-aidl`: Stripped AIDL service (8 methods) for hook event reporting and log aggregation only
- `anti-detect-hardening`: Additional detection vector coverage (ClassLoader.loadClass, Runtime.exec, RunningServices)
- `webview-completion`: Full WebView UA spoofing including static getDefaultUserAgent() and http.agent system property
- `system-hooker-hardening`: Complete Build.\* field coverage via class initializer hooking

### Modified Capabilities

_(No existing openspec specs to modify — this is the first openspec change for this project)_

## Impact

### Code Impact (~30 files across 3 modules)

- **`:xposed` module**: 15+ files rewritten/created (all hookers, entry point, base class, utils, service)
- **`:app` module**: 5 files modified (XposedPrefs, ConfigManager, ServiceClient, DiagnosticsViewModel, AndroidManifest)
- **`:common` module**: 3 files modified (DeviceProfilePreset enrichment, ValueGenerators TAC, AIDL interface rewrite)
- **Build config**: 4 files modified (libs.versions.toml, 2× build.gradle.kts, consumer-rules.pro)

### Dependency Changes

- **Removed**: yukihookapi-api, yukihookapi-ksp, kavaref-core, kavaref-extension, xposed-api:82 (5 deps + 1 plugin)
- **Added**: libxposed-api:100, libxposed-service:100-1.0.0 (2 deps, 0 plugins)
- **Net reduction**: 3 fewer dependencies, 1 fewer Gradle plugin (KSP removed if no other users)

### API Surface

- Module metadata format changes (META-INF instead of assets)
- LSPosed scope requirement unchanged (System Framework "android")
- SharedPrefsKeys contract unchanged — all pref key formats preserved
- AIDL interface reduced from 15 methods to 8 (diagnostics-only)

### Risk Assessment

- **High**: Entry point migration — if `java_init.list` is wrong, module won't load at all
- **High**: Hook API pattern change — every hooker must be rewritten correctly or hooks silently fail
- **Medium**: RemotePreferences — if libxposed-service integration is wrong, config delivery breaks
- **Low**: AIDL diagnostics — if service fails, hooks still work via RemotePreferences (non-fatal)
- **Low**: ProGuard rules — if wrong, release build crashes but debug build still works for testing
