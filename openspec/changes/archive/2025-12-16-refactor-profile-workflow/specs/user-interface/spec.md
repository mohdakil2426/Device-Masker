## MODIFIED Requirements

### Requirement: Bottom Navigation

The app SHALL use a bottom navigation bar for primary navigation.

#### Scenario: Navigation Items

- **WHEN** viewing the bottom bar
- **THEN** the following items are visible: Home, Profiles, Spoof (Global), Settings
- **AND** the Spoof tab acts as master control for all profiles

#### Scenario: Active Indicator

- **WHEN** navigating between screens
- **THEN** the active item is highlighted with animation
- **AND** the transition is smooth

---

### Requirement: Profile Screen

The Profile Screen SHALL display a list of profiles with navigation to details.

#### Scenario: Profile List

- **WHEN** the user navigates to profiles
- **THEN** all saved profiles are displayed as cards
- **AND** each card shows profile name, description, and assigned app count

#### Scenario: Profile Card Click

- **WHEN** the user taps a profile card
- **THEN** the app navigates to ProfileDetailScreen
- **AND** the profile's full configuration is shown

#### Scenario: Create Profile

- **WHEN** the user taps the FAB (Floating Action Button)
- **THEN** a dialog prompts for profile name
- **AND** the new profile is created with values copied from global defaults

#### Scenario: App Count Display

- **WHEN** viewing a profile card
- **THEN** the card displays the number of assigned apps (e.g., "3 apps")
- **AND** shows "No apps" if zero apps assigned

---

## ADDED Requirements

### Requirement: Global Spoof Screen

The Global Spoof Screen SHALL control master switches and default values for all spoof types.

#### Scenario: Master Switch Control

- **WHEN** the user toggles a spoof type OFF globally
- **THEN** that spoof type is disabled for ALL profiles
- **AND** hooks skip that type and return real values

#### Scenario: Master Switch Enabled

- **WHEN** a spoof type is enabled globally
- **THEN** profiles can use their own values for that type
- **AND** the toggle shows ON state

#### Scenario: Default Value Display

- **WHEN** viewing a spoof type in Global Spoof Screen
- **THEN** the current default value is displayed
- **AND** a regenerate button is available
- **AND** an edit button is available

#### Scenario: Default Value Purpose

- **WHEN** a new profile is created
- **THEN** the profile's initial values are copied from global defaults
- **AND** existing profiles are NOT affected by global default changes

#### Scenario: Category Organization

- **WHEN** viewing the Global Spoof Screen
- **THEN** spoof types are organized into 5 expandable categories:
  - Device (IMEI, Serial, etc.)
  - Network (MAC, SSID, etc.)
  - Advertising (GSF ID, GAID, etc.)
  - System (Build properties)
  - Location (GPS, Timezone, etc.)

---

### Requirement: Profile Detail Screen

The Profile Detail Screen SHALL display a tabbed interface for profile configuration.

#### Scenario: Two-Tab Layout

- **WHEN** opening a profile detail screen
- **THEN** two tabs are visible: "Spoof Values" and "Apps"
- **AND** the user can swipe between tabs

#### Scenario: Spoof Values Tab

- **WHEN** on the Spoof Values tab
- **THEN** all spoof types are displayed with profile-specific values
- **AND** the user can edit, regenerate, or copy values
- **AND** types disabled globally show as grayed out with "Disabled globally" label

#### Scenario: Apps Tab

- **WHEN** on the Apps tab
- **THEN** all installed apps are displayed
- **AND** apps assigned to THIS profile show checked
- **AND** apps assigned to OTHER profiles show the other profile's name and disabled checkbox

#### Scenario: Assign App to Profile

- **WHEN** the user checks an unassigned app
- **THEN** the app is added to this profile's assignedApps
- **AND** hooks will use this profile's values for that app

#### Scenario: Move App Between Profiles

- **WHEN** the user checks an app already assigned to another profile
- **THEN** a confirmation dialog appears: "Move to this profile?"
- **AND** on confirm, the app is removed from the old profile and added to this one

#### Scenario: Remove App from Profile

- **WHEN** the user unchecks an assigned app
- **THEN** the app is removed from this profile's assignedApps
- **AND** the app will use the default profile's values

#### Scenario: App Search

- **WHEN** the user types in the search bar on Apps tab
- **THEN** the app list filters by name or package
- **AND** filter chips allow filtering by User Apps / System Apps / All

---

## REMOVED Requirements

### Requirement: App Selection Screen

**Reason**: Replaced by profile-scoped app assignment in ProfileDetailScreen

**Migration**: The AppSelectionScreen composable is repurposed as ProfileAppsContent within ProfileDetailScreen. Users now assign apps from within each profile's detail view instead of a standalone screen.

---

### Requirement: Spoof Settings Screen (as main nav destination)

**Reason**: Replaced by GlobalSpoofScreen (master switches) and ProfileDetailScreen (profile values)

**Migration**: 
- Global toggles and default values → GlobalSpoofScreen
- Profile-specific values → ProfileDetailScreen's Spoof Values tab
- The original SpoofSettingsScreen component logic is split between these two screens
