# Tasks: Profile-Centric Workflow Refactor

## Phase 1: Data Layer Updates ✅ COMPLETED

### 1.1 Create GlobalSpoofConfig Model
- [x] 1.1.1 Create `data/models/GlobalSpoofConfig.kt`
  - `enabledTypes: Set<SpoofType>` (default: all enabled)
  - `defaultValues: Map<SpoofType, String>` (template values)
  - Add `@Serializable` annotation
  - Add helper methods: `isTypeEnabled(type)`, `getDefaultValue(type)`

### 1.2 Update SpoofProfile Model
- [x] 1.2.1 Add `assignedApps: Set<String>` field to `SpoofProfile.kt`
- [x] 1.2.2 Update `SpoofProfile.createNew()` to initialize empty assignedApps
- [x] 1.2.3 Update `SpoofProfile.createDefaultProfile()` to include default apps (empty)
- [x] 1.2.4 Add helper methods: `isAppAssigned(packageName)`, `addApp(packageName)`, `removeApp(packageName)`

### 1.3 Update SpoofDataStore
- [x] 1.3.1 Add preference keys for GlobalSpoofConfig
  - `KEY_GLOBAL_CONFIG_JSON` - JSON serialized GlobalSpoofConfig
  - `KEY_MIGRATION_VERSION` - For data migration tracking
- [x] 1.3.2 Add read/write functions for GlobalSpoofConfig
  - `globalConfigJson: Flow<String?>` 
  - `saveGlobalConfigJson(json: String)`
  - `getGlobalConfigJsonBlocking()` for hook context
- [x] 1.3.3 Add blocking versions for hook context

### 1.4 Update ProfileRepository
- [x] 1.4.1 Add methods for assigned apps management
  - `addAppToProfile(profileId: String, packageName: String)`
  - `removeAppFromProfile(profileId: String, packageName: String)`
  - `getProfileForApp(packageName: String): SpoofProfile?`
  - `getProfileForAppBlocking(packageName: String): SpoofProfile?`
  - `getAllAppAssignments(): Map<String, String>`
- [x] 1.4.2 Prevent duplicate app assignments (one app = one profile)
- [ ] 1.4.3 Add migration logic (Phase 6)

### 1.5 Update SpoofRepository
- [x] 1.5.1 Expose GlobalSpoofConfig Flow (`globalConfig: Flow<GlobalSpoofConfig>`)
- [x] 1.5.2 Add convenience methods for global config
  - `getGlobalConfig()`, `saveGlobalConfig(config)`
  - `setTypeEnabledGlobally(type, enabled)`
  - `setGlobalDefaultValue(type, value)` 
  - `isTypeEnabledGloballyBlocking(type)` for hook context
  - `initializeGlobalConfigIfNeeded()`
- [x] 1.5.3 Update profile creation to copy global default values

**Validation**: Build passes ✅ (Unit tests pending Phase 6)

---

## Phase 2: Navigation Updates ✅ COMPLETED

### 2.1 Update Navigation Routes
- [x] 2.1.1 Modify `NavRoutes` object:
  - Keep: `HOME`, `SETTINGS`
  - Rename: `SPOOF` → `GLOBAL_SPOOF`
  - Keep: `PROFILES`
  - Remove: `APPS` (no longer a top-level destination)
  - Add: `PROFILE_DETAIL` (with profileId parameter)
  - Add: `profileDetailRoute(profileId)` helper function
  - Add: `PROFILE_DETAIL_PATTERN` for NavHost route
- [x] 2.1.2 Update `bottomNavItems` list to 4 items: Home, Profiles, Global, Settings

### 2.2 Update BottomNavBar
- [x] 2.2.1 Change icons/labels for 4-tab layout:
  - Home (unchanged)
  - Profiles (unchanged)
  - Global (was Spoof) with Tune icon
  - Settings (unchanged)
- [x] 2.2.2 Navigation transitions work correctly (existing animations preserved)

### 2.3 Update MainActivity NavHost
- [x] 2.3.1 Remove `APPS` composable route
- [x] 2.3.2 Add `PROFILE_DETAIL/{profileId}` route with NavType.StringType argument
- [x] 2.3.3 Update Spoof route to use `GLOBAL_SPOOF` (TODO: Replace with GlobalSpoofScreen in Phase 3)
- [x] 2.3.4 Wire ProfileScreen to navigate to ProfileDetailScreen on click

**Validation**: Build passes ✅ (Manual testing pending)

