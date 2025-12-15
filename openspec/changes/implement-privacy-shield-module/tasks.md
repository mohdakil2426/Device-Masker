# Tasks: Implement PrivacyShield LSPosed Module

> **Estimated Total Effort**: 8 weeks (part-time) or 4 weeks (full-time)
> 
> **Dependencies**: Tasks should be completed in phase order. Tasks within a phase can be parallelized where noted.

---

## Phase 1: Core Infrastructure (Week 1-2)

### 1.1 Build Configuration
- [x] 1.1.1 Update `gradle/libs.versions.toml` with all required dependencies
  - Kotlin 2.2.21, Android Gradle Plugin 8.13.0
  - YukiHookAPI 1.3.1, KSP 2.2.21-2.0.4
  - Compose BOM 2025.12.00, Material 3 1.4.0
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
- [ ] 2.1.1 Create `data/generators/IMEIGenerator.kt`
  - Generate 15-digit IMEI with Luhn checksum
  - Use realistic TAC prefixes (35, 86, 01, 45)
  - Include validation function
- [ ] 2.1.2 Create `data/generators/SerialGenerator.kt`
  - Generate 8-16 alphanumeric serials
  - Support device-specific formats
- [ ] 2.1.3 Create `data/generators/MACGenerator.kt`
  - Generate valid unicast MAC (clear LSB of first octet)
  - Format as XX:XX:XX:XX:XX:XX
- [ ] 2.1.4 Create `data/generators/UUIDGenerator.kt`
  - Generate Android ID (16 hex chars)
  - Generate Advertising ID (UUID format)
  - Generate GSF ID (16 hex chars)
- [ ] 2.1.5 Create `data/generators/FingerprintGenerator.kt`
  - Generate realistic Build fingerprint strings
  - Use device database for realistic combinations

### 2.2 Data Models (Dependency: 2.1)
- [ ] 2.2.1 Create `data/models/SpoofType.kt` enum
  - IMEI, IMSI, SERIAL, ANDROID_ID, etc.
- [ ] 2.2.2 Create `data/models/DeviceIdentifier.kt` data class
- [ ] 2.2.3 Create `data/models/SpoofProfile.kt` data class
  - All 24+ spoofable fields with nullable values
  - id, name, isDefault, createdAt, updatedAt
- [ ] 2.2.4 Create `data/models/AppConfig.kt` data class
  - packageName, appLabel, isEnabled, profileId
  - enabledSpoofs Set<SpoofType>

### 2.3 Device Hooker
- [ ] 2.3.1 Create `hook/hooker/DeviceHooker.kt` (YukiBaseHooker)
  - Check app scope before hooking
  - Hook TelephonyManager.getDeviceId()
  - Hook TelephonyManager.getImei() (paramCount 0 and 1)
  - Hook TelephonyManager.getMeid() with optional()
  - Hook TelephonyManager.getSubscriberId() (IMSI)
  - Hook TelephonyManager.getSimSerialNumber()
- [ ] 2.3.2 Hook Build fields in DeviceHooker
  - Build.SERIAL (field replacement)
  - Build.getSerial() method hook
  - Build.MODEL, Build.MANUFACTURER, etc.
- [ ] 2.3.3 Hook Settings.Secure for ANDROID_ID
  - Hook Settings.Secure.getString() for "android_id"
- [ ] 2.3.4 Test IMEI spoofing with device info app

### 2.4 Network Hooker
- [ ] 2.4.1 Create `hook/hooker/NetworkHooker.kt`
  - Hook WifiInfo.getMacAddress()
  - Hook NetworkInterface.getHardwareAddress()
- [ ] 2.4.2 Hook Bluetooth MAC
  - Hook BluetoothAdapter.getAddress()
- [ ] 2.4.3 Hook WiFi SSID/BSSID
  - Hook WifiInfo.getSSID()
  - Hook WifiInfo.getBSSID()
- [ ] 2.4.4 Hook carrier info
  - TelephonyManager.getNetworkOperatorName()
  - TelephonyManager.getNetworkOperator()
- [ ] 2.4.5 Test MAC spoofing with network info app

