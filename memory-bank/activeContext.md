# Active Context: Device Masker

## Current Work Focus

### Active Change: `implement-privacy-shield-module`

**Status**: ✅ Phases 1-6 Substantially Complete (Unit Tests Pending)
**Location**: `openspec/changes/implement-privacy-shield-module/`
**Current Phase**: Phase 6 (Polish & Release) - 80% Complete
**Next Action**: Unit tests (6.1), Integration testing (6.2)

### What's Been Built

Complete implementation of Device Masker LSPosed module with Full UI:

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
5. **User Interface (Full)** - Material 3 Expressive ✅ **DONE**
   - **Screens**: HomeScreen, SpoofSettingsScreen, SettingsScreen, AppSelectionScreen, ProfileScreen, DiagnosticsScreen
   - **Components**: StatusIndicator, ToggleButton, AppListItem, ProfileCard, SpoofValueCard
   - **Navigation**: 5-tab bottom nav (Home → Apps → Spoof → Profiles → Settings)
   - **Diagnostics**: Accessible from Settings > Advanced > Diagnostics
   - AMOLED dark theme with dynamic colors support
6. **Polish & Release (Partial)** ✅ **IN PROGRESS**
   - **Performance**: Hook value caching (lazy), app list caching
   - **Documentation**: README.md, docs/USAGE.md created
   - **ProGuard**: Comprehensive R8 rules for release builds
   - **Release Build**: Signing configured, release APK builds
   - **Pending**: Unit tests, integration tests

## Recent Changes

### December 15, 2025 (Phase 5 Full UI + Critical Bug Fixes)

| Time | Change | Status |
|------|--------|--------|
| 19:55 | Created StatusIndicator with animated dot/badge variants | ✅ |
| 19:56 | Created ToggleButton with spring physics | ✅ |
| 19:57 | Created AppListItem with checkbox, icon, status | ✅ |
| 19:58 | Created ProfileCard with actions (edit/delete/default) | ✅ |
| 19:59 | Created SpoofValueCard with regenerate/edit/copy | ✅ |
| 20:00 | Created AppSelectionScreen with search, filters, bulk actions | ✅ |
| 20:05 | Created ProfileScreen with FAB, dialogs (create/edit/delete) | ✅ |
| 20:10 | Created DiagnosticsScreen with real vs spoofed comparison | ✅ |
| 20:15 | Added new routes to NavDestination (APPS, PROFILES, DIAGNOSTICS) | ✅ |
| 20:20 | Wired new screens in MainActivity NavHost | ✅ |
| 20:25 | Added repository methods for new screens | ✅ |
| 20:30 | Fixed suspend function issues with rememberCoroutineScope | ✅ |
| 20:35 | Build verified successful | ✅ |
| 20:45 | Fixed Gradle deprecation warnings (kotlinOptions -> compilerOptions) | ✅ |
| 20:55 | Fixed build warnings (deprecated flags, unchecked casts in AntiDetectHooker) | ✅ |
| 21:10 | FIXED CRITICAL: App stuck at logo (Self-hooking loop) in HookEntry | ✅ |
| 21:35 | DISABLED: Stack trace hooks in AntiDetectHooker (Bootloop cause) | ✅ |
| **01:55** | **CRITICAL FIX: Module was hooking 'android' system process** | ✅ |
| 01:56 | Added forbiddenProcesses list (android, system_server, systemui) | ✅ |
| 01:57 | Fixed AntiDetectHooker allowlist to never block module dependencies | ✅ |
| 01:58 | Build verified successful after all fixes | ✅ |
| **20:02** | **CONFIRMED FIX: App launches correctly after LSPosed enable** | ✅ ✅ |

### December 15, 2025 (Phase 6 - Documentation & Release)

