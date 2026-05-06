# :common Module Guide

Shared contracts, data models, identity generators, preference key builder, and diagnostics schema. No Android UI dependencies. No framework-specific code (constants like `NETWORK_TYPE_LTE=13` are hardcoded).

## Module Structure

```
common/src/main/
├── kotlin/.../common/
│   ├── models/             Carrier (~110 real), SIMConfig, DeviceHardwareConfig, LocationConfig, Country (16)
│   ├── generators/         IMEI, IMSI, ICCID, MAC, Serial, UUID, PhoneNumber, SIM, DeviceHardware, Fingerprint
│   ├── diagnostics/        DiagnosticEvent (22 fields, 31 types), DiagnosticRedactor
│   └── (root)              JsonConfig, AppConfig, SpoofGroup, SpoofType, DeviceIdentifier,
│                             DeviceProfilePreset, DevicePersona, PersonaGenerator, SharedPrefsKeys,
│                             Constants, Utils, NetworkTypeMapper, SecureRandomUtils
│
└── aidl/                   IDeviceMaskerService (diagnostics-only: 3 oneway + 5 sync methods)
```

## Source Of Truth Rules

- `JsonConfig.appConfigs` is canonical for per-app scope. `SpoofGroup.assignedApps` is legacy/display-only.
- `SharedPrefsKeys` is the ONLY place to build RemotePreferences key strings.
- Generators live here and run at config time, never at runtime in `:xposed`.
- All generators use `SecureRandom` for cryptographic-quality randomness.

## Data Model Hierarchy

```
JsonConfig (root, @Serializable)
├── version: Int (CONFIG_VERSION = 1)
├── isModuleEnabled: Boolean
├── groups: Map<String, SpoofGroup>           // JSON key: "profiles"
│   └── SpoofGroup
│       ├── id, name, description, isEnabled, isDefault
│       ├── identifiers: Map<SpoofType, DeviceIdentifier>
│       ├── assignedApps: Set<String>         // LEGACY/display only
│       ├── personaSeed, personaGeneratedAt
│       └── selectedCarrierMccMnc
└── appConfigs: Map<String, AppConfig>        // CANONICAL per-app scope
    └── AppConfig
        ├── packageName
        ├── groupId?                          // JSON key: "profileId"
        └── isEnabled
```

Migration: `withDerivedAppConfigsFromAssignedApps()` moves legacy group assignments into `appConfigs`.

## SpoofType Enum (24 values)

| Category | Types | CorrelationGroup |
|----------|-------|-----------------|
| DEVICE | IMEI, IMSI, SERIAL, ICCID, PHONE_NUMBER, SIM_COUNTRY_ISO, SIM_OPERATOR_NAME, NETWORK_COUNTRY_ISO | DEVICE_HARDWARE / SIM_CARD |
| NETWORK | NETWORK_OPERATOR, WIFI_MAC, BLUETOOTH_MAC, WIFI_SSID, WIFI_BSSID, CARRIER_NAME, CARRIER_MCC_MNC | SIM_CARD / DEVICE_HARDWARE |
| ADVERTISING | ANDROID_ID, GSF_ID, ADVERTISING_ID, MEDIA_DRM_ID | NONE |
| SYSTEM | DEVICE_PROFILE | NONE |
| LOCATION | LOCATION_LATITUDE, LOCATION_LONGITUDE, TIMEZONE, LOCALE | LOCATION |

## Correlation Groups

Values that MUST be generated together to avoid detection:
- **SIM_CARD**: IMSI, ICCID, carrier name, MCC/MNC, phone number, country ISOs — all from same `Carrier`
- **DEVICE_HARDWARE**: IMEI, serial, WiFi MAC — all from same `DeviceProfilePreset`
- **LOCATION**: Timezone, locale, GPS coordinates — all from same `Country`
- **NONE**: Independent values (Android ID, advertising ID, Bluetooth MAC, etc.)

## Generators

