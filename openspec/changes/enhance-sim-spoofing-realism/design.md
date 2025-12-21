# Design: Enhanced SIM Card Spoofing Architecture

## Context

### Problem Statement
Apps detect spoofing by cross-checking SIM identifiers. Current implementation:
- Generates IMSI, ICCID, phone, carrier separately
- Missing key TelephonyManager APIs (`getSimCountryIso`, `getNetworkCountryIso`)
- No carrier preset system for consistent profiles

### Goals
- Achieve 99% realism in SIM spoofing
- All SIM values correlate correctly within a carrier profile
- Support carrier preset selection for common configurations
- Hook all commonly-checked TelephonyManager APIs

### Non-Goals
- Real carrier database sync (uses static data)
- Roaming simulation (always returns non-roaming)
- Dual-SIM advanced handling (basic support only)

## Decisions

### 1. Carrier Data Model

**Decision:** Create comprehensive `CarrierPreset` with all fields

```kotlin
data class CarrierPreset(
    val id: String,                    // "airtel_delhi"
    val name: String,                  // "Airtel"
    val displayName: String,           // "Airtel (Delhi)"
    val countryName: String,           // "India"
    val countryIso: String,            // "in"
    val countryCode: String,           // "+91"
    val mcc: String,                   // "404"
    val mnc: String,                   // "10"
    val iccidIssuerCode: String,       // "10" (carrier-specific)
    val phonePrefix: String,           // "9" (first digit after +91)
)
```

**Rationale:** Single source of truth for all carrier-related values ensures perfect correlation.

### 2. New SpoofTypes

**Decision:** Add 4 new SpoofTypes to SIM_CARD correlation group

| Type | API Hooked | Example Value |
|------|------------|---------------|
| `SIM_COUNTRY_ISO` | `getSimCountryIso()` | `"in"` |
| `NETWORK_COUNTRY_ISO` | `getNetworkCountryIso()` | `"in"` |
| `SIM_OPERATOR_NAME` | `getSimOperatorName()` | `"Airtel"` |
| `NETWORK_OPERATOR` | `getNetworkOperator()` | `"40410"` |

**Rationale:** These APIs are commonly checked by apps and must return consistent values.

### 3. ICCID Generation Fix

**Current (problematic):**
```kotlin
val countryCode = COUNTRY_TO_ICCID_CODE[carrier.countryIso] ?: random
val issuer = String.format("%02d", secureRandom.nextInt(100))  // Random!
```

**New (carrier-aware):**
```kotlin
val countryCode = carrier.iccidCountryCode  // From carrier preset
val issuer = carrier.iccidIssuerCode        // Carrier-specific
```

**Rationale:** Issuer codes should match real carrier patterns, not be random.

### 4. UI Organization

**Decision:** Organize SIM Card category as:

```
┌─────────────────────────────────────────────┐
│ Phone Number + Carrier Name [Switch] [🔄]  │  Combined card (existing)
├─────────────────────────────────────────────┤
│ SIM Country         [Switch]               │  NEW - Independent
│ in                              [🔄]       │
├─────────────────────────────────────────────┤
│ Network Country     [Switch]               │  NEW - Independent
│ in                              [🔄]       │
├─────────────────────────────────────────────┤
│ IMSI               [Switch]    [🔄]        │  Existing
├─────────────────────────────────────────────┤
│ ICCID              [Switch]    [🔄]        │  Existing
├─────────────────────────────────────────────┤
│ MCC/MNC            [Switch]    [🔄]        │  Existing
└─────────────────────────────────────────────┘
```

**Alternative Considered:** Group all country/operator values into combined cards
**Rejected:** Keeps UI consistent with existing pattern and allows granular control

## Data Architecture

### Carrier Presets (India)

```kotlin
// India - Major carriers with regional MNCs
val INDIA_CARRIERS = listOf(
    // Airtel
    CarrierPreset(id = "airtel_delhi", name = "Airtel", mcc = "404", mnc = "10", ...),
    CarrierPreset(id = "airtel_karnataka", name = "Airtel", mcc = "404", mnc = "45", ...),
    CarrierPreset(id = "airtel_ap", name = "Airtel", mcc = "404", mnc = "49", ...),
    
    // Jio (nationwide)
    CarrierPreset(id = "jio", name = "Jio", mcc = "405", mnc = "857", ...),
    CarrierPreset(id = "jio_alt", name = "Jio", mcc = "405", mnc = "858", ...),
    
    // Vi (Vodafone-Idea)
    CarrierPreset(id = "vi_mumbai", name = "Vi", mcc = "404", mnc = "20", ...),
    CarrierPreset(id = "vi_delhi", name = "Vi", mcc = "404", mnc = "11", ...),
    
    // BSNL
    CarrierPreset(id = "bsnl", name = "BSNL", mcc = "404", mnc = "34", ...),
)
```

### SIMProfile Updated Model

```kotlin
data class SIMProfile(
    // Existing
    val carrier: Carrier,
    val imsi: String,
    val iccid: String,
    val phoneNumber: String,
    
    // New fields
    val simCountryIso: String,        // "in"
    val networkCountryIso: String,    // "in" (same unless roaming)
    val simOperatorName: String,      // "Airtel"
    val networkOperator: String,      // "40410"
    val mcc: String,                  // "404"
    val mnc: String,                  // "10"
)
```

## Hook Layer Design

### TelephonyManager Hooks

```kotlin
"android.telephony.TelephonyManager".toClass().apply {
    // Existing hooks...
    
    // NEW: SIM Country ISO
    method { name = "getSimCountryIso" }.hook {
        after { result = spoofValue(SpoofType.SIM_COUNTRY_ISO) }
    }
    
    // NEW: Network Country ISO
    method { name = "getNetworkCountryIso" }.hook {
        after { result = spoofValue(SpoofType.NETWORK_COUNTRY_ISO) }
    }
    
    // NEW: SIM Operator Name
    method { name = "getSimOperatorName" }.hook {
        after { result = spoofValue(SpoofType.SIM_OPERATOR_NAME) }
    }
    
    // NEW: Network Operator (MCC+MNC)
    method { name = "getNetworkOperator" }.hook {
        after { result = spoofValue(SpoofType.NETWORK_OPERATOR) }
    }
}
```

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Incomplete carrier data | Medium | Start with major India carriers, expand over time |
| Hook compatibility issues | Low | Use `optional()` for uncertain methods |
| UI complexity increase | Low | Follow existing category patterns |
| Performance overhead | Low | Values are cached in SIMProfile |

## Migration Plan

1. **Backward Compatible:** New fields have defaults, existing profiles continue working
2. **No Data Migration:** New values generated on-demand when profile is edited
3. **Progressive Enhancement:** UI shows "Not set" for new fields until regenerated

## Open Questions

1. Should we expose carrier preset picker in UI? (Proposed: Future enhancement)
2. Should NETWORK_COUNTRY_ISO differ from SIM_COUNTRY_ISO? (Proposed: No, assume non-roaming)
