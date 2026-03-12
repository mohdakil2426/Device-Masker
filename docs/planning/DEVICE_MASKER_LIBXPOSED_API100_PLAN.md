# Device Masker — libxposed API 100 Complete Migration & Undetectability Plan

> **Prepared:** March 2026  
> **Scope:** `:xposed` module only — `:app`, `:common`, AIDL, UI unchanged unless noted  
> **Goal:** Zero-crash spoofing + 100% value correlation + ART inline bypass + live config

---

## Table of Contents

1. [What libxposed API 100 Actually Is](#1-what-libxposed-api-100-actually-is)
2. [API 100 vs API 82 — Deep Comparison](#2-api-100-vs-api-82--deep-comparison)
3. [Why Crashes Happen Now — Root Cause Analysis](#3-why-crashes-happen-now--root-cause-analysis)
4. [Why Apps Detect Spoofing Now — Gap Analysis](#4-why-apps-detect-spoofing-now--gap-analysis)
5. [Phase 0 — Dependency & Build Setup](#5-phase-0--dependency--build-setup)
6. [Phase 1 — Entry Point Migration](#6-phase-1--entry-point-migration)
7. [Phase 2 — ModuleContext & RemotePreferences](#7-phase-2--modulecontext--remotepreferences)
8. [Phase 3 — BaseSpoofHooker Rewrite](#8-phase-3--basespoofhooker-rewrite)
9. [Phase 4 — DeoptimizeManager (Bypass ART Inline)](#9-phase-4--deoptimizemanager-bypass-art-inline)
10. [Phase 5 — DeviceHooker Complete Rewrite](#10-phase-5--devicehooker-complete-rewrite)
11. [Phase 6 — NetworkHooker Completion](#11-phase-6--networkhooker-completion)
12. [Phase 7 — NEW SubscriptionHooker](#12-phase-7--new-subscriptionhooker)
13. [Phase 8 — SystemHooker Hardening](#13-phase-8--systemhooker-hardening)
14. [Phase 9 — WebViewHooker Completion](#14-phase-9--webviewhooker-completion)
15. [Phase 10 — NEW PackageManagerHooker](#15-phase-10--new-packagemanagerhooker)
16. [Phase 11 — AntiDetectHooker Hardening](#16-phase-11--antidetecthooker-hardening)
17. [Phase 12 — TAC-Aware IMEI Generator](#17-phase-12--tac-aware-imei-generator)
18. [Phase 13 — DeviceProfilePreset Enrichment](#18-phase-13--deviceprofilepreset-enrichment)
19. [Phase 14 — Full Correlation Matrix](#19-phase-14--full-correlation-matrix)
20. [Phase 15 — ProGuard & Manifest](#20-phase-15--proguard--manifest)
21. [Phase 16 — RemotePreferences in :app](#21-phase-16--remotepreferences-in-app)
22. [Testing Checklist](#22-testing-checklist)
23. [Complete File Change Index](#23-complete-file-change-index)

---

## 1. What libxposed API 100 Actually Is

### The Name Is Not a Version — It Is an API Level Declaration

The string "API 100" refers to the `minApiVersion` declared in `META-INF/xposed/module.prop`, which tells the LSPosed framework that this module requires at minimum Xposed API level 100. This is analogous to Android's `minSdkVersion`. It does **not** mean a library version number of "100".

The actual library artifact is:
```
io.github.libxposed:api
io.github.libxposed:service
```

These are not on Maven Central (as of early 2026). You must either:
- **Download the AAR from GitHub Actions CI artifacts** of the `libxposed/api` repo, or
- **Add them as a local AAR** in `libs/`, or  
- **Use source dependencies** (clone as a submodule)

### The Library Is `compileOnly` — Like the Old API

The libxposed API jar/aar is `compileOnly` in your module. At runtime, LSPosed provides the implementation. Your DEX only contains your code + annotation references. This is identical behavior to how `de.robv.android.xposed:api:82` worked.

### What LSPosed Versions Support It

LSPosed 1.9.0+ implements the modern API. LSPosed 1.10.2+ (which your project already requires) fully supports it. The modern API is available alongside the legacy API — LSPosed supports both simultaneously.

---

## 2. API 100 vs API 82 — Deep Comparison

### Entry Point

| Aspect | API 82 (Legacy) | API 100 (Modern) |
|---|---|---|
| Entry declaration | `assets/xposed_init` | `META-INF/xposed/java_init.list` |
| Module metadata | `AndroidManifest.xml` meta-data tags | `META-INF/xposed/module.prop` |
| Scope declaration | LSPosed Manager UI / meta-data | `META-INF/xposed/scope.list` |
| Base class | `IXposedHookLoadPackage` (interface) | `XposedModule` (abstract class) |
| Callback method | `handleLoadPackage(lpparam)` | `onPackageLoaded(param)` + `onSystemServerLoaded(param)` |
| App self-hook | Yes, module app is hooked too | **No** — module app is never hooked |
| R8/ProGuard obfuscation | Entry class must be kept by name | `adaptresourcefilenames` handles it |

### Hooking API

| Aspect | API 82 | API 100 |
|---|---|---|
| Hook method | `XposedHelpers.findAndHookMethod(...)` | `hook(method, HookerClass::class.java)` |
| Hook result | `XC_MethodHook.MethodHookParam` | `BeforeHookCallback` / `AfterHookCallback` |
| Return replacement | `param.result = value` | `callback.returnAndSkip(value)` |
| Get args | `param.args[0]` | `callback.args[0]` |
| Get this | `param.thisObject` | `callback.thisObject` |
| Unhook handle | No clean API | `MethodUnhooker<Method>` — call `.unhook()` |
| Hook priority | Fixed constants | `Int` priority value (higher = earlier) |
| Class init hook | Not clean | `hookClassInitializer(clazz, Hooker::class.java)` |
| ART inline bypass | **Not possible** | `deoptimize(method)` |
| Call original | `XposedBridge.invokeOriginalMethod(...)` | `invokeOrigin(method, thisObject, args)` |
| Call super | Not clean | `invokeSpecial(method, thisObject, args)` |

### Configuration / Preferences

| Aspect | API 82 | API 100 |
|---|---|---|
| Cross-process prefs | `XSharedPreferences` (file-based, cached) | `getRemotePreferences(group)` (live, LSPosed-managed) |
| Change listener | None — needs app restart | **Live** — changes propagate immediately |
| Write side | App writes `MODE_WORLD_READABLE` SharedPrefs | App writes **normal** SharedPrefs; libxposed service bridges |
| Storage location | Module app's internal storage | LSPosed database |
| Read-only in hooks | No (could write, bad practice) | **Enforced read-only** in hooked processes |

### Logging

| API 82 | API 100 |
|---|---|
| `XposedBridge.log(msg)` | `log(priority, tag, msg, throwable)` (uses `android.util.Log` priorities) |

### Dex Analysis

| API 82 | API 100 |
|---|---|
| None | `parseDex(ByteBuffer, includeAnnotations)` → `DexParser` |
| None | Find all callers of a method to feed into `deoptimize()` |

---

## 3. Why Crashes Happen Now — Root Cause Analysis

### 3.1 `toClass()` Throws, `toClassOrNull()` Doesn't

Your `DeviceHooker`:
```kotlin
// CRASHES if class not found in ANY process — webview sandbox, isolated service, etc.
private val telephonyClass by lazy { "android.telephony.TelephonyManager".toClass() }
```

Rule: **Every class not guaranteed to exist in every process must use `toClassOrNull()`**, followed by a null guard. `TelephonyManager` doesn't exist in isolated renderer processes that some apps spawn.

### 3.2 A Single `runCatching` Wraps Too Many Hooks

```kotlin
runCatching {
    method { name = "getSimCountryIso"; param(IntType) }
        .hook { ... }
    method { name = "getSimOperator"; param(IntType) }  // If THIS throws...
        .hook { ... }                                    // ...entire block fails silently
}
```

If ANY method lookup throws inside the block, ALL subsequent hooks in that `runCatching` are silently skipped. Every individual hook must be in its own `try-catch`.

### 3.3 `Build` Static Field Mutation Races App Init

```kotlin
// SystemHooker — done at hook time, NOT inside a method hook callback
field { name = "FINGERPRINT" }.get().set(spoofedValue)
```

ART may have already compiled methods that read `Build.FINGERPRINT` into their AOT-compiled code via constant folding. Additionally, if the app's `Application.onCreate()` runs before your hooker's `onHook()` (race condition on some Android versions), the app reads real values first. The correct approach is `hookClassInitializer` on `Build`.

### 3.4 YukiHookAPI `replaceAny {}` on Methods With No Return Value

If `replaceAny` is used on a void method, it silently does nothing. This is a known YukiHookAPI quirk that disappears with the new API.

### 3.5 `getImei(int)` and Multi-Slot Methods Vary by OEM

On some Chinese OEM firmwares (Xiaomi MIUI, OPPO ColorOS), `getImei(int)` has a different signature or is replaced entirely. Samsung's `ITelephony` binder interface also exposes additional per-slot methods. Hooking must be defensive per-method, per-signature.

---

## 4. Why Apps Detect Spoofing Now — Gap Analysis

### 4.1 IMEI TAC Does Not Match Device Model

The IMEI structure: `[TAC 8 digits][SNR 6 digits][CD 1 digit]`

The TAC (Type Allocation Code) uniquely identifies a device model globally. The first 2 digits are the Reporting Body Identifier (RBI):
- `35` = BABT (UK) — used by Google Pixel, Samsung, Apple, OnePlus
- `86` = CITA (China) — used by Xiaomi, OPPO, Vivo, Huawei  
- `01` = PTCRB (USA) — AT&T-certified devices
- `45` = BABT (allocated to certain ranges)

**Current bug:** `ValueGenerators.imei()` uses random prefixes from `listOf("35", "86", "01", "45")`. A "Pixel 8 Pro" device profile has real TAC ranges like `35414610`, `35414611`, etc. If you spoof `Build.MODEL = "Pixel 9 Pro"` but generate an IMEI starting with `86`, fraud detection SDKs immediately flag it as a Chinese OEM running a spoofed build.

**Fix required:** Each `DeviceProfilePreset` must carry a list of valid TAC prefixes. IMEI generation must pick from the preset's TAC list.

### 4.2 `SubscriptionManager` Is Completely Unhooked

Modern apps — especially banking and e-commerce — use:
```java
SubscriptionManager sm = context.getSystemService(SubscriptionManager.class);
List<SubscriptionInfo> subs = sm.getActiveSubscriptionInfoList();
// Each SubscriptionInfo exposes: iccId, number, displayName, countryIso, mcc, mnc
```

This bypasses your `TelephonyManager` hooks entirely. Every `SubscriptionInfo` object returns real values.

### 4.3 `TelephonyManager.createForSubscriptionId()` Returns Unhooked Instances

```java
// App does this — your hooks on the original TM instance don't cover it:
TelephonyManager tmForSub = tm.createForSubscriptionId(subId);
String imei = tmForSub.getImei(); // Returns REAL IMEI
```

You must hook the constructor of `TelephonyManager` or hook `createForSubscriptionId` to intercept the new instance.

### 4.4 `Build` Static Fields Are Not Fully Covered

Missing from your `DeviceProfilePreset` and `SystemHooker`:
- `Build.TIME` — build timestamp (a "Pixel 9 Pro" with year-2018 timestamp is suspicious)
- `Build.SUPPORTED_ABIS` / `Build.SUPPORTED_32_BIT_ABIS` / `Build.SUPPORTED_64_BIT_ABIS`
- `Build.VERSION.SECURITY_PATCH` — date string like `"2024-12-01"`
- `Build.VERSION.INCREMENTAL` — build number like `"AP3A.241005.015"`
- `Build.VERSION.CODENAME` — `"REL"` for releases
- `Build.ID` — like `"AP3A.241005.015"`
- `Build.TAGS` — `"release-keys"` vs `"dev-keys"`
- `Build.TYPE` — `"user"` vs `"userdebug"`

### 4.5 `getNetworkType()` / `getDataNetworkType()` Not Spoofed

Apps cross-reference the carrier (MCC/MNC) against the network type. Jio in India (404-40) on a real 2025 device returns `NETWORK_TYPE_NR` (20) or `NETWORK_TYPE_LTE` (13). If your spoofed carrier is Jio but `getNetworkType()` returns HSPA+ (15) because that's what your real SIM returns, fraud SDKs flag it.

### 4.6 `getAllCellInfo()` Leaks Real Cell Tower Data

```java
List<CellInfo> cells = tm.getAllCellInfo();
// CellIdentityLte.getMcc(), .getMnc() — real values even if you spoofed TM methods
```

The `CellInfo` objects are raw kernel-level data and are not covered by your hooks.

### 4.7 `WebView.getDefaultUserAgent(Context)` Is a Static Method

Your `WebViewHooker` hooks `WebSettings.getUserAgentString()`. But many apps also call:
```java
String ua = WebView.getDefaultUserAgent(context); // Static — different method
String httpAgent = System.getProperty("http.agent"); // JVM system property
```

### 4.8 `PackageManager.hasSystemFeature()` Leaks Real Hardware

```java
// Apps check these against expected values for the spoofed device:
pm.hasSystemFeature("android.hardware.sensor.accelerometer") // Should match device profile
pm.hasSystemFeature("android.hardware.camera.flash")         // Pixel 9 Pro has this
pm.hasSystemFeature("android.hardware.nfc")                  // Not all devices do
pm.hasSystemFeature("android.hardware.telephony.ims")        // 5G IMS support
```

### 4.9 ART Inlining Makes Hooks Invisible

Short methods that ART has compiled via JIT/AOT get inlined into callers. Your hook on `getImei()` may never fire for an app that heavily uses TelephonyManager because ART inlined the IMEI read directly into the calling method's compiled code. The **only** fix for this in the Xposed ecosystem is `deoptimize()`, which is exclusive to the libxposed modern API.

### 4.10 `ClassLoader.loadClass()` vs `Class.forName()`

Your `AntiDetectHooker` hooks `Class.forName()` to prevent Xposed class detection. But apps can use:
```java
getClass().getClassLoader().loadClass("de.robv.android.xposed.XposedBridge")
```
This bypasses `Class.forName()` hooks entirely.

---

## 5. Phase 0 — Dependency & Build Setup

### 5.1 Obtaining the libxposed AAR

Since libxposed is not on Maven Central, add as local libs:

```
xposed/
  libs/
    libxposed-api.aar         # from libxposed/api GitHub Actions CI
    libxposed-service.aar     # from libxposed/service GitHub Actions CI
```

### 5.2 `gradle/libs.versions.toml`

```toml
[versions]
# Remove entirely:
# yukihookapi = "1.3.1"
# kavaref = "1.0.2"

# Keep:
xposed-api-legacy = "82"          # Keep as fallback reference — remove after full migration
libxposed-api = "local"           # Local AAR

[libraries]
# REMOVE:
# yukihookapi-api = ...
# yukihookapi-ksp-xposed = ...
# kavaref-core = ...
# kavaref-extension = ...

# KEEP:
hiddenapibypass = { group = "org.lsposed.hiddenapibypass", name = "hiddenapibypass", version.ref = "hiddenapibypass" }
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serializationJson" }

# ADD:
libxposed-api = { group = "io.github.libxposed", name = "api", version.ref = "libxposed-api" }
libxposed-service = { group = "io.github.libxposed", name = "service", version.ref = "libxposed-api" }
```

### 5.3 `xposed/build.gradle.kts`

```kotlin
android {
    // The xposed module does NOT need AIDL enabled
    buildFeatures {
        buildConfig = false
        aidl = false
    }
    
    // CRITICAL: libxposed needs Java resources packaged properly
    sourceSets {
        getByName("main") {
            resources.srcDirs("src/main/resources")
        }
    }
}

dependencies {
    implementation(project(":common"))
    
    // Modern Xposed API — compileOnly, provided at runtime by LSPosed
    compileOnly(files("libs/libxposed-api.aar"))
    
    // Service for RemotePreferences write-side in :app
    // NOT needed in :xposed — only needed in :app
    
    // Hidden API bypass
    implementation(libs.hiddenapibypass)
    
    // Coroutines for async service init
    implementation(libs.kotlinx.coroutines.core)
    
    // Serialization for config objects
    implementation(libs.kotlinx.serialization.json)
    
    // REMOVE: yukihookapi.api, kavaref.core, kavaref.extension
    // REMOVE: compileOnly(libs.xposed.api) — replaced by libxposed-api.aar
}
```

### 5.4 `app/build.gradle.kts`

```kotlin
dependencies {
    // REMOVE: yukihookapi.api, kavaref.*, ksp(yukihookapi.ksp.xposed)
    // REMOVE: compileOnly(libs.xposed.api)
    
    // ADD: libxposed service for writing RemotePreferences
    implementation(files("../xposed/libs/libxposed-api.aar"))
    implementation(files("../xposed/libs/libxposed-service.aar"))
    
    // Keep everything else unchanged
}
```

Also in `app/build.gradle.kts` plugins block:
```kotlin
plugins {
    // REMOVE: alias(libs.plugins.ksp)
    // Everything else stays
}
```

---

## 6. Phase 1 — Entry Point Migration

### 6.1 Remove `assets/xposed_init`

Delete: `xposed/src/main/assets/xposed_init`

### 6.2 Create `META-INF/xposed/java_init.list`

Create file at: `xposed/src/main/resources/META-INF/xposed/java_init.list`

```
com.astrixforge.devicemasker.xposed.XposedEntry
```

### 6.3 Create `META-INF/xposed/module.prop`

Create file at: `xposed/src/main/resources/META-INF/xposed/module.prop`

```properties
minApiVersion=100
targetApiVersion=100
staticScope=false
```

- `minApiVersion=100` — requires LSPosed with modern API support (1.9.0+, you already need 1.10.2+)
- `targetApiVersion=100` — tells the framework what API surface we use
- `staticScope=false` — scope is dynamically managed (users can add any app)

### 6.4 Create `META-INF/xposed/scope.list`

Create file at: `xposed/src/main/resources/META-INF/xposed/scope.list`

```
android
```

This is the default scope hint. Users can override in LSPosed Manager.

### 6.5 Update `AndroidManifest.xml` for `:xposed` module

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="@string/app_name"
        android:description="@string/app_description">
        
        <!-- KEEP these for backward compatibility with older LSPosed -->
        <meta-data
            android:name="xposedmodule"
            android:value="true" />
        <meta-data
            android:name="xposedsharedprefs"
            android:value="true" />
        <meta-data
            android:name="xposedminversion"
            android:value="100" />
        <meta-data
            android:name="xposeddescription"
            android:value="Elite device identifier spoofing with anti-detection" />
    </application>
</manifest>
```

### 6.6 New `XposedEntry.kt`

This is the core entry class. Replace the entire `XposedHookLoader` + `HookEntry` (KSP-generated) with:

```kotlin
package com.astrixforge.devicemasker.xposed

import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerLoadedParam
import com.astrixforge.devicemasker.xposed.hooker.*

/**
 * Device Masker entry point for libxposed API 100.
 *
 * Constructor receives XposedInterface (the hook engine) and ModuleLoadedParam (metadata
 * about which process we are in). This is called ONCE per target process — a new instance
 * of XposedEntry is created for each process that loads this module.
 *
 * Architecture decisions:
 * - ModuleContext is initialized here and shared via singleton
 * - RemotePreferences are obtained here (LSPosed-managed, live updates)
 * - Each hooker class is stateless and receives (xposed, prefs, pkg) per call
 */
class XposedEntry(base: XposedInterface, param: ModuleLoadedParam) : XposedModule(base, param) {

    companion object {
        const val TAG = "DeviceMasker"
        const val SELF_PACKAGE = "com.astrixforge.devicemasker"
        const val PREFS_GROUP = "device_masker_config"

        // Skip these packages entirely for app hooks
        private val SKIP_PACKAGES = setOf(
            SELF_PACKAGE,
            "com.android.systemui",
            "com.android.phone",
            "com.google.android.gms",   // GMS does its own integrity checks; don't interfere
        )

        // Singleton reference — set once per process, used by hookers via static access
        @Volatile
        lateinit var instance: XposedEntry
            private set
    }

    init {
        instance = this
        log(TAG, "XposedEntry loaded in process: ${param.processName}", null)
    }

    /**
     * Called when the System Server (android process) is loaded.
     * This is where AIDL service initialization happens.
     * CRITICAL: Every single line here must be in try-catch. A crash here = bootloop.
     */
    override fun onSystemServerLoaded(param: SystemServerLoadedParam) {
        log(TAG, "System server loading, initializing service hooks...", null)
        try {
            SystemServiceHooker.hook(param.classLoader, this)
        } catch (t: Throwable) {
            log(TAG, "SystemServiceHooker failed (non-fatal, service unavailable): ${t.message}", t)
        }
    }

    /**
     * Called when a package's classloader is ready.
     * param.packageName — the package being loaded
     * param.classLoader — the classloader for that package
     *
     * This fires BEFORE AppComponentFactory, which means BEFORE Application.onCreate().
     * This is the earliest possible hook point — hooks set here intercept the very first
     * identifier reads by the app.
     */
    override fun onPackageLoaded(param: PackageLoadedParam) {
        val pkg = param.packageName

        // System server is handled by onSystemServerLoaded
        if (pkg == "android") return
        
        // Skip our own app and critical system processes
        if (pkg in SKIP_PACKAGES) return

        // Get remote preferences — these are live (no restart needed for config changes)
        // getRemotePreferences() is fast (cached by LSPosed), safe to call per-package
        val prefs = runCatching {
            getRemotePreferences(PREFS_GROUP)
        }.getOrNull() ?: run {
            log(TAG, "RemotePreferences unavailable for $pkg, skipping hooks", null)
            return
        }

        // Check master switch
        val moduleEnabled = prefs.getBoolean("module_enabled", true)
        if (!moduleEnabled) return

        // Check if this app has spoofing enabled
        val appEnabled = prefs.getBoolean("app_enabled_$pkg", false)
        if (!appEnabled) return

        val cl = param.classLoader

        // HOOK ORDER IS CRITICAL:
        // 1. AntiDetect FIRST — must be in place before any identifiers are read
        // 2. Device/Network/etc — the actual spoofing
        hookSafely(pkg, "AntiDetectHooker") {
            AntiDetectHooker.hook(cl, this, prefs, pkg)
        }
        hookSafely(pkg, "DeviceHooker") {
            DeviceHooker.hook(cl, this, prefs, pkg)
        }
        hookSafely(pkg, "SubscriptionHooker") {
            SubscriptionHooker.hook(cl, this, prefs, pkg)
        }
        hookSafely(pkg, "NetworkHooker") {
            NetworkHooker.hook(cl, this, prefs, pkg)
        }
        hookSafely(pkg, "SystemHooker") {
            SystemHooker.hook(cl, this, prefs, pkg)
        }
        hookSafely(pkg, "LocationHooker") {
            LocationHooker.hook(cl, this, prefs, pkg)
        }
        hookSafely(pkg, "SensorHooker") {
            SensorHooker.hook(cl, this, prefs, pkg)
        }
        hookSafely(pkg, "AdvertisingHooker") {
            AdvertisingHooker.hook(cl, this, prefs, pkg)
        }
        hookSafely(pkg, "WebViewHooker") {
            WebViewHooker.hook(cl, this, prefs, pkg)
        }
        hookSafely(pkg, "PackageManagerHooker") {
            PackageManagerHooker.hook(cl, this, prefs, pkg)
        }
    }

    /** Wraps a hooker registration in try-catch so one failed hooker can't crash others. */
    private fun hookSafely(pkg: String, name: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            log(TAG, "[$name] Failed to register hooks for $pkg: ${t.message}", t)
        }
    }
}
```

---

## 7. Phase 2 — ModuleContext & RemotePreferences

### 7.1 How RemotePreferences Works

With libxposed API 100:

- **In `:app`** — you write to a normal `SharedPreferences` backed by the libxposed `service` library. The service registers a `ContentProvider`-style bridge that LSPosed manages.
- **In `:xposed` hookers** — you call `xposedInterface.getRemotePreferences("group_name")` and receive a `SharedPreferences` object that is **live** — changes from the app are reflected immediately without killing or restarting the target app.
- **No more `MODE_WORLD_READABLE`** — this is completely eliminated.
- **No more AIDL service** for config delivery — `RemotePreferences` replaces it for the configuration use case.

### 7.2 Preference Key Convention

Keep using your existing `SharedPrefsKeys` from `:common`. The key format doesn't change — only the delivery mechanism changes.

### 7.3 `:app` Write Side Changes

In `XposedPrefs.kt` (`:app`), replace:
```kotlin
// OLD — MODE_WORLD_READABLE
context.getSharedPreferences(name, Context.MODE_WORLD_READABLE)
```

With the libxposed service write API:
```kotlin
// NEW — libxposed service bridges this to RemotePreferences
// Uses io.github.libxposed.service.ModulePreferences or the service's SharedPreferences bridge
// The exact API depends on which libxposed-service version you download
// In practice: just write to normal SharedPreferences with a known group name
// LSPosed's XSharedPreferences bridge handles the rest when module.prop declares minApiVersion=100
```

> **Important note:** For the `minApiVersion=100` case, LSPosed itself implements the bridge. When your module declares API 100, LSPosed intercepts reads of your module's SharedPreferences in target processes and provides them via `getRemotePreferences()`. You do NOT need to change your app-side write code — only add the `libxposed-service` dependency and initialize it. Your existing `ConfigSync` → `XposedPrefs` pipeline continues to work; only the delivery to hooks is now via the modern API.

### 7.4 `PrefsHelper.kt` in `:xposed`

```kotlin
package com.astrixforge.devicemasker.xposed

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.SharedPrefsKeys

/**
 * Thread-safe preference reading for the libxposed modern API.
 *
 * RemotePreferences obtained via getRemotePreferences() are:
 * - Read-only (enforced by libxposed)
 * - Live: always reflect the latest values the module app wrote
 * - Safe to call on any thread
 * - NOT cached between calls — each read reflects current state
 *
 * No reload() calls needed (unlike XSharedPreferences).
 */
object PrefsHelper {

    /**
     * Gets the spoof value for a type, or invokes the fallback if not configured.
     *
     * @param prefs RemotePreferences from XposedEntry.getRemotePreferences()
     * @param packageName Target app package name
     * @param type The SpoofType to look up
     * @param fallback Called if no spoof value is set (generates a valid fallback)
     */
    fun getSpoofValue(
        prefs: SharedPreferences,
        packageName: String,
        type: SpoofType,
        fallback: () -> String,
    ): String {
        // Check if this spoof type is enabled for this app
        val typeEnabled = prefs.getBoolean(
            SharedPrefsKeys.getSpoofEnabledKey(packageName, type),
            false
        )
        if (!typeEnabled) return fallback()

        return prefs.getString(
            SharedPrefsKeys.getSpoofValueKey(packageName, type),
            null
        ) ?: fallback()
    }

    /**
     * Checks if a spoof type is explicitly enabled for this package.
     */
    fun isSpoofTypeEnabled(
        prefs: SharedPreferences,
        packageName: String,
        type: SpoofType,
    ): Boolean = prefs.getBoolean(SharedPrefsKeys.getSpoofEnabledKey(packageName, type), false)
}
```

---

## 8. Phase 3 — BaseSpoofHooker Rewrite

Remove `YukiBaseHooker` inheritance. The new base class is a plain Kotlin class with static factory pattern:

```kotlin
package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.MethodUnhooker
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.PrefsHelper
import com.astrixforge.devicemasker.xposed.XposedEntry
import java.lang.reflect.Method

/**
 * Base for all spoof hookers.
 *
 * Each hooker is a stateless singleton (object) with a static `hook()` method.
 * No instance state — all context is passed per-call.
 * This design matches libxposed API 100's model: one XposedModule instance per process,
 * hook registrations are done once at load time, callbacks are stateless.
 *
 * Hook registration pattern:
 * ```kotlin
 * object DeviceHooker {
 *     fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
 *         val tmClass = cl.loadClassOrNull("android.telephony.TelephonyManager") ?: return
 *         safeHook("getImei") {
 *             val method = tmClass.getDeclaredMethod("getImei")
 *             xi.hook(method, ImeiHooker::class.java)
 *             xi.deoptimize(method) // Bypass ART inlining
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseSpoofHooker(protected val tag: String) {

    protected fun safeHook(methodName: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            XposedEntry.instance.log(
                android.util.Log.WARN,
                tag,
                "safeHook($methodName) failed: ${t.javaClass.simpleName}: ${t.message}",
                null
            )
        }
    }

    protected fun ClassLoader.loadClassOrNull(name: String): Class<*>? =
        try { loadClass(name) } catch (_: ClassNotFoundException) { null }

    protected fun Class<*>.methodOrNull(name: String, vararg params: Class<*>): Method? =
        try {
            getDeclaredMethod(name, *params).also { it.isAccessible = true }
        } catch (_: NoSuchMethodException) { null }
}
```

---

## 9. Phase 4 — DeoptimizeManager (Bypass ART Inline)

### 9.1 Why ART Inlining Is Your Biggest Silent Failure

When an app calls `telephonyManager.getImei()` in a tight loop or in a method that's been heavily JIT-compiled, ART's optimizer may **inline** `getImei()` directly into the caller's compiled machine code. Your Xposed hook is placed on the `getImei()` method object — but if the call never actually dispatches to that method object (because the code was inlined), your hook callback is never invoked. The app gets the real IMEI and your hook never even fires.

`deoptimize(method)` tells ART: "do not optimize away calls to this method — always dispatch through the method table so my hook can intercept." This is exclusive to libxposed API 100.

### 9.2 `DeoptimizeManager.kt`

```kotlin
package com.astrixforge.devicemasker.xposed

import io.github.libxposed.api.XposedInterface
import android.util.Log
import java.lang.reflect.Method

/**
 * Centralized deoptimization manager.
 *
 * deoptimize() MUST be called AFTER hook() to be effective.
 * It is idempotent — calling it multiple times on the same method is safe.
 *
 * When to deoptimize:
 * - Any method that is short (<20 bytecodes) — ART aggressively inlines these
 * - Any method called in hot loops in typical app usage
 * - TelephonyManager getters — always inlined by banking/e-commerce apps
 * - Build field accessors — always inlined by any app reading device info at startup
 *
 * Cost: deoptimize() forces recompilation of the method. One-time cost at hook setup.
 * The app may briefly be slower to start but all subsequent calls go through the hook.
 */
object DeoptimizeManager {

    /**
     * Deoptimizes a method and all provided caller methods.
     * Callers are found using DexParser or known hot paths.
     */
    fun deoptimizeWithCallers(
        xi: XposedInterface,
        target: Method,
        callers: List<Method> = emptyList(),
    ) {
        val deoptResult = runCatching { xi.deoptimize(target) }.getOrElse { false }
        if (!deoptResult) {
            Log.w("DeoptimizeManager", "Failed to deoptimize ${target.declaringClass.simpleName}.${target.name}")
        }

        for (caller in callers) {
            runCatching { xi.deoptimize(caller) }
        }
    }

    /**
     * Batch-deoptimizes all methods in a list — useful after hooking an entire class's methods.
     */
    fun deoptimizeAll(xi: XposedInterface, methods: List<Method>) {
        for (method in methods) {
            runCatching { xi.deoptimize(method) }
        }
    }
}
```

---

## 10. Phase 5 — DeviceHooker Complete Rewrite

The libxposed API 100 hooker pattern uses static inner classes annotated with `@XposedHooker`. The `@BeforeInvocation` method returns a context object (or `Unit`/`null`) that is passed to `@AfterInvocation`. Context objects allow passing data between before/after.

### Critical Pattern: Static Context Object

Because `@BeforeInvocation` and `@AfterInvocation` methods **must be static**, you cannot capture outer class fields directly. Instead:
1. Store shared data in `companion object` of the hooker class
2. Pass context between before/after via the return value of `@BeforeInvocation`

```kotlin
package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.BeforeHookCallback
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.BeforeInvocation
import io.github.libxposed.api.annotations.XposedHooker
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DeoptimizeManager
import com.astrixforge.devicemasker.xposed.PrefsHelper
import com.astrixforge.devicemasker.xposed.XposedEntry
import com.astrixforge.devicemasker.xposed.utils.ValueGenerators

/**
 * Hooks TelephonyManager for IMEI, IMSI, ICCID, Serial, AndroidID, phone number.
 *
 * Key improvements over the YukiHookAPI version:
 * 1. deoptimize() called after every hook — bypasses ART inlining
 * 2. Individual try-catch per hook — one failure can't cascade
 * 3. Covers createForSubscriptionId() — unhooked instances are now intercepted
 * 4. getLine1Number() is now hooked (was missing)
 * 5. getPhoneCount() is hooked to match device profile (1 SIM for most presets)
 */
object DeviceHooker : BaseSpoofHooker("DeviceHooker") {

    /**
     * Storage for spoofed values, shared across hooker companion objects via thread-local.
     * Thread-local because each thread's app call may be in a different subscription context.
     *
     * These are populated in hook() at module load time from RemotePreferences.
     * They do NOT need to be re-read per-call because RemotePreferences are live.
     */
    object HookState {
        // Package-keyed spoof values loaded once per process
        @Volatile var pkg: String = ""
        @Volatile var prefs: SharedPreferences? = null
        @Volatile var xi: XposedInterface? = null
    }

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        HookState.pkg = pkg
        HookState.prefs = prefs
        HookState.xi = xi

        val tmClass = cl.loadClassOrNull("android.telephony.TelephonyManager") ?: return

        // IMEI hooks — getDeviceId() + getImei() — both signatures
        safeHook("getDeviceId()") {
            tmClass.methodOrNull("getDeviceId")?.let { m ->
                xi.hook(m, GetImeiHooker::class.java)
                DeoptimizeManager.deoptimizeWithCallers(xi, m)
            }
        }
        safeHook("getDeviceId(int)") {
            tmClass.methodOrNull("getDeviceId", Int::class.java)?.let { m ->
                xi.hook(m, GetImeiHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("getImei()") {
            tmClass.methodOrNull("getImei")?.let { m ->
                xi.hook(m, GetImeiHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("getImei(int)") {
            tmClass.methodOrNull("getImei", Int::class.java)?.let { m ->
                xi.hook(m, GetImeiHooker::class.java)
                xi.deoptimize(m)
            }
        }

        // IMSI — getSubscriberId
        safeHook("getSubscriberId()") {
            tmClass.methodOrNull("getSubscriberId")?.let { m ->
                xi.hook(m, GetImsiHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("getSubscriberId(int)") {
            tmClass.methodOrNull("getSubscriberId", Int::class.java)?.let { m ->
                xi.hook(m, GetImsiHooker::class.java)
                xi.deoptimize(m)
            }
        }

        // ICCID — getSimSerialNumber
        safeHook("getSimSerialNumber()") {
            tmClass.methodOrNull("getSimSerialNumber")?.let { m ->
                xi.hook(m, GetIccidHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("getSimSerialNumber(int)") {
            tmClass.methodOrNull("getSimSerialNumber", Int::class.java)?.let { m ->
                xi.hook(m, GetIccidHooker::class.java)
                xi.deoptimize(m)
            }
        }

        // Phone number — getLine1Number (was MISSING in original)
        safeHook("getLine1Number()") {
            tmClass.methodOrNull("getLine1Number")?.let { m ->
                xi.hook(m, GetPhoneNumberHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("getLine1Number(String)") {
            tmClass.methodOrNull("getLine1Number", String::class.java)?.let { m ->
                xi.hook(m, GetPhoneNumberHooker::class.java)
                xi.deoptimize(m)
            }
        }

        // Phone count — ensures single-SIM device profile reports 1
        safeHook("getPhoneCount()") {
            tmClass.methodOrNull("getPhoneCount")?.let { m ->
                xi.hook(m, GetPhoneCountHooker::class.java)
                xi.deoptimize(m)
            }
        }

        // SIM Country ISO
        safeHook("getSimCountryIso()") {
            tmClass.methodOrNull("getSimCountryIso")?.let { m ->
                xi.hook(m, GetSimCountryIsoHooker::class.java)
                xi.deoptimize(m)
            }
        }

        // Network type — CRITICAL for carrier correlation
        safeHook("getNetworkType()") {
            tmClass.methodOrNull("getNetworkType")?.let { m ->
                xi.hook(m, GetNetworkTypeHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("getDataNetworkType()") {
            tmClass.methodOrNull("getDataNetworkType")?.let { m ->
                xi.hook(m, GetNetworkTypeHooker::class.java)
                xi.deoptimize(m)
            }
        }

        // Serial — Build.getSerial() (API 26+, replaces Build.SERIAL)
        safeHook("Build.getSerial()") {
            cl.loadClassOrNull("android.os.Build")
                ?.methodOrNull("getSerial")
                ?.let { m ->
                    xi.hook(m, GetSerialHooker::class.java)
                    xi.deoptimize(m)
                }
        }

        // AndroidID via Settings.Secure
        safeHook("Settings.Secure.getString(androidId)") {
            cl.loadClassOrNull("android.provider.Settings\$Secure")
                ?.methodOrNull("getString", android.content.ContentResolver::class.java, String::class.java)
                ?.let { m ->
                    xi.hook(m, GetAndroidIdHooker::class.java)
                    xi.deoptimize(m)
                }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // HOOKER CLASSES — Static callbacks, no instance state
    // ═══════════════════════════════════════════════════════════

    @XposedHooker
    class GetImeiHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun before(callback: BeforeHookCallback) {
                val (prefs, pkg) = getPrefsAndPkg() ?: return
                val spoofed = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.IMEI) {
                    ValueGenerators.imeiForPackage(pkg)
                }
                callback.returnAndSkip(spoofed)
            }
        }
    }

    @XposedHooker
    class GetImsiHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun before(callback: BeforeHookCallback) {
                val (prefs, pkg) = getPrefsAndPkg() ?: return
                val spoofed = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.IMSI) {
                    ValueGenerators.imsi()
                }
                callback.returnAndSkip(spoofed)
            }
        }
    }

    @XposedHooker
    class GetIccidHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @AfterInvocation
            fun after(callback: AfterHookCallback) {
                val (prefs, pkg) = getPrefsAndPkg() ?: return
                val spoofed = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.ICCID) { return }
                callback.result = spoofed
            }
        }
    }

    @XposedHooker
    class GetPhoneNumberHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun before(callback: BeforeHookCallback) {
                val (prefs, pkg) = getPrefsAndPkg() ?: return
                val spoofed = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.PHONE_NUMBER) {
                    return  // Don't spoof if not configured — avoids null-related crashes
                }
                callback.returnAndSkip(spoofed)
            }
        }
    }

    @XposedHooker
    class GetPhoneCountHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @AfterInvocation
            fun after(callback: AfterHookCallback) {
                // Most device presets are single-SIM — return 1 unless dual-SIM preset
                // Future: read from DeviceProfilePreset.simCount
                callback.result = 1
            }
        }
    }

    @XposedHooker
    class GetSimCountryIsoHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @AfterInvocation
            fun after(callback: AfterHookCallback) {
                val (prefs, pkg) = getPrefsAndPkg() ?: return
                val spoofed = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.SIM_COUNTRY_ISO) {
                    return
                }
                callback.result = spoofed.lowercase()
            }
        }
    }

    @XposedHooker
    class GetNetworkTypeHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @AfterInvocation
            fun after(callback: AfterHookCallback) {
                val (prefs, pkg) = getPrefsAndPkg() ?: return
                // Get the spoofed MCC/MNC to determine appropriate network type
                val mccMnc = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC) {
                    return  // No carrier spoofed — don't change network type
                }
                // Map carrier to appropriate network type (LTE=13, NR=20)
                val networkType = NetworkTypeMapper.getForMccMnc(mccMnc)
                callback.result = networkType
            }
        }
    }

    @XposedHooker
    class GetSerialHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @BeforeInvocation
            fun before(callback: BeforeHookCallback) {
                val (prefs, pkg) = getPrefsAndPkg() ?: return
                val spoofed = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.SERIAL) {
                    ValueGenerators.serial()
                }
                callback.returnAndSkip(spoofed)
            }
        }
    }

    @XposedHooker
    class GetAndroidIdHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @AfterInvocation
            fun after(callback: AfterHookCallback) {
                val key = callback.args?.getOrNull(1) as? String ?: return
                if (key != "android_id") return
                val (prefs, pkg) = getPrefsAndPkg() ?: return
                val spoofed = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.ANDROID_ID) {
                    return
                }
                callback.result = spoofed
            }
        }
    }

    private fun getPrefsAndPkg(): Pair<SharedPreferences, String>? {
        val prefs = HookState.prefs ?: return null
        val pkg = HookState.pkg.takeIf { it.isNotEmpty() } ?: return null
        return prefs to pkg
    }
}
```

---

## 11. Phase 6 — NetworkHooker Completion

Add the missing hooks alongside existing ones:

```kotlin
// ADD to NetworkHooker.hook():

// WifiInfo.getBSSID() — not in original
safeHook("WifiInfo.getBSSID") {
    cl.loadClassOrNull("android.net.wifi.WifiInfo")
        ?.methodOrNull("getBSSID")
        ?.let { m ->
            xi.hook(m, GetBssidHooker::class.java)
            xi.deoptimize(m)
        }
}

// ConnectivityManager.getActiveNetworkInfo() — leaks network type
safeHook("ConnectivityManager.getActiveNetworkInfo") {
    cl.loadClassOrNull("android.net.ConnectivityManager")
        ?.methodOrNull("getActiveNetworkInfo")
        ?.let { m -> xi.hook(m, NetworkInfoHooker::class.java) }
}

// NetworkInterface.getHardwareAddress() — MAC via java.net (more thorough)
safeHook("NetworkInterface.getHardwareAddress") {
    cl.loadClassOrNull("java.net.NetworkInterface")
        ?.methodOrNull("getHardwareAddress")
        ?.let { m ->
            xi.hook(m, MacAddressHooker::class.java)
            xi.deoptimize(m)
        }
}

// WifiManager.getConnectionInfo() — returns WifiInfo with SSID/BSSID/MAC
safeHook("WifiManager.getConnectionInfo") {
    cl.loadClassOrNull("android.net.wifi.WifiManager")
        ?.methodOrNull("getConnectionInfo")
        ?.let { m -> xi.hook(m, WifiConnectionInfoHooker::class.java) }
}
```

---

## 12. Phase 7 — NEW SubscriptionHooker

This is the single most impactful missing hooker. Create `SubscriptionHooker.kt`:

```kotlin
package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.XposedHooker
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.PrefsHelper
import com.astrixforge.devicemasker.xposed.XposedEntry

/**
 * Hooks SubscriptionManager and SubscriptionInfo to spoof SIM card data.
 *
 * Apps that bypass TelephonyManager use SubscriptionManager directly:
 *   SubscriptionManager.getActiveSubscriptionInfoList()
 *     → List<SubscriptionInfo>
 *       → .getIccId()         — leaks real ICCID
 *       → .getNumber()        — leaks real phone number
 *       → .getDisplayName()   — leaks real carrier name
 *       → .getMcc() / .getMnc() — leaks real MCC/MNC
 *       → .getCountryIso()    — leaks real country
 *
 * Strategy: Hook each getter on SubscriptionInfo to return spoofed values.
 * Also hook getActiveSubscriptionInfoCount() to always report 1 (single SIM profile).
 * Also hook getDefaultDataSubscriptionId() / getDefaultSubscriptionId() — these
 * return a subscription ID that apps then use to create per-subscription TelephonyManager
 * instances via createForSubscriptionId(). By returning a consistent fake sub ID, we
 * ensure those derived TM instances also go through our hooks.
 */
object SubscriptionHooker : BaseSpoofHooker("SubscriptionHooker") {

    object HookState {
        @Volatile var pkg: String = ""
        @Volatile var prefs: SharedPreferences? = null
    }

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        HookState.pkg = pkg
        HookState.prefs = prefs

        val smClass = cl.loadClassOrNull("android.telephony.SubscriptionManager") ?: return
        val siClass = cl.loadClassOrNull("android.telephony.SubscriptionInfo")

        // Number of active SIMs — report 1 (matches single-SIM device profiles)
        safeHook("getActiveSubscriptionInfoCount") {
            smClass.methodOrNull("getActiveSubscriptionInfoCount")?.let { m ->
                xi.hook(m, SubCountHooker::class.java)
                xi.deoptimize(m)
            }
        }

        // If SubscriptionInfo class exists, hook all its getters
        if (siClass != null) {
            safeHook("SubscriptionInfo.getIccId") {
                siClass.methodOrNull("getIccId")?.let { m ->
                    xi.hook(m, SubIccIdHooker::class.java)
                    xi.deoptimize(m)
                }
            }
            safeHook("SubscriptionInfo.getNumber") {
                siClass.methodOrNull("getNumber")?.let { m ->
                    xi.hook(m, SubNumberHooker::class.java)
                    xi.deoptimize(m)
                }
            }
            safeHook("SubscriptionInfo.getDisplayName") {
                siClass.methodOrNull("getDisplayName")?.let { m ->
                    xi.hook(m, SubDisplayNameHooker::class.java)
                    xi.deoptimize(m)
                }
            }
            safeHook("SubscriptionInfo.getCountryIso") {
                siClass.methodOrNull("getCountryIso")?.let { m ->
                    xi.hook(m, SubCountryIsoHooker::class.java)
                    xi.deoptimize(m)
                }
            }
        }
    }

    @XposedHooker
    class SubCountHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic @AfterInvocation
            fun after(callback: AfterHookCallback) {
                callback.result = 1  // Always report single SIM
            }
        }
    }

    @XposedHooker
    class SubIccIdHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic @AfterInvocation
            fun after(callback: AfterHookCallback) {
                val (prefs, pkg) = getPrefsAndPkg() ?: return
                callback.result = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.ICCID) { return }
            }
        }
    }

    @XposedHooker
    class SubNumberHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic @AfterInvocation
            fun after(callback: AfterHookCallback) {
                val (prefs, pkg) = getPrefsAndPkg() ?: return
                callback.result = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.PHONE_NUMBER) { return }
            }
        }
    }

    @XposedHooker
    class SubDisplayNameHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic @AfterInvocation
            fun after(callback: AfterHookCallback) {
                val (prefs, pkg) = getPrefsAndPkg() ?: return
                callback.result = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.CARRIER_NAME) { return }
            }
        }
    }

    @XposedHooker
    class SubCountryIsoHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic @AfterInvocation
            fun after(callback: AfterHookCallback) {
                val (prefs, pkg) = getPrefsAndPkg() ?: return
                callback.result = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.SIM_COUNTRY_ISO) { return }
                    ?.lowercase()
            }
        }
    }

    private fun getPrefsAndPkg(): Pair<SharedPreferences, String>? {
        val prefs = HookState.prefs ?: return null
        val pkg = HookState.pkg.takeIf { it.isNotEmpty() } ?: return null
        return prefs to pkg
    }
}
```

---

## 13. Phase 8 — SystemHooker Hardening

### 13.1 Hook `Build` Class Initializer Instead of Direct Mutation

The critical bug in the current `SystemHooker` is mutating `Build` static fields directly. This races with ART's AOT compilation. The fix is to hook the static initializer (`<clinit>`) of `Build`, which fires exactly once when the class is first loaded:

```kotlin
// NEW approach in SystemHooker.hook():

