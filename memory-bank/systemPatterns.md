# System Patterns: PrivacyShield

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              LSPOSED FRAMEWORK                           │
│                         (External - Not Part of Module)                  │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            PRIVACYSHIELD MODULE                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                         HookEntry.kt                                │ │
│  │                    (@InjectYukiHookWithXposed)                      │ │
│  └────────────────────────────────┬───────────────────────────────────┘ │
│                                   │                                      │
│                    ┌──────────────┴──────────────┐                      │
│                    ▼                             ▼                      │
│  ┌─────────────────────────────┐  ┌─────────────────────────────────┐  │
│  │      SPOOFING ENGINE        │  │     ANTI-DETECT MANAGER         │  │
│  │                             │  │                                 │  │
│  │  • DeviceHooker             │  │  • StackTraceHider              │  │
│  │  • NetworkHooker            │  │  • ClassLoaderHider             │  │
│  │  • AdvertisingHooker        │  │  • NativeLibraryHider           │  │
│  │  • SystemHooker             │  │  • ReflectionHider              │  │
│  │  • LocationHooker           │  │  • ExceptionHider               │  │
│  └─────────────────────────────┘  └─────────────────────────────────┘  │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                          DATA LAYER                                 │ │
│  │  SpoofDataStore  │  ProfileManager  │  AppScopeManager             │ │
│  │  VALUE GENERATORS: IMEI • Serial • MAC • Fingerprint • UUID        │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          UI LAYER (App Interface)                        │
│       MATERIAL 3 EXPRESSIVE + JETPACK COMPOSE                           │
│       Home │ Apps │ Spoof Settings │ Profiles │ Diagnostics │ Settings  │
└─────────────────────────────────────────────────────────────────────────┘
```

## Key Technical Decisions

### AD-1: YukiHookAPI Selection

**Decision**: Use YukiHookAPI 1.2.1 instead of raw Xposed API

**Rationale**:
- Modern Kotlin DSL with type-safe method/field references
- `@InjectYukiHookWithXposed` eliminates boilerplate
- Built-in `YukiBaseHooker` for modular organization
- KSP annotation processing for compile-time safety
- Active maintenance and documentation

**Trade-offs**:
- Additional dependency (~350KB)
- Learning curve for raw Xposed developers

### AD-2: Modular Hooker Architecture

**Decision**: Separate hookers by domain (Device, Network, Advertising, System, Location, AntiDetect)

**Structure**:
```
hook/
├── HookEntry.kt
└── hooker/
    ├── DeviceHooker.kt       # IMEI, Serial, Hardware
    ├── NetworkHooker.kt      # MAC, WiFi, Bluetooth
    ├── AdvertisingHooker.kt  # GSF, AdvID, Android ID
    ├── SystemHooker.kt       # Build.*, SystemProperties
    ├── LocationHooker.kt     # GPS, Timezone
    └── AntiDetectHooker.kt   # Detection bypass
```

**Rationale**: Single Responsibility Principle, testable isolation, clear ownership

### AD-3: Anti-Detection First Loading

**Decision**: Always load AntiDetectHooker before spoofing hookers

**Implementation**:
```kotlin
override fun onHook() = encase {
    loadHooker(AntiDetectHooker)  // ⚠️ MUST BE FIRST
    loadHooker(DeviceHooker)
    // ...
}
```

**Rationale**: Detection checks may run early; must hide before spoofing begins

### AD-4: DataStore for Persistence

**Decision**: Use Jetpack DataStore (Preferences) over SharedPreferences

**Rationale**:
- Asynchronous by design (Kotlin Flow)
- No UI thread ANRs
- Type-safe key definitions
- Modern recommendation from Google

### AD-5: Unidirectional Data Flow (UDF)

**Decision**: StateFlow + immutable data classes for UI state

**Pattern**:
```
UI Events → ViewModel → Repository → DataStore
                ↓
         StateFlow<UiState>
                ↓
              UI (Compose)
