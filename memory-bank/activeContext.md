# Active Context: Device Masker

## Current Work Focus

### ✅ Complete: Timezone Picker & UI Improvements (Jan 1, 2026)

**Status**: Complete ✅  
**Scope**: Added timezone selection dialog, redesigned Location/SIM Card sections

#### Features Added

| Feature | Description |
|---------|-------------|
| **TimezonePickerDialog** | Searchable dialog for selecting timezones (similar to CountryPickerDialog) |
| **Timezone-Locale Sync** | Selecting timezone auto-updates locale to match country |
| **Location Section Redesign** | Now mirrors SIM Card design pattern |
| **SIM Card Merge** | Merged "Choose Sim" and "Carrier Info" into single card |

#### Files Created
| File | Purpose |
|------|---------|
| `ui/components/dialog/TimezonePickerDialog.kt` | Searchable timezone picker |

#### Files Modified
| File | Change |
|------|--------|
| `ui/screens/groupspoofing/categories/LocationContent.kt` | Redesigned to match SIM Card pattern |
| `ui/screens/groupspoofing/categories/SIMCardContent.kt` | Merged Carrier Info into main card |
| `ui/screens/groupspoofing/GroupSpoofingViewModel.kt` | Added `updateTimezone()` with locale sync |
| `ui/screens/groupspoofing/GroupSpoofingScreen.kt` | Added onTimezoneSelected callback |
| `ui/screens/groupspoofing/tabs/SpoofTabContent.kt` | Added onTimezoneSelected parameter |
| `ui/screens/groupspoofing/categories/CategorySection.kt` | Added onTimezoneSelected parameter |
| `common/models/LocationConfig.kt` | Added `getLocaleForTimezone()` helper |
| `res/values/strings.xml` | Added `label_choose_location` string |

#### UI Design Pattern

**Location Section (now matches SIM Card):**
```
┌─────────────────────────────────────────────┐
│ Choose Location                    [Switch] │
├─────────────────────────────────────────────┤
│ Timezone    │ Africa/Algiers           [>] │
│ Locale      │        ko_KR                 │ (read-only, synced)
└─────────────────────────────────────────────┘
```

**SIM Card Section (merged):**
```
┌─────────────────────────────────────────────┐
│ Choose Sim                         [Switch] │
├─────────────────────────────────────────────┤
│ Country     │ 🇮🇳 India               [>] │
│ Carrier     │ Airtel                  [▼] │
├─────────────────────────────────────────────┤
│ SIM Country          kr                     │
│ Network Country      kr                     │
│ MCC/MNC              45005                  │
│ Carrier Name         SK Telecom             │
│ SIM Operator         SK Telecom             │
│ Network Operator     45005                  │
└─────────────────────────────────────────────┘
```

#### Animation Fix
- Changed `AnimatedVisibility` to `if` statement in SIM Card
- Fixes animation sticking issue when closing the card
- Matches Device Profile implementation pattern

---

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


