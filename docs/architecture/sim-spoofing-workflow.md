# SIM Spoofing Workflow & Correlation System

> **Author:** AI Assistant | **Date:** 2025-12-21 | **Version:** 1.0

This document explains how Device Masker's SIM spoofing system works, including the data flow, synchronization, and why correlation between values is critical for avoiding detection.

---

## 📱 The Synchronization Problem

### On a Real Phone

All SIM values come from the **same physical SIM card**, so they're always consistent:

```
┌──────────────────────────────────────────────────────────┐
│                    PHYSICAL SIM CARD                      │
│                       (Airtel Delhi)                      │
├──────────────────────────────────────────────────────────┤
│  IMSI:        404101234567890  ← MCC(404) + MNC(10)     │
│  ICCID:       8991101234567890  ← 89 + Country(91) + ID │
│  Phone:       +91 98765 43210  ← Country code +91       │
│  Carrier:     "Airtel"                                   │
│  Country:     "in"                                       │
│  MCC/MNC:     "40410"                                    │
└──────────────────────────────────────────────────────────┘
                              ↓
              Apps call TelephonyManager APIs
                              ↓
            All values correlate because same SIM!
```

### The Problem with Random Spoofing

If each value is generated randomly without correlation:

```
❌ IMSI:    310260... (T-Mobile US)
❌ ICCID:   8944...   (UK prefix)
❌ Phone:   +91...    (India code)
❌ Carrier: "Jio"     (India carrier)
❌ Country: "gb"      (UK)

→ Detection! Values don't match = SPOOFED!
```

Apps can easily detect this by cross-checking values. If the IMSI shows a US carrier but the country code is India, it's obviously spoofed.

---

## ✅ Our Solution: Carrier-Based Profile Generation

We solve this by:
1. Selecting ONE carrier from our database
2. Generating ALL values from that carrier's data
3. Caching the profile so all values stay consistent

```
┌────────────────────────────────────────────────────────────────┐
│                         CARRIER DATABASE                        │
│                      (Carrier.kt - 63 carriers)                 │
├────────────────────────────────────────────────────────────────┤
│  Carrier("Airtel", "40410", "91", "IN", "10", "Delhi NCR")    │
│          ↓         ↓       ↓     ↓     ↓        ↓              │
│        name    mccMnc  code  iso  issuer  region               │
└────────────────────────────────────────────────────────────────┘
                                ↓
                    SIMProfileGenerator.generate()
                                ↓
┌────────────────────────────────────────────────────────────────┐
│                         SIM PROFILE                             │
│                    (All values from SAME carrier)               │
├────────────────────────────────────────────────────────────────┤
│  carrier:           Airtel (Delhi)                             │
│  imsi:              40410 + 1234567890 (random 10 digits)     │
│  iccid:             89 + 91 + 10 + serial + luhn              │
│  phoneNumber:       +91 + 9876543210                          │
│  simCountryIso:     "in"  ← from carrier.countryIsoLower      │
│  networkCountryIso: "in"  ← same (non-roaming)                │
│  simOperatorName:   "Airtel" ← from carrier.name              │
│  networkOperator:   "40410" ← from carrier.mccMnc             │
└────────────────────────────────────────────────────────────────┘
                                ↓
                         ALL VALUES CORRELATE!
```

---

## 🔗 The Complete Data Flow

### Step 1: User Enables SIM Spoofing

When the user toggles SIM spoofing ON in the profile settings:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              USER ACTION                                 │
│                         (Toggle SIM spoofing ON)                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│  STEP 1: SpoofRepository.generateSIMValue()                            │
│                                                                          │
│  if (cachedSIMProfile == null) {                                        │
│      cachedSIMProfile = SIMProfileGenerator.generate()  ← ONCE          │
│  }                                                                       │
│  return when(type) {                                                     │
│      IMSI → cachedSIMProfile.imsi                                       │
│      ICCID → cachedSIMProfile.iccid                                     │
│      PHONE_NUMBER → cachedSIMProfile.phoneNumber                        │
│      SIM_COUNTRY_ISO → cachedSIMProfile.simCountryIso                   │
│      ...                                                                 │
│  }                                                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

### Step 2: Values Stored in Profile

