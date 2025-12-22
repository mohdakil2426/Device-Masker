## MODIFIED Requirements

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

## ADDED Requirements

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
- `ui/screens/profile/` - Profile list feature
- `ui/screens/profiledetail/` - Profile detail/edit feature
- `ui/screens/settings/` - Settings feature
- `ui/screens/diagnostics/` - Diagnostics feature

#### Scenario: Feature Discovery

- **WHEN** a developer needs to modify a screen
- **THEN** all related files (Screen, ViewModel, State) are in one package
- **AND** the package name clearly indicates the feature
- **AND** no cross-feature dependencies exist between screen packages
