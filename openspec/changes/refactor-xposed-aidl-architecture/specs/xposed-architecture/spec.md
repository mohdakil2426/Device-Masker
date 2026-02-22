# xposed-architecture Specification Delta

## ADDED Requirements

### Requirement: System-Wide AIDL Service Architecture

The Xposed module SHALL use a system-wide AIDL service architecture for configuration delivery and hook management.

The architecture SHALL consist of:
1. **DeviceMaskerService**: Singleton AIDL service running in `system_server`
2. **ServiceBridge**: ContentProvider for service discovery
3. **SystemServiceHooker**: Boot-time hook to initialize service
4. **ServiceClient**: UI-side client for AIDL communication

#### Scenario: Service Initialization at Boot

- **WHEN** the device boots with LSPosed enabled and Device Masker module active
- **THEN** SystemServiceHooker hooks `ActivityManagerService.systemReady()`
- **AND** DeviceMaskerService initializes as a singleton
- **AND** ServiceBridge ContentProvider is registered
- **AND** configuration is loaded from `/data/misc/devicemasker/config.json`

#### Scenario: Service Availability Check

- **WHEN** the UI app is launched
- **THEN** ServiceClient connects via ContentProvider.call("getService")
- **AND** returns connected status if binder is received
- **AND** all AIDL methods are available for invocation

---

### Requirement: AIDL Interface Contract

The `IDeviceMaskerService.aidl` interface SHALL provide the following methods:

**Configuration Methods**:
- `writeConfig(String json)`: Persist new configuration
- `readConfig(): String`: Retrieve current configuration
- `reloadConfig()`: Force reload from disk

**Query Methods**:
- `isModuleEnabled(): boolean`: Check global module status
- `isAppEnabled(String packageName): boolean`: Check per-app status
- `getSpoofValue(String packageName, String key): String`: Get spoof value

**Statistics Methods**:
- `incrementFilterCount(String packageName)`: Track successful spoofs
- `getFilterCount(String packageName): int`: Get spoof count for app
- `getHookedAppCount(): int`: Get total hooked apps

**Logging Methods**:
- `log(String tag, String message, int level)`: Add log entry
- `getLogs(int maxCount): List<String>`: Retrieve recent logs
- `clearLogs()`: Clear log buffer

**Control Methods**:
- `isServiceAlive(): boolean`: Health check
- `getServiceVersion(): String`: Get service version
- `getServiceUptime(): long`: Get uptime in milliseconds

#### Scenario: Configuration Write

- **WHEN** UI calls `service.writeConfig(jsonString)`
- **THEN** the service parses the JSON
- **AND** updates the in-memory AtomicReference<JsonConfig>
- **AND** writes atomically to `/data/misc/devicemasker/config.json`
- **AND** returns without error

#### Scenario: Configuration Query

- **WHEN** a hooker calls `service.getSpoofValue(packageName, "imei")`
- **THEN** the service returns the configured IMEI for that package
- **OR** returns null if not configured

#### Scenario: Filter Count Tracking

- **WHEN** a hook successfully spoofs a value
- **THEN** it calls `service.incrementFilterCount(packageName)`
- **AND** the count is stored in ConcurrentHashMap
- **AND** UI can retrieve via `service.getFilterCount(packageName)`

---

### Requirement: Real-Time Configuration Updates

Configuration changes made via the UI SHALL apply immediately without requiring target app restart.

#### Scenario: IMEI Change Without Restart

- **WHEN** user changes IMEI value in UI
- **THEN** the change is pushed via AIDL to DeviceMaskerService
- **AND** the next API call in the target app returns the new IMEI
- **AND** no app restart is required
- **AND** change propagation completes in < 100ms

#### Scenario: App Enable/Disable Without Restart

- **WHEN** user toggles an app's enabled status
- **THEN** the change applies immediately
- **AND** subsequent hook calls check `service.isAppEnabled()`
- **AND** hooks are bypassed for disabled apps

---

### Requirement: Thread-Safe Service Implementation

DeviceMaskerService SHALL use thread-safe data structures for all mutable state.

- Configuration: `AtomicReference<JsonConfig>`
- Filter counts: `ConcurrentHashMap<String, AtomicInteger>`
- Logs: `ConcurrentLinkedDeque<String>`
- Hooked apps: `ConcurrentHashMap.newKeySet<String>()`

#### Scenario: Concurrent Config Updates

- **WHEN** multiple binder calls modify config simultaneously
- **THEN** all updates are applied correctly
- **AND** no data corruption occurs
- **AND** the final state reflects the last write

#### Scenario: Concurrent Hook Queries

- **WHEN** 100+ apps query `getSpoofValue()` simultaneously
- **THEN** all queries return correct values
- **AND** no race conditions occur
- **AND** response time remains < 10ms per query

---

### Requirement: Atomic Configuration Persistence

Configuration writes SHALL use atomic file operations to prevent corruption.

