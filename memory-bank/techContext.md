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

- **Minimum Xposed Version**: 82
- **Module Scope**: User-selected apps (not system-wide)
- **Cross-Process Data**: XSharedPreferences or YukiHookAPI DataChannel

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
// Entry point annotation
@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {
    override fun onInit() = configs {
        debugLog { tag = "Device Masker"; isEnable = BuildConfig.DEBUG }
    }
    
    override fun onHook() = encase {
        loadHooker(AntiDetectHooker)  // First!
        loadHooker(DeviceHooker)
        loadHooker(NetworkHooker)
    }
}

// Hooker class pattern
object DeviceHooker : YukiBaseHooker() {
    override fun onHook() {
        "android.telephony.TelephonyManager".toClass().apply {
            method { name = "getImei" }.hook {
                after { result = SpoofDataStore.getSpoofedIMEI() }
            }
        }
    }
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

### DataStore Pattern

```kotlin
object SpoofDataStore {
    private object Keys {
        val IMEI = stringPreferencesKey("spoofed_imei")
    }
    
    suspend fun getSpoofedIMEI(): String {
        return dataStore.data.first()[Keys.IMEI] ?: IMEIGenerator.generate()
    }
    
    // Blocking for hook context
    fun getSpoofedIMEIBlocking(): String = runBlocking { getSpoofedIMEI() }
}
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

## File Structure (Updated Dec 19, 2025)

```
devicemasker/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/astrixforge/devicemasker/
│   │   │   │   ├── DeviceMaskerApp.kt             # Application class
│   │   │   │   ├── hook/                           # YukiHookAPI layer
│   │   │   │   │   ├── HookEntry.kt
│   │   │   │   │   └── hooker/
│   │   │   │   │       ├── DeviceHooker.kt
│   │   │   │   │       ├── NetworkHooker.kt
│   │   │   │   │       ├── AdvertisingHooker.kt
│   │   │   │   │       ├── SystemHooker.kt
│   │   │   │   │       ├── LocationHooker.kt
│   │   │   │   │       └── AntiDetectHooker.kt
│   │   │   │   ├── data/                           # Data layer
│   │   │   │   │   ├── SpoofDataStore.kt
│   │   │   │   │   ├── ProfileManager.kt
│   │   │   │   │   ├── AppScopeManager.kt
│   │   │   │   │   ├── SpoofRepository.kt
│   │   │   │   │   ├── models/
│   │   │   │   │   └── generators/
│   │   │   │   ├── ui/                             # UI layer
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── MainViewModel.kt
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   ├── Motion.kt               # AppMotion spring specs
│   │   │   │   │   │   ├── Shapes.kt
│   │   │   │   │   │   └── Type.kt
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   ├── ProfileScreen.kt
│   │   │   │   │   │   ├── ProfileDetailScreen.kt
│   │   │   │   │   │   ├── SettingsScreen.kt
│   │   │   │   │   │   └── DiagnosticsScreen.kt
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── ProfileCard.kt
│   │   │   │   │   │   ├── SpoofValueCard.kt
│   │   │   │   │   │   ├── StatusIndicator.kt
│   │   │   │   │   │   ├── ToggleButton.kt
│   │   │   │   │   │   ├── IconCircle.kt           # Circular icon container
│   │   │   │   │   │   ├── ScreenHeader.kt         # Consistent headers
│   │   │   │   │   │   ├── SettingsItem.kt         # Settings row variants
│   │   │   │   │   │   ├── EmptyState.kt           # Empty list placeholder
│   │   │   │   │   │   ├── StatCard.kt             # Dashboard stat cards
│   │   │   │   │   │   ├── ValueRow.kt             # Label + value pairs
│   │   │   │   │   │   ├── AppListItem.kt          # App selection rows
│   │   │   │   │   │   ├── dialog/                 # Dialog components
│   │   │   │   │   │   │   └── StandardDialogs.kt  # Confirmation dialogs
│   │   │   │   │   │   └── expressive/             # M3 Expressive components
│   │   │   │   │   │       ├── AnimatedSection.kt
│   │   │   │   │   │       ├── ExpressiveCard.kt
│   │   │   │   │   │       ├── ExpressiveIconButton.kt
│   │   │   │   │   │       ├── ExpressiveLoadingIndicator.kt
│   │   │   │   │   │       ├── ExpressivePullToRefresh.kt
│   │   │   │   │   │       ├── ExpressiveSwitch.kt
│   │   │   │   │   │       ├── MorphingShape.kt
│   │   │   │   │   │       ├── QuickActionGroup.kt
│   │   │   │   │   │       ├── SectionHeader.kt
│   │   │   │   │   │       └── StatusIndicator.kt
│   │   │   │   │   └── navigation/
│   │   │   │   └── utils/                          # Utilities
│   │   │   │       ├── Constants.kt
│   │   │   │       └── ImageUtils.kt               # Drawable→Bitmap conversion
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── docs/                                            # Documentation
│   ├── rules/                                       # Development rules
│   ├── developer-android-docs/                      # Android best practices
│   └── material-ui/                                 # M3 Expressive rules
├── openspec/                                        # OpenSpec specs
└── memory-bank/                                     # Memory Bank
```

