# Active Context: Device Masker

## Current Work Focus

### ✅ Complete: Refactor Profile to Group (Dec 23, 2025)

**Status**: Complete
**Objective**: Rename all instances of "Profile" to "Group" to avoid terminology conflict with Android Work Profiles.

#### Key Changes
- Renamed `SpoofProfile` to `SpoofGroup` in `:common`
- Renamed package `ui.screens.profile` to `ui.screens.groups`
- Renamed package `ui.screens.profiledetail` to `ui.screens.groupspoofing`
- All classes, ViewModels, and UI components updated with "Group" terminology
- Navigation routes and string resources updated to use "group"
- Updated `JsonConfig` to use `@SerialName("profiles")` for backward compatibility with existing JSON storage
- Successfully resolved all compilation errors and verified full app functionality.

---

### ✅ Complete: MVVM Architecture Refactor (Dec 22, 2025)

**Status**: All phases complete - Tested and Archived!
**OpenSpec Change**: `refactor-mvvm-architecture` (archived as `2025-12-22-refactor-mvvm-architecture`)

#### Objective
Migrated Device Masker `:app` module from Repository-Direct pattern to Pure MVVM architecture.

#### Progress

| Phase | Status | Description |
|-------|--------|-------------|
| Phase 1 | ✅ Complete | Created feature package structure |
| Phase 2 | ✅ Complete | HomeScreen refactored to MVVM |
| Phase 3 | ✅ Complete | ViewModels/States for all screens |
| Phase 4 | ✅ Complete | All 5 screens migrated to MVVM |
| Phase 5 | ✅ Complete | HMA-OSS references removed |
| Phase 6 | ✅ Complete | Manual testing on device |

#### All Screens Migrated to MVVM

**Home Screen (`ui/screens/home/`):**
- `HomeState.kt` - UI state
- `HomeViewModel.kt` - Module status, groups, active group
- `HomeScreen.kt` - Uses ViewModel

**Settings Screen (`ui/screens/settings/`):**
- `SettingsState.kt` - UI state
- `SettingsViewModel.kt` - Settings management via SettingsDataStore
- `SettingsScreen.kt` - Uses ViewModel

**Group Screen (`ui/screens/groups/`):**
- `GroupsState.kt` - UI state
- `GroupsViewModel.kt` - CRUD operations
- `GroupsScreen.kt` - Uses ViewModel

**Group Spoofing Screen (`ui/screens/groupspoofing/`):**
- `GroupSpoofingState.kt` - UI state
- `GroupSpoofingViewModel.kt` - Spoof value operations
- `GroupSpoofingScreen.kt` - Uses ViewModel

**Diagnostics Screen (`ui/screens/diagnostics/`):**
- `DiagnosticsState.kt` - UI state + DiagnosticResult models
- `DiagnosticsViewModel.kt` - Diagnostic tests
- `DiagnosticsScreen.kt` - Uses ViewModel

#### Phase 5 Completed Work

Removed 12 HMA-OSS references from active code:
- `xposed/` module: 4 files
- `app/` module: 8 files
- All now reference "Multi-Module AIDL architecture"

---

## Recent Changes (Dec 22, 2025)

### 🏗️ MVVM Migration Complete

**All 5 Screens Now Use ViewModels:**
- HomeScreen → HomeViewModel ✅
- SettingsScreen → SettingsViewModel ✅
- GroupsScreen → GroupsViewModel ✅
- GroupSpoofingScreen → GroupSpoofingViewModel ✅
- DiagnosticsScreen → DiagnosticsViewModel ✅

**Key Architectural Changes:**
- All screens now use `collectAsStateWithLifecycle()` for state collection
- No repository dependencies in Composable functions
- ViewModels handle all async operations via `viewModelScope`
- Manual ViewModel instantiation using `viewModel { }` factory

**MainActivity Updated:**
- All screens receive ViewModels instead of Repository
- Clean separation between UI and business logic

### 🧹 HMA-OSS References Cleaned (Phase 5)

Changed "HMA-OSS" to "Multi-Module AIDL" in 12 files.

---

## ✅ Complete: Expressive UI Overhaul (Dec 22, 2025)

- Global Expressive Cards migration
- Bouncy touch feedback throughout app
- ExpressiveOutlinedCard component added

## ✅ Complete: Value Generation Improvements (Dec 21, 2025)

- 16 countries, 75+ carriers
- GPS correlation with carrier country
- Dual-SIM backend support
- US carrier expansion (45+ carriers)

---

## Build Status

| Module | Status | Last Build |
|--------|--------|------------|
| :common | ✅ SUCCESS | Dec 23, 2025 |
| :xposed | ✅ SUCCESS | Dec 23, 2025 |
| :app | ✅ SUCCESS | Dec 23, 2025 |
| Full APK | ✅ SUCCESS | Dec 23, 2025 |

---

## Next Steps

### No Active Development Work

All major refactoring and improvements are complete! The project is in a stable state.

### Future Enhancements (Optional)

- Add Dual-SIM UI section
- Dynamic fingerprint generation
- Cell Info Xposed hooks
- Carrier picker in group creation
- More device presets

---

## Important Files Reference

| File | Purpose |
|------|---------|
| `ui/screens/home/HomeViewModel.kt` | Home screen state management |
| `ui/screens/settings/SettingsViewModel.kt` | Settings screen state management |
| `ui/screens/groups/GroupsViewModel.kt` | Groups screen state management |
| `ui/screens/groupspoofing/GroupSpoofingViewModel.kt` | Group spoofing state management |
| `ui/screens/diagnostics/DiagnosticsViewModel.kt` | Diagnostics screen state management |
| `ui/MainActivity.kt` | Navigation + ViewModel instantiation |
| `openspec/changes/archive/2025-12-22-refactor-mvvm-architecture/` | Archived MVVM refactor documentation |


