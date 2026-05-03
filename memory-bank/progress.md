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
- Root Maximum collector builds bounded root artifact files behind opt-in root execution.
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

Note: target LSPosed runtime smoke has not yet been rerun after the 2026-05-03 remediation.

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

Engineering cleanup:

- Clean AGP 10 deprecation warnings.
- Replace deprecated Spotless `indentWithSpaces`.
- Add more hook helper tests.
- Keep docs and Memory Bank current after every runtime validation result.
