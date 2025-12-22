# System Patterns: Device Masker

## System Architecture

### High-Level Architecture (Multi-Module)

> **Updated Dec 22, 2025**: 3-module structure with MVVM in UI layer

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              LSPOSED FRAMEWORK                           │
│                         (External - Not Part of Module)                  │
└─────────────────────────────────┬───────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Device Masker MODULE                             │
│                   (3-Module Gradle Structure)                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                        :app MODULE                                │   │
│  │  HookEntry.kt (@InjectYukiHookWithXposed)                         │   │
│  │  UI Layer (MVVM + Material 3 Expressive + Jetpack Compose)        │   │
│  │  ├── screens/[feature]/ViewModel.kt (State management)            │   │
│  │  ├── screens/[feature]/State.kt (Immutable UI state)              │   │
│  │  └── screens/[feature]/Screen.kt (Composable UI)                  │   │
│  │  ServiceClient + ConfigManager (AIDL consumer) ✅                 │   │
│  └────────────────────────────────┬─────────────────────────────────┘   │
│                                   │                                      │
│                          loads XposedHookLoader                          │
│                                   ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                       :xposed MODULE                              │   │
│  │                                                                   │   │
│  │  ┌───────────────────────────┐  ┌───────────────────────────┐    │   │
│  │  │  DeviceMaskerService      │  │  XposedHookLoader         │    │   │
│  │  │  (IDeviceMaskerService)   │  │  (YukiBaseHooker)         │    │   │
│  │  │  - In-memory config       │  │  - Loads all hookers      │    │   │
│  │  │  - JSON persistence       │  │  - System init            │    │   │
│  │  └───────────────────────────┘  └───────────────────────────┘    │   │
│  │                                                                   │   │
│  │  ┌─────────────────────────────────────────────────────────────┐ │   │
│  │  │                       HOOKERS                                │ │   │
│  │  │  AntiDetect │ Device │ Network │ Advertising │ System │ Loc │ │   │
│  │  │  ─────────────────────────────────────────────────────────── │ │   │
│  │  │  All read from DeviceMaskerService.instance?.config          │ │   │
│  │  └─────────────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                   │                                      │
│                          uses shared models                              │
│                                   ▼                                      │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                       :common MODULE                              │   │
│  │                                                                   │   │
│  │  IDeviceMaskerService.aidl  │  JsonConfig  │  SpoofProfile       │   │
│  │  SpoofType  │  DeviceIdentifier  │  AppConfig  │  Constants      │   │
│  │  (All @Serializable for JSON persistence)                        │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### MVVM UI Architecture (Dec 22, 2025)

```
┌─────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                          │
│                                                                  │
│  ┌─────────────────┐         ┌─────────────────────────────┐    │
│  │  HomeScreen.kt  │◄────────│  HomeViewModel              │    │
│  │  (Composable)   │  state  │  ├─ _state: MutableStateFlow│    │
│  │                 │         │  ├─ state: StateFlow        │    │
│  │  - Observes     │────────►│  └─ fun onAction(...)       │    │
│  │    state        │  events │                             │    │
│  │  - Renders UI   │         └───────────┬─────────────────┘    │
│  │  - Sends events │                     │                      │
│  └─────────────────┘                     │                      │
│                                          ▼                      │
│                              ┌───────────────────────┐          │
│                              │   SpoofRepository     │          │
│                              │   (Singleton)         │          │
│                              │   ├─ flows            │          │
│                              │   └─ suspend funs     │          │
│                              └───────────────────────┘          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow (AIDL-based IPC)

```
┌─────────────┐     AIDL      ┌──────────────────────┐
│   App UI    │◄─────────────►│ DeviceMaskerService  │
│ (Material3) │  readConfig() │   (system_server)    │
│             │ writeConfig() │                      │
└─────────────┘               └──────────┬───────────┘
                                         │
                              ┌──────────┴──────────┐
                              │   JsonConfig (RAM)  │
                              └──────────┬──────────┘
                                         │ read by
                              ┌──────────┴──────────┐
                              ▼                     ▼
                        ┌──────────┐          ┌──────────┐
                        │DeviceHook│          │NetworkHok│
                        └──────────┘          └──────────┘