val buildClass = cl.loadClassOrNull("android.os.Build") ?: return
val preset = getActivePreset(prefs, pkg) ?: return

// Hook class initializer — fires before ANY code reads Build fields
safeHook("Build.<clinit>") {
    xi.hookClassInitializer(buildClass, BuildClassInitHooker::class.java)
}
// Note: if Build is already initialized (it is in most cases), fall back to direct field set
// but do it inside an AfterInvocation hook on the first method that reads Build fields
// The safest fallback: hook Build.getString() or any accessor used by the app
```

### 13.2 New `Build` Fields to Cover

Add to `DeviceProfilePreset` data class in `:common`:

```kotlin
data class DeviceProfilePreset(
    val id: String,
    val name: String,
    // Existing fields
    val fingerprint: String,
    val model: String,
    val manufacturer: String,
    val brand: String,
    val device: String,
    val product: String,
    val board: String,
    // NEW fields
    val buildTime: Long,          // epoch millis — e.g., 1700000000000L for Nov 2023
    val securityPatch: String,    // e.g., "2024-11-01"
    val buildId: String,          // e.g., "AP3A.241005.015"
    val incremental: String,      // e.g., "AP3A.241005.015.A2"
    val supportedAbis: List<String>, // e.g., ["arm64-v8a", "armeabi-v7a", "armeabi"]
    val tacPrefixes: List<String>,   // e.g., ["35414610", "35414611"] for IMEI generation
    val simCount: Int = 1,
    val hasNfc: Boolean = true,
    val has5G: Boolean = true,
)
```

Then in `SystemHooker`:
```kotlin
// Hook Build.TIME
safeHook("Build.TIME") {
    buildClass.getDeclaredField("TIME").apply {
        isAccessible = true
        set(null, preset.buildTime)
    }
}

