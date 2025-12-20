# data-management Spec Delta

## REPLACED Requirements

### Requirement: Value Persistence

The module SHALL persist all configuration using **plain JSON files** following the HMA-OSS pattern.

#### Scenario: App Module Storage

- **WHEN** the UI saves configuration
- **THEN** config is serialized to JSON using kotlinx-serialization
- **AND** JSON is written to `/data/data/[package]/files/config.json`
- **AND** JSON is sent to xposed service via AIDL `writeConfig()`

#### Scenario: Xposed Module Storage

- **WHEN** xposed service saves configuration
- **THEN** JSON is written to `/data/system/devicemasker/config.json`
- **AND** In-memory `JsonConfig` object is updated
- **AND** All hooks immediately see updated config

#### Scenario: Storage Format

- **WHEN** config file is examined
- **THEN** Format is plain JSON text (not Protobuf, not XML)
- **AND** File is human-readable
- **AND** Uses kotlinx.serialization.json for encode/decode

---

### Requirement: Configuration Model

The module SHALL use a unified `JsonConfig` model in the `:common` module.

#### Scenario: JsonConfig Structure

- **WHEN** JsonConfig is serialized
- **THEN** Contains `configVersion` integer
- **AND** Contains `moduleEnabled` boolean
- **AND** Contains `profiles` map of SpoofProfile objects
- **AND** Contains `scope` map of per-app configurations

#### Scenario: Model Serialization

- **WHEN** JsonConfig.toString() is called
- **THEN** Returns valid JSON string
- **AND** All nested objects are properly serialized

#### Scenario: Model Parsing

- **WHEN** JsonConfig.parse(json) is called
- **THEN** Returns parsed JsonConfig object
- **AND** Handles missing fields with defaults
- **AND** Ignores unknown keys for forward compatibility

---

### Requirement: ConfigManager (App Module)

The app module SHALL manage local configuration via `ConfigManager`.

#### Scenario: Configuration Loading

- **WHEN** ConfigManager.init() is called
- **THEN** Config file is read from disk if exists
- **AND** JsonConfig object is parsed into memory
- **AND** Default config is created if file doesn't exist

#### Scenario: Configuration Saving

- **WHEN** ConfigManager.saveConfig() is called
- **THEN** In-memory config is serialized to JSON
- **AND** JSON is written to local config file
- **AND** JSON is sent to xposed service via ServiceClient.writeConfig()

#### Scenario: Profile Management

- **WHEN** a profile is created/updated/deleted
- **THEN** In-memory config is updated
- **AND** saveConfig() is called automatically

---

## REMOVED Requirements

### Removed: Jetpack DataStore

- DataStore SHALL NOT be used anywhere in the application
- All DataStore dependencies SHALL be removed from build files
- All DataStore files SHALL be migrated and deleted

### Removed: XSharedPreferences

- XSharedPreferences SHALL NOT be used for configuration access
- SharedPreferences with MODE_WORLD_READABLE SHALL NOT be used
- All cross-process communication uses AIDL

### Removed: PreferenceSyncManager

- No sync bridge is needed between UI and hooks
- AIDL provides instant synchronization

---

## ADDED Requirements

### Requirement: In-Memory Config Access for Hooks

All hookers SHALL read configuration from the in-memory service object.

#### Scenario: Hook Config Access

- **WHEN** a hooker needs to read spoof values
- **THEN** It accesses `DeviceMaskerService.instance?.config`
- **AND** No file I/O occurs during hook execution
- **AND** Access is immediate (no disk read, no network)

#### Scenario: Config Change Propagation

- **WHEN** UI sends updated config via AIDL
- **THEN** Service updates in-memory config object
- **AND** All hookers immediately see new values
- **AND** No restart or reload is required

---

### Requirement: Data Migration

The module SHALL migrate data from old DataStore format on upgrade.

#### Scenario: Migration Detection

- **WHEN** app starts after upgrade
- **THEN** Old DataStore files are detected if present
- **AND** Migration is triggered before ConfigManager.init()

#### Scenario: Migration Execution

- **WHEN** migration runs
- **THEN** Old profiles are read from DataStore
- **AND** Profiles are converted to JsonConfig format
- **AND** New config.json file is created
- **AND** Old DataStore files are deleted

#### Scenario: Fresh Install

- **WHEN** app is freshly installed (no old data)
- **THEN** Migration is skipped
- **AND** Default config is created
