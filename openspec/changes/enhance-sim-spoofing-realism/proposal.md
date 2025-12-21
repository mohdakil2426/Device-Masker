# Change: Enhance SIM Card Spoofing for 99% Realism

## Why

Current SIM card spoofing implementation has several issues that reduce realism and increase detection risk:

1. **ICCID generator may produce inconsistent prefixes** - The country code mapping exists but issuer codes could be more realistic
2. **Missing SIM-related spoof types** - `SIM_COUNTRY_ISO` and `NETWORK_COUNTRY_ISO` are not implemented, which apps commonly check
3. **MCC/MNC data incomplete** - India carrier data needs expansion with region-specific MNCs (Airtel: 404/10, 404/45, etc.)
4. **No carrier preset system** - Users must rely on random generation instead of selecting realistic carrier profiles
5. **UI not updated** - SIM Card category needs to include new spoof types

## What Changes

### Phase 1: Data Layer - Carrier & Generator Fixes
- Add comprehensive `CarrierPreset` data class with all required fields
- Expand `Carrier.kt` with complete India carrier data (Airtel, Jio, Vi, BSNL with region-specific MNCs)
- Fix ICCID generator to use carrier-specific issuer codes
- Add `countryIso` and `networkCountryIso` fields to SIMProfile model

### Phase 2: New SpoofTypes
- Add `SIM_COUNTRY_ISO` (returns "in" for India, etc.)
- Add `NETWORK_COUNTRY_ISO` (returns current network country)
- Add `SIM_OPERATOR_NAME` (may differ from CARRIER_NAME in some cases)
- Add `NETWORK_OPERATOR` (returns MCC+MNC as string like "40410")

### Phase 3: Generator Enhancements
- Update `SIMProfileGenerator` to generate all new fields
- Ensure all values correlate correctly with selected carrier
- Add carrier preset selection support

### Phase 4: Repository & Hooker Updates
- Update `SpoofRepository` to handle new SpoofTypes
- Update `SIMHooker` (if exists) or create hooks for new APIs:
  - `TelephonyManager.getSimCountryIso()`
  - `TelephonyManager.getNetworkCountryIso()`
  - `TelephonyManager.getSimOperatorName()`
  - `TelephonyManager.getNetworkOperator()`

### Phase 5: UI Updates
- Update `UIDisplayCategory.SIM_CARD` to include new types
- Update `SIMCardCategoryContent` to display new values
- Add carrier preset picker (optional - future enhancement)

## Impact

### Affected Specs
- `device-spoofing` - MODIFIED: New SIM-related requirements

### Affected Code
- `common/models/Carrier.kt` - Expand carrier data
- `common/models/SIMProfile.kt` - Add new fields
- `common/SpoofType.kt` - Add new enum values
- `common/generators/ICCIDGenerator.kt` - Add issuer codes
- `common/generators/SIMProfileGenerator.kt` - Generate all fields
- `app/data/repository/SpoofRepository.kt` - Handle new types
- `app/ui/screens/ProfileDetailScreen.kt` - Display new types
- `xposed/hooker/` - New/updated SIM hooks

### Risk Assessment
- **Low Risk**: Changes are additive, no breaking changes to existing functionality
- **Detection Reduction**: Significantly improves realism, reducing app detection