```


## Key Technical Decisions

### AD-1: YukiHookAPI Selection

**Decision**: Use YukiHookAPI 1.3.1 instead of raw Xposed API

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

### AD-3b: Critical Safety Rules (MUST FOLLOW!)

**Decision**: Never hook system-critical processes or block essential classes

**Forbidden Processes** (SKIP these in HookEntry.onHook()):
```kotlin
val forbiddenProcesses = listOf(
    "android",              // Core Android framework - NEVER HOOK
    "system_server",        // System server - dangerous
    "com.android.systemui", // SystemUI - causes UI glitches
)

if (packageName in forbiddenProcesses || processName in forbiddenProcesses) {
    return@encase  // Skip entirely!
}
```

**Allowed Class Patterns** (NEVER block in AntiDetectHooker):
```kotlin
val allowedPatterns = listOf(
    "com.astrixforge.devicemasker",  // Our own module
    "androidx.",               // AndroidX libraries  
    "kotlin.", "kotlinx.",     // Kotlin stdlib
    "java.",                   // Java stdlib
    "android.",                // Android framework
    "com.google.android",      // Google libraries
)
```

**Why This Matters**:
1. YukiHookAPI's `packageName` in `encase {}` can be `"android"` in Zygote scope
2. Hooking system classes affects ALL apps including the module itself
3. Blocking essential class loading crashes the module app
4. **Hard-learned lesson**: App stuck at logo crash was caused by this exact issue!
5. **Warning Cleanup**: Use `@Suppress("UNCHECKED_CAST")` with safe casts (`as? Array<StackTraceElement>`) in `AntiDetectHooker` to avoid cluttering the build log with non-critical warnings.

**Rationale**: Prevents module from crashing itself or the entire system

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

**Decision**: Named profiles assignable per-app with independent enable/disable

**Structure**:
- `SpoofProfile`: Named collection of spoofed values with `isEnabled` flag
- `assignedApps: Set<String>`: Apps assigned to this profile
- Default profile for apps without explicit config

### AD-6b: Independent Profiles (No Global Config)

**Decision**: Remove GlobalSpoofConfig entirely, profiles are fully independent

**Why Global Config Was Removed** (Dec 17, 2025):
- **Simpler Mental Model**: Each profile controls its own behavior
- **No Conflicts**: No confusing interaction between global and profile settings
- **Cleaner Code**: Hookers only check profile settings, not two layers

**New Data Flow**:
```
App Launch → HookDataProvider.getProfileForPackage()
                      ↓
           Profile found? No → Return null (no spoofing)
                      ↓ Yes
           Profile.isEnabled? No → Return null (no spoofing)
                      ↓ Yes
           Type enabled in profile? No → Return null
                      ↓ Yes
           Return spoofed value
```

**Old Flow (Removed)**:
```
❌ GlobalSpoofConfig.isTypeEnabled() → Profile.isTypeEnabled()
```

**Hooker Pattern (Simplified)**:
```kotlin
private fun getSpoofValueOrGenerate(
    context: Context?,
    type: SpoofType,
    generator: () -> String
): String? {
    val provider = getProvider(context)
    if (provider == null) {
        return generator() // No provider, use fallback
    }
    
    // getSpoofValue handles ALL checks:
    // 1. Profile exists for this app
    // 2. Profile.isEnabled == true
    // 3. Profile.isTypeEnabled(type) == true
    return provider.getSpoofValue(type) ?: generator()
}
```

**Profile Model**:
```kotlin
data class SpoofProfile(
    val id: String,
    val name: String,
    val isEnabled: Boolean = true,  // Master switch per profile
    val isDefault: Boolean = false,
    val assignedApps: Set<String> = emptySet(),
    val identifiers: List<DeviceIdentifier> = emptyList()
) {
    fun isTypeEnabled(type: SpoofType): Boolean
    fun getValue(type: SpoofType): String?
    fun withEnabled(enabled: Boolean): SpoofProfile
}
```

### AD-7: Material 3 Expressive Design

**Decision**: Material 3 Expressive (1.5.0-alpha11) with physics-based animations

**Version**: Material 3 `1.5.0-alpha11` with Graphics Shapes `1.0.1`

**Motion Strategy (Spring-Based)**:
```kotlin
// Spatial Springs - For position, size, scale (CAN overshoot)
AppMotion.Spatial.Expressive  // Icon buttons, FABs (0.5 damping, low stiffness)
AppMotion.Spatial.Standard    // Navigation, list animations (0.75 damping, medium stiffness)
AppMotion.Spatial.Snappy      // Toggle switches, quick feedback (0.75 damping, high stiffness)

