# Active Context: Device Masker

## Current Work Focus

### ✅ COMPLETED: Latest libxposed API 101 Refresh (May 2, 2026)

**Status**: API dependency ✅ | metadata ✅ | hook entry ✅ | lint/test/debug/release ✅
**Branch**: `main`

Updated the live project from the previous API 101.0.0 baseline to the latest currently published
API 101 artifact set:
- `io.github.libxposed:api` → `101.0.1`
- `io.github.libxposed:service` remains `101.0.0` (latest published service artifact)
- `io.github.libxposed:interface` remains `101.0.0` (latest published interface artifact)

Context7 was requested but unavailable in this session because its OAuth token was expired. Official
libxposed API docs and Maven Central were used as fallback sources. Key docs confirmed:
`XposedModule`, `getRemotePreferences()`, `hook()`, `deoptimize()`, and API 101 runtime constants.

Key implementation update:
- `XposedEntry` now registers app hooks in `onPackageReady()` and uses `PackageReadyParam.classLoader`
  instead of `PackageLoadedParam.defaultClassLoader`, because the latter is annotated `@RequiresApi(29)`
  while this project supports minSdk 26.
- Legacy API 100 wording was updated across live code/resources.
- `xposedminversion` is now `101`; `module.prop` already had `minApiVersion=101` and
  `targetApiVersion=101`.
- Current persona migration compile gaps were closed by adding persona preference keys and
  ConfigManager persona lifecycle helpers.

Verification passed:
- `./gradlew.bat spotlessApply spotlessCheck lint test assembleDebug assembleRelease`
- Xposed safety greps: legacy callback annotations, hardcoded pref keys, non-secure random,
  Timber in `:xposed`, and Compose imports in `:common`/`:xposed` all returned 0 results.
- Release mapping contains critical xposed classes including `XposedEntry`, `AntiDetectHooker`, and
  `DeviceMaskerService`.

Remaining notes:
- Gradle/AGP deprecation warnings remain in `gradle.properties` and Spotless config; they do not
  fail the current gates but should be cleaned before AGP 10.
- Manual device testing with an LSPosed API 101 runtime is still required.

### ✅ COMPLETED: libxposed API 101 Migration — Full Finalization (Mar 19, 2026)

**Status**: API 101 ✅ | module.prop ✅ | ProGuard ✅ | All hooker docs ✅ | assembleDebug ✅
**Branch**: `main`

---

### ✅ RESOLVED BLOCKER: API Mismatch / local publishing

### Resolution
Migrated to the official `101.0.0` releases of `io.github.libxposed:api` and `io.github.libxposed:service` on Maven Central. This completely bypassed the local build compilation failures associated with missing Android SDK configurations inside the `docs/libxposed` sub-project and mismatching JDK versions. 
- Updated `libs.versions.toml` to point to `101.0.0`
- Removed `mavenLocal()` from `settings.gradle.kts`.
- Fixed deprecation warnings in `gradle.properties`.
- Addressed Spotless formatting violations across all files.

---

## ✅ COMPLETED THIS SESSION (Mar 19, 2026) — API 101 Full Finalization

- **module.prop**: Bumped `minApiVersion=101` + `targetApiVersion=101` (was 100).
- **consumer-rules.pro**: Full rewrite — removed dead API 100 `@XposedHooker` keeps + `XposedInterface$Hooker` keeps + deleted `ServiceBridge`/`DeoptimizeManager` keeps. Fixed `XposedEntry` constructor keep rule to no-arg (API 101 pattern).
- **KDoc updates**: All 10 hookers + `BaseSpoofHooker` + `DualLog` + `libs.versions.toml` comment updated from API 100 → 101.
- **SpoofValueCard.kt bug fix**: Added missing `stringResource` + `R` imports (pre-existing `:app` compile error, unrelated to migration).
- **assembleDebug PASSED**: 24.44 MB APK built successfully.
- **Xposed safety checks PASSED**: 0 @XposedHooker, 0 Timber, 0 hardcoded pref keys, 0 Compose in xposed.

---

## 🚀 PENDING TASKS

1. **Fix pre-existing lint warnings**: Package directive mismatch in all hooker files, string interpolation simplifications, `SERIAL_KEYS` local variable naming.
2. **Fix :common test compile**: `kotlin.test.Test` unresolved in generator tests — likely a Kotlin test JVM target or dependency variant issue in `common/build.gradle.kts`.
3. **Manual Device Testing**: Verify all hookers behave correctly on a real device with LSPosed installed.

---

## ✅ COMPLETED (Previous Sessions) — App-Side Config + Service

### Priority 1 — App-Side Config Migration ✅

