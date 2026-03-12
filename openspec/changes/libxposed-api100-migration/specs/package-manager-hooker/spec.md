## ADDED Requirements

### Requirement: Hook hasSystemFeature(String)

The module SHALL hook `ApplicationPackageManager.hasSystemFeature(String)` to return values consistent with the active device profile preset.

#### Scenario: NFC feature matches profile

- **WHEN** an app calls `pm.hasSystemFeature("android.hardware.nfc")` and the active preset has `hasNfc=true`
- **THEN** the hook returns `true`

#### Scenario: NFC feature absent in profile

- **WHEN** an app calls `pm.hasSystemFeature("android.hardware.nfc")` and the active preset has `hasNfc=false`
- **THEN** the hook returns `false`

### Requirement: Hook hasSystemFeature(String, int)

The module SHALL also hook the version-checking overload `hasSystemFeature(String, int)` with the same logic.

#### Scenario: Feature version check

- **WHEN** an app calls `pm.hasSystemFeature("android.hardware.camera.flash", 1)`
- **THEN** the hook applies the same identity-feature logic as the single-argument version

### Requirement: Only intercept identity-relevant features

The hook SHALL only modify results for features in the `IDENTITY_FEATURES` set (NFC, telephony, IMS, sensors, camera, WiFi, Bluetooth LE, eSIM). Non-identity features SHALL pass through the real value.

#### Scenario: Non-identity feature passthrough

- **WHEN** an app calls `pm.hasSystemFeature("android.hardware.touchscreen")`
- **THEN** the hook does NOT intercept — the real value is returned

### Requirement: No device profile = no interception

If no `DEVICE_PROFILE` spoof type is configured for the package, the hook SHALL return the real value for all features.

#### Scenario: No profile configured

- **WHEN** no device profile is set for `com.example.app`
- **THEN** all `hasSystemFeature()` calls return real values
