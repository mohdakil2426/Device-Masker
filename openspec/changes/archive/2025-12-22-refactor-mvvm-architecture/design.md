# Design: Pure MVVM Architecture

## Context

Device Masker is an LSPosed module with a Jetpack Compose UI. The current architecture uses a Repository-Direct pattern where Composable screens directly access `SpoofRepository`. This works but violates Android architecture guidelines and makes testing difficult.

## Goals

1. Introduce MVVM pattern with ViewModels for each screen
2. Improve testability by separating UI from business logic
3. Leverage ViewModel lifecycle awareness
4. Organize code by feature (screen-based packages)

## Non-Goals

1. Add Clean Architecture (UseCases, Domain layer)
2. Add dependency injection framework (Hilt/Koin)
3. Change any visual aspects of the UI
4. Modify `:common` or `:xposed` modules

## Architecture Pattern

### MVVM Flow

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

### ViewModel Pattern

```kotlin
class HomeViewModel(
    private val repository: SpoofRepository
) : ViewModel() {
    
    // Private mutable state
    private val _state = MutableStateFlow(HomeState())
    
    // Public immutable state
    val state: StateFlow<HomeState> = _state.asStateFlow()
    
    init {
        // Collect repository flows in viewModelScope
        viewModelScope.launch {
            repository.dashboardState.collect { dashboard ->
                _state.update { it.copy(
                    isModuleEnabled = dashboard.isModuleEnabled,
                    activeProfile = dashboard.activeProfile,
                    // ... other fields
                ) }
            }
        }
    }
    
    // Actions triggered by UI events
    fun setModuleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setModuleEnabled(enabled)
        }
    }
    
    fun selectProfile(profileId: String) {
        viewModelScope.launch {
            repository.setActiveProfile(profileId)
        }
    }
}
```

### State Class Pattern

```kotlin
data class HomeState(
    val isLoading: Boolean = true,
    val isXposedActive: Boolean = false,
    val isModuleEnabled: Boolean = false,
    val profiles: List<SpoofProfile> = emptyList(),
    val activeProfile: SpoofProfile? = null,
    val enabledAppsCount: Int = 0,
    val maskedIdentifiersCount: Int = 0,
    val error: String? = null
)
```

### Screen Pattern

```kotlin
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSpoof: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    HomeScreenContent(
        isXposedActive = state.isXposedActive,
        isModuleEnabled = state.isModuleEnabled,
        profiles = state.profiles,
        onModuleEnabledChange = viewModel::setModuleEnabled,
        onProfileSelected = viewModel::selectProfile,
        // ... other state and callbacks
    )
}

// Stateless content composable (unchanged from current)
@Composable
fun HomeScreenContent(
    isXposedActive: Boolean,
    isModuleEnabled: Boolean,
    // ... same parameters as before
)
```

## Decisions

### D1: No Dependency Injection Framework

**Decision**: Use manual ViewModel instantiation via `viewModel { }` factory.

**Rationale**:
- App has only 5 screens with simple dependencies
- Avoids adding Hilt/Koin complexity
- SpoofRepository is already a singleton
- Can migrate to Hilt later if needed

**Implementation**:
```kotlin
// In MainActivity or NavHost
val homeViewModel = viewModel {
    HomeViewModel(SpoofRepository.getInstance(LocalContext.current.applicationContext))
}
```

### D2: Feature-Based Package Structure

**Decision**: Organize screens in feature packages instead of flat structure.

**Rationale**:
- Groups related files (Screen + ViewModel + State)
- Scales better as app grows
- Industry standard pattern
- Clear ownership per feature

### D3: Keep Existing Content Composables

**Decision**: Keep the stateless `*Content` composables unchanged.

**Rationale**:
- Already follows state hoisting pattern
- Enables Compose previews
- Minimizes refactoring risk
- Only change is how state reaches them

### D4: StateFlow over LiveData

**Decision**: Use Kotlin StateFlow instead of LiveData.

**Rationale**:
- Already using Flows in repository
- Better Kotlin coroutines integration
- More flexible operators
- `collectAsStateWithLifecycle()` handles lifecycle

## File Structure

```
ui/screens/
├── home/
│   ├── HomeScreen.kt              # @Composable fun HomeScreen(viewModel, ...)
│   ├── HomeViewModel.kt           # class HomeViewModel : ViewModel()
│   └── HomeState.kt               # data class HomeState(...)
│
├── profile/
│   ├── ProfileScreen.kt
│   ├── ProfileViewModel.kt
│   └── ProfileState.kt
│
├── profiledetail/
│   ├── ProfileDetailScreen.kt
│   ├── ProfileDetailViewModel.kt
│   └── ProfileDetailState.kt
│
├── settings/
│   ├── SettingsScreen.kt
│   ├── SettingsViewModel.kt
│   └── SettingsState.kt
│
└── diagnostics/
    ├── DiagnosticsScreen.kt
    ├── DiagnosticsViewModel.kt
    └── DiagnosticsState.kt
```

## Migration Strategy

### Per-Screen Migration Steps

1. Create feature package (`ui/screens/home/`)
2. Create `HomeState.kt` with all UI state fields
3. Create `HomeViewModel.kt` that:
   - Collects repository flows
   - Updates state
   - Exposes action methods
4. Move `HomeScreen.kt` to feature package
5. Update `HomeScreen` to:
   - Accept ViewModel instead of Repository
   - Collect state with `collectAsStateWithLifecycle()`
   - Pass ViewModel methods as callbacks
6. Update `MainActivity.kt` to instantiate ViewModel
7. Delete old file location
8. Test screen functionality

### Order of Migration

1. **HomeScreen** (first - use as template)
2. **SettingsScreen** (simplest state)
3. **ProfileScreen** (list with actions)
4. **ProfileDetailScreen** (complex state)
5. **DiagnosticsScreen** (read-only state)

## Risks and Mitigations

| Risk | Mitigation |
|------|------------|
| Breaking navigation | Test all nav paths after each screen |
| State desync | Compare behavior before/after |
| Memory leaks | Use viewModelScope properly |
| Recomposition issues | Compare UI performance |

## Testing Strategy

### Per-Screen Verification

1. Screen renders correctly
2. State updates reflect in UI
3. Actions update repository
4. Navigation works
5. Configuration change preserves state
6. No crashes or ANRs

### Full App Verification

1. Create new profile
2. Edit profile values
3. Enable/disable spoofing
4. Navigate between all screens
5. Settings persist across restart
6. Diagnostics shows correct values

## Open Questions

None - straightforward refactor with proven patterns.
