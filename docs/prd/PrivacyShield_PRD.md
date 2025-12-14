# 🔒 PrivacyShield - Device Spoofing LSPosed Module
## Complete Product Requirements Document (PRD) v2.0
### December 2025 | Latest Tech Stack | Material 3 Expressive

---

## 📋 DOCUMENT INFORMATION

| **Field** | **Value** |
|---|---|
| **Document Version** | 2.0 |
| **Last Updated** | December 14, 2025 |
| **Project Name** | PrivacyShield |
| **Project Type** | LSPosed Module (Educational Security Research) |
| **Target Platforms** | Android 10 - Android 16 (API 29-36) |
| **Primary Language** | Kotlin 2.2.21 |
| **UI Framework** | Jetpack Compose + Material 3 Expressive |

---

## 🎯 EXECUTIVE SUMMARY

**PrivacyShield** is an open-source LSPosed module focused on **device identifier spoofing** with a robust **anti-detection layer** that prevents apps from detecting hook injection. This module is designed to work seamlessly alongside external modules (Shamiko, Play Integrity Fix, etc.) that handle root/SafetyNet/Play Integrity bypass.

### Core Philosophy
> **"Do one thing excellently"** - Spoof device identifiers and hide the injection, nothing more.

---

## 🏗️ SCOPE DEFINITION

### ✅ IN SCOPE (What This Module Does)

| **Category** | **Features** |
|---|---|
| **Device Spoofing** | IMEI, IMSI, Serial Number, Hardware ID, Device ID |
| **Network Spoofing** | MAC Address (WiFi + Bluetooth), SSID, Carrier Info |
| **Advertising Spoofing** | GSF ID, Advertising ID, Media DRM ID, Android ID |
| **System Spoofing** | Build Properties, Manufacturer, Model, Fingerprint |
| **Location Spoofing** | GPS Coordinates, Timezone, Language |
| **Anti-Detection** | Stack Trace Hiding, ClassLoader Hiding, /proc/maps Hiding |
| **UI/UX** | Material 3 Expressive, Dynamic Colors, AMOLED Theme |
| **Data Management** | Profile Management, Per-App Configuration |

### ❌ OUT OF SCOPE (External Modules)

| **Feature** | **Recommended External Module** |
|---|---|
| Root Detection Bypass | Shamiko, Zygisk-Next |
| SafetyNet Bypass | Play Integrity Fix (PIF) |
| Play Integrity Bypass | Tricky Store, PIF |
| Bootloader Status | Not bypassable (hardware) |
| Banking App Compatibility | Combination of external modules |

---

## 🔧 TECHNOLOGY STACK (December 2025)

### Core Technologies

| **Technology** | **Version** | **Purpose** |
|---|---|---|
| **Kotlin** | 2.2.21 | Primary language |
| **Android SDK** | API 36 (compileSdk) | Android 16 support |
| **Min SDK** | API 26 (Android 8.0) | Broader device compatibility |
| **Target SDK** | API 36 | Latest Android 16 |
| **Java Version** | 21 | Latest LTS |

### UI Framework

| **Library** | **Version** | **Purpose** |
|---|---|---|
| **Compose BOM** | 2025.12.00 | December '25 release |
| **Compose UI** | 1.10.0 | Core UI toolkit |
| **Material 3** | 1.4.0 | Material 3 Expressive |
| **Activity Compose** | 1.10.0 | Activity integration |
| **Navigation Compose** | 2.9.0 | Navigation component |
| **Lifecycle** | 2.9.0 | Lifecycle-aware components |

### Hooking Framework (YukiHookAPI)

| **Library** | **Version** | **Purpose** |
|---|---|---|
| **YukiHookAPI** | 1.2.1 | Modern Kotlin Hook API |
| **YukiHookAPI KSP** | 1.2.1 | Annotation processor |
| **LSPosed** | 1.10.2+ | Framework (external) |
| **Magisk** | 30.6+ | Root solution (external) |

### Data & Utilities

| **Library** | **Version** | **Purpose** |
|---|---|---|
| **DataStore** | 1.1.2 | Preferences storage |
| **Coroutines** | 1.9.0 | Async operations |
| **Timber** | 5.0.1 | Logging |

---

## 📁 PROJECT STRUCTURE

