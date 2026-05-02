# Active Context: Device Masker

## Current Focus

The project is in active development after the 2026-05-02 architecture audit remediation. The working tree contains code and documentation updates that make the current architecture coherent around libxposed API 101, RemotePreferences config delivery, diagnostics-only AIDL, canonical `AppConfig` scoping, and original-result fallback in hooks.

The next meaningful work is target-app runtime validation under LSPosed, especially the app that previously stuck on its logo.

## Current Verified State

Last full gate run:

```powershell
.\gradlew.bat spotlessApply spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug assembleRelease --no-daemon
```

Result: `BUILD SUCCESSFUL`.

Static Xposed safety greps returned no matches for:
- Legacy static hook annotations/callbacks.
- Hardcoded RemotePreferences key strings in app/xposed source.
- Insecure `Random()` in common generators.
- Timber usage in `:xposed`.
- Compose imports in `:common` or `:xposed`.
- Runtime generator/default fallback patterns in xposed hooks.

Emulator check:
- Installed `app/build/outputs/apk/debug/app-debug.apk` on `emulator-5554`.
- Launched `com.astrixforge.devicemasker`.
- Confirmed foreground `MainActivity`.
- Final screenshot: `docs/device-masker-home-verification-2026-05-02-final.png`.

## Recent Architecture Decisions

- libxposed API is 101.0.1 for `:xposed`; app-side service/interface artifacts are 101.0.0.
- Config delivery is RemotePreferences-first and not AIDL.
- AIDL service is diagnostics-only.
- `XposedModuleActive` sentinel was removed; app health uses `XposedPrefs.isServiceConnected`.
- `AppConfig` is canonical for protected app scope and group assignment.
- Legacy `SpoofGroup.assignedApps` is migration/display compatibility only.
- Hooks must return original framework values on disabled/missing/blank/malformed config.
- Hookers must not generate runtime identifiers.
- Default scope includes `android` and `system`.

## Recent Fixes To Preserve

- Config load and libxposed service bind trigger RemotePreferences sync.
- Full sync clears stale package keys.
- Deleted groups remove app configs assigned to that group.
- `SpoofRepository.removeAppFromGroup()` checks canonical `AppConfig`.
- Sensor hooks only register when `DEVICE_PROFILE` is enabled and valid.
- Malformed MediaDRM hex passes through to original bytes.
- Malformed MCC/MNC passes through to original values.
- PackageManager self-hide covers modern API 33+ flag overloads.
- Throw-based hiding hooks use `ExceptionMode.PASSTHROUGH`.
- Hook/list filters return filtered copies instead of mutating framework lists.
- App-side logs now persist without root through `PersistentAppLogTree` and `AppLogStore`.
- Log export now produces a minimal structured file from app-owned logs plus available diagnostics service logs.

## Open Validation Items

High priority:
- Run on rooted LSPosed runtime with Device Masker enabled and target app scoped.
- Verify the previously stuck target app now starts.
- Verify configured spoof values in target app APIs.
- Verify disabled or malformed values pass through to originals.
- Verify diagnostics service registers in system_server and reports hook events.
- Verify anti-detection behavior with target-process checks.
- Verify exported logs on-device after app startup, diagnostics connection, and at least one hooked target app event.

Medium priority:
- Clean AGP/Spotless deprecation warnings.
- Expand unit tests around additional hook conversion helpers.
- Add more explicit docs for the difference between service connection, module enabled, scope enabled, package hooked, and spoof event observed.

## Working Assumptions

- This is not a stable release branch.
- It is acceptable to keep development compatibility helpers for old config JSON.
- Stability and pass-through safety are more important than aggressive spoofing coverage.
- Manual LSPosed runtime validation is required before claiming target-app compatibility.
