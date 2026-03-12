## ADDED Requirements

### Requirement: deoptimize() called after every hook registration

For every method hooked via `xi.hook(method, HookerClass::class.java)`, the module SHALL immediately call `xi.deoptimize(method)` to prevent ART from inlining the hooked method.

#### Scenario: TelephonyManager.getImei() deoptimized

- **WHEN** `DeviceHooker.hook()` registers a hook on `TelephonyManager.getImei()`
- **THEN** `xi.deoptimize(getImeiMethod)` is called immediately after `xi.hook()`
- **THEN** the IMEI hook fires even in apps where ART has JIT-compiled the getImei() call path

#### Scenario: deoptimize failure is non-fatal

- **WHEN** `xi.deoptimize(method)` returns false or throws
- **THEN** a warning is logged ("Failed to deoptimize {class}.{method}") but the hook remains registered

### Requirement: DeoptimizeManager utility

A `DeoptimizeManager` object SHALL provide `deoptimizeWithCallers(xi, target, callers)` and `deoptimizeAll(xi, methods)` helper methods for batch deoptimization.

#### Scenario: Batch deoptimization of Build field accessors

- **WHEN** SystemHooker hooks all Build.\* field accessors
- **THEN** `DeoptimizeManager.deoptimizeAll()` is called with all hooked methods

### Requirement: Critical deoptimization targets

The following methods SHALL always be deoptimized when hooked: all `TelephonyManager` getters, all `Build.getSerial()`/`Build.*` accessors, all `Settings.Secure.getString()`, all `SubscriptionInfo` getters, `WifiInfo.getMacAddress()`, `NetworkInterface.getHardwareAddress()`.

#### Scenario: Banking app reads IMEI via JIT-compiled path

- **WHEN** a banking app calls `tm.getImei()` in a JIT-compiled method
- **THEN** the deoptimized method forces dispatch through the method table
- **THEN** the IMEI hook callback fires and returns the spoofed value