// Effect Springs - For color, opacity (NO overshoot)
AppMotion.Effect.Color        // Background, track, thumb color changes
AppMotion.Effect.Alpha        // Fade in/out, visibility
AppMotion.Effect.Quick        // Immediate feedback
```

**Expressive Components (10 Total)**:
```
ui/components/expressive/
├── AnimatedSection.kt           # Animated expand/collapse sections
├── ExpressiveCard.kt            # Card with spring press feedback
├── ExpressiveIconButton.kt      # Icon button with spring scale animation
├── ExpressiveLoadingIndicator.kt # M3 LoadingIndicator wrapper
├── ExpressivePullToRefresh.kt   # Pull-to-refresh with morphing indicator
├── ExpressiveSwitch.kt          # M3 Switch with spring thumb animation
├── MorphingShape.kt             # Animated corner radius utilities
├── QuickActionGroup.kt          # M3 ButtonGroup wrapper
├── SectionHeader.kt             # Consistent section headers
└── StatusIndicator.kt           # Status dot indicators
```

**ExpressiveSwitch Usage**:
```kotlin
// Basic usage - uses MaterialTheme colors automatically
ExpressiveSwitch(
    checked = isEnabled,
    onCheckedChange = { onEnableChange(it) }
)

// Without thumb icon
ExpressiveSwitch(
    checked = checked,
    onCheckedChange = onCheckedChange,
    showThumbIcon = false
)
```

**ExpressiveIconButton Usage**:
```kotlin
// Standard size (40dp button, 24dp icon) - for prominent actions
ExpressiveIconButton(
    onClick = { onRefresh() },
    icon = Icons.Filled.Refresh,
    contentDescription = "Refresh",
    tint = MaterialTheme.colorScheme.primary
)

// Compact size (36dp button, 20dp icon) - for action rows
CompactExpressiveIconButton(
    onClick = { onCopy() },
    icon = Icons.Filled.ContentCopy,
    contentDescription = "Copy",
    tint = MaterialTheme.colorScheme.onSecondaryContainer
)
```

**Color Strategy**:
- Dynamic colors on Android 12+ (Material You)
- Custom Teal/Cyan accent as fallback
- Pure black (#000000) background in dark mode
- Secondary color for active navigation labels (M3 1.4.0+ spec)
- ExpressiveSwitch uses `colorScheme.primary` for checked track (theme-aware)

### AD-8: UI Layering (Z-Order) for Overlays

**Decision**: Place all overlays (Loading, Error, Dialogs) as the **last child** of the main `Box` container.

**Rationale**:
- In Jetpack Compose `Box`, the rendering order is sequential; later children appear on top.
- Ensures `AnimatedLoadingOverlay` properly obscures and blocks interaction with the dashboard while loading.
- Prevents components from being "cut off" or obscured by sibling elements.

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

### 5. Screen Header Pattern (Material 3 Consistency)

**IMPORTANT**: Screen header style depends on navigation context:

#### Main Navigation Destinations (Bottom Nav Tabs)
Use **inline headers** inside LazyColumn with `headlineMedium` typography:

```kotlin
// ✅ CORRECT for main nav destinations (Home, Apps, Spoof, Profiles, Settings)
LazyColumn(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
) {
    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Screen Title",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            // Optional action buttons
        }
    }
    // Content items...
}
```

#### Sub-Screens / Detail Screens
Use **TopAppBar** with Scaffold for screens navigated TO (not in bottom nav):

```kotlin
// ✅ CORRECT for sub-screens (Diagnostics, Profile Details, etc.)
Scaffold(
    topBar = {
        TopAppBar(
            title = { Text("Detail Screen") },
            navigationIcon = { /* Back button */ }
        )
    }
) { innerPadding ->
    // Content...
}
```

| Screen Type | Header Style | Example Screens |
|-------------|--------------|-----------------|
| Main Nav Destination | Inline `headlineMedium` | Home, Apps, Spoof, Profiles, Settings |
| Sub-screen/Detail | TopAppBar in Scaffold | Diagnostics, Profile Edit |

### 6. Value Generator Pattern

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

### UI Layer Dependencies (MVVM - Dec 22, 2025)

```
MainActivity
    │
    └──▶ NavHost (with viewModel { } factory for each screen)
              │
              ├──▶ HomeScreen ──▶ HomeViewModel ──▶ SpoofRepository
              │         └─ collectAsStateWithLifecycle(HomeState)
              │
              ├──▶ SettingsScreen ──▶ SettingsViewModel ──▶ SettingsDataStore
              │         └─ collectAsStateWithLifecycle(SettingsState)
              │
              ├──▶ ProfileScreen ──▶ ProfileViewModel ──▶ SpoofRepository
              │         └─ collectAsStateWithLifecycle(ProfileState)
              │
              ├──▶ ProfileDetailScreen ──▶ ProfileDetailViewModel ──▶ SpoofRepository
              │         └─ collectAsStateWithLifecycle(ProfileDetailState)
              │
              └──▶ DiagnosticsScreen ──▶ DiagnosticsViewModel ──▶ SpoofRepository
                        └─ collectAsStateWithLifecycle(DiagnosticsState)
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