```
┌─────────────────────────────────────────────────────────────────────────┐
│  STEP 2: Values stored in SpoofProfile (DataStore)                     │
│                                                                          │
│  {                                                                       │
│    "IMSI": "404101234567890",                                           │
│    "ICCID": "89911012345678901",                                        │
│    "PHONE_NUMBER": "+919876543210",                                     │
│    "CARRIER_NAME": "Airtel",                                            │
│    "SIM_COUNTRY_ISO": "in",                                             │
│    "NETWORK_COUNTRY_ISO": "in",                                         │
│    "SIM_OPERATOR_NAME": "Airtel",                                       │
│    "NETWORK_OPERATOR": "40410",                                         │
│    "CARRIER_MCC_MNC": "40410"                                           │
│  }                                                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

### Step 3: Hook Intercepts App Calls

```
┌─────────────────────────────────────────────────────────────────────────┐
│  STEP 3: Hook intercepts app API call                                   │
│                                                                          │
│  TARGET APP: telephonyManager.getSimCountryIso()                        │
│                        ↓                                                 │
│  DeviceHooker: result = getSpoofValue(SIM_COUNTRY_ISO) { "us" }        │
│                        ↓                                                 │
│  Returns: "in" (from stored profile)                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🔄 Regeneration Workflow

When user clicks **Regenerate** button in the UI:

```
┌──────────────────────────────────────────────────────────────────────┐
│                  PHONE NUMBER + CARRIER NAME CARD                     │
│                                                                        │
│  [Phone Number]                              [Switch] [🔄 Regenerate] │
│  +91 98765 43210                                                      │
│  Carrier: Airtel                                                      │
└──────────────────────────────────────────────────────────────────────┘
                              │
                    User clicks [🔄]
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│  onRegenerate(PHONE_NUMBER)                                          │
│  onRegenerate(CARRIER_NAME)                                          │
│            ↓                                                          │
│  SpoofRepository clears cachedSIMProfile                             │
│            ↓                                                          │
│  SIMProfileGenerator.generate() → NEW Carrier selected              │
│            ↓                                                          │
│  ALL 9 SIM values regenerated with NEW carrier                       │
│  (Jio this time → all values now match Jio)                          │
└──────────────────────────────────────────────────────────────────────┘
```

**Key Point:** Regenerating ANY SIM value should regenerate ALL correlated values to maintain consistency.

---

## 📊 Correlation Group Concept

The `CorrelationGroup.SIM_CARD` ensures these 9 values always come from the same carrier:

```
┌───────────────────────────────────────────────────────────────────────┐
│                    CorrelationGroup.SIM_CARD                          │
│                                                                        │
│  These 9 values MUST come from the SAME carrier:                      │
│                                                                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐       │
│  │ PHONE_NUMBER    │  │ CARRIER_NAME    │  │ CARRIER_MCC_MNC │       │
│  │ +91 98765 43210 │  │ Airtel          │  │ 40410           │       │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘       │
│           │                    │                    │                 │
│           └────────────────────┴────────────────────┘                 │
│                                │                                       │
│              All derived from: Carrier("Airtel", "40410", ...)        │
│                                │                                       │
│           ┌────────────────────┴────────────────────┐                 │
│           │                    │                    │                 │
│  ┌────────┴────────┐  ┌────────┴────────┐  ┌───────┴─────────┐       │
│  │ IMSI            │  │ ICCID           │  │ SIM_COUNTRY_ISO │       │
│  │ 40410123456789  │  │ 8991101234...   │  │ in              │       │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘       │
│                                                                        │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐       │
│  │ NETWORK_COUNTRY │  │ SIM_OPERATOR    │  │ NETWORK_OPERATOR│       │
│  │ in              │  │ Airtel          │  │ 40410           │       │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘       │
└───────────────────────────────────────────────────────────────────────┘
```

---

## 🎯 Detection Avoidance - Why Correlation Matters

### Without Correlation (Detectable)

```
App Detection Logic:
1. mcc = getSimOperator().substring(0, 3)  → "404" (India)
2. country = getSimCountryIso()            → "gb" (UK)
3. if (mcc doesn't match country) → FLAG AS SPOOFED!
```

### With Correlation (Undetectable)

```
App Detection Logic:
1. mcc = getSimOperator().substring(0, 3)  → "404" (India)
2. country = getSimCountryIso()            → "in" (India)
3. carrier = getSimOperatorName()          → "Airtel"
4. imsi = getSubscriberId()                → "40410..." (starts with 404)
5. ALL VALUES MATCH → Looks like real device!
```

---

## 🔌 Hooked APIs

We hook 27 methods across two classes to ensure complete coverage:

### TelephonyManager Hooks (18 methods)