// Hook Build.ID
safeHook("Build.ID") {
    buildClass.getDeclaredField("ID").apply {
        isAccessible = true
        set(null, preset.buildId)
    }
}

// Hook Build.TAGS  
safeHook("Build.TAGS") {
    buildClass.getDeclaredField("TAGS").apply {
        isAccessible = true
        set(null, "release-keys")
    }
}

// Hook Build.TYPE
safeHook("Build.TYPE") {
    buildClass.getDeclaredField("TYPE").apply {
        isAccessible = true
        set(null, "user")
    }
}

// Hook Build.VERSION.SECURITY_PATCH
safeHook("Build.VERSION.SECURITY_PATCH") {
    val versionClass = cl.loadClassOrNull("android.os.Build\$VERSION") ?: return@safeHook
    versionClass.getDeclaredField("SECURITY_PATCH").apply {
        isAccessible = true
        set(null, preset.securityPatch)
    }
}

// Hook Build.SUPPORTED_ABIS
safeHook("Build.SUPPORTED_ABIS") {
    buildClass.getDeclaredField("SUPPORTED_ABIS").apply {
        isAccessible = true
        set(null, preset.supportedAbis.toTypedArray())
    }
}
```

---

## 14. Phase 9 — WebViewHooker Completion

### 14.1 Missing Hooks

```kotlin
// ADD to WebViewHooker.hook():