```
PrivacyShield/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/privacyshield/
│   │   │   │   │
│   │   │   │   ├── PrivacyShieldApp.kt          # ModuleApplication
│   │   │   │   │
│   │   │   │   ├── hook/                        # 🎣 YUKIHOOKAPI LAYER
│   │   │   │   │   ├── HookEntry.kt             # @InjectYukiHookWithXposed
│   │   │   │   │   │
│   │   │   │   │   ├── hooker/                  # YukiBaseHooker classes
│   │   │   │   │   │   ├── DeviceHooker.kt      # IMEI, Serial, Hardware
│   │   │   │   │   │   ├── NetworkHooker.kt     # MAC, WiFi, Bluetooth
│   │   │   │   │   │   ├── AdvertisingHooker.kt # GSF, AdvID, Android ID
│   │   │   │   │   │   ├── SystemHooker.kt      # Build props, SystemProperties
│   │   │   │   │   │   ├── LocationHooker.kt    # GPS, Timezone
│   │   │   │   │   │   └── AntiDetectHooker.kt  # Stack trace, ClassLoader hiding
│   │   │   │   │
│   │   │   │   ├── data/                        # 💾 DATA LAYER
│   │   │   │   │   ├── SpoofDataStore.kt        # DataStore preferences
│   │   │   │   │   ├── ProfileManager.kt        # Spoof profile management
│   │   │   │   │   ├── AppScopeManager.kt       # Per-app configuration
│   │   │   │   │   │
│   │   │   │   │   ├── models/                  # Data Models
│   │   │   │   │   │   ├── SpoofProfile.kt
│   │   │   │   │   │   ├── DeviceIdentifier.kt
│   │   │   │   │   │   ├── AppConfig.kt
│   │   │   │   │   │   └── GeneratorSettings.kt
│   │   │   │   │   │
│   │   │   │   │   └── generators/              # Value Generators
│   │   │   │   │       ├── IMEIGenerator.kt
│   │   │   │   │       ├── SerialGenerator.kt
│   │   │   │   │       ├── MACGenerator.kt
│   │   │   │   │       ├── FingerprintGenerator.kt
│   │   │   │   │       └── UUIDGenerator.kt
│   │   │   │   │
│   │   │   │   ├── ui/                          # 🎨 USER INTERFACE
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── MainViewModel.kt
│   │   │   │   │   │
│   │   │   │   │   ├── theme/                   # Material 3 Expressive
│   │   │   │   │   │   ├── Theme.kt
│   │   │   │   │   │   ├── Color.kt
│   │   │   │   │   │   ├── Typography.kt
│   │   │   │   │   │   ├── Shapes.kt
│   │   │   │   │   │   └── Motion.kt            # Spring animations
│   │   │   │   │   │
│   │   │   │   │   ├── screens/                 # App Screens
│   │   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   │   ├── AppSelectionScreen.kt
│   │   │   │   │   │   ├── SpoofSettingsScreen.kt
│   │   │   │   │   │   ├── ProfileScreen.kt
│   │   │   │   │   │   ├── DiagnosticsScreen.kt
│   │   │   │   │   │   └── SettingsScreen.kt
│   │   │   │   │   │
│   │   │   │   │   ├── components/              # Reusable Components
│   │   │   │   │   │   ├── AppListItem.kt
│   │   │   │   │   │   ├── SpoofValueCard.kt
│   │   │   │   │   │   ├── ProfileCard.kt
│   │   │   │   │   │   ├── ToggleButton.kt
│   │   │   │   │   │   ├── SplitActionButton.kt
│   │   │   │   │   │   └── StatusIndicator.kt
│   │   │   │   │   │
│   │   │   │   │   └── navigation/              # Navigation
│   │   │   │   │       ├── NavGraph.kt
│   │   │   │   │       └── BottomNavBar.kt
│   │   │   │   │
│   │   │   │   └── utils/                       # 🔧 UTILITIES
│   │   │   │       ├── ValidationUtil.kt
│   │   │   │       ├── LoggingUtil.kt
│   │   │   │       ├── LuhnValidator.kt
│   │   │   │       └── Constants.kt
│   │   │   │
│   │   │   ├── res/
│   │   │   │   ├── drawable/
│   │   │   │   ├── mipmap/
│   │   │   │   └── values/
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   └── test/                                # Unit Tests
│   │
│   └── build.gradle.kts
│
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## 🏗️ ARCHITECTURE

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
│  │                    (IXposedHookLoadPackage)                         │ │
│  └────────────────────────────────┬───────────────────────────────────┘ │
│                                   │                                      │
│                    ┌──────────────┴──────────────┐                      │
│                    ▼                             ▼                      │
│  ┌─────────────────────────────┐  ┌─────────────────────────────────┐  │
│  │      SPOOFING ENGINE        │  │     ANTI-DETECT MANAGER         │  │
│  │                             │  │                                 │  │
│  │  ┌───────────────────────┐  │  │  ┌───────────────────────────┐  │  │
│  │  │    Device Hooks       │  │  │  │   StackTraceHider         │  │  │
│  │  │  • IMEI/IMSI          │  │  │  │   ClassLoaderHider        │  │  │
│  │  │  • Serial             │  │  │  │   NativeLibraryHider      │  │  │
│  │  │  • Hardware ID        │  │  │  │   ReflectionHider         │  │  │
│  │  └───────────────────────┘  │  │  │   ExceptionHider          │  │  │
│  │                             │  │  └───────────────────────────┘  │  │
│  │  ┌───────────────────────┐  │  │                                 │  │
│  │  │    Network Hooks      │  │  │  Purpose:                       │  │
│  │  │  • WiFi MAC           │  │  │  Hide that hooks are running    │  │
│  │  │  • Bluetooth MAC      │  │  │  from target applications       │  │
│  │  │  • SSID/Carrier       │  │  │                                 │  │
│  │  └───────────────────────┘  │  └─────────────────────────────────┘  │
│  │                             │                                        │
│  │  ┌───────────────────────┐  │                                        │
│  │  │  Advertising Hooks    │  │                                        │
│  │  │  • GSF ID             │  │                                        │
│  │  │  • Advertising ID     │  │                                        │
│  │  │  • Android ID         │  │                                        │
│  │  └───────────────────────┘  │                                        │
│  │                             │                                        │
│  │  ┌───────────────────────┐  │                                        │
│  │  │    System Hooks       │  │                                        │
│  │  │  • Build Properties   │  │                                        │
│  │  │  • SystemProperties   │  │                                        │
│  │  │  • Fingerprint        │  │                                        │
│  │  └───────────────────────┘  │                                        │
│  │                             │                                        │
│  │  ┌───────────────────────┐  │                                        │
│  │  │   Location Hooks      │  │                                        │
│  │  │  • GPS Coordinates    │  │                                        │
│  │  │  • Timezone           │  │                                        │
│  │  └───────────────────────┘  │                                        │
│  │                             │                                        │
│  └─────────────────────────────┘                                        │
│                                                                          │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │                          DATA LAYER                                 │ │
│  │                                                                     │ │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  │ │
│  │  │  SpoofDataStore  │  │  ProfileManager  │  │  AppScopeManager │  │ │
│  │  │  (DataStore)     │  │  (Profiles)      │  │  (Per-App Config)│  │ │
│  │  └──────────────────┘  └──────────────────┘  └──────────────────┘  │ │
│  │                                                                     │ │
│  │  ┌──────────────────────────────────────────────────────────────┐  │ │
│  │  │                      VALUE GENERATORS                         │  │ │
│  │  │  IMEI • Serial • MAC • Fingerprint • UUID • Android ID       │  │ │
│  │  └──────────────────────────────────────────────────────────────┘  │ │
│  │                                                                     │ │
│  └────────────────────────────────────────────────────────────────────┘ │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          UI LAYER (App Interface)                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │              MATERIAL 3 EXPRESSIVE + JETPACK COMPOSE             │    │
│  │                                                                   │    │
│  │   Home  │  Apps  │  Spoof Settings  │  Profiles  │  Diagnostics  │    │
│  │                                                                   │    │
│  │   • Dynamic Colors (Material You)                                 │    │
│  │   • AMOLED Black Theme                                            │    │
│  │   • Spring-Based Animations                                       │    │
│  │   • New M3 Components (Split Buttons, Button Groups)              │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Data Flow Diagram

```
┌──────────────────┐
│   Target App     │
│  (e.g., Banking) │
└────────┬─────────┘
         │
         │ Calls getDeviceId(), getImei(), etc.
         ▼
