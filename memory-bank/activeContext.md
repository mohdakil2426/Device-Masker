# Active Context: Device Masker

## Current Work Focus

### ✅ COMPLETE: Migration Research Report (Mar 13, 2026)

**Status**: Complete ✅  
**Deliverable**: `docs/reports/MIGRATION_RESEARCH_REPORT.md` (782 lines, 7 sections)  
**Scope**: Comprehensive research on libxposed API 100 migration + Storage/IPC architecture decision

**Key Decisions:**

- **Architecture: Option B** — AIDL demoted to diagnostics-only, config via RemotePreferences
- **Hook API: libxposed API 100** — migration from YukiHookAPI 1.3.1 justified by `deoptimize()`
- **16-phase migration plan** — ~34 hours estimated, 30 files affected
- **10 spoofing gaps identified** — SubscriptionManager, TAC correlation, ART inlining are critical

**Research Documents Created:**

- `docs/planning/DEVICE_MASKER_STORAGE_ARCHITECTURE_OPTIONS.md` — Options A vs B detailed analysis
- `docs/planning/DEVICE_MASKER_LIBXPOSED_API100_PLAN.md` — 16-phase implementation plan (2090 lines)
- `docs/reports/MIGRATION_RESEARCH_REPORT.md` — Final comprehensive report (782 lines)

---

### ⏳ IN PROGRESS: Hook Safety Audit & Code Quality Hardening (Mar 12, 2026)

**Status**: Partially Complete — critical fixes applied, final fixes pending  
**Branch**: `main`  
**Scope**: Grep-driven safety audit of all Xposed hookers + R8 release build optimization

#### What Was Completed This Session

| Task                                 | Status      | Notes                                                                                         |
| ------------------------------------ | ----------- | --------------------------------------------------------------------------------------------- |
| R8 Full Mode enabled                 | ✅ Complete | Removed `android.r8.strictFullModeForKeepRules=false`                                         |
| `gradle.properties` tuned            | ✅ Complete | 4GB heap, ParallelGC, build cache, parallel builds, VFS watch                                 |
| `app/proguard-rules.pro` rewritten   | ✅ Complete | Full coverage: AIDL, Binder, Timber strip, serialization, singletons                          |
| `xposed/consumer-rules.pro` expanded | ✅ Complete | Added full service layer + utils                                                              |
| `common/consumer-rules.pro` expanded | ✅ Complete | Added AIDL Stub/Proxy, generators, SharedPrefsKeys, INSTANCE                                  |
| `app/build.gradle.kts` improved      | ✅ Complete | Signing config, packaging exclusions, isDebuggable=false                                      |
| GEMINI.md Commands section           | ✅ Complete | 7 command categories, 7-gate quality pipeline, Xposed grep checks                             |
| `ValueGenerators.kt` security fix    | ✅ Complete | `java.util.Random` → `java.security.SecureRandom`                                             |
| `AdvertisingHooker.kt` safety fix    | ✅ Complete | All `after{}` wrapped in `runCatching`, `generateHexId` uses SecureRandom                     |
| `AntiDetectHooker.kt` partial fix    | ✅ Complete | `hookProcMaps()`, `hookPackageManager()` fixed; `getInstalledApplications` after{} still bare |
| `NetworkHooker.kt` safety fix        | ✅ Complete | All `after{}` in hookNetworkInterface, hookBluetoothAdapter, hookTelephonyCarrier wrapped     |

---

## 🚨 PENDING TASKS — Must Complete Next Session

### Priority 1 — Critical Safety Fix (Hooker `runCatching` gaps)

These bare `after {}` / `before {}` hook callbacks were found by automated grep scan and **NOT yet fixed**:

#### `DeviceHooker.kt` — Multiple bare `after {}` (HIGH RISK — crashes target apps)

