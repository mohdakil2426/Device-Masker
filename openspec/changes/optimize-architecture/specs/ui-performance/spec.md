# ui-performance Specification Delta

## ADDED Requirements

### Requirement: Stable Keys for Lazy Lists

All LazyColumn and LazyRow usages SHALL provide stable key parameters for efficient recomposition.

#### Scenario: Group List Keys
- **WHEN** the groups list is rendered in LazyColumn
- **THEN** each group item has `key = { group.id }`
- **AND** adding/removing groups only recomposes affected items

#### Scenario: App List Keys
- **WHEN** the apps list is rendered in LazyColumn
- **THEN** each app item has `key = { app.packageName }`
- **AND** filtering apps preserves scroll position

#### Scenario: Item Reorder Animation
- **WHEN** items are reordered in a lazy list
- **THEN** Compose animates items to new positions smoothly
- **AND** no items are unnecessarily destroyed and recreated

---

### Requirement: Derived State for Expensive Computations

Expensive computations in Composables SHALL use `derivedStateOf` to minimize recomputations.

#### Scenario: App Filtering
- **WHEN** the app list is filtered by search query
- **THEN** the filter runs only when apps or query changes
- **AND** other state changes do not trigger re-filtering

#### Scenario: Count Calculations
- **WHEN** enabled app counts are displayed
- **THEN** the count runs only when the app list changes
- **AND** unrelated recompositions skip the count

#### Scenario: Derived State Dependencies
- **WHEN** derivedStateOf is used with remember
- **THEN** the dependencies are explicitly declared via remember keys
- **AND** the derived value updates only when dependencies change

---

### Requirement: Thread-Safe State Updates

All ViewModel StateFlow updates SHALL use thread-safe patterns.

#### Scenario: Concurrent State Updates
- **WHEN** multiple coroutines update state concurrently
- **THEN** all updates are applied without data loss
- **AND** `.update {}` is used instead of `.value = .copy()`

#### Scenario: Atomic Read-Modify-Write
- **WHEN** a state update depends on current state
- **THEN** the update block receives the current state atomically
- **AND** no race condition can cause stale reads

---

### Requirement: Config Sync Documentation

The UI SHALL clearly inform users about XSharedPreferences caching behavior.

#### Scenario: DiagnosticsScreen Info Card
- **WHEN** the user opens DiagnosticsScreen
- **THEN** an info card is visible explaining config sync
- **AND** the card states that target apps must restart for changes

#### Scenario: Card Visibility
- **WHEN** the DiagnosticsScreen scrolls
- **THEN** the info card remains visible at top or prominent position
- **AND** the card uses secondary container color for attention
