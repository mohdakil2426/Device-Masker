# Active Context: Device Masker

## Current Work Focus

### ✅ Completed Change: `refactor-profile-workflow` (ARCHIVED)

**Status**: Fully Complete & Archived
**Location**: `openspec/changes/archive/2025-12-16-refactor-profile-workflow/`
**Archived Date**: December 17, 2025

### Remaining Active Changes

| Change ID | Status | Progress |
|-----------|--------|----------|
| `implement-privacy-shield-module` | Active | 126/149 tasks |
| `rebrand-to-device-masker` | Not Started | 0/103 tasks |

## Completed Work: Profile-Centric Workflow Redesign

### Summary

The profile-centric workflow redesign was a major architectural change that:
1. Moved app assignment from central AppConfig to profile-based `SpoofProfile.assignedApps`
2. Added `GlobalSpoofConfig` for master switches and default value templates
3. Reorganized navigation from 5-tab to 4-tab layout
4. Implemented `ProfileDetailScreen` with tabbed interface (Spoof Values / Apps)
5. Updated all hookers to use profile-based value resolution via `HookDataProvider`
6. Added data migration from V1 to V2 schema

### All 9 Phases Complete

| Phase | Description | Status |
|-------|-------------|--------|
| Phase 1 | Data Model Changes | ✅ |
| Phase 2 | Navigation Updates | ✅ |
| Phase 3 | GlobalSpoofScreen | ✅ |
| Phase 4 | ProfileDetailScreen | ✅ |
| Phase 5 | Profile Screen Updates | ✅ |
| Phase 6 | Hook Layer Updates | ✅ |
| Phase 7 | Data Migration | ✅ |
| Phase 8 | Cleanup & Polish | ✅ |
| Phase 9 | Testing | ✅ |

### Key Files Created/Modified

#### New Files
- `data/models/GlobalSpoofConfig.kt` - Master switches and default values
- `hook/HookDataProvider.kt` - Profile resolution for hook layer
- `data/MigrationManager.kt` - V1→V2 data migration
- `ui/screens/GlobalSpoofScreen.kt` - Global spoof settings UI
- `ui/screens/ProfileDetailScreen.kt` - Profile detail with tabs

#### Modified Files
- `SpoofProfile.kt` - Added `assignedApps: Set<String>`
- `SpoofDataStore.kt` - GlobalSpoofConfig storage
- `ProfileRepository.kt` - App assignment methods
- `SpoofRepository.kt` - GlobalSpoofConfig support
- `NavDestination.kt` - 4-tab layout
- `MainActivity.kt` - Updated navigation
- All `*Hooker.kt` files - Profile-based value resolution

## Recent Changes

### December 17, 2025 (Profile Workflow Archive)

| Time | Change | Status |
|------|--------|--------|
| 00:40 | Completed Phase 9 testing | ✅ |
| 00:41 | Archived refactor-profile-workflow change | ✅ |
| 00:42 | Updated memory bank | ✅ |

### December 16, 2025 (Profile Workflow Completion)

| Time | Change | Status |
|------|--------|--------|
| 19:00 | Phase 6: Updated all 5 hookers with HookDataProvider | ✅ |
| 20:00 | Phase 7: Created MigrationManager with V1→V2 migration | ✅ |
| 21:00 | Phase 8: UI Polish (empty states, loading, snackbars) | ✅ |
| 23:12 | Phase 8.2.3: Added confirmation snackbars | ✅ |

### December 16, 2025 (Profile Workflow Start)

| Time | Change | Status |
|------|--------|--------|
| 14:32 | Created proposal, design, tasks for profile workflow | ✅ |
| 15:00 | Phase 1: Data model changes (GlobalSpoofConfig, assignedApps) | ✅ |
| 16:00 | Phase 2: Navigation updates (4-tab layout) | ✅ |
| 17:00 | Phase 3 & 4: GlobalSpoofScreen and ProfileDetailScreen | ✅ |
| 18:00 | Phase 5: Updated ProfileCard with app count | ✅ |

## Next Steps

### Immediate Priority
- Continue with `implement-privacy-shield-module` remaining tasks (23 remaining)
- OR start `rebrand-to-device-masker` change

### Future Enhancements
1. Per-app profile switching via quick settings tile
2. Backup/restore profiles to file
3. Import device fingerprints from real devices
4. Root detection bypass (SafetyNet/Play Integrity)

## Architecture Overview (Post-Redesign)

### Data Flow
```
User selects app → Find profile with app in assignedApps → Use profile values
                                    ↓
                    GlobalSpoofConfig.isTypeEnabled() check
                                    ↓
                         If enabled → Return spoofed value
                         If disabled → Return original value
```

### Navigation Structure
```
Bottom Navigation (4 tabs):
├── Home (Dashboard)
├── Profiles → ProfileDetailScreen (with tabs)
│   ├── Tab: Spoof Values (per-profile)
│   └── Tab: Apps (assignment)
├── Global Spoof (master switches + defaults)
└── Settings
```

## Files to Watch

| File | Reason |
|------|--------|
| `hook/HookDataProvider.kt` | Central profile resolution for hooks |
| `data/MigrationManager.kt` | Data schema migrations |
| `data/models/GlobalSpoofConfig.kt` | Master switch logic |
| `ui/screens/ProfileDetailScreen.kt` | Main profile editing UI |
| `hook/hooker/*Hooker.kt` | All 5 hookers use profile values |

## Important Patterns & Preferences

### Profile-Based Spoofing Pattern
```kotlin
fun getSpoofValueOrGenerate(
    packageName: String,
    spoofType: SpoofType,
    generateValue: () -> String
): String? {
    val config = getGlobalConfig()
    if (!config.isTypeEnabled(spoofType)) {
        YLog.debug("$spoofType disabled globally, skipping spoof")
        return null
    }
    
    val profile = getProfileForPackage(packageName)
    if (profile != null) {
        val identifier = profile.getIdentifier(spoofType)
        if (identifier != null && identifier.isEnabled) {
            YLog.debug("Using profile '${profile.name}' value for $spoofType")
            return identifier.value
        }
    }
    
    // Fallback to generated value
    return generateValue()
}
```

### Snackbar Confirmation Pattern
```kotlin
scope.launch {
    // Perform action
    repository.updateProfile(updated)
    
    // Show confirmation
    snackbarHostState.showSnackbar(
        message = "${type.displayName} regenerated",
        duration = SnackbarDuration.Short
    )
}
```
