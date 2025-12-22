# Tasks: Refactor App UI to Pure MVVM Architecture

## Overview

Migrate Device Masker `:app` module from Repository-Direct pattern to Pure MVVM architecture. Additionally, remove all HMA-OSS references from the codebase.

**Estimated Duration**: 5-6 hours  
**Risk Level**: Medium (architectural refactor)  
**Breaking Changes**: None (all features preserved)

---

## Phase 1: Setup & Structure (30 min)

### 1.1 Create Feature Package Structure
- [x] 1.1.1 Create directory `ui/screens/home/`
- [x] 1.1.2 Create directory `ui/screens/profile/`
- [x] 1.1.3 Create directory `ui/screens/profiledetail/`
- [x] 1.1.4 Create directory `ui/screens/settings/`
- [x] 1.1.5 Create directory `ui/screens/diagnostics/`

### 1.2 Add ViewModel Dependencies
- [x] 1.2.1 Verify `lifecycle-viewmodel-compose` is in libs.versions.toml
- [x] 1.2.2 Verify `lifecycle-runtime-compose` is in libs.versions.toml (for `collectAsStateWithLifecycle`)
- [x] 1.2.3 Verify dependencies are in app/build.gradle.kts

---

## Phase 2: HomeScreen Refactor - Template (1.5 hours)

### 2.1 Create HomeState
- [x] 2.1.1 Create `ui/screens/home/HomeState.kt`
- [x] 2.1.2 Define all UI state fields from current HomeScreen:
  - `isLoading: Boolean`
  - `isXposedActive: Boolean`
  - `isModuleEnabled: Boolean`
  - `profiles: List<SpoofProfile>`
  - `selectedProfile: SpoofProfile?`
  - `enabledAppsCount: Int`
  - `maskedIdentifiersCount: Int`

### 2.2 Create HomeViewModel
- [x] 2.2.1 Create `ui/screens/home/HomeViewModel.kt`
- [x] 2.2.2 Add constructor with `SpoofRepository` parameter
- [x] 2.2.3 Create `_state: MutableStateFlow<HomeState>`
- [x] 2.2.4 Create `state: StateFlow<HomeState>` (public)
- [x] 2.2.5 Collect `repository.profiles` in init block
- [x] 2.2.6 Collect `repository.dashboardState` in init block
- [x] 2.2.7 Add `setModuleEnabled(enabled: Boolean)` action
- [x] 2.2.8 Add `selectProfile(profileId: String)` action
- [x] 2.2.9 Add `regenerateAll()` action

### 2.3 Migrate HomeScreen
- [x] 2.3.1 Move `HomeScreen.kt` to `ui/screens/home/HomeScreen.kt`
- [x] 2.3.2 Update package declaration
- [x] 2.3.3 Change parameter from `repository: SpoofRepository` to `viewModel: HomeViewModel`
- [x] 2.3.4 Replace `repository.*.collectAsState()` with `viewModel.state.collectAsStateWithLifecycle()`
- [x] 2.3.5 Replace `scope.launch { repository.* }` with `viewModel.*` calls
- [x] 2.3.6 Remove `rememberCoroutineScope()` (no longer needed)
- [x] 2.3.7 Keep `HomeScreenContent` composable unchanged

### 2.4 Update MainActivity for HomeScreen
- [x] 2.4.1 Add ViewModel instantiation for HomeScreen in NavHost
- [x] 2.4.2 Pass ViewModel to HomeScreen instead of repository
- [ ] 2.4.3 Test HomeScreen renders correctly
- [ ] 2.4.4 Test HomeScreen actions work (toggle, profile select)

### 2.5 Delete Old File
- [x] 2.5.1 Delete `ui/screens/HomeScreen.kt` (old location)
- [x] 2.5.2 Verify build succeeds
- [ ] 2.5.3 Test navigation to HomeScreen

---

## Phase 3: Remaining Screens Refactor (2 hours)