```

### AD-6: Profile-Based Configuration

**Decision**: Named profiles assignable per-app

**Structure**:
- `SpoofProfile`: Named collection of spoofed values
- `AppConfig`: Links package name to profile + enabled spoofs
- Default profile for apps without explicit config

### AD-7: Material 3 Expressive Design

**Decision**: Material 3 with Expressive enhancements + AMOLED optimization

**Color Strategy**:
- Dynamic colors on Android 12+ (Material You)
- Custom Teal/Cyan accent as fallback
- Pure black (#000000) background in dark mode

**Motion Strategy**:
- Spring-based animations (not duration-based)
- `Spring.DampingRatioMediumBouncy` for transitions

## Design Patterns in Use

### 1. YukiBaseHooker Pattern

Each domain has a dedicated hooker class:

```kotlin
object DeviceHooker : YukiBaseHooker() {
    override fun onHook() {
        // Check app scope first
        if (!SpoofDataStore.isAppEnabled(packageName)) return
        
        // Hook methods
        "android.telephony.TelephonyManager".toClass().apply {
            method { name = "getImei" }.hook {
                after { result = SpoofDataStore.getSpoofedIMEI() }
            }
        }
    }
}
```

### 2. Repository Pattern

Abstracts data access from ViewModels:

```kotlin
class SpoofRepository(
    private val dataStore: SpoofDataStore,
    private val profileManager: ProfileManager
) {
    fun getSpoofedValues(): Flow<SpoofProfile>
    suspend fun setIMEI(imei: String)
    suspend fun regenerateAll()
}
```

### 3. State Hoisting Pattern

Compose screens receive state and emit events:

```kotlin
@Composable
fun SpoofValueCard(
    label: String,
    value: String,
    onRegenerate: () -> Unit,  // Event up
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier
)
```

### 4. Navigation Routes Pattern

Type-safe navigation using string constants and data class (NOT sealed class objects):

```kotlin
// ⚠️ IMPORTANT: Do NOT use sealed class with object declarations for navigation
// It causes NullPointerException on Android 16 (API 36) during Compose recomposition

// ✅ CORRECT: String constants + data class
object NavRoutes {
    const val HOME = "home"
    const val SPOOF = "spoof" 
    const val SETTINGS = "settings"
}

data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems: List<NavItem> = listOf(
    NavItem(route = NavRoutes.HOME, label = "Home", ...),
    NavItem(route = NavRoutes.SPOOF, label = "Spoof", ...),
    NavItem(route = NavRoutes.SETTINGS, label = "Settings", ...)
)

// ❌ WRONG: Sealed class objects (causes crashes on Android 16)
// sealed class NavDestination { object Home : NavDestination() }
```

**Why**: Sealed class object declarations can cause `NullPointerException` during 
Compose recomposition on Android 16 (API 36). The objects may not be fully 
initialized when NavigationBar lambda captures them. Using simple data classes 
with string constants avoids this initialization timing issue.

### 5. Value Generator Pattern

Stateless generators with validation:

```kotlin
object IMEIGenerator {
    fun generate(): String {
        val tac = getRealisticTAC()
        val serial = generateSerial()
        val checkDigit = calculateLuhn(tac + serial)
        return tac + serial + checkDigit
    }
    
    fun isValid(imei: String): Boolean {
        if (imei.length != 15) return false
        return calculateLuhn(imei.dropLast(1)) == imei.last().digitToInt()
    }
}
```

## Component Relationships

### Hook Layer Dependencies

```
HookEntry
    │
    ├──▶ AntiDetectHooker (loads first)
    │         └──▶ (no dependencies, self-contained)
    │
    ├──▶ DeviceHooker
    │         └──▶ SpoofDataStore (read values)
    │
    ├──▶ NetworkHooker
    │         └──▶ SpoofDataStore
    │
    ├──▶ AdvertisingHooker
    │         └──▶ SpoofDataStore
    │
    ├──▶ SystemHooker
    │         └──▶ SpoofDataStore
    │
    └──▶ LocationHooker
              └──▶ SpoofDataStore
