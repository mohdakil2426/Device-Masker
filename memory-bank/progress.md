# Progress: Device Masker

## Current Status

| Area | Status |
| --- | --- |
| Project phase | Development, first working base |
| Build | Passing |
| Unit tests | Passing |
| Lint/format | Passing |
| Debug APK launch | Verified on `emulator-5554` |
| LSPosed metadata | API 101, entry and scope present |
| Config architecture | RemotePreferences primary, local JSON persistence |
| Diagnostics | Structured JSONL app logs, best-effort AIDL diagnostics, support bundle export |
| Target hook validation | Smoke-passed on `com.mantle.verify` |
| Stable release readiness | Not ready; broader target validation required |

## What Works

- Three-module Gradle structure: `:app`, `:common`, `:xposed`.
- Compose app launches.
- Local config persists to app `filesDir/config.json`.
- App-side libxposed service binding exposes connection state.
- Config sync writes flattened per-app RemotePreferences keys.
- Full config sync clears stale package keys.
- `AppConfig` is the canonical app/group assignment model.
- Hook-side pref reads distinguish missing/disabled values from configured values.
- High-risk hooks pass through when config is unsafe.
- Hook-side registration and spoof events are mirrored to LSPosed logs.
- Rootless app log export works from app-owned storage.
- Redacted support bundle export works for Basic, Full Debug, and Root Maximum modes at the unit level.
- Root Maximum collector builds bounded root artifact files behind opt-in libsu root execution.
- Root Maximum support bundles include root artifacts and command-result manifests when exported.
- Root access is requested on first app startup, tracked centrally, and surfaced in Settings.
- Boot/startup Root Maximum capture writes latest root artifacts before export.
- Diagnostics state distinguishes framework connection, optional diagnostics service availability, and service-backed hook evidence.
- `com.mantle.verify` launched after latest remediation and emitted spoof events.

## Completed Milestones

### 2026-05-02 Architecture Remediation

- Reworked app status around libxposed service connection.
- Made RemotePreferences the config delivery path.
- Kept AIDL diagnostics-only.
- Reworked config projection around canonical `AppConfig`.
- Added stale-key cleanup.
- Removed runtime generator fallback from hook callbacks.
- Added original-result fallback for malformed MediaDRM and carrier values.
- Made sensor spoofing depend on valid enabled device profile config.
- Added default `system` scope.
- Added or preserved deoptimize calls.
- Expanded PackageManager hiding for API 33+ paths.
- Updated architecture docs and Memory Bank.

### 2026-05-02 Logging Remediation

- Added `AppLogStore`.
- Added `PersistentAppLogTree`.
- Reworked `LogManager` export into minimal structured output.
- Kept `READ_LOGS` out of app requirements.
- Added log persistence and export tests.

### 2026-05-02 Target Crash Remediation

- Analyzed app export logs and LSPosed Manager logs.
- Disabled release shrinking/minification during libxposed hook validation.
- Removed target-process custom diagnostics `ServiceManager` lookup.
- Added tests for release hooker bytecode safety and diagnostics lookup safety.
- Fixed anti-detection class-loading ANR patterns with reentry guards and direct bootstrap class literals.
- Added safe-prefix class hiding helper tests.
- Disabled global `Class.forName` and `ClassLoader.loadClass` anti-detection registration by default.
- Removed Android-invalid WebView lookbehind regex.
- Made WebView UA parsing defensive.
- Skipped abstract `WebSettings` methods.
- Added xposed regression tests for WebView UA parsing and anti-detect class lookup pass-through.

### First Working Base

- Installed rebuilt debug APK on `emulator-5554`.
- Launched `com.mantle.verify` under LSPosed.
- Confirmed `XposedEntry` loaded.
- Confirmed hooks registered.
- Confirmed spoof events for multiple identifiers in LSPosed logs.
- Confirmed previous target startup crash signatures were absent in the final smoke launch.

### 2026-05-03 libxposed Audit Remediation

- Wrote and updated `docs/reports/LIBXPOSED_CODE_AUDIT_2026-05-03.md`.
- Fixed libxposed immutable chain args misuse in `WebViewHooker`.
- Added `XposedFrameworkError` rethrow handling before generic hook registration/deoptimization fallbacks.
- Improved `XposedEntry` process/package selection for secondary package callbacks while preserving one hook registration per classloader.
- Narrowed locale/timezone hooks by removing broad `Locale.toString()` and `TimeZone.getID()` interception.
- Fixed `ConfigSync.syncApp()` to honor canonical `AppConfig.isEnabled`.
- Changed RemotePreferences writes in config sync/direct setters to synchronous `commit()` with warning logs on failure.
- Guarded `XposedPrefs.init()` against duplicate libxposed listener registration.
- Updated stale libxposed migration comments and app keep rule naming to `RemotePreferences` / `XposedProvider`.
- Added regression tests for chain args mutation, Xposed framework-error handling presence, and quick sync app enablement.

### 2026-05-03 Maximum Diagnostics Logging

- Added shared diagnostic event schema and deterministic redaction.
- Replaced app TSV log persistence with structured JSONL diagnostic events.
- Added Xposed structured event sink, hook health registry, and spoof aggregation.
- Added diagnostics service log buffer cap and dropped-count tracking.
- Added root command runner and Root Maximum log collector.
- Added redacted snapshots and ZIP support bundle builder.
- Added Settings export modes for Basic, Full Debug, and Root Maximum bundles.
- Documented architecture in `docs/reports/MAXIMUM_DIAGNOSTICS_LOGGING_ARCHITECTURE_2026-05-03.md`.

