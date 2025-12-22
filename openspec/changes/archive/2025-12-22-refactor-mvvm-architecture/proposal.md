# Change: Refactor App UI to Pure MVVM Architecture

## Why

The current `:app` module uses a **Repository-Direct pattern** where Composable screens directly collect from `SpoofRepository` flows without ViewModels. While functional, this pattern:

1. **Lacks separation of concerns** - UI directly depends on data layer
2. **Makes testing difficult** - Cannot unit test screens without repository mocks
3. **Bypasses lifecycle awareness** - No ViewModel to survive configuration changes
4. **Violates Android best practices** - Does not follow recommended MVVM architecture

Additionally, the codebase contains numerous references to "HMA-OSS" from a previous architectural migration that should be cleaned up.

### Goals

1. **Refactor to Pure MVVM** - Introduce ViewModels for each screen
2. **Organize by feature** - Move screens into feature-based packages
3. **Zero feature breakage** - All existing functionality must work identically
4. **Remove HMA-OSS references** - Clean up comments and outdated documentation
5. **No UI changes** - Visual appearance and UX must remain identical

### Non-Goals

1. NOT adding Clean Architecture (no UseCases, no Domain layer)
2. NOT changing any UI components or visual design
3. NOT modifying the `:common` or `:xposed` modules (except comment cleanup)
4. NOT changing the data storage mechanism
5. NOT adding new features
6. NOT breaking existing functionality

## What Changes

### Structural Changes

```
BEFORE:                              AFTER:
ui/screens/                          ui/screens/
├── HomeScreen.kt                    ├── home/
├── ProfileScreen.kt                 │   ├── HomeScreen.kt
├── ProfileDetailScreen.kt           │   ├── HomeViewModel.kt (NEW)
├── SettingsScreen.kt                │   └── HomeState.kt (NEW)
└── DiagnosticsScreen.kt             ├── profile/
                                     │   ├── ProfileScreen.kt
                                     │   ├── ProfileViewModel.kt (NEW)
                                     │   └── ProfileState.kt (NEW)
                                     ├── profiledetail/
                                     │   ├── ProfileDetailScreen.kt
                                     │   ├── ProfileDetailViewModel.kt (NEW)
                                     │   └── ProfileDetailState.kt (NEW)
                                     ├── settings/
                                     │   ├── SettingsScreen.kt
                                     │   ├── SettingsViewModel.kt (NEW)
                                     │   └── SettingsState.kt (NEW)
                                     └── diagnostics/
                                         ├── DiagnosticsScreen.kt
                                         ├── DiagnosticsViewModel.kt (NEW)
                                         └── DiagnosticsState.kt (NEW)
```

### Pattern Changes

| Aspect | Before | After |
|--------|--------|-------|
| State Management | `repository.flow.collectAsState()` | `viewModel.state.collectAsStateWithLifecycle()` |
| Actions | `scope.launch { repository.action() }` | `viewModel.onAction()` |
| Lifecycle | rememberCoroutineScope (manual) | viewModelScope (automatic) |
| Configuration Changes | State lost on rotation | State preserved |

### Documentation Cleanup

- Remove all "HMA-OSS" references from code comments
- Update memory bank files to reflect MVVM architecture
- Archive the `adopt-hma-architecture` OpenSpec change

## Impact

### Affected Code

| Location | Files | Change Type |
|----------|-------|-------------|
| `ui/screens/` | 5 screens | Refactor + move to feature packages |
| `ui/screens/*/` | 10 new files | New ViewModels + States |
| `ui/MainActivity.kt` | 1 file | Update ViewModel instantiation |
| `:xposed` module | 4 files | Comment cleanup only |
| `memory-bank/` | 4 files | Documentation update |

### Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Feature regression | Low | High | Test each screen after refactor |
| Build failures | Low | Medium | Incremental commits per screen |
| Navigation breaks | Low | High | Test all navigation paths |
| State bugs | Low | Medium | Verify StateFlow behavior |

## Success Criteria

1. ✅ All 5 screens render correctly
2. ✅ All spoofing features work (create/edit/delete profiles)
3. ✅ Navigation between screens works identically
4. ✅ Settings persistence works
5. ✅ Build completes without errors
6. ✅ No "HMA-OSS" references in active code comments
7. ✅ No visual changes to UI
8. ✅ Configuration changes preserve state

## Files Summary

### New Files (15 total)

| File | Purpose |
|------|---------|
| `ui/screens/home/HomeViewModel.kt` | Home screen state management |
| `ui/screens/home/HomeState.kt` | Home screen UI state |
| `ui/screens/profile/ProfileViewModel.kt` | Profile list state management |
| `ui/screens/profile/ProfileState.kt` | Profile list UI state |
| `ui/screens/profiledetail/ProfileDetailViewModel.kt` | Profile detail state |
| `ui/screens/profiledetail/ProfileDetailState.kt` | Profile detail UI state |
| `ui/screens/settings/SettingsViewModel.kt` | Settings state management |
| `ui/screens/settings/SettingsState.kt` | Settings UI state |
| `ui/screens/diagnostics/DiagnosticsViewModel.kt` | Diagnostics state |
| `ui/screens/diagnostics/DiagnosticsState.kt` | Diagnostics UI state |

### Modified Files (6 total)

| File | Changes |
|------|---------|
| `ui/screens/home/HomeScreen.kt` | Remove repository, use ViewModel |
| `ui/screens/profile/ProfileScreen.kt` | Remove repository, use ViewModel |
| `ui/screens/profiledetail/ProfileDetailScreen.kt` | Remove repository, use ViewModel |
| `ui/screens/settings/SettingsScreen.kt` | Remove repository, use ViewModel |
| `ui/screens/diagnostics/DiagnosticsScreen.kt` | Remove repository, use ViewModel |
| `ui/MainActivity.kt` | ViewModel instantiation |

### Deleted Files (5 old locations)

| File | Reason |
|------|--------|
| `ui/screens/HomeScreen.kt` | Moved to `home/HomeScreen.kt` |
| `ui/screens/ProfileScreen.kt` | Moved to `profile/ProfileScreen.kt` |
| `ui/screens/ProfileDetailScreen.kt` | Moved to `profiledetail/ProfileDetailScreen.kt` |
| `ui/screens/SettingsScreen.kt` | Moved to `settings/SettingsScreen.kt` |
| `ui/screens/DiagnosticsScreen.kt` | Moved to `diagnostics/DiagnosticsScreen.kt` |

## Timeline Estimate

| Phase | Duration | Description |
|-------|----------|-------------|
| Phase 1 | 30 min | Create feature package structure |
| Phase 2 | 1.5 hours | Refactor HomeScreen (template) |
| Phase 3 | 2 hours | Refactor remaining 4 screens |
| Phase 4 | 30 min | Update MainActivity |
| Phase 5 | 30 min | Remove HMA-OSS references |
| Phase 6 | 30 min | Testing and verification |
| **Total** | **~5-6 hours** | |
