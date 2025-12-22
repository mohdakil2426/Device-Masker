# Project Brief: Device Masker

## Overview

**Device Masker** is an open-source LSPosed/Xposed module for Android that provides comprehensive device identifier spoofing with a robust anti-detection layer. The module is designed to protect user privacy by intercepting and spoofing device identifiers while preventing apps from detecting the hook injection.

## Core Philosophy

> **"Do one thing excellently"** - Spoof device identifiers and hide the injection, nothing more.

Device Masker focuses exclusively on spoofing and anti-detection. Root hiding, SafetyNet bypass, and Play Integrity bypass are intentionally out of scope and delegated to companion modules (Shamiko, PIF, Tricky Store).

## Project Goals

### Primary Goals
1. **Comprehensive Device Spoofing**: Spoof 24+ device identifiers including IMEI, Serial, MAC, Android ID, GSF ID, Advertising ID, Build properties, and location data
2. **Robust Anti-Detection**: Prevent target apps from detecting Xposed/LSPosed hooks through stack trace filtering, ClassLoader interception, and /proc/maps hiding
3. **Per-App Configuration**: Allow users to enable/disable spoofing per app and assign different groups to different apps
4. **Beautiful Modern UI**: Material 3 Expressive design with dynamic colors, AMOLED optimization, and spring animations
5. **Valid Value Generation**: Generate spoofed values that pass validation (Luhn-valid IMEI, unicast MAC, realistic fingerprints)

### Secondary Goals
1. Group import/export for backup
2. Diagnostics screen for verifying spoofing
3. Detailed logging for troubleshooting

## Success Criteria

| Criteria | Measurement |
|----------|-------------|
| Module Recognition | Appears in LSPosed Manager correctly |
| IMEI Spoofing | IMEI checker apps show spoofed value |
| Anti-Detection | RootBeer/SafetyNet Helper don't detect hooks |
| UI Polish | Smooth animations, responsive layout |
| Compatibility | Works on Android 8.0-16 (API 26-36) |
| Value Validity | All generated values pass format validation |

## Scope Definition

### ✅ IN SCOPE
- **Device Spoofing**: IMEI, IMSI, Serial Number, Hardware ID, Device ID
- **Network Spoofing**: MAC Address (WiFi + Bluetooth), SSID, Carrier Info
- **Advertising Spoofing**: GSF ID, Advertising ID, Media DRM ID, Android ID
- **System Spoofing**: Build Properties, Manufacturer, Model, Fingerprint
- **Location Spoofing**: GPS Coordinates, Timezone, Language
- **Anti-Detection**: Stack Trace Hiding, ClassLoader Hiding, /proc/maps Hiding
- **UI/UX**: Material 3 Expressive, Dynamic Colors, AMOLED Theme
- **Data Management**: Group Management, Per-App Configuration

### ❌ OUT OF SCOPE (External Modules)
| Feature | Recommended Module |
|---------|-------------------|
| Root Detection Bypass | Shamiko, Zygisk-Next |
| SafetyNet Bypass | Play Integrity Fix (PIF) |
| Play Integrity Bypass | Tricky Store, PIF |
| Bootloader Status | Not bypassable (hardware) |
| Banking App Compatibility | Combination of external modules |

## Target Users

1. **Privacy-Conscious Users**: People who want to protect their device fingerprint from tracking
2. **Security Researchers**: Testing application behavior with different device identities
3. **Developers**: Learning about Android security and hook frameworks
4. **Multi-Account Users**: Need different device identities for different apps

## Project Timeline

| Phase | Description | Duration |
|-------|-------------|----------|
| Phase 1 | Core Infrastructure | Week 1-2 |
| Phase 2 | Device Spoofing Hooks | Week 2-3 |
| Phase 3 | Anti-Detection Layer | Week 3-4 |
| Phase 4 | Data Management | Week 4-5 |
| Phase 5 | User Interface | Week 5-7 |
| Phase 6 | Testing & Polish | Week 7-8 |

**Total Estimated Duration**: 8 weeks (part-time) or 4 weeks (full-time)

## Key Stakeholders

- **Developer**: AstrixForge (com.astrixforge.devicemasker)
- **License**: GPL-3.0 (Open Source for Educational Purposes)
- **Target Framework**: LSPosed 1.10.2+ on Magisk 30.6+

## Legal Disclaimer

This module is for **educational and security research purposes only**. Users are responsible for ensuring compliance with applicable laws and terms of service.

**NOT for use in**:
- Fraudulent transactions
- Bypassing security for unauthorized access
- Violating terms of service
- Any illegal activities
