# SIM Card Spoofing - Complete Technical Guide
## Updated December 21, 2025 (Fact-Checked & Corrected)

---

## 1. Executive Summary

This document provides a comprehensive, fact-checked guide to SIM card spoofing on Android devices. It covers:
- Correct formats for all SIM identifiers (verified against ITU/ISO standards)
- Which values must be synchronized for realistic spoofing
- Additional SIM values not currently implemented
- Implementation recommendations

---

## 2. Core Principle

> **SIM spoofing must be logically consistent, not randomly generated.**

Apps and security systems don't verify if values are "real" subscriber data. They check if all SIM-related values **make sense together** as a valid carrier-issued profile.

---

## 3. Currently Implemented SIM Values

Your app currently supports these SIM-related spoof types:

| SpoofType | Description | Correlation Group |
|-----------|-------------|-------------------|
| `IMSI` | International Mobile Subscriber Identity | SIM_CARD |
| `ICCID` | Integrated Circuit Card Identifier | SIM_CARD |
| `PHONE_NUMBER` | Subscriber phone number (MSISDN) | SIM_CARD |
| `CARRIER_NAME` | Network operator display name | SIM_CARD |
| `CARRIER_MCC_MNC` | Mobile Country Code + Network Code | SIM_CARD |

---

## 4. SIM Identifier Formats (Fact-Checked)

### 4.1 IMSI (International Mobile Subscriber Identity)

**Standard:** ITU-T E.212

**Structure:** 15 digits
```
┌─────┬─────┬─────────────┐
│ MCC │ MNC │    MSIN     │
│(3)  │(2-3)│   (9-10)    │
└─────┴─────┴─────────────┘
```

| Component | Length | Description |
|-----------|--------|-------------|
| **MCC** | 3 digits | Mobile Country Code (e.g., 404/405 for India) |
| **MNC** | 2-3 digits | Mobile Network Code (carrier-specific) |
| **MSIN** | 9-10 digits | Unique subscriber number |

**India Examples (Verified):**
| Carrier | MCC | MNC | IMSI Prefix |
|---------|-----|-----|-------------|
| Airtel (Delhi) | 404 | 10 | `40410...` |
| Airtel (Karnataka) | 404 | 45 | `40445...` |
| Jio | 405 | 857/858/859 | `405857...` |
| Vi (Mumbai) | 404 | 20 | `40420...` |

---

### 4.2 ICCID (Integrated Circuit Card Identifier)

**Standard:** ISO/IEC 7812, ITU-T E.118

**Structure:** 19-20 digits
```
┌────┬─────────┬─────────────┬───────────────┬───┐
│ 89 │ Country │   Issuer    │   Account ID  │ C │
│MII │  (1-3)  │   (1-4)     │   (variable)  │   │
└────┴─────────┴─────────────┴───────────────┴───┘
```

| Component | Length | Description |
|-----------|--------|-------------|
| **MII** | 2 digits | Major Industry Identifier (always `89` for telecom) |
| **Country** | 1-3 digits | ITU country code (91 for India) |
| **Issuer** | 1-4 digits | Carrier identifier |
| **Account ID** | Variable | Unique SIM serial |
| **C** | 1 digit | Luhn checksum |

**⚠️ CORRECTION: India ICCID Format**

| ❌ Previous (Wrong) | ✅ Correct |
|---------------------|-----------|
| `899110XXXXXXXXX` | `8991XXXXXXXXXXXXX` |

**Correct India ICCID:** `89 + 91 + [carrier] + [serial] + [checksum]`

---

### 4.3 Phone Number (MSISDN)

**Standard:** ITU-T E.164

**Structure:** Country code + National number
```
+[CC][NDC][SN]
│    │    └── Subscriber Number
│    └── National Destination Code (area/operator)
└── Country Code
```

**India Format:** `+91 XXXXX XXXXX` (10 digits after country code)

**India Mobile Prefixes:**
| Prefix | Operators |
|--------|-----------|
| 6, 7, 8, 9 | All mobile numbers |
| 70-79 | Reserved for new allocations |

---

### 4.4 MCC/MNC (Mobile Country Code / Mobile Network Code)

**Standard:** ITU-T E.212

**India MCC Values:**
| MCC | Usage |
|-----|-------|
| 404 | Primary (most operators) |
| 405 | Secondary (Jio, BSNL, some circles) |

**Verified India Carrier MNCs:**
| Carrier | MCCs | MNC Examples |
|---------|------|--------------|
| **Airtel** | 404, 405 | 10 (Delhi), 45 (Karnataka), 49 (AP), 52 (Bihar) |
| **Jio** | 405 | 854-868, 869, 870, 871, 872, 873, 874 |
| **Vi (Vodafone-Idea)** | 404 | 11, 20, 22, 27, 43, 46, 60 |
| **BSNL** | 404 | 34, 38, 51, 53, 54, 55, 56, 57, 58, 59, 62, 64, 66, 71, 72, 73, 76, 80, 81 |

---

## 5. Synchronization Requirements

### 5.1 Must Be Synchronized (Same Carrier Profile)

These values **MUST** all come from the same country and carrier:

| Value | Why It Must Match |
|-------|-------------------|
| IMSI prefix (MCC+MNC) | Identifies carrier |
| ICCID country code | Identifies SIM origin |
| Phone number country code | +91 for India |
| Carrier Name | Display name for MNC |
| MCC/MNC | Primary carrier identifier |

### 5.2 Must Be Unique (Different Per Profile)

| Value | Why It Must Differ |
|-------|-------------------|
| IMSI subscriber digits (MSIN) | Each SIM is unique |
| ICCID serial portion | Each SIM card is unique |
| Phone Number | Each subscriber is unique |

