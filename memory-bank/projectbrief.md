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

Latest full gate (post-Master Implementation Plan 2026-05-04 M3E follow-up):

```powershell
.\gradlew.bat spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug assembleRelease :app:assembleCiRelease --no-daemon
```

Result: `BUILD SUCCESSFUL`.

Latest Detekt strictness state as of 2026-05-08:
- Detekt runs with `allRules=true`.
- `:app`, `:common`, and `:xposed` baselines are empty.
- `.\gradlew.bat detekt --no-daemon --stacktrace` passes after baseline regeneration.
- Keep baselines at zero unless accepted existing debt is explicitly documented.

Master Implementation Plan status on 2026-05-04:
- Phase 0: Safety & Stability core fixes complete.
- Phase 1: Testing Infrastructure complete for current unit-test scope.
- Phase 2: M3E Theme Foundation core tokens complete.
- Phase 3: Architecture & State (SavedStateHandle, @Immutable, UX fixes)
- Phase 4: Motion & Components core tokens complete.
- Phase 5: Dependency Upgrade complete for material3 `1.5.0-alpha18`; native `LoadingIndicator`, native `ButtonGroup`, `SplitButtonLayout`, `FloatingActionButtonMenu`, `HorizontalFloatingToolbar`, and `MaterialShapes` are adopted.
- Phase 6: Navigation Modernization complete for type-safe routes.
- Phase 7: Build Hardening complete for current gates and `ciRelease`.
- Phase 8: Polish partially complete: previews, window size class adaptation, and compact Mobile MCP smoke done.
- Phase 9: Final Validation partially complete: Gradle full gate and two target-app LSPosed smoke tests pass.

Still open before stable release: full visual/accessibility matrix, reduced-motion manual validation, 10-minute ANR/jank test, reboot boot-capture validation, disabled/missing/malformed pass-through checks, exact spoof value assertions, and broader target-app validation.

Latest release R8 runtime check:
- Device/emulator: `emulator-5554`.
- Installed signed release APK built with R8 minification/resource shrinking enabled.
- Scoped targets: `com.mantle.verify` and `flar2.devcheck`.
- LSPosed loaded `com.astrixforge.devicemasker.xposed.XposedEntry`.
- Hooks registered successfully.
- Spoof events appeared in both targets.
- Mobile MCP observed Mantle displaying spoofed model and fingerprint.
- Previous release crash signatures did not appear in the final checked log windows:
  - `AbstractMethodError` from `XposedInterface.Hooker.intercept(...)`
  - `androidx.work.WorkManagerInitializer`
  - WebView regex `PatternSyntaxException`
  - abstract WebView hook failure
  - target-app fatal crash
- User reported the same R8 fix working on a real Android 16 device.

## Development Status

Current status: working development base with release R8 enabled, emulator smoke coverage on Mantle and DevCheck, and user-reported success on a real Android 16 device. The project is not stable until broader LSPosed validation passes across more target apps, more Android versions, and enabled/disabled/malformed config scenarios.

Known stability decisions:
- Release shrinking/minification is enabled after replacing direct libxposed SAM/lambda callbacks with the `StableHooker` adapter path.
- Hook registration must use `intercept(stableHooker { ... })` or explicit named `XposedInterface.Hooker` implementations; do not reintroduce direct `.intercept { ... }` callbacks in runtime hookers.
- Global `Class.forName` and `ClassLoader.loadClass` anti-detection hooks are implemented but not registered by default because they caused or contributed to target startup instability.
- No custom diagnostics Binder remains; LSPosed logs are the authoritative runtime source for hook events.

## Quality Bar

Target app safety is more important than broad spoof coverage.

Hooks must:
- Return original framework values when config is disabled, missing, blank, malformed, or unsafe.
- Avoid generating random fallback identifiers in target processes.
- Avoid mutating framework-returned lists in place.
- Avoid static initializers that can throw in target processes.
- Skip unhookable methods such as abstract framework declarations.
- Log hook registration and spoof events to LSPosed logs.
