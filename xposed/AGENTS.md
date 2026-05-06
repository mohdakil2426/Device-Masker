# :xposed Module Guide

libxposed module entry, hook layer, RemotePreferences reader, anti-detection, diagnostics service. This code runs INSIDE target app processes and system_server. Every line must be safe for hostile process contexts.

## Module Structure

```
xposed/src/main/
├── kotlin/.../xposed/
│   ├── hooker/             Target-process hook implementations and shared hook helpers
│   ├── service/            System-server diagnostics service support
│   ├── diagnostics/        Hook-side diagnostics and health tracking
│   └── (root)              Module entry, preference reader, logging, deoptimization support
│
└── resources/META-INF/xposed/
    ├── java_init.list      XposedEntry class name
    ├── module.prop         minApiVersion=101, targetApiVersion=101
    └── scope.list          android, system
```

## Critical Constraints

- **No Timber** — use `DualLog` (logcat + diagnostics sink)
- **No random identifier generation** — read values from RemotePreferences only
- **No reading app-private JSON** from target processes
- **No hardcoded key strings** — use `PrefsKeys` (delegates to `SharedPrefsKeys` in `:common`)
- **No Compose imports** — pure Kotlin + Android framework only
- **No StrictMode installation** in target app or system_server hook paths. StrictMode is app-process debug tooling only.
- **No direct Kotlin SAM `.intercept { ... }` callbacks** — use `stableHooker { ... }` or explicit named `XposedInterface.Hooker`
- `libxposed:api` is `compileOnly` — LSPosed provides runtime
- Rethrow `XposedFrameworkError` before generic `Throwable` in all catch blocks

## Entry Point — XposedEntry

Extends `XposedModule` (libxposed API 101). No-arg constructor, framework-instantiated.

### Lifecycle

1. **`onModuleLoaded`** — sets singleton `instance`, logs process name
2. **`onSystemServerStarting`** — calls `SystemServiceHooker.hook()` to register diagnostics AIDL. Every line try-caught (crash = bootloop).
3. **`onPackageReady`** — main hook path:
   - Skips `android`, own package, SystemUI, Phone, GMS
   - Gets `RemotePreferences` via `getRemotePreferences(PREFS_GROUP)`
   - Checks global kill-switch + per-app enable
   - Deduplicates per ClassLoader (`ConcurrentHashMap` of identity hashes)
   - Registers target hookers in the project-defined order. Keep anti-detection first unless a fresh runtime validation proves a different order safe.

### Package Selection

`selectHookPackage()` derives base package from `processName` (before `:`), checks both loaded and base package against RemotePreferences enable keys, returns first enabled candidate.

## Hook Registration Pattern

Every hook follows this pattern:

```kotlin
safeHook("methodName") {
    class.methodOrNull(params)?.let { m ->
        xi.hook(m).intercept(stableHooker { chain ->
            val original = chain.proceed()           // get real value first
            val spoofed = getConfiguredValue(...)    // from RemotePreferences
            if (spoofed != null) { reportSpoof(); return@stableHooker spoofed }
            original                                 // fallback to real value
        })
        xi.deoptimize(m)    // prevent ART JIT inlining
    }
}
```

Direct `xi.hook(m).intercept { ... }` Kotlin SAM callbacks are forbidden in runtime hookers unless release R8/runtime validation explicitly proves them safe.

## BaseSpoofHooker — Shared Helpers

| Method | Purpose |
|--------|---------|
| `safeHook(name, block)` | Try-catch per method. One failure never blocks others. Rethrows `XposedFrameworkError`. |
| `ClassLoader.loadClassOrNull(name)` | Returns null on `ClassNotFoundException` |
| `Class.methodOrNull(name, params)` | Returns null on `NoSuchMethodException`, auto-sets accessible |
| `getSpoofValue(prefs, pkg, type, fallback)` | Read with fallback |
| `getConfiguredSpoofValue(prefs, pkg, type)` | Returns null if not configured |
| `reportSpoofEvent(pkg, type)` | Fire-and-forget to diagnostics |

## PrefsHelper — Config Reader

| Method | Returns |
|--------|---------|
| `getStoredSpoofValue(prefs, pkg, type)` | `null` if disabled/blank/absent |
| `getSpoofValue(prefs, pkg, type, fallback)` | value or `fallback()` |
| `isSpoofTypeEnabled(prefs, pkg, type)` | `false` default (opt-in) |

## Hooker Summary