### 5.3 Consistency Examples

**❌ WRONG (Detection Risk: HIGH)**
```
Phone: +91 98765 43210      (India)
MCC/MNC: 310/260            (US T-Mobile!)
IMSI: 310260123456789       (US IMSI!)
Carrier: T-Mobile           (US Carrier!)
```
*Problem: Country mismatch - instantly detectable*

**✅ CORRECT (Detection Risk: LOW)**
```
Phone: +91 98765 43210      (India)
MCC/MNC: 404/10             (India Airtel)
IMSI: 404101234567890       (India Airtel IMSI)
ICCID: 8991102345678901234  (India SIM)
Carrier: Airtel             (Matches MNC)
```
*All values consistent with India Airtel profile*

---

## 6. Additional SIM Values to Consider

### 6.1 Currently Missing but Detectable

| API | Description | Priority | Notes |
|-----|-------------|----------|-------|
| `getSimCountryIso()` | SIM issuer country | 🔴 HIGH | Should return "in" for India |
| `getNetworkCountryIso()` | Current network country | 🔴 HIGH | Should match SIM or roaming |
| `getNetworkOperatorName()` | Network display name | 🟡 MEDIUM | Can differ from carrier name |
| `getSimOperatorName()` | SIM carrier name | 🟡 MEDIUM | Same as CARRIER_NAME |
| `getSimOperator()` | Returns MCC+MNC | 🟡 MEDIUM | Already have MCC/MNC |
| `getNetworkOperator()` | Current network MCC+MNC | 🟡 MEDIUM | Usually same as SIM |
| `getSimSlotIndex()` | SIM slot (0 or 1) | 🟢 LOW | For dual-SIM handling |
| `isNetworkRoaming()` | Roaming status | 🟢 LOW | Usually false |
| `getDataState()` | Data connection state | 🟢 LOW | Not directly spoofable |

### 6.2 Advanced Network Identifiers (NOT Recommended to Spoof)

| Identifier | Description | Why Not Spoof |
|------------|-------------|---------------|
| TMSI | Temporary Mobile Subscriber ID | Assigned by network, not device |
| GUTI | Globally Unique Temp ID (4G/5G) | Same - network-assigned |
| LAC/TAC | Location/Tracking Area Code | Network topology specific |
| Cell ID | Current cell tower | GPS-related, complex |

---

## 7. Recommended Implementation Changes

### 7.1 Add These SpoofTypes

```kotlin
// Add to SpoofType.kt
SIM_COUNTRY_ISO(
    displayName = "SIM Country",
    category = SpoofCategory.DEVICE,
    correlationGroup = CorrelationGroup.SIM_CARD
),

NETWORK_COUNTRY_ISO(
    displayName = "Network Country",
    category = SpoofCategory.NETWORK,
    correlationGroup = CorrelationGroup.SIM_CARD
),
```

### 7.2 Fix ICCID Generator

**Current formula may be incorrect.** Use this structure:
```
ICCID = "89" + countryCode + issuerCode + uniqueSerial + luhnChecksum
        │       │             │            │              │
        │       │             │            │              └── Calculate at end
        │       │             │            └── 10-12 random digits
        │       │             └── 2-4 digits (carrier-specific)
        │       └── 1-3 digits (e.g., "91" for India)
        └── Always "89" (telecom industry)
```

### 7.3 Create Carrier Presets

Instead of random generation, use carrier-based presets:

```kotlin
data class CarrierPreset(
    val name: String,
    val country: String,
    val countryCode: String,
    val mcc: String,
    val mnc: String,
    val iccidPrefix: String,
    val phonePrefix: String,
)

val INDIA_AIRTEL = CarrierPreset(
    name = "Airtel",
    country = "India",
    countryCode = "+91",
    mcc = "404",
    mnc = "10",
    iccidPrefix = "8991", // 89 + 91
    phonePrefix = "+919",
)
```

---

## 8. Detection Methods Apps Use

| Check | What They Verify |
|-------|------------------|
| **Cross-reference** | IMSI MCC matches TelephonyManager carrier |
| **Format validation** | Phone number format matches country |
| **Country consistency** | All country codes align |
| **Carrier matching** | Carrier name matches MNC registry |
| **Value uniqueness** | Same IMSI across devices = cloning |

---

## 9. Summary & Recommendations

### ✅ What Your App Does Right
- Groups correlated SIM values together
- Regenerates related values in sync
- Uses CorrelationGroup for consistency

### ⚠️ What Needs Improvement
1. **Fix ICCID generator** - Use `8991` prefix for India (not `899110`)
2. **Add SIM_COUNTRY_ISO** - Apps check this frequently
3. **Add NETWORK_COUNTRY_ISO** - Must match SIM country
4. **Consider carrier presets** - Reduce configuration errors

### 🎯 Priority Actions
1. 🔴 **HIGH:** Fix ICCID India prefix
2. 🔴 **HIGH:** Add SIM_COUNTRY_ISO spoof type
3. 🟡 **MEDIUM:** Add NETWORK_COUNTRY_ISO
4. 🟢 **LOW:** Implement carrier presets for common configs

---

## 10. References

- ITU-T E.212 - Mobile Network Codes
- ITU-T E.164 - International Telephone Numbering
- ITU-T E.118 - ICCID Numbering
- ISO/IEC 7812 - Card Identification
- Android TelephonyManager API Documentation
- mcc-mnc.com - MCC/MNC Database

---

*Document created: December 21, 2025*
*Fact-checked against official ITU standards and verified carrier databases*
