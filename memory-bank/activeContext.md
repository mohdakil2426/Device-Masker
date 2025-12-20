# Active Context: Device Masker

## Current Work Focus

### ✅ Complete: HMA-OSS Architecture Migration

**Status**: Complete (Device Testing Done, Fixes Applied)
**Date**: December 20, 2025
**OpenSpec Change**: `adopt-hma-architecture`

#### Migration Progress

| Phase | Status | Description |
|-------|--------|-------------|
| Phase 1 | ✅ Complete | Multi-Module Gradle Structure (:app, :common, :xposed) |
| Phase 2 | ✅ Complete | :common Module (AIDL + Models + JsonConfig) |
| Phase 3 | ✅ Complete | :xposed Module (Hooks + DeviceMaskerService) |
| Phase 4 | ✅ Complete | :app Refactor (ConfigManager + ServiceClient) |
| Phase 5 | ⏭️ Skipped | Data Migration (not needed for unreleased app) |
| Phase 6 | ✅ Complete | Build Verification + Device Testing |
| Phase 7 | ✅ Complete | Cleanup & Documentation |

---

## Recent Changes (Dec 20, 2025)

### 🔧 Bug Fixes from Device Testing
Following first device test, identified and fixed critical issues:

#### Fix 1: Directory Creation (DeviceMaskerService.kt)
- **Issue**: Config save failed - `/data/system/devicemasker/` directory didn't exist
- **Fix**: Added `file.parentFile?.mkdirs()` before writing config
- **Status**: ✅ Fixed

#### Fix 2: Class Loading Hook (AntiDetectHooker.kt)
- **Issue**: `Class.forName` hook was too broad, blocked `androidx.work.WorkManagerInitializer`
- **Root Cause**: Used `contains()` instead of `startsWith()` for pattern matching
- **Fix**: Changed to strict prefix matching only for actual Xposed classes
- **Status**: ✅ Fixed

#### Fix 3: Logging Clarity (XposedEntry.kt)
- **Issue**: Log said "Initializing in system_server" for app processes
- **Fix**: Added process name to log messages for clarity
- **Status**: ✅ Fixed

### Previous: Phase 4 Complete - :app Module Refactor
- ✅ Created `ServiceClient.kt` - AIDL proxy for UI communication
- ✅ Created `ServiceProvider.kt` - ContentProvider for binder delivery
- ✅ Created `ConfigManager.kt` - Central config manager with StateFlow
- ✅ Updated `HookEntry.kt` - Delegates to XposedHookLoader
- ✅ Refactored `SpoofRepository.kt` - Bridge pattern to ConfigManager
- ✅ Created `SettingsDataStore.kt` - UI settings only (theme, AMOLED)
- ✅ Created `TypeAliases.kt` - Backward compat for old imports
- ✅ Deleted old model files (moved to :common)
- ✅ Deleted old hookers (moved to :xposed)

### Phase 5: N/A (App Not Released)
- ℹ️ Migration code removed - app is in testing phase
- ℹ️ No existing users to migrate
- ✅ Fresh config.json created on first run

### Phase 6 Complete: Build Verification + Device Testing
- ✅ Full APK builds successfully (24.82 MB)
- ✅ All 3 modules compile without errors
- ✅ First device test completed
- ✅ Critical bugs identified and fixed

### Phase 7 Complete: Cleanup & Documentation
- ✅ No TODO comments remaining
- ✅ Memory bank updated
- ✅ tasks.md fully updated

---

## Final Module Structure

