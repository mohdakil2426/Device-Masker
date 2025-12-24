# Progress: Device Masker

## Overall Status

| Metric | Value |
|--------|-------|
| **Project Phase** | PRODUCTION READY ✅ |
| **Active Changes** | 0 |
| **Archived Changes** | 8 |
| **Last Major Update** | December 24, 2025 - XSharedPreferences Config Sharing Complete |


---

## ✅ Complete: XSharedPreferences Cross-Process Config (Dec 24, 2025)

**Status**: Complete - SPOOFING NOW WORKS! 🎉
**Started**: December 23, 2025
**Completed**: December 24, 2025

### Problem Solved

The AIDL ServiceManager approach was initially attempted but proved unsuitable due to:
- SELinux restrictions preventing app UIDs from registering services
- Android's security policies for ServiceManager
- Performance overhead irrelevant for config-read-once scenarios
- Added complexity (723+ lines of code) with no reliability benefit

After comprehensive research (Dec 24, 2025), AIDL was **completely removed** in favor of XSharedPreferences.

### Solution: XSharedPreferences via YukiHookAPI

| Component | File | Purpose |
|-----------|------|---------|
| SharedPrefsKeys | `common/SharedPrefsKeys.kt` | Shared key generator |
| XposedPrefs | `app/data/XposedPrefs.kt` | Write with MODE_WORLD_READABLE |
| ConfigSync | `app/data/ConfigSync.kt` | Sync JsonConfig → per-app keys |
| PrefsKeys | `xposed/PrefsKeys.kt` | YukiHookAPI PrefsData definitions |
| PrefsReader | `xposed/PrefsReader.kt` | Helper functions for hooks |

### Key Implementation Details

1. **App writes config** using `MODE_WORLD_READABLE` SharedPreferences
2. **ConfigSync flattens** group-based config to per-app keys
3. **Hooks read via** YukiHookAPI's `prefs` property (XSharedPreferences)
4. **AndroidManifest.xml** has `xposedsharedprefs=true` meta-data

### Files Modified

| Module | Files |
|--------|-------|
| :common | `SharedPrefsKeys.kt` (new) |
| :app | `XposedPrefs.kt`, `ConfigSync.kt` (new), `ConfigManager.kt`, `AndroidManifest.xml` |
| :xposed | `XposedEntry.kt`, `PrefsKeys.kt`, `PrefsReader.kt` (new), all 6 hookers |

---

## ✅ Complete: AIDL Cleanup (Dec 24, 2025)

**Status**: Complete - Codebase Cleaned
**Objective**: Remove all AIDL dead code after research proved XSharedPreferences superior

### Research Findings

Comprehensive analysis of AIDL vs XSharedPreferences for LSPosed modules:

| Criteria | AIDL | XSharedPreferences | Winner |
|----------|------|-------------------|--------|
| **Performance** | 10-100x faster for high-frequency IPC | ~5-10ms overhead (once per app launch) | ✅ XShared (negligible for our use case) |
| **Reliability** | SELinux blocks ServiceManager registration | Battle-tested in HMA-OSS (1M+ downloads) | ✅ XShared |
| **Complexity** | 723+ lines of boilerplate | Simple file-based reads | ✅ XShared |
| **Security** | Requires weakening SELinux | Safe within LSPosed context | ✅ XShared |
| **Suitability** | Optimized for thousands of calls/sec | Perfect for config-read-once | ✅ XShared |

### Files Deleted (834 lines removed)

1. `common/src/main/aidl/` - Entire AIDL directory
2. `common/src/main/aidl/com/astrixforge/devicemasker/common/IDeviceMaskerService.aidl`
3. `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/DeviceMaskerService.kt`
4. `app/src/main/kotlin/com/astrixforge/devicemasker/service/ServiceClient.kt`
5. `app/src/main/kotlin/com/astrixforge/devicemasker/service/ServiceProvider.kt`

### Files Updated

- `ConfigManager.kt` - Removed all ServiceClient/AIDL references
- `SettingsViewModel.kt` - exportLogs() now suggests adb logcat
- `consumer-rules.pro` - Removed AIDL ProGuard rules
- Multiple comment updates to reflect XSharedPreferences-only architecture

---

## ✅ Complete: Critical Crash Fix (Dec 23, 2025)

**Status**: Complete
**Root Cause**: AntiDetectHooker blocking AndroidX class loading + directory creation failures

---

## ✅ Complete: Profile to Group Refactor (Dec 23, 2025)

**Status**: Complete
**Objective**: Renamed "Profile" → "Group" throughout codebase

---

## ✅ Complete: MVVM Architecture Refactor (Dec 22, 2025)

**Status**: All 5 screens migrated to pure MVVM

| Screen | ViewModel | State | Status |
|--------|-----------|-------|--------|
| HomeScreen | HomeViewModel | HomeState | ✅ Migrated |
| SettingsScreen | SettingsViewModel | SettingsState | ✅ Migrated |
| GroupsScreen | GroupsViewModel | GroupsState | ✅ Refactored |
| GroupSpoofingScreen | GroupSpoofingViewModel | GroupSpoofingState | ✅ Refactored |
| DiagnosticsScreen | DiagnosticsViewModel | DiagnosticsState | ✅ Migrated |

---

## ✅ Complete: Value Generation Improvements (Dec 21, 2025)

| Category | Before | After |
|----------|--------|-------|
| Countries | 9 | **16** |
| US Carriers | 6 | **45+** |
| Total Carriers | ~30 | **75+** |
| GPS Cities | 0 | **42** |
| Dual-SIM Types | 0 | **5** |

---

## Architecture Summary (Multi-Module + XSharedPreferences)

