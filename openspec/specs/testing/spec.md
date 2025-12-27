# testing Specification

## Purpose
TBD - created by archiving change optimize-architecture. Update Purpose after archive.
## Requirements
### Requirement: Value Generator Unit Tests

The common module SHALL have unit tests for all value generators to ensure correctness.

#### Scenario: Test Execution
- **WHEN** `./gradlew :common:test` is run
- **THEN** all generator tests execute
- **AND** test results are reported with pass/fail status

---

### Requirement: IMEI Generator Testing

The IMEIGenerator SHALL be validated by unit tests.

#### Scenario: IMEI Length Validation
- **WHEN** an IMEI is generated
- **THEN** the test verifies it is exactly 15 digits
- **AND** all characters are numeric

#### Scenario: IMEI Luhn Checksum Validation
- **WHEN** an IMEI is generated
- **THEN** the test verifies it passes Luhn checksum validation
- **AND** the check digit is correct

#### Scenario: IMEI TAC Prefix Validation
- **WHEN** an IMEI is generated
- **THEN** the test verifies it starts with a valid TAC prefix (35, 86, 01, 45, 49)
- **AND** the prefix represents a realistic device manufacturer

---

### Requirement: MAC Address Generator Testing

The MACGenerator SHALL be validated by unit tests.

#### Scenario: MAC Format Validation
- **WHEN** a MAC address is generated
- **THEN** the test verifies format matches XX:XX:XX:XX:XX:XX
- **AND** all characters are uppercase hexadecimal or colons

#### Scenario: MAC Unicast Bit Validation
- **WHEN** a MAC address is generated
- **THEN** the test verifies the unicast bit is set correctly
- **AND** the LSB of the first byte equals 0 (unicast, not multicast)

---

### Requirement: Android ID Generator Testing

The AndroidIdGenerator SHALL be validated by unit tests.

#### Scenario: Android ID Length Validation
- **WHEN** an Android ID is generated
- **THEN** the test verifies it is exactly 16 characters
- **AND** all characters are lowercase hexadecimal

#### Scenario: Android ID Format Validation
- **WHEN** an Android ID is generated
- **THEN** the test verifies it matches regex `[0-9a-f]{16}`
- **AND** no uppercase letters or special characters are present

---

### Requirement: Serial Number Generator Testing

The SerialGenerator SHALL be validated by unit tests.

#### Scenario: Serial Length Validation
- **WHEN** a serial number is generated
- **THEN** the test verifies length is between 8 and 16 characters

#### Scenario: Serial Format Validation
- **WHEN** a serial number is generated
- **THEN** the test verifies it is alphanumeric only
- **AND** no special characters are present

---

### Requirement: Test Repeatability

All generator tests SHALL run multiple iterations to ensure statistical correctness.

#### Scenario: Multiple Iteration Testing
- **WHEN** a generator test runs
- **THEN** it generates at least 100 values
- **AND** all 100 values pass validation
- **AND** no single failure causes false positive

#### Scenario: Determinism Independence
- **WHEN** tests run repeatedly
- **THEN** results are consistent across runs
- **AND** random seed does not cause flaky tests

