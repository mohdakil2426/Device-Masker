# Spoofing Value Correlation & Consistency Analysis

**Date**: December 20, 2025  
**Status**: CRITICAL SECURITY ANALYSIS  
**Priority**: HIGH - Detection Risk Assessment

---

## Executive Summary

**Question**: Are our independently generated spoofing values creating **detectable inconsistencies** that could expose the device as spoofed?

**Answer**: ⚠️ **YES - 5 CRITICAL ISSUES FOUND**

We have **independent generators** for values that **MUST be correlated** in real devices. This creates **fingerprinting patterns** that detection systems can identify.

---

## Current Spoofing Implementation

### All Spoof Types (17 Total)

```
DEVICE (6):
├── IMEI               [Independent] ❌
├── MEID               [Independent] ❌
├── IMSI               [Independent] ⚠️
├── SERIAL             [Independent] ✅
├── ICCID              [Independent] ⚠️
└── PHONE_NUMBER       [Independent] ✅

NETWORK (6):
├── WIFI_MAC           [Independent] ⚠️
├── BLUETOOTH_MAC      [Independent] ✅
├── WIFI_SSID          [Independent] ✅
├── WIFI_BSSID         [Independent] ✅
├── CARRIER_NAME       [Independent] ⚠️
└── CARRIER_MCC_MNC    [Independent] ⚠️

ADVERTISING (4):
├── ANDROID_ID         [Independent] ✅
├── GSF_ID             [Independent] ✅
├── ADVERTISING_ID     [Independent] ✅
└── MEDIA_DRM_ID       [Independent] ✅

SYSTEM (1):
└── DEVICE_PROFILE     [Preset-based] ✅

LOCATION (4):
├── LOCATION_LATITUDE  [Independent] ✅
├── LOCATION_LONGITUDE [Independent] ✅
├── TIMEZONE           [Independent] ⚠️
└── LOCALE             [Independent] ⚠️
```

---

## CRITICAL ISSUES FOUND

### 🚨 ISSUE 1: IMEI vs MEID (Device Identity)

**Current Implementation**:
```kotlin
IMEI -> IMEIGenerator.generate()  // Generate random 15-digit IMEI
MEID -> IMEIGenerator.generate()  // Generate ANOTHER random 15-digit IMEI
```

**Problem**: IMEI and MEID are **mutually exclusive** on real devices!

**Research Findings**:
- **IMEI**: Used on GSM devices (AT&T, T-Mobile)
- **MEID**: Used on CDMA devices (Verizon, Sprint - mostly deprecated)
- **Real devices**: Have EITHER IMEI OR MEID, **NOT BOTH**
- **Dual-SIM devices**: Have **2 IMEIs** (one per SIM slot), NOT IMEI + MEID
- **Modern phones**: Only have IMEI (CDMA networks are being phased out)

**Detection Risk**: **CRITICAL** ⚠️⚠️⚠️  
Apps can check: `if (device.hasIMEI() && device.hasMEID()) → SPOOFED!`

**Fix Required**:
```kotlin
// Option 1: Never generate both at same time
if (profile.hasIMEI()) {
    // Don't generate MEID
    return null
}

// Option 2: Generate 2 IMEIs for dual-SIM instead
if (isDualSIM) {
    IMEI_1 -> IMEIGenerator.generate()
    IMEI_2 -> IMEIGenerator.generate()  // Different value
    MEID -> null
}
```

---

### 🚨 ISSUE 2: IMSI vs ICCID vs CARRIER (SIM Card Identity)

**Current Implementation**:
```kotlin
IMSI -> IMSIGenerator.generate()              // Random MCC/MNC
ICCID -> ICCIDGenerator.generate()            // Random country code
CARRIER_NAME -> random("T-Mobile", "Verizon") // Random carrier
CARRIER_MCC_MNC -> "310${random()}"           // Random US MCC/MNC
```

**Problem**: **ALL FOUR** values are from the **SAME SIM CARD** but generated independently!