```

### Data Layer Dependencies

```
SpoofRepository
    │
    ├──▶ SpoofDataStore (DataStore Preferences)
    │
    ├──▶ ProfileManager
    │         └──▶ SpoofDataStore (profile storage)
    │
    └──▶ AppScopeManager
              └──▶ SpoofDataStore (app config storage)
```

### UI Layer Dependencies

```
MainActivity
    │
    └──▶ NavHost
              │
              ├──▶ HomeScreen ──▶ MainViewModel ──▶ SpoofRepository
              ├──▶ AppSelectionScreen ──▶ AppsViewModel ──▶ AppScopeManager
              ├──▶ SpoofSettingsScreen ──▶ SpoofViewModel ──▶ SpoofRepository
              ├──▶ ProfileScreen ──▶ ProfileViewModel ──▶ ProfileManager
              ├──▶ DiagnosticsScreen ──▶ DiagnosticsViewModel
              └──▶ SettingsScreen ──▶ SettingsViewModel
```

## Critical Implementation Paths

### Path 1: IMEI Spoofing Flow

```
1. User enables app in AppSelectionScreen
2. AppScopeManager saves to DataStore
3. User sets IMEI in SpoofSettingsScreen
4. SpoofRepository saves via SpoofDataStore
5. Target app launches
6. HookEntry.onHook() executes
7. DeviceHooker checks isAppEnabled(packageName) ✓
8. Hook intercepts TelephonyManager.getImei()
9. SpoofDataStore.getSpoofedIMEIBlocking() returns value
10. Target app receives spoofed IMEI
```

### Path 2: Anti-Detection Flow

```
1. Target app calls Thread.getStackTrace()
2. AntiDetectHooker intercepts (loaded first!)
3. Filter stack frames containing:
   - de.robv.android.xposed.*
   - io.github.lsposed.*
   - com.highcapable.yukihookapi.*
   - EdHooker*, LSPHooker*, XposedBridge, etc.
4. Return filtered stack trace
5. App sees no Xposed evidence
```

### Path 3: Profile Switch Flow

```
1. User creates new profile in ProfileScreen
2. ProfileManager generates default values
3. User edits values in SpoofSettingsScreen
4. User assigns profile to app in AppSelectionScreen
5. AppScopeManager saves profileId to app config
6. Next app launch uses new profile values
```

## Anti-Patterns to Avoid

### ❌ DON'T: Blocking UI Thread

```kotlin
// BAD
fun loadProfiles(): List<Profile> {
    return runBlocking { dataStore.data.first() }  // ❌ Blocks UI
}

// GOOD
fun loadProfiles(): Flow<List<Profile>> {
    return dataStore.data.map { it.profiles }  // ✅ Reactive
}
```

### ❌ DON'T: Raw Xposed API

```kotlin
// BAD
XposedHelpers.findAndHookMethod(...)  // ❌ Old way

// GOOD
"ClassName".toClass().method { name = "methodName" }.hook { ... }  // ✅
```

### ❌ DON'T: Hardcoded Values

```kotlin
// BAD
val primary = Color(0xFF00BCD4)  // ❌ Hardcoded

// GOOD
MaterialTheme.colorScheme.primary  // ✅ Theme-aware
```

### ❌ DON'T: Mutable State Classes

```kotlin
// BAD
class UiState(var imei: String)  // ❌ Mutable

// GOOD
data class UiState(val imei: String)  // ✅ Immutable
```

### ❌ DON'T: Missing optional() for Uncertain Methods

```kotlin
// BAD - may crash on some Android versions
method { name = "newApiMethod" }.hook { ... }  // ❌

// GOOD
method { name = "newApiMethod" }.optional().hook { ... }  // ✅
```
