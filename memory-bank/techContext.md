# Technical Context: Device Masker

## Technology Stack

### Core Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **Kotlin** | 2.2.21 | Primary language |
| **Android SDK** | API 36 (compileSdk) | Android 16 support |
| **Min SDK** | API 26 (Android 8.0) | Broader device compatibility |
| **Target SDK** | API 36 | Latest Android 16 |
| **Java Version** | 21 (Target) / 25 (Host) | Toolchain provisioning via Foojay |
| **Gradle** | 9.1.0 (Wrapper) | Build system (Java 25 compatible) |

### UI Framework

| Library | Version | Purpose |
|---------|---------|---------|
| **Compose BOM** | 2025.12.00 | December 2025 release |
| **Compose UI** | BOM | Core UI toolkit |
| **Material 3** | 1.5.0-alpha11 | Material 3 Expressive |
| **Graphics Shapes** | 1.0.1 | Shape morphing support |
| **Activity Compose** | 1.12.1 | Activity integration |
| **Navigation Compose** | 2.9.6 | Navigation component |
| **Lifecycle** | 2.10.0 | Lifecycle-aware components |

### Hooking Framework

| Library | Version | Purpose |
|---------|---------|---------|
| **YukiHookAPI** | 1.3.1 | Modern Kotlin Hook API |
| **KavaRef** | 1.0.2 | Reflection engine (required by Yuki v1.3+) |
| **AndroidHiddenApiBypass** | 6.1 | Access to hidden APIs |
| **YukiHookAPI KSP** | 2.2.21-2.0.4 | Annotation processor |
| **LSPosed (Xposed)** | 1.10.2+ (API 82) | Framework (external) |
| **Magisk** | 30.6+ | Root solution (external) |

### Data & Utilities

| Library | Version | Purpose |
|---------|---------|---------|
| **DataStore** | 1.2.0 | Preferences storage |
| **Kotlinx Coroutines** | 1.10.2 | Async operations |
| **Kotlinx Serialization** | 1.9.0 | JSON serialization |
| **Timber** | 5.0.1 | Logging |
| **Coil Compose** | 3.2.0 | Image loading (app icons) |

## Development Setup

### Prerequisites

1. **Android Studio** Ladybug (2024.2.1) or newer
2. **JDK 25** (Host) & **JDK 21** (Target, auto-provisioned)
3. **Android SDK** with API 36 platform tools
4. **Rooted Android Device** with:
   - Magisk 30.6+
   - Zygisk enabled
   - LSPosed 1.10.2+ installed

### Project Setup

```bash
# Clone repository
git clone https://github.com/astrixforge/devicemasker.git
cd Device Masker

# Open in Android Studio
# File → Open → select Device Masker directory

# Sync Gradle (automatic or manual)
# Build → Make Project
```

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Install to connected device
./gradlew installDebug

# Run tests
./gradlew test
```

### IDE Configuration

- **Code Style**: Kotlin official style (Settings → Editor → Code Style → Kotlin)
- **Inspections**: Enable Compose-specific inspections
- **Plugins**: Kotlin, Compose Multiplatform (optional)

## Technical Constraints

### Android Version Compatibility

| API Level | Android Version | Support Status |
|-----------|-----------------|----------------|
| 26-28 | Android 8.0-9.0 | ✅ Supported |
| 29-30 | Android 10-11 | ✅ Supported |
| 31-32 | Android 12-12L | ✅ Supported (dynamic colors) |
| 33 | Android 13 | ✅ Supported |
| 34 | Android 14 | ✅ Supported |
| 35 | Android 15 | ✅ Supported |
| 36 | Android 16 | ✅ Supported (target) |

### LSPosed Requirements

- **Minimum Xposed Version**: 93 (required for XSharedPreferences)
- **Module Scope**: User-selected apps (not system-wide)
- **Cross-Process Data**: XSharedPreferences via YukiHookAPI `prefs` property

**Required AndroidManifest.xml meta-data**:
```xml
<meta-data android:name="xposedmodule" android:value="true" />
<meta-data android:name="xposedsharedprefs" android:value="true" />
<meta-data android:name="xposedminversion" android:value="93" />
```

**XSharedPreferences Architecture** (Dec 24, 2025):
| Component | Module | Purpose |
|-----------|--------|---------|
| `SharedPrefsKeys` | `:common` | Shared key generator for app ↔ hooks |
| `XposedPrefs` | `:app` | Write with MODE_WORLD_READABLE |
| `ConfigSync` | `:app` | Sync JsonConfig → per-app keys |
| `PrefsKeys` | `:xposed` | YukiHookAPI PrefsData definitions |
| `PrefsReader` | `:xposed` | PrefsHelper for hook access |

### Performance Constraints

- Hook methods must execute quickly (<1ms overhead)
- DataStore reads should be cached in hooks
- Anti-detection must run before spoofing hooks
- UI must maintain 60fps animations

### Security Constraints

- Never crash target apps (use `optional()` for uncertain methods)
- Generate valid format values (Luhn IMEI, unicast MAC)
- Hide all Xposed-related patterns from detection

### Android 16 (API 36) Specific Issues

> **Issue Discovered**: Dec 15, 2025

| Issue | Symptom | Solution |
|-------|---------|----------|
| **Sealed Class Navigation Crash** | `NullPointerException` on `NavDestination.getRoute()` during Compose recomposition | Use `data class` + string constants instead of sealed class `object` declarations |

**Root Cause**: Sealed class `object` declarations (e.g., `object Home : NavDestination()`) 
can fail to initialize properly during Jetpack Compose's recomposition cycle on Android 16. 
When a lambda captures these objects (e.g., in `NavigationBar`), they may be null.

**Solution Applied**: 
```kotlin
// Replace sealed class objects with data class + string constants
object NavRoutes { const val HOME = "home" }
data class NavItem(val route: String, val label: String, ...)
val bottomNavItems: List<NavItem> = listOf(...)
```

## Dependencies

### Gradle Version Catalog (`libs.versions.toml`)

```toml
[versions]
# Core
agp = "8.13.2"
kotlin = "2.2.21"
ksp = "2.2.21-2.0.4"

