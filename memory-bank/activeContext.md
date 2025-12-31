# Active Context: Device Masker

## Current Work Focus

### ✅ Complete: Kotlin 2.3.0 Upgrade (Jan 1, 2026)

**Status**: Complete ✅  
**Scope**: Upgraded from Kotlin 2.2.21 to Kotlin 2.3.0 stable release

#### Changes Made

| Component | Before | After |
|-----------|--------|-------|
| **Kotlin** | 2.2.21 | 2.3.0 |
| **KSP** | 2.2.21-2.0.4 | 2.3.4 (KSP2) |

#### Benefits Gained
- Java 25 native support
- K2 compiler improvements (faster builds, better type inference)
- Compose compiler stack traces for minified apps
- Stable Time API in standard library
- Improved UUID generation

#### Files Modified
| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Updated kotlin and ksp versions |

---

### ✅ Complete: Simplified Log Export (Dec 27, 2025)

**Status**: Complete ✅  
**Scope**: Simplified log export by removing logcat capture and direct download options

#### Changes Made

| Change | Description |
|--------|-------------|
| **Removed Logcat Capture** | Eliminated root-dependent logcat feature that was unreliable |
| **Removed Root Request** | No longer requests Magisk/SuperSU root on app start |
| **Removed Save Location Dialog** | No more "Downloads or Custom?" dialog |
| **Direct File Picker** | Export Logs now opens native file picker immediately |

#### Files Removed
- `service/RootManager.kt` - Root access utility no longer needed

#### Files Modified
| File | Change |
|------|--------|
| `service/LogManager.kt` | Simplified to YLog export only |
| `ui/screens/settings/SettingsScreen.kt` | Single export button, no dialog |
| `ui/screens/settings/SettingsViewModel.kt` | Removed logcat methods |
| `ui/screens/settings/SettingsState.kt` | Removed logcat state |
| `ui/screens/home/HomeViewModel.kt` | Removed root request |
| `MainActivity.kt` | Removed logcat callbacks |
| `res/values/strings.xml` | Removed unused strings |

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
| :common | ✅ SUCCESS | Dec 27, 2025 |
| :xposed | ✅ SUCCESS | Dec 27, 2025 |
| :app | ✅ SUCCESS | Dec 27, 2025 |
| Full APK | ✅ SUCCESS | Dec 27, 2025 |

---

## Next Steps

### Future Enhancements (Optional)

- Add Dual-SIM UI section
- Dynamic fingerprint generation
- Cell Info Xposed hooks
- Carrier picker in group creation
- More device presets
- Real-time config updates (without app restart)

---

## Important Files Reference

### Log Export Files
| File | Purpose |
|------|---------|
| `service/LogManager.kt` | YLog in-memory export with file picker |
| `ui/screens/settings/SettingsScreen.kt` | Export button |
| `ui/screens/settings/SettingsViewModel.kt` | Export action |

### Sync Architecture Files
| File | Purpose |
|------|---------|
| `common/SharedPrefsKeys.kt` | **SINGLE SOURCE OF TRUTH** for preference keys |
| `xposed/PrefsKeys.kt` | Delegates to SharedPrefsKeys |
| `xposed/PrefsReader.kt` | PrefsHelper for reading config in hooks |
| `app/data/XposedPrefs.kt` | Delegates to SharedPrefsKeys, writes with MODE_WORLD_READABLE |
| `app/data/ConfigSync.kt` | Syncs UI config to XposedPrefs |

### Hook Entry Files
| File | Purpose |
|------|---------|
| `app/hook/HookEntry.kt` | KSP entry point, YLog config with isRecord=true |
| `xposed/XposedEntry.kt` | Hook loader, loads all hookers |
| `xposed/DualLog.kt` | Logs to both YLog and internal buffer |
| `xposed/hooker/*.kt` | Individual hookers |


