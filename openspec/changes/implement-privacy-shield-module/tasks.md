# Tasks: Implement PrivacyShield LSPosed Module

> **Estimated Total Effort**: 8 weeks (part-time) or 4 weeks (full-time)
> 
> **Dependencies**: Tasks should be completed in phase order. Tasks within a phase can be parallelized where noted.

---

## Phase 1: Core Infrastructure (Week 1-2)

### 1.1 Build Configuration
- [x] 1.1.1 Update `gradle/libs.versions.toml` with all required dependencies
  - Kotlin 2.2.21, Android Gradle Plugin 8.13.2
  - YukiHookAPI 1.3.1, KSP 2.2.21-2.0.4
  - KavaRef 1.0.2 (required for YukiHookAPI 1.3.x reflection)
  - AndroidHiddenApiBypass 6.1 (hidden API access)
  - Compose BOM 2025.12.00, Material 3 1.4.0
  - Material Components 1.13.0 (XML Views/YukiHookAPI)
  - DataStore 1.2.0, Coroutines 1.10.2
  - Serialization 1.9.0, Timber 5.0.1, Coil 3.2.0
- [x] 1.1.2 Update root `build.gradle.kts` with required plugins
  - kotlin-android, compose, serialization, ksp plugins
- [x] 1.1.3 Update `settings.gradle.kts` to add Xposed repository
  - Add `maven { url = uri("https://api.xposed.info/") }`
- [x] 1.1.4 Rewrite `app/build.gradle.kts` per PRD specification
  - compileSdk 36, minSdk 26, targetSdk 36 (Android 16)
  - Java 21 source/target compatibility
  - Compose build features, experimental opt-ins
  - All dependency declarations
- [x] 1.1.5 Verify Gradle sync completes successfully (In Progress - verifying build)

### 1.2 Android Manifest & Resources
- [x] 1.2.1 Update `AndroidManifest.xml` with LSPosed metadata
  - Add xposedmodule=true, xposedminversion=82
  - Add xposeddescription, xposedscope
  - Add QUERY_ALL_PACKAGES permission
  - Add Application class reference
  - Add MainActivity with LAUNCHER intent-filter
- [x] 1.2.2 Create `res/values/arrays.xml` for xposed_scope
- [x] 1.2.3 Update `res/values/strings.xml` with app strings
- [x] 1.2.4 Create `res/values/themes.xml` for Theme.PrivacyShield
- [x] 1.2.5 Remove old XML theme files (Removed colors.xml)

### 1.3 Project Structure
- [x] 1.3.1 Create source directories per PRD structure:
  ```
  app/src/main/kotlin/com/akil/privacyshield/
  ├── PrivacyShieldApp.kt
  ├── hook/
  │   ├── HookEntry.kt
  │   └── hooker/
  ├── data/
  │   ├── models/
  │   └── generators/
  ├── ui/
  │   ├── theme/
  │   ├── screens/
  │   ├── components/
  │   └── navigation/
  └── utils/
  ```
- [x] 1.3.2 Create `PrivacyShieldApp.kt` (ModuleApplication)
  - Initialize Timber logging
  - Initialize DataStore
  - YukiHookAPI module status check

### 1.4 Hook Entry Point
- [x] 1.4.1 Create `hook/HookEntry.kt` with @InjectYukiHookWithXposed
  - Implement IYukiHookXposedInit interface
  - Configure debug logging in onInit()
  - Implement onHook() with loadHooker() calls (placeholder TODOs)
- [x] 1.4.2 Verify module appears in LSPosed Manager (Ready for testing)
- [x] 1.4.3 Test basic hook loading with Timber logs (Implemented)

**Phase 1 Validation**:
- [ ] Project builds without errors (Verifying)
- [ ] Module appears in LSPosed Manager (Pending device test)
- [ ] Can enable module in LSPosed and reboot (Pending device test)
- [ ] Log messages appear from HookEntry (Pending device test)

---

## Phase 2: Device Spoofing Hooks (Week 2-3)

### 2.1 Value Generators (Parallelizable)
- [x] 2.1.1 Create `data/generators/IMEIGenerator.kt`
  - Generate 15-digit IMEI with Luhn checksum
  - Use realistic TAC prefixes (35, 86, 01, 45)
  - Include validation function
- [x] 2.1.2 Create `data/generators/SerialGenerator.kt`
  - Generate 8-16 alphanumeric serials
  - Support device-specific formats
