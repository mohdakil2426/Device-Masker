# Comprehensive Spoofing Values Analysis Report
## Device Masker - Making Spoof Values 100% Realistic

**Date**: December 20, 2025  
**Author**: Deep Research Analysis  
**Status**: Actionable Recommendations

---

## Executive Summary

This document provides a comprehensive analysis of how to make Device Masker's spoofed values 100% realistic and working. Based on research from successful projects like PlayIntegrityFix, HMA-OSS, and other Xposed modules, this report identifies issues in current implementations and provides concrete fixes.

---

## Table of Contents

1. [IMEI Spoofing](#1-imei-spoofing)
2. [Serial Number Spoofing](#2-serial-number-spoofing)
3. [Android ID Spoofing](#3-android-id-spoofing)
4. [MAC Address Spoofing](#4-mac-address-spoofing)
5. [Build Fingerprint Spoofing](#5-build-fingerprint-spoofing)
6. [Advertising ID Spoofing](#6-advertising-id-spoofing)
7. [GSF ID Spoofing](#7-gsf-id-spoofing)
8. [Current Issues & Fixes](#8-current-issues--fixes)
9. [Implementation Recommendations](#9-implementation-recommendations)

---

## 1. IMEI Spoofing

### 1.1 Standard IMEI Format

**Structure**: 15 digits total
```
AA-BBBBBB-CCCCCC-D
│  │       │      └─ Check Digit (Luhn algorithm)
│  │       └──────── Serial Number (6 digits, SNR)
│  └──────────────── Type Allocation Code (6 digits, TAC)
└─────────────────── Reporting Body Identifier (2 digits)
```

### 1.2 Valid TAC Prefixes by Manufacturer

| Manufacturer | TAC Prefixes | Example Full TAC |
|--------------|--------------|------------------|
| Samsung | 35, 35XXXX | 353456, 352345 |
| Apple | 35, 01 | 356789, 014567 |
| Google/Pixel | 35, 98 | 353393, 988765 |
| Xiaomi | 86, 86XXXX | 862345, 867890 |
| OnePlus | 86, 35 | 867891, 355678 |
| Huawei | 86, 35 | 864523, 352341 |
| Sony | 35 | 354321 |
| LG | 35 | 353245 |

### 1.3 Current Implementation Issues

```kotlin
// CURRENT (DeviceHooker.kt line 244-250)
private fun generateImei(): String {
    val tac = listOf("35", "86", "01", "45").random()
    val serial = (1000000..9999999).random().toString()
    val base = tac + serial.padStart(12, '0').take(12)  // ❌ WRONG: TAC is 6 digits, not 2
    return base + calculateLuhn(base)
}
```

### 1.4 Correct Implementation

```kotlin
private fun generateImei(): String {
    // Valid TAC prefixes (6 digits) from real devices
    val validTacs = listOf(
        "353456", "353393", "356789", "352345",  // Samsung/Pixel
        "867890", "862345", "864523", "867891",  // Xiaomi/OnePlus
        "013456", "014567", "015678",            // Apple
    )
    val tac = validTacs.random()
    
    // Serial number (6 digits)
    val serial = String.format("%06d", (0..999999).random())
    
    // Base is TAC (6) + Serial (6) = 12 digits
    val base = tac + serial
    
    // Calculate Luhn check digit
    val checkDigit = calculateLuhn(base)
    
    return base + checkDigit  // 15 digits total
}

private fun calculateLuhn(digits: String): Char {
    var sum = 0
    digits.reversed().forEachIndexed { index, char ->
        var digit = char.digitToInt()
        if (index % 2 == 0) {  // Even positions (from right) are doubled
            digit *= 2
            if (digit > 9) digit -= 9
        }
        sum += digit
    }
    return ((10 - (sum % 10)) % 10).digitToChar()
}
```

### 1.5 Methods to Hook

| Method | API Level | Notes |
|--------|-----------|-------|
| `TelephonyManager.getDeviceId()` | < 26 | Deprecated but still used |
| `TelephonyManager.getDeviceId(int slot)` | 23+ | For dual SIM |
| `TelephonyManager.getImei()` | 26+ | Primary method |
| `TelephonyManager.getImei(int slot)` | 26+ | Dual SIM |
| `TelephonyManager.getMeid()` | 26+ | CDMA devices |
| `TelephonyManager.getMeid(int slot)` | 26+ | Dual SIM CDMA |

---

## 2. Serial Number Spoofing

### 2.1 Format Requirements

**Android CTS Requirement**: 6-20 alphanumeric characters  
**Pattern**: `^([0-9A-Za-z]{6,20})$`

### 2.2 Manufacturer-Specific Formats

| Manufacturer | Format | Example |
|--------------|--------|---------|
| Samsung | `RxxYxxxxxxxxx` (12+ chars) | `R58R12345678` |
| Google Pixel | `xxxxxxxxxxxxxxxx` (16 chars hex) | `FA6AB0301534` |
| Xiaomi | `xxxxxxxx-xxxx-xxxx` | `12345678-1234-5678` |
| OnePlus | `xxxxxxxxxxxxxxxx` (16 chars) | `42fc5abcdef12345` |
| General | Alphanumeric 8-16 chars | `ABC123DEF456` |

### 2.3 Current Implementation Issues

```kotlin
// CURRENT (line 265-268)
private fun generateSerial(): String {
    val chars = "0123456789ABCDEF"
    return (1..16).map { chars.random() }.joinToString("")
}
// ❌ ISSUE: Only hex chars, no lowercase variation
```

### 2.4 Correct Implementation

```kotlin
private fun generateSerial(): String {
    // Follow manufacturer patterns
    val patterns = listOf(
        { generateSamsungSerial() },
        { generatePixelSerial() },
        { generateGenericSerial() },
    )
    return patterns.random()()
}

private fun generateSamsungSerial(): String {
    // Samsung format: R + 2 chars + year indicator + 8 digits
    val prefix = "R${('0'..'9').random()}${('0'..'9').random()}"
    val yearIndicator = ('A'..'Z').filter { it !in listOf('I', 'O', 'Q') }.random()
    val serial = (1..8).map { ('0'..'9').random() }.joinToString("")
    return prefix + yearIndicator + serial
}

private fun generatePixelSerial(): String {
    // Pixel format: 6-char prefix + 10 alphanumeric
    val chars = "0123456789ABCDEF"
    return (1..16).map { chars.random() }.joinToString("")
}

private fun generateGenericSerial(): String {
    val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val length = (8..16).random()
    return (1..length).map { chars.random() }.joinToString("")
}
```

### 2.5 Properties to Hook

| Property | Notes |
|----------|-------|
| `ro.serialno` | Primary |
| `ro.boot.serialno` | Boot-set value |
| `ril.serialnumber` | Samsung pre-Android 9 |
| `Build.getSerial()` | API 26+ |

---

## 3. Android ID Spoofing

### 3.1 Format Requirements

- **Type**: 64-bit number
- **Representation**: 16-character lowercase hexadecimal string
- **Example**: `32ff79aa2427be03`

### 3.2 Important Notes

- Since Android 8.0 (API 26), Android ID is scoped per app signing key
- Different apps see different Android IDs on same device
- Resets on factory reset

### 3.3 Current Implementation ✅ CORRECT

```kotlin
private fun generateAndroidId(): String {
    val chars = "0123456789abcdef"
    return (1..16).map { chars.random() }.joinToString("")
}
// ✅ Correct: 16 lowercase hex chars
```

---

## 4. MAC Address Spoofing

### 4.1 Format Requirements

- **Total bytes**: 6 (48 bits)
- **Format**: `XX:XX:XX:XX:XX:XX`
- **Critical bits**:
  - **Bit 0 of byte 0 (LSB)**: Unicast (0) vs Multicast (1)
  - **Bit 1 of byte 0**: Universal (0) vs Local (1)

### 4.2 Locally Administered MAC Rules

For spoofed MAC addresses, the second hex digit should be: **2, 6, A, or E**

```
Valid locally administered addresses:
x2:xx:xx:xx:xx:xx
x6:xx:xx:xx:xx:xx
xA:xx:xx:xx:xx:xx
xE:xx:xx:xx:xx:xx
```

### 4.3 Current Implementation ✅ MOSTLY CORRECT

```kotlin
private fun generateMac(): String {
    val bytes = ByteArray(6)
    java.util.Random().nextBytes(bytes)
    // Clear multicast bit, set local bit
    bytes[0] = ((bytes[0].toInt() and 0xFC) or 0x02).toByte()
    return bytes.joinToString(":") { String.format("%02X", it) }
}
// ✅ Correctly sets locally administered bit
```

### 4.4 Improvement: Use Real OUIs

```kotlin
private fun generateRealisticMac(): String {
    // Real manufacturer OUIs for more realistic values
    val realOuis = listOf(
        "00:1A:11", // Google
        "F8:E4:E3", // Samsung
        "AC:37:43", // Apple
        "00:9A:CD", // Xiaomi
        "94:65:2D", // OnePlus
        "B4:F1:DA", // LG
        "00:0A:F5", // Airgo Networks (common)
    )
    
    val oui = realOuis.random()
    val devicePart = (1..3).map { 
        String.format("%02X", (0..255).random()) 
    }.joinToString(":")
    
    return "$oui:$devicePart"
}

// OR for locally administered (untraceable):
private fun generateLocalMac(): String {
    val bytes = ByteArray(6)
    java.security.SecureRandom().nextBytes(bytes)
    bytes[0] = ((bytes[0].toInt() and 0xFC) or 0x02).toByte()
    return bytes.joinToString(":") { String.format("%02X", it) }
}
```

---

## 5. Build Fingerprint Spoofing

### 5.1 Format Structure

```
BRAND/PRODUCT/DEVICE:VERSION.RELEASE/BUILD_ID/VERSION.INCREMENTAL:TYPE/TAGS
```

**Example**:
```
google/raven/raven:14/AP2A.240805.005/12025142:user/release-keys
samsung/a52qnsxx/a52q:13/TP1A.220624.014/A525FXXS5DWK1:user/release-keys
```

### 5.2 Valid Components

| Component | Description | Example |
|-----------|-------------|---------|
| BRAND | Manufacturer | `google`, `samsung`, `oneplus` |
| PRODUCT | Product codename | `raven`, `beyond2q`, `guacamole` |
| DEVICE | Device codename | `raven`, `beyond2`, `OnePlus7Pro` |
| VERSION.RELEASE | Android version | `14`, `13`, `12` |
| BUILD_ID | Build identifier | `AP2A.240805.005`, `TP1A.220624.014` |
| VERSION.INCREMENTAL | Build number | `12025142`, `G960U1UEU9FUE4` |
| TYPE | Build type | `user` (production), `userdebug` |
| TAGS | Signing keys | `release-keys` (official), `test-keys` (custom) |

### 5.3 CRITICAL: Fingerprint Consistency

**All related fields MUST be consistent**:

```kotlin
// Example: Pixel 7 Pro fingerprint profile
data class DeviceProfile(
    val fingerprint: String = "google/cheetah/cheetah:14/AP2A.240805.005/12025142:user/release-keys",
    val manufacturer: String = "Google",
    val brand: String = "google",
    val model: String = "Pixel 7 Pro",
    val device: String = "cheetah",
    val product: String = "cheetah",
    val board: String = "cheetah",
    val securityPatch: String = "2024-08-05",
    val firstApiLevel: Int = 33,  // Important for attestation
)
```

### 5.4 Current Implementation Issues

```kotlin
// CURRENT: Fields are hooked independently
// ❌ ISSUE: No consistency between fields
hookBuildFields() // Hooks fields individually without correlation
```

### 5.5 Recommended Implementation

```kotlin
// Complete device profiles for consistency
object DeviceProfiles {
    val PIXEL_8_PRO = DeviceProfile(
        brand = "google",
        manufacturer = "Google",
        product = "husky",
        device = "husky",
        model = "Pixel 8 Pro",
        board = "husky",
        fingerprint = "google/husky/husky:14/AD1A.240530.047/11777660:user/release-keys",
        securityPatch = "2024-05-30",
    )
    
    val SAMSUNG_S24_ULTRA = DeviceProfile(
        brand = "samsung",
        manufacturer = "samsung",
        product = "dm3q",
        device = "dm3q",
        model = "SM-S928B",
        board = "pineapple",
        fingerprint = "samsung/dm3qxxx/dm3q:14/UP1A.231005.007/S928BXXS2AXL5:user/release-keys",
        securityPatch = "2024-12-01",
    )
}
```

---

## 6. Advertising ID Spoofing

### 6.1 Format Requirements

- **Type**: UUID v4
- **Format**: `xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx`
- **Where**: 
  - `x` is any hex digit
  - `4` indicates version 4
  - `y` is 8, 9, A, or B

### 6.2 Current Implementation ✅ CORRECT

```kotlin
private val fallbackAdvertisingId by lazy { java.util.UUID.randomUUID().toString() }
// ✅ Correct: Uses standard UUID generation
```

---

## 7. GSF ID Spoofing

### 7.1 Format Requirements

- **Type**: 64-bit integer
- **Representation**: 16-character hexadecimal (can be uppercase or lowercase)
- **Example**: `3A7D8F2BC1E54690`

### 7.2 Current Implementation ✅ CORRECT

```kotlin
private val fallbackGsfId by lazy { generateHexId(16) }

private fun generateHexId(length: Int): String {
    val chars = "0123456789abcdef"
    return (1..length).map { chars.random() }.joinToString("")
}
// ✅ Correct: 16 hex characters
```

---

## 8. Current Issues & Fixes

### 8.1 Summary of Issues Found

| Issue | Severity | Location | Fix Required |
|-------|----------|----------|--------------|
| IMEI TAC only 2 digits | 🔴 HIGH | DeviceHooker.kt:246 | Use full 6-digit TAC |
| IMEI Luhn calculation timing | 🟡 MEDIUM | DeviceHooker.kt:252 | Fix algorithm position logic |
| Serial number format too simple | 🟡 MEDIUM | DeviceHooker.kt:265 | Use manufacturer patterns |
| Build fields not correlated | 🔴 HIGH | SystemHooker.kt | Use complete device profiles |
| Missing IMSI/ICCID validation | 🟡 MEDIUM | DeviceHooker.kt:275-280 | Add proper format validation |
| MAC uses java.util.Random | 🟡 MEDIUM | NetworkHooker.kt:168 | Use SecureRandom |

### 8.2 Critical Fixes Required

#### Fix 1: IMEI Generator

**File**: `xposed/src/main/kotlin/.../hooker/DeviceHooker.kt`

```kotlin
private fun generateImei(): String {
    // Real 8-digit TACs from popular devices (first 8 digits of IMEI structure)
    val validTacs = listOf(
        "35390909", // Samsung Galaxy S series
        "35269410", // Samsung Galaxy Note
        "35473108", // Google Pixel
        "35327410", // Apple iPhone
        "86761604", // Xiaomi
        "86901604", // OnePlus
        "35451510", // Sony Xperia
        "35854110", // LG
    )
    val tac = validTacs.random()
    
    // Serial number portion (6 digits)
    val serial = String.format("%06d", (0..999999).random())
    
    // Base = TAC (8) + Serial (6) = 14 digits
    val base = tac.take(8) + serial.take(6)
    
    // Calculate Luhn check digit (15th digit)
    return base + calculateLuhnCheckDigit(base)
}

private fun calculateLuhnCheckDigit(number: String): Char {
    var sum = 0
    for ((index, char) in number.withIndex()) {
        var digit = char.digitToInt()
        // Double every other digit starting from rightmost
        if ((number.length - index) % 2 == 0) {
            digit *= 2
            if (digit > 9) digit -= 9
        }
        sum += digit
    }
    return ((10 - (sum % 10)) % 10).digitToChar()
}
```

#### Fix 2: Consistent Device Profiles

**New File**: `common/src/main/kotlin/.../DeviceProfiles.kt`

```kotlin
package com.astrixforge.devicemasker.common

import kotlinx.serialization.Serializable

@Serializable
data class DeviceProfile(
    val name: String,
    val brand: String,
    val manufacturer: String,
    val product: String,
    val device: String,
    val model: String,
    val board: String,
    val fingerprint: String,
    val securityPatch: String,
) {
    companion object {
        val PRESETS = listOf(
            DeviceProfile(
                name = "Google Pixel 8 Pro",
                brand = "google",
                manufacturer = "Google",
                product = "husky",
                device = "husky",
                model = "Pixel 8 Pro",
                board = "husky",
                fingerprint = "google/husky/husky:14/AD1A.240530.047/11777660:user/release-keys",
                securityPatch = "2024-05-30",
            ),
            DeviceProfile(
                name = "Samsung Galaxy S24 Ultra",
                brand = "samsung",
                manufacturer = "samsung",
                product = "dm3q",
                device = "dm3q",
                model = "SM-S928B",
                board = "pineapple",
                fingerprint = "samsung/dm3qxxx/dm3q:14/UP1A.231005.007/S928BXXS2AXL5:user/release-keys",
                securityPatch = "2024-12-01",
            ),
            DeviceProfile(
                name = "OnePlus 12",
                brand = "OnePlus",
                manufacturer = "OnePlus",
                product = "waffle",
                device = "CPH2581",
                model = "CPH2581",
                board = "pineapple",
                fingerprint = "OnePlus/CPH2581/OP5D13L1:14/UP1A.231005.007/U.R4T3.15eeb8c-1:user/release-keys",
                securityPatch = "2024-10-05",
            ),
        )
    }
}
```

---

## 9. Implementation Recommendations

### 9.1 Priority Order

| Priority | Task | Impact |
|----------|------|--------|
| 🔴 P0 | Fix IMEI generator (6-digit TAC) | Apps detect invalid IMEI |
| 🔴 P0 | Add device profile presets | Fingerprint consistency |
| 🟡 P1 | Use SecureRandom for cryptographic values | Predictability |
| 🟡 P1 | Add IMSI/ICCID format validation | Banking apps |
| 🟢 P2 | Add more TAC prefixes | Variety |
| 🟢 P2 | Add more device profiles | User options |

### 9.2 Testing Recommendations

1. **Use Device Info apps** to verify spoofed values:
   - AIDA64
   - DevCheck Device Info
   - CPU-Z

2. **Test with detection apps**:
   - SafetyNet Test
   - Play Integrity API Checker
   - RootBeer (root detection)

3. **Verify format compliance**:
   - IMEI: Use online IMEI validators
   - MAC: Check locally administered bit
   - Fingerprint: Match format pattern

### 9.3 Reference Projects

| Project | URL | Key Feature |
|---------|-----|-------------|
| PlayIntegrityFix | github.com/chiteroman/PlayIntegrityFix | Device fingerprint spoofing |
| HMA-OSS | github.com/frknkrc44/HMA-OSS | App hiding, AIDL architecture |
| XPrivacyLua | github.com/M66B/XPrivacyLua | Comprehensive privacy hooks |
| DeviceIdMasker | GitHub various | IMEI/Serial spoofing patterns |

---

## 10. Appendix: Complete Realistic Value Generator

```kotlin
object RealisticValueGenerator {
    
    private val secureRandom = java.security.SecureRandom()
    
    // ═══════════════════════════════════════════════════════════
    // IMEI (15 digits, Luhn-valid)
    // ═══════════════════════════════════════════════════════════
    
    private val VALID_TACS = listOf(
        "35390909", "35269410", "35473108", "35327410",
        "86761604", "86901604", "35451510", "35854110",
    )
    
    fun generateImei(): String {
        val tac = VALID_TACS.random()
        val serial = String.format("%06d", secureRandom.nextInt(1000000))
        val base = tac.take(8) + serial.take(6)
        return base + calculateLuhn(base)
    }
    
    fun generateMeid(): String = generateImei().take(14)
    
    // ═══════════════════════════════════════════════════════════
    // Serial Number (6-20 alphanumeric)
    // ═══════════════════════════════════════════════════════════
    
    fun generateSerial(): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val length = 12 + secureRandom.nextInt(5) // 12-16 chars
        return (1..length).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
    }
    
    // ═══════════════════════════════════════════════════════════
    // Android ID (16 lowercase hex)
    // ═══════════════════════════════════════════════════════════
    
    fun generateAndroidId(): String {
        val bytes = ByteArray(8)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { String.format("%02x", it) }
    }
    
    // ═══════════════════════════════════════════════════════════
    // MAC Address (locally administered)
    // ═══════════════════════════════════════════════════════════
    
    fun generateMac(): String {
        val bytes = ByteArray(6)
        secureRandom.nextBytes(bytes)
        bytes[0] = ((bytes[0].toInt() and 0xFC) or 0x02).toByte()
        return bytes.joinToString(":") { String.format("%02X", it) }
    }
    
    // ═══════════════════════════════════════════════════════════
    // UUID v4 (Advertising ID)
    // ═══════════════════════════════════════════════════════════
    
    fun generateUUID(): String = java.util.UUID.randomUUID().toString()
    
    // ═══════════════════════════════════════════════════════════
    // GSF ID (16 hex characters)
    // ═══════════════════════════════════════════════════════════
    
    fun generateGsfId(): String {
        val bytes = ByteArray(8)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { String.format("%02x", it) }
    }
    
    // ═══════════════════════════════════════════════════════════
    // IMSI (15 digits: MCC + MNC + MSIN)
    // ═══════════════════════════════════════════════════════════
    
    private val VALID_MCC_MNC = listOf(
        "310260", // T-Mobile US
        "311480", // Verizon US
        "310410", // AT&T US
        "302720", // Rogers Canada
        "234150", // Vodafone UK
        "262010", // T-Mobile Germany
    )
    
    fun generateImsi(): String {
        val mccMnc = VALID_MCC_MNC.random()
        val msin = String.format("%09d", secureRandom.nextLong() % 1000000000L)
        return mccMnc + msin
    }
    
    // ═══════════════════════════════════════════════════════════
    // ICCID (19-20 digits)
    // ═══════════════════════════════════════════════════════════
    
    fun generateIccid(): String {
        val prefix = "8901" // Standard telecom prefix
        val issuer = String.format("%02d", secureRandom.nextInt(100))
        val account = String.format("%012d", secureRandom.nextLong() % 1000000000000L)
        val base = prefix + issuer + account
        return base + calculateLuhn(base)
    }
    
    // ═══════════════════════════════════════════════════════════
    // Helper: Luhn Check Digit
    // ═══════════════════════════════════════════════════════════
    
    private fun calculateLuhn(number: String): Char {
        var sum = 0
        for ((index, char) in number.withIndex()) {
            var digit = char.digitToInt()
            if ((number.length - index) % 2 == 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return ((10 - (sum % 10)) % 10).digitToChar()
    }
}
```

---

## Conclusion

To make Device Masker's spoofing 100% realistic and working:

1. **Fix IMEI generation** with proper 8-digit TAC prefixes and correct Luhn calculation
2. **Implement device profiles** for consistent fingerprint + Build.* field spoofing
3. **Use SecureRandom** instead of java.util.Random for cryptographic quality
4. **Add validation** to ensure generated values pass format checks
5. **Test thoroughly** with device info and detection apps

Following these recommendations will significantly improve the module's ability to spoof values that pass validation by apps and services.
