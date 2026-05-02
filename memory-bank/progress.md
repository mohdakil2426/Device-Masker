# Progress: Device Masker

## Current Status

| Area | Status |
| --- | --- |
| Project phase | Development, post-audit remediation |
| Build | Passing |
| Unit tests | Passing |
| Lint/format | Passing |
| Debug APK launch | Verified on `emulator-5554` |
| LSPosed metadata | API 101, entry and scope present |
| Config architecture | RemotePreferences primary, local JSON persistence |
| Diagnostics | AIDL diagnostics-only, app logs persisted rootlessly |
| Target-app hook validation | Pending |
| Stable release readiness | Not ready until target-app LSPosed validation passes |

## What Works Now

- Three-module Gradle structure: `:app`, `:common`, `:xposed`.
- Compose app launches to Home.
- Config persists to `filesDir/config.json`.
- App-side libxposed service binding exposes connection state.
- Config sync writes flattened per-app RemotePreferences keys.
- Stale RemotePreferences keys are cleared on full sync.
- `AppConfig` is the canonical protected-app table.
- Existing assigned-app-only development configs can migrate to app configs.
- Hook-side pref reads can distinguish configured stored values from missing/disabled values.
- High-risk identifier hooks pass through to original results when config is unsafe.
- Diagnostics AIDL contract is narrowed to events, logs, hooked package list, clear, and health.
- App-side logs persist to an app-owned structured file without root or `READ_LOGS`.
- Log export writes a minimal file containing app log entries and current xposed diagnostics entries.
- Release build runs R8 and keeps critical xposed classes.

## Latest Completed Work

2026-05-02 architecture remediation:
- Fixed Home/Diagnostics activation state by using libxposed service connection.
- Added service-bind and config-load resync.
- Reworked config projection around canonical `AppConfig`.
- Added stale-key cleanup.
- Removed runtime generator fallback from hook callbacks.
- Fixed malformed-value pass-through for MediaDRM and carrier MCC/MNC.
- Made sensor spoofing depend on an enabled valid device profile.
- Added `system` default scope.
- Added or filled deoptimize calls across hookers.
- Hardened anti-detection exception mode and list filtering.
- Expanded PackageManager hiding for API 33+ flag overloads.
- Added regression tests for config sync, stored spoof reads, legacy config migration, and group-delete app config cleanup.
- Updated architecture audit report and Memory Bank.

2026-05-02 logging/export remediation:
- Added rootless persistent app log storage through `AppLogStore`.
- Added `PersistentAppLogTree` so Timber app logs are stored in the app sandbox.
- Reworked `LogManager` export to avoid decorative empty templates.
- Export now merges persistent app entries with reachable `DeviceMaskerService` logs.
- Added regression tests for log persistence, trimming, sanitization, and minimal export formatting.

## Verification Evidence

Full gate:

```powershell
.\gradlew.bat spotlessApply spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug assembleRelease --no-daemon
```

Result: `BUILD SUCCESSFUL`.

Emulator:
- `emulator-5554`
- Installed debug APK.
- Launched package `com.astrixforge.devicemasker`.
- Foreground activity confirmed: `com.astrixforge.devicemasker/.ui.MainActivity`.

Audit report:
- `docs/DEVICE_MASKER_ARCHITECTURE_AUDIT_2026-05-02.md`

Screenshots:
- `docs/device-masker-launch-verification-2026-05-02.png`
- `docs/device-masker-home-verification-2026-05-02.png`
- `docs/device-masker-home-verification-2026-05-02-final.png`

## Remaining Work

Before calling this stable:
- Validate on a rooted LSPosed API 101 runtime.
- Scope and launch the affected target app that previously stuck on logo.
- Confirm spoof events appear in diagnostics.
- Confirm target app receives configured spoof values.
- Confirm disabled/missing/malformed spoof values pass through.
- Confirm anti-detection hooks hide Device Masker and known LSPosed/tool packages in target processes.
- Test at least one real API 33+ PackageManager flag query path.
- Export logs from the installed app after target hooks fire and confirm both app and xposed sections contain useful entries.

Engineering cleanup:
- Remove or update stale references outside Memory Bank if found.
- Clean AGP 10 deprecation warnings.
- Consider more tests around hook value conversion and config migrations.
- Keep Memory Bank updated after each architecture-affecting change.