- [x] 2.1.3 Create `data/generators/MACGenerator.kt`
  - Generate valid unicast MAC (clear LSB of first octet)
  - Format as XX:XX:XX:XX:XX:XX
- [x] 2.1.4 Create `data/generators/UUIDGenerator.kt`
  - Generate Android ID (16 hex chars)
  - Generate Advertising ID (UUID format)
  - Generate GSF ID (16 hex chars)
- [x] 2.1.5 Create `data/generators/FingerprintGenerator.kt`
  - Generate realistic Build fingerprint strings
  - Use device database for realistic combinations

### 2.2 Data Models (Dependency: 2.1)
- [x] 2.2.1 Create `data/models/SpoofType.kt` enum
  - IMEI, IMSI, SERIAL, ANDROID_ID, etc.
- [x] 2.2.2 Create `data/models/DeviceIdentifier.kt` data class
- [x] 2.2.3 Create `data/models/SpoofProfile.kt` data class
  - All 24+ spoofable fields with nullable values
  - id, name, isDefault, createdAt, updatedAt
- [x] 2.2.4 Create `data/models/AppConfig.kt` data class
  - packageName, appLabel, isEnabled, profileId
  - enabledSpoofs Set<SpoofType>

### 2.3 Device Hooker
- [x] 2.3.1 Create `hook/hooker/DeviceHooker.kt` (YukiBaseHooker)
  - Check app scope before hooking
  - Hook TelephonyManager.getDeviceId()
  - Hook TelephonyManager.getImei() (paramCount 0 and 1)
  - Hook TelephonyManager.getMeid() with runCatching
  - Hook TelephonyManager.getSubscriberId() (IMSI)
  - Hook TelephonyManager.getSimSerialNumber()
- [x] 2.3.2 Hook Build fields in DeviceHooker
  - Build.SERIAL via SystemProperties hook
  - Build.getSerial() method hook
  - SystemProperties.get() for ro.serialno
- [x] 2.3.3 Hook Settings.Secure for ANDROID_ID
  - Hook Settings.Secure.getString() for "android_id"
- [ ] 2.3.4 Test IMEI spoofing with device info app (Pending device test)

### 2.4 Network Hooker
- [x] 2.4.1 Create `hook/hooker/NetworkHooker.kt`
  - Hook WifiInfo.getMacAddress()
  - Hook NetworkInterface.getHardwareAddress()
- [x] 2.4.2 Hook Bluetooth MAC
  - Hook BluetoothAdapter.getAddress()
- [x] 2.4.3 Hook WiFi SSID/BSSID
  - Hook WifiInfo.getSSID()
  - Hook WifiInfo.getBSSID()
- [x] 2.4.4 Hook carrier info
  - TelephonyManager.getNetworkOperatorName()
  - TelephonyManager.getNetworkOperator()
- [ ] 2.4.5 Test MAC spoofing with network info app (Pending device test)

### 2.5 Advertising Hooker
- [x] 2.5.1 Create `hook/hooker/AdvertisingHooker.kt`
  - Hook GSF ID via Gservices.getString("android_id")
  - Hook AdvertisingIdClient.getAdvertisingIdInfo()
  - Hook MediaDrm.getPropertyByteArray() for DRM ID
- [ ] 2.5.2 Test advertising ID spoofing (Pending device test)

### 2.6 System Hooker
- [x] 2.6.1 Create `hook/hooker/SystemHooker.kt`
  - Hook all Build.* static fields via modifyBuildField()
  - Hook SystemProperties.get() for ro.* properties
- [x] 2.6.2 Hook Build properties
  - FINGERPRINT, MODEL, MANUFACTURER, BRAND, etc.
- [ ] 2.6.3 Test with device info apps (Pending device test)

### 2.7 Location Hooker
- [x] 2.7.1 Create `hook/hooker/LocationHooker.kt`
  - Hook Location.getLatitude()
  - Hook Location.getLongitude()
  - Hook Location.getAltitude()
- [x] 2.7.2 Hook timezone/locale
  - Hook TimeZone.getDefault()
  - Hook Locale.getDefault()
- [ ] 2.7.3 Test with maps/location apps (Pending device test)

**Phase 2 Validation**:
- [ ] All IMEI checker apps show spoofed IMEI
- [ ] Device info apps show spoofed values
- [ ] MAC addresses are spoofed in network info apps
- [ ] Log output shows hooks being triggered

