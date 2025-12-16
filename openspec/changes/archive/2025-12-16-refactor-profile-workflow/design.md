# Design: Profile-Centric Workflow Refactor

## Context

Device Masker currently uses a **flat workflow** where:
1. Users configure global spoof values
2. Users enable/disable apps separately
3. Users can assign profiles to apps (but this is disconnected)

This mental model has friction because users think in terms of "identities" (profiles) that group apps together, not individual app toggles.

**Stakeholders**: Privacy-conscious users, multi-account users, security researchers

**Constraints**:
- Must maintain backward compatibility with existing data
- UI must match Material 3 Expressive patterns already in use
- Hook layer performance must not degrade
- Android 8.0-16 (API 26-36) compatibility

## Goals / Non-Goals

### Goals
- ✅ Profile-centric mental model: "Profile = Identity + Apps"
- ✅ Global master switches that control all profiles
- ✅ Clear hierarchy: Global → Profile → Apps
- ✅ Simpler navigation (4 tabs instead of 5)
- ✅ Single location for profile configuration

### Non-Goals
- ❌ Multiple profiles per app (one app = one profile only)
- ❌ Profile scheduling (time-based profile switching)
- ❌ Profile import/export (future enhancement)
- ❌ Root hiding integration (out of scope for this module)

## Decisions

### D1: Profile stores assigned apps

**Decision**: Add `assignedApps: Set<String>` to `SpoofProfile` data class

**Rationale**: 
- Cleaner data model - each profile is self-contained
- Easy to query "which apps use this profile" without joins
- Simplifies hook lookup (scan profiles for package name match)

**Alternatives considered**:
- Keep in AppConfig with profileId reference → More complex queries, split data
- Separate ProfileAppAssignment table → Over-engineering for local storage

### D2: Global config as master switches

**Decision**: Create separate `GlobalSpoofConfig` with:
- `enabledTypes: Set<SpoofType>` - master switches
- `defaultValues: Map<SpoofType, String>` - template for new profiles

**Rationale**:
- Clean separation: Global = what CAN be spoofed, Profile = what values to use
- Easy "kill switch" functionality per category
- New profiles get sensible defaults

**Alternatives considered**:
- Global profile that others inherit → Confusing inheritance chain
- No global config, just per-profile → No central control

### D3: Profile creation copies global values

**Decision**: When creating a new profile:
1. Copy current `GlobalSpoofConfig.defaultValues` to profile
2. Profile values are then independent (not linked)
3. Profile respects `GlobalSpoofConfig.enabledTypes` (master switch) at runtime

**Rationale**:
- Simple mental model: "Create once, then independent"
- Avoids complex inheritance/override logic
- Master switch still controls what's active

### D4: App lookup via profile scan

**Decision**: Hook layer finds profile for app by:
```kotlin
fun getProfileForApp(packageName: String): SpoofProfile {
    return allProfiles.find { packageName in it.assignedApps }
        ?: defaultProfile
}
```

**Rationale**:
- Simple O(n) scan is fast enough (typically <10 profiles)
- Could add cache if profiling shows issues
- Avoids maintaining separate index

**Alternatives considered**:
- Package → ProfileId map → Extra data structure to maintain
- Store in AppConfig → Existing approach, being replaced

### D5: TabRow for Profile Detail

**Decision**: Use `SecondaryTabRow` + `HorizontalPager` for ProfileDetailScreen tabs

**Rationale**:
- Material 3 pattern for sub-navigation within a screen
- HorizontalPager enables swipe between tabs
- Maintains consistent styling with rest of app

```kotlin
@Composable
fun ProfileDetailScreen(...) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Column {
        SecondaryTabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { 
                Text("Spoof Values") 
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { 
                Text("Apps") 
            }
        }
        
        HorizontalPager(state = pagerState) { page ->
            when (page) {
                0 -> ProfileSpoofContent(profile)
                1 -> ProfileAppsContent(profile, allApps)
            }
        }
    }
}
```

## Risks / Trade-offs

| Risk | Impact | Mitigation |
|------|--------|------------|
| Data migration for existing users | Med | Migrate AppConfig.profileId → Profile.assignedApps on first launch |
| One app in multiple profiles | Low | UI prevents assignment if already assigned; show warning |
| Global toggle confusion | Med | Clear UI labels: "Master Switch" vs "Profile Value" |
| Performance of profile scan | Low | Cache profile-to-app map if >100 lookups/sec |
| Tab swipe conflicts with other gestures | Low | Standard HorizontalPager handles this well |

## Migration Plan

### Step 1: Data Migration
1. On app launch, check for `MIGRATION_VERSION` preference
2. If old version:
   - Read all `AppConfig` entries
   - For each with `profileId`, add `packageName` to that profile's `assignedApps`
   - Clear old `AppConfig.profileId` field (or remove)
   - Store new `GlobalSpoofConfig` with defaults
   - Update `MIGRATION_VERSION`

### Step 2: Rollback
- Keep old `AppConfig` data for 2 versions
- If critical issue, can restore by reversing migration
- Migration is idempotent (safe to run multiple times)

## UI Component Hierarchy

```
MainActivity
├── BottomNavBar (4 tabs: Home, Profiles, Spoof, Settings)
│
├── NavHost
│   ├── HomeScreen (unchanged)
│   │
│   ├── ProfileScreen (list of profiles)
│   │   └── → ProfileDetailScreen (on profile click)
│   │       ├── [Tab 1] ProfileSpoofContent
│   │       │   └── CategorySection × 5 (Device, Network, etc.)
│   │       │       └── SpoofValueItem × n
│   │       └── [Tab 2] ProfileAppsContent
│   │           └── AppListItem × n (with checkbox)
│   │
│   ├── GlobalSpoofScreen (master switches + default values)
│   │   └── GlobalCategorySection × 5
│   │       └── GlobalSpoofToggle × n (switch + optional default value)
│   │
│   └── SettingsScreen (unchanged)
```

## Open Questions

1. **Should disabled global types show grayed-out in profile detail?**
   - Proposed: Yes, with "Disabled globally" label

2. **What happens when user tries to assign app already in another profile?**
   - Proposed: Show dialog "Move to this profile?" → Removes from old profile

3. **Should we show profile badge on app icons in hook layer logs?**
   - Proposed: Yes for debug mode, helps troubleshooting
