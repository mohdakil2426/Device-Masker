## ADDED Requirements

### Requirement: AIDL interface reduced to diagnostics

The `IDeviceMaskerService.aidl` SHALL contain exactly 8 methods: 3 `oneway` reporting methods (`reportSpoofEvent`, `reportLog`, `reportPackageHooked`) and 5 read methods (`getSpoofEventCount`, `getHookedPackages`, `getLogs`, `clearDiagnostics`, `isAlive`).

#### Scenario: AIDL interface has no config methods

- **WHEN** the AIDL file is compiled
- **THEN** it contains zero config-related methods (no `writeConfig`, `readConfig`, `reloadConfig`, `isModuleEnabled`, `isAppEnabled`, `getSpoofValue`)

### Requirement: Hook reporting is oneway (non-blocking)

All methods called FROM hooks TO the diagnostics service (`reportSpoofEvent`, `reportLog`, `reportPackageHooked`) SHALL be declared `oneway` in the AIDL interface.

#### Scenario: oneway binder call from hook

- **WHEN** a hook callback calls `service.reportSpoofEvent("com.example.app", "IMEI")`
- **THEN** the call returns in ~5μs without waiting for the service to process it

### Requirement: DeviceMaskerService stripped to diagnostics

`DeviceMaskerService.kt` SHALL only maintain: `logs: ConcurrentLinkedDeque<String>`, `spoofCounts: ConcurrentHashMap<String, AtomicInteger>`, `hookedPackages: ConcurrentHashSet<String>`. All `config: AtomicReference<JsonConfig>` and `ConfigManager` references SHALL be removed.

#### Scenario: Service contains no config state

- **WHEN** DeviceMaskerService is instantiated in system_server
- **THEN** it has no `AtomicReference<JsonConfig>`, no `ConfigManager` reference, and no file I/O to `/data/misc/devicemasker/`

### Requirement: ServiceClient simplified for diagnostics reads

`ServiceClient.kt` in `:app` SHALL only contain methods for reading diagnostics: `getSpoofEventCount()`, `getHookedPackages()`, `getLogs()`, `clearDiagnostics()`, `isAlive()`, and `connect()/disconnect()`.

#### Scenario: No config write methods in ServiceClient

- **WHEN** ServiceClient is used from DiagnosticsViewModel
- **THEN** there are no `writeConfig()`, `readConfig()`, `isModuleEnabled()`, `isAppEnabled()`, or `getSpoofValue()` methods

### Requirement: Diagnostics service failure is non-fatal

If the AIDL diagnostics service fails to initialize or is unavailable, hooks SHALL still function via RemotePreferences. The diagnostics screen shows "Service unavailable" but spoofing continues.

#### Scenario: Service unavailable

- **WHEN** SystemServiceHooker fails to register DeviceMaskerService at boot
- **THEN** hooks read config from RemotePreferences (unaffected)
- **THEN** DiagnosticsViewModel shows `connectionState = FAILED` and empty statistics

### Requirement: Hook event reporting in hookers

Each hooker's hook callback SHALL call `XposedEntry.instance.reportSpoofEvent(pkg, spoofTypeName)` after returning the spoofed value. This call is fire-and-forget (oneway).

#### Scenario: IMEI spoof reported

- **WHEN** `GetImeiHooker.before()` returns a spoofed IMEI via `callback.returnAndSkip()`
- **THEN** `reportSpoofEvent("com.example.app", "IMEI")` is called (non-blocking)