### ❌ DON'T: Hook System Processes (CRITICAL!)

```kotlin
// BAD - Causes module app crash and system instability
override fun onHook() = encase {
    // No checks! Hooks EVERYTHING including "android" core process
    loadHooker(AntiDetectHooker)  // ❌ Will block own class loading
}

// GOOD
override fun onHook() = encase {
    // Skip system-critical processes
    val forbidden = listOf("android", "system_server", "com.android.systemui")
    if (packageName in forbidden) return@encase  // ✅ Safe
    
    loadHooker(AntiDetectHooker)
}
```

### ❌ DON'T: Block Essential Class Loading

```kotlin
// BAD - Blocks module's own dependencies from loading
private fun shouldBlockClass(className: String): Boolean {
    return HIDDEN_PATTERNS.any { className.contains(it) }  // ❌ No allowlist!
}

// GOOD
private fun shouldBlockClass(className: String): Boolean {
    // Allowlist essential classes FIRST
    val allowed = listOf("androidx.", "kotlin.", "java.", "android.")
    if (allowed.any { className.startsWith(it) }) return false  // ✅ Never block
    
    return HIDDEN_PATTERNS.any { className.contains(it) }
}
```

## Theming Patterns

### TP-1: Complete Dark Color Schemes

**Pattern**: Material 3 `darkColorScheme()` does NOT provide sensible defaults - always specify ALL color roles.

```kotlin
// BAD - Missing critical colors, content will be invisible
darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark
    // Missing background, surface, onSurface, etc.!
)

// GOOD - Complete color scheme
darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color.Black,
    // ... all primaries/secondaries/tertiaries/errors
    
    // CRITICAL - These are often forgotten:
    background = Color(0xFF121212),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFC0C0C0),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF2E2E2E),
    surfaceContainerLow = Color(0xFF161616),
    surfaceContainerLowest = Color(0xFF0E0E0E)
)
```

### TP-2: Dynamic Edge-to-Edge with Theme Changes

**Pattern**: `SystemBarStyle.auto()` follows SYSTEM theme, not app theme. Use `DisposableEffect` to update on app theme changes.

```kotlin
// BAD - Uses system theme, not app theme
enableEdgeToEdge()  // or SystemBarStyle.auto()

// GOOD - Reacts to app theme changes
DisposableEffect(darkTheme) {
    activity.enableEdgeToEdge(
        statusBarStyle = if (darkTheme) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        },
        navigationBarStyle = if (darkTheme) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(...)
        }
    )
    onDispose { }
}
```

### TP-3: Consistent Screen Layout Pattern

**Pattern**: Main navigation screens use LazyColumn with inline headers. Sub-screens can use TopAppBar.

