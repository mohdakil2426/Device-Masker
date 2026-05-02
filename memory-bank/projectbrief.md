# Project Brief: Device Masker

## Overview

Device Masker is an Android LSPosed/libxposed module for privacy research and controlled per-app device identity spoofing. It has a Compose app for configuration, a shared `:common` contract module, and a `:xposed` hook layer that runs inside scoped target app processes.

The project is active development, not a stable release. As of 2026-05-02, the app has its first verified working base: `com.mantle.verify` launched under LSPosed after the latest crash remediation, Device Masker hooks registered, and LSPosed logs showed live spoof events for multiple identifiers.

## Core Goal

Provide stable, configured spoofed identity values to selected apps while keeping target app startup safe.

Primary goals:
- Per-app and per-group spoof configuration.
- Stable stored values generated in the app and consumed by hooks.
- Hook coverage for Android ID, device profile, telephony, SIM/carrier, network, Advertising ID, Media DRM, location, sensor, WebView, and package visibility paths.
- Anti-detection for safer surfaces such as stack traces, package visibility, and `/proc/self/maps`.
- Diagnostics and logs through rootless app logs plus LSPosed hook-side logs.

## Non-Goals

Device Masker does not attempt:
- Root hiding.
- Play Integrity, SafetyNet, or hardware attestation bypass.
- Bootloader or verified boot bypass.
- Fraud, ban evasion, or unauthorized access workflows.
- Global device mutation outside selected target app processes.

## Target Users

- Security researchers testing app fingerprinting behavior.
- Android privacy researchers who need controlled app-specific identities.
- Developers studying libxposed API 101 architecture and RemotePreferences configuration.
- Advanced users validating how apps consume Android framework identifiers.

## Current Verified State

Latest full gate:

```powershell
.\gradlew.bat spotlessApply spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug assembleRelease --no-daemon
```

Result: `BUILD SUCCESSFUL`.

Runtime smoke check:
- Device/emulator: `emulator-5554`.
- Installed rebuilt debug APK.
- Scoped target: `com.mantle.verify`.
- LSPosed loaded `com.astrixforge.devicemasker.xposed.XposedEntry`.
- Hooks registered successfully.
- Spoof events appeared for Android ID, carrier MCC/MNC, network operator, IMEI, Wi-Fi MAC, Wi-Fi SSID, Advertising ID, Media DRM ID, and SIM operator name.
- Previous crash signatures did not appear in the final launch log window:
  - `androidx.work.WorkManagerInitializer`
  - WebView regex `PatternSyntaxException`
  - abstract WebView hook failure
  - target-app fatal crash

## Development Status

Current status: working development base with one target-app smoke pass. The project is not stable until broader LSPosed validation passes across more target apps, more Android versions, and enabled/disabled/malformed config scenarios.

Known stability decisions:
- Release shrinking/minification is disabled while libxposed hook lambdas are being live-validated.
- Global `Class.forName` and `ClassLoader.loadClass` anti-detection hooks are implemented but not registered by default because they caused or contributed to target startup instability.
- AIDL diagnostics is best-effort only; LSPosed logs are the authoritative runtime source for hook events.

## Quality Bar

Target app safety is more important than broad spoof coverage.

Hooks must:
- Return original framework values when config is disabled, missing, blank, malformed, or unsafe.
- Avoid generating random fallback identifiers in target processes.
- Avoid mutating framework-returned lists in place.
- Avoid static initializers that can throw in target processes.
- Skip unhookable methods such as abstract framework declarations.
- Log hook registration and spoof events to LSPosed logs.