| Hooker | APIs Hooked | SpoofType |
|--------|-------------|-----------|
| **DeviceHooker** (451 lines) | TelephonyManager (getDeviceId, getImei, getSubscriberId, getSimSerialNumber, getSimCountryIso, getNetworkCountryIso, getSimOperatorName, getSimOperator, getNetworkOperator, getLine1Number — no-arg + int-slot variants), Build.getSerial(), Settings.Secure.getString (android_id), SystemProperties.get (ro.serialno, ro.boot.serialno, ril.serialnumber) | IMEI, IMSI, ICCID, SIM_COUNTRY_ISO, NETWORK_COUNTRY_ISO, SIM_OPERATOR_NAME, CARRIER_MCC_MNC, NETWORK_OPERATOR, PHONE_NUMBER, SERIAL, ANDROID_ID |
| **SubscriptionHooker** | SubscriptionInfo (getIccId, getCountryIso, getCarrierName, getDisplayName, getMcc, getMnc, getMccString, getMncString, getNumber), SubscriptionManager.getActiveSubscriptionInfoList (pass-through) | ICCID, SIM_COUNTRY_ISO, CARRIER_NAME, CARRIER_MCC_MNC, PHONE_NUMBER |
| **NetworkHooker** | WifiInfo (getMacAddress, getSSID, getBSSID), NetworkInterface.getHardwareAddress (wlan/wifi/p2p only), BluetoothAdapter.getAddress, TelephonyManager (getNetworkOperatorName, getNetworkOperator) | WIFI_MAC, WIFI_SSID, WIFI_BSSID, BLUETOOTH_MAC, CARRIER_NAME, CARRIER_MCC_MNC |
| **SystemHooker** | Build static fields (direct mutation: FINGERPRINT, MODEL, MANUFACTURER, BRAND, DEVICE, PRODUCT, BOARD, HARDWARE, TAGS, TYPE), SystemProperties.get (16 ro.* keys) | DEVICE_PROFILE |
| **LocationHooker** | Location (getLatitude, getLongitude), LocationManager.getLastKnownLocation, TimeZone.getDefault, Locale.getDefault | LOCATION_LATITUDE, LOCATION_LONGITUDE, TIMEZONE, LOCALE |
| **SensorHooker** | SensorManager.getSensorList (TYPE_ALL only, filters when >10), Sensor (getVendor, getVersion, getName) | DEVICE_PROFILE |
| **AdvertisingHooker** | AdvertisingIdClient.Info.getId, Gservices (getString, getLong for android_id), MediaDrm.getPropertyByteArray (deviceUniqueId) | ADVERTISING_ID, GSF_ID, MEDIA_DRM_ID |
| **WebViewHooker** | WebSettings (getUserAgentString, setUserAgentString), WebView.getDefaultUserAgent | DEVICE_PROFILE |
| **PackageManagerHooker** | ApplicationPackageManager (getPackageInfo, getApplicationInfo, getInstalledPackages, getInstalledApplications, queryIntentActivities) — throws NameNotFoundException for self | Self-hiding |

## AntiDetectHooker — Detection Vectors

| Vector | Status | Method |
|--------|--------|--------|
| Stack trace filtering | **ACTIVE** | Hooks `Thread.getStackTrace()`, `Throwable.getStackTrace()`. Filters 16 patterns (XposedBridge, LSPosed, YukiHookAPI, EdXposed, libxposed, etc.). ThreadLocal reentrant guard. |
| `/proc/self/maps` | **ACTIVE** | Hooks `BufferedReader.readLine()`. Returns empty for 8 patterns (libxposed, liblspd, libriru, etc.). |
| Package hiding | **ACTIVE** | Hides 6 packages (LSPosed Manager 3 variants, Magisk, VirtualXposed, EdXposed) via PM hooks. `ExceptionMode.PASSTHROUGH` for throws. |
| `Class.forName` | **DISABLED** | Defined but not called. Caused startup instability. |
| `ClassLoader.loadClass` | **DISABLED** | Defined but not called. Caused startup instability. |

Safe class prefixes (never hidden): `android.`, `androidx.`, `com.android.`, `com.google.`, `dalvik.`, `java.`, `javax.`, `kotlin.`, `kotlinx.`, `org.jetbrains.`

## DeoptimizeManager

Prevents ART JIT/AOT from inlining hooked methods. Short methods (<20 bytecodes) get inlined, bypassing hooks.

- `deoptimizeWithCallers(xi, target, callers?)` — primary + optional callers
- `deoptimizeAll(xi, methods)` — batch with per-method guarding
- **Always call AFTER `xi.hook()`** — deoptimizing before hooking is a no-op

## Diagnostics

- `DualLog` — logs to logcat + `XposedDiagnosticEventSink` + AIDL service
- `HookMetrics` — success/failure counts via `HookHealthRegistry`
- `HookHealthRegistry` — thread-safe AtomicLong counters, per-method health, spoof event throttling (first 5, then 10/100/1000 milestones)
- `XposedDiagnosticEventSink` — builds `DiagnosticEvent`, forwards to logcat + `XposedEntry.log()` + AIDL
- `DeviceMaskerService` — runs in system_server, 500-entry ring buffer, per-package spoof counters
- `SystemServiceHooker` — hooks `AMS.systemReady()` + `SystemServer.run()`, registers service as `user.devicemasker_diag`

## Metadata

- `META-INF/xposed/java_init.list`: `com.astrixforge.devicemasker.xposed.XposedEntry`
- `META-INF/xposed/module.prop`: `minApiVersion=101`, `targetApiVersion=101`, `staticScope=false`
- `META-INF/xposed/scope.list`: `android`, `system`
- `src/main/resources/` must be in `resources.srcDirs` (Java resources, not Android assets)

## Build

- `android.library`, `kotlin.serialization`, JVM 17
- `buildConfig = false`, `aidl = false`
- `compileOnly(libs.libxposed.api)` — LSPosed provides runtime
- `implementation(libs.hiddenapibypass)`, `libs.kotlinx.coroutines.core`, `libs.kotlinx.serialization.json`
- `testImplementation(libs.libxposed.api)` — for unit tests that reference API classes

## ProGuard (consumer-rules.pro)

Keep rules must preserve the libxposed entry point, hooker classes, diagnostics service support, preference helpers, hook-side logging/metrics, libxposed service bridge types, and the R8-safe callback package `xposed.hooker.callback.**`.

## Testing

Coverage should include:
- `PrefsHelperTest` — null for disabled/blank, returns configured value
- `HookSafetyTest` — WebView UA parsing, AntiDetect safe-prefix pass-through, all hookers use safeHook, PM overload discovery, Location copy safety
- `DiagnosticsLogBufferTest` — cap, dropped count, clear
- `HookHealthRegistryTest` — registration counters, spoof aggregation, rate-limiting
- `R8HookerAbiTest` — forbids direct runtime `.intercept { ... }` callbacks and checks R8 callback keep coverage