| Generator | Output | Algorithm |
|-----------|--------|-----------|
| `IMEIGenerator` | 15-digit IMEI | TAC (8) + Serial (6) + Luhn check (1). 60+ real TAC prefixes. |
| `IMSIGenerator` | 15-digit IMSI | MCC/MNC + MSIN. 28 real carrier combos. Location-aware. |
| `ICCIDGenerator` | 19-digit ICCID | MII(89) + Country + Issuer + Account + Luhn. Carrier-specific. |
| `MACGenerator` | `XX:XX:XX:XX:XX:XX` | Real OUIs for Samsung/Apple/Google/Xiaomi/etc. Unicast + locally-administered bits. |
| `SerialGenerator` | Manufacturer-specific | Samsung: `R`+2+year+8. Pixel: 16 hex. Xiaomi: 12-16 alphanumeric. |
| `UUIDGenerator` | Android ID, GAID, GSF ID, Media DRM ID, Instance ID | `SecureRandom` hex/UUID |
| `PhoneNumberGenerator` | `+{code}{number}` | 150+ US area codes, NANP rules |
| `SIMGenerator` | Complete `SIMConfig` | Orchestrates IMSI+ICCID+Phone+Carrier |
| `DeviceHardwareGenerator` | Complete `DeviceHardwareConfig` | Orchestrates IMEI+Serial+WiFiMAC+BTMAC |
| `FingerprintGenerator` | Build fingerprint + Build.* properties | 21 device configs |

## PersonaGenerator — Deterministic Coherent Identity

`PersonaGenerator.generate(group, packageName)` builds a complete `DevicePersona` using SHA-256 seeded derivation from `rootSeed`. Priority: explicit override > stored value > deterministic derivation.

`DevicePersona` contains: `HardwarePersona`, `List<SubscriptionPersona>`, `LocationPersona`, `NetworkEnvironmentPersona`, `TrackingPersona`, `BrowserPersona`.

`materializeGroup()` writes persona values back into a `SpoofGroup`'s identifier map.

## Sub-Models

| Model | Key Fields | Validation |
|-------|-----------|------------|
| `Carrier` | name, mccMnc, countryCode, countryIso, iccidIssuerCode | ~110 real carriers across 16 countries |
| `SIMConfig` | carrier, imsi, iccid, phoneNumber, simCountryIso, networkCountryIso | IMSI must start with carrier MCC/MNC. Constructor throws on mismatch. |
| `DeviceHardwareConfig` | deviceProfile, imei, serial, wifiMAC, bluetoothMAC | IMEI must be 15 digits |
| `LocationConfig` | country, timezone, locale, latitude, longitude | GPS bounds for 16 countries, multiple cities each |
| `Country` | iso, name, emoji, phoneCode | 16 countries |
| `DeviceProfilePreset` | id, name, brand, model, device, fingerprint, tacPrefixes, simCount | 10 presets (Pixel, Samsung, OnePlus, Xiaomi, Sony, Nothing) |

## SharedPrefsKeys — Key Patterns

| Pattern | Example |
|---------|---------|
| `module_enabled` | global toggle |
| `app_enabled_{sanitizedPkg}` | `app_enabled_com_example_app` |
| `spoof_enabled_{sanitizedPkg}_{TYPE}` | `spoof_enabled_com_example_app_IMEI` |
| `spoof_{sanitizedPkg}_{TYPE}` | `spoof_com_example_app_ANDROID_ID` |
| `persona_blob_{sanitizedPkg}` | persona JSON blob |
| `persona_version_{sanitizedPkg}` | persona version counter |

Package names: `.` replaced with `_`. Validation regex in `isValidKey()`.

## Diagnostics Schema

`DiagnosticEvent`: 22 fields, 31 event types, 7 sources, 7 severity levels. Event ID format: `evt_{wallMillis}_{sequence6digits}`.

`DiagnosticRedactor`: Two modes (REDACTED/UNREDACTED). Redacts IMEI, IMSI, ICCID, MAC, Android ID, phone numbers, GPS coordinates, package names (SHA-256 prefix).

## AIDL — IDeviceMaskerService

Diagnostics-only. Never used for config delivery.

- Oneway (hook→service): `reportSpoofEvent`, `reportLog`, `reportPackageHooked`
- Sync (UI→service): `getSpoofEventCount`, `getHookedPackages`, `getLogs`, `clearDiagnostics`, `isAlive`

## Build

- `android.library`, `kotlin.serialization`
- `aidl = true` for the diagnostics contract
- No Compose, no UI dependencies
- Dependencies: `kotlinx-serialization-json`, `kotlinx-coroutines-core`

## Testing

9 test files covering: JsonConfig parsing/migration, PersonaGenerator stability, IMEI/MAC/Serial/AndroidId generators, DiagnosticEvent serialization, DiagnosticRedactor, DeviceHardwareConfig.