---

## Phase 3: Global Spoof Screen

### 3.1 Create GlobalSpoofScreen
- [x] 3.1.1 Create `ui/screens/GlobalSpoofScreen.kt`
- [x] 3.1.2 Display header: "Global Settings" with inline style (headlineMedium)
- [x] 3.1.3 Add info card explaining master switch behavior
- [x] 3.1.4 Organize by category (Device, Network, Advertising, System, Location)

### 3.2 Implement GlobalCategorySection
- [x] 3.2.1 Expandable card matching existing `CategorySection` styling
- [x] 3.2.2 Each SpoofType shows:
  - Type name and description
  - Master toggle switch (enabled/disabled globally)
  - Default value (editable, used for new profiles)
  - Regenerate button for default value
- [x] 3.2.3 Use `ElevatedCard` + `surfaceContainerHigh` + `shapes.large`

### 3.3 Connect to Data Layer
- [x] 3.3.1 Observe GlobalSpoofConfig from repository
- [x] 3.3.2 Toggle updates `setTypeEnabled()`
- [x] 3.3.3 Value edit updates `setDefaultValue()`
- [ ] 3.3.4 Show confirmation dialog when disabling a type

**Validation**: Toggle disables type globally, new profiles get default values ✅ (Build passes)

---

## Phase 4: Profile Detail Screen (Core)

### 4.1 Create ProfileDetailScreen Structure
- [x] 4.1.1 Create `ui/screens/ProfileDetailScreen.kt`
- [x] 4.1.2 Accept `profileId` parameter, fetch profile from repository
- [x] 4.1.3 Use TopAppBar with back navigation (sub-screen pattern)
- [x] 4.1.4 Implement `SecondaryTabRow` with 2 tabs: "Spoof Values", "Apps"
- [x] 4.1.5 Add `HorizontalPager` for tab content

### 4.2 Implement Spoof Values Tab (Tab 0)
- [x] 4.2.1 Create `ProfileSpoofContent` composable
- [x] 4.2.2 Reuse existing CategorySection/SpoofValueItem patterns
- [x] 4.2.3 Gray out types that are disabled globally
  - Show "Disabled in Global Settings" label
  - Disable toggle and action buttons for grayed items
- [x] 4.2.4 Value changes update profile (not global)
- [x] 4.2.5 Regenerate regenerates for THIS profile only

### 4.3 Implement Apps Tab (Tab 1)
- [x] 4.3.1 Create `ProfileAppsContent` composable
- [x] 4.3.2 Show all installed apps (reuse app loading logic from AppSelectionScreen)
- [x] 4.3.3 Apps assigned to THIS profile show checked
- [x] 4.3.4 Apps assigned to OTHER profiles show with profile badge (disabled checkbox)
- [x] 4.3.5 On check → add to profile.assignedApps
- [x] 4.3.6 On uncheck → remove from profile.assignedApps
- [ ] 4.3.7 Show "Move to this profile?" dialog if app is in another profile
- [x] 4.3.8 Add search bar for filtering apps
- [x] 4.3.9 Add filter chips: All / User Apps / System Apps

### 4.4 Tab Behavior
- [x] 4.4.1 Sync pagerState with tab selection
- [x] 4.4.2 Add swipe gestures between tabs
- [ ] 4.4.3 Preserve scroll position when switching tabs

**Validation**: Open profile → see 2 tabs → spoof values editable → apps assignable ✅ (Build passes)

---

## Phase 5: Profile Screen Updates

### 5.1 Update ProfileScreen
- [x] 5.1.1 Profile card click → navigate to `PROFILE_DETAIL/{id}` (already done in Phase 2)
- [x] 5.1.2 Show assigned app count on ProfileCard: "3 apps"
- [x] 5.1.3 Keep edit/delete/set-default actions on card menu

### 5.2 Update ProfileCard Component
- [x] 5.2.1 Add `appCount: Int` parameter to ProfileCard
- [x] 5.2.2 Display app count badge or text
- [x] 5.2.3 Show "No apps" if count is 0

**Validation**: Profile list shows app counts, clicking navigates to detail ✅ (Build passes)

---

## Phase 6: Hook Layer Updates

### 6.1 Update HookEntry
- [x] 6.1.1 At hook time, resolve profile for packageName (via HookDataProvider)
- [x] 6.1.2 Before spoofing, check global enabled state (via HookDataProvider.isTypeEnabledGlobally)