```kotlin
// Main Nav Screens (Home, Apps, Spoof, Profiles, Settings)
LazyColumn(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    item {
        Text(
            text = "Screen Title",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
    // Content items...
}

// Sub-Screens with FAB: Use Box + positioned FAB (NOT nested Scaffold)
Box(modifier = modifier.fillMaxSize()) {
    LazyColumn(...) { /* content */ }
    
    ExtendedFloatingActionButton(
        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
    )
}
```

### TP-4: Consistent Card Styling

**Pattern**: All cards use the same base styling for visual consistency.

```kotlin
// STANDARD: All cards in the app
ElevatedCard(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ),
    shape = MaterialTheme.shapes.large  // or shapes.medium for list items
) {
    // Card content
}

// ACTIVE/SELECTED STATE: Use primaryContainer with alpha
colors = CardDefaults.elevatedCardColors(
    containerColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
)
```

**Do NOT use**:
- `Card` (use `ElevatedCard` instead)
- `surfaceContainerLow` (use `surfaceContainerHigh`)
- Missing `colors` parameter (always specify explicitly)
- Missing `shape` parameter (always specify explicitly)

### TP-5: Device Profile Presets (System Spoofing)

**Pattern**: Use unified device profiles instead of individual Build.* fields to ensure consistency.

**Rationale**: Apps detect spoofing by checking if Build.* values are consistent (e.g., fingerprint matches model, brand, device). Spoofing these individually leads to detection failures.

**Implementation**:
```kotlin
// DeviceProfilePreset.kt - Predefined consistent profiles
data class DeviceProfilePreset(
    val id: String,           // e.g., "pixel_8_pro"
    val name: String,         // e.g., "Google Pixel 8 Pro"
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val product: String,
    val board: String,
    val fingerprint: String,  // Consistent with all above
    val securityPatch: String,
)

// 10 presets available: Pixel, Samsung, OnePlus, Xiaomi, Sony, Nothing
```

**UI Pattern**:
- Single toggle for entire device profile
- Display preset name (not ID)
- Regenerate randomly picks from 10 presets
- Same pattern as all other spoof items (toggle, copy, regenerate)

**Hook Pattern**:
```kotlin
// SystemHooker.kt - Apply all Build.* from preset
val preset = DeviceProfilePreset.findById(profileId)
if (preset != null) {
    hookBuildField("MODEL", preset.model)
    hookBuildField("MANUFACTURER", preset.manufacturer)
    hookBuildField("FINGERPRINT", preset.fingerprint)
    // ... all other fields
}
```

**SpoofType Change**: 
- Removed: `BUILD_FINGERPRINT`, `BUILD_MODEL`, `BUILD_MANUFACTURER`, `BUILD_BRAND`, `BUILD_DEVICE`, `BUILD_PRODUCT`, `BUILD_BOARD`
- Added: `DEVICE_PROFILE` (value = preset ID)

---

## Spoof Value Correlation UI Patterns

> **Updated Dec 20, 2025**: Simplified patterns after fixing regeneration bugs

### Category Types

| Category | Type | UI Pattern | Regeneration Behavior |
|----------|------|------------|----------------------|
| **SIM Card** | Correlated | Single switch + regenerate at category top | All values regenerate together |
| **Device Hardware** | Independent | All 3 use `IndependentSpoofItem` | Each regenerates independently |
| **Location** | Hybrid | Timezone+Locale combined card, Lat/Long independent | TZ regenerate updates both TZ+Locale |
| **Network** | Independent | Each item has own switch + regenerate | Individual regeneration |
| **Advertising** | Independent | Each item has own switch + regenerate | Individual regeneration |

### UI Component Hierarchy

```
ProfileDetailScreen
├── ProfileSpoofContent
│   └── ProfileCategorySection (for each UIDisplayCategory)
│       ├── Category Header (icon + title + expand arrow)
│       └── AnimatedVisibility (expanded content)
│           ├── If SIM_CARD (Correlated):
│           │   ├── Switch + Regenerate row
│           │   └── CorrelatedSpoofItem[] (display-only)
│           ├── If DEVICE_HARDWARE:
│           │   └── DeviceHardwareCategoryContent
│           │       └── 3x IndependentSpoofItem (Profile, IMEI, Serial)
│           ├── If LOCATION:
│           │   └── LocationCategoryContent
│           │       ├── Timezone+Locale card (combined, single switch)
│           │       └── 2x IndependentSpoofItem (Lat, Long)
│           └── If Network/Advertising (Independent):
│               └── IndependentSpoofItem[] (each has switch + regenerate)
```

