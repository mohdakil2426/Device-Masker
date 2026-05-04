# Active Context: Device Masker

## Current Focus

The latest successful runtime smoke test launched `com.mantle.verify` under LSPosed on `emulator-5554`. `XposedEntry` loaded, hooks registered, and LSPosed logs showed spoof events for Android ID, carrier, IMEI, Wi-Fi, Advertising ID, Media DRM, and SIM operator paths. The previous crash signatures were absent from the final launch window.

## 2026-05-04 Navigation 3 Implementation

Implemented the Navigation 3 migration requested after the M3E implementation work:
- Removed Navigation Compose 2.x runtime navigation from the app.
- Added Navigation 3 runtime/ui `1.1.1` and lifecycle ViewModel Navigation 3 `2.10.0`.
- Added adaptive Navigation 3 `1.3.0-alpha10` and moved the project to compile SDK 37 to satisfy the official artifact requirements.
- Migrated `NavDestination` to Navigation 3 `NavKey`.
- Replaced `NavHost`/`NavController`/`composable`/`toRoute` usage in `MainActivity` with `NavDisplay`, `entryProvider`, entry decorators, and typed destination entries.
- Added `DeviceMaskerNavigationState` and `DeviceMaskerNavigator` for explicit top-level Home/Groups/Settings stacks, Group Detail routing, Diagnostics routing, and back behavior.
- Added M3E navigation transitions and reduced-motion fallback to `NavDisplay`.
- Added adaptive list-detail scene metadata for Groups -> Group Detail. Compact width uses Navigation 3's default single-pane scene, while medium/expanded widths use the Material list-detail strategy.
- Added saveable selected top-level destination state; individual top-level stacks are saved through `rememberNavBackStack`.
- Added `devicemasker://open/...` deep links for Home, Groups, Group Detail, Settings, and Diagnostics. Group Detail and Diagnostics use Navigation 3 synthetic stacks so Back returns to Groups and Settings respectively.
- Added `DeviceMaskerNavigatorTest` covering initial state, stack preservation, restored selected top-level state, child pop, top-level back to Home, app-exit request from Home root, deep-link parsing, and deep-link stack replacement.

Verification:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.astrixforge.devicemasker.ui.navigation.DeviceMaskerNavigatorTest --no-daemon
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --tests com.astrixforge.devicemasker.ui.navigation.DeviceMaskerNavigatorTest --no-daemon
.\gradlew.bat spotlessCheck :app:testDebugUnitTest lint assembleDebug --no-daemon
```

All commands returned `BUILD SUCCESSFUL`.

Post-audit Navigation 3 fix:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.astrixforge.devicemasker.ui.navigation.DeviceMaskerNavigatorTest --no-daemon
```

Result: `BUILD SUCCESSFUL`.

Deep-link completion verification:

```powershell
.\gradlew.bat spotlessApply spotlessCheck :app:testDebugUnitTest lint assembleDebug --no-daemon
adb shell am start -W -a android.intent.action.VIEW -d "devicemasker://open/groups/9733ad2b-1e87-4f92-b869-f549fc82b26e" com.astrixforge.devicemasker
adb shell am start -W -a android.intent.action.VIEW -d "devicemasker://open/diagnostics" com.astrixforge.devicemasker
```

Result: Gradle gate returned `BUILD SUCCESSFUL`. Mobile MCP verified the group deep link opened the `Tesing` group detail screen, the diagnostics deep link opened Diagnostics, and Back from Diagnostics returned to Settings.
Mobile MCP landscape validation also showed the Groups list and `Tesing` group detail rendered side by side, validating the available expanded-width/list-detail behavior on the current emulator.

Open validation from this navigation migration:
- Manual process-death restoration validation for the full UI flow. Automated coverage now verifies restored selected top-level state.

## 2026-05-04 Implementation Verification Follow-up

Manual verification found that the previous implementation summary overstated current-runtime completion. The app-side implementation gaps have now been completed and verified by the full Gradle gate, including `:app:assembleCiRelease`. Device Masker installs and launches on `emulator-5554` through Mobile MCP and ADB, and app-owned root-capture artifacts are present.