**Research Findings**:
- **IMSI** contains **MCC+MNC** (first 5-6 digits) identifying carrier
- **ICCID** contains **country code** (2 digits after MII)
- **CARRIER_NAME** and **CARRIER_MCC_MNC** must match IMSI's MCC/MNC
- **Real SIM**: All 4 values are **strictly correlated**

**Example Real SIM** (T-Mobile US):
```
IMSI:         310260123456789
              ^^^^^^--- MCC=310 (US), MNC=260 (T-Mobile)
ICCID:        8901260123456789012
              ^^^^--- MII=89, Country=01 (US)
CARRIER_NAME: "T-Mobile"
CARRIER_MCC_MNC: "310260"
```

**Our Current Output** (WRONG):
```
IMSI:         404450123456789  (Airtel India)
              ^^^^^^--- MCC=404 (India), MNC=450
ICCID:        8912456789012345678  (Some random country)
CARRIER_NAME: "Verizon"  (US carrier)
CARRIER_MCC_MNC: "310123"  (Random US code)
```

**Detection Pattern**:
```javascript
if (IMSI_MCC !== CARRIER_MCC) → SPOOFED!
if (CARRIER_NAME === "Verizon" && IMSI_MNC !== "480") → SPOOFED!
if (IMSI_country !== ICCID_country) → SUSPICIOUS!
```

**Detection Risk**: **CRITICAL** ⚠️⚠️⚠️

**Fix Required**:
```kotlin
data class SIMCardProfile(
    val carrier: Carrier,  // Define carrier first
    val mccMnc: String,    // From carrier
    val imsi: String,      // Generated with carrier's MCC/MNC
    val iccid: String,     // Generated with matching country
    val phoneNumber: String  // Optional, matching country code
)

// Example carriers
val CARRIERS = listOf(
    Carrier("T-Mobile US", "310260", countryCode = "1"),
    Carrier("Verizon", "311480", countryCode = "1"),
    Carrier("Airtel India", "404450", countryCode = "91"),
    // ...
)

// Generate correlated values
fun generateSIMProfile(): SIMCardProfile {
    val carrier = CARRIERS.random()
    return SIMCardProfile(
        carrier = carrier,
        mccMnc = carrier.mccMnc,
        imsi = IMSIGenerator.generate(mccMnc = carrier.mccMnc),
        iccid = ICCIDGenerator.generate(countryCode = carrier.countryIso),
        phoneNumber = generatePhoneNumber(countryCode = carrier.countryCode)
    )
}
```

---

### ⚠️ ISSUE 3: TIMEZONE vs LOCALE vs IMSI (Location Consistency)

**Current Implementation**:
```kotlin
TIMEZONE -> random("America/New_York", "Asia/Tokyo", ...)
LOCALE -> random("en_US", "ja_JP", ...)
IMSI -> IMSIGenerator.generate()  // Random country
```

**Problem**: All 3 indicate device location but can mismatch!

**Research Findings**:
- **TIMEZONE**: Where device currently is
- **LOCALE**: User's language/region preference
- **IMSI**: SIM card's home network country
- **Realistic**: TIMEZONE and LOCALE often match IMSI country (but not always due to roaming/travel)

**Example Mismatch** (Suspicious):
```
TIMEZONE:  "Asia/Tokyo" (Japan)
LOCALE:    "en_US" (USA)
IMSI:      310260... (T-Mobile USA)
```

**Detection Pattern**:
```javascript
// Suspicious if all 3 are from different countries
if (timezone_country !== locale_country && 
    locale_country !== imsi_country &&
    timezone_country !== imsi_country) → SUSPICIOUS!
```

**Detection Risk**: **MEDIUM** ⚠️

**Fix Recommendation**:
```kotlin
// Option 1: Match IMSI country (realistic for home network)
val simCountry = getCountryFromIMSI(imsi)
timezone = getTimezoneForCountry(simCountry).random()
locale = getLocaleForCountry(simCountry).random()

// Option 2: Allow mismatch but keep timezone+locale matched
val deviceCountry = countries.random()
timezone = getTimezoneForCountry(deviceCountry).random()
locale = getLocaleForCountry(deviceCountry).random()
// IMSI can be different (roaming scenario)
```

---

