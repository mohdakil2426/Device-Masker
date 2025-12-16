# data-management Specification Delta

## REMOVED Requirements

### Requirement: Global Spoof Configuration

**Reason**: Replaced with independent profile-based controls. The GlobalSpoofConfig model and its storage are no longer needed as each profile manages its own spoof type settings.

**Migration**: 
- Existing GlobalSpoofConfig data in DataStore will be orphaned (not deleted, just ignored)
- All profiles retain their individual per-type enable/disable settings
- No user action required

---

## MODIFIED Requirements

### Requirement: Profile Data Model

The SpoofProfile data model SHALL include an enable/disable flag for the entire profile.

#### Scenario: isEnabled Field

- **WHEN** a SpoofProfile is created
- **THEN** it includes an `isEnabled: Boolean` field
- **AND** the default value is `true`

#### Scenario: Enable Profile

- **WHEN** `isEnabled` is set to `true`
- **THEN** all apps assigned to this profile will receive spoofed values
- **AND** individual spoof type toggles are respected

#### Scenario: Disable Profile

- **WHEN** `isEnabled` is set to `false`
- **THEN** all apps assigned to this profile will receive original values
- **AND** hooks skip this profile's apps entirely

#### Scenario: Backward Compatibility

- **WHEN** loading a profile saved before this change (missing `isEnabled` field)
- **THEN** the profile defaults to `isEnabled = true`
- **AND** existing behavior is preserved

---

### Requirement: Profile Repository

The ProfileRepository SHALL provide methods for toggling profile enabled state.

#### Scenario: Set Profile Enabled

- **WHEN** `setProfileEnabled(id: String, enabled: Boolean)` is called
- **THEN** the profile with matching ID has its `isEnabled` field updated
- **AND** the change is persisted to DataStore

#### Scenario: Toggle Profile Enabled

- **WHEN** `toggleProfileEnabled(id: String)` is called
- **THEN** the profile's `isEnabled` field is inverted
- **AND** the change is persisted to DataStore

---

## ADDED Requirements

### Requirement: Hook Data Resolution Without Global Config

The HookDataProvider SHALL resolve spoof values using only profile-level settings.

#### Scenario: Profile Disabled Check

- **WHEN** resolving a spoof value for an app
- **AND** the assigned profile has `isEnabled = false`
- **THEN** `null` is returned (no spoofing)
- **AND** a debug log indicates the profile is disabled

#### Scenario: Type Enabled Check

- **WHEN** resolving a spoof value for an app
- **AND** the assigned profile has `isEnabled = true`
- **THEN** the profile's `isTypeEnabled(type)` is checked
- **AND** if the type is disabled in the profile, `null` is returned

#### Scenario: No Global Override

- **WHEN** resolving a spoof value
- **THEN** no GlobalSpoofConfig is consulted
- **AND** the profile is the single source of truth

---

### Requirement: Profile Name Validation

The profile name input SHALL enforce length constraints.

#### Scenario: Character Limit

- **WHEN** creating or editing a profile name
- **THEN** the input is limited to 12 characters maximum
- **AND** the user cannot enter more than 12 characters

#### Scenario: Character Counter Display

- **WHEN** entering a profile name
- **THEN** a counter shows current length vs maximum (e.g., "8/12")