// WebView.getDefaultUserAgent(Context) — STATIC method — was completely missing
safeHook("WebView.getDefaultUserAgent") {
    cl.loadClassOrNull("android.webkit.WebView")
        ?.methodOrNull("getDefaultUserAgent", android.content.Context::class.java)
        ?.let { m ->
            xi.hook(m, GetDefaultUserAgentHooker::class.java)
            xi.deoptimize(m)
        }
}

// System.getProperty("http.agent") — used by OkHttp and HttpURLConnection
safeHook("System.getProperty(http.agent)") {
    cl.loadClassOrNull("java.lang.System")
        ?.methodOrNull("getProperty", String::class.java)
        ?.let { m ->
            xi.hook(m, SystemPropertyHooker::class.java)
            xi.deoptimize(m)
        }
}

// WebViewDatabase.getInstance() exists hooks — WebView fingerprinting via storage
// (lower priority, add in future iteration)
```

### 14.2 `GetDefaultUserAgentHooker`

```kotlin
@XposedHooker
class GetDefaultUserAgentHooker : XposedInterface.Hooker {
    companion object {
        @JvmStatic
        @AfterInvocation
        fun after(callback: AfterHookCallback) {
            val preset = WebViewHooker.getPreset() ?: return
            val original = callback.result as? String ?: return
            callback.result = modifyUserAgent(original, preset.model)
        }

        private fun modifyUserAgent(ua: String, model: String): String {
            val regex = Regex("""\(Linux; Android (\d+(?:\.\d+)?); ([^)]*)\)""")
            return regex.replace(ua) { match ->
                val version = match.groupValues[1]
                "(Linux; Android $version; $model)"
            }
        }
    }
}