### ⚠️ ISSUE 4: WiFi MAC OUI vs Device Manufacturer

**Current Implementation**:
```kotlin
DEVICE_PROFILE -> "pixel_8_pro"  (Google device)
WIFI_MAC -> MACGenerator.generateWiFiMAC()  // Random OUI
```

**Problem**: MAC OUI (first 3 bytes) should often match device manufacturer!

**Research Findings**:
- **MAC OUI**: First 3 bytes identify hardware manufacturer
- **Real devices**: WiFi chip often from **device manufacturer** or **known partners**
- **Examples**:
  - Google Pixel → Google OUI (`DA:A1:19`, `F4:F5:E8`, etc.)
  - Samsung → Samsung OUI (`00:16:32`, `34:23:BA`, etc.)
  - iPhone → Apple OUI (`00:03:93`, `00:1F:F3`, etc.)

**OUI Databases**: Publicly available, easy to check!

**Current Risk**:
```kotlin
DEVICE_PROFILE: "pixel_8_pro" (Google Pixel 8 Pro)
WIFI_MAC: "00:16:32:XX:XX:XX"  (Samsung OUI ❌)
```

**Detection Pattern**:
```javascript
device_brand = "Google"
mac_oui = "00:16:32"  // Samsung
if (OUI_DATABASE[mac_oui].manufacturer !== device_brand) → SUSPICIOUS!
```

**Detection Risk**: **MEDIUM** ⚠️

**Fix Recommendation**:
```kotlin
fun generateWiFiMAC(deviceProfile: DeviceProfilePreset): String {
    // Get OUIs for device manufacturer
    val manufacturerOUIs = when (deviceProfile.manufacturer.lowercase()) {
        "google" -> listOf("DA:A1:19", "F4:F5:E8", "3C:5A:B4")
        "samsung" -> listOf("00:16:32", "34:23:BA", "D0:DF:9A")
        "apple" -> listOf("00:03:93", "00:1F:F3", "00:25:BC")
        // ...
        else -> null  // Use generic locally-administered MAC
    }
    
    return if (manufacturerOUIs != null && Random.nextBoolean()) {
        // 50% chance: Use real manufacturer OUI
        MACGenerator.generateWithOUI(manufacturerOUIs.random())
    } else {
        // 50% chance: Use locally-administered (also realistic)
        MACGenerator.generate()
    }
}
```

---

### ⚠️ ISSUE 5: Serial Number vs Device Profile

**Current Implementation**:
```kotlin
DEVICE_PROFILE -> "pixel_8_pro"
SERIAL -> SerialGenerator.generate()  // Random pattern
```

**Problem**: Serial number pattern should match device manufacturer!

**Current Status**: **MOSTLY OK** ✅  
Our `SerialGenerator` already has manufacturer-specific patterns!

```kotlin
fun generate(): String {
    val patterns = listOf(
        ::generateSamsungSerial,   ✅
        ::generatePixelSerial,     ✅
        ::generateXiaomiSerial,    ✅
        ::generateGenericSerial,   ✅
    )
    return patterns[secureRandom.nextInt(patterns.size)]()
}
```

**Minor Issue**: Pattern is selected **randomly**, not based on DEVICE_PROFILE!

**Detection Pattern** (Rare but possible):
```javascript
device_model = "Pixel 8 Pro"
serial_pattern = "R58M..."  // Samsung pattern
if (!serial_pattern_matches(device_model, serial)) → SUSPICIOUS!
```

**Detection Risk**: **LOW** ⚠️ (but should fix for perfection)

**Fix Recommendation**:
```kotlin
fun generateSerial(deviceProfile: DeviceProfilePreset): String {
    return when (deviceProfile.manufacturer.lowercase()) {
        "samsung" -> generateSamsungSerial()
        "google" -> generatePixelSerial()
        "xiaomi", "redmi", "poco" -> generateXiaomiSerial()
        else -> generateGenericSerial()
    }
}
```

---

## Values That Are Correctly Independent ✅

These values **SHOULD** be independent and our implementation is **CORRECT**:

| Value | Why Independent is OK |
|-------|----------------------|
| **Android ID** | App-scoped since Android 8, different per app ✅ |
| **GSF ID** | Per-device, no correlation with other IDs ✅ |
| **Advertising ID** | User-resettable, UUID format, no pattern ✅ |
| **Media DRM ID** | Widevine-specific, 64 hex chars, independent ✅ |
| **Bluetooth MAC** | Can differ from WiFi MAC (separate chip) ✅ |
| **WIFI_SSID/BSSID** | External network, not device property ✅ |
| **Lat/Long** | GPS location, fully independent ✅ |

---

## Summary: Detection Risk Matrix

| Issue | Current Status | Detection Risk | Fix Priority |
|-------|----------------|----------------|--------------|
| **IMEI + MEID both present** | ❌ Both generated independently | **CRITICAL** | 🔴 P0 |
| **IMSI ↔ ICCID ↔ CARRIER mismatch** | ❌ All independent | **CRITICAL** | 🔴 P0 |
| **TIMEZONE ↔ LOCALE ↔ IMSI mismatch** | ❌ All independent | **MEDIUM** | 🟡 P1 |
| **MAC OUI ↔ Device mismatch** | ⚠️ Random OUI | **MEDIUM** | 🟡 P1 |
| **Serial ↔ Device mismatch** | ⚠️ Random pattern | **LOW** | 🟢 P2 |

---

## Recommended Architecture Changes

### Phase 1: Critical Fixes (P0)

#### 1.1 Create Correlated Value Groups

```kotlin
// NEW: SpoofValueCorrelation.kt
data class CorrelatedDeviceIdentity(
    // Device Hardware (mutually exclusive)
    val imei1: String?,          // Primary IMEI (always present)
    val imei2: String?,          // Secondary IMEI (dual-SIM only)
    val meid: String?,           // MEID (CDMA only, deprecated)
    
    // SIM Card (all must correlate)
    val simProfile: SIMProfile,
    
    // Device Info
    val deviceProfile: DeviceProfilePreset,
    val serial: String,
    
    // Network
    val wifiMAC: String,
    val bluetoothMAC: String,
    
    // Location (correlated)
    val locationProfile: LocationProfile,
    
    // Advertising (independent - OK)
    val androidId: String,
    val gsfId: String,
    val advertisingId: String,
    val mediaDrmId: String
)

data class SIMProfile(
    val carrier: Carrier,
    val mccMnc: String,
    val imsi: String,
    val iccid: String,
    val phoneNumber: String
)

data class LocationProfile(
    val country: String,
    val timezone: String,
    val locale: String
)

data class Carrier(
    val name: String,              // "T-Mobile"
    val mccMnc: String,            // "310260"
    val countryCode: String ,      // "1" (phone code)
    val countryIso: String         // "US"
)
```

#### 1.2 Update SpoofRepository.generateValue()

```kotlin
fun generateValue(type: SpoofType): String {
    // For correlated values, generate as a group
    if (type in CORRELATED_TYPES) {
        return getCorrelatedValue(type)
    }
    
    // Independent values (unchanged)
    return when (type) {
        SpoofType.ANDROID_ID -> UUIDGenerator.generateAndroidId()
        // ... other independent values
    }
}

private fun getCorrelatedValue(type: SpoofType): String {
    // Generate full correlation group if not cached
    if (cachedCorrelation == null) {
        cachedCorrelation = generateCorrelatedIdentity()
    }
    
    // Return requested value from group
    return when (type) {
        SpoofType.IMEI -> cachedCorrelation!!.imei1
        SpoofType.IMSI -> cachedCorrelation!!.simProfile.imsi
        SpoofType.ICCID -> cachedCorrelation!!.simProfile.iccid
        SpoofType.CARRIER_NAME -> cachedCorrelation!!.simProfile.carrier.name
        SpoofType.CARRIER_MCC_MNC -> cachedCorrelation!!.simProfile.mccMnc
        // ...
    }
}
```

#### 1.3 Generator Updates