#### Scenario: Atomic Write Process

- **WHEN** `ConfigManager.saveConfig()` is called
- **THEN** config is written to `config.json.tmp`
- **AND** existing `config.json` is backed up to `config.json.bak`
- **AND** `config.json.tmp` is renamed to `config.json`
- **AND** operation is atomic (no partial writes)

#### Scenario: Recovery from Corruption

- **WHEN** `config.json` is corrupted or missing
- **THEN** ConfigManager attempts to load `config.json.bak`
- **OR** creates default configuration
- **AND** logs the recovery action

---

### Requirement: LSPosed System Framework Scope

The module SHALL be configured for System Framework (android) scope only.

#### Scenario: LSPosed Configuration

- **WHEN** user configures Device Masker in LSPosed
- **THEN** only "System Framework (android)" is selected in scope
- **AND** module loads hooks via `loadSystem { }` block
- **AND** service initializes in system_server process

#### Scenario: Universal App Coverage

- **WHEN** any app is launched after boot
- **THEN** `loadApp { }` block runs for that app
- **AND** hookers are loaded if app is enabled in config
- **AND** no manual per-app LSPosed scope selection needed

---

### Requirement: Service-Aware Hooker Base Class

BaseSpoofHooker SHALL provide service access and common utilities to all hookers.

Constructor parameters:
- `service: IDeviceMaskerService` - AIDL service reference
- `targetPackage: String` - Current app package name

Utility methods:
- `getSpoofValue(key: String): String?` - Query service for value
- `incrementFilterCount()` - Report successful spoof
- `log(message: String)` - Log to service
- `logWarn(message, throwable?)` - Warning log
- `logError(message, throwable?)` - Error log

#### Scenario: Hooker Value Query

- **WHEN** DeviceHooker needs IMEI spoof value
- **THEN** it calls `getSpoofValue(SharedPrefsKeys.SPOOF_IMEI)`
- **AND** receives the configured value or null
- **AND** applies the hook if value is not null

#### Scenario: Hooker Logging

- **WHEN** a hooker logs a message via `log("Spoofed IMEI")`
- **THEN** the message is sent to DualLog (local)
- **AND** the message is sent to DeviceMaskerService (centralized)
- **AND** UI can retrieve via `service.getLogs()`

---

### Requirement: Graceful Degradation

The hooking system SHALL continue operation if service is unavailable.

#### Scenario: Service Unavailable at App Start

- **WHEN** loadApp runs but service is not initialized
- **THEN** hookers are not loaded for that app
- **AND** a warning is logged
- **AND** no crash occurs in target app

#### Scenario: Service Crash During Operation

- **WHEN** service crashes mid-operation
- **THEN** binder calls return null/default values
- **AND** hooks continue with cached values (if any)
- **AND** service restarts on next boot

---

### Requirement: Centralized Diagnostics

The service SHALL provide diagnostic information for troubleshooting.

#### Scenario: Service Health Check

- **WHEN** UI calls `service.isServiceAlive()`
- **THEN** returns `true` if service is functioning
- **AND** UI displays "Service Connected" status

#### Scenario: Uptime Tracking

- **WHEN** UI queries `service.getServiceUptime()`
- **THEN** returns milliseconds since service initialization
- **AND** UI displays human-readable uptime

#### Scenario: Hooked App Enumeration

- **WHEN** UI queries `service.getHookedAppCount()`
- **THEN** returns count of apps with active hooks
- **AND** UI displays in Diagnostics screen

---

## MODIFIED Requirements

### Requirement: Hook Entry Point

The HookEntry object SHALL serve as the main entry point for YukiHookAPI initialization.

**Previous behavior**: Loads hookers directly per-app via loadApp

**New behavior**: Loads SystemServiceHooker via loadSystem, then loads spoofing hookers per-app with service reference

#### Scenario: Hook Loading Order

- **WHEN** a target application is launched with the module enabled
- **THEN** loadSystem initializes DeviceMaskerService (if system_server)
- **THEN** loadApp queries service for app enabled status
- **THEN** AntiDetectHooker loads FIRST with service reference
- **THEN** all spoofing hookers load with service reference
- **AND** debug logs are emitted (in debug builds)

#### Scenario: Selective Loading

- **WHEN** an app is not in the enabled apps list (checked via service)
- **THEN** no hooks are applied to that app
- **AND** no performance overhead is incurred

---

## REMOVED Requirements

### Requirement: XSharedPreferences-based Configuration

**Reason**: Replaced by AIDL service architecture

**Migration**: All configuration reads now go through `DeviceMaskerService.getSpoofValue()` instead of `XSharedPreferences.getString()`

---

### Requirement: PrefsHelper Utility Class

**Reason**: Replaced by BaseSpoofHooker service utilities

**Migration**: Hookers extend `BaseSpoofHooker(service, pkg)` and use `getSpoofValue()` method

---
