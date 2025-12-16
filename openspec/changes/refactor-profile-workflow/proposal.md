# Change: Refactor Profile-Centric Workflow

## Why

The current workflow separates profile management from app selection, making it difficult for users to understand:
- Which apps are using which profile
- How to configure spoofing for a specific group of apps
- The relationship between global settings and per-profile values

Users expect a **profile-centric mental model** where each profile is a complete "identity bundle" containing both spoof values AND assigned apps.

## What Changes

### Navigation Simplification
- **REMOVE** the standalone "Apps" tab from bottom navigation
- **REDUCE** to 4 tabs: Home → Profiles → Spoof (Global) → Settings
- **ADD** Global Spoof screen as master switch layer

### Global Spoof Screen (NEW)
- Acts as **master control** for all spoof types
- Each spoof type has:
  - **Enable/Disable toggle** (master switch - affects ALL profiles)
  - **Default value** (template for NEW profiles only)
- When a type is disabled globally → all profiles cannot spoof that type
- Changing a value globally → only affects new profile creation (existing profiles keep their values)

### Profile Detail Screen (NEW)
- When user taps a profile card → opens **ProfileDetailScreen**
- **Two-tab layout**:
  - **Tab 1: Spoof Values** - Profile-specific values (independent from global after creation)
  - **Tab 2: Apps** - Select which apps use THIS profile's spoof values
- Profiles inherit global values at creation time, then become independent
- Profiles respect global enabled/disabled state (master switch)

### Data Model Changes
- **SpoofProfile** gains `assignedApps: Set<String>` field for app package names
- **Global config** stores enabled/disabled state per SpoofType + default values
- Remove per-app profile assignment from AppConfig (now lives in profile)

### Workflow Changes
| Current | New |
|---------|-----|
| Create profile → separately assign to apps | Create profile → configure values + apps in one place |
| Global spoof = active profile values | Global spoof = master switches + default template |
| 5-tab navigation | 4-tab navigation |
| Apps tab shows all apps | Apps are grouped by profile |

### **NON-BREAKING**: Hook Layer
- HookEntry resolves `packageName → profile` by scanning profiles' `assignedApps`
- If app not in any profile → use default profile
- Respects global enabled/disabled state

## Impact

- **Affected specs**: 
  - `user-interface` (MODIFIED - navigation, screens)
  - `data-management` (MODIFIED - profile model, global config)
  
- **Affected code**:
  - `ui/navigation/NavDestination.kt` - Remove APPS route
  - `ui/navigation/BottomNavBar.kt` - 4 tabs instead of 5
  - `ui/screens/ProfileScreen.kt` - Now opens detail instead of inline
  - `ui/screens/ProfileDetailScreen.kt` - NEW tabbed screen
  - `ui/screens/GlobalSpoofScreen.kt` - NEW (rename/refactor from SpoofSettingsScreen)
  - `ui/screens/AppSelectionScreen.kt` - Repurposed for profile-scoped use
  - `data/models/SpoofProfile.kt` - Add assignedApps field
  - `data/models/GlobalConfig.kt` - NEW model for global switches
  - `data/repository/SpoofRepository.kt` - New methods for global config
  - `data/repository/ProfileRepository.kt` - Handle assignedApps
  - `data/SpoofDataStore.kt` - Store global config
  - `hook/hooker/*Hooker.kt` - Read global enabled state + profile lookup

## UI Consistency Requirements

All UI changes must follow existing patterns from `systemPatterns.md`:

| Pattern | Implementation |
|---------|----------------|
| **Card styling** | `ElevatedCard` + `surfaceContainerHigh` + `shapes.large` |
| **Inline headers** | `headlineMedium` + `FontWeight.Bold` for main nav screens |
| **TabRow** | Material 3 `TabRow` with `PrimaryTabRow` pattern |
| **Spring animations** | Use `AppMotion.FastSpring` for all transitions |
| **Color scheme** | Dark theme uses complete color scheme from Theme.kt |
| **FAB positioning** | `Box` with `Alignment.BottomEnd` + 16.dp padding |
| **Profile card** | Reuse existing `ProfileCard` component |
| **App list item** | Reuse existing `AppListItem` component |

## Success Criteria

1. Bottom navigation has 4 tabs: Home, Profiles, Spoof, Settings
2. Tapping a profile opens ProfileDetailScreen with 2 tabs
3. Global Spoof screen controls enabled/disabled state for all profiles
4. Profiles have their own values (copied from global at creation)
5. Apps can be assigned to profiles from profile detail screen
6. Hook layer respects global enabled state + profile values
7. UI matches existing styling patterns (cards, colors, animations)
