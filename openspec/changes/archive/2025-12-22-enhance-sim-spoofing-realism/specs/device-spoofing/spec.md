# device-spoofing Specification Delta

## ADDED Requirements

### Requirement: SIM Country ISO Spoofing
The module SHALL intercept SIM country ISO retrieval and return spoofed values.

#### Scenario: TelephonyManager.getSimCountryIso() Hook
- **WHEN** a target app calls `TelephonyManager.getSimCountryIso()`
- **THEN** the spoofed SIM country ISO code is returned (e.g., "in" for India)
- **AND** the returned value correlates with the carrier's country

#### Scenario: SubscriptionInfo.getCountryIso() Hook
- **WHEN** a target app calls `SubscriptionInfo.getCountryIso()`
- **THEN** the spoofed country ISO is returned matching the SIM profile

---

### Requirement: Network Country ISO Spoofing
The module SHALL intercept network country ISO retrieval and return spoofed values.

#### Scenario: TelephonyManager.getNetworkCountryIso() Hook
- **WHEN** a target app calls `TelephonyManager.getNetworkCountryIso()`
- **THEN** the spoofed network country ISO is returned
- **AND** the value matches SIM country ISO (non-roaming assumed)

---

### Requirement: SIM Operator Name Spoofing
The module SHALL intercept SIM operator name retrieval and return spoofed values.

#### Scenario: TelephonyManager.getSimOperatorName() Hook
- **WHEN** a target app calls `TelephonyManager.getSimOperatorName()`
- **THEN** the spoofed operator name is returned (e.g., "Airtel")
- **AND** the value correlates with the carrier's MCC/MNC

---

### Requirement: Network Operator Spoofing
The module SHALL intercept network operator retrieval and return the MCC+MNC string.

#### Scenario: TelephonyManager.getNetworkOperator() Hook
- **WHEN** a target app calls `TelephonyManager.getNetworkOperator()`
- **THEN** the spoofed network operator string is returned (e.g., "40410")
- **AND** the value matches the carrier's MCC+MNC combination

---

### Requirement: Carrier Preset System
The module SHALL provide predefined carrier presets for consistent SIM profile generation.

#### Scenario: Generate Profile from Carrier Preset
- **WHEN** a user selects a carrier preset (e.g., "Airtel Delhi")
- **THEN** all SIM values are generated using that carrier's configuration
- **AND** IMSI prefix matches carrier MCC+MNC
- **AND** ICCID uses carrier-specific issuer code
- **AND** phone number uses country-appropriate format
- **AND** all country/operator values correlate

#### Scenario: Random Carrier Selection
- **WHEN** no specific carrier is selected
- **THEN** a random carrier from the database is selected
- **AND** all SIM values correlate with the selected carrier

---

### Requirement: Enhanced ICCID Generation
The module SHALL generate ICCIDs with carrier-specific issuer codes.

#### Scenario: ICCID Issuer Code Correlation
- **GIVEN** a carrier preset is selected
- **WHEN** an ICCID is generated
- **THEN** the ICCID format is: 89 + country_code + issuer_code + serial + luhn_checksum
- **AND** the issuer code is carrier-specific (not random)

#### Scenario: India ICCID Format
- **GIVEN** an India carrier is selected
- **WHEN** an ICCID is generated
- **THEN** the ICCID starts with "8991" (89 = telecom, 91 = India)
- **AND** NOT "899110" or other incorrect formats

---

## MODIFIED Requirements

### Requirement: SIM Serial Spoofing
The module SHALL intercept SIM serial number retrieval and return spoofed values that correlate with the carrier profile.

#### Scenario: getSimSerialNumber Hook
- **WHEN** a target app calls `TelephonyManager.getSimSerialNumber()`
- **THEN** the spoofed 19-20 digit SIM serial (ICCID) is returned
- **AND** the ICCID uses correct country prefix for the carrier
- **AND** the ICCID includes carrier-specific issuer code
- **AND** the ICCID passes Luhn checksum validation

---

### Requirement: IMSI Spoofing
The module SHALL intercept IMSI retrieval and return spoofed values matching the carrier profile.

#### Scenario: getSubscriberId Hook
- **WHEN** a target app calls `TelephonyManager.getSubscriberId()`
- **THEN** the spoofed 15-digit IMSI is returned
- **AND** the IMSI prefix matches the carrier's MCC+MNC
- **AND** the MSIN portion is randomly generated

#### Scenario: India IMSI Format
- **GIVEN** an India carrier is selected (e.g., Airtel Delhi)
- **WHEN** an IMSI is generated
- **THEN** the IMSI starts with carrier's MCC+MNC (e.g., "40410")
- **AND** the total length is exactly 15 digits

---

### Requirement: Carrier Name Spoofing
The module SHALL intercept carrier name retrieval and return values matching the SIM profile.

#### Scenario: getSimOperator Hook
- **WHEN** a target app calls `TelephonyManager.getSimOperator()`
- **THEN** the spoofed MCC+MNC string is returned
- **AND** the value matches the carrier profile's mcc+mnc

#### Scenario: Carrier Name Consistency
- **GIVEN** a user has selected or generated a SIM profile
- **WHEN** carrier name APIs are called
- **THEN** all carrier-related values (name, MCC, MNC) correlate
