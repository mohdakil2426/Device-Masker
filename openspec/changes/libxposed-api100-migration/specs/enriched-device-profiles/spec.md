## ADDED Requirements

### Requirement: DeviceProfilePreset enriched fields

Each `DeviceProfilePreset` SHALL include: `buildTime: Long`, `securityPatch: String`, `buildId: String`, `incremental: String`, `supportedAbis: List<String>`, `tacPrefixes: List<String>`, `simCount: Int`, `hasNfc: Boolean`, `has5G: Boolean`.

#### Scenario: Pixel 9 Pro preset has all fields

- **WHEN** `DeviceProfilePreset.findById("pixel_9_pro")` is called
- **THEN** the preset contains `buildTime=1728100000000L`, `securityPatch="2024-10-05"`, `buildId="AP3A.241005.015"`, `supportedAbis=["arm64-v8a", "armeabi-v7a", "armeabi"]`, `tacPrefixes=["35414610", "35414611", "35414612"]`, `simCount=1`, `hasNfc=true`, `has5G=true`

### Requirement: All 10 existing presets enriched

Every preset in the existing preset list SHALL be updated with the new fields containing accurate data for that device model.

#### Scenario: Samsung S24 Ultra has dual SIM

- **WHEN** the Samsung Galaxy S24 Ultra preset is loaded
- **THEN** `simCount=2` (dual SIM variant)

#### Scenario: Budget device has no NFC

- **WHEN** a budget device preset (e.g., Redmi Note) is loaded
- **THEN** `hasNfc=false` if that model lacks NFC

### Requirement: Default values for backward compatibility

The new fields SHALL have sensible defaults: `simCount=1`, `hasNfc=true`, `has5G=true`, `supportedAbis=["arm64-v8a"]`, `tacPrefixes=["35000000"]`, `buildTime=currentTimeMillis` (fallback), `securityPatch=""` (empty = don't spoof).

#### Scenario: Custom/unknown preset

- **WHEN** a preset with minimal data is used
- **THEN** defaults are applied and no crash occurs
