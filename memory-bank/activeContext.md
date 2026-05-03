# Active Context: Device Masker

## Current Focus

The latest successful runtime smoke test launched `com.mantle.verify` under LSPosed on `emulator-5554`. `XposedEntry` loaded, hooks registered, and LSPosed logs showed spoof events for Android ID, carrier, IMEI, Wi-Fi, Advertising ID, Media DRM, and SIM operator paths. The previous crash signatures were absent from the final launch window.

## Current Verified State

Full gate:

```powershell
.\gradlew.bat spotlessApply spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug assembleRelease --no-daemon
```

Result: `BUILD SUCCESSFUL`.

App smoke:
- Installed debug APK on `emulator-5554`.
- Launched `com.astrixforge.devicemasker`.
- App opens to `MainActivity`.

Target smoke:
- Installed rebuilt debug APK on `emulator-5554`.
- Force-stopped and launched `com.mantle.verify`.
- Confirmed target process stayed alive after startup.
- Confirmed LSPosed logs:
  - `XposedEntry loaded for process: com.mantle.verify`
  - `Anti-detection hooks registered`
  - `All hooks registered`
  - multiple `Spoof event` entries.
- No final-window recurrence of:
  - `androidx.work.WorkManagerInitializer`
  - WebView regex `PatternSyntaxException`
  - `Cannot hook abstract methods`
  - fatal `AndroidRuntime` crash.

## Recent Decisions To Preserve

- Config delivery is RemotePreferences-first.
- RemotePreferences config writes should use explicit `commit()` when the app needs to know sync succeeded.
- AIDL is diagnostics-only.
- LSPosed logs are authoritative for target-process hook proof.
- `JsonConfig.appConfigs` is canonical.
- `SpoofGroup.assignedApps` is legacy/display compatibility.
- `SharedPrefsKeys` is the only key builder.
- Hooks return originals for unsafe config.
- Hookers do not generate runtime identifiers.
- Release minification and resource shrinking stay disabled during hook validation.
- Target app processes do not look up custom diagnostics through `ServiceManager`.
- Global class lookup anti-detection hooks are not registered by default.
- WebView UA spoofing uses defensive string parsing and skips abstract methods.
- libxposed `Chain.getArgs()` is immutable; hooks that change arguments must call `chain.proceed(Object[])`.
- Hook registration fallback catches must rethrow `XposedFrameworkError` / `HookFailedError` before handling ordinary failures.
- Locale/timezone spoofing should target default reads, not every constructed `Locale` or `TimeZone` instance.

## 2026-05-03 libxposed Audit Remediation

Applied fixes from `docs/reports/LIBXPOSED_CODE_AUDIT_2026-05-03.md`:
- Fixed `WebViewHooker` UA setter interception to use copied args plus `chain.proceed(args)`.
- Added explicit `XposedFrameworkError` rethrows in hook registration/deoptimization fallback paths.
- Reworked `XposedEntry` package selection so secondary package callbacks can hook when the process base or loaded package is enabled, while still registering once per classloader.
- Removed broad `TimeZone.getID()` and `Locale.toString()` hooks; kept default timezone/locale hooks.
- Fixed `ConfigSync.syncApp()` to honor `AppConfig.isEnabled` and nonblank values.
- Replaced async RemotePreferences `apply()` writes in config sync and direct XposedPrefs setters with explicit `commit()`.
- Guarded `XposedPrefs.init()` so libxposed service listener registration is local-once.
- Cleaned touched stale comments/keep rules from `ModulePreferences` / `ModulePreferencesProvider` to `RemotePreferences` / `XposedProvider`.
- Added static/unit coverage for immutable chain args, framework-error rethrow presence, and quick app sync enablement.

Target runtime smoke has not yet been rerun after these fixes.

## 2026-05-03 Maximum Diagnostics Logging

Implemented local-first structured diagnostics:
- Shared `DiagnosticEvent` contract and redaction engine in `:common`.
- Rotating app JSONL diagnostic store and app Timber migration.
- Structured Xposed diagnostic sink and hook health registry with spoof event aggregation.
- Diagnostics service ring buffer with dropped-log tracking.
- Root shell and Root Maximum collector for bounded logcat, ANR, tombstone, dumpsys, and getprop artifacts.
- Redacted snapshots and support bundle ZIP export modes: Basic, Full Debug, Root Maximum.

Root Maximum still needs rooted-device smoke validation. Target LSPosed smoke has not been rerun after this diagnostics work.

## Latest Crash Lessons

The working base came from fixing these failure modes:
- R8/minification caused invalid libxposed hooker lambda behavior in target processes.
- Target processes were not allowed to discover custom diagnostics Binder services.
- Global class lookup hooks could destabilize target startup.
- Android regex rejected variable-length lookbehind in `WebViewHooker` static initialization.
- Abstract `android.webkit.WebSettings` methods cannot be hooked.

## Open Validation Items

High priority:
- Repeat `com.mantle.verify` validation after reboot and LSPosed module toggle.
- Validate at least two more target apps.
- Confirm configured spoof values match UI-generated stored values.
- Confirm disabled/missing/malformed values pass through to originals.
- Verify LSPosed log export after target hook events.
- Verify PackageManager hiding on API 33+ flag object overloads.
- Verify anti-detection behavior with class lookup hooks still disabled.

Medium priority:
- Add per-app safe-mode controls for risky hook groups.
- Add optional per-app class lookup anti-detection kill switch before reintroducing class hiding.
- Add more unit tests for hook conversion helpers.
- Clean AGP and Spotless deprecation warnings.
- Improve UI wording around service connected vs target hooked vs spoof observed.

## Working Assumptions

- This is a development build.
- Stability beats spoof breadth.
- LSPosed runtime validation is required before stability claims.
- The first working base should be protected from broad refactors.
- New hook areas should start disabled or pass-through safe until proven.