┌──────────────────────────────────────────────────────────────┐
│                    LSPOSED HOOK INTERCEPTION                  │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  1. Anti-Detection Layer runs FIRST                          │
│     └─→ Hides Xposed presence from stack traces              │
│     └─→ Blocks class loading detection                       │
│     └─→ Filters /proc/maps reads                             │
│                                                               │
│  2. Spoofing Hooks intercept API calls                       │
│     └─→ TelephonyManager.getImei() → SpoofedIMEI             │
│     └─→ Build.SERIAL → SpoofedSerial                         │
│     └─→ WifiInfo.getMacAddress() → SpoofedMAC                │
│                                                               │
│  3. DataStore provides spoofed values                        │
│     └─→ Returns per-app or global spoofed values             │
│                                                               │
└──────────────────────────────────────────────────────────────┘
         │
         │ Returns spoofed values
         ▼
┌──────────────────┐
│   Target App     │
│  Receives fake   │
│  device info     │
└──────────────────┘
```

---

## 📱 SPOOFING TARGETS (24+ Identifiers)

### Device Identifiers

| **Identifier** | **Source Class/Method** | **Type** | **Format** |
|---|---|---|---|
| IMEI | `TelephonyManager.getImei()` | String | 15 digits (Luhn validated) |
| IMEI (Legacy) | `TelephonyManager.getDeviceId()` | String | 15 digits |
| IMSI | `TelephonyManager.getSubscriberId()` | String | 15 digits (MCC+MNC+MSIN) |
| Serial Number | `Build.SERIAL`, `Build.getSerial()` | String | 8-16 alphanumeric |
| Android ID | `Settings.Secure.ANDROID_ID` | String | 16 hex chars |
| Hardware Serial | `SystemProperties.get("ro.serialno")` | String | Variable |
| SIM Serial | `TelephonyManager.getSimSerialNumber()` | String | 19-20 digits |

### Network Identifiers

| **Identifier** | **Source Class/Method** | **Type** | **Format** |
|---|---|---|---|
| WiFi MAC | `WifiInfo.getMacAddress()` | String | XX:XX:XX:XX:XX:XX |
| Bluetooth MAC | `BluetoothAdapter.getAddress()` | String | XX:XX:XX:XX:XX:XX |
| SSID | `WifiInfo.getSSID()` | String | Network name |
| BSSID | `WifiInfo.getBSSID()` | String | XX:XX:XX:XX:XX:XX |
| Carrier Name | `TelephonyManager.getNetworkOperatorName()` | String | Carrier string |
| MCC/MNC | `TelephonyManager.getNetworkOperator()` | String | 5-6 digits |

### Advertising & Tracking

| **Identifier** | **Source Class/Method** | **Type** | **Format** |
|---|---|---|---|
| GSF ID | `Gservices.getString("android_id")` | String | 16 hex chars |
| Advertising ID | `AdvertisingIdClient.getAdvertisingIdInfo()` | String | UUID format |
| Media DRM ID | `MediaDrm.getPropertyByteArray()` | ByteArray | Device-unique |
| Firebase Installation ID | `FirebaseInstallations.getId()` | String | 22 chars |

### System Properties

| **Identifier** | **Source** | **Hook Type** | **Example Value** |
|---|---|---|---|
| Build.MANUFACTURER | Static field | Field replacement | "Google" |
| Build.MODEL | Static field | Field replacement | "Pixel 9 Pro" |
| Build.DEVICE | Static field | Field replacement | "husky" |
| Build.PRODUCT | Static field | Field replacement | "husky" |
| Build.BRAND | Static field | Field replacement | "google" |
| Build.FINGERPRINT | Static field | Field replacement | Full fingerprint |
| Build.HARDWARE | Static field | Field replacement | "husky" |
| Build.BOOTLOADER | Static field | Field replacement | "husky-1.0" |
| Build.DISPLAY | Static field | Field replacement | "AP3A.241105.007" |
| Build.ID | Static field | Field replacement | "AP3A.241105.007" |
| Build.VERSION.RELEASE | Static field | Field replacement | "16" |
| Build.VERSION.SDK_INT | Static field | Field replacement | 36 |
| Build.VERSION.SECURITY_PATCH | Static field | Field replacement | "2025-12-05" |

### Location Data

| **Identifier** | **Source Class/Method** | **Type** |
|---|---|---|
| Latitude | `Location.getLatitude()` | Double |
| Longitude | `Location.getLongitude()` | Double |
| Altitude | `Location.getAltitude()` | Double |
| Timezone | `TimeZone.getDefault()` | TimeZone |
| Locale/Language | `Locale.getDefault()` | Locale |

---

## 🛡️ ANTI-DETECTION LAYER

### Detection Methods & Bypasses

| **Detection Method** | **How Apps Detect** | **Bypass Strategy** | **Implementation** |
|---|---|---|---|
| **Stack Trace Analysis** | `Thread.getStackTrace()` for Xposed classes | Filter Xposed frames | `StackTraceHider.kt` |
| **Exception Stack Traces** | `Throwable.getStackTrace()` | Filter stack traces | `ExceptionHider.kt` |
| **Class Loading** | `Class.forName("de.robv.android.xposed.*")` | Throw ClassNotFoundException | `ClassLoaderHider.kt` |
| **/proc/maps Reading** | Read `/proc/self/maps` for `libxposed*.so` | Filter file content | `NativeLibraryHider.kt` |
| **Reflection Check** | Check for modified methods/fields | Return original info | `ReflectionHider.kt` |
| **Package Check** | `PackageManager.getPackageInfo()` for Xposed | Return null/throw | `ClassLoaderHider.kt` |

### Patterns to Hide

```kotlin
// Xposed-related patterns that must be hidden
val XPOSED_PATTERNS = listOf(
    "de.robv.android.xposed",
    "io.github.lsposed",
    "org.lsposed.lspd",
    "EdHooker",
    "LSPHooker", 
    "XposedBridge",
    "XC_MethodHook",
    "XposedHelpers"
)