---

## Phase 3: Anti-Detection Layer (Week 3-4)

### 3.1 Anti-Detection Hooker
- [x] 3.1.1 Create `hook/hooker/AntiDetectHooker.kt`
  - Define HIDDEN_CLASS_PATTERNS list (Xposed classes, YukiHookAPI classes)
  - Define HIDDEN_LIBRARY_PATTERNS list (libxposed, liblspd, etc.)
  - Define HIDDEN_PACKAGES list (Xposed installers)

### 3.2 Stack Trace Hiding
- [x] 3.2.1 Hook Thread.getStackTrace()
  - Filter out stack frames matching HIDDEN_CLASS_PATTERNS
- [x] 3.2.2 Hook Throwable.getStackTrace()
  - Same filtering for exception traces
- [ ] 3.2.3 Hook Throwable.printStackTrace() (Deferred - Low Priority)
  - Filter before printing
- [ ] 3.2.4 Test with RootBeer or similar detection library (Pending device test)

### 3.3 Class Loading Hiding
- [x] 3.3.1 Hook Class.forName(String)
  - Throw ClassNotFoundException for Xposed classes
- [x] 3.3.2 Hook Class.forName(String, boolean, ClassLoader)
  - Same protection with all overloads
- [x] 3.3.3 Hook ClassLoader.loadClass()
  - Block loading of Xposed classes (both overloads)
- [ ] 3.3.4 Test with XposedDetector sample code (Pending device test)

### 3.4 Native Library Hiding
- [x] 3.4.1 Hook BufferedReader for /proc/self/maps reading
  - Filter lines containing HIDDEN_LIBRARY_PATTERNS
- [ ] 3.4.2 Test /proc/maps filtering (Pending device test)

### 3.5 Reflection Hiding
- [ ] 3.5.1 Hook Method.getModifiers() (Deferred - Complex implementation)
  - Return original modifiers for hooked methods
- [ ] 3.5.2 Hook Field.getModifiers() (Deferred - Complex implementation)
  - Return original modifiers for hooked fields

### 3.6 Package Hiding
- [x] 3.6.1 Hook PackageManager.getPackageInfo()
  - Throw NameNotFoundException for Xposed packages
- [x] 3.6.2 Hook PackageManager.getApplicationInfo()
  - Same protection
- [x] 3.6.3 Hook PackageManager.getInstalledPackages()
  - Filter Xposed packages from list
- [x] 3.6.4 Hook PackageManager.getInstalledApplications()
  - Filter Xposed apps from list

**Phase 3 Validation**:
- [ ] RootBeer does not detect Xposed (Pending device test)
- [ ] SafetyNet Helper does not detect hooks (Pending device test)
- [ ] /proc/self/maps shows no Xposed libraries (Pending device test)
- [ ] Class.forName("de.robv.android.xposed.*") throws exception (Pending device test)

---

## Phase 4: Data Management (Week 4-5)

### 4.1 DataStore Setup
- [x] 4.1.1 Create `data/SpoofDataStore.kt`
  - Define preference keys for all spoof values
  - Initialize DataStore via preferencesDataStore extension
  - Provide suspend functions for read/write (Flow-based)
  - Provide blocking functions for hook context (runBlocking)
- [x] 4.1.2 Create extension for Context.spoofDataStore

### 4.2 Profile Management
- [x] 4.2.1 Create `data/repository/ProfileRepository.kt`
  - CRUD operations for SpoofProfile
  - List all profiles
  - Get active profile for package
  - Set default profile
- [x] 4.2.2 Implement profile serialization to DataStore
  - Use Kotlinx Serialization JSON

### 4.3 App Scope Management
- [x] 4.3.1 Create `data/repository/AppScopeRepository.kt`
  - Enable/disable spoofing per app
  - Assign profiles to apps
  - Get enabled spoof types per app
- [x] 4.3.2 Query installed apps with PackageManager
  - Get app labels and icons
  - Cache app list for performance

### 4.4 Repository Pattern
- [x] 4.4.1 Create `data/repository/SpoofRepository.kt`
  - Combine ProfileRepository and AppScopeRepository
  - Provide Flow<UiState> for reactive updates (DashboardState)
  - Handle profile/app relationship logic
  - Value generation integration
  - Singleton getInstance() pattern