# Android
coreKtx = "1.17.0"
appcompat = "1.7.1"
activityCompose = "1.12.1"
lifecycleRuntimeKtx = "2.10.0"
navigationCompose = "2.9.6"

# Compose
composeBom = "2025.12.00"
material3 = "1.5.0-alpha11"

# YukiHookAPI
material = "1.13.0"
yukihookapi = "1.3.1"
kavaref = "1.0.2"
hiddenapibypass = "6.1"

# Data
datastore = "1.2.0"
serializationJson = "1.9.0"
coroutines = "1.10.2"

# Utilities
timber = "5.0.1"
coilCompose = "3.2.0"

# Testing
junit = "4.13.2"
junitVersion = "1.2.1"
espressoCore = "3.7.0"

[libraries]
# ... (library definitions)

[plugins]
# ... (plugin definitions)
```

### Repository Configuration

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://repo.lsposed.foundation/") } // Xposed API
    }
}
```

## Tool Usage Patterns

### YukiHookAPI Patterns

```kotlin
// Entry point annotation (in :app module only)
@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {
    override fun onInit() = configs {
        debugLog { tag = "Device Masker"; isEnable = BuildConfig.DEBUG }
    }
    
    override fun onHook() = encase {
        // Load XposedHookLoader from :xposed module
        loadHooker(XposedHookLoader)
    }
}

// Hooker class pattern (in :xposed module)
object DeviceHooker : YukiBaseHooker() {
    override fun onHook() {
        "android.telephony.TelephonyManager".toClass().apply {
            method { name = "getImei" }.hook {
                after { 
                    result = getSpoofValue(SpoofType.IMEI) { fallbackImei }
                }
            }
        }
    }
    
    private fun getSpoofValue(type: SpoofType, fallback: () -> String): String {
        val service = DeviceMaskerService.instance ?: return fallback()
        val group = service.config.getGroupForApp(packageName) ?: return fallback()
        return group.getValue(type) ?: fallback()
    }
}
```

### HMA-OSS Architecture Pattern

```kotlin
// AIDL Service (in :xposed module, runs in system_server)
class DeviceMaskerService : IDeviceMaskerService.Stub() {
    companion object {
        @Volatile var instance: DeviceMaskerService? = null
        
        fun init() {
            instance = DeviceMaskerService().apply {
                loadConfig()
            }
        }
    }
    
    var config: JsonConfig = JsonConfig.createDefault()
        private set
    
    override fun readConfig(): String = config.toJsonString()
    override fun writeConfig(json: String) {
        config = JsonConfig.parse(json)
        saveConfigInternal(config)
    }
}

// ServiceClient (in :app module, for UI communication)
class ServiceClient(private val binder: IBinder) {
    private val service: IDeviceMaskerService = 
        IDeviceMaskerService.Stub.asInterface(binder)
    
    fun readConfig(): JsonConfig = JsonConfig.parse(service.readConfig())
    fun writeConfig(config: JsonConfig) = service.writeConfig(config.toJsonString())
}
```