| Line    | Method                                               | Issue                                                              |
| ------- | ---------------------------------------------------- | ------------------------------------------------------------------ |
| ~95     | `hookTelephonyManager()` → `getSimSerialNumber`      | Bare `after { result = cachedIccid }`                              |
| ~101    | `hookTelephonyManager()` → `getSimSerialNumber(Int)` | Bare `after { result = cachedIccid }`                              |
| ~109    | `hookTelephonyManager()` → `getSimCountryIso`        | Bare `after { result = ... }`                                      |
| ~115    | `hookTelephonyManager()` → `getSimCountryIso(Int)`   | Bare `after { result = ... }`                                      |
| ~122    | `hookTelephonyManager()` → `getNetworkCountryIso`    | Bare `after { result = ... }`                                      |
| ~129    | `hookTelephonyManager()` → `getNetworkOperator(Int)` | Bare `after { result = ... }`                                      |
| ~138    | `hookTelephonyManager()` → `getSimOperatorName`      | Bare `after { result = ... }`                                      |
| ~146    | `hookTelephonyManager()` → `getSimOperatorName(Int)` | Bare `after { result = ... }`                                      |
| ~154    | `hookTelephonyManager()` → `getNetworkOperator`      | Bare `after { result = ... }`                                      |
| ~161    | `hookTelephonyManager()` → `getNetworkOperator(Int)` | Bare `after { result = ... }`                                      |
| ~169    | `hookTelephonyManager()` → `getSimOperator`          | Bare `after { result = ... }`                                      |
| ~176    | `hookTelephonyManager()` → `getSimOperator(Int)`     | Bare `after { result = ... }`                                      |
| ~213    | `hookSubscriptionInfo()` → `getCarrierName`          | Bare `after { result = ... }`                                      |
| ~249    | `hookSubscriptionInfo()` → `getMcc`                  | Bare `after { result = ... }`                                      |
| ~273    | `hookSubscriptionInfo()` → `getMccString`            | Bare `after { result = ... }`                                      |
| ~350    | `hookSettingsSecure()` → `getString`                 | Bare `after { if (args...) result = ... }`                         |
| Line 20 | `telephonyClass`                                     | Uses `.toClass()` not `.toClassOrNull()` — throws if class missing |

**Fix approach**: Wrap every `after { ... }` body inside `runCatching { }`. Also wrap top-level method hooks that don't have `runCatching` around `.hook{}`. Also change `telephonyClass` lazy val from `.toClass()` to `.toClassOrNull()` and add null safety in `hookTelephonyManager()`.

#### `AntiDetectHooker.kt` — `getInstalledApplications` bare `after {}`

| Line | Method                                              | Issue                                                                   |
| ---- | --------------------------------------------------- | ----------------------------------------------------------------------- |
| ~264 | `hookPackageManager()` → `getInstalledApplications` | Bare `after { val apps = result as? MutableList...}` — no `runCatching` |

**Fix approach**: Wrap the `after {}` body with `runCatching {}`.

### Priority 2 — Verify Release APK

After completing Priority 1 fixes:

1. Run `./gradlew assembleRelease` — confirm R8 passes
2. Verify `xposed_init` asset in release APK
3. Check `mapping.txt` confirms `DeviceMaskerService`, `XposedEntry`, `HookEntry`, `AntiDetectHooker` are kept

### Priority 3 — Device Testing (Existing Backlog)

1. ⬜ Deploy debug APK to rooted device
2. ⬜ Set LSPosed scope to "System Framework (android)"
3. ⬜ Reboot and verify service initialization via Diagnostics screen
4. ⬜ Test real-time config updates (change value → verify in target app without restart)
5. ⬜ Verify hook statistics showing in Diagnostics
6. ⬜ Test anti-detection: run detection apps (e.g., RootBeer, XposedChecker)

### Priority 4 — Future Enhancements

- Add Dual-SIM UI section
- Dynamic fingerprint generation
- Cell Info Xposed hooks
- Carrier picker in group creation
- More device presets (aim for 20+)

---

## ✅ COMPLETE: AIDL Architecture Migration (Jan 20, 2026)

**Status**: Implementation Complete ✅ (Device Testing Pending)  
**Scope**: Major refactor from XSharedPreferences to System-Wide AIDL Service

| Phase    | Task                           | Status                                                              |
| -------- | ------------------------------ | ------------------------------------------------------------------- |
| Phase 1  | AIDL Interface & Common Module | ✅ `IDeviceMaskerService.aidl` with 15 methods                      |
| Phase 2  | Xposed Service Implementation  | ✅ `DeviceMaskerService.kt`, `ConfigManager.kt`, `ServiceBridge.kt` |
| Phase 3  | System Hook Implementation     | ✅ `SystemServiceHooker.kt`, `XposedEntry.kt` loadSystem            |
| Phase 4  | Hooker Migration               | ✅ Hybrid `BaseSpoofHooker` (service + XSharedPrefs fallback)       |
| Phase 5  | UI Integration                 | ✅ `ServiceClient.kt`, `DiagnosticsViewModel` service status        |
| Phase 6  | Testing & Validation           | ⏳ Pending device deployment                                        |
| Phase 7  | Documentation & Cleanup        | ✅ Complete                                                         |
| Phase 8  | Dependency Modernization       | ✅ Complete (AGP 9.1.0, Gradle 9.3.1)                               |
| Phase 9  | Stable M3 Migration            | ✅ Complete (Replaced alpha expressive components)                  |
| Phase 10 | R8 Release Build Optimization  | ✅ Complete (Mar 12, 2026)                                          |
| Phase 11 | Hook Safety Audit              | ⏳ In Progress — DeviceHooker + AntiDetectHooker pending            |

