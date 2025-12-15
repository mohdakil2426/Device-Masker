# Active Context: PrivacyShield

## Current Work Focus

### Active Change: `implement-privacy-shield-module`

**Status**: ✅ Phases 1-5 MVP Complete (Testing Pending)
**Location**: `openspec/changes/implement-privacy-shield-module/`
**Current Phase**: Phase 5 MVP Complete → Ready for Device Testing & Polish
**Next Action**: Test on physical device with LSPosed, then polish UI

### What's Been Built

Complete implementation of PrivacyShield LSPosed module with MVP UI:

1. **Core Infrastructure** - Build config, manifest, hook entry ✅ **DONE**
2. **Device Spoofing** - 24+ device identifier hooks ✅ **DONE**
   - All 5 generators (IMEI, Serial, MAC, UUID, Fingerprint)
   - All 4 data models (SpoofType, DeviceIdentifier, SpoofProfile, AppConfig)
   - All 5 hookers (Device, Network, Advertising, System, Location)
3. **Anti-Detection** - Stack trace/ClassLoader/Package hiding ✅ **DONE**
   - Stack trace filtering (Thread + Throwable)
   - Class loading blocking (Class.forName, ClassLoader.loadClass)
   - /proc/maps filtering (BufferedReader)
   - Package hiding (PackageManager)
4. **Data Management** - DataStore, Repositories ✅ **DONE**
   - SpoofDataStore with preference keys
   - ProfileRepository for profile CRUD
   - AppScopeRepository for per-app config
   - SpoofRepository combining all data access
5. **User Interface (MVP)** - Material 3 Expressive ✅ **DONE**
   - HomeScreen with animated status card, stats, profile, quick actions
   - SpoofSettingsScreen with 5 expandable categories, controls
   - SettingsScreen with theme toggles and about section
   - Bottom navigation with spring animations
   - AMOLED dark theme with dynamic colors support

## Recent Changes

### December 15, 2025 (continued)

| Time | Change | Status |
|------|--------|--------|
| 18:30 | Created HomeScreen with animated status card, stats, actions | ✅ |
| 18:35 | Created SpoofSettingsScreen with 5 expandable categories | ✅ |
| 18:40 | Created SettingsScreen with appearance/debug toggles | ✅ |
| 18:45 | Created BottomNavBar with spring animations | ✅ |
| 18:45 | Created NavDestination sealed class (later replaced) | ⚠️ Crashed |
| 18:50 | Updated MainActivity with NavHost and bottom nav | ✅ |
| 19:00 | Fixed animationSpec type mismatches (spring\<Color\>, spring\<IntSize\>) | ✅ |
| 19:10 | Fixed themes.xml API level warnings (tools:targetApi) | ✅ |
| 19:50 | **FIXED:** Android 16 crash - replaced sealed class with data class | ✅ |
| 19:55 | App successfully running on Android 16 device | ✅ |

### Android 16 Navigation Crash Fix

**Problem**: App crashed on launch with `NullPointerException: NavDestination.getRoute()`.

**Root Cause**: Using `sealed class NavDestination { object Home : NavDestination(...) }` 
caused null objects during Compose recomposition on Android 16 (API 36).

**Solution**: Replaced with simple `data class NavItem` + `object NavRoutes` string constants.

```kotlin
// OLD (broken on Android 16):
sealed class NavDestination { object Home : NavDestination("home") }

// NEW (working):
object NavRoutes { const val HOME = "home" }
data class NavItem(val route: String, val label: String, ...)
```

### December 15, 2025 (early)

| Time | Change | Status |
|------|--------|--------|
| 12:46 | Started Phase 2 implementation | ✅ |
| 12:50 | Created IMEIGenerator with Luhn validation | ✅ |
| 12:50 | Created SerialGenerator with manufacturer formats | ✅ |
| 12:50 | Created MACGenerator with real OUIs | ✅ |
| 12:50 | Created UUIDGenerator for all ID types | ✅ |
| 12:50 | Created FingerprintGenerator with device database | ✅ |
| 12:52 | Created SpoofType enum (24+ identifiers) | ✅ |
| 12:52 | Created DeviceIdentifier data class | ✅ |
| 12:52 | Created SpoofProfile data class | ✅ |
| 12:52 | Created AppConfig data class | ✅ |
| 12:55 | Created DeviceHooker (TelephonyManager, Build, Settings) | ✅ |
| 12:55 | Created NetworkHooker (WiFi, Bluetooth, Carrier) | ✅ |
| 12:55 | Created AdvertisingHooker (GSF, GAID, MediaDRM) | ✅ |
| 12:55 | Created SystemHooker (Build.*, SystemProperties) | ✅ |
| 12:55 | Created LocationHooker (GPS, Timezone, Locale) | ✅ |
| 13:00 | Updated HookEntry to load all Phase 2 hookers | ✅ |
| 13:05 | Fixed compilation errors (runCatching for optional methods) | ✅ |
| 13:10 | Build verified successful (EXIT CODE 0) | ✅ |