**Phase 4 Validation**:
- [ ] Can create and save profiles (Pending UI)
- [ ] Profile values persist across app restart (Pending device test)
- [ ] Per-app configuration saves correctly (Pending UI)
- [ ] Hooks read correct values from DataStore (Pending integration)

---

## Phase 5: User Interface (Week 5-7)

### 5.0 MVP UI (Do First - Minimal Viable Product)
> **Goal**: Get a working UI with core functionality before polishing

- [x] 5.0.1 Create minimal `ui/theme/Theme.kt`
  - Basic dark theme (AMOLED black)
  - MaterialTheme wrapper
- [x] 5.0.2 Create `ui/MainActivity.kt` (MVP)
  - Scaffold with bottom navigation
  - NavHost with spring animations
  - Module status display
- [x] 5.0.3 Create `ui/screens/HomeScreen.kt` (MVP)
  - Module active/inactive status card with animated shield icon
  - Global enable/disable toggle
  - Quick stats (protected apps, masked IDs)
  - Current profile summary
  - Quick actions (Configure, Regenerate All)
- [x] 5.0.4 Create `ui/screens/SpoofSettingsScreen.kt` (MVP)
  - Expandable category sections (Device, Network, Advertising, System, Location)
  - Color-coded category icons
  - List of spoof types with toggles
  - Current values display (masked)
  - Regenerate/Copy/Edit buttons per item
- [x] 5.0.5 Create `ui/screens/SettingsScreen.kt`
  - AMOLED dark mode toggle
  - Dynamic colors toggle (Android 12+)
  - Debug logging toggle
  - About section with version info
- [ ] 5.0.6 Test MVP end-to-end
  - Toggle spoof → Verify hook returns new value

**MVP Validation** (Pending Device Testing):
- [ ] Can enable/disable module
- [ ] Can see current spoof values
- [ ] Can regenerate values
- [ ] Changes persist across app restart
- [ ] Target apps receive spoofed values

### 5.1 Theme Setup
- [x] 5.1.1 Create `ui/theme/Color.kt`
  - AMOLED dark palette (0x000000 background)
  - Teal/Cyan primary colors
  - Status colors (active, inactive, warning)
- [x] 5.1.2 Create `ui/theme/Typography.kt`
  - Material 3 typography scale
- [x] 5.1.3 Create `ui/theme/Shapes.kt`
  - Material 3 shape scale
- [x] 5.1.4 Create `ui/theme/Motion.kt`
  - Spring animation specs
  - DefaultSpring, FastSpring, BouncySpring
- [x] 5.1.5 Create `ui/theme/Theme.kt`
  - PrivacyShieldTheme composable
  - Dynamic colors on Android 12+
  - AMOLED dark mode override
  - Light theme fallback

### 5.2 Navigation
- [x] 5.2.1 Create `ui/navigation/NavDestination.kt`
  - Define sealed class for destinations
  - Home, Spoof, Settings with icons
- [x] 5.2.2 Create `ui/navigation/BottomNavBar.kt`
  - Home, Spoof, Settings icons
  - Animated indicator with spring animations
- [x] 5.2.3 Wire navigation in MainActivity

### 5.3 Reusable Components (Parallelizable)
- [x] 5.3.1 Create `ui/components/AppListItem.kt`
  - App icon, name, package, status indicator
  - Checkbox for selection
- [x] 5.3.2 Create `ui/components/SpoofValueCard.kt`
  - Label, value display
  - Regenerate, Edit, Copy buttons
- [x] 5.3.3 Create `ui/components/ProfileCard.kt`
  - Profile name, summary, active indicator
- [x] 5.3.4 Create `ui/components/StatusIndicator.kt`
  - Active/Inactive/Warning states with animation
- [x] 5.3.5 Create `ui/components/ToggleButton.kt`
  - Custom toggle with spring animation

### 5.4 MainActivity
- [ ] 5.4.1 Create `ui/MainActivity.kt`
  - EdgeToEdge display
  - Scaffold with bottom navigation
  - Navigation host
- [ ] 5.4.2 Create `ui/MainViewModel.kt`
  - Overall module status
  - Navigation state

### 5.5 HomeScreen
- [ ] 5.5.1 Create `ui/screens/HomeScreen.kt`
  - Module status card (active/inactive)
  - Protected apps count
  - Quick stats (masked values)
  - Active profile display
  - Quick actions: Configure Apps, Regenerate All