| Time | Change | Status |
|------|--------|--------|
| 20:09 | Updated 5-tab navigation (Home, Apps, Spoof, Profiles, Settings) | ✅ |
| 20:21 | Added comprehensive ProGuard rules for release builds | ✅ |
| 20:32 | Configured release signing via Android Studio | ✅ |
| 21:00 | Release build successful | ✅ |
| 21:42 | Updated tasks.md with all completion status | ✅ |
| 21:52 | Created README.md with full documentation | ✅ |
| 21:52 | Created docs/USAGE.md detailed walkthrough | ✅ |
| 22:00 | Verified performance optimizations in place | ✅ |
| 22:37 | UI CONSISTENCY FIX: AppSelectionScreen inline header | ✅ |
| 22:37 | UI CONSISTENCY FIX: ProfileScreen inline header | ✅ |
| 22:37 | Material 3 Pattern: Main nav destinations use inline headers | ✅ |
| **22:50** | **ProfileScreen: Replaced Scaffold with Box + positioned FAB** | ✅ |
| **22:55** | **DiagnosticsScreen: Replaced TopAppBar with inline Row header** | ✅ |
| **23:10** | **CRITICAL FIX: Dark mode content invisible - incomplete color scheme** | ✅ |
| **23:15** | **Theme.kt: Added complete dark color scheme (background, surface, etc.)** | ✅ |
| **23:18** | **MainActivity: Dynamic edge-to-edge using DisposableEffect + theme** | ✅ |
| **23:28** | **Card Consistency: DiagnosticsScreen cards (ModuleStatus, Category)** | ✅ |
| **23:30** | **Card Consistency: ProfileCard, CompactProfileCard** | ✅ |
| **23:32** | **Card Consistency: SpoofValueCard** | ✅ |
| **23:34** | **Card Consistency: AntiDetectionSection** | ✅ |
| **23:38** | **All cards now use ElevatedCard + surfaceContainerHigh + shapes.large** | ✅ |

### Critical Bug Fix: Dark Mode Content Invisible (December 15, 2025)

**Problem**: App content was completely invisible in dark mode (black on black).

**Root Cause Analysis**:
1. Regular dark theme (non-AMOLED) was **incomplete** - missing essential colors:
   - `background`, `onBackground`
   - `surface`, `onSurface`
   - All `surfaceContainer` variants
2. `enableEdgeToEdge()` used `SystemBarStyle.auto()` which follows SYSTEM theme, not app theme
3. When app uses dark mode but system is light mode, status bar icons became invisible

**Solution Applied**:
1. **Theme.kt**: Added complete dark color scheme with all 20+ color roles:
   ```kotlin
   darkColorScheme(
       background = Color(0xFF121212),
       onBackground = Color(0xFFE3E3E3),
       surface = Color(0xFF121212),
       onSurface = Color(0xFFE3E3E3),
       surfaceContainer = Color(0xFF1A1A1A),
       // ... all surface containers
   )
   ```
2. **MainActivity.kt**: Added `DisposableEffect(darkTheme)` to dynamically update edge-to-edge styling:
   ```kotlin
   DisposableEffect(darkTheme) {
       activity.enableEdgeToEdge(
           statusBarStyle = if (darkTheme) {
               SystemBarStyle.dark(Color.TRANSPARENT)
           } else {
               SystemBarStyle.light(...)
           }
       )
       onDispose { }
   }
   ```

**Key Lesson**: Material 3 `darkColorScheme()` does NOT provide sensible defaults - you MUST specify all color roles!

### Card Consistency Fix (December 15, 2025)

**Problem**: Cards in Profiles and Diagnostics screens had different styling from Settings/Spoof screens.

**Root Cause**:
1. Some cards used `Card` instead of `ElevatedCard`
2. Mixed use of `surfaceContainerLow` vs `surfaceContainerHigh`
3. Missing explicit `shape` on some cards

**Solution Applied**: Standardized all cards to use:
- `ElevatedCard` (not `Card`)
- `CardDefaults.elevatedCardColors(containerColor = surfaceContainerHigh)`
- `MaterialTheme.shapes.large` (sections) or `shapes.medium` (items)

**Files Updated**:
- `DiagnosticsScreen.kt`: ModuleStatusCard, AntiDetectionSection, CategoryDiagnosticSection
- `ProfileCard.kt`: ProfileCard, CompactProfileCard
- `SpoofValueCard.kt`: SpoofValueCard

### Critical Bug Fix: App Stuck at Logo/Splash (December 15, 2025)