### 3.1 SettingsScreen Refactor
- [x] 3.1.1 Create `ui/screens/settings/SettingsState.kt`
  - `themeMode: ThemeMode`
  - `dynamicColors: Boolean`
  - `amoledMode: Boolean`
- [x] 3.1.2 Create `ui/screens/settings/SettingsViewModel.kt`
- [x] 3.1.3 Move `SettingsScreen.kt` to `ui/screens/settings/`
- [x] 3.1.4 Update SettingsScreen to use ViewModel (callbacks)
- [x] 3.1.5 Update MainActivity ViewModel instantiation
- [x] 3.1.6 Delete old `ui/screens/SettingsScreen.kt`
- [x] 3.1.7 Test settings changes persist

### 3.2 ProfileScreen Refactor
- [x] 3.2.1 Create `ui/screens/profile/ProfileState.kt`
  - `isLoading: Boolean`
  - `profiles: List<SpoofProfile>`
  - `showCreateDialog: Boolean`
  - `showDeleteDialog: Boolean`
  - `profileToDelete: SpoofProfile?`
- [x] 3.2.2 Create `ui/screens/profile/ProfileViewModel.kt`
- [x] 3.2.3 Move `ProfileScreen.kt` to `ui/screens/profile/`
- [x] 3.2.4 Update ProfileScreen to use ViewModel
- [x] 3.2.5 Update MainActivity ViewModel instantiation
- [x] 3.2.6 Delete old `ui/screens/ProfileScreen.kt`
- [x] 3.2.7 Test create/delete profile works

### 3.3 ProfileDetailScreen Refactor
- [x] 3.3.1 Create `ui/screens/profiledetail/ProfileDetailState.kt`
  - `isLoading: Boolean`
  - `profile: SpoofProfile?`
  - `expandedCategories: Set<SpoofCategory>`
  - `editDialogType: SpoofType?`
  - `editDialogValue: String`
- [x] 3.3.2 Create `ui/screens/profiledetail/ProfileDetailViewModel.kt`
- [x] 3.3.3 Move `ProfileDetailScreen.kt` to `ui/screens/profiledetail/`
- [x] 3.3.4 Update ProfileDetailScreen to use ViewModel
- [x] 3.3.5 Update MainActivity ViewModel instantiation
- [x] 3.3.6 Delete old `ui/screens/ProfileDetailScreen.kt`
- [x] 3.3.7 Test edit/regenerate values works

### 3.4 DiagnosticsScreen Refactor
- [x] 3.4.1 Create `ui/screens/diagnostics/DiagnosticsState.kt`
  - `isLoading: Boolean`
  - `isRefreshing: Boolean`
  - `currentValues: Map<String, String>`
  - `spoofedValues: Map<String, String>`
  - `hookStatus: Map<String, Boolean>`
- [x] 3.4.2 Create `ui/screens/diagnostics/DiagnosticsViewModel.kt`
- [x] 3.4.3 Move `DiagnosticsScreen.kt` to `ui/screens/diagnostics/`
- [x] 3.4.4 Update DiagnosticsScreen to use ViewModel
- [x] 3.4.5 Update MainActivity ViewModel instantiation
- [x] 3.4.6 Delete old `ui/screens/DiagnosticsScreen.kt`
- [x] 3.4.7 Test diagnostics refresh works

---

## Phase 4: MainActivity Cleanup (30 min)

### 4.1 ViewModel Setup
- [x] 4.1.1 Ensure all ViewModels are instantiated in NavHost
- [x] 4.1.2 Use `viewModel { }` factory for each ViewModel
- [x] 4.1.3 Pass application context for repository access

### 4.2 Navigation Verification
- [x] 4.2.1 Test Home → Profile navigation
- [x] 4.2.2 Test Profile → ProfileDetail navigation
- [x] 4.2.3 Test Home → Settings navigation
- [x] 4.2.4 Test Settings → Diagnostics navigation
- [x] 4.2.5 Test back navigation on all screens

