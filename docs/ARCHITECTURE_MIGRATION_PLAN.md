# Device Masker Architecture Migration Plan

## System-Wide AIDL Service Architecture (HMA-OSS Style)

**Document Version**: 1.0  
**Created**: 2026-01-20  
**Status**: PLANNING  
**Estimated Duration**: 2-3 weeks

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current Architecture](#2-current-architecture)
3. [Target Architecture](#3-target-architecture)
4. [Benefits Analysis](#4-benefits-analysis)
5. [Risk Assessment](#5-risk-assessment)
6. [Detailed Implementation Plan](#6-detailed-implementation-plan)
7. [File Changes](#7-file-changes)
8. [Testing Strategy](#8-testing-strategy)
9. [Rollback Plan](#9-rollback-plan)
10. [Timeline](#10-timeline)
11. [Success Criteria](#11-success-criteria)

---

## 1. Executive Summary

### 1.1 Objective

Migrate Device Masker from a **per-app hook loading architecture** using XSharedPreferences to a **system-wide framework hooking architecture** using AIDL service running in `system_server`, modeled after HMA-OSS.

### 1.2 Key Changes

| Component | Current | Target |
|-----------|---------|--------|
| LSPosed Scope | Multiple apps | Single `android` package |
| Hook Location | Each app process | `system_server` + app processes |
| Config Delivery | XSharedPreferences (cached) | AIDL Binder (real-time) |
| Config Updates | Requires app restart | Instant via binder |
| Service Model | Stateless | Persistent singleton |

### 1.3 Expected Outcomes

- ✅ Real-time configuration updates without app restart
- ✅ Centralized logging and diagnostics
- ✅ Simplified LSPosed configuration (single scope)
- ✅ Better performance (hooks loaded once)
- ✅ Richer IPC capabilities (stats, force-stop, health monitoring)

---

## 2. Current Architecture

### 2.1 Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CURRENT ARCHITECTURE                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────────┐     XSharedPrefs      ┌─────────────────────────┐  │
│  │   App UI    │ ──────────────────────▶│   shared_prefs/*.xml   │  │
│  │  (Compose)  │       (write)          │    (MODE_WORLD_READ)   │  │
│  └─────────────┘                        └───────────┬─────────────┘  │
│                                                     │                 │
│                                                     │ (read, cached)  │
│                                                     ▼                 │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                     LSPosed Framework                            │ │
│  │  ┌─────────────────────────────────────────────────────────────┐│ │
│  │  │                    Per-App Hook Loading                     ││ │
│  │  │                                                             ││ │
│  │  │   loadApp(com.example.app1) → XposedEntry.onHook()          ││ │
│  │  │   loadApp(com.example.app2) → XposedEntry.onHook()          ││ │
│  │  │   loadApp(com.example.app3) → XposedEntry.onHook()          ││ │
│  │  │   ...                                                        ││ │
│  │  └─────────────────────────────────────────────────────────────┘│ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                       │
│  Each app process contains:                                           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐    │
│  │ PrefsHelper │ │DeviceHooker│ │NetworkHooker│ │  ...Hookers │    │
│  │ (reads xml) │ │            │ │             │ │             │    │
│  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────┘    │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 Data Flow

1. **UI** writes config to `shared_prefs/device_masker_prefs.xml` with `MODE_WORLD_READABLE`
2. **XSharedPreferences** reads these values (cached at first read)
3. **PrefsHelper** provides typed access to config values
4. **Each Hooker** queries PrefsHelper for spoof values
5. **Config changes require target app restart** (XSharedPreferences caching)

### 2.3 Key Files

| File | Purpose |
|------|---------|
| `xposed/XposedEntry.kt` | Entry point, loads hookers per-app |
| `xposed/PrefsHelper.kt` | XSharedPreferences wrapper |
| `xposed/hooker/BaseSpoofHooker.kt` | Base class for all hookers |
| `xposed/hooker/*Hooker.kt` | 8 individual hookers |
| `app/XposedPrefs.kt` | UI-side prefs writer |
| `common/SharedPrefsKeys.kt` | Key constants (single source of truth) |

### 2.4 Limitations

1. **No real-time updates**: XSharedPreferences caches values at first read
2. **Per-app overhead**: Hooks load separately in each app process
3. **Limited IPC**: Can only read preferences, cannot call methods
4. **No centralized logging**: Each app logs independently
5. **Complex scope management**: Users must manually select apps in LSPosed

---

## 3. Target Architecture

### 3.1 Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        TARGET ARCHITECTURE                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────────┐      AIDL Binder      ┌─────────────────────────┐  │
│  │   App UI    │ ◀────────────────────▶│  DeviceMaskerService    │  │
│  │  (Compose)  │    (real-time IPC)    │  (in system_server)     │  │
│  └─────────────┘                        └───────────┬─────────────┘  │
│        │                                            │                 │
│        │ ContentProvider                            │ Manages         │
│        │ "getService"                               │                 │
│        ▼                                            ▼                 │
│  ┌─────────────┐                        ┌─────────────────────────┐  │
│  │ServiceClient│                        │  /data/misc/devicemasker│  │
│  │  (binder)   │                        │  └── config.json        │  │
│  └─────────────┘                        └─────────────────────────┘  │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                     LSPosed Framework                            │ │
│  │  ┌─────────────────────────────────────────────────────────────┐│ │
│  │  │              System Framework Hook (android)                 ││ │
│  │  │                                                             ││ │
│  │  │   loadSystem() → SystemServiceHooker                        ││ │
│  │  │                  └── Registers DeviceMaskerService          ││ │
│  │  │                                                             ││ │
│  │  │   loadApp(*) → Hookers query DeviceMaskerService            ││ │
│  │  │               └── DeviceHooker(service)                     ││ │
│  │  │               └── NetworkHooker(service)                    ││ │
│  │  │               └── ...                                       ││ │
│  │  └─────────────────────────────────────────────────────────────┘│ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 Data Flow

1. **SystemServiceHooker** hooks `ActivityManagerService.systemReady()`
2. **DeviceMaskerService** initializes as singleton in `system_server`
3. **Service** loads config from `/data/misc/devicemasker/config.json`
4. **ContentProvider** bridge allows UI to get binder reference
5. **UI** communicates with service via AIDL for config updates
6. **Hookers** in app processes query service for spoof values
7. **Config changes are immediate** (service holds live config)

### 3.3 Component Responsibilities

#### DeviceMaskerService (system_server)
- Holds configuration in memory (AtomicReference)
- Provides AIDL interface for queries
- Manages centralized logging
- Tracks filter statistics per app
- Handles config persistence

#### SystemServiceHooker
- Hooks system boot process
- Registers DeviceMaskerService
- Sets up ContentProvider bridge

#### ServiceClient (UI side)
- Connects to service via ContentProvider
- Provides typed API for UI operations
- Handles connection lifecycle

#### Hookers (app processes)
- Query service for enabled status
- Fetch spoof values via binder
- Report filter hits to service

---

## 4. Benefits Analysis

### 4.1 Real-Time Configuration ⭐⭐⭐

**Current**: User changes IMEI → Must restart target app → New IMEI applied  
**Target**: User changes IMEI → Binder call to service → Immediately effective

**Impact**: Dramatically improved user experience for iterative configuration.

### 4.2 Centralized Service ⭐⭐⭐

| Capability | Current | Target |
|------------|---------|--------|
| Real-time logs | ❌ | ✅ Stream via AIDL |
| Filter counts | ❌ | ✅ Per-app statistics |
| Force-stop apps | ❌ | ✅ Via ActivityManager |
| Health monitoring | ❌ | ✅ Service alive check |
| Hooked app count | ❌ | ✅ Real-time metric |

### 4.3 Simplified LSPosed Config ⭐⭐

**Current**: User must manually select every app to spoof in LSPosed scope  
**Target**: User selects only "System Framework (android)" once

**Impact**: Zero user configuration in LSPosed after initial setup.

### 4.4 Performance ⭐⭐

| Aspect | Current | Target |
|--------|---------|--------|
| Hook loading | Per-app (duplicated) | Once at boot |
| Class cache | Per-app (duplicated) | Shared singleton |
| Memory footprint | Higher (per-app) | Lower (centralized) |
| Config reads | File I/O per access | In-memory (service) |

### 4.5 Better Diagnostics ⭐⭐

- Centralized logging to single file
- Real-time log streaming to UI
- Per-app filter count statistics
- Service health monitoring
- Hooked app enumeration

---

## 5. Risk Assessment

### 5.1 Critical Risks

#### R1: System Instability (CRITICAL)
**Risk**: Bugs in service code can cause bootloops  
**Mitigation**:
- Extensive try-catch wrapping in SystemServiceHooker
- Safe mode detection (skip service on 3 consecutive boot failures)
- Config validation before applying
- Graceful degradation on service failure

#### R2: SELinux Denials (HIGH)
**Risk**: `system_server` cannot access config files  
**Mitigation**:
- Use `/data/misc/` path (accessible by system)
- Test with `setenforce 0` first, then fix denials
- Provide Magisk module for SELinux context fix if needed

#### R3: Android Version Compatibility (HIGH)
**Risk**: System APIs differ across Android 8-16  
**Mitigation**:
- Version-specific hooks where needed
- Extensive testing matrix (Android 8, 10, 11, 12, 13, 14, 15, 16)
- Fallback to XSharedPreferences if service fails

### 5.2 Moderate Risks

#### R4: Binder Thread Safety (MEDIUM)
**Risk**: Race conditions in multi-threaded binder calls  
**Mitigation**:
- Use `AtomicReference` for config
- Use `ConcurrentHashMap` for statistics
- Proper synchronization in service methods

#### R5: Service Connection Failures (MEDIUM)
**Risk**: UI cannot connect to service  
**Mitigation**:
- Retry logic with exponential backoff
- Clear error messages in UI
- Fallback to local-only mode

#### R6: Config File Corruption (LOW)
**Risk**: Config file becomes unreadable  
**Mitigation**:
- Atomic file writes (write to temp, then rename)
- Backup before modification
- JSON schema validation

---

## 6. Detailed Implementation Plan

### Phase 1: AIDL & Service Foundation (3-4 days)

#### Step 1.1: Create AIDL Interface

**File**: `common/src/main/aidl/com/astrixforge/devicemasker/IDeviceMaskerService.aidl`

```aidl
package com.astrixforge.devicemasker;

interface IDeviceMaskerService {
    // === Configuration ===
    void writeConfig(in String json);
    String readConfig();
    void reloadConfig();
    
    // === App Queries ===
    boolean isModuleEnabled();
    boolean isAppEnabled(in String packageName);
    String getSpoofValue(in String packageName, in String key);
    Map getAllSpoofValues(in String packageName);
    
    // === Logging ===
    void log(in String tag, in String message, int level);
    List<String> getLogs(int maxCount);
    void clearLogs();
    
    // === Statistics ===
    void incrementFilterCount(in String packageName);
    int getFilterCount(in String packageName);
    Map getAllFilterCounts();
    int getHookedAppCount();
    
    // === Control ===
    boolean isServiceAlive();
    void forceStopApp(in String packageName);
    String getServiceVersion();
    long getServiceUptime();
}
```

#### Step 1.2: Enable AIDL in Gradle

**File**: `common/build.gradle.kts`

```kotlin
android {
    buildFeatures {
        aidl = true
    }
}
```

#### Step 1.3: Implement DeviceMaskerService

**File**: `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/service/DeviceMaskerService.kt`

```kotlin
package com.astrixforge.devicemasker.xposed.service

import android.os.RemoteException
import com.astrixforge.devicemasker.IDeviceMaskerService
import com.astrixforge.devicemasker.common.model.JsonConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class DeviceMaskerService private constructor() : IDeviceMaskerService.Stub() {
    
    companion object {
        const val TAG = "DeviceMaskerService"
        const val VERSION = "1.0.0"
        const val MAX_LOGS = 1000
        
        @Volatile
        private var instance: DeviceMaskerService? = null
        
        fun getInstance(): DeviceMaskerService {
            return instance ?: synchronized(this) {
                instance ?: DeviceMaskerService().also { 
                    instance = it
                    it.initialize()
                }
            }
        }
    }
    
    // State
    private val config = AtomicReference<JsonConfig>(JsonConfig())
    private val startTime = AtomicLong(System.currentTimeMillis())
    private val logs = ConcurrentLinkedDeque<String>()
    private val filterCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val hookedApps = ConcurrentHashMap.newKeySet<String>()
    
    private fun initialize() {
        runCatching {
            ConfigManager.loadConfig()?.let { config.set(it) }
            log(TAG, "Service initialized", 0)
        }.onFailure {
            log(TAG, "Failed to load config: ${it.message}", 2)
        }
    }
    
    // === Configuration ===
    
    override fun writeConfig(json: String) {
        runCatching {
            val newConfig = JsonConfig.fromJson(json)
            config.set(newConfig)
            ConfigManager.saveConfig(newConfig)
            log(TAG, "Config updated", 0)
        }.onFailure {
            throw RemoteException("Failed to write config: ${it.message}")
        }
    }
    
    override fun readConfig(): String {
        return config.get().toJson()
    }
    
    override fun reloadConfig() {
        runCatching {
            ConfigManager.loadConfig()?.let { config.set(it) }
            log(TAG, "Config reloaded", 0)
        }
    }
    
    // === App Queries ===
    
    override fun isModuleEnabled(): Boolean {
        return config.get().moduleEnabled
    }
    
    override fun isAppEnabled(packageName: String): Boolean {
        if (!isModuleEnabled()) return false
        val appConfig = config.get().getAppConfig(packageName)
        return appConfig?.enabled == true
    }
    
    override fun getSpoofValue(packageName: String, key: String): String? {
        val appConfig = config.get().getAppConfig(packageName) ?: return null
        return appConfig.getValue(key)
    }
    
    override fun getAllSpoofValues(packageName: String): Map<String, String> {
        val appConfig = config.get().getAppConfig(packageName) ?: return emptyMap()
        return appConfig.getAllValues()
    }
    
    // === Logging ===
    
    override fun log(tag: String, message: String, level: Int) {
        val levelStr = when (level) {
            0 -> "I"
            1 -> "W"
            2 -> "E"
            else -> "D"
        }
        val entry = "${System.currentTimeMillis()}|$levelStr|$tag|$message"
        logs.addLast(entry)
        while (logs.size > MAX_LOGS) {
            logs.pollFirst()
        }
    }
    
    override fun getLogs(maxCount: Int): List<String> {
        return logs.takeLast(maxCount.coerceAtMost(MAX_LOGS))
    }
    
    override fun clearLogs() {
        logs.clear()
        log(TAG, "Logs cleared", 0)
    }
    
    // === Statistics ===
    
    override fun incrementFilterCount(packageName: String) {
        filterCounts.computeIfAbsent(packageName) { AtomicInteger(0) }.incrementAndGet()
        hookedApps.add(packageName)
    }
    
    override fun getFilterCount(packageName: String): Int {
        return filterCounts[packageName]?.get() ?: 0
    }
    
    override fun getAllFilterCounts(): Map<String, Int> {
        return filterCounts.mapValues { it.value.get() }
    }
    
    override fun getHookedAppCount(): Int {
        return hookedApps.size
    }
    
    // === Control ===
    
    override fun isServiceAlive(): Boolean = true
    
    override fun forceStopApp(packageName: String) {
        runCatching {
            val am = android.app.ActivityManager::class.java
            val method = am.getMethod("forceStopPackage", String::class.java)
            // This requires system permissions, will only work in system_server
            // method.invoke(activityManager, packageName)
            log(TAG, "Force stop: $packageName", 0)
        }
    }
    
    override fun getServiceVersion(): String = VERSION
    
    override fun getServiceUptime(): Long {
        return System.currentTimeMillis() - startTime.get()
    }
}
```

#### Step 1.4: Implement ConfigManager

**File**: `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/service/ConfigManager.kt`

```kotlin
package com.astrixforge.devicemasker.xposed.service

import com.astrixforge.devicemasker.common.model.JsonConfig
import java.io.File

object ConfigManager {
    private const val TAG = "ConfigManager"
    
    private val CONFIG_DIR = File("/data/misc/devicemasker")
    private val CONFIG_FILE = File(CONFIG_DIR, "config.json")
    private val BACKUP_FILE = File(CONFIG_DIR, "config.json.bak")
    
    fun loadConfig(): JsonConfig? {
        return runCatching {
            if (!CONFIG_FILE.exists()) {
                // First run, create default config
                CONFIG_DIR.mkdirs()
                val default = JsonConfig()
                saveConfig(default)
                return default
            }
            CONFIG_FILE.readText().let { JsonConfig.fromJson(it) }
        }.getOrNull()
    }
    
    fun saveConfig(config: JsonConfig) {
        runCatching {
            CONFIG_DIR.mkdirs()
            
            // Backup existing config
            if (CONFIG_FILE.exists()) {
                CONFIG_FILE.copyTo(BACKUP_FILE, overwrite = true)
            }
            
            // Atomic write: write to temp, then rename
            val tempFile = File(CONFIG_DIR, "config.json.tmp")
            tempFile.writeText(config.toJson())
            tempFile.renameTo(CONFIG_FILE)
        }
    }
    
    fun deleteConfig() {
        CONFIG_FILE.delete()
        BACKUP_FILE.delete()
    }
}
```

#### Step 1.5: Create ServiceBridge ContentProvider

**File**: `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/service/ServiceBridge.kt`

```kotlin
package com.astrixforge.devicemasker.xposed.service

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/**
 * ContentProvider that acts as a bridge between the UI app and DeviceMaskerService.
 * This is registered dynamically in system_server after service initialization.
 */
class ServiceBridge : ContentProvider() {
    
    companion object {
        const val AUTHORITY = "com.astrixforge.devicemasker.service"
        val URI: Uri = Uri.parse("content://$AUTHORITY")
        const val METHOD_GET_SERVICE = "getService"
        const val KEY_BINDER = "binder"
    }
    
    override fun onCreate(): Boolean = true
    
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method) {
            METHOD_GET_SERVICE -> {
                Bundle().apply {
                    putBinder(KEY_BINDER, DeviceMaskerService.getInstance())
                }
            }
            else -> null
        }
    }
    
    // Required ContentProvider methods (unused)
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, 
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, 
                        selectionArgs: Array<out String>?): Int = 0
}
```

---

### Phase 2: Hook Architecture Refactor (4-5 days)

#### Step 2.1: Create SystemServiceHooker

**File**: `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/SystemServiceHooker.kt`

```kotlin
package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.xposed.service.DeviceMaskerService
import com.astrixforge.devicemasker.xposed.util.DualLog
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker

/**
 * Hooks into system_server to initialize DeviceMaskerService at boot.
 * This MUST be loaded in the System Framework (android package) scope.
 */
object SystemServiceHooker : YukiBaseHooker() {
    
    private const val TAG = "SystemServiceHooker"
    
    override fun onHook() {
        // Hook ActivityManagerService.systemReady() to initialize our service
        runCatching {
            "com.android.server.am.ActivityManagerService".toClass().apply {
                method {
                    name = "systemReady"
                    paramCount(1..3) // Different signatures across Android versions
                }.hook {
                    after {
                        initializeService()
                    }
                }
            }
        }.onFailure {
            DualLog.error(TAG, "Failed to hook AMS.systemReady", it)
            // Fallback: try alternative hook point
            hookAlternative()
        }
    }
    
    private fun initializeService() {
        runCatching {
            // Initialize singleton service
            val service = DeviceMaskerService.getInstance()
            
            // Register ContentProvider for UI communication
            registerServiceBridge()
            
            DualLog.info(TAG, "DeviceMaskerService initialized successfully")
        }.onFailure {
            DualLog.error(TAG, "Failed to initialize service", it)
        }
    }
    
    private fun registerServiceBridge() {
        // Dynamic ContentProvider registration in system_server
        // This allows the UI app to discover and connect to our service
        runCatching {
            // Implementation depends on Android version
            // May need to use ActivityThread.installProvider() or similar
        }
    }
    
    private fun hookAlternative() {
        // Try SystemServer.run() as fallback
        runCatching {
            "com.android.server.SystemServer".toClass().apply {
                method { name = "run" }.hook {
                    after { initializeService() }
                }
            }
        }
    }
}
```

#### Step 2.2: Modify XposedEntry for System-Wide Loading

**File**: `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/XposedEntry.kt`

```kotlin
package com.astrixforge.devicemasker.xposed

import com.astrixforge.devicemasker.xposed.hooker.*
import com.astrixforge.devicemasker.xposed.service.DeviceMaskerService
import com.astrixforge.devicemasker.xposed.util.DualLog
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

@InjectYukiHookWithXposed
class XposedEntry : IYukiHookXposedInit {
    
    companion object {
        private const val TAG = "DeviceMasker"
        
        // Packages to never hook (system critical)
        private val SKIP_PACKAGES = setOf(
            "com.astrixforge.devicemasker", // Self
            "android",                       // System (but we DO hook for service)
            "com.android.systemui",
        )
        
        // System server process name
        private const val SYSTEM_SERVER = "system"
    }
    
    override fun onInit() = configs {
        debugLog {
            tag = TAG
            isEnable = BuildConfig.DEBUG
        }
    }
    
    override fun onHook() = encase {
        // === SYSTEM FRAMEWORK HOOK ===
        // Only load when LSPosed scope is "android" (System Framework)
        loadSystem {
            DualLog.info(TAG, "Loading system hooks...")
            
            // Initialize our service in system_server
            loadHooker(SystemServiceHooker)
            
            DualLog.info(TAG, "System hooks loaded")
        }
        
        // === APP PROCESS HOOKS ===
        // Load spoofing hooks in each app process
        loadApp {
            val pkg = packageName
            
            // Skip critical packages
            if (pkg in SKIP_PACKAGES) return@loadApp
            
            // Get service reference
            val service = getServiceOrNull()
            if (service == null) {
                DualLog.warn(TAG, "[$pkg] Service not available, skipping")
                return@loadApp
            }
            
            // Check if module is enabled globally
            if (!service.isModuleEnabled()) {
                return@loadApp
            }
            
            // Check if this specific app is enabled
            if (!service.isAppEnabled(pkg)) {
                return@loadApp
            }
            
            DualLog.info(TAG, "[$pkg] Loading hooks...")
            
            // Load anti-detection FIRST (critical order)
            loadHooker(AntiDetectHooker(service, pkg))
            
            // Load spoofing hookers
            loadHooker(DeviceHooker(service, pkg))
            loadHooker(NetworkHooker(service, pkg))
            loadHooker(AdvertisingHooker(service, pkg))
            loadHooker(LocationHooker(service, pkg))
            loadHooker(SystemHooker(service, pkg))
            loadHooker(SensorHooker(service, pkg))
            loadHooker(WebViewHooker(service, pkg))
            
            DualLog.info(TAG, "[$pkg] Hooks loaded successfully")
        }
    }
    
    private fun getServiceOrNull(): DeviceMaskerService? {
        return runCatching {
            DeviceMaskerService.getInstance()
        }.getOrNull()
    }
}
```

#### Step 2.3: Refactor BaseSpoofHooker

**File**: `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/BaseSpoofHooker.kt`

```kotlin
package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.IDeviceMaskerService
import com.astrixforge.devicemasker.xposed.util.DualLog
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker

/**
 * Base class for all spoofing hookers.
 * Provides common functionality for accessing the service and logging.
 */
abstract class BaseSpoofHooker(
    protected val service: IDeviceMaskerService,
    protected val targetPackage: String
) : YukiBaseHooker() {
    
    abstract val TAG: String
    
    /**
     * Get a spoof value from the service for this app.
     * Returns null if the key is not configured or service is unavailable.
     */
    protected fun getSpoofValue(key: String): String? {
        return runCatching {
            service.getSpoofValue(targetPackage, key)
        }.getOrNull()
    }
    
    /**
     * Check if a specific spoof category is enabled for this app.
     */
    protected fun isCategoryEnabled(category: String): Boolean {
        return getSpoofValue("${category}_enabled")?.toBoolean() == true
    }
    
    /**
     * Increment the filter count for this app.
     * Called when a value is successfully spoofed.
     */
    protected fun incrementFilterCount() {
        runCatching {
            service.incrementFilterCount(targetPackage)
        }
    }
    
    /**
     * Log a message to both local log and service log.
     */
    protected fun log(message: String) {
        DualLog.info(TAG, "[$targetPackage] $message")
        runCatching {
            service.log(TAG, "[$targetPackage] $message", 0)
        }
    }
    
    protected fun logWarn(message: String, throwable: Throwable? = null) {
        DualLog.warn(TAG, "[$targetPackage] $message", throwable)
        runCatching {
            service.log(TAG, "[$targetPackage] $message", 1)
        }
    }
    
    protected fun logError(message: String, throwable: Throwable? = null) {
        DualLog.error(TAG, "[$targetPackage] $message", throwable)
        runCatching {
            service.log(TAG, "[$targetPackage] $message", 2)
        }
    }
}
```

#### Step 2.4: Migrate Individual Hookers

Each of the 8 hookers needs to be updated to:
1. Extend `BaseSpoofHooker(service, packageName)` instead of `YukiBaseHooker`
2. Use `getSpoofValue(key)` instead of `prefs.getString(key)`
3. Call `incrementFilterCount()` on successful spoofs
4. Use `log()`, `logWarn()`, `logError()` for logging

**Example Migration (DeviceHooker)**:

```kotlin
// BEFORE
class DeviceHooker : YukiBaseHooker() {
    override fun onHook() {
        val imei = PrefsHelper.getString(SharedPrefsKeys.SPOOF_IMEI, null)
        if (imei != null) {
            // Hook TelephonyManager.getDeviceId()
        }
    }
}

// AFTER
class DeviceHooker(
    service: IDeviceMaskerService,
    packageName: String
) : BaseSpoofHooker(service, packageName) {
    
    override val TAG = "DeviceHooker"
    
    override fun onHook() {
        val imei = getSpoofValue(SharedPrefsKeys.SPOOF_IMEI)
        if (imei != null) {
            log("Spoofing IMEI to: ${imei.take(4)}***")
            // Hook TelephonyManager.getDeviceId()
            // On success: incrementFilterCount()
        }
    }
}
```

---

### Phase 3: UI Integration (2-3 days)

#### Step 3.1: Create ServiceClient

**File**: `app/src/main/kotlin/com/astrixforge/devicemasker/service/ServiceClient.kt`

```kotlin
package com.astrixforge.devicemasker.service

import android.content.Context
import android.os.Bundle
import com.astrixforge.devicemasker.IDeviceMaskerService
import com.astrixforge.devicemasker.xposed.service.ServiceBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ServiceClient(private val context: Context) {
    
    private var service: IDeviceMaskerService? = null
    
    val isConnected: Boolean
        get() = service?.isServiceAlive() == true
    
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bundle = context.contentResolver.call(
                ServiceBridge.URI,
                ServiceBridge.METHOD_GET_SERVICE,
                null,
                null
            )
            val binder = bundle?.getBinder(ServiceBridge.KEY_BINDER)
            service = IDeviceMaskerService.Stub.asInterface(binder)
            service?.isServiceAlive() == true
        }.getOrDefault(false)
    }
    
    fun disconnect() {
        service = null
    }
    
    // === Config Operations ===
    
    suspend fun writeConfig(json: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            service?.writeConfig(json)
            true
        }.getOrDefault(false)
    }
    
    suspend fun readConfig(): String? = withContext(Dispatchers.IO) {
        runCatching { service?.readConfig() }.getOrNull()
    }
    
    suspend fun reloadConfig() = withContext(Dispatchers.IO) {
        runCatching { service?.reloadConfig() }
    }
    
    // === Statistics ===
    
    suspend fun getFilterCount(packageName: String): Int = withContext(Dispatchers.IO) {
        runCatching { service?.getFilterCount(packageName) ?: 0 }.getOrDefault(0)
    }
    
    suspend fun getAllFilterCounts(): Map<String, Int> = withContext(Dispatchers.IO) {
        runCatching { 
            @Suppress("UNCHECKED_CAST")
            service?.allFilterCounts as? Map<String, Int> ?: emptyMap() 
        }.getOrDefault(emptyMap())
    }
    
    suspend fun getHookedAppCount(): Int = withContext(Dispatchers.IO) {
        runCatching { service?.hookedAppCount ?: 0 }.getOrDefault(0)
    }
    
    // === Logging ===
    
    suspend fun getLogs(maxCount: Int = 100): List<String> = withContext(Dispatchers.IO) {
        runCatching { service?.getLogs(maxCount) ?: emptyList() }.getOrDefault(emptyList())
    }
    
    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        runCatching { service?.clearLogs() }
    }
    
    // === Control ===
    
    suspend fun forceStopApp(packageName: String) = withContext(Dispatchers.IO) {
        runCatching { service?.forceStopApp(packageName) }
    }
    
    suspend fun getServiceInfo(): ServiceInfo? = withContext(Dispatchers.IO) {
        runCatching {
            ServiceInfo(
                version = service?.serviceVersion ?: "unknown",
                uptime = service?.serviceUptime ?: 0L,
                hookedApps = service?.hookedAppCount ?: 0
            )
        }.getOrNull()
    }
    
    data class ServiceInfo(
        val version: String,
        val uptime: Long,
        val hookedApps: Int
    )
}
```

#### Step 3.2: Update ViewModels

Example updates needed for ViewModels:

```kotlin
// Before: Using XposedPrefs directly
class HomeViewModel(
    private val xposedPrefs: XposedPrefs
) : ViewModel() {
    fun saveConfig() {
        xposedPrefs.save(config)
        // User must restart target app!
    }
}

// After: Using ServiceClient
class HomeViewModel(
    private val serviceClient: ServiceClient,
    private val configRepository: ConfigRepository
) : ViewModel() {
    
    private val _serviceConnected = MutableStateFlow(false)
    val serviceConnected: StateFlow<Boolean> = _serviceConnected.asStateFlow()
    
    init {
        connectToService()
    }
    
    private fun connectToService() {
        viewModelScope.launch {
            _serviceConnected.value = serviceClient.connect()
        }
    }
    
    fun saveConfig() {
        viewModelScope.launch {
            // Save locally
            configRepository.save(config)
            
            // Push to service immediately!
            serviceClient.writeConfig(config.toJson())
            
            // No restart needed! 🎉
        }
    }
    
    fun refreshStats() {
        viewModelScope.launch {
            val counts = serviceClient.getAllFilterCounts()
            _state.update { it.copy(filterCounts = counts) }
        }
    }
}
```

#### Step 3.3: Add Diagnostics Screen

New UI screen showing:
- Service connection status
- Service version and uptime
- Hooked app count
- Per-app filter counts
- Real-time log viewer
- Force-stop button for apps

---

### Phase 4: Testing & Polish (3-4 days)

#### Step 4.1: Testing Matrix

| Android Version | API | Priority | Status |
|-----------------|-----|----------|--------|
| Android 8.0 | 26 | High | ⬜ |
| Android 9.0 | 28 | Medium | ⬜ |
| Android 10 | 29 | High | ⬜ |
| Android 11 | 30 | High | ⬜ |
| Android 12 | 31 | High | ⬜ |
| Android 13 | 33 | High | ⬜ |
| Android 14 | 34 | High | ⬜ |
| Android 15 | 35 | High | ⬜ |
| Android 16 | 36 | Critical | ⬜ |

#### Step 4.2: Test Cases

1. **Service Initialization**
   - [ ] Service starts on boot
   - [ ] Service survives sleep/wake
   - [ ] Service accessible from UI
   - [ ] Config persists across reboots

2. **Configuration**
   - [ ] Config writes propagate immediately
   - [ ] Per-app configs work correctly
   - [ ] Group configs resolve properly
   - [ ] Default values work

3. **Hooking**
   - [ ] All 8 hookers function correctly
   - [ ] Anti-detection loads first
   - [ ] Filter counts increment
   - [ ] Logs capture correctly

4. **Edge Cases**
   - [ ] App installed after boot
   - [ ] App uninstalled while running
   - [ ] Service crash recovery
   - [ ] Config corruption handling

---

## 7. File Changes

### 7.1 New Files

| File | Purpose |
|------|---------|
| `common/src/main/aidl/.../IDeviceMaskerService.aidl` | AIDL interface |
| `xposed/service/DeviceMaskerService.kt` | Main service implementation |
| `xposed/service/ConfigManager.kt` | Config file I/O |
| `xposed/service/ServiceBridge.kt` | ContentProvider bridge |
| `xposed/hooker/SystemServiceHooker.kt` | System boot hook |
| `app/service/ServiceClient.kt` | UI-side service client |

### 7.2 Modified Files

| File | Changes |
|------|---------|
| `common/build.gradle.kts` | Enable AIDL |
| `xposed/XposedEntry.kt` | System-wide loading logic |
| `xposed/hooker/BaseSpoofHooker.kt` | Service-aware base class |
| `xposed/hooker/DeviceHooker.kt` | Use service |
| `xposed/hooker/NetworkHooker.kt` | Use service |
| `xposed/hooker/AdvertisingHooker.kt` | Use service |
| `xposed/hooker/LocationHooker.kt` | Use service |
| `xposed/hooker/SystemHooker.kt` | Use service |
| `xposed/hooker/SensorHooker.kt` | Use service |
| `xposed/hooker/WebViewHooker.kt` | Use service |
| `xposed/hooker/AntiDetectHooker.kt` | Use service |
| `app/viewmodel/*.kt` | Use ServiceClient |

### 7.3 Deprecated/Removed Files

| File | Reason |
|------|--------|
| `xposed/PrefsHelper.kt` | Replaced by service queries |
| `app/XposedPrefs.kt` | Replaced by ServiceClient |

---

## 8. Testing Strategy

### 8.1 Unit Tests

- ConfigManager serialization/deserialization
- Service state management
- Filter count tracking
- Log rotation

### 8.2 Integration Tests

- AIDL communication round-trip
- Config propagation end-to-end
- Hook loading in test app

### 8.3 Manual Testing

- Install on physical devices
- Various Android versions
- Different ROM types (AOSP, MIUI, OneUI, etc.)
- Stress testing (rapid config changes)

---

## 9. Rollback Plan

### 9.1 Feature Flags

```kotlin
object FeatureFlags {
    // If true, use new AIDL architecture
    // If false, fall back to XSharedPreferences
    const val USE_AIDL_SERVICE = true
}
```

### 9.2 Fallback Logic

```kotlin
override fun onHook() {
    if (FeatureFlags.USE_AIDL_SERVICE) {
        val service = getServiceOrNull()
        if (service != null) {
            // New architecture
            loadHooker(DeviceHooker(service, packageName))
        } else {
            // Fallback to old architecture
            loadHooker(LegacyDeviceHooker())
        }
    } else {
        // Disabled, use old architecture
        loadHooker(LegacyDeviceHooker())
    }
}
```

### 9.3 Recovery Mode

If device bootloops:
1. Boot to recovery
2. Delete `/data/misc/devicemasker/`
3. Or: Disable LSPosed via recovery

---

## 10. Timeline

```
Week 1:
├── Day 1-2: AIDL interface, DeviceMaskerService skeleton
├── Day 3-4: ConfigManager, ServiceBridge
└── Day 5: SystemServiceHooker, basic testing

Week 2:
├── Day 1-2: XposedEntry refactor, BaseSpoofHooker
├── Day 3-4: Migrate all 8 hookers
└── Day 5: ServiceClient, ViewModel updates

Week 3:
├── Day 1-2: UI integration, Diagnostics screen
├── Day 3-4: Testing across Android versions
└── Day 5: Bug fixes, documentation
```

---

## 11. Success Criteria

### 11.1 Must Have (MVP)

- [ ] DeviceMaskerService runs in system_server
- [ ] UI can connect and push config changes
- [ ] Config changes apply without app restart
- [ ] All 8 hookers work with new architecture
- [ ] Stable on Android 10-16

### 11.2 Should Have

- [ ] Filter count statistics in UI
- [ ] Real-time log viewing
- [ ] Force-stop apps from UI
- [ ] Service health monitoring

### 11.3 Nice to Have

- [ ] Android 8-9 support
- [ ] SELinux auto-fix Magisk module
- [ ] Boot time optimization
- [ ] Config sync across devices

---

## 12. Open Questions

1. **SELinux Policy**: Do we need a Magisk module for SELinux context?
2. **ContentProvider Registration**: How to dynamically register in system_server?
3. **Android 16 Changes**: Any breaking changes to system_server hooking?
4. **Legacy Support**: Keep XSharedPreferences path as fallback?

---

## Appendix A: HMA-OSS Reference

Key files from HMA-OSS for reference:
- `common/src/main/aidl/icu/nullptr/hidemyapplist/common/IHMAService.aidl`
- `xposed/src/main/java/icu/nullptr/hidemyapplist/xposed/HMAService.kt`
- `xposed/src/main/java/icu/nullptr/hidemyapplist/xposed/hook/HMAHookEntry.kt`

---

## Appendix B: Glossary

| Term | Definition |
|------|------------|
| AIDL | Android Interface Definition Language |
| Binder | Android's IPC mechanism |
| system_server | Core Android system process |
| LSPosed | Xposed framework for modern Android |
| XSharedPreferences | Xposed's cross-process SharedPreferences |

---

**Document Status**: READY FOR REVIEW  
**Next Step**: User approval → Begin Phase 1 implementation
