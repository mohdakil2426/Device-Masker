# Technical Context: PrivacyShield

## Technology Stack

### Core Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **Kotlin** | 2.2.21 | Primary language |
| **Android SDK** | API 36 (compileSdk) | Android 16 support |
| **Min SDK** | API 26 (Android 8.0) | Broader device compatibility |
| **Target SDK** | API 36 | Latest Android 16 |
| **Java Version** | 21 | Latest LTS |
| **Gradle** | 8.7.3 (AGP) | Build system |

### UI Framework

| Library | Version | Purpose |
|---------|---------|---------|
| **Compose BOM** | 2025.12.00 | December 2025 release |
| **Compose UI** | 1.10.0 | Core UI toolkit |
| **Material 3** | 1.4.0 | Material 3 Expressive |
| **Activity Compose** | 1.10.0 | Activity integration |
| **Navigation Compose** | 2.9.0 | Navigation component |
| **Lifecycle** | 2.9.0 | Lifecycle-aware components |

### Hooking Framework

| Library | Version | Purpose |
|---------|---------|---------|
| **YukiHookAPI** | 1.2.1 | Modern Kotlin Hook API |
| **YukiHookAPI KSP** | 1.2.1 | Annotation processor |
| **LSPosed** | 1.10.2+ | Framework (external) |
| **Magisk** | 30.6+ | Root solution (external) |

### Data & Utilities

| Library | Version | Purpose |
|---------|---------|---------|
| **DataStore** | 1.1.2 | Preferences storage |
| **Kotlinx Coroutines** | 1.9.0 | Async operations |
| **Kotlinx Serialization** | 1.8.0 | JSON serialization |
| **Timber** | 5.0.1 | Logging |
| **Coil Compose** | 2.7.0 | Image loading (app icons) |

## Development Setup

### Prerequisites

1. **Android Studio** Ladybug (2024.2.1) or newer
2. **JDK 21** (bundled with Android Studio or standalone)
3. **Android SDK** with API 36 platform tools
4. **Rooted Android Device** with:
   - Magisk 30.6+
   - Zygisk enabled
   - LSPosed 1.10.2+ installed

### Project Setup

```bash
# Clone repository
git clone https://github.com/akil/privacyshield.git
cd privacyshield

# Open in Android Studio
# File → Open → select privacyshield directory

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

- Hook methods must execute quickly (< 1ms overhead)
- DataStore reads should be cached in hooks
- Anti-detection must run before spoofing hooks
- UI must maintain 60fps animations

### Security Constraints

- Never crash target apps (use `optional()` for uncertain methods)
- Generate valid format values (Luhn IMEI, unicast MAC)
- Hide all Xposed-related patterns from detection

## Dependencies

### Gradle Version Catalog (`libs.versions.toml`)

```toml
[versions]
# Core
agp = "8.7.3"
kotlin = "2.2.21"
ksp = "2.2.21-1.0.31"

# Android
coreKtx = "1.16.0"
appcompat = "1.7.0"
activityCompose = "1.10.0"
lifecycleRuntimeKtx = "2.9.0"
navigationCompose = "2.9.0"

# Compose
composeBom = "2025.12.00"
material3 = "1.4.0"

# YukiHookAPI
yukihookapi = "1.2.1"

# Data
datastore = "1.1.2"
serializationJson = "1.8.0"
coroutines = "1.9.0"

# Utilities
timber = "5.0.1"
coilCompose = "2.7.0"

# Testing
junit = "4.13.2"
junitVersion = "1.2.1"
espressoCore = "3.6.1"

[libraries]
# ... (library definitions)

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### Repository Configuration

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://api.xposed.info/") }  // YukiHookAPI
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
        debugLog { tag = "PrivacyShield"; isEnable = BuildConfig.DEBUG }
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

PrivacyShield operates entirely offline. No network requests are made to external services. All data stays on device.

## File Structure

```
PrivacyShield/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/akil/privacyshield/
│   │   │   │   ├── PrivacyShieldApp.kt          # Application class
│   │   │   │   ├── hook/                        # YukiHookAPI layer
│   │   │   │   │   ├── HookEntry.kt
│   │   │   │   │   └── hooker/
│   │   │   │   │       ├── DeviceHooker.kt
│   │   │   │   │       ├── NetworkHooker.kt
│   │   │   │   │       ├── AdvertisingHooker.kt
│   │   │   │   │       ├── SystemHooker.kt
│   │   │   │   │       ├── LocationHooker.kt
│   │   │   │   │       └── AntiDetectHooker.kt
│   │   │   │   ├── data/                        # Data layer
│   │   │   │   │   ├── SpoofDataStore.kt
│   │   │   │   │   ├── ProfileManager.kt
│   │   │   │   │   ├── AppScopeManager.kt
│   │   │   │   │   ├── SpoofRepository.kt
│   │   │   │   │   ├── models/
│   │   │   │   │   └── generators/
│   │   │   │   ├── ui/                          # UI layer
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── MainViewModel.kt
│   │   │   │   │   ├── theme/
│   │   │   │   │   ├── screens/
│   │   │   │   │   ├── components/
│   │   │   │   │   └── navigation/
│   │   │   │   └── utils/                       # Utilities
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
│   └── libs.versions.toml
├── docs/prd/                                    # PRD documents
├── openspec/                                    # OpenSpec specs
└── memory-bank/                                 # Memory Bank
```
