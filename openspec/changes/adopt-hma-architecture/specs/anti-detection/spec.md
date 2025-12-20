# anti-detection Spec Delta

## MODIFIED Requirements

### Requirement: Hook Execution Context

The anti-detection hooks SHALL run in system_server process.

#### Scenario: System Server Hooks

- **WHEN** anti-detection hooks are loaded
- **THEN** They execute in system_server process context
- **AND** They have access to system-level APIs
- **AND** They can intercept system services directly

#### Scenario: Target App Hooks

- **WHEN** target app-specific hooks are needed
- **THEN** They run via `loadApp { }` in target process
- **AND** They read config from in-memory service via internal IPC
- **AND** They don't need file access for configuration

---

### Requirement: Anti-Detection Loading Order

Anti-detection hooks SHALL be loaded first in the xposed module.

#### Scenario: Load Order in DeviceMaskerService

- **WHEN** `installHooks()` is called
- **THEN** AntiDetectHooker is loaded before all other hookers
- **AND** Anti-detection is active before any spoofing occurs

---

### Requirement: Recursion Prevention

The anti-detection hooks SHALL prevent infinite recursion.

#### Scenario: Class.forName Recursion Guard

- **WHEN** Class.forName hook executes
- **THEN** ThreadLocal<Boolean> guard prevents re-entry
- **AND** Nested Class.forName calls are NOT intercepted
- **AND** No StackOverflowError can occur

#### Scenario: Safe Logging

- **WHEN** logging is needed in critical hooks
- **THEN** Use Logcat helper from xposed module
- **AND** NO logging inside Class.forName hook callback
- **AND** NO logging inside ClassLoader.loadClass hook callback

---

### Requirement: Config Access in Anti-Detection

Anti-detection hooks SHALL access config from in-memory service.

#### Scenario: Hidden Patterns Access

- **WHEN** AntiDetectHooker needs list of patterns to hide
- **THEN** It reads from `DeviceMaskerService.instance?.config`
- **AND** Patterns are immediately available (no file I/O)
- **AND** Updates via AIDL are reflected instantly

#### Scenario: Service Not Available

- **WHEN** DeviceMaskerService.instance is null
- **THEN** Anti-detection uses hardcoded default patterns
- **AND** Module still provides basic protection
- **AND** Error is logged once (not on every hook call)

---

## ADDED Requirements

### Requirement: System-Level Anti-Detection Capabilities

The module SHALL provide enhanced anti-detection capabilities by running in system_server.

#### Scenario: Process List Interception

- **WHEN** apps query running processes
- **THEN** Module can intercept at system level
- **AND** Xposed-related processes can be hidden

#### Scenario: Package Manager Interception

- **WHEN** apps query installed packages
- **THEN** Module can intercept PackageManagerService
- **AND** LSPosed Manager can be hidden if configured

---

### Requirement: Logcat Helper

The xposed module SHALL provide safe logging utilities.

#### Scenario: Log Levels

- **WHEN** logging is needed
- **THEN** Logcat.logD(), logI(), logW(), logE() are available
- **AND** Log level can be configured via service config
- **AND** Detailed logging can be disabled in production

#### Scenario: Log Collection

- **WHEN** UI requests logs via AIDL
- **THEN** Service returns collected log entries
- **AND** Logs can be cleared via AIDL
- **AND** Log buffer has configurable size limit
