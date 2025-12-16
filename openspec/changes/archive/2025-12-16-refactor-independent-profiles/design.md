# Design: Independent Profile-Based Spoofing Architecture

## Context

The current Device Masker architecture implements a two-layer spoofing control system:
1. **GlobalSpoofConfig** - Master switches that override all profiles
2. **SpoofProfile** - Individual profile settings

This creates user confusion and adds complexity to the hook layer. When a user enables a spoof type in their profile but it shows as "Disabled globally," they need to navigate to a separate screen to enable it.

### Stakeholders
- **End users** - Want simple, intuitive profile management
- **Developers** - Need clean architecture for hook layer
- **Hook layer** - Must resolve spoof values efficiently

## Goals / Non-Goals

### Goals
- Each profile works completely independently
- Simple on/off switch per profile controls all its apps
- Clean data flow: Profile → Apps → Hooks
- Session-based UI state for better UX
- Real app icons for visual recognition
- Exclude system apps and self to prevent issues

### Non-Goals
- Supporting Apple/iOS devices
- Per-app override of profile settings (all apps in profile use same settings)
- Cross-profile app assignment (an app can only belong to one profile)
- Persistent UI state across app restarts (session-only for simplicity)

## Decisions

### Decision 1: Remove GlobalSpoofConfig Entirely

**Choice**: Delete `GlobalSpoofConfig.kt` and all references instead of making it optional.

**Rationale**: 
- Simpler architecture with single source of truth (profiles)
- Less code to maintain
- Clearer user mental model
- Each profile's `isTypeEnabled()` already exists and works

**Alternatives Considered**:
- Keep GlobalConfig as "advanced settings" → Adds confusion
- Make GlobalConfig a "template" for new profiles → Already implemented, rarely used

### Decision 2: Add `isEnabled` Boolean to SpoofProfile

**Choice**: Simple boolean field, default `true`.

**Rationale**:
- Minimal data model change
- Easy to check in hook layer: `if (!profile.isEnabled) return null`
- Allows editing disabled profile (user expectation)

**Implementation**:
```kotlin
@Serializable
data class SpoofProfile(
    val id: String,
    val name: String,
    val isEnabled: Boolean = true,  // NEW FIELD
    val identifiers: Map<SpoofType, DeviceIdentifier>,
    val assignedApps: Set<String>,
    // ... other fields
)
```

### Decision 3: 3-Tab Navigation Layout

**Choice**: `Home | Profiles | Settings` (remove Global Spoof tab)

**Rationale**:
- Cleaner navigation with focused purposes
- Home = Dashboard overview
- Profiles = All profile management (list + detail)
- Settings = App settings, theme, diagnostics access

### Decision 4: Session-Based UI State for Spoof Cards

**Choice**: Use `remember {}` with key-based state, not persisted to DataStore.

**Rationale**:
- Simpler implementation
- No DataStore schema changes
- UI state resets on app restart (acceptable trade-off)
- Meets user expectation (collapse states remembered during session)

**Implementation Pattern**:
```kotlin
// In ProfileDetailScreen
val expandedCategories = remember { mutableStateMapOf<SpoofCategory, Boolean>() }

// Default collapsed, expand on click
val isExpanded = expandedCategories[category] ?: false
```

### Decision 5: Load Real App Icons Using PackageManager

**Choice**: Load `Drawable` via `packageManager.getApplicationIcon()`, display with `Image(painter = rememberDrawablePainter(icon))`.

**Rationale**:
- Standard Android approach
- Coil's `rememberAsyncImagePainter` can handle Drawable
- No additional permissions needed

**Implementation**:
```kotlin
val context = LocalContext.current
val icon = remember(app.packageName) {
    try {
        context.packageManager.getApplicationIcon(app.packageName)
    } catch (e: Exception) {
        null
    }
}
```

### Decision 6: HomeScreen Profile Selector via Dropdown

**Choice**: Use `ExposedDropdownMenu` with `DropdownMenuItem` list.

**Rationale**:
- Material 3 standard component
- Works well with many profiles
- Clear selection state
- Takes minimal screen space

### Decision 7: Filter System Apps and Self from Target Apps

**Choice**: Filter in `ProfileAppsContent` composable using existing `isSystemApp` flag.

**Rationale**:
- System apps shouldn't be hooked (can cause system instability)
- Own app must never hook itself (causes infinite loops/crashes)

**Implementation**:
```kotlin
val filteredApps = installedApps.filter { app ->
    !app.isSystemApp && 
    app.packageName != BuildConfig.APPLICATION_ID
}
```

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| Users with GlobalConfig settings lose them | Migration sets all profile types to enabled by default (no data loss) |
| Session UI state lost on restart | Acceptable UX trade-off; simple implementation |
| Loading many app icons may be slow | Use `remember` to cache icons; lazy loading in LazyColumn |
| Profile without `isEnabled` field (old data) | Default value `true` in data class handles this |

## Migration Plan

### Phase 1: Data Model Changes
1. Add `isEnabled: Boolean = true` to `SpoofProfile`
2. Existing profiles automatically get `isEnabled = true` (default)
3. No migration code needed for this field

### Phase 2: Remove GlobalSpoofConfig
1. Delete `GlobalSpoofConfig.kt`
2. Remove from `SpoofDataStore.kt` (storage methods)
3. Remove from `SpoofRepository.kt` (flow and access)
4. Update `HookDataProvider.kt` to remove global checks
5. Old global config JSON in DataStore becomes orphan (ignored)

### Phase 3: Navigation Changes
1. Remove GlobalSpoofScreen composable
2. Update `NavDestination.kt` to 3 tabs
3. Update `MainActivity.kt` NavHost

### Phase 4: UI Updates
1. ProfileCard + ProfileScreen enable switch
2. ProfileDetailScreen tab renames, icon loading, state management
3. HomeScreen dropdown and expandable profile

### Rollback Plan
- Revert commits if issues found
- GlobalSpoofConfig data still exists in DataStore (can restore if needed)
- No database migrations to undo

## Open Questions

1. **Character limit for profile name** - Confirmed: 12 characters max
2. **Should disabled profile apps still appear in HomeScreen count?** - Proposal: No, only count enabled profiles' apps

---

## Architecture Diagram

```
┌─────────────────────┐
│     HomeScreen      │
│  ┌───────────────┐  │
│  │ Profile       │  │
│  │ Dropdown      │──┼────► Selected Profile ID
│  └───────────────┘  │
│  Protected Apps: N  │◄───── selectedProfile.assignedApps.size
│  Quick Actions      │
│  ├─ Configure ──────┼────► Navigate to ProfileDetailScreen
│  └─ Regenerate ─────┼────► Regenerate selectedProfile values
└─────────────────────┘

┌─────────────────────┐
│   ProfileScreen     │
│  ┌───────────────┐  │
│  │ ProfileCard   │  │
│  │ ┌─────────┐   │  │
│  │ │ Switch  │   │──┼────► profile.isEnabled toggle
│  │ └─────────┘   │  │
│  └───────────────┘  │
└─────────────────────┘

┌─────────────────────┐
│ProfileDetailScreen  │
│  Tabs:              │
│  ├─ Spoof Identity  │──── Value cards with expand/collapse state
│  └─ Target Apps     │──── Filtered list (no system apps, no self)
│       App Icons ────┼──── Real icons via PackageManager
└─────────────────────┘

┌─────────────────────┐
│  HookDataProvider   │
│                     │
│  Check order:       │
│  1. profile.isEnabled?
│  2. profile.isTypeEnabled(type)?
│  3. Return profile.getValue(type)
└─────────────────────┘
```