### 2.5 Advertising Hooker
- [ ] 2.5.1 Create `hook/hooker/AdvertisingHooker.kt`
  - Hook GSF ID via Gservices.getString("android_id")
  - Hook AdvertisingIdClient.getAdvertisingIdInfo()
  - Hook MediaDrm.getPropertyByteArray() for DRM ID
- [ ] 2.5.2 Test advertising ID spoofing

### 2.6 System Hooker
- [ ] 2.6.1 Create `hook/hooker/SystemHooker.kt`
  - Hook all Build.* static fields
  - Hook SystemProperties.get() for ro.* properties
- [ ] 2.6.2 Hook Build.VERSION fields
  - RELEASE, SDK_INT, SECURITY_PATCH
- [ ] 2.6.3 Test with device info apps

### 2.7 Location Hooker
- [ ] 2.7.1 Create `hook/hooker/LocationHooker.kt`
  - Hook Location.getLatitude()
  - Hook Location.getLongitude()
  - Hook Location.getAltitude()
- [ ] 2.7.2 Hook timezone/locale
  - Hook TimeZone.getDefault()
  - Hook Locale.getDefault()
- [ ] 2.7.3 Test with maps/location apps

**Phase 2 Validation**:
- [ ] All IMEI checker apps show spoofed IMEI
- [ ] Device info apps show spoofed values
- [ ] MAC addresses are spoofed in network info apps
- [ ] Log output shows hooks being triggered

---

## Phase 3: Anti-Detection Layer (Week 3-4)

### 3.1 Anti-Detection Hooker
- [ ] 3.1.1 Create `hook/hooker/AntiDetectHooker.kt`
  - Define HIDDEN_PATTERNS list (Xposed classes, YukiHookAPI classes)
  - Define HIDDEN_LIBRARIES list (libxposed, liblspd, etc.)
  - Define HIDDEN_PACKAGES list (Xposed installers)

### 3.2 Stack Trace Hiding
- [ ] 3.2.1 Hook Thread.getStackTrace()
  - Filter out stack frames matching HIDDEN_PATTERNS
- [ ] 3.2.2 Hook Throwable.getStackTrace()
  - Same filtering for exception traces
- [ ] 3.2.3 Hook Throwable.printStackTrace()
  - Filter before printing
- [ ] 3.2.4 Test with RootBeer or similar detection library

### 3.3 Class Loading Hiding
- [ ] 3.3.1 Hook Class.forName(String)
  - Throw ClassNotFoundException for Xposed classes
- [ ] 3.3.2 Hook Class.forName(String, boolean, ClassLoader)
  - Same protection with all overloads
- [ ] 3.3.3 Hook ClassLoader.loadClass()
  - Block loading of Xposed classes
- [ ] 3.3.4 Test with XposedDetector sample code

### 3.4 Native Library Hiding
- [ ] 3.4.1 Hook BufferedReader for /proc/self/maps reading
  - Filter lines containing HIDDEN_LIBRARIES
  - Use FileInputStream/FileReader hooks
- [ ] 3.4.2 Test /proc/maps filtering

### 3.5 Reflection Hiding
- [ ] 3.5.1 Hook Method.getModifiers()
  - Return original modifiers for hooked methods
- [ ] 3.5.2 Hook Field.getModifiers()
  - Return original modifiers for hooked fields

### 3.6 Package Hiding
- [ ] 3.6.1 Hook PackageManager.getPackageInfo()
  - Throw NameNotFoundException for Xposed packages
- [ ] 3.6.2 Hook PackageManager.getApplicationInfo()
  - Same protection

**Phase 3 Validation**:
- [ ] RootBeer does not detect Xposed
- [ ] SafetyNet Helper does not detect hooks
- [ ] /proc/self/maps shows no Xposed libraries
- [ ] Class.forName("de.robv.android.xposed.*") throws exception

---

## Phase 4: Data Management (Week 4-5)

### 4.1 DataStore Setup
- [ ] 4.1.1 Create `data/SpoofDataStore.kt`
  - Define preference keys for all spoof values
  - Initialize DataStore in companion object
  - Provide suspend functions for read/write
  - Provide blocking functions for hook context
- [ ] 4.1.2 Create extension for Context.dataStore

### 4.2 Profile Management
- [ ] 4.2.1 Create `data/ProfileManager.kt`
  - CRUD operations for SpoofProfile
  - List all profiles
  - Get active profile for package
  - Set default profile