@XposedHooker
class SystemPropertyHooker : XposedInterface.Hooker {
    companion object {
        @JvmStatic
        @AfterInvocation
        fun after(callback: AfterHookCallback) {
            val key = callback.args?.getOrNull(0) as? String ?: return
            if (key != "http.agent") return
            val preset = WebViewHooker.getPreset() ?: return
            val original = callback.result as? String ?: return
            // Replace device model in the http.agent system property
            callback.result = original.replace(
                Regex("""\(Linux;[^)]+\)"""),
                "(Linux; Android ${android.os.Build.VERSION.RELEASE}; ${preset.model})"
            )
        }
    }
}
```

---

## 15. Phase 10 — NEW PackageManagerHooker

```kotlin
package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.XposedHooker
import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.PrefsHelper

/**
 * Hooks PackageManager.hasSystemFeature() to match the spoofed device profile.
 *
 * Why this matters:
 * - A "Pixel 9 Pro" should have NFC, 5G, multiple cameras, etc.
 * - A "Xiaomi Redmi Note 9" might not have NFC
 * - Fraud detection SDKs cross-reference hasSystemFeature() with the spoofed model
 * - The OS/fingerprint says one thing, hasSystemFeature says another = detectable mismatch
 *
 * Strategy: For features that are in the device preset's feature set → return true.
 *           For features that are NOT in the preset → return false.
 *           For features not relevant to device identity → pass through real value.
 */
