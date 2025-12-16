# user-interface Specification Delta

## REMOVED Requirements

### Requirement: Global Spoof Settings Screen

**Reason**: Replaced with independent profile-based controls. Each profile now manages its own spoof type enable/disable settings without global overrides.

**Migration**: Users configure spoof types within each profile's "Spoof Identity" tab. All spoof types are enabled by default in profiles.

---

## MODIFIED Requirements

### Requirement: Bottom Navigation

The app SHALL use a bottom navigation bar for primary navigation.

#### Scenario: Navigation Items

- **WHEN** viewing the bottom bar
- **THEN** the following items are visible: Home, Profiles, Settings
- **AND** the "Global" tab is NOT present

#### Scenario: Active Indicator

- **WHEN** navigating between screens
- **THEN** the active item is highlighted with animation
- **AND** the transition is smooth

---

### Requirement: Home Screen

The Home Screen SHALL display module status, profile selection, and quick access to key functions.

#### Scenario: Module Status Display

- **WHEN** the user opens the app
- **THEN** they see whether the module is ACTIVE or INACTIVE
- **AND** a status indicator is prominently displayed

#### Scenario: Profile Dropdown Selector

- **WHEN** on the home screen
- **THEN** a dropdown menu shows all available profiles
- **AND** the user can select which profile to view/manage
- **AND** the selected profile name is displayed

#### Scenario: Protected Apps Count

- **WHEN** on the home screen
- **THEN** the count of protected apps reflects the selected profile's assigned apps
- **AND** if the selected profile is disabled, the count shows 0

#### Scenario: Quick Actions - Configure

- **WHEN** the user taps "Configure"
- **THEN** they are navigated to the ProfileDetailScreen for the selected profile

#### Scenario: Quick Actions - Regenerate All

- **WHEN** the user taps "Regenerate All"
- **THEN** all spoof values for the selected profile are regenerated
- **AND** a confirmation message is shown

---

### Requirement: Profile Screen

The Profile Screen SHALL manage saved spoof profiles with enable/disable controls.

#### Scenario: Profile List

- **WHEN** the user navigates to profiles
- **THEN** all saved profiles are displayed
- **AND** the default profile is visually indicated
- **AND** each profile shows an enable/disable switch

#### Scenario: Profile Enable/Disable Switch

- **WHEN** the user toggles a profile's switch
- **THEN** the profile's `isEnabled` state changes
- **AND** when disabled, the profile card appears grayed out
- **AND** disabled profiles can still be edited

#### Scenario: Create Profile

- **WHEN** the user taps the FAB (Floating Action Button)
- **THEN** a dialog appears for profile creation
- **AND** the profile name input is limited to 12 characters
- **AND** a character counter shows current/max length

#### Scenario: Delete Profile

- **WHEN** the user deletes a profile
- **THEN** a confirmation dialog appears
- **AND** deletion cannot be undone

#### Scenario: Set Default

- **WHEN** the user sets a profile as default
- **THEN** it becomes the fallback for apps without explicit assignment

---

## ADDED Requirements

### Requirement: Profile Detail Screen - Spoof Identity Tab

The Profile Detail Screen SHALL have a "Spoof Identity" tab for configuring spoof values.

#### Scenario: Tab Label

- **WHEN** viewing profile details
- **THEN** the first tab is labeled "Spoof Identity" (not "Spoof Values")

#### Scenario: Category Cards Collapsed by Default

- **WHEN** entering the Spoof Identity tab
- **THEN** all category cards are collapsed by default

#### Scenario: Collapse State Persistence Within Session

- **WHEN** the user expands/collapses a category card
- **AND** navigates away and returns within the same app session
- **THEN** the expand/collapse state is preserved
- **AND** state resets when app is restarted

#### Scenario: Independent Spoof Type Controls

- **WHEN** viewing spoof type toggles
- **THEN** each type can be enabled/disabled independently
- **AND** there is no "Disabled globally" indicator (global config removed)

---

### Requirement: Profile Detail Screen - Target Apps Tab

The Profile Detail Screen SHALL have a "Target Apps" tab for assigning apps to the profile.

#### Scenario: Tab Label

- **WHEN** viewing profile details
- **THEN** the second tab is labeled "Target Apps" (not "Apps")

#### Scenario: System Apps Filtered

- **WHEN** viewing the target apps list
- **THEN** system apps are NOT shown
- **AND** only user-installed apps appear

#### Scenario: Own App Excluded

- **WHEN** viewing the target apps list
- **THEN** the Device Masker app itself is NOT shown
- **AND** this prevents accidental self-hooking

#### Scenario: Real App Icons

- **WHEN** viewing an app in the target apps list
- **THEN** the app's actual icon is displayed
- **AND** if icon loading fails, a default placeholder is shown

---

### Requirement: Profile-Based Spoofing Control

Each profile SHALL independently control its spoofing behavior.

#### Scenario: Profile Enabled

- **WHEN** a profile has `isEnabled = true`
- **THEN** apps assigned to this profile receive spoofed values
- **AND** individual spoof type toggles within the profile are respected

#### Scenario: Profile Disabled

- **WHEN** a profile has `isEnabled = false`
- **THEN** apps assigned to this profile receive original (unspoofed) values
- **AND** the profile can still be edited in the UI
- **AND** no hooks are applied to the profile's apps

#### Scenario: No Global Override

- **WHEN** spoofing is evaluated for an app
- **THEN** only the assigned profile's settings are checked
- **AND** there is no global enable/disable layer