// Native libraries to hide from /proc/maps
val HIDDEN_LIBRARIES = listOf(
    "libxposed",
    "liblspd",
    "libedxposed",
    "libwhale",
    "libsandhook",
    "libriru"
)

// Packages to hide
val HIDDEN_PACKAGES = listOf(
    "de.robv.android.xposed.installer",
    "io.github.lsposed.manager",
    "org.meowcat.edxposed.manager"
)
```

---

## 🎨 UI/UX DESIGN (Material 3 Expressive)

### Design System

#### Color Palette

```kotlin
// AMOLED Dark Theme (Primary)
val AmoledBlack = Color(0xFF000000)
val AmoledSurface = Color(0xFF0A0A0A)
val AmoledSurfaceVariant = Color(0xFF1A1A1A)

// Primary Colors (Teal/Privacy theme)
val PrimaryLight = Color(0xFF00BCD4)
val PrimaryDark = Color(0xFF00E5FF)
val PrimaryContainer = Color(0xFF004D56)

// Accent Colors
val SecondaryLight = Color(0xFF26A69A)
val SecondaryDark = Color(0xFF64FFDA)

// Status Colors
val StatusActive = Color(0xFF4CAF50)
val StatusInactive = Color(0xFFFF5722)
val StatusWarning = Color(0xFFFF9800)
```

#### Typography (Material 3 Expressive)

```kotlin
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
)
```

#### Motion (Spring-Based Animations)

```kotlin
// Material 3 Expressive Spring Specs
object AppMotion {
    val DefaultSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    val FastSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = Spring.StiffnessLow
    )
}
```

### Screen Designs

#### 1. Home Screen

```
┌─────────────────────────────────────────┐
│  PrivacyShield                     ⚙️   │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────────┐│
│  │       MODULE STATUS                 ││
│  │                                     ││
│  │    🛡️  ACTIVE                       ││
│  │    Protecting 12 apps               ││
│  │                                     ││
│  │    [Configure Apps]                 ││
│  └─────────────────────────────────────┘│
│                                         │
│  ┌─────────────────────────────────────┐│
│  │       QUICK STATS                   ││
│  │                                     ││
│  │  IMEI      ████████████5678         ││
│  │  Serial    ████████████ABCD         ││
│  │  MAC       ██:██:██:██:12:34        ││
│  │                                     ││
│  │    [Regenerate All]                 ││
│  └─────────────────────────────────────┘│
│                                         │
│  ┌─────────────────────────────────────┐│
│  │       ACTIVE PROFILE                ││
│  │                                     ││
│  │  📱 Pixel 9 Pro (Default)           ││
│  │                                     ││
│  │    [Switch Profile]                 ││
│  └─────────────────────────────────────┘│
│                                         │
├─────────────────────────────────────────┤
│  🏠 Home  📱 Apps  ⚙️ Spoof  👤 Profile │
└─────────────────────────────────────────┘
```

#### 2. App Selection Screen

```
┌─────────────────────────────────────────┐
│  ← Select Apps                          │
├─────────────────────────────────────────┤
│  🔍 Search apps...                      │
├─────────────────────────────────────────┤
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ [✓] WhatsApp                        ││
│  │     com.whatsapp                    ││
│  │     Using: Default Profile          ││
│  └─────────────────────────────────────┘│
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ [✓] Instagram                       ││
│  │     com.instagram.android           ││
│  │     Using: Custom Profile           ││
│  └─────────────────────────────────────┘│
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ [ ] Chrome                          ││
│  │     com.android.chrome              ││
│  │     Not enabled                     ││
│  └─────────────────────────────────────┘│
│                                         │
│  ┌─────────────────────────────────────┐│
│  │ [✓] YouTube                         ││
│  │     com.google.android.youtube      ││
│  │     Using: Default Profile          ││
│  └─────────────────────────────────────┘│
│                                         │
├─────────────────────────────────────────┤
│    [Select All]        [Clear All]      │
└─────────────────────────────────────────┘
```

#### 3. Spoof Settings Screen

```
┌─────────────────────────────────────────┐
│  ← Spoof Settings                       │
├─────────────────────────────────────────┤
│                                         │
│  DEVICE IDENTIFIERS                     │
│  ─────────────────────                  │
│                                         │
│  IMEI                                   │
│  ┌─────────────────────────────────────┐│
│  │ 358673912845672                     ││
│  │                   [🔄] [✏️] [📋]    ││
│  └─────────────────────────────────────┘│
│                                         │
│  Serial Number                          │
│  ┌─────────────────────────────────────┐│
│  │ RF8M12AB34CD56                      ││
│  │                   [🔄] [✏️] [📋]    ││
│  └─────────────────────────────────────┘│
│                                         │
│  Android ID                             │
│  ┌─────────────────────────────────────┐│
│  │ a1b2c3d4e5f67890                    ││
│  │                   [🔄] [✏️] [📋]    ││
│  └─────────────────────────────────────┘│
│                                         │
│  NETWORK IDENTIFIERS                    │
│  ─────────────────────                  │
│                                         │
│  WiFi MAC Address                       │
│  ┌─────────────────────────────────────┐│
│  │ 02:00:00:AB:CD:EF                   ││
│  │                   [🔄] [✏️] [📋]    ││
│  └─────────────────────────────────────┘│
│                                         │
├─────────────────────────────────────────┤
│  🏠 Home  📱 Apps  ⚙️ Spoof  👤 Profile │
└─────────────────────────────────────────┘
```

---

## 💾 DATA MODELS

### SpoofProfile

```kotlin
@Serializable
data class SpoofProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Device Identifiers
    val imei: String? = null,
    val imsi: String? = null,
    val serialNumber: String? = null,
    val androidId: String? = null,
    val hardwareSerial: String? = null,
    val simSerial: String? = null,
    
    // Network Identifiers
    val wifiMac: String? = null,
    val bluetoothMac: String? = null,
    val ssid: String? = null,
    val bssid: String? = null,
    val carrierName: String? = null,
    val mccMnc: String? = null,
    
    // Advertising IDs
    val gsfId: String? = null,
    val advertisingId: String? = null,
    val mediaDrmId: String? = null,
    
    // System Properties
    val manufacturer: String? = null,
    val model: String? = null,
    val device: String? = null,
    val product: String? = null,
    val brand: String? = null,
    val fingerprint: String? = null,
    val hardware: String? = null,
    val bootloader: String? = null,
    val buildDisplay: String? = null,
    val buildId: String? = null,
    val androidVersion: String? = null,
    val sdkVersion: Int? = null,
    val securityPatch: String? = null,
    
    // Location
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val locale: String? = null
)
```

### AppConfig

```kotlin
@Serializable
data class AppConfig(
    val packageName: String,
    val appLabel: String,
    val isEnabled: Boolean = false,
    val profileId: String? = null,  // null = use default profile
    val enabledSpoofs: Set<SpoofType> = SpoofType.values().toSet()
)