### Compose State Pattern

```kotlin
// ViewModel
class SpoofViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SpoofUiState())
    val uiState: StateFlow<SpoofUiState> = _uiState.asStateFlow()
    
    fun regenerateIMEI() {
        viewModelScope.launch {
            val newImei = IMEIGenerator.generate()
            repository.setIMEI(newImei)
            _uiState.update { it.copy(imei = newImei) }
        }
    }
}

// Immutable state
data class SpoofUiState(
    val imei: String = "",
    val serial: String = "",
    val isLoading: Boolean = false
)
```

## External Services & Dependencies

### External Modules (User Installed)

| Module | Purpose | Repository |
|--------|---------|------------|
| Shamiko | Root hiding, Zygisk deny list | LSPosed/Shamiko |
| Play Integrity Fix | Pass Play Integrity checks | chiteroman/PlayIntegrityFix |
| Tricky Store | Hardware attestation | 5ec1cff/TrickyStore |
| Zygisk-Next | Zygisk on KernelSU/APatch | Dr-TSNG/ZygiskNext |

### No External API Calls

Device Masker operates entirely offline. No network requests are made to external services. All data stays on device.

## File Structure (Updated Dec 24, 2025 - XSharedPreferences Config Complete)

```
devicemasker/
├── app/                                    # Main application (UI + Entry)
│   ├── src/main/kotlin/com/astrixforge/devicemasker/
│   │   ├── DeviceMaskerApp.kt             # Application class (initializes ConfigManager)
│   │   ├── hook/
│   │   │   └── HookEntry.kt               # @InjectYukiHookWithXposed (delegates to :xposed)
│   │   ├── service/                        # Service Layer
│   │   │   ├── ServiceClient.kt           # AIDL proxy (legacy, kept for compatibility)
│   │   │   ├── ServiceProvider.kt         # ContentProvider for binder delivery
│   │   │   └── ConfigManager.kt           # StateFlow config manager + ConfigSync integration
│   │   ├── data/
│   │   │   ├── SettingsDataStore.kt       # UI settings only (theme, AMOLED)
│   │   │   ├── XposedPrefs.kt             # ⭐ NEW: MODE_WORLD_READABLE SharedPreferences writer
│   │   │   ├── ConfigSync.kt              # ⭐ NEW: Syncs JsonConfig → XposedPrefs per-app keys
│   │   │   ├── models/
│   │   │   │   ├── TypeAliases.kt         # Backward compat for old imports
│   │   │   │   └── InstalledApp.kt        # App model for UI
│   │   │   └── repository/
│   │   │       ├── SpoofRepository.kt     # Bridge to ConfigManager
│   │   │       └── AppScopeRepository.kt  # Installed apps access
│   │   ├── ui/                             # UI layer (M3 Expressive + MVVM)
│   │   │   ├── MainActivity.kt
│   │   │   ├── theme/                     # Motion, Colors, Shapes
│   │   │   ├── screens/                   # Feature-based MVVM screens
│   │   │   │   ├── home/                  # Home (dashboard)
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   ├── HomeState.kt
│   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   ├── settings/              # App settings
│   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   ├── SettingsState.kt
│   │   │   │   │   └── SettingsViewModel.kt
│   │   │   │   ├── groups/                # Group list/CRUD
│   │   │   │   │   ├── GroupsScreen.kt
│   │   │   │   │   ├── GroupsState.kt
│   │   │   │   │   └── GroupsViewModel.kt
│   │   │   │   ├── groupspoofing/         # Group spoof values
│   │   │   │   │   ├── GroupSpoofingScreen.kt
│   │   │   │   │   ├── GroupSpoofingState.kt
│   │   │   │   │   └── GroupSpoofingViewModel.kt
│   │   │   │   └── diagnostics/           # Diagnostics/testing
│   │   │   │       ├── DiagnosticsScreen.kt
│   │   │   │       ├── DiagnosticsState.kt
│   │   │   │       └── DiagnosticsViewModel.kt
│   │   │   ├── components/                # Reusable + Expressive
│   │   │   └── navigation/                # Nav routes
│   │   └── utils/
│   ├── src/main/AndroidManifest.xml        # ServiceProvider + LSPosed metadata + xposedsharedprefs
│   └── build.gradle.kts                    # YukiHookAPI KSP enabled
│
├── common/                                 # Shared models & AIDL
│   ├── src/main/aidl/com/astrixforge/devicemasker/common/
│   │   └── IDeviceMaskerService.aidl      # AIDL interface (legacy)
│   ├── src/main/kotlin/com/astrixforge/devicemasker/common/
│   │   ├── SpoofType.kt                   # @Serializable enum
│   │   ├── SpoofCategory.kt               # Categories
│   │   ├── DeviceIdentifier.kt            # @Serializable data class
│   │   ├── SpoofGroup.kt                  # @Serializable data class
│   │   ├── DeviceProfilePreset.kt         # Predefined device profiles
│   │   ├── AppConfig.kt                   # @Serializable data class
│   │   ├── JsonConfig.kt                  # Root config container
│   │   ├── SharedPrefsKeys.kt             # ⭐ NEW: Shared key generator for XSharedPreferences
│   │   ├── Constants.kt                   # Shared constants
│   │   ├── Utils.kt                       # Validation utilities
│   │   ├── models/                        # Internal Config models
│   │   │   ├── SIMConfig.kt               # Correlated SIM values
│   │   │   ├── LocationConfig.kt          # Correlated location values
│   │   │   ├── DeviceHardwareConfig.kt    # Correlated hardware values
│   │   │   └── Carrier.kt                 # Carrier database
│   │   └── generators/                    # Value Generators
│   │       ├── IMEIGenerator.kt           # IMEI with Luhn checksum
│   │       ├── SerialGenerator.kt         # Manufacturer patterns
│   │       ├── MACGenerator.kt            # WiFi/Bluetooth MAC
│   │       ├── UUIDGenerator.kt           # Android ID, GSF ID, Advertising ID
│   │       ├── IMSIGenerator.kt           # MCC/MNC combinations
│   │       ├── ICCIDGenerator.kt          # SIM card ID with Luhn
│   │       ├── SIMGenerator.kt            # Correlated SIM config
│   │       ├── DeviceHardwareGenerator.kt # Correlated hardware config
│   │       └── FingerprintGenerator.kt    # Build fingerprints
│   └── build.gradle.kts                   # android-library
│
├── xposed/                                 # Xposed module logic
│   ├── src/main/kotlin/com/astrixforge/devicemasker/xposed/
│   │   ├── XposedEntry.kt                 # ⭐ UPDATED: Uses prefs property for config
│   │   ├── PrefsKeys.kt                   # ⭐ NEW: YukiHookAPI PrefsData definitions
│   │   ├── PrefsReader.kt                 # ⭐ NEW: PrefsHelper object for hooks
│   │   ├── XposedHookLoader.kt            # YukiBaseHooker (loaded by app)
│   │   ├── DeviceMaskerService.kt         # AIDL implementation (legacy)
│   │   ├── ServiceHelper.kt               # Binder access (legacy)
│   │   ├── Logcat.kt                      # Safe logging
│   │   └── hooker/
│   │       ├── AntiDetectHooker.kt        # Xposed hiding (FIRST)
│   │       ├── DeviceHooker.kt            # ⭐ UPDATED: Uses PrefsHelper
│   │       ├── NetworkHooker.kt           # ⭐ UPDATED: Uses PrefsHelper
│   │       ├── AdvertisingHooker.kt       # ⭐ UPDATED: Uses PrefsHelper
│   │       ├── SystemHooker.kt            # ⭐ UPDATED: Uses PrefsHelper
│   │       └── LocationHooker.kt          # ⭐ UPDATED: Uses PrefsHelper
│   └── build.gradle.kts                   # android-library (no KSP)
│
├── build.gradle.kts                        # Root build script
├── settings.gradle.kts                     # Includes :app, :common, :xposed
├── gradle/
│   └── libs.versions.toml                  # Dependency catalog
├── docs/                                   # Documentation
├── openspec/                               # OpenSpec specs
│   └── changes/archive/                    # Archived changes (8 total)
│       └── 2025-12-22-refactor-mvvm-architecture/  # Latest
└── memory-bank/                            # Memory Bank
```

## Module Dependencies

```kotlin
// settings.gradle.kts
include(":app", ":common", ":xposed")

// app/build.gradle.kts
dependencies {
    implementation(project(":common"))
    implementation(project(":xposed"))
    ksp(libs.yukihookapi.ksp.xposed)  // KSP only in app
}

// xposed/build.gradle.kts
dependencies {
    implementation(project(":common"))
    implementation(libs.yukihookapi.api)  // API only, no KSP
}

// common/build.gradle.kts
dependencies {
    implementation(libs.kotlinx.serialization.json)
}
```


