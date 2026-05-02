# Project Brief: Device Masker

## Overview

Device Masker is an Android LSPosed/libxposed module for privacy research and controlled device-identity spoofing. It provides a Compose app for configuration and an Xposed hook layer that intercepts selected Android framework and Google service APIs in scoped target apps.

The project is in active development. The current codebase builds and launches, and the architecture has been remediated after the 2026-05-02 audit, but full end-to-end spoof validation must still be performed against real target apps on a rooted LSPosed runtime.

## Core Goal

Spoof configured device identifiers per app while hiding obvious LSPosed/module signals from target apps.

The project should stay focused on:
- Device, SIM, network, advertising, system profile, and location identifier spoofing.
- Per-app and per-group configuration.
- Anti-detection for the module/LSPosed presence.
- Diagnostics for hook registration, spoof events, and logs.

## Non-Goals

Device Masker does not attempt to solve:
- Root hiding.
- Play Integrity or SafetyNet bypass.
- Hardware attestation bypass.
- Bootloader status spoofing.
- General fraud or unauthorized access workflows.

Those concerns belong to separate modules or are out of scope.

## Target Users

- Privacy-conscious Android users who want app-specific device identities.
- Security researchers testing app fingerprinting behavior.
- Developers studying modern libxposed API 101 module architecture.
- Advanced users who need multiple consistent identity groups for controlled testing.

## Development Status

Current status: development build, post-audit remediation complete in working tree.

Known verified state:
- Debug/release Gradle builds pass.
- Unit tests pass.
- Lint and Spotless pass.
- Xposed static safety greps pass.
- Debug APK installs and launches to `MainActivity` on `emulator-5554`.
- LSPosed metadata uses API 101 and scope includes `android` and `system`.

Remaining validation:
- Test live hooks against scoped target apps under LSPosed.
- Verify the previously stuck target app now passes splash/startup.
- Validate anti-detection behavior in real target processes.
- Confirm diagnostics Binder registration in system_server on the target runtime.

## Quality Bar

The project should prefer correctness and target-app safety over broad spoof coverage. A disabled, missing, blank, or malformed spoof value must pass through to the original framework result rather than inventing a runtime fallback.

Runtime hook callbacks should be conservative:
- Call the original API first when appropriate.
- Return configured stored values only when explicitly enabled.
- Avoid generating fresh identifiers in target processes.
- Deoptimize hooked methods.
- Never crash target apps or system_server.