**Problem**: App would launch normally before enabling LSPosed module. After enabling and restarting device, app stuck at logo and closed.

**Root Cause Analysis** (from LSPosed logs):
1. Module was hooking `packageName = "android"` (system process), NOT skipping it
2. `AntiDetectHooker` was blocking `Class.forName()` and `ClassLoader.loadClass()` for YukiHookAPI patterns
3. When module app launched, its own classes couldn't load because of the system-level block
4. `scopes.txt` showed module was in its own scope, triggering self-hook

**Log Evidence**:
```
[Device Masker][D][android-zygote] Device Masker: Starting hooks for package: android
[Device Masker][D][android-zygote] AntiDetectHooker: CRITICAL - Loading anti-detection hooks FIRST
[Device Masker][D][android-zygote] Before Hook Member [ClassLoader.loadClass] done  (×100+ times)
```

**Solution Applied**:
1. **HookEntry.kt**: Added `forbiddenProcesses` list to skip `android`, `system_server`, `com.android.systemui`
2. **HookEntry.kt**: Enhanced self-check to also match `processName.startsWith(selfPackage)`
3. **AntiDetectHooker.kt**: Added comprehensive `allowedPatterns` in `shouldBlockClass()` to never block:
   - `com.astrixforge.devicemasker.*`
   - `androidx.*`, `kotlin.*`, `kotlinx.*`
   - `android.*`, `java.*`, `com.google.android.*`
4. **AntiDetectHooker.kt**: Added same skip logic for system processes

**Key Lesson**: The `packageName` in YukiHookAPI's `encase {}` block can be `"android"` when loaded in Zygote scope, NOT just target apps. Always explicitly skip system processes!

### December 15, 2025 (earlier)

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
app/src/main/kotlin/com/astrixforge/devicemasker/data/generators/
├── IMEIGenerator.kt           ✅ Created (Luhn validation, TAC prefixes)
├── SerialGenerator.kt         ✅ Created (Manufacturer-specific formats)
├── MACGenerator.kt            ✅ Created (Real OUIs, unicast)
├── UUIDGenerator.kt           ✅ Created (Android ID, GAID, GSF, DRM)
└── FingerprintGenerator.kt    ✅ Created (Device database)

app/src/main/kotlin/com/astrixforge/devicemasker/data/models/
├── SpoofType.kt               ✅ Created (24+ enum values with categories)
├── DeviceIdentifier.kt        ✅ Created (Type + value wrapper)
├── SpoofProfile.kt            ✅ Created (Named profile with all values)
└── AppConfig.kt               ✅ Created (Per-app configuration)

app/src/main/kotlin/com/astrixforge/devicemasker/hook/hooker/
├── DeviceHooker.kt            ✅ Created (IMEI, Serial, Android ID)
├── NetworkHooker.kt           ✅ Created (MAC, SSID, BSSID, Carrier)
├── AdvertisingHooker.kt       ✅ Created (GSF, GAID, Firebase, DRM)
├── SystemHooker.kt            ✅ Created (Build.*, SystemProperties)
└── LocationHooker.kt          ✅ Created (GPS, Timezone, Locale)
```

## Next Steps

### Immediate (Phase 6 Remaining)

1. **Unit Tests**: Add tests for generators (IMEIGenerator, MACGenerator, etc.)
2. **Integration Testing**: Test with popular detection apps, banking apps

### Completed in This Session

1. ✅ **5-tab Navigation**: Home → Apps → Spoof → Profiles → Settings
2. ✅ **Diagnostics Access**: Available from Settings > Advanced
3. ✅ **ProGuard Rules**: Comprehensive R8 rules for release
4. ✅ **Release Signing**: Configured via Android Studio
5. ✅ **README.md**: Installation, usage, troubleshooting
6. ✅ **USAGE.md**: Detailed walkthrough guide
7. ✅ **Performance**: Value caching, app list lazy loading

### Future Enhancements

1. Per-app profile switching via quick settings tile
2. Backup/restore profiles to file
3. Import device fingerprints from real devices
4. Root detection bypass (SafetyNet/Play Integrity)

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

- **Package**: `com.astrixforge.devicemasker`
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