---

## Build Status

| Module           | Status     | Last Build   | Notes                                |
| ---------------- | ---------- | ------------ | ------------------------------------ |
| `:common`        | ✅ SUCCESS | Mar 12, 2026 |                                      |
| `:xposed`        | ✅ SUCCESS | Mar 12, 2026 |                                      |
| `:app`           | ✅ SUCCESS | Mar 12, 2026 |                                      |
| Full Debug APK   | ✅ SUCCESS | Mar 12, 2026 | All hooks safety-fixed compile clean |
| Full Release APK | ✅ SUCCESS | Mar 12, 2026 | R8 full mode passes                  |

---

## Recent Decisions & Learnings

### 1. `throwToApp()` is a Throwable Extension in YukiHookAPI

Must call as `exception.throwToApp()` not `throwToApp(exception)`.

```kotlin
// CORRECT
android.content.pm.PackageManager.NameNotFoundException(pkgName).throwToApp()
// WRONG — compile error (receiver type mismatch)
throwToApp(PackageManager.NameNotFoundException(pkgName))
```

### 2. ProGuard `object` Type Specifier Does Not Exist

R8/ProGuard only knows `class`, `interface`, `enum` — not `object`.
Kotlin `object` singletons must use `class` in ProGuard rules:

```proguard
# CORRECT
-keepclassmembers class com.astrixforge.devicemasker.** {
    public static final *** INSTANCE;
}
# WRONG — R8 parse error at build time
-keepclassmembers object ** { ... }
```

### 3. `SecureRandom` is Mandatory in Generators

`java.util.Random` and `Kotlin.random.Random.Default` (used by `chars.random()`) are NOT cryptographically secure. All ID generators must use `java.security.SecureRandom`.

### 4. `.toClass()` vs `.toClassOrNull()` in Hookers

`.toClass()` throws `ClassNotFoundException` if the class doesn't exist on the current Android version. Always prefer `.toClassOrNull()?. apply { ... }` for safety. The only exception is core classes guaranteed to exist (e.g., `java.lang.Thread`).

---

## Important Files Reference

### Hooker Files (`:xposed`)

| File                          | Status                    | Notes                              |
| ----------------------------- | ------------------------- | ---------------------------------- |
| `hooker/AntiDetectHooker.kt`  | ⚠️ 1 fix pending          | `getInstalledApplications` after{} |
| `hooker/DeviceHooker.kt`      | ❌ Multiple fixes pending | See pending tasks above            |
| `hooker/NetworkHooker.kt`     | ✅ Fixed Mar 12           | All after{} wrapped                |
| `hooker/AdvertisingHooker.kt` | ✅ Fixed Mar 12           | All after{} + SecureRandom         |
| `hooker/BaseSpoofHooker.kt`   | ✅ Good                   |                                    |
| `hooker/LocationHooker.kt`    | ⬜ Not yet audited        | Run grep check                     |
| `hooker/SensorHooker.kt`      | ⬜ Not yet audited        | Run grep check                     |
| `hooker/SystemHooker.kt`      | ⬜ Not yet audited        | Run grep check                     |
| `hooker/WebViewHooker.kt`     | ⬜ Not yet audited        | Run grep check                     |
| `utils/ValueGenerators.kt`    | ✅ Fixed Mar 12           | Now uses SecureRandom              |

### Build Configuration Files

| File                        | Status              | Notes                                   |
| --------------------------- | ------------------- | --------------------------------------- |
| `gradle.properties`         | ✅ Optimized Mar 12 | R8 full mode, 4GB heap, parallel, cache |
| `app/proguard-rules.pro`    | ✅ Rewritten Mar 12 | Comprehensive coverage                  |
| `xposed/consumer-rules.pro` | ✅ Expanded Mar 12  | Full service layer                      |
| `common/consumer-rules.pro` | ✅ Expanded Mar 12  | AIDL + generators                       |
| `app/build.gradle.kts`      | ✅ Updated Mar 12   | Signing, packaging                      |

### AIDL Architecture Files

| File                                    | Purpose                     |
| --------------------------------------- | --------------------------- |
| `common/aidl/IDeviceMaskerService.aidl` | AIDL interface (14 methods) |
| `xposed/service/DeviceMaskerService.kt` | Service in system_server    |
| `xposed/service/ConfigManager.kt`       | Config persistence          |
| `xposed/service/ServiceBridge.kt`       | ContentProvider bridge      |
| `xposed/hooker/SystemServiceHooker.kt`  | Boot-time hook              |
| `app/service/ServiceClient.kt`          | UI client                   |
