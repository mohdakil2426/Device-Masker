# Active Context: Device Masker

## Current Work Focus

### ✅ Complete: YukiHookAPI Code Quality & Sync Improvements (Dec 25, 2025)

**Status**: Complete ✅  
**Scope**: Code refactoring, centralized utilities, sync architecture fixes

#### 1. Code Quality Improvements (Phase 1)

| Component | File | Purpose |
|-----------|------|---------|
| **ValueGenerators** | `xposed/utils/ValueGenerators.kt` | Centralized value generation (IMEI, MAC, Android ID, etc.) |
| **BaseSpoofHooker** | `xposed/hooker/BaseSpoofHooker.kt` | Abstract base class with shared hooker functionality |

#### 2. Hookers Refactored (~33% Code Reduction)

| Hooker | Before | After | Reduction |
|--------|--------|-------|-----------|
| DeviceHooker | 492 lines | 253 lines | -48% |
| NetworkHooker | 175 lines | 134 lines | -23% |
| AdvertisingHooker | 150 lines | 107 lines | -29% |
| LocationHooker | 177 lines | 134 lines | -24% |
| SystemHooker | 176 lines | 118 lines | -33% |
| SensorHooker | 158 lines | 123 lines | -22% |
| WebViewHooker | 98 lines | 76 lines | -22% |
| AntiDetectHooker | 277 lines | 195 lines | -30% |

#### 3. Sync Architecture Fix (Phase 2)

**Problem Solved**: Duplicate key generators in 3 files risked drift.

**New Architecture**:
```
┌─────────────────────────────────────────────────────────────────┐
│                    COMMON MODULE (Single Source of Truth)       │
│   SharedPrefsKeys.kt                                            │
│   ├── KEY_MODULE_ENABLED, KEY_DEBUG_ENABLED, etc.              │
│   ├── getAppEnabledKey(packageName)                            │
│   ├── getSpoofValueKey(packageName, type)                      │
│   └── getSpoofEnabledKey(packageName, type)                    │
└─────────────────────────────────────────────────────────────────┘
                    ▲                           ▲
         ┌─────────┴────────┐        ┌─────────┴────────┐
         │    APP MODULE    │        │   XPOSED MODULE  │
         │  XposedPrefs.kt  │        │   PrefsKeys.kt   │
         │   ↓ DELEGATES    │        │   ↓ DELEGATES    │
         │  SharedPrefsKeys │        │  SharedPrefsKeys │
         └──────────────────┘        └──────────────────┘
```

#### Files Modified in Sync Fix

| File | Change |
|------|--------|
| `common/SharedPrefsKeys.kt` | Enhanced as single source of truth |
| `xposed/PrefsKeys.kt` | Now delegates to SharedPrefsKeys |
| `app/XposedPrefs.kt` | Now delegates to SharedPrefsKeys |
| `app/ConfigSync.kt` | Added docs clarifying caching behavior |
| `xposed/PrefsReader.kt` | Added sync architecture docs |

---

### ✅ Complete: Comprehensive Enhancements (Dec 25, 2025)

**Status**: Complete ✅  
**Scope**: Logging, Stability, Performance, Code Quality

#### 1. Fixed Log Export (CRITICAL)

| Component | File | Purpose |
|-----------|------|---------|
| **LogManager** | `app/service/LogManager.kt` | Exports logs to Downloads via MediaStore |
| **YLog Recording** | `app/hook/HookEntry.kt` | Enabled `isRecord = true` for in-memory logs |
| **SettingsViewModel** | Fixed | Now uses LogManager for real export |
| **Storage Permissions** | `AndroidManifest.xml` | Added WRITE_EXTERNAL_STORAGE for legacy Android |

#### 2. Xposed Performance Optimizations

| Hooker | Changes |
|--------|---------|
| **DeviceHooker** | 4 cached classes, 3 cached values, 8 hooks → `replaceAny` |
| **NetworkHooker** | 4 cached classes, 3 cached values, 3 hooks → `replaceAny` |
| **SensorHooker** | 2 cached classes, cached preset |
| **WebViewHooker** | 1 cached class, cached model |

