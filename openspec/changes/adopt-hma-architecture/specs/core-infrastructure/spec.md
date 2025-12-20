# core-infrastructure Spec Delta

## REPLACED Requirements

### Requirement: Project Structure

The project SHALL be organized as a **multi-module Gradle project** following the HMA-OSS architecture pattern.

#### Scenario: Module Structure

- **WHEN** the project is opened in Android Studio
- **THEN** three modules are visible: `:app`, `:common`, `:xposed`
- **AND** `settings.gradle.kts` includes all three modules
- **AND** each module has its own `build.gradle.kts`

#### Scenario: Module Dependencies

- **WHEN** module dependencies are analyzed
- **THEN** `:app` depends on `:common` and `:xposed`
- **AND** `:xposed` depends on `:common`
- **AND** `:common` has no internal module dependencies

#### Scenario: Module Responsibilities

- **WHEN** code is organized across modules
- **THEN** `:common` contains AIDL interface, models, and constants
- **AND** `:xposed` contains YukiHookAPI entry, service, and hookers
- **AND** `:app` contains UI, ConfigManager, and ServiceClient

---

### Requirement: Build Configuration

The project SHALL use Gradle Kotlin DSL with version catalog.

#### Scenario: :common Module Build

- **WHEN** `:common` module is built
- **THEN** AIDL build feature is enabled
- **AND** kotlinx-serialization-json is available
- **AND** Namespace is `com.astrixforge.devicemasker.common`

#### Scenario: :xposed Module Build

- **WHEN** `:xposed` module is built
- **THEN** YukiHookAPI and KSP are configured
- **AND** Xposed API is compileOnly
- **AND** KavaRef dependencies are included
- **AND** Hidden API Bypass is included
- **AND** Namespace is `com.astrixforge.devicemasker.xposed`

#### Scenario: :app Module Build

- **WHEN** `:app` module is built
- **THEN** Jetpack Compose is configured
- **AND** Material 3 is included
- **AND** Navigation Compose is included
- **AND** DataStore is NOT included (removed)
- **AND** AIDL build feature is enabled

---

### Requirement: LSPosed Module Metadata

The module SHALL be properly recognized by LSPosed.

#### Scenario: Manifest Configuration

- **WHEN** the APK is inspected
- **THEN** `xposedmodule = true` metadata exists
- **AND** `xposeddescription` metadata exists
- **AND** `xposedminversion = 93` or higher
- **AND** ContentProvider for ServiceProvider is declared

#### Scenario: Xposed Init File

- **WHEN** the APK assets are inspected
- **THEN** `assets/xposed_init` file exists in xposed module
- **AND** Contains entry class `com.astrixforge.devicemasker.xposed.XposedEntry`

---

### Requirement: Hook Entry Point

The xposed module SHALL provide YukiHookAPI entry point.

#### Scenario: XposedEntry Structure

- **WHEN** XposedEntry.kt is loaded by LSPosed
- **THEN** Class has `@InjectYukiHookWithXposed` annotation
- **AND** Class implements `IYukiHookXposedInit`
- **AND** `onInit()` configures debug settings
- **AND** `onHook()` contains hook registration

#### Scenario: System Server Hooks

- **WHEN** `onHook()` is called
- **THEN** `loadSystem { }` block registers system_server hooks
- **AND** `UserService.register()` is called to start service
- **AND** `DeviceMaskerService` is instantiated

#### Scenario: Hooker Loading

- **WHEN** service is initialized
- **THEN** AntiDetectHooker is loaded first
- **AND** All other hookers (Device, Network, etc.) are loaded
- **AND** Hookers read config from in-memory service object

---

## ADDED Requirements

### Requirement: System Service (DeviceMaskerService)

The xposed module SHALL provide an AIDL service running in system_server.

#### Scenario: Service Initialization

- **WHEN** device boots and LSPosed loads module
- **THEN** DeviceMaskerService instance is created
- **AND** Data directory is created at `/data/system/devicemasker/`
- **AND** Config is loaded from `config.json` into memory
- **AND** All hooks are installed

#### Scenario: AIDL Interface Implementation

- **WHEN** UI app calls AIDL methods
- **THEN** `readConfig()` returns current config JSON
- **AND** `writeConfig(json)` updates in-memory config and saves to disk
- **AND** `isModuleEnabled()` returns module state
- **AND** All hooks immediately see config changes

#### Scenario: Binder Transfer

- **WHEN** UI app starts
- **THEN** Service detects app UID becoming active
- **AND** Service sends binder to app via ContentProvider
- **AND** App can call AIDL methods

---

### Requirement: ServiceClient and ServiceProvider

The app module SHALL connect to xposed service via AIDL.

#### Scenario: ServiceProvider

- **WHEN** xposed module sends binder
- **THEN** ServiceProvider.call() receives binder in extras
- **AND** Binder is passed to ServiceClient.linkService()

#### Scenario: ServiceClient Connection

- **WHEN** binder is linked
- **THEN** ServiceClient can call all AIDL methods
- **AND** Death recipient is registered for binder death
- **AND** UI shows "Module Active" status

#### Scenario: Binder Death

- **WHEN** xposed service stops or crashes
- **THEN** ServiceClient.binderDied() is called
- **AND** UI shows "Module Inactive" status
- **AND** UI gracefully handles disconnection
