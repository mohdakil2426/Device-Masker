# Tasks: Profile-Centric Workflow Refactor

## Phase 1: Data Layer Updates

### 1.1 Create GlobalSpoofConfig Model
- [ ] 1.1.1 Create `data/models/GlobalSpoofConfig.kt`
  - `enabledTypes: Set<SpoofType>` (default: all enabled)
  - `defaultValues: Map<SpoofType, String>` (template values)
  - Add `@Serializable` annotation
  - Add helper methods: `isTypeEnabled(type)`, `getDefaultValue(type)`

### 1.2 Update SpoofProfile Model
- [ ] 1.2.1 Add `assignedApps: Set<String>` field to `SpoofProfile.kt`
- [ ] 1.2.2 Update `SpoofProfile.createNew()` to initialize empty assignedApps
- [ ] 1.2.3 Update `SpoofProfile.createDefaultProfile()` to include default apps (empty)
- [ ] 1.2.4 Add helper methods: `isAppAssigned(packageName)`, `addApp(packageName)`, `removeApp(packageName)`

### 1.3 Update SpoofDataStore
- [ ] 1.3.1 Add preference keys for GlobalSpoofConfig
  - `GLOBAL_ENABLED_TYPES` - Set<String> of enabled SpoofType names
  - `GLOBAL_DEFAULT_VALUES` - JSON string of default values map
- [ ] 1.3.2 Add read/write functions for GlobalSpoofConfig
  - `getGlobalConfig(): Flow<GlobalSpoofConfig>`
  - `setGlobalConfig(config: GlobalSpoofConfig)`
  - `setTypeEnabled(type: SpoofType, enabled: Boolean)`
  - `setDefaultValue(type: SpoofType, value: String)`
- [ ] 1.3.3 Add blocking versions for hook context

### 1.4 Update ProfileRepository
- [ ] 1.4.1 Add methods for assigned apps management
  - `addAppToProfile(profileId: String, packageName: String)`
  - `removeAppFromProfile(profileId: String, packageName: String)`
  - `getProfileForApp(packageName: String): SpoofProfile?`
- [ ] 1.4.2 Prevent duplicate app assignments (one app = one profile)
- [ ] 1.4.3 Add migration logic (Phase 6)

### 1.5 Update SpoofRepository
- [ ] 1.5.1 Expose GlobalSpoofConfig Flow
- [ ] 1.5.2 Add convenience methods for global config
- [ ] 1.5.3 Update profile creation to copy global default values

**Validation**: Unit tests for data models and repository methods

---

## Phase 2: Navigation Updates

### 2.1 Update Navigation Routes
- [ ] 2.1.1 Modify `NavRoutes` object:
  - Keep: `HOME`, `SETTINGS`
  - Rename: `SPOOF` → `GLOBAL_SPOOF`
  - Keep: `PROFILES`
  - Remove: `APPS` (no longer a top-level destination)
  - Add: `PROFILE_DETAIL` (with profileId parameter)
- [ ] 2.1.2 Update `bottomNavItems` list to 4 items: Home, Profiles, Spoof, Settings

### 2.2 Update BottomNavBar
- [ ] 2.2.1 Change icons/labels for 4-tab layout:
  - Home (unchanged)
  - Profiles (unchanged)
  - Spoof → "Global" with Settings icon
  - Settings (unchanged)
- [ ] 2.2.2 Verify navigation transitions work correctly

### 2.3 Update MainActivity NavHost
- [ ] 2.3.1 Remove `APPS` composable route
- [ ] 2.3.2 Add `PROFILE_DETAIL/{profileId}` route
- [ ] 2.3.3 Update Spoof route to use GlobalSpoofScreen
- [ ] 2.3.4 Wire ProfileScreen to navigate to ProfileDetailScreen on click

**Validation**: Build + manual test - 4 tabs visible, navigation works

---

## Phase 3: Global Spoof Screen

