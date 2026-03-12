## ADDED Requirements

### Requirement: Hook SubscriptionInfo getters

The module SHALL hook `SubscriptionInfo.getIccId()`, `.getNumber()`, `.getDisplayName()`, `.getCountryIso()` to return spoofed values matching the configured carrier.

#### Scenario: Banking app reads ICCID via SubscriptionManager

- **WHEN** an app calls `SubscriptionManager.getActiveSubscriptionInfoList()` and then `info.getIccId()`
- **THEN** the hooked `getIccId()` returns the spoofed ICCID from RemotePreferences

#### Scenario: SubscriptionInfo class not found

- **WHEN** `SubscriptionInfo` class does not exist in the target process classloader
- **THEN** SubscriptionInfo hooks are skipped (no crash) and TelephonyManager hooks still function

### Requirement: Hook getActiveSubscriptionInfoCount

The module SHALL hook `SubscriptionManager.getActiveSubscriptionInfoCount()` to return the value matching the device profile's `simCount` (default: 1).

#### Scenario: Single-SIM device profile

- **WHEN** a device profile with `simCount=1` is active
- **THEN** `getActiveSubscriptionInfoCount()` returns 1

### Requirement: Hook SubscriptionInfo carrier name

The module SHALL hook `SubscriptionInfo.getDisplayName()` to return the spoofed `CARRIER_NAME` from RemotePreferences.

#### Scenario: Carrier name matches SIM config

- **WHEN** user has configured carrier "Jio" for an app
- **THEN** `SubscriptionInfo.getDisplayName()` returns the Jio carrier name

### Requirement: Hook SubscriptionInfo country ISO

The module SHALL hook `SubscriptionInfo.getCountryIso()` to return the spoofed `SIM_COUNTRY_ISO` in lowercase.

#### Scenario: Country ISO matches carrier

- **WHEN** carrier MCC/MNC is 404-40 (India/Jio)
- **THEN** `SubscriptionInfo.getCountryIso()` returns "in"

### Requirement: Each SubscriptionInfo hook individually wrapped

Each `SubscriptionInfo` method hook SHALL be registered in its own `safeHook()` block so one missing method does not prevent other hooks.

#### Scenario: getNumber() missing on API 29+

- **WHEN** `getNumber()` is deprecated/removed on the device's Android version
- **THEN** only the `getNumber()` hook fails; `getIccId()`, `getDisplayName()`, `getCountryIso()` hooks still register successfully