### Copy & Regenerate UX

| Interaction | Pattern |
|-------------|---------|
| **Copy value** | Long-press on value text |
| **Regenerate icon** | IconButton (standard size) |
| **Category header** | Icon + title only (clean) |

### Key Composables

| Composable | Purpose | Used For |
|------------|---------|----------|
| `CorrelatedSpoofItem` | Display-only item (long-press to copy) | SIM Card category items |
| `IndependentSpoofItem` | Item with switch + regenerate | Network, Advertising, Device Hardware items, Lat/Long |
| `LocationCategoryContent` | Timezone+Locale combined card + Lat/Long independent | Location category |
| `DeviceHardwareCategoryContent` | 3x IndependentSpoofItem (all independent) | Device Hardware category |

### Implementation Notes

1. **Location Regeneration** (Fixed):
   ```kotlin
   // Regenerate button calls BOTH individually to avoid cache issues
   IconButton(onClick = {
       onRegenerate(SpoofType.TIMEZONE)
       onRegenerate(SpoofType.LOCALE)
   }) { ... }
   ```

2. **Device Hardware Simplified**:
   ```kotlin
   // All 3 items now use standard IndependentSpoofItem
   IndependentSpoofItem(
       type = SpoofType.DEVICE_PROFILE,
       value = deviceProfileDisplayValue,  // Shows preset name, not ID
       ...
   )
   ```

3. **Long-press to Copy**:
   ```kotlin
   @OptIn(ExperimentalFoundationApi::class)
   Text(
       text = value,
       modifier = Modifier.combinedClickable(
           onClick = { },
           onLongClick = { onCopy(value) }
       )
   )
   ```

4. **UIDisplayCategory enum** uses `isCorrelated = false` for Location and Device Hardware to enable custom rendering logic via dedicated composables.

## Value Generation & Correlation Patterns (Dec 21, 2025)

### Correlation Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   CORRELATION GROUPS                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  SIM_CARD (Primary)           SIM_CARD_2 (Dual-SIM)         │
│  ├── IMSI                     ├── IMSI_2                    │
│  ├── ICCID                    ├── ICCID_2                   │
│  ├── PHONE_NUMBER             ├── PHONE_NUMBER_2            │
│  ├── CARRIER_NAME             ├── CARRIER_NAME_2            │
│  └── CARRIER_MCC_MNC          └── CARRIER_MCC_MNC_2         │
│                                                              │
│  LOCATION (Country-Based)     DEVICE_HARDWARE               │
│  ├── TIMEZONE                 ├── IMEI                      │
│  ├── LOCALE                   ├── SERIAL                    │
│  ├── LOCATION_LATITUDE        └── (Device Profile)          │
│  └── LOCATION_LONGITUDE                                     │
│                                                              │
│  NONE (Independent)                                          │
│  ├── WIFI_SSID, WIFI_BSSID, WIFI_MAC                        │
│  ├── BLUETOOTH_MAC, ANDROID_ID                              │
│  └── GSF_ID, ADVERTISING_ID                                 │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Carrier → Location Sync Flow

```
User selects Carrier (e.g., T-Mobile US)
         │
         ▼
┌─────────────────────────────────────────┐
│     SpoofRepository.updateProfileWithCarrier()     │
├─────────────────────────────────────────┤
│  1. Generate SIMProfile for carrier                │
│  2. Generate LocationProfile for carrier's country │
│  3. Update profile with:                           │
│     - All SIM values (IMSI, ICCID, Phone, etc.)   │
│     - Timezone (country-appropriate)              │
│     - Locale (country-appropriate)                │
│     - GPS coordinates (city-level bounds)         │
└─────────────────────────────────────────┘
```

### GPS Correlation Implementation

