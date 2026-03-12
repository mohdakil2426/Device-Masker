## ADDED Requirements

### Requirement: Carrier MCC/MNC to network type mapping

A `NetworkTypeMapper` utility SHALL map carrier MCC/MNC codes to the expected network type constant (LTE=13, NR=20) for that carrier in 2025+.

#### Scenario: US carrier returns NR

- **WHEN** the spoofed carrier MCC/MNC starts with "310" or "311" (US carriers)
- **THEN** `NetworkTypeMapper.getForMccMnc()` returns `NETWORK_TYPE_NR` (20)

#### Scenario: Unknown carrier returns LTE

- **WHEN** the spoofed carrier MCC/MNC is not in any known mapping
- **THEN** `NetworkTypeMapper.getForMccMnc()` returns `NETWORK_TYPE_LTE` (13) as a safe default

### Requirement: getNetworkType() spoofed

`DeviceHooker` SHALL hook `TelephonyManager.getNetworkType()` and `getDataNetworkType()` to return the value from `NetworkTypeMapper` based on the spoofed carrier.

#### Scenario: Jio carrier with NR

- **WHEN** the spoofed carrier is "40440" (Jio India) and the app calls `getNetworkType()`
- **THEN** the hook returns `NETWORK_TYPE_NR` (20)

#### Scenario: No carrier spoofed = passthrough

- **WHEN** no `CARRIER_MCC_MNC` is configured for the package
- **THEN** `getNetworkType()` returns the real value (no interception)

### Requirement: Network type consistent with carrier

The network type returned by hooks SHALL be consistent with the spoofed carrier's country and capabilities.

#### Scenario: Cross-reference check

- **WHEN** carrier is "23430" (UK/EE) and network type is NR
- **THEN** this is valid (EE supports 5G in UK urban areas)