object PackageManagerHooker : BaseSpoofHooker("PackageManagerHooker") {

    object HookState {
        @Volatile var pkg: String = ""
        @Volatile var prefs: SharedPreferences? = null
    }

    // Features relevant to device identity fingerprinting
    private val IDENTITY_FEATURES = setOf(
        "android.hardware.nfc",
        "android.hardware.nfc.hce",
        "android.hardware.telephony",
        "android.hardware.telephony.ims",
        "android.hardware.sensor.accelerometer",
        "android.hardware.sensor.gyroscope",
        "android.hardware.sensor.barometer",
        "android.hardware.sensor.heartrate",
        "android.hardware.camera.flash",
        "android.hardware.camera.front",
        "android.hardware.wifi",
        "android.hardware.bluetooth_le",
        "android.hardware.usb.accessory",
        "android.hardware.se.omapi.uicc",  // eSIM
    )

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        HookState.pkg = pkg
        HookState.prefs = prefs

        // Hook ApplicationPackageManager.hasSystemFeature(String)
        // and hasSystemFeature(String, int) — the version-check overload
        val pmClass = cl.loadClassOrNull("android.app.ApplicationPackageManager")
            ?: cl.loadClassOrNull("android.content.pm.PackageManager")
            ?: return

        safeHook("hasSystemFeature(String)") {
            pmClass.methodOrNull("hasSystemFeature", String::class.java)?.let { m ->
                xi.hook(m, HasSystemFeatureHooker::class.java)
            }
        }
        safeHook("hasSystemFeature(String,int)") {
            pmClass.methodOrNull("hasSystemFeature", String::class.java, Int::class.java)?.let { m ->
                xi.hook(m, HasSystemFeatureHooker::class.java)
            }
        }
    }

    @XposedHooker
    class HasSystemFeatureHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            @AfterInvocation
            fun after(callback: AfterHookCallback) {
                val featureName = callback.args?.getOrNull(0) as? String ?: return
                if (featureName !in IDENTITY_FEATURES) return  // Only intercept identity features

                val prefs = HookState.prefs ?: return
                val pkg = HookState.pkg.takeIf { it.isNotEmpty() } ?: return

                val presetId = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.DEVICE_PROFILE) {
                    return  // No device profile configured — pass through real value
                }
                val preset = DeviceProfilePreset.findById(presetId) ?: return

                // Determine if the feature should exist on this device profile
                callback.result = featureExistsOnPreset(preset, featureName)
            }

            private fun featureExistsOnPreset(preset: DeviceProfilePreset, feature: String): Boolean {
                return when (feature) {
                    "android.hardware.nfc",
                    "android.hardware.nfc.hce" -> preset.hasNfc
                    "android.hardware.telephony",
                    "android.hardware.telephony.ims" -> true  // All phone presets have telephony
                    "android.hardware.sensor.accelerometer",
                    "android.hardware.sensor.gyroscope",
                    "android.hardware.camera.flash",
                    "android.hardware.camera.front",
                    "android.hardware.wifi",
                    "android.hardware.bluetooth_le" -> true  // All modern presets have these
                    "android.hardware.se.omapi.uicc" -> preset.has5G  // eSIM correlates with 5G
                    else -> true
                }
            }
        }
    }
}
```

---

## 16. Phase 11 — AntiDetectHooker Hardening

### 16.1 Add Missing Detection Vectors

```kotlin
// ADD to AntiDetectHooker.hook():

// ClassLoader.loadClass() — bypasses Class.forName() hook
safeHook("ClassLoader.loadClass") {
    cl.loadClassOrNull("java.lang.ClassLoader")
        ?.methodOrNull("loadClass", String::class.java)
        ?.let { m -> xi.hook(m, LoadClassHooker::class.java) }
}

// Runtime.exec() — apps may exec shell commands to check for /system/lib/libart-xposed.so
safeHook("Runtime.exec(String)") {
    cl.loadClassOrNull("java.lang.Runtime")
        ?.methodOrNull("exec", String::class.java)
        ?.let { m -> xi.hook(m, RuntimeExecHooker::class.java) }
}

// ActivityManager.getRunningServices() — leaks module processes
safeHook("ActivityManager.getRunningServices") {
    cl.loadClassOrNull("android.app.ActivityManager")
        ?.methodOrNull("getRunningServices", Int::class.java)
        ?.let { m -> xi.hook(m, RunningServicesHooker::class.java) }
}

// Process.myPid() alternative — some apps read /proc/self/maps directly
// Cannot hook file reads, but we can deoptimize common readers
// This is handled by the /proc/maps interception (existing hook, verify it covers smaps too)
```

### 16.2 `LoadClassHooker`

```kotlin
@XposedHooker
class LoadClassHooker : XposedInterface.Hooker {
    companion object {
        private val XPOSED_CLASSES = setOf(
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XposedHelpers",
            "de.robv.android.xposed.XC_MethodHook",
            "io.github.libxposed.api.XposedInterface",
            "io.github.libxposed.api.XposedModule",
            "com.highcapable.yukihookapi.YukiHookAPI",
        )

        @JvmStatic
        @BeforeInvocation
        fun before(callback: BeforeHookCallback) {
            val name = callback.args?.getOrNull(0) as? String ?: return
            if (name in XPOSED_CLASSES) {
                callback.throwable = ClassNotFoundException("$name not found")
            }
        }
    }
}
```

---

## 17. Phase 12 — TAC-Aware IMEI Generator

This is a `:common` module change in `ValueGenerators.kt`:

```kotlin
/**
 * TAC prefixes for known device models.
 * First 8 digits of IMEI. Source: Osmocom TAC DB / GSMA allocations.
 * Each list contains multiple valid TAC values for the same model (different regions/batches).
 */
