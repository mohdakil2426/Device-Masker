# Change: Refactor to Independent Profile-Based Spoofing

## Why

The current architecture uses a `GlobalSpoofConfig` layer that acts as master switches controlling all profiles. This creates confusion for users about whether spoofing is controlled globally or per-profile, and adds unnecessary complexity. Users expect each profile to work independently with its own enable/disable controls.

Additionally, the UI needs several improvements:
- Profile cards lack enable/disable switches
- App icons show placeholders instead of real icons
- System apps clutter the target apps list
- Spoof value cards don't remember collapse/expand state
- HomeScreen profile selection is not intuitive

## What Changes

### **BREAKING** - Remove GlobalSpoofScreen & GlobalSpoofConfig
- Delete `GlobalSpoofScreen.kt` and remove from navigation
- Delete `GlobalSpoofConfig.kt` data model
- Remove GlobalSpoofConfig from `SpoofDataStore.kt` and `SpoofRepository.kt`
- Update `HookDataProvider.kt` to only check profile-level settings
- Change navigation from 4-tab to 3-tab layout: **Home | Profiles | Settings**

### Profile Data Model Changes
- Add `isEnabled: Boolean` field to `SpoofProfile` data class
- When `isEnabled = false`, hooks skip all apps assigned to that profile
- Each profile's individual spoof type toggles work independently (no global override)

### UI Improvements

#### ProfileScreen
- Add enable/disable switch to each `ProfileCard`
- Disabled profile shows grayed-out appearance but remains editable
- Add 12-character limit to profile name input

#### ProfileDetailScreen
- Rename tab: "Spoof Values" → "Spoof Identity"
- Rename tab: "Apps" → "Target Apps"
- Filter out system apps from target apps list
- Exclude own app (`com.astrixforge.devicemasker`) from list for safety
- Show real app icons instead of Android placeholder
- Add UI state management for spoof cards (collapse by default, remember state within session)

#### HomeScreen
- Keep hero status card unchanged
- Add dropdown profile selector instead of single profile display
- "Protected Apps" count reflects selected profile's assigned apps
- "Configure" button navigates to selected profile
- "Regenerate All" regenerates values for selected profile only
- Profile card becomes expandable to show all profiles

## Impact

- **Affected specs**: `user-interface`, `data-management`
- **Affected code**:
  - `data/models/GlobalSpoofConfig.kt` (DELETE)
  - `data/models/SpoofProfile.kt` (MODIFY - add isEnabled)
  - `data/SpoofDataStore.kt` (MODIFY - remove global config)
  - `data/repository/SpoofRepository.kt` (MODIFY - remove global config)
  - `ui/screens/GlobalSpoofScreen.kt` (DELETE)
  - `ui/navigation/NavDestination.kt` (MODIFY - 3 tabs)
  - `ui/screens/ProfileScreen.kt` (MODIFY - add switch)
  - `ui/screens/ProfileDetailScreen.kt` (MODIFY - tabs, icons, state)
  - `ui/screens/HomeScreen.kt` (MODIFY - dropdown, expandable)
  - `ui/components/ProfileCard.kt` (MODIFY - add switch)
  - `hook/HookDataProvider.kt` (MODIFY - remove global checks)
  - `MainActivity.kt` (MODIFY - remove global screen)

## Migration

- Existing profiles will have `isEnabled = true` by default
- GlobalSpoofConfig data will be ignored (all types enabled in profiles by default)
- No data loss - profiles maintain their individual spoof type settings