### Files Created in Phase 2

```
app/src/main/kotlin/com/akil/privacyshield/data/generators/
├── IMEIGenerator.kt           ✅ Created (Luhn validation, TAC prefixes)
├── SerialGenerator.kt         ✅ Created (Manufacturer-specific formats)
├── MACGenerator.kt            ✅ Created (Real OUIs, unicast)
├── UUIDGenerator.kt           ✅ Created (Android ID, GAID, GSF, DRM)
└── FingerprintGenerator.kt    ✅ Created (Device database)

app/src/main/kotlin/com/akil/privacyshield/data/models/
├── SpoofType.kt               ✅ Created (24+ enum values with categories)
├── DeviceIdentifier.kt        ✅ Created (Type + value wrapper)
├── SpoofProfile.kt            ✅ Created (Named profile with all values)
└── AppConfig.kt               ✅ Created (Per-app configuration)

app/src/main/kotlin/com/akil/privacyshield/hook/hooker/
├── DeviceHooker.kt            ✅ Created (IMEI, Serial, Android ID)
├── NetworkHooker.kt           ✅ Created (MAC, SSID, BSSID, Carrier)
├── AdvertisingHooker.kt       ✅ Created (GSF, GAID, Firebase, DRM)
├── SystemHooker.kt            ✅ Created (Build.*, SystemProperties)
└── LocationHooker.kt          ✅ Created (GPS, Timezone, Locale)
```

## Next Steps

### Immediate (Phase 3 - Anti-Detection)

1. **Create AntiDetectHooker**: Implement Xposed detection bypass
   - Stack trace filtering (Thread.getStackTrace, Throwable.getStackTrace)
   - ClassLoader hiding (Class.forName, ClassLoader.loadClass)
   - /proc/maps filtering (BufferedReader hooks)
   - Package hiding (PackageManager.getPackageInfo)

2. **Load AntiDetectHooker FIRST** in HookEntry.onHook()

### Short-Term

3. Test spoofing on physical device with LSPosed
4. Verify hooks trigger with Timber logs
5. Test with device info apps (IMEI check, DeviceInfo, etc.)

## Active Decisions & Considerations

### Decision: Use runCatching Instead of optional()

**Rationale**: The YukiHookAPI `optional()` method syntax didn't chain properly with `.hook {}`. Using Kotlin's `runCatching {}` provides the same safety for methods that may not exist on all Android versions.

**Implementation**: Wrapped optional method hooks in `runCatching { }` blocks to prevent crashes if methods don't exist.

### Decision: Lazy Value Generation

**Rationale**: Spoofed values are generated lazily using `by lazy { }` delegation. This ensures:
- Values are generated only when first accessed
- Same value is returned for all subsequent accesses within a process
- Will be replaced with DataStore reads in Phase 4

## Important Patterns & Preferences

### Code Style

- **Package**: `com.akil.privacyshield`
- **Source Set**: `kotlin/` (not `java/`)
- **Naming**: `*Hooker.kt`, `*Generator.kt`, `*Screen.kt`
- **Theme**: AMOLED black, Teal/Cyan primary

### YukiHookAPI Patterns Used

```kotlin
// Basic hook pattern
"ClassName".toClass().apply {
    method {
        name = "methodName"
        param(ParamType)
    }.hook {
        after {
            result = spoofedValue
        }
    }
}

// Optional method pattern (for APIs that might not exist)
runCatching {
    method { name = "optionalMethod" }.hook { }
}
```

## Files to Watch

| File | Reason |
|------|--------|
| `hook/HookEntry.kt` | Module entry point, hooker loading order |
| `hook/hooker/*Hooker.kt` | All 5 Phase 2 hookers |
| `data/generators/*.kt` | Value generation logic |
| `data/models/*.kt` | Data model definitions |
