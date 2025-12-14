# Change: Implement PrivacyShield LSPosed Module

## Why

The PrivacyShield project is currently an empty Android Studio scaffold. We need to implement the complete LSPosed module for device identifier spoofing with anti-detection capabilities as defined in the PRD v2.0. This module will help users protect their privacy by spoofing device identifiers while remaining undetected by apps attempting to identify Xposed/LSPosed hooks.

## What Changes

### Phase 1: Core Infrastructure
- **Build Configuration**: Update `build.gradle.kts` files with YukiHookAPI, Compose BOM, DataStore, Serialization, Coroutines, KSP
- **LSPosed Metadata**: Add Xposed module metadata to `AndroidManifest.xml`
- **Module Application**: Create `PrivacyShieldApp.kt` with proper initialization
- **Hook Entry**: Implement `HookEntry.kt` with `@InjectYukiHookWithXposed` annotation

### Phase 2: Device Spoofing Layer
- **DeviceHooker.kt**: Hook IMEI, IMSI, Serial, Device ID, Android ID
- **NetworkHooker.kt**: Hook WiFi/Bluetooth MAC, SSID, BSSID, Carrier
- **AdvertisingHooker.kt**: Hook GSF ID, Advertising ID, Media DRM ID
- **SystemHooker.kt**: Hook Build.* fields and SystemProperties
- **LocationHooker.kt**: Hook GPS coordinates, Timezone, Locale

### Phase 3: Anti-Detection Layer
- **AntiDetectHooker.kt**: Orchestrate all anti-detection measures
- **StackTraceHider**: Filter Xposed/LSPosed classes from stack traces
- **ClassLoaderHider**: Block Class.forName() for Xposed classes
- **NativeLibraryHider**: Filter /proc/maps to hide Xposed libraries

### Phase 4: Data Management
- **SpoofDataStore.kt**: DataStore-based preferences storage
- **ProfileManager.kt**: Create/Read/Update/Delete spoof profiles
- **AppScopeManager.kt**: Per-app configuration management
- **Data Models**: SpoofProfile, AppConfig, DeviceIdentifier, SpoofType
- **Generators**: IMEI (Luhn-valid), Serial, MAC, Fingerprint, UUID generators

### Phase 5: User Interface
- **Material 3 Expressive Theme**: Dynamic colors, AMOLED black, spring animations
- **HomeScreen**: Module status, quick stats, active profile
- **AppSelectionScreen**: Select apps to spoof with search
- **SpoofSettingsScreen**: Configure individual spoof values
- **ProfileScreen**: Manage spoof profiles
- **DiagnosticsScreen**: Verify spoofing is working
- **SettingsScreen**: App preferences
- **Navigation**: Bottom navigation with proper back stack

### Phase 6: Testing & Polish
- Unit tests for generators and validators
- Integration testing documentation
- Performance optimization
- Documentation

## Impact

- **Affected specs**: 
  - `core-infrastructure` (NEW)
  - `device-spoofing` (NEW)
  - `anti-detection` (NEW)
  - `data-management` (NEW)
  - `user-interface` (NEW)
  
- **Affected code**:
  - `build.gradle.kts` (root & app)
  - `settings.gradle.kts`
  - `libs.versions.toml`
  - `AndroidManifest.xml`
  - `app/src/main/kotlin/com/akil/privacyshield/**` (entire source tree)
  - `app/src/main/res/**` (resources)

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Language | Kotlin | 2.2.21 |
| Android SDK | compileSdk/targetSdk | 36 (Android 16) |
| Min SDK | minSdk | 26 (Android 8.0) |
| Java | JDK | 21 |
| Hook Framework | YukiHookAPI + KSP | 1.2.1 |
| UI | Jetpack Compose + Material 3 | BOM 2025.12.00 |
| Data Storage | DataStore Preferences | 1.1.2 |
| Serialization | Kotlinx Serialization JSON | 1.8.0 |
| Async | Kotlinx Coroutines | 1.9.0 |
| Logging | Timber | 5.0.1 |
| Image Loading | Coil Compose | 2.7.0 |

## Success Criteria

1. Module loads successfully in LSPosed Manager
2. All 24+ device identifiers can be spoofed
3. Anti-detection layer hides hooks from detection apps
4. Material 3 Expressive UI is responsive and polished
5. Per-app profiles work correctly
6. Values regenerate with valid formats (Luhn-valid IMEI, unicast MAC, etc.)