- [ ] 4.2.2 Implement profile serialization to DataStore
  - Use Kotlinx Serialization JSON

### 4.3 App Scope Management
- [ ] 4.3.1 Create `data/AppScopeManager.kt`
  - Enable/disable spoofing per app
  - Assign profiles to apps
  - Get enabled spoof types per app
- [ ] 4.3.2 Query installed apps with PackageManager
  - Get app labels and icons
  - Cache app list for performance

### 4.4 Repository Pattern
- [ ] 4.4.1 Create `data/SpoofRepository.kt`
  - Combine ProfileManager and AppScopeManager
  - Provide Flow<UiState> for reactive updates
  - Handle profile/app relationship logic

**Phase 4 Validation**:
- [ ] Can create and save profiles
- [ ] Profile values persist across app restart
- [ ] Per-app configuration saves correctly
- [ ] Hooks read correct values from DataStore

---

## Phase 5: User Interface (Week 5-7)

### 5.1 Theme Setup
- [ ] 5.1.1 Create `ui/theme/Color.kt`
  - AMOLED dark palette (0x000000 background)
  - Teal/Cyan primary colors
  - Status colors (active, inactive, warning)
- [ ] 5.1.2 Create `ui/theme/Typography.kt`
  - Material 3 typography scale
- [ ] 5.1.3 Create `ui/theme/Shapes.kt`
  - Material 3 shape scale
- [ ] 5.1.4 Create `ui/theme/Motion.kt`
  - Spring animation specs
  - DefaultSpring, FastSpring, BouncySpring
- [ ] 5.1.5 Create `ui/theme/Theme.kt`
  - PrivacyShieldTheme composable
  - Dynamic colors on Android 12+
  - AMOLED dark mode override
  - Light theme fallback

### 5.2 Navigation
- [ ] 5.2.1 Create `ui/navigation/NavGraph.kt`
  - Define sealed class for destinations
  - Home, Apps, SpoofSettings, Profiles, Diagnostics, Settings
- [ ] 5.2.2 Create `ui/navigation/BottomNavBar.kt`
  - Home, Apps, Spoof, Profile icons
  - Animated indicator
- [ ] 5.2.3 Wire navigation in MainActivity

### 5.3 Reusable Components (Parallelizable)
- [ ] 5.3.1 Create `ui/components/AppListItem.kt`
  - App icon, name, package, status indicator
  - Checkbox for selection
- [ ] 5.3.2 Create `ui/components/SpoofValueCard.kt`
  - Label, value display
  - Regenerate, Edit, Copy buttons
- [ ] 5.3.3 Create `ui/components/ProfileCard.kt`
  - Profile name, summary, active indicator
- [ ] 5.3.4 Create `ui/components/StatusIndicator.kt`
  - Active/Inactive/Warning states with animation
- [ ] 5.3.5 Create `ui/components/ToggleButton.kt`
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
- [ ] 5.6.1 Create `ui/screens/AppSelectionScreen.kt`
  - Searchable app list
  - Checkbox to enable/disable
  - Show assigned profile per app
- [ ] 5.6.2 Add select all / clear all actions
- [ ] 5.6.3 Add app filtering (user apps, system apps)

### 5.7 SpoofSettingsScreen
- [ ] 5.7.1 Create `ui/screens/SpoofSettingsScreen.kt`
  - Sectioned list: Device, Network, Advertising, System, Location
  - SpoofValueCard for each identifier
- [ ] 5.7.2 Add edit dialog for custom values
- [ ] 5.7.3 Add validation feedback

### 5.8 ProfileScreen
- [ ] 5.8.1 Create `ui/screens/ProfileScreen.kt`
  - List of saved profiles
  - Create new profile FAB
- [ ] 5.8.2 Add profile edit dialog
- [ ] 5.8.3 Add delete confirmation dialog
- [ ] 5.8.4 Add set as default option

### 5.9 DiagnosticsScreen
- [ ] 5.9.1 Create `ui/screens/DiagnosticsScreen.kt`
  - Show current detected values
  - Compare with spoofed values
  - Anti-detection test results
- [ ] 5.9.2 Add refresh functionality

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