Current runtime caveat: after installing the rebuilt APK, the app shows `Module Not Injected`. A fresh `com.mantle.verify` launch keeps the target process alive, but current logcat does not show `XposedEntry`, `All hooks registered`, or `Spoof event` entries. Do not claim current-build target-process hook success until LSPosed module injection is restored and logs confirm hook registration/spoof events again.

Superseding runtime validation later on 2026-05-04 restored module injection on `emulator-5554`:
- Device Masker UI showed `Protection Active` and `Module Enabled`.
- Group `test` showed `5 apps • 2 assigned`.
- Settings showed `Root Access: Granted`.
- Diagnostics showed `Module Active`, anti-detection `4/4 tests passed`, real/spoofed Android ID, and real/spoofed Device Profile.
- `com.mantle.verify` launched through Mobile MCP, stayed alive, displayed spoofed timezone/device profile values, and logcat showed `XposedEntry`, target selection, `All hooks registered`, and spoof events for multiple identifiers.
- `flar2.devcheck` launched through Mobile MCP, stayed alive, displayed spoofed device/network values, and logcat showed `XposedEntry`, target selection, `All hooks registered`, and spoof events.
- Basic support export completed through Android DocumentsUI as `/sdcard/devicemasker_support_20260504_150228.zip`.
- Final crash-signature scan found no `FATAL EXCEPTION`, `PatternSyntaxException`, `Cannot hook abstract`, `AbstractMethodError`, `WorkManagerInitializer`, Mantle ANR, or DevCheck ANR matches.

