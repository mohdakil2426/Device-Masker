# core-infrastructure Specification

## Purpose
TBD - created by archiving change implement-privacy-shield-module. Update Purpose after archive.
## Requirements
### Requirement: Build Configuration

The project SHALL be configured with the following build settings for Android 8.0 through Android 16 compatibility:

- compileSdk = 36
- minSdk = 26
- targetSdk = 36
- Java 21 source and target compatibility
- Kotlin 2.2.21 with KSP annotation processing
- YukiHookAPI 1.2.1 as the hooking framework
- Jetpack Compose BOM 2025.12.00 for UI
- DataStore Preferences 1.1.2 for persistence
- Kotlinx Coroutines 1.9.0 for async operations

#### Scenario: Successful Gradle Sync

- **WHEN** the project is opened in Android Studio
- **THEN** Gradle sync completes without errors
- **AND** all dependencies are resolved from configured repositories

#### Scenario: Module Compilation

- **WHEN** the project is built
- **THEN** compilation succeeds with no errors
- **AND** KSP generates required YukiHookAPI entry classes

---

### Requirement: LSPosed Module Metadata

The AndroidManifest.xml SHALL declare the app as an LSPosed/Xposed module with appropriate metadata.

#### Scenario: Module Recognition

- **WHEN** the APK is installed on a device with LSPosed
- **THEN** the module appears in LSPosed Manager
- **AND** the module description reads "Device Spoofing Module with Anti-Detection"
- **AND** the minimum Xposed version is 82

#### Scenario: Module Scope

- **WHEN** the module is enabled in LSPosed
- **THEN** it can hook any user-selected application based on xposed_scope configuration

---

### Requirement: Project Structure

The project SHALL follow MVVM architecture with separation of concerns:

- `hook/` - YukiHookAPI hook entry point (delegates to :xposed module)
- `data/` - Data layer with repositories, models, and DataStore
- `service/` - AIDL service client and configuration management
- `ui/` - Jetpack Compose UI organized by feature:
  - `ui/screens/[feature]/` - Feature packages with Screen, ViewModel, State
  - `ui/components/` - Reusable UI components
  - `ui/navigation/` - Navigation setup
  - `ui/theme/` - Material 3 theming
- `utils/` - Utility functions and constants

Each screen feature package SHALL contain:
- `*Screen.kt` - Composable UI (stateless, receives ViewModel)
- `*ViewModel.kt` - State management and business logic
- `*State.kt` - Immutable UI state data class

#### Scenario: Code Organization

- **WHEN** a developer explores the codebase
- **THEN** each package has a single, clear responsibility
- **AND** dependencies flow from UI → ViewModel → Repository → Data
- **AND** ViewModels do not expose Repository directly to Composables

#### Scenario: MVVM Pattern Compliance

- **WHEN** a Screen composable is reviewed
- **THEN** it accepts a ViewModel parameter (not Repository)
- **AND** it collects state using `collectAsStateWithLifecycle()`
- **AND** it delegates actions to ViewModel methods
- **AND** the stateless Content composable remains unchanged for previews

---

### Requirement: Module Application

The PrivacyShieldApp class SHALL initialize global components when the module app starts.

#### Scenario: Application Startup

- **WHEN** the PrivacyShield app is launched
- **THEN** Timber logging is initialized (debug builds only)
- **AND** DataStore is configured and ready for use
- **AND** YukiHookAPI module status can be queried

---

### Requirement: Hook Entry Point

The HookEntry object SHALL serve as the main entry point for YukiHookAPI initialization.

#### Scenario: Hook Loading Order

- **WHEN** a target application is launched with the module enabled
- **THEN** AntiDetectHooker loads FIRST
- **THEN** all spoofing hookers load in sequence
- **AND** debug logs are emitted (in debug builds)

#### Scenario: Selective Loading

- **WHEN** an app is not in the enabled apps list
- **THEN** no hooks are applied to that app
- **AND** no performance overhead is incurred

### Requirement: ViewModel Architecture

Each screen in the application SHALL have a dedicated ViewModel that:

1. Extends `androidx.lifecycle.ViewModel`
2. Exposes UI state via `StateFlow<*State>`
3. Uses `viewModelScope` for coroutine operations
4. Provides action methods for UI events
5. Collects Repository flows and transforms to UI state

#### Scenario: ViewModel Lifecycle

- **WHEN** a screen is navigated to
- **THEN** its ViewModel is created or retrieved from ViewModelStore
- **AND** the ViewModel survives configuration changes (rotation)
- **AND** the ViewModel is cleared when the screen is removed from back stack

#### Scenario: State Management

- **WHEN** a ViewModel receives an action call
- **THEN** it updates internal `MutableStateFlow`
- **AND** the UI recomposes with new state
- **AND** no direct repository access occurs in Composable functions

---

### Requirement: Feature Package Structure

Each screen feature SHALL be organized in its own package under `ui/screens/`:

- `ui/screens/home/` - Home screen feature
- `ui/screens/groups/` - Groups list feature
- `ui/screens/groupspoofing/` - Group spoofing/edit feature
- `ui/screens/settings/` - Settings feature
- `ui/screens/diagnostics/` - Diagnostics feature

#### Scenario: Feature Discovery

- **WHEN** a developer needs to modify a screen
- **THEN** all related files (Screen, ViewModel, State) are in one package
- **AND** the package name clearly indicates the feature
- **AND** no cross-feature dependencies exist between screen packages