### 3.1 Create GlobalSpoofScreen
- [ ] 3.1.1 Create `ui/screens/GlobalSpoofScreen.kt`
- [ ] 3.1.2 Display header: "Global Settings" with inline style (headlineMedium)
- [ ] 3.1.3 Add info card explaining master switch behavior
- [ ] 3.1.4 Organize by category (Device, Network, Advertising, System, Location)

### 3.2 Implement GlobalCategorySection
- [ ] 3.2.1 Expandable card matching existing `CategorySection` styling
- [ ] 3.2.2 Each SpoofType shows:
  - Type name and description
  - Master toggle switch (enabled/disabled globally)
  - Default value (editable, used for new profiles)
  - Regenerate button for default value
- [ ] 3.2.3 Use `ElevatedCard` + `surfaceContainerHigh` + `shapes.large`

### 3.3 Connect to Data Layer
- [ ] 3.3.1 Observe GlobalSpoofConfig from repository
- [ ] 3.3.2 Toggle updates `setTypeEnabled()`
- [ ] 3.3.3 Value edit updates `setDefaultValue()`
- [ ] 3.3.4 Show confirmation dialog when disabling a type

**Validation**: Toggle disables type globally, new profiles get default values

---

## Phase 4: Profile Detail Screen (Core)

### 4.1 Create ProfileDetailScreen Structure
- [ ] 4.1.1 Create `ui/screens/ProfileDetailScreen.kt`
- [ ] 4.1.2 Accept `profileId` parameter, fetch profile from repository
- [ ] 4.1.3 Use TopAppBar with back navigation (sub-screen pattern)
- [ ] 4.1.4 Implement `SecondaryTabRow` with 2 tabs: "Spoof Values", "Apps"
- [ ] 4.1.5 Add `HorizontalPager` for tab content

### 4.2 Implement Spoof Values Tab (Tab 0)
- [ ] 4.2.1 Create `ProfileSpoofContent` composable
- [ ] 4.2.2 Reuse existing CategorySection/SpoofValueItem patterns
- [ ] 4.2.3 Gray out types that are disabled globally
  - Show "Disabled in Global Settings" label
  - Disable toggle and action buttons for grayed items
- [ ] 4.2.4 Value changes update profile (not global)
- [ ] 4.2.5 Regenerate regenerates for THIS profile only

### 4.3 Implement Apps Tab (Tab 1)
- [ ] 4.3.1 Create `ProfileAppsContent` composable
- [ ] 4.3.2 Show all installed apps (reuse app loading logic from AppSelectionScreen)
- [ ] 4.3.3 Apps assigned to THIS profile show checked
- [ ] 4.3.4 Apps assigned to OTHER profiles show with profile badge (disabled checkbox)
- [ ] 4.3.5 On check → add to profile.assignedApps
- [ ] 4.3.6 On uncheck → remove from profile.assignedApps
- [ ] 4.3.7 Show "Move to this profile?" dialog if app is in another profile
- [ ] 4.3.8 Add search bar for filtering apps
- [ ] 4.3.9 Add filter chips: All / User Apps / System Apps

### 4.4 Tab Behavior
- [ ] 4.4.1 Sync pagerState with tab selection
- [ ] 4.4.2 Add swipe gestures between tabs
- [ ] 4.4.3 Preserve scroll position when switching tabs

**Validation**: Open profile → see 2 tabs → spoof values editable → apps assignable

---

## Phase 5: Profile Screen Updates

### 5.1 Update ProfileScreen
- [ ] 5.1.1 Profile card click → navigate to `PROFILE_DETAIL/{id}`
- [ ] 5.1.2 Show assigned app count on ProfileCard: "3 apps"
- [ ] 5.1.3 Keep edit/delete/set-default actions on card menu

### 5.2 Update ProfileCard Component
- [ ] 5.2.1 Add `appCount: Int` parameter to ProfileCard
- [ ] 5.2.2 Display app count badge or text
- [ ] 5.2.3 Show "No apps" if count is 0