**IMSIGenerator** - Add carrier-based generation:
```kotlin
fun generate(mccMnc: String): String {
    val msinLength = 15 - mccMnc.length
    val msin = buildString {
        repeat(msinLength) {
            append(secureRandom.nextInt(10))
        }
    }
    return mccMnc + msin
}
```

**ICCIDGenerator** - Add country-based generation:
```kotlin
fun generate(countryCode: String = "01"): String {
    val mii = "89"
    val issuer = String.format("%02d", secureRandom.nextInt(100))
    val account = buildString { repeat(12) { append(secureRandom.nextInt(10)) } }
    val base = mii + countryCode + issuer + account
    val checkDigit = calculateLuhnCheckDigit(base)
    return base + checkDigit
}
```

---

### Phase 2: Medium Priority Fixes (P1)

#### 2.1 Location Correlation

```kotlin
fun generateLocationProfile(simCountry: String?): LocationProfile {
    // Option A: Match SIM country (realistic for home network)
    val country = simCountry ?: COUNTRIES.random()
    
    return LocationProfile(
        country = country,
        timezone = COUNTRY_TIMEZONES[country]!!.random(),
        locale = COUNTRY_LOCALES[country]!!.random()
    )
}
```

#### 2.2 MAC OUI Matching

```kotlin
fun generateWiFiMAC(manufacturer: String): String {
    val ouis = MANUFACTURER_OUIS[manufacturer.lowercase()]
    
    return if (ouis != null && secureRandom.nextBoolean()) {
        // 50%: Use manufacturer OUI
        MACGenerator.generateWithOUI(ouis.random())
    } else {
        // 50%: Use locally-administered (also realistic)
        MACGenerator.generate()
    }
}
```

---

## Testing Strategy

### Unit Tests

```kotlin
class CorrelationTest {
    @Test
    fun `IMSI MCC MNC matches carrier`() {
        val correlation = generateCorrelatedIdentity()
        val imsiMccMnc = correlation.simProfile.imsi.substring(0, 6)
        assertEquals(correlation.simProfile.mccMnc, imsiMccMnc)
    }
    
    @Test
    fun `device never has both IMEI and MEID`() {
        val correlation = generateCorrelatedIdentity()
        assertFalse(
            correlation.imei1 != null && correlation.meid != null,
            "Device cannot have both IMEI and MEID"
        )
    }
    
    @Test
    fun `carrier name matches IMSI MCC MNC`() {
        val correlation = generateCorrelatedIdentity()
        val carrier = findCarrierByMccMnc(correlation.simProfile.mccMnc)
        assertEquals(carrier?.name, correlation.simProfile.carrier.name)
    }
}
```

---

## Implementation Checklist

### Critical (P0) - MUST fix before release
- [ ] Remove simultaneous IMEI + MEID generation
- [ ] Create SIMProfile correlation (IMSI, ICCID, CARRIER)
- [ ] Update generators to accept correlation parameters
- [ ] Add carrier database with MCC/MNC
- [ ] Update SpoofRepository with correlated generation
- [ ] Add correlation validation tests

### High (P1) - Should fix soon
- [ ] Add LocationProfile correlation
- [ ] Match MAC OUI with device manufacturer
- [ ] Match serial pattern with device manufacturer
- [ ] Add timezone/locale databases by country

### Medium (P2) - Nice to have
- [ ] Add dual-SIM support (2 IMEIs)
- [ ] Add roaming scenario (IMSI country ≠ timezone)
- [ ] Add more carrier profiles (currently ~30)

---

## Conclusion

**Current Risk**: **HIGH** ⚠️⚠️⚠️

We have **2 critical issues** that create **easily detectable patterns**:
1. IMEI + MEID both present (impossible on real devices)
2. IMSI/ICCID/CARRIER values don't correlate (trivial to detect)

**Recommendation**: **Implement Phase 1 (P0 fixes) immediately** before any production release.

The good news: **7/17 values** are correctly independent. We just need to fix the **5 correlated groups**.

**Estimated effort**: 
- Phase 1 (Critical): 6-8 hours
- Phase 2 (High): 4-6 hours  
- Phase 3 (Medium): 2-4 hours

**Total**: ~12-18 hours to achieve **100% realistic value correlation** 🎯
