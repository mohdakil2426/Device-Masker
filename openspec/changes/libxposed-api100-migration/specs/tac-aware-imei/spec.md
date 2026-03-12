## ADDED Requirements

### Requirement: IMEI TAC matches device profile

When generating an IMEI for a package with a configured device profile, the generator SHALL use a TAC prefix from the preset's `tacPrefixes` list instead of random TAC generation.

#### Scenario: Pixel 9 Pro IMEI

- **WHEN** the active preset is `pixel_9_pro` with `tacPrefixes=["35414610", "35414611", "35414612"]`
- **THEN** the generated IMEI starts with one of those 8-digit TAC prefixes
- **THEN** the full 15-digit IMEI passes Luhn check digit validation

#### Scenario: No profile = generic TAC

- **WHEN** no device profile is configured for the package
- **THEN** the IMEI is generated with a generic TAC prefix (e.g., "35000000")

### Requirement: Luhn validation preserved

All generated IMEIs SHALL pass Luhn check digit validation regardless of TAC source.

#### Scenario: Check digit correctness

- **WHEN** an IMEI is generated with TAC "35414610" + random 6-digit SNR
- **THEN** the 15th digit is the correct Luhn check digit for the first 14 digits

### Requirement: SecureRandom for SNR portion

The 6-digit Serial Number Range (SNR) portion of the IMEI SHALL be generated using `java.security.SecureRandom`, not `java.util.Random`.

#### Scenario: Cryptographic randomness

- **WHEN** `imeiForPreset()` generates the SNR portion
- **THEN** `SecureRandom.nextInt()` is used for the random digits