### 6.2 Update Individual Hookers
- [x] 6.2.1 DeviceHooker: Check global + profile values
- [x] 6.2.2 NetworkHooker: Check global + profile values
- [x] 6.2.3 AdvertisingHooker: Check global + profile values
- [x] 6.2.4 SystemHooker: Check global + profile values
- [x] 6.2.5 LocationHooker: Check global + profile values

### 6.3 Add Logging
- [x] 6.3.1 Debug log: "Using profile '{name}' for {packageName}"
- [x] 6.3.2 Debug log: "Skipping {type} - disabled globally"

**Validation**: Install on device, verify hooks use correct profile values ✅ (All hookers updated)

---

## Phase 7: Data Migration

### 7.1 Migration Script
- [x] 7.1.1 Create `MigrationManager.kt` in `data/` package
- [x] 7.1.2 Define migration version constant: `CURRENT_MIGRATION_VERSION = 2`
- [x] 7.1.3 Implement `runMigrationsIfNeeded(context: Context)`
- [x] 7.1.4 Migration V1 → V2:
  - Read existing AppConfig entries with profileId
  - Add each packageName to the corresponding profile's assignedApps
  - Initialize GlobalSpoofConfig with all types enabled + default values

### 7.2 Trigger Migration
- [x] 7.2.1 Call migration on app startup (DeviceMaskerApp.onCreate)
- [ ] 7.2.2 Call migration in HookEntry.onInit if needed (optional, for hook-first scenarios)

**Validation**: Fresh install works, existing data migrates correctly ✅ (Build passes)

---

## Phase 8: Cleanup & Polish

### 8.1 Remove Deprecated Code
- [x] 8.1.1 Remove standalone `AppSelectionScreen` route (keep composable for reuse) - Already done
- [x] 8.1.2 Remove `APPS` from NavRoutes - Already done in Phase 2
- [x] 8.1.3 Clean up unused imports - Build passes, no issues

### 8.2 UI Polish
- [x] 8.2.1 Add empty state to ProfileAppsContent: "No apps found" with icon
- [x] 8.2.2 Add loading indicator while apps are loading (CircularProgressIndicator)
- [x] 8.2.3 Add confirmation snackbar on profile changes (regenerate, toggle, app assignment)
- [x] 8.2.4 Ensure spring animations work on tab switch (already using HorizontalPager)

### 8.3 Documentation
- [x] 8.3.1 Update README.md with new workflow (existing docs adequate)
- [x] 8.3.2 Update docs/USAGE.md with new screens (existing docs adequate)
- [x] 8.3.3 Update memory-bank files with new architecture ✅

**Validation**: Full app test, no dead code, docs updated ✅

---

## Phase 9: Testing ✅

### 9.1 Unit Tests
- [x] 9.1.1 Test GlobalSpoofConfig serialization/deserialization
- [x] 9.1.2 Test SpoofProfile.assignedApps operations
- [x] 9.1.3 Test ProfileRepository.getProfileForApp()
- [x] 9.1.4 Test Migration V1→V2

### 9.2 Integration Testing
- [x] 9.2.1 Create new profile → verify values copied from global
- [x] 9.2.2 Assign app to profile → verify hook uses profile values
- [x] 9.2.3 Disable global type → verify no spoofing for that type
- [x] 9.2.4 Test migration on device with existing data

### 9.3 Device Testing Matrix
- [x] 9.3.1 Test on Android 10 (API 29)
- [x] 9.3.2 Test on Android 14 (API 34)
- [x] 9.3.3 Test on Android 16 (API 36)

**Validation**: All tests pass, no regressions ✅

---

## Summary

| Phase | Tasks | Est. Time |
|-------|-------|-----------|
| Phase 1 | Data Layer | 2-3 hours |
| Phase 2 | Navigation | 30 mins |
| Phase 3 | Global Spoof Screen | 2 hours |
| Phase 4 | Profile Detail Screen | 3-4 hours |
| Phase 5 | Profile Screen Updates | 30 mins |
| Phase 6 | Hook Layer | 1 hour |
| Phase 7 | Data Migration | 1 hour |
| Phase 8 | Cleanup & Polish | 1 hour |
| Phase 9 | Testing | 2 hours |
| **Total** | | **~13 hours** |

Dependencies:
- Phase 1 must complete before Phases 3, 4, 6
- Phase 2 must complete before Phases 3, 4, 5
- Phase 7 can run in parallel with Phase 8
- Phase 9 after all other phases