#### 3. SDK Version Safety (Android 16 Compatible)

| Component | File | Purpose |
|-----------|------|---------|
| **HookHelper** | `xposed/HookHelper.kt` | SDK version utilities |
| **SdkVersions** | Object | Constants for LOLLIPOP through BAKLAVA (Android 16) |
| **Validation** | Functions | IMEI, MAC, Android ID format validation |

---

### ✅ Complete: XSharedPreferences Cross-Process Config (Dec 24, 2025)

**Status**: Complete - SPOOFING NOW WORKS! 🎉

#### How It Works

```
┌─────────────────────────────────────────────────────────────┐
│                        APP UI PROCESS                        │
├─────────────────────────────────────────────────────────────┤
│  User saves config → ConfigManager.saveConfigInternal()     │
│                           │                                  │
│                           ▼                                  │
│  ConfigSync.syncFromConfig() → XposedPrefs (MODE_WORLD_READABLE)
│                                                              │
│  Writes keys like:                                           │
│  - module_enabled = true                                     │
│  - app_enabled_com_target_app = true                        │
│  - spoof_enabled_com_target_app_IMEI = true                 │
│  - spoof_value_com_target_app_IMEI = "358673912845672"      │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ XSharedPreferences reads file
┌─────────────────────────────────────────────────────────────┐
│                      TARGET APP PROCESS                      │
├─────────────────────────────────────────────────────────────┤
│  XposedEntry.onHook() → prefs.get(PrefsKeys.moduleEnabled)  │
│                           │                                  │
│                           ▼                                  │
│  DeviceHooker.hook → PrefsHelper.getSpoofValue(prefs, ...)  │
│                           │                                  │
│                           ▼                                  │
│  Returns spoofed "358673912845672" to app                    │
└─────────────────────────────────────────────────────────────┘
```

#### Critical Note: Caching Behavior
> XSharedPreferences **CACHES values** in hooked apps. Config changes require the target app to restart. This is a limitation of Android's cross-process SharedPreferences mechanism.

---

## Build Status

| Module | Status | Last Build |
|--------|--------|------------|
| :common | ✅ SUCCESS | Dec 25, 2025 |
| :xposed | ✅ SUCCESS | Dec 25, 2025 |
| :app | ✅ SUCCESS | Dec 25, 2025 |
| Full APK | ✅ SUCCESS | Dec 25, 2025 |

---

## Next Steps

### No Active Development Work

The spoofing functionality is now **fully working, optimized, and properly synced**! 🎉

### Future Enhancements (Optional)

- Add Dual-SIM UI section
- Dynamic fingerprint generation
- Cell Info Xposed hooks
- Carrier picker in group creation
- More device presets
- Real-time config updates (without app restart)

---

## Important Files Reference

### Sync Architecture Files
| File | Purpose |
|------|---------|
| `common/SharedPrefsKeys.kt` | **SINGLE SOURCE OF TRUTH** for preference keys |
| `xposed/PrefsKeys.kt` | Delegates to SharedPrefsKeys |
| `xposed/PrefsReader.kt` | PrefsHelper for reading config in hooks |
| `app/data/XposedPrefs.kt` | Delegates to SharedPrefsKeys, writes with MODE_WORLD_READABLE |
| `app/data/ConfigSync.kt` | Syncs UI config to XposedPrefs |

### Code Quality Files
| File | Purpose |
|------|---------|
| `xposed/utils/ValueGenerators.kt` | Centralized IMEI, MAC, Android ID generators |
| `xposed/hooker/BaseSpoofHooker.kt` | Abstract base with shared hooker functionality |
| `xposed/XposedEntry.kt` | Hook entry point, uses prefs property |
| `app/service/ConfigManager.kt` | Config management + sync trigger |