```
┌────────────────────────────────────────────────────────────────┐
│                         :app Module                            │
│  ┌──────────────┐  ┌─────────────────────────────────────────┐│
│  │  HookEntry   │  │           Service Layer                 ││
│  │  (KSP entry) │  │  ConfigManager → ConfigSync → XposedPrefs│
│  │      ↓       │  └─────────────────────────────────────────┘│
│  │  Delegates   │                                             │
│  │  to :xposed  │  ┌─────────────────────────────────────────┐│
│  └──────────────┘  │         SharedPreferences               ││
│                    │  (MODE_WORLD_READABLE via LSPosed)       ││
│                    └─────────────────────────────────────────┘│
└────────────────────────────────────────────────────────────────┘
                               ↓ XSharedPreferences
┌────────────────────────────────────────────────────────────────┐
│                        :xposed Module                          │
│  XposedEntry → prefs property → PrefsHelper → Hookers          │
│  - AntiDetectHooker (first)                                    │
│  - DeviceHooker, NetworkHooker, AdvertisingHooker              │
│  - SystemHooker, LocationHooker                                │
└────────────────────────────────────────────────────────────────┘
                               ↓
┌────────────────────────────────────────────────────────────────┐
│                        :common Module                          │
│  SharedPrefsKeys │ JsonConfig │ SpoofGroup │ generators/       │
└────────────────────────────────────────────────────────────────┘
```

---

## What Works

### ✅ Core Infrastructure - Complete
- [x] libs.versions.toml - Full dependency catalog
- [x] Build configuration - Gradle 9.1.0/Java 25
- [x] AndroidManifest.xml - LSPosed metadata + xposedsharedprefs
- [x] 3-module Gradle structure (:app, :common, :xposed)

### ✅ Cross-Process Config - Complete (NEW!)
- [x] XSharedPreferences via YukiHookAPI prefs property
- [x] SharedPrefsKeys in :common for key consistency
- [x] XposedPrefs with MODE_WORLD_READABLE
- [x] ConfigSync to flatten groups to per-app keys
- [x] PrefsHelper for easy access in hooks

### ✅ :common Module - Complete
- [x] SpoofType, SpoofCategory - Spoofing enums (22 types)
- [x] DeviceProfilePreset - 10 predefined device profiles
- [x] SpoofGroup, DeviceIdentifier, AppConfig - Data models
- [x] JsonConfig - Root configuration container
- [x] SharedPrefsKeys - Cross-process key generator
- [x] generators/ - 7 value generators

### ✅ :xposed Module - Complete
- [x] XposedEntry - Uses prefs property for config
- [x] PrefsKeys - YukiHookAPI PrefsData definitions
- [x] PrefsReader - PrefsHelper for hooks
- [x] 6 Hookers - All use PrefsHelper.getSpoofValue()

### ✅ :app Module - Complete (Refactored)
- [x] XposedPrefs - MODE_WORLD_READABLE writer
- [x] ConfigSync - JsonConfig → XposedPrefs sync
- [x] ConfigManager - Integrated with ConfigSync
- [x] SpoofRepository - Bridge to ConfigManager

### ✅ User Interface - Complete (M3 Expressive)
| Component | Status |
|-----------|--------|
| Theme System (Motion) | ✅ Done |
| MainActivity.kt | ✅ Done (3-tab navigation) |
| HomeScreen.kt | ✅ Done |
| GroupsScreen.kt | ✅ Done |
| GroupSpoofingScreen.kt | ✅ Done |
| SettingsScreen.kt | ✅ Done |
| DiagnosticsScreen.kt | ✅ Done |

---

## Build Status

| Build Type | Status | Last Run |
|------------|--------|----------|
| :common:assembleDebug | ✅ Success | Dec 24, 2025 |
| :xposed:assembleDebug | ✅ Success | Dec 24, 2025 |
| :app:assembleDebug | ✅ Success | Dec 24, 2025 |
| Full APK Build | ✅ Success | Dec 24, 2025 |

---

## Milestones

| Milestone | Target | Status |
|-----------|--------|--------|
| 📋 Planning Complete | Week 0 | ✅ Done |
| 🔧 Core Infrastructure | Week 2 | ✅ Done |
| 🎣 Device Spoofing | Week 3 | ✅ Done |
| 🛡️ Anti-Detection | Week 4 | ✅ Done |
| 💾 Data Persistence | Week 5 | ✅ Done |
| 🎨 UI Complete | Week 7 | ✅ Done |
| 📝 Documentation | Week 8 | ✅ Done |
| 📦 Release Build | Week 8 | ✅ Done |
| 🔄 Group Workflow Redesign | Week 9 | ✅ Done |
| 🔓 Independent Groups | Week 10 | ✅ Done |
| ✨ M3 Expressive Features | Week 11 | ✅ Done |
| 🏗️ Multi-Module Migration | Week 12 | ✅ Done |
| 📱 Device Profile UI | Week 12 | ✅ Done |
| 🔒 Value Generator Quality | Week 12 | ✅ Done |
| 📦 Generator Migration to :common | Week 12 | ✅ Done |
| 🔗 Spoof Value Correlation UI | Week 12 | ✅ Done |
| 🌍 Value Generation Improvements | Week 13 | ✅ Done |
| ✅ Expressive Cards App-wide | Week 13 | ✅ Done |
| 🔄 Refactor Profile to Group | Week 14 | ✅ Done |
| 🔧 XSharedPreferences Config | Week 14 | ✅ Done |
| 🗑️ AIDL Complete Removal | Week 14 | ✅ Done (834 lines removed) |
| ✅ v1.0 Release Ready | Week 14 | ✅ COMPLETE! 🎉 |