```kotlin
// LocationProfile now includes GPS coordinates
data class LocationProfile(
    val country: String,      // ISO code
    val timezone: String,     // TZ database name
    val locale: String,       // Language_Country
    val latitude: Double,     // Within country bounds
    val longitude: Double     // Within country bounds
)

// GPS bounds per country (major cities)
private val COUNTRY_GPS_BOUNDS = mapOf(
    "US" to listOf(
        GPSBounds(40.4774, 40.9176, -74.2591, -73.7004),   // NYC
        GPSBounds(33.7037, 34.3373, -118.6682, -117.6462), // LA
        // ... more cities
    ),
    "IN" to listOf(
        GPSBounds(18.8928, 19.2705, 72.7758, 72.9866),     // Mumbai
        GPSBounds(28.4041, 28.8835, 76.8380, 77.3419),     // Delhi
        // ... more cities
    ),
    // 16 countries total, 42 city bounds
)
```

### Dual-SIM Generation

```kotlin
// SIM 2 has its own cache and generates independently
private var cachedSIM2Profile: SIMProfile? = null

private fun generateSIM2Value(type: SpoofType): String {
    if (cachedSIM2Profile == null) {
        cachedSIM2Profile = SIMProfileGenerator.generate()  // Different carrier
    }
    return when (type) {
        SpoofType.IMSI_2 -> cachedSIM2Profile!!.imsi
        SpoofType.ICCID_2 -> cachedSIM2Profile!!.iccid
        // ... etc
    }
}
```

### Country Data Coverage

| Country | ISO | Carriers | GPS Cities | Timezones | Locales |
|---------|-----|----------|------------|-----------|---------|
| 🇺🇸 USA | US | 45+ | 6 | 7 | en_US, es_US |
| 🇮🇳 India | IN | 5 | 6 | 1 | en_IN, hi_IN |
| 🇬🇧 UK | GB | 4 | 3 | 1 | en_GB |
| 🇩🇪 Germany | DE | 3 | 3 | 1 | de_DE |
| 🇫🇷 France | FR | 3 | 3 | 1 | fr_FR |
| 🇯🇵 Japan | JP | 3 | 3 | 1 | ja_JP |
| 🇨🇳 China | CN | 3 | 3 | 2 | zh_CN |
| 🇦🇺 Australia | AU | 3 | 4 | 5 | en_AU |
| 🇨🇦 Canada | CA | 3 | 3 | 5 | en_CA, fr_CA |
| 🇰🇷 South Korea | KR | 3 | 3 | 1 | ko_KR |
| 🇧🇷 Brazil | BR | 4 | 3 | 3 | pt_BR |
| 🇷🇺 Russia | RU | 4 | 3 | 3 | ru_RU |
| 🇲🇽 Mexico | MX | 3 | 3 | 3 | es_MX |
| 🇮🇩 Indonesia | ID | 4 | 3 | 3 | id_ID |
| 🇸🇦 Saudi Arabia | SA | 3 | 3 | 1 | ar_SA |
| 🇦🇪 UAE | AE | 2 | 3 | 1 | ar_AE, en_AE |

### Phone Number Generation

```kotlin
// Area codes by US state for realism
private val US_AREA_CODES = listOf(
    // Northeast
    212, 347, 718, 917, 646,  // New York
    201, 551, 609, 732, 856,  // New Jersey
    // ... 100+ total
)

// NANP-compliant generation
fun generateUSPhoneNumber(): String {
    val areaCode = US_AREA_CODES.random()
    val exchange = "${(2..9).random()}${secureRandom.nextInt(10)}${secureRandom.nextInt(10)}"
    val subscriber = buildString { repeat(4) { append(secureRandom.nextInt(10)) } }
    return "+1$areaCode$exchange$subscriber"
}
```

### WiFi SSID Patterns

```kotlin
private fun generateRealisticSSID(): String {
    val patterns = listOf(
        "NETGEAR${secureRandom.nextInt(100).toString().padStart(2, '0')}",
        "TP-LINK_${generateHexSuffix(4)}",
        "ASUS_RT-${listOf("AC68U", "AX88U", "N66U").random()}",
        "ATT${generateHexSuffix(6)}",
        "xfinitywifi",
        "Linksys${secureRandom.nextInt(10000).toString().padStart(5, '0')}",
        "${listOf("Home", "Guest", "MyNetwork").random()}_${generateHexSuffix(4)}",
    )
    return patterns.random()
}
```