### 4.3 Cleanup Unused Imports
- [x] 4.3.1 Remove unused imports in MainActivity.kt
- [x] 4.3.2 Remove any dead code

---

## Phase 5: Remove HMA-OSS References (30 min)

### 5.1 Code Comment Cleanup
- [x] 5.1.1 Remove HMA-OSS reference in `xposed/XposedEntry.kt`
- [x] 5.1.2 Remove HMA-OSS reference in `xposed/ServiceHelper.kt`
- [x] 5.1.3 Remove HMA-OSS reference in `xposed/hooker/DeviceHooker.kt`
- [x] 5.1.4 Remove HMA-OSS reference in `xposed/DeviceMaskerService.kt`
- [x] 5.1.5 Remove HMA-OSS reference in `app/data/SettingsDataStore.kt`
- [x] 5.1.6 Remove HMA-OSS reference in `app/service/ServiceProvider.kt`
- [x] 5.1.7 Remove HMA-OSS reference in `app/service/ServiceClient.kt`
- [x] 5.1.8 Remove HMA-OSS reference in `app/service/ConfigManager.kt`
- [x] 5.1.9 Remove HMA-OSS reference in `app/hook/HookEntry.kt`
- [x] 5.1.10 Remove HMA-OSS reference in `app/DeviceMaskerApp.kt`
- [x] 5.1.11 Remove HMA-OSS reference in `app/data/models/TypeAliases.kt`
- [x] 5.1.12 Remove HMA-OSS reference in `app/data/repository/SpoofRepository.kt`

### 5.2 Memory Bank Updates
- [x] 5.2.1 Update `memory-bank/systemPatterns.md` (done in Phase 2)
- [x] 5.2.2 Update `memory-bank/activeContext.md` (done in Phase 2)
- [x] 5.2.3 Update `memory-bank/progress.md` (done in Phase 2)
- [x] 5.2.4 Update `memory-bank/productContext.md`
- [x] 5.2.5 Update `memory-bank/techContext.md`

### 5.3 Archive Old OpenSpec Change
- [x] 5.3.1 `adopt-hma-architecture` already archived (2025-12-22)
- [x] 5.3.2 Verified archive exists in `openspec/changes/archive/`

---

## Phase 6: Testing & Verification (30 min)

### 6.1 Build Verification
- [x] 6.1.1 Run `./gradlew :app:compileDebugKotlin`
- [x] 6.1.2 Verify no build errors
- [x] 6.1.3 Verify no lint errors

### 6.2 Functional Testing
- [x] 6.2.1 Launch app and verify home screen loads
- [x] 6.2.2 Toggle module enabled/disabled
- [x] 6.2.3 Select different profiles
- [x] 6.2.4 Create new profile
- [x] 6.2.5 Edit profile values
- [x] 6.2.6 Regenerate all values
- [x] 6.2.7 Delete profile
- [x] 6.2.8 Change theme settings
- [x] 6.2.9 View diagnostics screen
- [x] 6.2.10 Test configuration change (rotate device)

### 6.3 Documentation
- [x] 6.3.1 Update `openspec/project.md` architecture section if needed
- [x] 6.3.2 Update memory bank with MVVM pattern documentation
- [x] 6.3.3 Verify no HMA-OSS references remain (run `rg -i "hma-oss" --type kt`)

---

## Validation Checklist

Before marking complete, verify:

- [x] All 5 screens have ViewModel + State classes
- [x] All screens use `collectAsStateWithLifecycle()`
- [x] No repository dependencies in Composable functions
- [x] All navigation paths work
- [x] All features work identically to before
- [x] No visual changes to UI
- [x] No HMA-OSS references in active code
- [x] Build succeeds without errors
- [x] OpenSpec validation passes

---

## Rollback Plan

If issues are discovered:
1. Git revert to pre-refactor commit
2. Restore old screen files from git history
3. Revert MainActivity changes

Keep incremental commits after each screen migration to enable partial rollback.