### 2026-05-03 Diagnostics, Root Logging, And Spoofing Audit Fixes

- Added libsu core 6.0.0 for production root command execution.
- Kept `RootCommandExecutor` injectable for unit tests.
- Wired `RootLogCollector` into Root Maximum support bundle creation.
- Ensured Root Maximum share export can build a bundle even when app/service logs are empty.
- Added root command manifests with status, exit code, timeout, root availability, and stderr summary.
- Added target package validation before shell command construction and skipped target-specific commands for invalid package names.
- Reworked Diagnostics service state so absent service evidence is unavailable/unknown instead of hooked app count `0`.
- Labeled unavailable service bridge data instead of passing empty xposed events as meaningful target evidence.
- Changed location last-known-location spoofing to return a copied location with coherent metadata.
- Added API 33+ PackageManager flag-overload discovery coverage.

### 2026-05-04 Startup Root And Boot Capture

- Added `RootAccessManager` for central root grant state.
- Requested root during app startup instead of during export.
- Added warning dialog when root is denied or unavailable.
- Added Settings root access status.
- Disabled Root Maximum export UI when root is not granted.
- Added `RootLogCaptureService` foreground service for bounded root capture.
- Added `BootCaptureReceiver` for `BOOT_COMPLETED` root capture.
- Added latest root capture artifact storage under app files.
- Changed Root Maximum export to package captured artifacts and avoid surprise root prompts after folder selection.

## Verification Evidence

Full gate:

```powershell
.\gradlew.bat spotlessApply spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug assembleRelease --no-daemon
```

Result: `BUILD SUCCESSFUL`.

Targeted post-audit remediation gate:

```powershell
.\gradlew.bat spotlessApply :app:testDebugUnitTest :xposed:testDebugUnitTest --no-daemon
```

Result: `BUILD SUCCESSFUL`.

Diagnostics/root audit gate:

```powershell
.\gradlew.bat spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug --no-daemon
```

Result: `BUILD SUCCESSFUL` (Kotlin daemon emitted a transient invalid-session warning and Gradle used fallback compilation).

Startup root/boot capture gate:

```powershell
.\gradlew.bat spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug --no-daemon
```

Result: `BUILD SUCCESSFUL`.

Startup capture runtime check:

- Installed `app/build/outputs/apk/debug/app-debug.apk` on `emulator-5554`.
- Launched `com.astrixforge.devicemasker`; `pidof` returned `11060`.
- Confirmed latest root capture manifest: `{"trigger":"startup","status":"COMPLETED",...}`.
- Confirmed root capture files exist under `files/logs/root-capture/latest/`.
- Could not fake `BOOT_COMPLETED` through adb because Android rejects shell-sent protected boot broadcasts with `SecurityException`; real reboot validation remains open.

Target LSPosed runtime smoke has been rerun after the diagnostics/root audit fixes.

Post diagnostics/root audit runtime smoke:

- Installed `app/build/outputs/apk/debug/app-debug.apk` on `emulator-5554`.
- Launched `com.mantle.verify`; `pidof` returned `7537`.
- LSPosed/logcat showed `XposedEntry loaded for process: com.mantle.verify`.
- LSPosed/logcat showed `All hooks registered for: com.mantle.verify`.
- Spoof events included LOCALE, ANDROID_ID, CARRIER_MCC_MNC, NETWORK_OPERATOR, IMEI, BLUETOOTH_MAC, WIFI_MAC, WIFI_SSID, PHONE_NUMBER, ADVERTISING_ID, MEDIA_DRM_ID, SIM_OPERATOR_NAME, and TIMEZONE.
- The checked log window did not show `FATAL EXCEPTION`, `PatternSyntaxException`, `Cannot hook abstract`, `AbstractMethodError`, or `WorkManagerInitializer`.

Target smoke evidence:

- `TARGET_PID=6592` in the successful retry.
- LSPosed log showed `All hooks registered for: com.mantle.verify`.
- LSPosed spoof events included:
  - `CARRIER_MCC_MNC`
  - `NETWORK_OPERATOR`
  - `ANDROID_ID`
  - `IMEI`
  - `WIFI_MAC`
  - `WIFI_SSID`
  - `ADVERTISING_ID`
  - `MEDIA_DRM_ID`
  - `SIM_OPERATOR_NAME`

## Remaining Work

Before calling this stable:

- Validate after emulator/device reboot.
- Validate after LSPosed module disable/enable cycle.
- Validate at least two additional target apps.
- Confirm actual returned values inside target apps, not only spoof event logs.
- Rerun `com.mantle.verify` target smoke after the 2026-05-03 libxposed audit fixes.
- Confirm disabled/missing/malformed values pass through.
- Confirm anti-detection behavior with current safer surfaces.
- Add a safe-mode UI/config flag for risky hook groups.
- Reconsider class lookup hiding only behind a per-app kill switch.
- Test package visibility hiding with API 33+ flag object overloads.
- Export logs after target hook events and verify exported output is useful.
- Smoke-test Basic, Full Debug, and Root Maximum bundle export on device.
- Reboot emulator/device and verify `BootCaptureReceiver` creates a `boot` root capture.

Engineering cleanup:

- Clean AGP 10 deprecation warnings.
- Replace deprecated Spotless `indentWithSpaces`.
- Add more hook helper tests.
- Keep docs and Memory Bank current after every runtime validation result.