Fresh verification completed:

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat spotlessApply spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug assembleRelease :app:assembleCiRelease --no-daemon
```

Both commands returned `BUILD SUCCESSFUL`.

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

Target runtime smoke was rerun later on 2026-05-04; see the runtime-smoke evidence below for `com.mantle.verify` and `flar2.devcheck`.

## 2026-05-03 Maximum Diagnostics Logging

Implemented local-first structured diagnostics:
- Shared `DiagnosticEvent` contract and redaction engine in `:common`.
- Rotating app JSONL diagnostic store and app Timber migration.
- Structured Xposed diagnostic sink and hook health registry with spoof event aggregation.
- Diagnostics service ring buffer with dropped-log tracking.
- Root shell and Root Maximum collector for bounded logcat, ANR, tombstone, dumpsys, and getprop artifacts.
- Redacted snapshots and support bundle ZIP export modes: Basic, Full Debug, Root Maximum.

Root Maximum still needs rooted-device smoke validation. Target LSPosed smoke has not been rerun after this diagnostics work.

## 2026-05-03 Diagnostics And Root Export Audit Fixes

Implemented the follow-up audit fixes for diagnostics and root evidence:
- Diagnostics UI state now distinguishes app-side framework connection from optional diagnostics service availability and hook evidence. Unavailable service-backed hook evidence is represented as unavailable/unknown instead of `0`.
- Root Maximum share/export no longer short-circuits as `NoLogs` before root collection.
- Root Maximum support bundles now invoke `RootLogCollector`, include `root/` artifacts, and snapshot actual root/service availability.
- Production root command execution uses libsu core 6.0.0; the `RootCommandExecutor` interface remains for unit tests.
- Root command output now includes per-command manifests and a collector `command_manifest.jsonl` with status, exit code, timeout, root availability, and stderr summary.
- Target package names are validated before inclusion in root shell commands; invalid or blank target packages skip target-specific commands.
- `LocationManager.getLastKnownLocation()` now returns a copied `Location` when spoofing coordinates instead of mutating the framework-returned instance in place.
- PackageManager method discovery has regression coverage for API 33+ flags object overloads.

Verification:

```powershell
.\gradlew.bat spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug --no-daemon
```

Result: `BUILD SUCCESSFUL`. Gradle emitted a transient Kotlin daemon session warning and compiled with fallback, but the command exited successfully.

Rooted-device Root Maximum export and target LSPosed runtime smoke still need to be rerun.

Runtime smoke rerun after these fixes:
- Installed `app/build/outputs/apk/debug/app-debug.apk` on `emulator-5554`.
- Force-stopped and launched `com.mantle.verify`.
- `pidof com.mantle.verify` returned `7537`.
- Logcat showed `XposedEntry loaded for process: com.mantle.verify`.
- Logcat showed `All hooks registered for: com.mantle.verify`.
- Logcat showed spoof events for LOCALE, ANDROID_ID, CARRIER_MCC_MNC, NETWORK_OPERATOR, IMEI, BLUETOOTH_MAC, WIFI_MAC, WIFI_SSID, PHONE_NUMBER, ADVERTISING_ID, MEDIA_DRM_ID, SIM_OPERATOR_NAME, and TIMEZONE.
- No matching final-window fatal signatures were found for `FATAL EXCEPTION`, `PatternSyntaxException`, `Cannot hook abstract`, `AbstractMethodError`, or `WorkManagerInitializer`.

Root Maximum in-app export still needs manual/UI validation on the rooted device.

## 2026-05-04 Startup Root And Boot Capture

Implemented root-first diagnostics flow:
- `RootAccessManager` now owns root state: unknown, requesting, granted, denied, unavailable.
- First app launch/startup requests root from `MainActivity`; denial/unavailable shows a warning dialog explaining that Root Maximum logging, boot capture, and privileged diagnostics will not work.
- Settings shows root access status and disables Root Maximum export when root is not granted.
- `RootShell` no longer probes with `Shell.getShell()` during export. Export uses root only when `Shell.isAppGrantedRoot()` already reports granted, avoiding the late root prompt after folder selection.
- Added `RootLogCaptureService` as a `specialUse` foreground service for bounded root capture.
- Added `BootCaptureReceiver` for `BOOT_COMPLETED`; it starts the foreground root capture service after device boot.
- Startup root grant starts the same capture service immediately with trigger `startup`.
- Root Maximum export packages the latest captured root artifacts and can add an export snapshot only if root is already granted.

Validation:
- `.\gradlew.bat :app:testDebugUnitTest --no-daemon` passed.
- `.\gradlew.bat lint assembleDebug --no-daemon` passed.
- `.\gradlew.bat spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug --no-daemon` passed.
- Installed debug APK on `emulator-5554`, launched the app, and confirmed process `11060` stayed running.
- Startup root capture wrote `files/logs/root-capture/latest/root_capture_manifest.json` with status `COMPLETED` and produced root artifacts including logcat, dumpsys, getprop, and command manifest files.

Limit:
- `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED` is blocked by Android with `SecurityException`, so actual boot receiver behavior still needs a real reboot validation.

## Latest Crash Lessons

The working base came from fixing these failure modes:
- R8/minification caused invalid libxposed hooker lambda behavior in target processes.
- Target processes were not allowed to discover custom diagnostics Binder services.
- Global class lookup hooks could destabilize target startup.
- Android regex rejected variable-length lookbehind in `WebViewHooker` static initialization.
- Abstract `android.webkit.WebSettings` methods cannot be hooked.

## 2026-05-04 Master Implementation Plan Completion

Current status of `docs/reports/MASTER_IMPLEMENTATION_PLAN_2026-05-04.md`:

**Phase 0 (Safety):** ConfigManager atomic CAS + Mutex, AppLogStore async channel logging with injectable dispatcher, SpoofRepository AtomicReference caches, AppScopeRepository AtomicBoolean cache, RootShell deadlock fix + suspend guard, RootLogCollector shell escaping + regex fix, LogManager IO dispatcher, manifest `allowBackup=false`, SupportBundleBuilder streaming ZIP, JsonlDiagnosticStore synchronized reads, ConfigSync suspend variants.

**Phase 1 (Testing):** Created `MainDispatcherRule`, 7 fake implementations (`FakeSpoofRepository`, `FakeSettingsDataStore`, `FakeLogManager`, `FakeConfigManager`, `FakeAppScopeRepository`, `FakeServiceClient`, `FakeSharedPreferences`), ViewModel tests for all 5 screens. Found and fixed production bug: `SettingsViewModel` was using hardcoded `Dispatchers.IO` instead of injected `ioDispatcher`.

**Phase 2 (M3E Theme):** Extracted named colors to `Color.kt`, removed hardcoded `Color(0x...)` outside `Color.kt` under `ui/`, added surface container roles to `LightColorScheme`, AMOLED KDoc, 10-step shape scale + asymmetric variants, 15 emphasized typography styles + `LocalEmphasizedTypography`, and `UIDisplayCategory.themeColor()` mapping. API 34 contrast preference still needs final verification.

**Phase 3 (Architecture):** SavedStateHandle injected into all 5 ViewModels, `@Immutable` + `ImmutableList` on all State classes, `Flow.combine` in HomeViewModel, redundant suspend modifiers removed, `importGroups` returns `Result<Unit>`, loading overlays replaced, pager/tab sync fixed, contentDescription added, SimpleDateFormat cached, rememberSaveable for dialogs, AnimatedVisibility for loading.

**Phase 4 (Motion):** `MotionTokens` Expressive/Standard hierarchy, `ElevationTokens`, `MotionScheme.expressive()`, springs updated in components, touch targets ≥48dp in touched components, `graphicsLayer` instead of `scale()`, and ToggleButton accessibility semantics. Reduced-motion manual validation with animation scale disabled remains open.

**Phase 5 (Dependencies/M3E components):** Upgraded BOM to `2026.04.01` and material3 to `1.5.0-alpha18`. Adopted `MaterialExpressiveTheme`, `MotionScheme.expressive()`, native `LoadingIndicator`, native `ContainedLoadingIndicator`, native `ButtonGroup`, `SplitButtonLayout` for export actions, `FloatingActionButtonMenu` for group quick actions, `HorizontalFloatingToolbar` for group editing, and `MaterialShapes.SoftBurst` in the Home hero.

**Phase 6 (Navigation):** Navigation 3 `NavKey` `NavDestination`, `NavDisplay` + `entryProvider`, explicit top-level stacks in `DeviceMaskerNavigationState`, M3E transition specs, adaptive Groups -> Group Detail list-detail metadata, and navigator unit tests.

**Phase 7 (Build):** `ciRelease` build type with minification, Compose compiler metrics, Spotless ktfmt in version catalog, turbine/mockk test deps, `windowSoftInputMode="adjustResize"`, redundant daemon property removed.

**Phase 8 (Polish):** @Preview added to touched tab/components, window size class adaptation with NavigationRail for medium/expanded screens, compact Mobile MCP smoke across Home/Configure/Group Detail/Apps/Groups/Settings, and advanced M3E smoke for Settings export split buttons, Groups FAB menu, and Group Detail horizontal toolbar.

**Phase 9 (Validation):** Full gate passes — `spotlessCheck`, all unit tests (`:common`, `:app`, `:xposed`), `lint`, `test`, `assembleDebug`, `assembleRelease`, and `:app:assembleCiRelease`. ADB smoke on 2026-05-04 showed `com.mantle.verify` and `flar2.devcheck` alive with `XposedEntry`, `All hooks registered`, and spoof events; no matching fatal crash signatures appeared in the tested log windows.

Not complete: full light/dark/AMOLED/dynamic-color visual matrix, Accessibility Scanner/TalkBack audit, 10-minute ANR/jank run, reboot boot-capture validation, disabled/missing/malformed pass-through checks, and exact value-by-value spoof assertions.

## Open Validation Items

High priority:
- Repeat `com.mantle.verify` validation after reboot and LSPosed module toggle.
- Validate at least two more target apps.
- Confirm configured spoof values match UI-generated stored values.
- Confirm disabled/missing/malformed values pass through to originals.
- Verify LSPosed log export after target hook events.
- Verify PackageManager hiding on API 33+ flag object overloads.
- Verify anti-detection behavior with class lookup hooks still disabled.
- Revisit M3E component migration when material3 1.5.0 reaches stable.

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