private val DEVICE_TAC_PREFIXES: Map<String, List<String>> = mapOf(
    // Google Pixel 9 Pro
    "pixel_9_pro"      to listOf("35414610", "35414611", "35414612"),
    // Google Pixel 8 Pro  
    "pixel_8_pro"      to listOf("35173210", "35173211", "35173212"),
    // Google Pixel 8
    "pixel_8"          to listOf("35173310", "35173311"),
    // Samsung Galaxy S24 Ultra
    "samsung_s24_ultra" to listOf("35326014", "35326015", "35921714"),
    // Samsung Galaxy S24
    "samsung_s24"      to listOf("35921814", "35921815"),
    // OnePlus 12
    "oneplus_12"        to listOf("86843204", "86843205"),
    // Xiaomi 14 Pro
    "xiaomi_14_pro"    to listOf("86907504", "86907505"),
    // Generic fallback for unknown presets
    "generic"          to listOf("35000000", "35000001"),
)

/**
 * Generates a Luhn-valid IMEI matching the device profile's TAC range.
 *
 * @param presetId The DeviceProfilePreset ID
 * @return 15-digit Luhn-valid IMEI string
 */
fun imeiForPreset(presetId: String): String {
    val tacPrefixes = DEVICE_TAC_PREFIXES[presetId] ?: DEVICE_TAC_PREFIXES["generic"]!!
    val tac = tacPrefixes.random()  // Pick a random valid TAC for this model
    return generateImeiWithTac(tac)
}

/**
 * Generates a Luhn-valid IMEI given an 8-digit TAC prefix.
 */
private fun generateImeiWithTac(tac: String): String {
    require(tac.length == 8) { "TAC must be 8 digits" }
    val snr = (0..999999).random().toString().padStart(6, '0')
    val partial = tac + snr
    val checkDigit = luhnCheckDigit(partial)
    return partial + checkDigit
}

private fun luhnCheckDigit(number: String): Int {
    var sum = 0
    var alternate = true
    for (i in number.length - 1 downTo 0) {
        var n = number[i].digitToInt()
        if (alternate) {
            n *= 2
            if (n > 9) n -= 9
        }
        sum += n
        alternate = !alternate
    }
    return (10 - (sum % 10)) % 10
}
```

---

## 18. Phase 13 — DeviceProfilePreset Enrichment

Update all existing presets in `:common/models/DeviceProfilePreset.kt` with the new fields:

```kotlin
// Example: Pixel 9 Pro preset (expanded)
val PIXEL_9_PRO = DeviceProfilePreset(
    id = "pixel_9_pro",
    name = "Google Pixel 9 Pro",
    fingerprint = "google/caiman/caiman:15/AP3A.241005.015/12393350:user/release-keys",
    model = "Pixel 9 Pro",
    manufacturer = "Google",
    brand = "google",
    device = "caiman",
    product = "caiman",
    board = "caiman",
    // NEW enriched fields:
    buildTime = 1728100000000L,          // Oct 2024
    securityPatch = "2024-10-05",
    buildId = "AP3A.241005.015",
    incremental = "12393350",
    supportedAbis = listOf("arm64-v8a", "armeabi-v7a", "armeabi"),
    tacPrefixes = listOf("35414610", "35414611", "35414612"),
    simCount = 1,                         // Single physical SIM + eSIM
    hasNfc = true,
    has5G = true,
)

// Samsung Galaxy S24 Ultra
val SAMSUNG_S24_ULTRA = DeviceProfilePreset(
    id = "samsung_s24_ultra",
    name = "Samsung Galaxy S24 Ultra",
    fingerprint = "samsung/samsungexynos9820/beyondlte:10/QP1A.190711.020/G975FXXS7GUF1:user/release-keys",
    model = "SM-S928B",
    manufacturer = "samsung",
    brand = "samsung",
    device = "e3q",
    product = "e3qxxx",
    board = "e3q",
    buildTime = 1710000000000L,          // Mar 2024
    securityPatch = "2024-03-01",
    buildId = "UP1A.231005.007.S928BXXS5AXH2",
    incremental = "S928BXXS5AXH2",
    supportedAbis = listOf("arm64-v8a", "armeabi-v7a", "armeabi"),
    tacPrefixes = listOf("35326014", "35326015"),
    simCount = 2,                        // Dual SIM variant (adjust getPhoneCount)
    hasNfc = true,
    has5G = true,
)
```

---

## 19. Phase 14 — Full Correlation Matrix

This table defines the complete set of correlated values that MUST be consistent:

| Source: Carrier (SIM_CARD group) | Derived values | Hook Location |
|---|---|---|
| `CARRIER_MCC_MNC` | `NETWORK_TYPE` (LTE/NR per region) | DeviceHooker.GetNetworkTypeHooker |
| `CARRIER_MCC_MNC` | `SIM_COUNTRY_ISO`, `NETWORK_COUNTRY_ISO` | DeviceHooker, SubscriptionHooker |
| `CARRIER_MCC_MNC` | `TIMEZONE` range (must match country) | LocationHooker |
| `CARRIER_MCC_MNC` | `LOCALE` language (must match country) | LocationHooker |
| `CARRIER_MCC_MNC` | GPS lat/lon (must be in carrier country) | LocationHooker |
| `IMSI` prefix | Must start with same MCC+MNC as CARRIER | ValueGenerators.imsi() |
| `ICCID` prefix | Country code in digits 4-5 must match | ValueGenerators.iccid() |
| `PHONE_NUMBER` | Country calling code must match carrier | ValueGenerators.phoneNumber() |

| Source: Device Profile | Derived values | Hook Location |
|---|---|---|
| `DEVICE_PROFILE` | `IMEI` TAC prefix | DeviceHooker (via imeiForPreset()) |
| `DEVICE_PROFILE` | `SERIAL` format | DeviceHooker (brand-specific format) |
| `DEVICE_PROFILE` | `Build.*` all fields | SystemHooker |
| `DEVICE_PROFILE` | `hasSystemFeature(nfc)` | PackageManagerHooker |
| `DEVICE_PROFILE` | `getPhoneCount()` (1 or 2) | DeviceHooker |
| `DEVICE_PROFILE` | `Sensor` list (match device specs) | SensorHooker |
| `DEVICE_PROFILE` | `WebView` User-Agent model | WebViewHooker |
| `DEVICE_PROFILE` | `Build.SUPPORTED_ABIS` | SystemHooker |
| `DEVICE_PROFILE` | `Build.TIME` (build date) | SystemHooker |
| `DEVICE_PROFILE` | `Build.VERSION.SECURITY_PATCH` | SystemHooker |

### 19.1 `NetworkTypeMapper.kt` (New utility)

```kotlin
object NetworkTypeMapper {
    // TelephonyManager.NETWORK_TYPE_* constants
    private const val NETWORK_TYPE_LTE = 13
    private const val NETWORK_TYPE_NR = 20
    private const val NETWORK_TYPE_HSPA = 10

    /**
     * Returns the expected network type for a given carrier's MCC/MNC.
     * In 2025, most major carriers globally support LTE minimum, 5G in urban areas.
     */
    fun getForMccMnc(mccMnc: String): Int {
        return when {
            // US carriers — Verizon, AT&T, T-Mobile — all NR (5G) by 2025
            mccMnc.startsWith("310") || mccMnc.startsWith("311") -> NETWORK_TYPE_NR
            // India — Jio (404-40, 404-50), Airtel (404-10) — NR in urban areas
            mccMnc == "40440" || mccMnc == "40450" || mccMnc == "40410" -> NETWORK_TYPE_NR
            // UK — EE (234-30), O2 (234-10), Vodafone (234-15)
            mccMnc.startsWith("234") -> NETWORK_TYPE_LTE
            // Germany — Telekom (262-01), Vodafone (262-02), O2 (262-07)
            mccMnc.startsWith("262") -> NETWORK_TYPE_LTE
            // Default: LTE (safe for any carrier in 2025)
            else -> NETWORK_TYPE_LTE
        }
    }
}
```

---

## 20. Phase 15 — ProGuard & Manifest

### 20.1 `xposed/consumer-rules.pro`

```pro
# ═══════════════════════════════════════════════════════════
# libxposed API 100 rules
# ═══════════════════════════════════════════════════════════

# Keep entry point (referenced in META-INF/xposed/java_init.list)
-keep class com.astrixforge.devicemasker.xposed.XposedEntry { *; }

# Keep all hooker classes — they are referenced by class literal (MyHooker::class.java)
# R8 would otherwise remove the companion object static methods
-keep class * implements io.github.libxposed.api.XposedInterface$Hooker { *; }

# Keep @XposedHooker annotated classes and their companion static methods
-keep @io.github.libxposed.api.annotations.XposedHooker class * { *; }
-keepclassmembers @io.github.libxposed.api.annotations.XposedHooker class * {
    @io.github.libxposed.api.annotations.BeforeInvocation <methods>;
    @io.github.libxposed.api.annotations.AfterInvocation <methods>;
    public static <methods>;
}

# Keep all hooker package classes
-keep class com.astrixforge.devicemasker.xposed.** { *; }

# Keep the XposedModule base class reference
-keep class io.github.libxposed.api.XposedModule { *; }
-keep class io.github.libxposed.api.XposedInterface { *; }
-keep class io.github.libxposed.api.XposedModuleInterface { *; }
-keep class io.github.libxposed.api.XposedModuleInterface$* { *; }
-keep class io.github.libxposed.api.annotations.** { *; }

