# Project Context

## Purpose

**Device Masker** is an open-source LSPosed module focused on **device identifier spoofing** with a robust **anti-detection layer**. The module is designed to protect user privacy by spoofing device identifiers while preventing apps from detecting hook injection.

### Core Philosophy
> **"Do one thing excellently"** - Spoof device identifiers and hide the injection, nothing more.

### Scope
- ✅ IN SCOPE: Device spoofing (IMEI, Serial, MAC, etc.), Anti-detection, Material 3 UI
- ❌ OUT OF SCOPE: Root hiding (Shamiko), SafetyNet bypass (PIF), Play Integrity (Tricky Store)

## Tech Stack

- **Language**: Kotlin 2.3.0
- **Android SDK**: compileSdk/targetSdk 36, minSdk 26
- **Java**: JDK 25
- **Hook Framework**: YukiHookAPI 1.3.1 (LSPosed module)
- **UI**: Jetpack Compose + Material 3 Expressive
- **Data Storage**: DataStore Preferences 1.1.2
- **Async**: Kotlinx Coroutines 1.9.0
- **Serialization**: Kotlinx Serialization JSON 1.8.0
- **Logging**: Timber 5.0.1
- **Images**: Coil Compose 2.7.0
- **IPC**: AIDL Binder (system_server → app processes)

## Project Conventions

### Code Style
- Kotlin official style guide
- 4-space indentation
- 120-character line limit
- Package names: `com.astrixforge.devicemasker.*`
- Use `@Serializable` for data classes that need persistence
- Use `data class` for immutable state
- Use `object` for singletons (Hookers, Managers)

### Architecture Patterns

#### Hook Layer (AIDL Service Architecture)
- **DeviceMaskerService**: Singleton AIDL service running in `system_server`
- **ServiceBridge**: ContentProvider for service discovery
- **SystemServiceHooker**: Hooks AMS.systemReady() for service initialization
- **BaseSpoofHooker**: Abstract base with service access and utilities
- **Hooker classes**: Extend BaseSpoofHooker(service, packageName)

#### Data Layer
- **Repository pattern** with DataStore and ConfigManager
- **ConfigManager**: Persistent storage in `/data/misc/devicemasker/config.json`
- **ServiceClient**: UI-side AIDL client for real-time updates

#### UI Layer
- **MVVM** with StateFlow (Unidirectional Data Flow)
- **ViewModels** use ServiceClient for config operations
- **Navigation**: Compose Navigation with simple string routes

#### State
- Immutable data classes with `.copy()`
- Thread-safe: `AtomicReference`, `ConcurrentHashMap` in service

### Naming Conventions
- Hookers: `*Hooker.kt` (e.g., `DeviceHooker.kt`)
- Generators: `*Generator.kt` (e.g., `SIMGenerator.kt`)
- Screens: `*Screen.kt` (e.g., `GroupsScreen.kt`)
- Components: Descriptive names (e.g., `GroupCard.kt`)
- ViewModels: `*ViewModel.kt` (e.g., `GroupsViewModel.kt`)
- Services: `*Service.kt` (e.g., `DeviceMaskerService.kt`)
- AIDL Interfaces: `I*Service.aidl` (e.g., `IDeviceMaskerService.aidl`)

### Testing Strategy
- Unit tests for generators and validators
- Manual integration testing on real devices
- Test on Android 10-16 range (primary), 8-9 (secondary)
- Test with RootBeer, SafetyNet Helper for anti-detection
- Boot stability testing (100+ cycles)

### Git Workflow
- Feature branches: `feature/description`
- Bug fix branches: `fix/description`
- Conventional commits: `feat:`, `fix:`, `docs:`, `refactor:`

## Domain Context

### YukiHookAPI Concepts
- `@InjectYukiHookWithXposed`: Entry annotation for modules
- `YukiBaseHooker`: Base class for modular hooks
- `encase {}`: Lambda for hook registration
- `loadSystem {}`: System Framework scope (system_server)
- `loadZygote {}`: Zygote scope (pre-app fork)
- `loadApp {}`: Per-app scope
- `loadHooker()`: Load a hooker class
- `.toClass()`: Convert string to Class for hooking
- `.method {}`: Find method to hook
- `.hook {}`: Apply hook with `before {}` / `after {}`
- `optional()`: Mark method as optional (may not exist)
- `onXposedEvent()`: Access raw Xposed API

### AIDL Service Concepts (New)
- **DeviceMaskerService**: Runs in `system_server` process
- **IDeviceMaskerService.aidl**: Defines IPC contract
- **Binder**: Cross-process object for method calls
- **ContentProvider Bridge**: Service discovery mechanism
- **Real-time updates**: Config changes apply immediately via binder

### LSPosed Concepts
- Module must declare xposedmodule metadata in manifest
- **System Framework scope**: Hook "android" for system-wide access
- Hooks run in target process, not module process
- AIDL Binder for cross-process communication

### Spoofing Concepts
- IMEI: 15 digits, Luhn checksum validated
- MAC: 6 bytes, unicast bit must be set
- Build.FINGERPRINT: Complex string format
- Android ID: 16 hex characters

## Important Constraints

- Must NOT crash target apps (use optional() for uncertain methods)
- Must NOT crash system_server (wrap all hooks in try-catch)
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
- [Architecture Migration Plan](../docs/ARCHITECTURE_MIGRATION_PLAN.md)
- [HMA-OSS Reference](../docs/oth-repo-projects/hma-oss.txt)