| File                           | Change                                                                                                                                                                                       |
| ------------------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `app/data/XposedPrefs.kt`      | **Full rewrite** — `XposedServiceHelper.registerListener()` API (Context7-verified). Removed `ModulePreferences.from()` (doesn't exist). `getPrefs()` returns nullable `SharedPreferences?`. |
| `app/DeviceMaskerApp.kt`       | `XposedPrefs.init()` (no context arg). `isXposedModuleActive` sentinel retained.                                                                                                             |
| `app/service/ConfigManager.kt` | Removed `syncToAidlService()`. Write path: JSON file + `ConfigSync` (ModulePreferences) only.                                                                                                |
| `app/data/ConfigSync.kt`       | **Full rewrite** — context param retained for API compat, but prefs now via `XposedPrefs.getPrefs()`. Null-safe: silently no-ops when module not active.                                     |

### Priority 2 — AIDL Demoted to Diagnostics-Only (Option B) ✅

| File                                                 | Change                                                                                                                         |
| ---------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `common/aidl/IDeviceMaskerService.aidl`              | Reduced from 15 → 8 methods. Config methods removed. `reportSpoofEvent`, `reportLog`, `reportPackageHooked` are `oneway`.      |
| `xposed/service/DeviceMaskerService.kt`              | Config state removed. 8-method diagnostic-only impl (logs, spoofCounts, hookedPackages).                                       |
| `app/service/ServiceClient.kt`                       | Config methods removed. Diagnostics-only: `getHookedPackages`, `getLogs`, `getSpoofEventCount`, `clearDiagnostics`, `isAlive`. |
| `app/ui/screens/diagnostics/DiagnosticsViewModel.kt` | Uses new `ServiceClient` diagnostic methods only. Graceful null handling when service unavailable.                             |

### Priority 3 — ProGuard Rules ✅

| File                        | Change                                                                                                                                         |
| --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- |
| `xposed/consumer-rules.pro` | Full rewrite for API 100: `@XposedHooker`, `XposedInterface.Hooker`, `XposedModule`. YukiHookAPI rules removed.                                |
| `app/proguard-rules.pro`    | Full rewrite: `XposedServiceHelper`, `libxposed-service`, `XposedModuleActive`. YukiHookAPI/KavaRef/KSP rules removed.                         |
| `common/consumer-rules.pro` | Fixed AIDL package path (`com.astrixforge.devicemasker`, not `.common.aidl`). Added `NetworkTypeMapper`, enriched `DeviceProfilePreset` rules. |

### Priority 4 — Cleanup (Legacy File Deletion) ✅

| File                         | Action                                                                                 |
| ---------------------------- | -------------------------------------------------------------------------------------- |
| `app/hook/HookEntry.kt`      | **Deleted** — YukiHookAPI entry, replaced by `XposedEntry.kt`                          |
| `xposed/utils/ClassCache.kt` | **Deleted** — LRU cache no longer needed (libxposed API 100 uses ClassLoader directly) |
| `xposed/HookHelper.kt`       | **Deleted** — YukiHookAPI DSL helper, replaced by `BaseSpoofHooker.safeHook()`         |

### Priority 5 — Common Module Enrichment ✅

| File                                 | Change                                                                                                                                                                                    |
| ------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `common/DeviceProfilePreset.kt`      | **8 new fields**: `buildTime`, `buildId`, `incremental`, `supportedAbis`, `tacPrefixes`, `simCount`, `hasNfc`, `has5G`. All 10 presets updated with real-device data + GSMA TAC prefixes. |
| `common/generators/IMEIGenerator.kt` | Added `generateForPreset(preset)` (TAC-correlated) and `generateWithTac(tac)`. Closes fraud detection TAC-mismatch gap.                                                                   |
| `common/NetworkTypeMapper.kt`        | **New file** — maps carrier MCC/MNC to `NETWORK_TYPE_NR` (5G) or `NETWORK_TYPE_LTE` (4G). Covers US/UK/IN/CN/EU/KR/JP/UAE/SA/SG/AU.                                                       |

### Dependency Fix Attempt ✅ (partial)

| Change                                                                 | Status                                                |
| ---------------------------------------------------------------------- | ----------------------------------------------------- |
| Added `libxposed-iface` to `libs.versions.toml`                        | ✅ Done                                               |
| Fixed `interface` Kotlin keyword collision in accessor                 | ✅ Done (`libxposed.iface` not `libxposed.interface`) |
| Added `implementation(libs.libxposed.iface)` to `app/build.gradle.kts` | ✅ Done                                               |
| Actual artifact resolution from `repo.lsposed.foundation`              | 🔴 **Blocked** (DNS failure)                          |

---

## 🚀 NEXT STEPS: Pre-Release Validation

### Step 1 — Verify Local Artifacts (DONE) ✅

`libxposed-api:100` and `libxposed-service` are correctly resolved from `mavenLocal()`.

### Step 2 — Audit & Quality Gates (DONE) ✅

All 15 quality gates (Safety + Build) are passing. Section A (Xposed Safety) and Section B (Gradle Gates) are all [PASS].

### Step 3 — Manual Device Verification ⏳

1. Deploy debug APK to rooted device with LSPosed.
2. Enable module, set scope to target apps.
3. Verify **RemotePreferences** live config delivery (no app restart needed).
4. Check **Diagnostics** screen for hook counts and logs.
5. Test anti-detection layers against RootBeer/Play Integrity.

---

## Recent Architecture Decisions

### libxposed Service — Correct API (Context7-Verified)

**App side (write):**

```kotlin
// In Application.onCreate():
XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
    override fun onServiceBind(service: XposedService) {
        // service.getRemotePreferences("device_masker_config") → SharedPreferences
        // Write via .edit().put*().apply()
    }
    override fun onServiceDied(service: XposedService) { /* clear ref */ }
})
```

**Hook side (read):**

```kotlin
// In XposedModule.onPackageLoaded():
val prefs = getRemotePreferences("device_masker_config")  // read-only in hooked process
val enabled = prefs.getBoolean("app_enabled_com.example", false)
```

**Key insight**: `ModulePreferences.from()` does NOT exist in the libxposed-service library.
The correct API is `XposedService.getRemotePreferences(group)` obtained via `XposedServiceHelper`.
The `ModulePreferencesProvider` in `AndroidManifest.xml` IS correct — it ships in the service jar
and acts as the ContentProvider bridge for this mechanism.

### XposedPrefs Null-Safety Contract

`XposedPrefs.getPrefs()` returns `null` when: module not active, LSPosed not running, or
XposedService not yet bound. All callers must handle null gracefully — `ConfigSync` does this
with early-return. No crash on non-rooted devices.

### TAC-Aware IMEI Generation

```kotlin
// When DEVICE_PROFILE is set, generate IMEI with matching TAC:
val preset = DeviceProfilePreset.findById(deviceProfileId)
val imei = if (preset != null) {
    IMEIGenerator.generateForPreset(preset)   // TAC from preset.tacPrefixes
} else {
    IMEIGenerator.generate()  // random from global list
}
```

### Common + Xposed Audit Remediation (Mar 16, 2026)

- `XposedEntry.onPackageLoaded()` now guards on `PackageLoadedParam.isFirstPackage` and skips
  duplicate classloader registration so later package loads in the same process do not overwrite
  process-global hook state.
- Diagnostics logging is now consolidated through `IDeviceMaskerService.reportLog(...)`:
  `DualLog` forwards structured failures into the service, and audited hooker callback failures
  were migrated off plain `Log.w(...)`.
- `DeviceMaskerService` timestamps now use `DateTimeFormatter` instead of shared
  `SimpleDateFormat`, removing binder-thread formatting races.
- Stale bootstrap/migration residue removed:
  - `xposed/src/main/assets/xposed_init` deleted
  - `xposed/service/ServiceBridge.kt` deleted
  - app-side diagnostics binder discovery now goes straight through `ServiceManager`
- Concrete audit gaps closed in code:
  - `NetworkInterface.getHardwareAddress()` only spoofs Wi-Fi-like interfaces
  - `AntiDetectHooker` covers `ClassLoader.loadClass(String, boolean)` and common
    `Class.forName(...)` paths
  - `PackageManagerHooker` now implements `queryIntentActivities(Intent, int)`
  - `SystemServiceHooker` scans all `systemReady()` overloads with parameter counts 0..5
  - `DeviceHardwareConfig.isDualSIM` now derives from preset `simCount`
  - `IMEIGenerator.generateForPreset()` fallback path simplified

---

## ✅ COMPLETE (Previous Sessions)

### Phase 0–5: libxposed API 100 Hooker Rewrites (Mar 13, 2026) ✅

- All 10 hookers rewritten with `@XposedHooker` + `try-catch` pattern
- Zero YukiHookAPI imports in `:xposed/src`
- `DeoptimizeManager.kt` added for ART inlining bypass
- `SubscriptionHooker.kt` and `PackageManagerHooker.kt` added (new gaps closed)

### Migration Research (Mar 13, 2026) ✅

- `docs/reports/MIGRATION_RESEARCH_REPORT.md`
- `docs/planning/DEVICE_MASKER_LIBXPOSED_API100_PLAN.md`
- OpenSpec change: `openspec/changes/libxposed-api100-migration/`
