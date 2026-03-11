# Device Masker - Technology Stack

> Last Updated: 2026-03-09

---

## Build Configuration

| Component | Version |
|-----------|---------|
| **Android Gradle Plugin (AGP)** | 9.0.1 |
| **Kotlin** | 2.3.0 |
| **KSP (Kotlin Symbol Processing)** | 2.3.4 |
| **Gradle** | 9.1.0 |
| **JDK Toolchain** | 17 |
| **Spotless** | 7.0.4 |
| **ktfmt** | 0.54 |

---

## Android SDK

| Component | Version |
|-----------|---------|
| **compileSdk** | 36 |
| **targetSdk** | 36 |
| **minSdk** | 26 |
| **Android Support** | Android 8.0 - 16 (API 26-36) |

---

## Core Android Libraries

| Library | Version | Description |
|---------|---------|-------------|
| **androidx.core:core-ktx** | 1.17.0 | Android KTX extensions |
| **androidx.appcompat:appcompat** | 1.7.1 | AppCompat library |
| **com.google.android.material:material** | 1.13.0 | Material Design Components |

---

## Jetpack Compose

| Library | Version | Description |
|---------|---------|-------------|
| **Compose BOM** | 2025.12.00 | Bill of Materials (manages versions) |
| **Material 3** | 1.5.0-alpha11 | Material 3 Expressive design system |
| **compose-ui** | (BOM) | Core UI primitives |
| **compose-ui-graphics** | (BOM) | Graphics utilities |
| **compose-ui-tooling** | (BOM) | Debug tooling |
| **compose-ui-tooling-preview** | (BOM) | Preview annotations |
| **compose-animation** | (BOM) | Animation APIs |
| **compose-foundation** | (BOM) | Foundation components |
| **compose-material-icons-extended** | (BOM) | Extended icon set |
| **material3-window-size-class** | 1.5.0-alpha11 | Window size utilities |
| **androidx.graphics:graphics-shapes** | 1.0.1 | Shape rendering (Morphing shapes) |

---

## Activity & Lifecycle

| Library | Version | Description |
|---------|---------|-------------|
| **androidx.activity:activity-compose** | 1.12.1 | Compose Activity integration |
| **androidx.lifecycle:lifecycle-runtime-ktx** | 2.10.0 | Lifecycle runtime |
| **androidx.lifecycle:lifecycle-runtime-compose** | 2.10.0 | Compose Lifecycle integration |
| **androidx.lifecycle:lifecycle-viewmodel-compose** | 2.10.0 | ViewModel Compose integration |

---

## Navigation

| Library | Version | Description |
|---------|---------|-------------|
| **androidx.navigation:navigation-compose** | 2.9.6 | Compose Navigation |

---

## Xposed / Hooking Framework

| Library | Version | Description |
|---------|---------|-------------|
| **Xposed API** | 82 | Core Xposed framework API |
| **YukiHookAPI** | 1.3.1 | Modern Kotlin hook framework |
| **YukiHookAPI KSP Xposed** | 1.3.1 | KSP processor for Xposed |
| **KavaRef Core** | 1.0.2 | Reflection API |
| **KavaRef Extension** | 1.0.2 | Reflection extensions |
| **HiddenAPIBypass** | 6.1 | Bypass hidden API restrictions |

---

## Data Storage

| Library | Version | Description |
|---------|---------|-------------|
| **androidx.datastore:datastore-preferences** | 1.2.0 | Preferences DataStore |
| **kotlinx-serialization-json** | 1.9.0 | JSON serialization |

---

## Coroutines

| Library | Version | Description |
|---------|---------|-------------|
| **kotlinx-coroutines-core** | 1.10.2 | Core coroutines |
| **kotlinx-coroutines-android** | 1.10.2 | Android-specific coroutine dispatchers |

---

## Utilities

| Library | Version | Description |
|---------|---------|-------------|
| **Timber** | 5.0.1 | Logging utility |
| **Coil Compose** | 3.2.0 | Image loading for Compose |

---

## Testing

| Library | Version | Description |
|---------|---------|-------------|
| **JUnit** | 4.13.2 | Unit testing framework |
| **androidx.test.ext:junit** | 1.2.1 | Android JUnit extensions |
| **androidx.test.espresso:espresso-core** | 3.7.0 | UI testing framework |
| **kotlinx-coroutines-test** | 1.10.2 | Coroutines testing utilities |
| **compose-ui-test-junit4** | (BOM) | Compose UI testing |
| **compose-ui-test-manifest** | (BOM) | Compose test manifest |

---

## Module Dependencies Summary

### :app Module
- All Compose libraries
- YukiHookAPI + KSP + KavaRef
- Navigation, Lifecycle, DataStore
- Coil, Timber, Coroutines
- Xposed API (compileOnly)
- Project dependencies: `:common`, `:xposed`

### :xposed Module
- YukiHookAPI (API only)
- KavaRef (core + extension)
- HiddenAPIBypass
- Xposed API (compileOnly)
- Kotlinx Serialization
- Coroutines
- Project dependency: `:common`

### :common Module
- Kotlinx Serialization
- Kotlinx Coroutines Core
- AIDL enabled for IPC

---

## Gradle Plugins Applied

```kotlin
// Root build.gradle.kts
com.android.application (apply false)
com.android.library (apply false)
org.jetbrains.kotlin.android (apply false)
org.jetbrains.kotlin.compose (apply false)
org.jetbrains.kotlin.plugin.serialization (apply false)
com.google.devtools.ksp (apply false)
com.diffplug.spotless
```

---

## Repositories

```kotlin
google()
mavenCentral()
maven("https://jitpack.io")
maven("https://maven.aliyun.com/repository/public")
maven("https://repo.lsposed.foundation/")
```

---

## Code Style

- **Formatter**: ktfmt 0.54 (kotlinlangStyle)
- **Indentation**: 4 spaces
- **Line Ending**: Unix (LF)
- **Trailing Whitespace**: Trimmed
