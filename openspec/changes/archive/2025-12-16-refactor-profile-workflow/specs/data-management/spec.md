## MODIFIED Requirements

### Requirement: Spoof Profile Management

The module SHALL support named profiles containing complete sets of spoofed values AND assigned apps.

#### Scenario: Create Profile

- **WHEN** a user creates a new profile
- **THEN** the profile is assigned a unique ID
- **AND** values are copied from GlobalSpoofConfig.defaultValues
- **AND** assignedApps is initialized as empty set
- **AND** the profile is persisted to DataStore

#### Scenario: Edit Profile Values

- **WHEN** a user edits a profile value
- **THEN** the change is validated for format correctness
- **AND** the updated profile is persisted
- **AND** active hooks reflect the new value (on next app launch)
- **AND** other profiles are NOT affected

#### Scenario: Delete Profile

- **WHEN** a user deletes a profile
- **THEN** the profile is removed from storage
- **AND** apps that were assigned to this profile become unassigned
- **AND** unassigned apps use the default profile

#### Scenario: Default Profile

- **WHEN** no specific profile has an app assigned
- **THEN** the default profile values are used for that app
- **AND** exactly one profile can be marked as default

#### Scenario: Assigned Apps

- **WHEN** a profile has apps in its assignedApps set
- **THEN** those apps use this profile's spoofed values
- **AND** each app can only be in ONE profile's assignedApps

---

### Requirement: Per-App Configuration

The module SHALL determine which profile to use for each app based on profile assignments.

#### Scenario: App in Profile

- **WHEN** an app's package name is in a profile's assignedApps
- **THEN** that profile's values are used for hooks in that app

#### Scenario: App Not in Any Profile

- **WHEN** an app is not in any profile's assignedApps
- **THEN** the default profile's values are used
- **AND** the app is considered "unassigned"

#### Scenario: Assign App to Profile

- **WHEN** a user assigns an app to a profile
- **THEN** the app's package name is added to profile.assignedApps
- **AND** if the app was in another profile, it is removed from that profile first

#### Scenario: Remove App from Profile

- **WHEN** a user removes an app from a profile
- **THEN** the app's package name is removed from profile.assignedApps
- **AND** the app will use the default profile on next launch

---

## ADDED Requirements

### Requirement: Global Spoof Configuration

The module SHALL maintain a global configuration that controls master switches and default values.

#### Scenario: Global Type Enabled

- **WHEN** a SpoofType is in GlobalSpoofConfig.enabledTypes
- **THEN** profiles can use their own values for that type
- **AND** hooks will apply spoofing for that type

#### Scenario: Global Type Disabled

- **WHEN** a SpoofType is NOT in GlobalSpoofConfig.enabledTypes
- **THEN** NO profile can spoof that type
- **AND** hooks return real device values for that type
- **AND** the type appears grayed out in profile detail

#### Scenario: Global Default Values

- **WHEN** a new profile is created
- **THEN** the profile's values are copied from GlobalSpoofConfig.defaultValues
- **AND** the profile's values become independent from global defaults thereafter

#### Scenario: Modify Global Default

- **WHEN** a user changes a global default value
- **THEN** the new value is stored in GlobalSpoofConfig.defaultValues
- **AND** existing profiles are NOT modified
- **AND** only new profiles will use the new default

#### Scenario: Toggle Global Type

- **WHEN** a user toggles a type enabled/disabled
- **THEN** GlobalSpoofConfig.enabledTypes is updated
- **AND** the change takes effect on next hook execution
- **AND** all profiles respect this master switch

---

### Requirement: Profile-App Lookup

The module SHALL efficiently resolve which profile to use for a given app.

#### Scenario: Lookup by Package Name

- **WHEN** a hook needs to get spoofed values for an app
- **THEN** the module scans all profiles for matching packageName in assignedApps
- **AND** returns the matching profile, or default if none found

#### Scenario: Caching

- **WHEN** performing multiple lookups
- **THEN** the profile-to-app mapping may be cached for performance
- **AND** the cache is invalidated when profile assignments change

---

### Requirement: Data Migration

The module SHALL migrate existing data to the new profile-centric model.

#### Scenario: First Launch After Update

- **WHEN** the app launches and migration has not been performed
- **THEN** existing AppConfig.profileId references are migrated
- **AND** each app's packageName is added to the referenced profile's assignedApps
- **AND** GlobalSpoofConfig is initialized with all types enabled

#### Scenario: Migration Idempotency

- **WHEN** migration runs multiple times
- **THEN** it produces the same result
- **AND** data is not duplicated or corrupted

#### Scenario: Fresh Install

- **WHEN** the app is installed fresh (no existing data)
- **THEN** migration completes without error
- **AND** GlobalSpoofConfig is initialized with default values
- **AND** a default profile is created