# REMOVE old YukiHookAPI keeps:
# -keep class com.highcapable.yukihookapi.** — DELETE
# -keep class * implements de.robv.android.xposed.IXposedHookLoadPackage — DELETE
```

### 20.2 `app/AndroidManifest.xml` Addition

The libxposed service requires a `ContentProvider` for the write-side of `RemotePreferences`. In `:app/AndroidManifest.xml`, add:

```xml
<!-- Required for libxposed RemotePreferences write side -->
<!-- The exact provider authority and class depends on the libxposed-service version -->
<!-- Check libxposed/service README for current declaration -->
<provider
    android:name="io.github.libxposed.service.ModulePreferencesProvider"
    android:authorities="${applicationId}.lspd_prefs"
    android:exported="true"
    android:multiprocess="false" />
```

---

## 21. Phase 16 — RemotePreferences in :app

### 21.1 Replace `XposedPrefs.kt` Write Side

```kotlin
// app/data/XposedPrefs.kt — UPDATED

package com.astrixforge.devicemasker.app.data

import android.content.Context
import io.github.libxposed.service.ModulePreferences  // from libxposed-service.aar

/**
 * Writes spoofing config for the libxposed RemotePreferences system.
 *
 * libxposed-service provides ModulePreferences which:
 * - Writes to LSPosed's database (not MODE_WORLD_READABLE files)
 * - Is instantly available in hooked processes via getRemotePreferences()
 * - Does NOT require target app restart
 * - Is read-only in hooked processes (libxposed enforces this)
 */
object XposedPrefs {

    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        // ModulePreferences.from() returns a SharedPreferences implementation
        // backed by the libxposed service bridge
        prefs = ModulePreferences.from(context, XposedEntry.PREFS_GROUP)
    }

    fun getPrefs(context: Context): android.content.SharedPreferences {
        return prefs ?: ModulePreferences.from(context, XposedEntry.PREFS_GROUP).also { prefs = it }
    }
}
```

### 21.2 Update `ConfigSync.kt`

`ConfigSync.kt` already writes to `XposedPrefs`. The only change needed is calling `XposedPrefs.init(context)` in `Application.onCreate()` before any config sync. No key format changes. No other changes.

---

## 22. Testing Checklist

### After Each Phase — Run This Verification

**Phase 0-1 (Build setup):**
- [ ] `./gradlew :xposed:assembleDebug` compiles without errors
- [ ] APK contains `META-INF/xposed/java_init.list` (check with `unzip -l app.apk`)
- [ ] APK contains `META-INF/xposed/module.prop`
- [ ] No YukiHookAPI classes in DEX (check with `dexdump` or `apkanalyzer`)
- [ ] LSPosed Manager shows module with correct description from `android:description`

**Phase 2-3 (Entry + Prefs):**
- [ ] Module loads without crash in logcat (filter: `DeviceMasker`)
- [ ] `XposedEntry loaded in process:` appears in logs for target apps
- [ ] RemotePreferences readable: enable spoofing for a test app, check logs

**Phase 4 (Deoptimize):**
- [ ] No `deoptimize() failed` warnings in logcat for TelephonyManager methods
- [ ] IMEI hook fires in apps where it previously was silent (test with IMEI checker app)

**Phase 5 (DeviceHooker):**
- [ ] IMEI spoofed: `*#06#` in dialer shows real, but IMEI-checker apps show spoofed
- [ ] `getLine1Number()` spoofed: phone number apps show spoofed number
- [ ] No crashes on Xiaomi/Samsung devices (test `getImei(int)` overload handling)

**Phase 6 (NetworkHooker):**
- [ ] WiFi MAC spoofed: check in `Settings > About phone`
- [ ] BSSID spoofed in apps that read WiFi info

**Phase 7 (SubscriptionHooker):**
- [ ] Apps using `SubscriptionManager.getActiveSubscriptionInfoList()` show spoofed ICCID/number
- [ ] Subscription count reports 1

**Phase 8 (SystemHooker):**
- [ ] `Build.MODEL` spoofed correctly
- [ ] `Build.TIME` updated (no 2018 timestamps)
- [ ] `Build.VERSION.SECURITY_PATCH` matches preset
- [ ] `Build.SUPPORTED_ABIS` matches preset (arm64-v8a for Pixel)

**Phase 9 (WebViewHooker):**
- [ ] WebView UA shows spoofed model (test in browser app)
- [ ] `WebView.getDefaultUserAgent()` returns spoofed model
- [ ] `http.agent` system property shows spoofed model

**Phase 10 (PackageManagerHooker):**
- [ ] `hasSystemFeature("android.hardware.nfc")` returns `preset.hasNfc`
- [ ] Feature set consistent with device model

**Phase 11 (AntiDetect):**
- [ ] `ClassLoader.loadClass("de.robv.android.xposed.XposedBridge")` throws `ClassNotFoundException`
- [ ] RootBeer does not detect hooks
- [ ] No Xposed-related class names in `/proc/self/maps` (check with root file manager)

**Phase 12-13 (TAC + Preset):**
- [ ] Generated IMEI TAC matches device profile (first 8 digits)
- [ ] Luhn validation passes (use online validator)
- [ ] No `86` TAC prefix when profile is a Google/Samsung device

**Phase 14 (Correlation):**
- [ ] IMSI starts with same MCC+MNC as CARRIER_MCC_MNC
- [ ] ICCID country digits match carrier country
- [ ] GPS coordinates are in the correct country for the spoofed carrier
- [ ] Timezone offset is consistent with GPS country
- [ ] Locale language matches carrier country

---

## 23. Complete File Change Index

| File | Action | Module | Notes |
|---|---|---|---|
| `gradle/libs.versions.toml` | Modify | root | Remove yuki/kavaref, add libxposed |
| `xposed/build.gradle.kts` | Modify | `:xposed` | New deps, remove KSP plugin |
| `app/build.gradle.kts` | Modify | `:app` | Remove ksp plugin, add service dep |
| `xposed/src/main/assets/xposed_init` | **DELETE** | `:xposed` | Replaced by java_init.list |
| `xposed/src/main/resources/META-INF/xposed/java_init.list` | **CREATE** | `:xposed` | New entry |
| `xposed/src/main/resources/META-INF/xposed/module.prop` | **CREATE** | `:xposed` | minApiVersion=100 |
| `xposed/src/main/resources/META-INF/xposed/scope.list` | **CREATE** | `:xposed` | Default scope |
| `xposed/src/main/AndroidManifest.xml` | Modify | `:xposed` | Update xposedminversion=100 |
| `app/src/main/AndroidManifest.xml` | Modify | `:app` | Add libxposed-service provider |
| `xposed/.../XposedEntry.kt` | **REWRITE** | `:xposed` | `XposedModule` subclass |
| `xposed/.../BaseSpoofHooker.kt` | **REWRITE** | `:xposed` | Remove YukiBaseHooker |
| `xposed/.../PrefsHelper.kt` | **REWRITE** | `:xposed` | Use RemotePreferences |
| `xposed/.../DeoptimizeManager.kt` | **CREATE** | `:xposed` | New — ART inline bypass |
| `xposed/.../NetworkTypeMapper.kt` | **CREATE** | `:xposed` | New — carrier→network type |
| `xposed/hooker/DeviceHooker.kt` | **REWRITE** | `:xposed` | API 100 hooker pattern |
| `xposed/hooker/NetworkHooker.kt` | Modify | `:xposed` | Add missing hooks |
| `xposed/hooker/SubscriptionHooker.kt` | **CREATE** | `:xposed` | New — biggest gap |
| `xposed/hooker/SystemHooker.kt` | Modify | `:xposed` | Add TIME, ABIS, PATCH |
| `xposed/hooker/WebViewHooker.kt` | Modify | `:xposed` | Add getDefaultUserAgent |
| `xposed/hooker/PackageManagerHooker.kt` | **CREATE** | `:xposed` | New — feature set spoofing |
| `xposed/hooker/AntiDetectHooker.kt` | Modify | `:xposed` | Add loadClass, runningServices |
| `xposed/hooker/LocationHooker.kt` | Modify | `:xposed` | API 100 pattern migration |
| `xposed/hooker/SensorHooker.kt` | Modify | `:xposed` | API 100 pattern migration |
| `xposed/hooker/AdvertisingHooker.kt` | Modify | `:xposed` | API 100 pattern migration |
| `xposed/consumer-rules.pro` | **REWRITE** | `:xposed` | API 100 keep rules |
| `app/data/XposedPrefs.kt` | Modify | `:app` | ModulePreferences write side |
| `common/models/DeviceProfilePreset.kt` | Modify | `:common` | Add enriched fields |
| `common/utils/ValueGenerators.kt` | Modify | `:common` | TAC-aware IMEI generation |
| `common/utils/NetworkTypeMapper.kt` | **CREATE** | `:common` (or xposed) | Carrier→network type |

---

## Key Principles to Never Violate

1. **Never call `toClass()` — always `toClassOrNull()` followed by `?: return`**
2. **Every hook registration in its own `try-catch` — never group multiple hooks**
3. **Always call `deoptimize()` after `hook()` for every TelephonyManager/Build getter**
4. **AntiDetectHooker MUST be registered before any spoofing hook in `onPackageLoaded()`**
5. **System server hooks (`onSystemServerLoaded`) need a `try-catch` around the entire body — crash = bootloop**
6. **Hook callbacks (`@BeforeInvocation`, `@AfterInvocation`) must never throw — wrap in runCatching if needed**
7. **RemotePreferences are read-only in hooks — never attempt to write from hook callbacks**
8. **All spoofed values must be self-consistent per the correlation matrix — one wrong value breaks the entire illusion**
9. **TAC prefix must match the device model preset — random TAC generation is detectable**
10. **`getPhoneCount()` must match `simCount` in the device preset — dual-SIM device with phoneCount=1 is suspicious**

---

*End of Plan — Device Masker libxposed API 100 Migration*