enum class SpoofType {
    IMEI, IMSI, SERIAL, ANDROID_ID,
    WIFI_MAC, BLUETOOTH_MAC, SSID,
    GSF_ID, ADVERTISING_ID,
    BUILD_PROPS, LOCATION
}
```

---

## 📦 BUILD CONFIGURATION

### settings.gradle.kts

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://api.xposed.info/") }
    }
}

rootProject.name = "PrivacyShield"
include(":app")
```

### build.gradle.kts (Project)

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
}
```

### build.gradle.kts (App Module)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp") version "2.2.21-1.0.31"
}

android {
    namespace = "com.privacyshield"
    compileSdk = 36
    
    defaultConfig {
        applicationId = "com.privacyshield"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // ═══════════════════════════════════════════════════════════
    // CORE ANDROID
    // ═══════════════════════════════════════════════════════════
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    
    // ═══════════════════════════════════════════════════════════
    // JETPACK COMPOSE (December 2025)
    // ═══════════════════════════════════════════════════════════
    implementation(platform("androidx.compose:compose-bom:2025.12.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // ═══════════════════════════════════════════════════════════
    // ACTIVITY & LIFECYCLE
    // ═══════════════════════════════════════════════════════════
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    
    // ═══════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════
    implementation("androidx.navigation:navigation-compose:2.9.0")
    
    // ═══════════════════════════════════════════════════════════
    // YUKIHOOKAPI (Modern Kotlin Hook Framework)
    // ═══════════════════════════════════════════════════════════
    implementation("com.highcapable.yukihookapi:api:1.2.1")
    ksp("com.highcapable.yukihookapi:ksp-xposed:1.2.1")
    
    // ═══════════════════════════════════════════════════════════
    // DATA STORAGE
    // ═══════════════════════════════════════════════════════════
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
    
    // ═══════════════════════════════════════════════════════════
    // COROUTINES
    // ═══════════════════════════════════════════════════════════
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    
    // ═══════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("io.coil-kt:coil-compose:2.7.0")  // For app icons
    
    // ═══════════════════════════════════════════════════════════
    // TESTING
    // ═══════════════════════════════════════════════════════════
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.12.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

### AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- LSPosed Module Metadata -->
    <meta-data
        android:name="xposedmodule"
        android:value="true" />
    <meta-data
        android:name="xposedminversion"
        android:value="82" />
    <meta-data
        android:name="xposeddescription"
        android:value="Device Spoofing Module with Anti-Detection" />
    <meta-data
        android:name="xposedscope"
        android:resource="@array/xposed_scope" />

    <!-- Permissions for UI functionality -->
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".PrivacyShieldApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.PrivacyShield">

        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:theme="@style/Theme.PrivacyShield">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

---

## 🚀 IMPLEMENTATION PHASES

### Phase 1: Core Infrastructure (Week 1-2)

| **Task** | **Priority** | **Effort** |
|---|---|---|
| Project setup with Gradle configuration | High | 4h |
| HookEntry.kt - LSPosed entry point | High | 2h |
| BaseHook.kt - Abstract hook class | High | 2h |
| HookManager.kt - Hook registration | High | 4h |
| SpoofDataStore.kt - DataStore setup | High | 4h |
| Value generators (IMEI, Serial, MAC, etc.) | High | 8h |

### Phase 2: Device Spoofing Hooks (Week 2-3)

| **Task** | **Priority** | **Effort** |
|---|---|---|
| IMEIHook.kt - TelephonyManager hooks | High | 4h |
| SerialHook.kt - Build.SERIAL hooks | High | 2h |
| AndroidIDHook.kt - Settings.Secure hooks | High | 2h |
| MACAddressHook.kt - WiFi/Bluetooth MAC | High | 4h |
| GSFIDHook.kt - Google Services ID | Medium | 2h |
| AdvertisingIDHook.kt - Ad ID hooks | Medium | 2h |
| BuildPropsHook.kt - Build.* fields | High | 4h |
| SystemPropertiesHook.kt - SystemProperties.get() | High | 4h |
| LocationHook.kt - GPS coordinates | Medium | 4h |

### Phase 3: Anti-Detection Layer (Week 3-4)

| **Task** | **Priority** | **Effort** |
|---|---|---|
| AntiDetectManager.kt - Orchestrator | High | 2h |
| StackTraceHider.kt - Stack trace filtering | High | 4h |
| ClassLoaderHider.kt - Class loading bypass | High | 4h |
| NativeLibraryHider.kt - /proc/maps filtering | High | 6h |
| ReflectionHider.kt - Method/field hiding | Medium | 4h |
| ExceptionHider.kt - Exception trace filtering | Medium | 2h |

### Phase 4: Data Management (Week 4-5)

| **Task** | **Priority** | **Effort** |
|---|---|---|
| ProfileManager.kt - Profile CRUD | High | 6h |
| AppScopeManager.kt - Per-app config | High | 4h |
| Data models (SpoofProfile, AppConfig) | High | 2h |
| Import/Export profiles | Low | 4h |

### Phase 5: UI Implementation (Week 5-7)

| **Task** | **Priority** | **Effort** |
|---|---|---|
| Theme.kt - Material 3 Expressive setup | High | 4h |
| Color.kt, Typography.kt, Shapes.kt | High | 2h |
| Motion.kt - Spring animations | Medium | 2h |
| HomeScreen.kt | High | 6h |
| AppSelectionScreen.kt | High | 6h |
| SpoofSettingsScreen.kt | High | 8h |
| ProfileScreen.kt | Medium | 6h |
| DiagnosticsScreen.kt | Low | 4h |
| SettingsScreen.kt | Low | 4h |
| Navigation setup | High | 2h |
| Reusable components | Medium | 6h |

### Phase 6: Testing & Polish (Week 7-8)

| **Task** | **Priority** | **Effort** |
|---|---|---|
| Unit tests for generators | High | 4h |
| Unit tests for hooks | High | 6h |
| Integration testing on devices | High | 8h |
| UI polish and animations | Medium | 4h |
| Performance optimization | Medium | 4h |
| Documentation | Medium | 4h |

---

## 📊 TESTING CHECKLIST

### Functional Testing

- [ ] Module loads without crash in LSPosed
- [ ] IMEI spoofing works (verified with IMEI checker app)
- [ ] Serial number spoofing works
- [ ] Android ID spoofing works
- [ ] MAC address spoofing works (WiFi + Bluetooth)
- [ ] GSF ID spoofing works
- [ ] Advertising ID spoofing works
- [ ] Build properties spoofing works
- [ ] Location spoofing works
- [ ] Per-app profiles work correctly
- [ ] Profile switching works
- [ ] Value regeneration works

### Anti-Detection Testing

- [ ] Stack trace analysis apps cannot detect hooks
- [ ] Class loading detection fails
- [ ] /proc/maps reading doesn't reveal Xposed
- [ ] Reflection checks don't show modifications
- [ ] Apps using RootBeer library don't detect hooks
- [ ] Apps using SafetyNet Helper don't detect hooks

### UI Testing

- [ ] All screens render correctly
- [ ] Navigation works properly
- [ ] Dark theme displays correctly
- [ ] Dynamic colors work on Android 12+
- [ ] Animations are smooth
- [ ] Responsive on different screen sizes

### Compatibility Testing

- [ ] Works on Android 10 (API 29)
- [ ] Works on Android 11 (API 30)
- [ ] Works on Android 12 (API 31)
- [ ] Works on Android 13 (API 33)
- [ ] Works on Android 14 (API 34)
- [ ] Works on Android 15 (API 35)
- [ ] Works on Android 16 (API 36)
- [ ] Works with Magisk 30.6+
- [ ] Works with LSPosed 1.10.2+
- [ ] Works alongside Shamiko
- [ ] Works alongside Play Integrity Fix

---

## 📚 EXTERNAL MODULE RECOMMENDATIONS

For complete protection, users should install these alongside PrivacyShield:

| **Module** | **Purpose** | **Repository** |
|---|---|---|
| **Shamiko** | Root hiding, Zygisk deny list | GitHub: LSPosed/Shamiko |
| **Play Integrity Fix** | Pass Play Integrity checks | GitHub: chiteroman/PlayIntegrityFix |
| **Tricky Store** | Hardware attestation | GitHub: 5ec1cff/TrickyStore |
| **Zygisk-Next** | Zygisk on KernelSU/APatch | GitHub: Dr-TSNG/ZygiskNext |

---

## 📄 LICENSE

GPL-3.0 - Open Source for Educational Purposes

---

## ⚠️ LEGAL DISCLAIMER

This module is provided for **educational and security research purposes only**. Users are responsible for ensuring their use complies with applicable laws and terms of service. The developers are not responsible for any misuse of this software.

**Do NOT use this module for:**
- Fraudulent transactions
- Bypassing security for unauthorized access
- Violating terms of service
- Any illegal activities

---

## 🔗 REFERENCES

- [YukiHookAPI Documentation](https://highcapable.github.io/YukiHookAPI/en/) ⭐ Primary Hook Framework
- [YukiHookAPI GitHub](https://github.com/HighCapable/YukiHookAPI)
- [LSPosed GitHub](https://github.com/LSPosed/LSPosed)
- [Material 3 Design Guidelines](https://m3.material.io/)
- [Material 3 Expressive](https://developer.android.com/develop/ui/compose/designsystems/material3-expressive)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Android Security Best Practices](https://developer.android.com/topic/security/best-practices)

---

**Document Version:** 2.0  
**Last Updated:** December 14, 2025  
**Author:** PrivacyShield Development Team
