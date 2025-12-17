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
- ✅ Phase 10: Code Quality Refactor (Modern Hooks & UI)

## Recent Achievements
- **Standardized Hookers**: Refactored all 5 hooker classes to use a non-nullable `getSpoofValueOrGenerate` pattern. This eliminated redundant null checks and simplified the logic for providing fallback values when spoofing is disabled or unavailable.
- **Modernized String Literals**: Adopted multi-dollar string literals (`$$"outer$inner"`) for class names containing inner classes, improving readability and adhering to modern Kotlin standards.
- **UI Code Quality**: Reordered parameters in all Composable screens (`HomeScreen`, `ProfileScreen`, `SettingsScreen`, `DiagnosticsScreen`, etc.) to ensure the `modifier` parameter is consistently the first optional parameter.
- **KTX Adherence**: Updated `MigrationManager` to use the idiomatic KTX `SharedPreferences.edit` extension, removing boilerplate and improving maintainability.
- **Lint & Privacy**: Suppressed `HardwareIds` warnings for `ANDROID_ID` usage in the Diagnostics screen, as spoofing these IDs is the core functionality of the app.

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

### Hooker Pattern (Refactored)
```kotlin
private fun getSpoofValueOrGenerate(
    context: Context?,
    type: SpoofType,
    generator: () -> String
): String { // Now returns non-nullable String
    val provider = getProvider(context)
    if (provider == null) {
        return generator()
    }
    
    // getSpoofValue now handles ALL checks:
    // 1. Profile exists for this app
    // 2. Profile is enabled
    // 3. Type is enabled in profile
    return provider.getSpoofValue(type) ?: generator()
}
```

## Important Patterns & Preferences

### Profile-Based Spoofing
Profiles are now independent. The old pattern with `isTypeEnabledGlobally()` has been completely removed.
Each profile controls its own spoofing behavior entirely.

### Modifier Order Convention
Always place the `modifier` parameter as the first optional parameter in Composables:
`fun MyComponent(required: String, modifier: Modifier = Modifier, optional: Int = 0)`