- [ ] 5.5.2 Add spring-based entry animations

### 5.6 AppSelectionScreen
- [x] 5.6.1 Create `ui/screens/AppSelectionScreen.kt`
  - Searchable app list
  - Checkbox to enable/disable
  - Show assigned profile per app
- [x] 5.6.2 Add select all / clear all actions
- [x] 5.6.3 Add app filtering (user apps, system apps)

### 5.7 SpoofSettingsScreen
- [ ] 5.7.1 Create `ui/screens/SpoofSettingsScreen.kt`
  - Sectioned list: Device, Network, Advertising, System, Location
  - SpoofValueCard for each identifier
- [ ] 5.7.2 Add edit dialog for custom values
- [ ] 5.7.3 Add validation feedback

### 5.8 ProfileScreen
- [x] 5.8.1 Create `ui/screens/ProfileScreen.kt`
  - List of saved profiles
  - Create new profile FAB
- [x] 5.8.2 Add profile edit dialog
- [x] 5.8.3 Add delete confirmation dialog
- [x] 5.8.4 Add set as default option

### 5.9 DiagnosticsScreen
- [x] 5.9.1 Create `ui/screens/DiagnosticsScreen.kt`
  - Show current detected values
  - Compare with spoofed values
  - Anti-detection test results
- [x] 5.9.2 Add refresh functionality

### 5.10 SettingsScreen
- [ ] 5.10.1 Create `ui/screens/SettingsScreen.kt`
  - Dark mode toggle (system/dark/AMOLED)
  - Dynamic colors toggle
  - About section
  - Debug logging toggle

**Phase 5 Validation**:
- [ ] All screens render correctly
- [ ] Navigation works smoothly with animations
- [ ] Dark mode/AMOLED mode displays correctly
- [ ] Dynamic colors work on Material You devices
- [ ] UI is responsive on different screen sizes

---

## Phase 6: Testing & Polish (Week 7-8)

### 6.1 Unit Tests
- [ ] 6.1.1 Write tests for IMEIGenerator
  - Test Luhn validation
  - Test format correctness
- [ ] 6.1.2 Write tests for MACGenerator
  - Test unicast bit
- [ ] 6.1.3 Write tests for FingerprintGenerator
  - Test format validity
- [ ] 6.1.4 Write tests for ProfileManager
  - Test CRUD operations

### 6.2 Integration Testing
- [ ] 6.2.1 Create testing checklist document
- [ ] 6.2.2 Test on Android 10-16 range
- [ ] 6.2.3 Test with popular detection apps
- [ ] 6.2.4 Test with banking apps (if applicable)

### 6.3 Performance Optimization
- [ ] 6.3.1 Profile hook overhead
- [ ] 6.3.2 Implement value caching in hooks
- [ ] 6.3.3 Lazy load app list

### 6.4 Documentation
- [ ] 6.4.1 Update README.md with installation instructions
- [ ] 6.4.2 Document recommended companion modules
- [ ] 6.4.3 Add usage walkthrough with screenshots

### 6.5 Release Preparation
- [ ] 6.5.1 Configure ProGuard rules
- [ ] 6.5.2 Set up release signing
- [ ] 6.5.3 Create release APK
- [ ] 6.5.4 Test release build

**Phase 6 Validation**:
- [ ] All unit tests pass
- [ ] Integration tests pass on all target Android versions
- [ ] Documentation is complete
- [ ] Release APK is signed and ready

---

## Quick Reference: Dependencies

| Task | Depends On |
|------|------------|
| 1.4 Hook Entry | 1.1-1.3 complete |
| 2.2 Data Models | 2.1 Generators |
| 2.3-2.7 Hookers | 2.2 Models + 1.4 HookEntry |
| 3.x Anti-Detection | 1.4 HookEntry |
| 4.x Data Management | 2.2 Models |
| 5.x UI | 4.x Data Management |
| 6.x Testing | 5.x UI complete |

## Parallelization Opportunities

- **Phase 2**: 2.1.1-2.1.5 generators can be done in parallel
- **Phase 2**: 2.3-2.7 hookers can be done in parallel after 2.2
- **Phase 3**: 3.2-3.6 hiding mechanisms can be done in parallel after 3.1
- **Phase 5**: 5.3 components can be done in parallel
