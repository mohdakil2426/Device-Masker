# Technical Design: Profile → Group Rename

## Context

This is a codebase-wide terminology change from "Profile" to "Group" affecting:
- 50+ files across 3 modules
- Core data model used in Xposed hooks
- JSON persistence configuration
- All UI screens and components

## Goals / Non-Goals

### Goals
- Rename all "Profile" references to "Group" for clarity
- Maintain backward compatibility with existing saved configurations
- Preserve all existing functionality without behavioral changes
- Update all documentation consistently

### Non-Goals
- Change any spoofing logic
- Modify data structures (only renaming)
- Add new features

## Decisions

### Decision 1: Rename Strategy
**Choice**: Batch rename with IDE refactoring + manual verification

**Rationale**:
- IDE refactoring (Rename Symbol) handles most cases automatically
- Manual review needed for strings, comments, and documentation
- Tests validate no regressions

**Alternatives Considered**:
1. `sed` replacement - Too risky, may miss context
2. Incremental rename - Takes too long, partial state is confusing

### Decision 2: JSON Backward Compatibility
**Choice**: Use `@SerialName("profiles")` annotation for `groups` field

```kotlin
@Serializable
data class JsonConfig(
    @SerialName("profiles")  // Backward compat
    val groups: List<SpoofGroup> = emptyList(),
    // ...
)
```

**Rationale**:
- Zero migration code needed
- Existing JSON files continue to work
- New files use same key (transparent to user)

**Alternative**: Write migration - More complex, higher risk

### Decision 3: Package Rename Order
**Choice**: Bottom-up (common → app → xposed)

**Rationale**:
- `:common` has no dependencies, safe to rename first
- `:app` depends on common, rename after
- `:xposed` depends on common, rename last

## Affected Files Analysis

### Module: `:common` (5 files)

| File | Change |
|------|--------|
| `SpoofProfile.kt` | Rename to `SpoofGroup.kt`, class `SpoofProfile` → `SpoofGroup` |
| `JsonConfig.kt` | `profiles: List<SpoofProfile>` → `groups: List<SpoofGroup>` |
| `Constants.kt` | `PROFILES_KEY` → `GROUPS_KEY` (if exists) |
| `AppConfig.kt` | Update profile references |
| `DeviceProfilePreset.kt` | **KEEP UNCHANGED** (different domain - device hardware) |

### Module: `:app` (~35 files)

#### Navigation (2 files)
| File | Change |
|------|--------|
| `NavDestination.kt` | `PROFILES` → `GROUPS`, `PROFILE_DETAIL` → `GROUP_SPOOFING` |
| `MainActivity.kt` | Update navigation routes |

#### Screens - profile/ (3 files) → groups/
| Current | New |
|---------|-----|
| `profile/ProfileScreen.kt` | `groups/GroupsScreen.kt` |
| `profile/ProfileViewModel.kt` | `groups/GroupsViewModel.kt` |
| `profile/ProfileState.kt` | `groups/GroupsState.kt` |

#### Screens - profiledetail/ (14 files) → groupspoofing/
| Current | New |
|---------|-----|
| `profiledetail/ProfileDetailScreen.kt` | `groupspoofing/GroupSpoofingScreen.kt` |
| `profiledetail/ProfileDetailViewModel.kt` | `groupspoofing/GroupSpoofingViewModel.kt` |
| `profiledetail/ProfileDetailState.kt` | `groupspoofing/GroupSpoofingState.kt` |
| (All subdirectories move with renamed package) | |

#### Components (2 files)
| Current | New |
|---------|-----|
| `ProfileCard.kt` | `GroupCard.kt` |
| References in other components | Update imports |

#### Data Layer (2 files)
| File | Change |
|------|--------|
| `SpoofRepository.kt` | `getProfiles()` → `getGroups()`, variable names |
| `ConfigManager.kt` | Update profile references |

#### Home Screen (3 files)
| File | Change |
|------|--------|
| `HomeScreen.kt` | Update imports and variable names |
| `HomeViewModel.kt` | Update profile → group references |
| `HomeState.kt` | `profiles: List<SpoofProfile>` → `groups: List<SpoofGroup>` |

### Module: `:xposed` (6 files)

| File | Change |
|------|--------|
| `DeviceMaskerService.kt` | `profile` → `group` in config reads |
| `hooker/DeviceHooker.kt` | Update profile references |
| `hooker/NetworkHooker.kt` | Update profile references |
| `hooker/AdvertisingHooker.kt` | Update profile references |
| `hooker/SystemHooker.kt` | Update profile references |
| `hooker/LocationHooker.kt` | Update profile references |

### String Resources (`strings.xml`)

**Rename Keys**:
```
profile_screen_title → group_screen_title
profile_list_empty → group_list_empty
profile_new → group_new
profile_create_new → group_create_new
profile_edit_dialog_title → group_edit_dialog_title
profile_name_hint → group_name_hint
profile_name_exists → group_name_exists
profile_description_hint → group_description_hint
profile_delete_dialog_title → group_delete_dialog_title
profile_delete_confirm → group_delete_confirm
profile_delete_warning → group_delete_warning
profile_detail_* → group_spoofing_*
profile_card_* → group_card_*
home_active_profile_label → home_active_group_label
home_no_profile → home_no_group
```

**Update User-Facing Text**:
- "Profile" → "Group"
- "Profiles" → "Groups"
- "Create New Profile" → "Create New Group"
- "Delete Profile" → "Delete Group"
- etc.

### Memory Bank (6 files)

All files need updates:
- `activeContext.md`
- `productContext.md`
- `progress.md`
- `projectbrief.md`
- `systemPatterns.md`
- `techContext.md`

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Breaking existing saved configs | `@SerialName` annotation for backward compat |
| Missing a reference | Comprehensive grep + build verification |
| Xposed hooks fail after rename | Test on device before release |
| Large PR hard to review | Split into logical commits per module |

## Migration Plan

### Execution Order
1. `:common` module (data model first)
2. `:app` module (depends on common)
3. `:xposed` module (depends on common)
4. String resources
5. Memory bank documentation
6. Build verification
7. Device testing

### Rollback
- Git reset if issues found before merge
- After merge: No automatic rollback (breaking change)

## Open Questions

1. ~~Should DeviceProfilePreset also be renamed?~~
   **Resolved**: No - it refers to hardware device profiles (Samsung, Pixel), not spoof groups

2. ~~Should we use "Group" or "SpoofGroup" in UI?~~
   **Resolved**: "Group" in UI (user-facing), "SpoofGroup" in code (for clarity)
