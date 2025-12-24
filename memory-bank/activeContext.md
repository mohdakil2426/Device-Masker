# Active Context: Device Masker

## Current Work Focus

### ✅ Complete: XSharedPreferences Cross-Process Config (Dec 24, 2025)

**Status**: Complete - SPOOFING NOW WORKS! 🎉
**Root Cause Analysis**: The AIDL ServiceManager approach was blocked by SELinux. After thorough evaluation (Dec 24, 2025), AIDL was completely removed in favor of XSharedPreferences due to better reliability, simplicity, and suitability for the configuration-read-once use case.

#### Solution Implemented: XSharedPreferences via YukiHookAPI

| Component | Purpose | Location |
|-----------|---------|----------|
| **SharedPrefsKeys** | Shared key generator (app + xposed) | `common/SharedPrefsKeys.kt` |
| **XposedPrefs** | Write prefs with MODE_WORLD_READABLE | `app/data/XposedPrefs.kt` |
| **ConfigSync** | Sync JsonConfig → XposedPrefs | `app/data/ConfigSync.kt` |
| **PrefsKeys** | YukiHookAPI PrefsData keys | `xposed/PrefsKeys.kt` |
| **PrefsReader** | Helper to read prefs in hooks | `xposed/PrefsReader.kt` |
| **XposedEntry** | Updated to use prefs property | `xposed/XposedEntry.kt` |

#### Key Files Modified

| File | Changes |
|------|---------|
| `xposed/XposedEntry.kt` | Uses `prefs` property and `PrefsHelper` for config |
| `xposed/hooker/*.kt` | All 6 hookers use `PrefsHelper.getSpoofValue()` |
| `app/service/ConfigManager.kt` | Calls `ConfigSync.syncFromConfig()` on save |
| `app/AndroidManifest.xml` | Added `xposedsharedprefs=true` meta-data |

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

#### Critical Requirements for XSharedPreferences

1. **AndroidManifest.xml** must have:
   ```xml
   <meta-data android:name="xposedsharedprefs" android:value="true" />
   <meta-data android:name="xposedminversion" android:value="93" />
   ```

2. **SharedPreferences** must use `MODE_WORLD_READABLE`

3. **Keys must match** between app (XposedPrefs) and module (PrefsKeys)

4. **Config changes require app restart** - XSharedPreferences caches values

---

### ✅ Complete: Profile to Group Refactor (Dec 23, 2025)

**Status**: Complete
**Objective**: Renamed all "Profile" references to "Group" to avoid Android Work Profile confusion.

---

### ✅ Complete: MVVM Architecture (Dec 22, 2025)

**Status**: All 5 screens migrated to pure MVVM pattern.

---

## Recent Changes (Dec 24, 2025)

### 🔧 XSharedPreferences Implementation

**New Files Created:**
- `common/SharedPrefsKeys.kt` - Shared key constants
- `app/data/XposedPrefs.kt` - SharedPreferences writer
- `app/data/ConfigSync.kt` - JsonConfig → XposedPrefs sync
- `xposed/PrefsKeys.kt` - YukiHookAPI PrefsData definitions
- `xposed/PrefsReader.kt` - PrefsHelper object

**Modified Files:**
- `xposed/XposedEntry.kt` - Uses prefs property
- `xposed/hooker/DeviceHooker.kt` - PrefsHelper.getSpoofValue()
- `xposed/hooker/NetworkHooker.kt` - PrefsHelper.getSpoofValue()
- `xposed/hooker/AdvertisingHooker.kt` - PrefsHelper.getSpoofValue()
- `xposed/hooker/SystemHooker.kt` - PrefsHelper.getSpoofValue()
- `xposed/hooker/LocationHooker.kt` - PrefsHelper.getSpoofValue()
- `app/service/ConfigManager.kt` - ConfigSync integration
- `app/AndroidManifest.xml` - xposedsharedprefs meta-data

### 🧹 AIDL Complete Removal (Dec 24, 2025)

**Status**: Complete - Codebase cleaned  
**Objective**: Remove all AIDL infrastructure after comprehensive research showed XSharedPreferences is superior for this use case.

**Files Deleted** (834 lines removed):
- `common/src/main/aidl/com/astrixforge/devicemasker/common/IDeviceMaskerService.aidl` (entire directory)
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/DeviceMaskerService.kt`
- `app/src/main/kotlin/com/astrixforge/devicemasker/service/ServiceClient.kt`
- `app/src/main/kotlin/com/astrixforge/devicemasker/service/ServiceProvider.kt`

**Files Updated**:
- `app/service/ConfigManager.kt` - Removed all ServiceClient references
- `app/ui/screens/settings/SettingsViewModel.kt` - Updated exportLogs() to inform about adb logcat alternative
- `common/consumer-rules.pro` - Removed AIDL ProGuard rules
- Comments updated in DeviceMaskerApp.kt, HookEntry.kt, DualLog.kt

**Research Summary**: Extensive research comparing AIDL vs XSharedPreferences concluded that:
1. **Performance**: AIDL's speed advantage is irrelevant for config-read-once scenarios
2. **Reliability**: XSharedPreferences with LSPosed API 93+ is battle-tested (1M+ downloads in HMA-OSS)
3. **Complexity**: AIDL adds 4x more code with no benefit for our use case
4. **SELinux**: AIDL ServiceManager registration fails due to security restrictions
5. **Simplicity**: XSharedPreferences is the proven standard for LSPosed modules

---

## Build Status

| Module | Status | Last Build |
|--------|--------|------------|
| :common | ✅ SUCCESS | Dec 24, 2025 |
| :xposed | ✅ SUCCESS | Dec 24, 2025 |
| :app | ✅ SUCCESS | Dec 24, 2025 |
| Full APK | ✅ SUCCESS | Dec 24, 2025 |

---

## Next Steps

### No Active Development Work

The spoofing functionality is now **fully working**! 🎉

### Future Enhancements (Optional)

- Add Dual-SIM UI section
- Dynamic fingerprint generation
- Cell Info Xposed hooks
- Carrier picker in group creation
- More device presets
- Real-time config updates (without app restart)

---

## Important Files Reference

| File | Purpose |
|------|---------|
| `xposed/XposedEntry.kt` | Hook entry point, uses prefs property |
| `xposed/PrefsReader.kt` | PrefsHelper for reading config in hooks |
| `xposed/PrefsKeys.kt` | PrefsData key definitions |
| `app/data/XposedPrefs.kt` | SharedPreferences writer (MODE_WORLD_READABLE) |
| `app/data/ConfigSync.kt` | Syncs UI config to XposedPrefs |
| `common/SharedPrefsKeys.kt` | Shared key generator |
| `app/service/ConfigManager.kt` | Config management + sync trigger |


