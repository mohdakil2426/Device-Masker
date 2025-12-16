# Active Context: Device Masker

## Current Work Focus

### ✅ Completed Change: `refactor-independent-profiles`

**Status**: Complete - Ready to Archive
**Location**: `openspec/changes/refactor-independent-profiles/`
**Started**: December 17, 2025
**Completed**: December 17, 2025

#### All Phases Complete
- ✅ Phase 1: Data Model Changes (isEnabled field)
- ✅ Phase 2: Remove GlobalSpoofConfig (BREAKING)
- ✅ Phase 3: Navigation Updates (3-tab layout)
- ✅ Phase 4: ProfileScreen Updates (enable switch, 12-char limit)
- ✅ Phase 5: ProfileDetailScreen (collapse state, app filtering, real icons)
- ✅ Phase 6: HomeScreen Updates (profile dropdown)
- ✅ Phase 7: Hook Layer Updates (all hookers refactored)
- ✅ Phase 8: Testing & Validation (all tests passed)
- ✅ Phase 9: Cleanup (Spotless formatting configured)

### Archived Changes

| Change ID | Status | Archived Date |
|-----------|--------|---------------|
| `implement-privacy-shield-module` | Archived | Dec 16, 2025 |
| `refactor-profile-workflow` | Archived | Dec 17, 2025 |
| `refactor-independent-profiles` | Ready to Archive | Dec 17, 2025 |

## Recent Changes

### December 17, 2025 (Independent Profiles Refactor)

| Time | Change | Status |
|------|--------|--------|
| 02:55 | HomeScreen profile dropdown selector | ✅ |
| 02:50 | Real app icons in ProfileDetailScreen | ✅ |
| 02:50 | Filter system apps and own app | ✅ |
| 02:50 | Session-based collapse state for spoof cards | ✅ |
| 02:50 | 12-character limit on profile names | ✅ |
| 02:45 | Removed GlobalSpoofConfig from ProfileDetailScreen | ✅ |
| 02:40 | Refactored all 5 hookers - removed isTypeEnabledGlobally | ✅ |
| 02:35 | Updated MigrationManager - removed GlobalSpoofConfig refs | ✅ |
| 02:30 | Build verification - SUCCESS | ✅ |

### Key Accomplishments Today
1. **Removed GlobalSpoofConfig entirely** - Profiles are now fully independent
2. **Updated all 5 hookers** - Removed `isTypeEnabledGlobally` checks
3. **HomeScreen profile dropdown** - Select active profile from dropdown
4. **Real app icons** - Using PackageManager to load actual app icons
5. **System app filtering** - Excludes system apps and own app by default
6. **12-char profile name limit** - With character counter in dialog
7. **Session-based UI state** - Spoof cards default to collapsed

## Architecture Overview (Post-Refactor)

### Data Flow (NEW - Independent Profiles)
```
User selects app → Find profile with app in assignedApps
                         ↓
              Check profile.isEnabled
                         ↓
         If enabled → Check type.isEnabled in profile
                         ↓
              If enabled → Return spoofed value
              If disabled → Return null (original value)
```

**Key Difference**: No more global config checks! Each profile controls its own:
- Master enable/disable switch (`profile.isEnabled`)
- Per-type enable/disable toggles (`profile.isTypeEnabled(type)`)

### Navigation Structure (3-Tab Layout)
```
Bottom Navigation (3 tabs):
├── Home (Dashboard with profile dropdown)
├── Profiles → ProfileDetailScreen (with tabs)
│   ├── Tab: Spoof Values (per-profile, collapsed by default)
│   └── Tab: Apps (filtered, real icons)
└── Settings
```

### HomeScreen Profile Dropdown
- Shows all profiles with selection state
- Updates protected apps count based on selected profile
- "Configure" navigates to selected profile's detail screen
- "Regenerate All" regenerates values for selected profile

### Hooker Pattern (SIMPLIFIED)
```kotlin
private fun getSpoofValueOrGenerate(
    context: Context?,
    type: SpoofType,
    generator: () -> String
): String? {
    val provider = getProvider(context)
    if (provider == null) {
        return generator() // Fallback
    }
    
    // getSpoofValue now handles ALL checks:
    // 1. Profile exists for this app
    // 2. Profile is enabled
    // 3. Type is enabled in profile
    return provider.getSpoofValue(type) ?: generator()
}
```

## Files Modified Today

| File | Change |
|------|--------|
| `ui/screens/HomeScreen.kt` | Added profile dropdown selector |
| `ui/screens/ProfileDetailScreen.kt` | Real icons, filtering, collapse state |
| `ui/screens/ProfileScreen.kt` | 12-char name limit |
| `hook/hooker/DeviceHooker.kt` | Removed isTypeEnabledGlobally check |
| `hook/hooker/NetworkHooker.kt` | Removed isTypeEnabledGlobally check |
| `hook/hooker/SystemHooker.kt` | Removed isTypeEnabledGlobally check |
| `hook/hooker/LocationHooker.kt` | Removed isTypeEnabledGlobally check |
| `hook/hooker/AdvertisingHooker.kt` | Removed isTypeEnabledGlobally check |
| `data/MigrationManager.kt` | Removed GlobalSpoofConfig references |

## Next Steps

### Device Testing Required
- Test profile enable/disable switch works
- Test disabled profiles don't hook apps
- Test app filtering (system apps, own app)
- Test real app icons display correctly
- Test HomeScreen profile dropdown
- Test spoof card collapse state persists

### Cleanup Tasks
- Remove unused imports
- Run code formatting
- Final build verification on device

## Important Patterns & Preferences

### Profile-Based Spoofing Pattern (NEW)
```kotlin
// In HookDataProvider.getSpoofValue()
fun getSpoofValue(type: SpoofType): String? {
    val profile = _profile ?: return null
    
    // Master switch check
    if (!profile.isEnabled) {
        YLog.debug("Profile '${profile.name}' is disabled")
        return null
    }
    
    // Type-level check
    if (!profile.isTypeEnabled(type)) {
        YLog.debug("$type is disabled in profile '${profile.name}'")
        return null
    }
    
    return profile.getValue(type)
}
```

### No More Global Config!
The old pattern with `isTypeEnabledGlobally()` has been completely removed.
Profiles are now independent - each controls its own spoofing behavior.

