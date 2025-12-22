# Active Context: Device Masker

## Current Work Focus

### ✅ Complete: Value Generation Improvements (Dec 21, 2025)

**Status**: Complete
**Date**: December 21, 2025

Implemented comprehensive value generation improvements for maximum realism and data correlation.

#### Key Improvements Implemented

| # | Improvement | Status | Details |
|---|-------------|--------|---------|
| 1 | Canada to Country.ALL | ✅ Done | Added `Country("CA", "Canada", "🇨🇦", "1")` |
| 2 | Dual-SIM support | ✅ Done | `SIM_CARD_2` group + 5 new SpoofTypes |
| 3 | SIM-Location correlation | ✅ Done | Carrier change → auto-syncs timezone/locale/GPS |
| 4 | More countries/carriers | ✅ Done | 16 countries, 75+ carriers total |
| 5 | GPS correlation | ✅ Done | GPS coordinates tied to carrier country (city-level) |
| 6 | Device-Hardware sync | ✅ Done | Device Profile → syncs hardware identifiers |
| 7 | WiFi SSID patterns | ✅ Done | NETGEAR, TP-LINK, ASUS, ATT, Linksys patterns |
| 12 | TAC database expansion | ✅ Done | iPhone 16, Pixel 9, S25, Nothing Phone TACs |

#### Data Expansion Summary

| Category | Before | After |
|----------|--------|-------|
| Countries | 9 | **16** (+7 new) |
| US Carriers | 6 | **45+** |
| Total Carriers | ~30 | **75+** |
| US Timezones | 4 | **7** |
| US Area Codes | 0 | **100+** |
| TAC Prefixes | 30 | **65+** |
| GPS Bounds | 0 | **42 cities** |
| Dual-SIM Types | 0 | **5** |

#### New Countries Added

| Country | ISO | Carriers | GPS Cities |
|---------|-----|----------|------------|
| South Korea 🇰🇷 | KR | SK Telecom, KT, LG U+ | Seoul, Busan, Daegu |
| Brazil 🇧🇷 | BR | Vivo, Claro, Tim, Oi | São Paulo, Rio, Brasília |
| Russia 🇷🇺 | RU | MTS, Beeline, MegaFon, Tele2 | Moscow, St. Petersburg |
| Mexico 🇲🇽 | MX | Telcel, AT&T MX, Movistar | Mexico City, Monterrey |
| Indonesia 🇮🇩 | ID | Telkomsel, Indosat, XL, Tri | Jakarta, Surabaya |
| Saudi Arabia 🇸🇦 | SA | STC, Mobily, Zain | Riyadh, Jeddah, Mecca |
| UAE 🇦🇪 | AE | Etisalat, Du | Dubai, Abu Dhabi, Sharjah |

#### Files Modified

| File | Changes |
|------|---------|
| `Country.kt` | Added 7 new countries (KR, BR, RU, MX, ID, SA, AE) |
| `Carrier.kt` | Added 22+ carriers for new countries, expanded US to 45+ |
| `LocationProfile.kt` | Added GPS bounds, latitude/longitude fields, 7 new country data |
| `PhoneNumberGenerator.kt` | 100+ US area codes, Canadian area codes |
| `IMEIGenerator.kt` | 2024-2025 TAC prefixes (iPhone 16, Pixel 9, S25) |
| `SpoofType.kt` | Added `SIM_CARD_2` group + 5 SIM 2 types |
| `SpoofRepository.kt` | Dual-SIM generation, GPS sync on carrier change |
| `ExpressiveCard.kt` | Added `ExpressiveOutlinedCard` and `containerColor` support |

---

## Recent Changes (Dec 21, 2025)

### 🔧 Dual-SIM Support Added

- **New Correlation Group**: `CorrelationGroup.SIM_CARD_2`
- **New SpoofTypes**: IMSI_2, ICCID_2, PHONE_NUMBER_2, CARRIER_NAME_2, CARRIER_MCC_MNC_2
- **Repository Cache**: `cachedSIM2Profile` with independent generation
- **Reset Methods**: Updated `resetCorrelations()` and `resetCorrelationGroup()`

### 🌍 GPS Correlation Complete

- `LocationProfile` now has `latitude` and `longitude` fields
- GPS bounds defined for 42 major cities across 16 countries
- When carrier changes, GPS auto-updates to that country's city coordinates
- Prevents SIM/GPS country mismatch detection

### 📊 Carrier Database Expanded

- USA: 45+ carriers (T-Mobile variations, Verizon, AT&T, MVNOs, regionals)
- Added carriers for KR, BR, RU, MX, ID, SA, AE
- Total: 75+ carriers worldwide

---

## ✨ Expressive UI Overhaul (Dec 22, 2025)

### 🎨 App-wide Expressive Cards
- **Global Replacement**: Systematic migration of all `ElevatedCard`, `Card`, and `OutlinedCard` usages to `ExpressiveCard`.
- **Bouncy Touch Feedback**: Every interactive section (Home profile, Stats, Spoof Items, Settings, App List) now features a spring-animated scale-down on press.
- **Component Addition**: Introduced `ExpressiveOutlinedCard` to support outlined styles with expressive feedback.
- **Improved Interaction**: Shifted click logic from inner layouts to the `ExpressiveCard` itself for more robust visual feedback.

### 🧩 Components Updated
- `ProfileCard`, `StatCard`, `SpoofValueCard`, `AppListItem`, `SettingsSection`
- `ProfileSelectorCard` (Home Screen)
- `ModuleStatusCard` (Diagnostics Screen)
- All Category and Item sections in `ProfileDetailScreen`

---

## Build Status

| Module | Status | Last Build |
|--------|--------|------------|
| :common | ✅ SUCCESS | Dec 22, 2025 |
| :xposed | ✅ SUCCESS | Dec 22, 2025 |
| :app | ✅ SUCCESS | Dec 22, 2025 |
| Full APK | ✅ SUCCESS | Dec 22, 2025 |

---

## Next Steps

### Remaining Improvements (Optional)

1. **Carrier in Profile Creation** - Add carrier picker to profile creation dialog
2. **Dynamic Fingerprints** - Generate Build fingerprints dynamically
3. **Validation Warnings** - Show UI warnings for location mismatches
4. **Cell Info Hooks** - Add CellInfo/CellLocation Xposed hooks
5. **Dual-SIM UI** - Add SIM 2 section to ProfileDetailScreen

### Device Testing

- [ ] Test new countries on device
- [ ] Verify GPS coordinates work correctly
- [ ] Test dual-SIM generation (when UI is added)
- [ ] Verify carrier selection correlation

---

## Important Files Reference

| File | Purpose |
|------|---------|
| `common/.../models/Country.kt` | Country database (16 countries) |
| `common/.../models/Carrier.kt` | Carrier database (75+ carriers) |
| `common/.../models/LocationProfile.kt` | Location + GPS generation |
| `common/.../generators/PhoneNumberGenerator.kt` | Phone number generation |
| `common/.../generators/IMEIGenerator.kt` | IMEI generation with TACs |
| `common/.../SpoofType.kt` | Spoof types including dual-SIM |
| `app/.../repository/SpoofRepository.kt` | Value generation + correlation |
