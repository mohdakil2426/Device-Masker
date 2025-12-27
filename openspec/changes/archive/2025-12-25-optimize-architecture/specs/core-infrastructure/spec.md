# core-infrastructure Specification Delta

## MODIFIED Requirements

### Requirement: ViewModel Architecture

Each screen in the application SHALL have a dedicated ViewModel that:

1. Extends `androidx.lifecycle.ViewModel`
2. Exposes UI state via `StateFlow<*State>`
3. Uses `viewModelScope` for coroutine operations
4. Provides action methods for UI events
5. Collects Repository flows and transforms to UI state
6. **Uses thread-safe `.update {}` pattern for StateFlow modifications**

#### Scenario: ViewModel Lifecycle

- **WHEN** a screen is navigated to
- **THEN** its ViewModel is created or retrieved from ViewModelStore
- **AND** the ViewModel survives configuration changes (rotation)
- **AND** the ViewModel is cleared when the screen is removed from back stack

#### Scenario: State Management

- **WHEN** a ViewModel receives an action call
- **THEN** it updates internal `MutableStateFlow` using `.update {}` block
- **AND** the UI recomposes with new state
- **AND** no direct repository access occurs in Composable functions

#### Scenario: Thread-Safe State Updates

- **WHEN** multiple coroutines update state concurrently
- **THEN** all updates are applied correctly without data loss
- **AND** `_state.update { it.copy(...) }` is used instead of `_state.value = _state.value.copy(...)`

---

## ADDED Requirements

### Requirement: User Documentation for Config Behavior

The application SHALL clearly document XSharedPreferences caching behavior to users.

#### Scenario: In-App Documentation

- **WHEN** the user views the Diagnostics screen
- **THEN** an info card explains that config changes require app restart
- **AND** the info uses clear, non-technical language

#### Scenario: README Documentation

- **WHEN** a user reads the README
- **THEN** an "Important Notes" section explains the restart requirement
- **AND** the documentation provides clear steps to apply config changes
