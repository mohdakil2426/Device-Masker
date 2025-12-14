## ADDED Requirements

### Requirement: Stack Trace Hiding

The module SHALL filter Xposed/LSPosed class references from stack traces to prevent detection.

#### Scenario: Thread.getStackTrace() Filter

- **WHEN** a target app calls `Thread.getStackTrace()`
- **THEN** all stack frames containing Xposed patterns are removed
- **AND** the filtered stack trace appears natural

#### Scenario: Throwable.getStackTrace() Filter

- **WHEN** a target app catches an exception and calls `getStackTrace()`
- **THEN** Xposed-related frames are filtered out

#### Scenario: Hidden Patterns

- **WHEN** filtering stack traces
- **THEN** the following patterns are removed:
  - `de.robv.android.xposed.*`
  - `io.github.lsposed.*`
  - `org.lsposed.lspd.*`
  - `com.highcapable.yukihookapi.*`
  - `EdHooker*`, `LSPHooker*`
  - `XposedBridge`, `XC_MethodHook`, `XposedHelpers`

---

### Requirement: Class Loading Detection Bypass

The module SHALL prevent apps from detecting Xposed through class loading.

#### Scenario: Class.forName() Block

- **WHEN** a target app calls `Class.forName("de.robv.android.xposed.XposedBridge")`
- **THEN** a `ClassNotFoundException` is thrown
- **AND** the real class is NOT returned

#### Scenario: ClassLoader.loadClass() Block

- **WHEN** a target app attempts to load Xposed classes via ClassLoader
- **THEN** a `ClassNotFoundException` is thrown

---

### Requirement: Native Library Detection Bypass

The module SHALL hide Xposed libraries from `/proc/maps` reading.

#### Scenario: /proc/self/maps Filtering

- **WHEN** a target app reads `/proc/self/maps`
- **THEN** lines containing `libxposed`, `liblspd`, `libedxposed`, `libwhale`, `libsandhook`, `libriru` are removed

---

### Requirement: Package Manager Detection Bypass

The module SHALL hide Xposed-related packages from PackageManager queries.

#### Scenario: getPackageInfo() Block

- **WHEN** a target app calls `PackageManager.getPackageInfo("de.robv.android.xposed.installer", ...)`
- **THEN** a `NameNotFoundException` is thrown

#### Scenario: Hidden Packages

- **WHEN** querying for packages
- **THEN** the following are hidden:
  - `de.robv.android.xposed.installer`
  - `io.github.lsposed.manager`
  - `org.meowcat.edxposed.manager`

---

### Requirement: Reflection Detection Bypass

The module SHALL hide method/field modifications from reflection inspection.

#### Scenario: Method.getModifiers() Masking

- **WHEN** a target app inspects a hooked method's modifiers via reflection
- **THEN** the original, unmodified modifiers are returned

---

### Requirement: Anti-Detection Loading Order

The anti-detection hooks SHALL load before any spoofing hooks to ensure protection.

#### Scenario: First Loader

- **WHEN** the module activates in a target process
- **THEN** AntiDetectHooker runs BEFORE DeviceHooker, NetworkHooker, etc.
- **AND** detection attempts during spoofing hook setup are blocked
