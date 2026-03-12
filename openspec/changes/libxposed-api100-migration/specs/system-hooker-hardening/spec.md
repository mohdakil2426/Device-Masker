## ADDED Requirements

### Requirement: Hook Build class initializer

The module SHALL hook `android.os.Build`'s class initializer (`<clinit>`) using `xi.hookClassInitializer()` to set spoofed field values before ANY app code reads them.

#### Scenario: Build fields set before Application.onCreate

- **WHEN** a target app's `Application.onCreate()` reads `Build.MODEL`
- **THEN** the class initializer hook has already set `Build.MODEL` to the spoofed value

#### Scenario: Class already initialized fallback

- **WHEN** `Build` class is already initialized before the module loads (common case)
- **THEN** the module falls back to direct field mutation via reflection as a secondary approach

### Requirement: Build.TIME spoofed

`Build.TIME` SHALL be set to the preset's `buildTime` value (epoch millis) to match the device model's expected build date.

#### Scenario: Pixel 9 Pro build time

- **WHEN** the active preset is Pixel 9 Pro with `buildTime=1728100000000L`
- **THEN** `Build.TIME` returns `1728100000000L` (October 2024)

### Requirement: Build.VERSION.SECURITY_PATCH spoofed

`Build.VERSION.SECURITY_PATCH` SHALL be set to the preset's `securityPatch` string.

#### Scenario: Security patch matches preset

- **WHEN** the active preset has `securityPatch="2024-10-05"`
- **THEN** `Build.VERSION.SECURITY_PATCH` returns `"2024-10-05"`

### Requirement: Build.SUPPORTED_ABIS spoofed

`Build.SUPPORTED_ABIS` SHALL be set to the preset's `supportedAbis` array.

#### Scenario: ARM64 ABIs

- **WHEN** the active preset has `supportedAbis=["arm64-v8a", "armeabi-v7a", "armeabi"]`
- **THEN** `Build.SUPPORTED_ABIS` returns that array

### Requirement: Build.ID and Build.TAGS spoofed

`Build.ID` SHALL be set to the preset's `buildId`. `Build.TAGS` SHALL always be set to `"release-keys"`. `Build.TYPE` SHALL always be set to `"user"`.

#### Scenario: Release-keys and user type

- **WHEN** the SystemHooker is active
- **THEN** `Build.TAGS` is `"release-keys"` and `Build.TYPE` is `"user"` regardless of the real device

### Requirement: Each Build field mutation individually wrapped

Each `Build.*` field mutation SHALL be wrapped in its own `safeHook()` block to prevent one failure from blocking others.

#### Scenario: SECURITY_PATCH field inaccessible

- **WHEN** `Build.VERSION.SECURITY_PATCH` field cannot be made accessible via reflection
- **THEN** only that field fails; `Build.TIME`, `Build.ID`, `Build.TAGS` still get set
