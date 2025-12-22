## MODIFIED Requirements

### Requirement: Spoof Configuration Storage
The system SHALL persist spoof configurations using **SpoofGroup** data model.

#### Scenario: Save group configuration
- **WHEN** user creates or modifies a spoof group
- **THEN** the group SHALL be serialized to JSON with key `"profiles"` for backward compatibility
- **AND** the group data SHALL include all enabled spoof types and their values

#### Scenario: Load existing configuration
- **WHEN** app launches with existing saved configuration
- **THEN** the system SHALL deserialize JSON `"profiles"` key into `List<SpoofGroup>`
- **AND** all previous profile data SHALL be available as groups

### Requirement: Group-Based App Assignment
The system SHALL associate apps with spoof groups (formerly profiles).

#### Scenario: App assigned to group
- **WHEN** user assigns an app to a spoof group
- **THEN** the app package name SHALL be stored in `SpoofGroup.assignedApps`
- **AND** the Xposed module SHALL apply that group's spoof values to the app