| Method | SpoofType |
|--------|-----------|
| `getDeviceId()` | IMEI |
| `getImei()` | IMEI |
| `getSubscriberId()` | IMSI |
| `getSimSerialNumber()` | ICCID |
| `getSimCountryIso()` | SIM_COUNTRY_ISO |
| `getNetworkCountryIso()` | NETWORK_COUNTRY_ISO |
| `getSimOperatorName()` | SIM_OPERATOR_NAME |
| `getNetworkOperator()` | NETWORK_OPERATOR |
| `getSimOperator()` | CARRIER_MCC_MNC |

*Note: Most methods have overloads with slot index (IntType) for dual-SIM*

### SubscriptionInfo Hooks (9 methods)

| Method | SpoofType |
|--------|-----------|
| `getCountryIso()` | SIM_COUNTRY_ISO |
| `getCarrierName()` | CARRIER_NAME |
| `getDisplayName()` | CARRIER_NAME |
| `getMcc()` | CARRIER_MCC_MNC (first 3 digits) |
| `getMnc()` | CARRIER_MCC_MNC (remaining digits) |
| `getMccString()` | CARRIER_MCC_MNC (first 3 digits) |
| `getMncString()` | CARRIER_MCC_MNC (remaining digits) |
| `getIccId()` | ICCID |
| `getNumber()` | PHONE_NUMBER |

---

## 📋 Carrier Database

We maintain a comprehensive carrier database with 63 carriers:

### India Coverage (Highest Priority)

| Carrier | Regional MNCs | Count |
|---------|---------------|-------|
| **Airtel** | Delhi, Karnataka, Mumbai, Chennai, etc. | 17 |
| **Jio** | 405857-405874 (nationwide) | 18 |
| **Vi** | Delhi, Mumbai, Kerala, TN, etc. | 13 |
| **BSNL** | Various states | 15 |

### Other Countries

| Country | Carriers |
|---------|----------|
| USA | T-Mobile, Verizon, AT&T, Sprint |
| UK | Vodafone, EE, O2, Three |
| Germany | T-Mobile DE, Vodafone DE, O2 DE |
| France | SFR, Orange, Bouygues |
| China | China Mobile, Unicom, Telecom |
| Japan | NTT DoCoMo, SoftBank, KDDI |
| Australia | Telstra, Optus, Vodafone AU |

---

## 🏗️ Architecture Components

```
┌─────────────────────────────────────────────────────────────────────┐
│                           DATA LAYER                                 │
├─────────────────────────────────────────────────────────────────────┤
│  Carrier.kt           → Carrier database (63 carriers)             │
│  SIMProfile.kt        → Data class with all 9 correlated fields    │
│  SpoofType.kt         → Enum with 9 SIM-related types              │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────┐
│                         GENERATOR LAYER                              │
├─────────────────────────────────────────────────────────────────────┤
│  SIMProfileGenerator   → Generates complete correlated profiles    │
│  IMSIGenerator         → Generates IMSI with carrier MCC/MNC       │
│  ICCIDGenerator        → Generates ICCID with carrier issuer code  │
│  PhoneNumberGenerator  → Generates phone with country code         │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────┐
│                        REPOSITORY LAYER                              │
├─────────────────────────────────────────────────────────────────────┤
│  SpoofRepository       → Manages cachedSIMProfile                   │
│                        → generateSIMValue() returns correlated data │
│                        → Stores values in DataStore                 │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────┐
│                           HOOK LAYER                                 │
├─────────────────────────────────────────────────────────────────────┤
│  DeviceHooker          → hookTelephonyManager() (18 methods)        │
│                        → hookSubscriptionInfo() (9 methods)         │
│                        → getSpoofValue() reads from config          │
└─────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────┐
│                            UI LAYER                                  │
├─────────────────────────────────────────────────────────────────────┤
│  SIMCardCategoryContent → Displays all 9 SIM values                 │
│                         → Phone+Carrier combined card               │
│                         → Others as independent items               │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 📝 Summary

| Concept | Implementation |
|---------|----------------|
| **Carrier Database** | 63 carriers in `Carrier.kt` with MCC/MNC, country, issuer codes |
| **SIMProfile** | Data class with 9 fields, generated from single carrier |
| **cachedSIMProfile** | Ensures all values from same generation |
| **Regenerate** | Clears cache, picks new carrier, regenerates all |
| **Hooks** | 27 methods hooked (TelephonyManager + SubscriptionInfo) |
| **UI** | Phone+Carrier grouped, others independent |
| **Goal** | 99%+ realism through perfect correlation |

---

## 🎯 Key Takeaway

> **"Generate once, use everywhere"**
> 
> All SIM values come from the same `SIMProfile` instance, which was generated from a single carrier's data. This guarantees perfect correlation across all 27 hooked API methods, making detection virtually impossible.