**Validation**: Profile list shows app counts, clicking navigates to detail

---

## Phase 6: Hook Layer Updates

### 6.1 Update HookEntry
- [ ] 6.1.1 At hook time, resolve profile for packageName:
  ```kotlin
  val profile = SpoofDataStore.getProfileForApp(packageName)
      ?: SpoofDataStore.getDefaultProfile()
  ```
- [ ] 6.1.2 Before spoofing, check global enabled state:
  ```kotlin
  if (!SpoofDataStore.isTypeEnabledGlobally(spoofType)) {
      return@hook // Skip, return real value
  }
  ```

### 6.2 Update Individual Hookers
- [ ] 6.2.1 DeviceHooker: Check global + profile values
- [ ] 6.2.2 NetworkHooker: Check global + profile values
- [ ] 6.2.3 AdvertisingHooker: Check global + profile values
- [ ] 6.2.4 SystemHooker: Check global + profile values
- [ ] 6.2.5 LocationHooker: Check global + profile values

### 6.3 Add Logging
- [ ] 6.3.1 Debug log: "Using profile '{name}' for {packageName}"
- [ ] 6.3.2 Debug log: "Skipping {type} - disabled globally"

**Validation**: Install on device, verify hooks use correct profile values

---

## Phase 7: Data Migration

### 7.1 Migration Script
- [ ] 7.1.1 Create `MigrationManager.kt` in `data/` package
- [ ] 7.1.2 Define migration version constant: `CURRENT_MIGRATION_VERSION = 2`
- [ ] 7.1.3 Implement `runMigrationsIfNeeded(context: Context)`
- [ ] 7.1.4 Migration V1 → V2:
  - Read existing AppConfig entries with profileId
  - Add each packageName to the corresponding profile's assignedApps
  - Initialize GlobalSpoofConfig with all types enabled + default values

### 7.2 Trigger Migration
- [ ] 7.2.1 Call migration on app startup (DeviceMaskerApp.onCreate)
- [ ] 7.2.2 Call migration in HookEntry.onInit if needed

**Validation**: Fresh install works, existing data migrates correctly

---

## Phase 8: Cleanup & Polish

### 8.1 Remove Deprecated Code
- [ ] 8.1.1 Remove standalone `AppSelectionScreen` route (keep composable for reuse)
- [ ] 8.1.2 Remove `APPS` from NavRoutes
- [ ] 8.1.3 Clean up unused imports

### 8.2 UI Polish
- [ ] 8.2.1 Add empty state to ProfileAppsContent: "No apps in search results"
- [ ] 8.2.2 Add loading indicator while apps are loading
- [ ] 8.2.3 Add confirmation snackbar on profile changes
- [ ] 8.2.4 Ensure spring animations work on tab switch

### 8.3 Documentation
- [ ] 8.3.1 Update README.md with new workflow
- [ ] 8.3.2 Update docs/USAGE.md with new screens
- [ ] 8.3.3 Update memory-bank files with new architecture

**Validation**: Full app test, no dead code, docs updated

---

## Phase 9: Testing

### 9.1 Unit Tests
- [ ] 9.1.1 Test GlobalSpoofConfig serialization/deserialization
- [ ] 9.1.2 Test SpoofProfile.assignedApps operations
- [ ] 9.1.3 Test ProfileRepository.getProfileForApp()
- [ ] 9.1.4 Test Migration V1→V2

### 9.2 Integration Testing
- [ ] 9.2.1 Create new profile → verify values copied from global
- [ ] 9.2.2 Assign app to profile → verify hook uses profile values
- [ ] 9.2.3 Disable global type → verify no spoofing for that type
- [ ] 9.2.4 Test migration on device with existing data

### 9.3 Device Testing Matrix
- [ ] 9.3.1 Test on Android 10 (API 29)
- [ ] 9.3.2 Test on Android 14 (API 34)
- [ ] 9.3.3 Test on Android 16 (API 36)

**Validation**: All tests pass, no regressions

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