```
devicemasker/
├── app/                                    # Main application (UI + Entry)
│   ├── build.gradle.kts                    # YukiHookAPI KSP enabled
│   └── src/main/kotlin/.../
│       ├── DeviceMaskerApp.kt              # App initialization
│       ├── hook/
│       │   └── HookEntry.kt                # @InjectYukiHookWithXposed
│       ├── service/
│       │   ├── ServiceClient.kt            # AIDL proxy
│       │   ├── ServiceProvider.kt          # Binder delivery
│       │   └── ConfigManager.kt            # StateFlow config manager
│       ├── data/
│       │   ├── SettingsDataStore.kt        # UI settings only
│       │   ├── models/TypeAliases.kt       # Backward compat
│       │   ├── generators/                 # Value generators
│       │   └── repository/
│       │       ├── SpoofRepository.kt      # Bridge to ConfigManager
│       │       └── AppScopeRepository.kt   # Installed apps
│       └── ui/                             # M3 Expressive UI
│
├── common/                                 # Shared models & AIDL
│   ├── src/main/aidl/.../
│   │   └── IDeviceMaskerService.aidl       # 10-method interface
│   └── src/main/kotlin/.../
│       ├── SpoofType.kt                    # 24 spoof types
│       ├── SpoofCategory.kt                # Categories
│       ├── DeviceIdentifier.kt             # Identifier model
│       ├── SpoofProfile.kt                 # Profile model
│       ├── AppConfig.kt                    # App config model
│       ├── JsonConfig.kt                   # Root config container
│       ├── Constants.kt                    # Shared constants
│       └── Utils.kt                        # Validation utilities
│
└── xposed/                                 # Xposed module logic
    └── src/main/kotlin/.../
        ├── XposedHookLoader.kt             # YukiBaseHooker
        ├── DeviceMaskerService.kt          # AIDL implementation
        ├── ServiceHelper.kt                # Binder access
        ├── Logcat.kt                       # Safe logging
        └── hooker/
            ├── AntiDetectHooker.kt         # Xposed hiding (FIRST)
            ├── DeviceHooker.kt             # IMEI, Serial, Android ID
            ├── NetworkHooker.kt            # WiFi/Bluetooth MAC
            ├── AdvertisingHooker.kt        # GSF ID, Ad ID
            ├── SystemHooker.kt             # Build.*, SystemProperties
            └── LocationHooker.kt           # GPS, Timezone, Locale
```

---

## HMA-OSS Architecture Overview

### Key Concepts

1. **AIDL-based IPC**: Service runs in system_server, UI communicates via AIDL
2. **In-Memory Config**: DeviceMaskerService holds JsonConfig in RAM
3. **Instant Sync**: No file I/O during hook execution
4. **JSON Storage**: Config persisted to `/data/system/devicemasker/config.json`

### Data Flow

```
┌─────────────┐     AIDL      ┌──────────────────────┐
│   App UI    │◄─────────────►│ DeviceMaskerService  │
│ (Material3) │  readConfig() │   (system_server)    │
│             │ writeConfig() │                      │
└─────────────┘               └──────────┬───────────┘
                                         │
                                         ▼ in-memory
                                   ┌──────────┐
                                   │JsonConfig│
                                   └────┬─────┘
                                        │ read by
                              ┌─────────┴─────────┐
                              ▼                   ▼
                        ┌──────────┐        ┌──────────┐
                        │DeviceHook│        │NetworkHok│
                        └──────────┘        └──────────┘
```

---

## Build Status

| Module | Status | Last Build |
|--------|--------|------------|
| :common | ✅ SUCCESS | Dec 20, 2025 |
| :xposed | ✅ SUCCESS | Dec 20, 2025 |
| :app | ✅ SUCCESS | Dec 20, 2025 |
| Full APK | ✅ SUCCESS (24.82 MB) | Dec 20, 2025 |

---

## Next Steps: Device Testing

The following require testing on a rooted device with LSPosed:

1. [ ] Install APK on device with LSPosed
2. [ ] Enable module in LSPosed Manager
3. [ ] Select scope (system apps + target apps)
4. [ ] Reboot device
5. [ ] Verify service starts (check logcat for "Service initialized")
6. [ ] Verify UI connects (check for "Binder received")
7. [ ] Test config sync (create profile, verify in logs)
8. [ ] Test hooks (IMEI checker app)
9. [ ] Test anti-detection (RootBeer app)

---

## Important Files for Reference

| File | Purpose |
|------|---------|
| `openspec/changes/adopt-hma-architecture/tasks.md` | Detailed task checklist |
| `app/.../service/ConfigManager.kt` | Central config manager |
| `app/.../service/ServiceClient.kt` | AIDL proxy |
| `app/.../data/SettingsDataStore.kt` | UI settings (theme, etc.) |
| `xposed/.../DeviceMaskerService.kt` | Service implementation |
| `xposed/.../XposedHookLoader.kt` | Hooker loader |
| `common/.../JsonConfig.kt` | Root config container |
| `common/.../IDeviceMaskerService.aidl` | AIDL interface |
