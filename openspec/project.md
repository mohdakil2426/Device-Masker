# Project Context

## Purpose

**PrivacyShield** is an open-source LSPosed module focused on **device identifier spoofing** with a robust **anti-detection layer**. The module is designed to protect user privacy by spoofing device identifiers while preventing apps from detecting hook injection.

### Core Philosophy
> **"Do one thing excellently"** - Spoof device identifiers and hide the injection, nothing more.

### Scope
- ✅ IN SCOPE: Device spoofing (IMEI, Serial, MAC, etc.), Anti-detection, Material 3 UI
- ❌ OUT OF SCOPE: Root hiding (Shamiko), SafetyNet bypass (PIF), Play Integrity (Tricky Store)

## Tech Stack

- **Language**: Kotlin 2.2.21
- **Android SDK**: compileSdk/targetSdk 36, minSdk 26
- **Java**: JDK 21
- **Hook Framework**: YukiHookAPI 1.2.1 (LSPosed module)
- **UI**: Jetpack Compose + Material 3 Expressive
- **Data Storage**: DataStore Preferences 1.1.2
- **Async**: Kotlinx Coroutines 1.9.0
- **Serialization**: Kotlinx Serialization JSON 1.8.0
- **Logging**: Timber 5.0.1
- **Images**: Coil Compose 2.7.0

## Project Conventions

### Code Style
- Kotlin official style guide
- 4-space indentation
- 120-character line limit
- Package names: `com.akil.privacyshield.*`
- Use `@Serializable` for data classes that need persistence
- Use `data class` for immutable state
- Use `object` for singletons (Hookers, Managers)

### Architecture Patterns
- **Hook Layer**: YukiBaseHooker classes, one per domain
- **Data Layer**: Repository pattern with DataStore
- **UI Layer**: MVVM with StateFlow, Unidirectional Data Flow
- **State**: Immutable data classes, `.copy()` for updates
- **Navigation**: Compose Navigation with sealed class destinations

### Naming Conventions
- Hookers: `*Hooker.kt` (e.g., `DeviceHooker.kt`)
- Generators: `*Generator.kt` (e.g., `IMEIGenerator.kt`)
- Screens: `*Screen.kt` (e.g., `HomeScreen.kt`)
- Components: Descriptive names (e.g., `SpoofValueCard.kt`)
- ViewModels: `*ViewModel.kt` (e.g., `SpoofViewModel.kt`)

### Testing Strategy
- Unit tests for generators and validators
- Manual integration testing on real devices
- Test on Android 10-16 range
- Test with RootBeer, SafetyNet Helper for anti-detection

### Git Workflow
- Feature branches: `feature/description`
- Bug fix branches: `fix/description`
- Conventional commits: `feat:`, `fix:`, `docs:`, `refactor:`

## Domain Context

### YukiHookAPI Concepts
- `@InjectYukiHookWithXposed`: Entry annotation for modules
- `YukiBaseHooker`: Base class for modular hooks
- `encase {}`: Lambda for hook registration
- `loadHooker()`: Load a hooker class
- `.toClass()`: Convert string to Class for hooking
- `.method {}`: Find method to hook
- `.hook {}`: Apply hook with `before {}` / `after {}`
- `optional()`: Mark method as optional (may not exist)

### LSPosed Concepts
- Module must declare xposedmodule metadata in manifest
- Module scope defines which apps can be hooked
- Hooks run in target process, not module process
- XSharedPreferences or DataChannel for cross-process data

### Spoofing Concepts
- IMEI: 15 digits, Luhn checksum validated
- MAC: 6 bytes, unicast bit must be set
- Build.FINGERPRINT: Complex string format
- Android ID: 16 hex characters

## Important Constraints

- Must NOT crash target apps (use optional() for uncertain methods)
- Must NOT block UI thread (use coroutines)
- Must load anti-detection BEFORE spoofing hooks
- Must generate valid formats (Luhn for IMEI, unicast for MAC)
- Must support Android 8.0 (API 26) through Android 16 (API 36)

## External Dependencies

- **LSPosed**: Framework that enables Xposed modules
- **Magisk**: Root solution providing Zygisk
- **Companion Modules** (installed by user separately):
  - Shamiko: Root hiding
  - Play Integrity Fix: SafetyNet/Play Integrity bypass
  - Tricky Store: Hardware attestation

## Documentation References

- [YukiHookAPI Documentation](https://highcapable.github.io/YukiHookAPI/en/)
- [LSPosed GitHub](https://github.com/LSPosed/LSPosed)
- [Material 3 Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [PRD Document](./docs/prd/PrivacyShield_PRD.md)
- [Best Practices](./docs/prd/CodeExamples_BestPractices.md)
