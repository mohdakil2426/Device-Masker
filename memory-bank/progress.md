# Progress: Device Masker

## Current Status

| Area | Status |
| --- | --- |
| Project phase | Development, R8 enabled in release |
| Build | Focused R8 release gate passing: `spotlessApply spotlessCheck :xposed:testDebugUnitTest :app:assembleRelease` |
| Unit tests | Passing |
| Lint/format/static analysis | Passing with Spotless, lint, and Detekt baselines |
| Debug APK launch | Verified on `emulator-5554` |
| LSPosed metadata | API 101, entry and scope present |
| Config architecture | RemotePreferences primary, local JSON persistence |
| Diagnostics | Structured JSONL app logs, best-effort AIDL diagnostics, support bundle export |
| Target hook validation | R8 release APK smoke-passed on `com.mantle.verify` and `flar2.devcheck` with LSPosed hook/spoof log evidence; user confirmed real Android 16 device success |
| M3E UI | Core and advanced implementation verified |
| Navigation | Navigation 3 `NavDisplay` with typed `NavKey` destinations, app-owned saveable top-level stacks |
| R8 minification | **Enabled in release** with StableHooker callback adapter. Latest checked APK: 4,007,831 bytes unsigned, 4,069,566 bytes signed. |
| Stable release readiness | R8 callback crash resolved; broader pass-through, reboot, and app-category validation still required before final stable release claims |

## Latest Audit Remediation

- Fixed current live coroutine/performance audit issues across `:app`, `:common`, and `:xposed`.
- Production app sources no longer contain `runBlocking` or `Thread.sleep` from the audited paths.
- `AppLogStore` uses non-blocking channel append plus monitor-based flush.
- `ConfigManager.resetForTests()` no longer blocks on a coroutine mutex.
- `SpoofRepository` no longer stores a `Context` property in the singleton instance.
- `HomeViewModel` now combines repository/module/Xposed flows through one collector.
- Common key/MAC/UUID validation regexes are precompiled.
- `Country` is serializable.
- `SensorHooker` avoids per-sensor reflection in the hook callback, and `AntiDetectHooker.filterStackTrace()` avoids array allocation when there are no hidden frames.
- Verified with `spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest --no-daemon --no-configuration-cache` and `lint test assembleDebug assembleRelease --no-daemon --no-configuration-cache`, both `BUILD SUCCESSFUL`.
- `graphify update .` completed after code changes.

## What Works

- Three-module Gradle structure: `:app`, `:common`, `:xposed`.
- Compose app launches.
- Full Gradle gate passes, including the `ciRelease` minified variant.
- Detekt runs across `:app`, `:common`, and `:xposed` with central config, Xposed-safe overrides, and initial per-module baselines.
- Local config persists to app `filesDir/config.json`.
- App-side libxposed service binding exposes connection state.
- Config sync writes flattened per-app RemotePreferences keys.
- Full config sync clears stale package keys.
- `AppConfig` is the canonical app/group assignment model.
- Per-app risky-hook and class lookup hiding opt-ins are persisted through local config and RemotePreferences.
- Hook-side pref reads distinguish missing/disabled values from configured values.
- High-risk hooks pass through when config is unsafe.
- Class lookup anti-detection remains disabled by default and only registers when both per-app opt-ins are enabled.
- Hook-side registration and spoof events are mirrored to LSPosed logs.
- Rootless app log export works from app-owned storage.
- Redacted support bundle export works for Basic, Full Debug, and Root Maximum modes at the unit level.
- Root Maximum collector builds bounded root artifact files behind opt-in libsu root execution.
- Root Maximum support bundles include root artifacts and command-result manifests when exported.
- Root access is requested on first app startup, tracked centrally, and surfaced in Settings.
- Boot/startup Root Maximum capture writes latest root artifacts before export.
- The current debug APK installs and launches on `emulator-5554` through Mobile MCP and ADB.
- App-owned root capture artifacts are visible through `adb shell run-as com.astrixforge.devicemasker ls files/logs/root-capture/latest`.
- Diagnostics state distinguishes framework connection, optional diagnostics service availability, and service-backed hook evidence.
- `com.mantle.verify` launched after latest remediation and emitted spoof events.
- `flar2.devcheck` launched after latest remediation and emitted spoof events.
- Diagnostics UI reports `Module Active`, anti-detection `4/4 tests passed`, real/spoofed Android ID, and real/spoofed Device Profile.
- Basic support export through DocumentsUI works and saved `/sdcard/devicemasker_support_20260504_150228.zip`.
- M3E core UI now uses `MaterialExpressiveTheme`, `MotionScheme.expressive()`, native `LoadingIndicator`, native `ContainedLoadingIndicator`, native `ButtonGroup`, 10-step shape tokens, asymmetric shape tokens, 15 emphasized typography styles, `LocalEmphasizedTypography`, and a `MaterialShapes.SoftBurst` Home hero moment.
- M3E advanced UI placements now use `SplitButtonLayout` in Settings export actions, `FloatingActionButtonMenu` in Groups quick actions, and `HorizontalFloatingToolbar` in the group editor.
- API 34 contrast preference tracking applies high-contrast color-role overrides when system contrast is raised.
- Legacy `ToggleButton` was removed; production flows use Material/Expressive switch patterns.
- Static UI audit found no remaining `Modifier.scale()` usage under `ui/` and no hardcoded `Color(0x...)` under `ui/` outside `Color.kt`.
- Mobile MCP smoke on `emulator-5554` verified Home, Configure action, Group Detail, category expansion, Apps tab, Groups, Settings, Settings export split buttons, Groups FAB menu, and Group Detail horizontal toolbar after the M3E changes.
- Navigation 3 app navigation compiles and passes navigator unit tests. `NavController`, `NavHost`, Navigation Compose `composable`, and `toRoute()` have been removed from app runtime navigation. The selected top-level tab is saved with `rememberSaveable`, while each stack is saved with `rememberNavBackStack`.
- Navigation 3 deep links support `devicemasker://open/home`, `/groups`, `/groups/{groupId}`, `/settings`, and `/diagnostics`. Group Detail and Diagnostics use synthetic stacks, and emulator smoke verified Group Detail and Diagnostics links.
- Navigation 3 expanded-width/list-detail smoke passed in emulator landscape: Groups list and Group Detail rendered side by side.

Latest verification caveat:
- On 2026-05-04 later in the emulator session, module injection was active again. `com.mantle.verify` and `flar2.devcheck` both showed `XposedEntry`, target selection, `All hooks registered`, and spoof events in logcat.
- Runtime gaps remain for disabled/missing/malformed pass-through, exact value-by-value assertions for all spoof types, real reboot boot-capture validation, and broader app-category validation.

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

Navigation 3 gate:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.astrixforge.devicemasker.ui.navigation.DeviceMaskerNavigatorTest --no-daemon
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --tests com.astrixforge.devicemasker.ui.navigation.DeviceMaskerNavigatorTest --no-daemon
.\gradlew.bat spotlessCheck :app:testDebugUnitTest lint assembleDebug --no-daemon
.\gradlew.bat spotlessApply spotlessCheck :app:testDebugUnitTest lint assembleDebug --no-daemon
adb shell am start -W -a android.intent.action.VIEW -d "devicemasker://open/groups/9733ad2b-1e87-4f92-b869-f549fc82b26e" com.astrixforge.devicemasker
adb shell am start -W -a android.intent.action.VIEW -d "devicemasker://open/diagnostics" com.astrixforge.devicemasker
```

Result: Gradle commands returned `BUILD SUCCESSFUL`. Mobile MCP verified Group Detail opened for the configured `Tesing` group, Diagnostics opened from the custom URI, Back from Diagnostics returned to Settings, and landscape list-detail rendered Groups plus Group Detail side by side.

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

### 2026-05-04 Master Implementation Plan Completion Status

The master plan now has an updated verified checklist. Core safety/build/runtime, core M3E implementation, and advanced M3E placements are complete. On 2026-05-06, remaining manual/device/testing checks were accepted as user-owned external validation and the master plan was closed at 100% implementation completion.

**Phase 0 (Safety & Stability):**
- ConfigManager: atomic CAS update with `MutableStateFlow.update`, Mutex for file writes, corrupted config backup preservation.
- AppLogStore: async channel-based logging with injectable `CoroutineDispatcher`, `runBlocking { channel.send() }` for synchronous test behavior.
- SpoofRepository: `AtomicReference` caches for SIM, location, device hardware configs.
- AppScopeRepository: `AtomicBoolean` for cache validity.
- RootShell: separate threads for stdout/stderr before `waitFor()`, suspend guard with `withContext(Dispatchers.IO)`.
- RootLogCollector: strict target package regex validation `[a-zA-Z0-9._]+`, conditional grep pattern building.
- LogManager: all file I/O wrapped in `withContext(Dispatchers.IO)`.
- Manifest: `allowBackup=false`, `windowSoftInputMode="adjustResize"`.
- SupportBundleBuilder: streaming ZIP instead of `readText()` OOM risk.
- JsonlDiagnosticStore: `@Synchronized` on `readEvents()`.
- ConfigSync: suspend IO variants `syncAppAsync()`/`clearAppAsync()`.

**Phase 1 (Testing Infrastructure):**
- `MainDispatcherRule` for coroutine testing.
- 7 fake implementations for testability.
- ViewModel tests for Home, Groups, GroupSpoofing, Settings, Diagnostics screens.
- SettingsViewModel bug fix: `ioDispatcher` injection instead of hardcoded `Dispatchers.IO`.

**Phase 2 (M3E Theme Foundation):**
- All hardcoded colors extracted to named constants in `Color.kt`.
- No hardcoded `Color(0x...)` remains under `ui/` outside `Color.kt`.
- Complete surface container roles in `LightColorScheme`.
- AMOLED scheme documented as intentional deviation.
- Category colors derived from `MaterialTheme.colorScheme`.
- Status colors mapped to semantic theme roles.
- 10-step symmetric shape scale + asymmetric variants.
- 15 emphasized typography styles + `LocalEmphasizedTypography` composition local.
- API 34 contrast preference support implemented; high-contrast visual validation remains open.

**Phase 3 (Architecture & State):**
- `SavedStateHandle` injected in all 5 ViewModels.
- All State classes marked `@Immutable` with `ImmutableList`.
- Inline lambdas hoisted with `remember`.
- `Flow.combine` in HomeViewModel for 6 flows.
- Redundant suspend modifiers removed.
- `importGroups()` returns `Result<Unit>` with typed errors.
- Full-screen loading overlays replaced with inline indicators.
- Bidirectional pager/tab sync fixed with `snapshotFlow` + `distinctUntilChanged`.
- GroupSpoofingScreen navigates back on null group (deleted).
- IME insets handled with `imePadding()` + `adjustResize`.
- `contentDescription` added to critical icons.
- `SimpleDateFormat` cached with `remember`.
- Dialog visibility uses `rememberSaveable`.
- Loading state uses `AnimatedVisibility` not `Modifier.alpha()`.
- `contentType` added to LazyColumns.
- SettingsScreen export mode synced with `LaunchedEffect`.

**Phase 4 (Motion & Components):**
- `MotionTokens` with Expressive/Standard × spatial/effects hierarchy.
- `ElevationTokens` Level 0-5.
- `MotionScheme.expressive()` adopted in `MaterialExpressiveTheme`.
- ExpressiveIconButton default size ≥48dp, Compact uses `minimumInteractiveComponentSize()`.
- `surfaceColorAtElevation()` used for the remaining custom elevated surface.
- Raw tonal-elevation dp values replaced with `ElevationTokens`.
- All `scale()` modifiers replaced with `graphicsLayer`.
- ExpressiveSwitch, ExpressiveCard, ExpressiveIconButton use fast spatial spring.
- Reduced-motion manual animation-scale-off validation remains open.

**Phase 5 (Dependency Upgrade):**
- BOM upgraded to `2026.04.01`.
- Material3 upgraded to `1.5.0-alpha18`.
- `MaterialExpressiveTheme`, native `LoadingIndicator`, native `ContainedLoadingIndicator`, native `ButtonGroup`, `SplitButtonLayout`, `FloatingActionButtonMenu`, `HorizontalFloatingToolbar`, and `MaterialShapes.SoftBurst` adopted and compile.
- Legacy `ToggleButton` removed after production callers migrated to Material/Expressive switch patterns.

**Phase 6 (Navigation):**
- Navigation 3 `NavKey` route types in `NavDestination`.
- Navigation Compose 2.x runtime navigation removed.
- `NavDisplay`, `entryProvider`, saveable-state entry decorator, and ViewModel-store entry decorator adopted.
- `DeviceMaskerNavigationState` preserves Home, Groups, and Settings top-level stacks.
- Selected top-level destination restoration has automated unit coverage.
- `DeviceMaskerNavigator` centralizes screen navigation and back behavior.
- M3E navigation motion and reduced-motion fallback are wired to `NavDisplay`.
- Adaptive Navigation 3 list-detail metadata added for Groups -> Group Detail.
- Deep links implemented with explicit parser, manifest `ACTION_VIEW` filter, `singleTop` `onNewIntent` handling, and synthetic stack replacement.
- Navigator unit tests pass, including restored selected top-level state and deep-link parsing/stack replacement.

**Phase 7 (Build Hardening):**
- `ciRelease` build type with `isMinifyEnabled=true`.
- Compose compiler metrics enabled.
- Spotless ktfmt version in catalog.
- Turbine + MockK test dependencies.
- Redundant daemon property removed from `gradle.properties`.
- IDE-specific build logic removed.
- Deprecated Spotless `indentWithSpaces` removed.
- Unused Android build features `resValues` and `shaders` disabled globally.
- Unused `BuildConfig` generation removed from `:common`.
- Obsolete `android.uniquePackageNames=false` removed.
- 16 KB page-size APK verifier added and passing for current debug APK.

**Phase 8 (Polish):**
- @Preview added to SpoofTabContent and AppsTabContent.
- Window size class adaptation with NavigationRail for medium/expanded screens.
- Compact Mobile MCP smoke passed across Home, Configure, Group Detail, Apps tab, Groups, and Settings.
- Advanced M3E component smoke passed for Settings export split buttons, Groups floating action button menu, and Group Detail horizontal toolbar.

**Phase 9 (Validation):**
- Full gate passes: `spotlessCheck`, all unit tests, `lint`, `test`, `assembleDebug`, `assembleRelease`, `:app:assembleCiRelease`.
- `com.mantle.verify` and `flar2.devcheck` smoke runs passed with LSPosed hook registration and spoof-event evidence.

### 2026-05-05 Build, 16 KB, And Runtime Verification

- Full gate passed: `.\gradlew.bat spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug assembleRelease :app:assembleCiRelease --warning-mode all --no-daemon`.
- 16 KB verifier passed: `powershell -ExecutionPolicy Bypass -File scripts\verify-16kb-page-support.ps1 app\build\outputs\apk\debug\app-debug.apk`.
- Mobile MCP installed and launched the rebuilt debug APK on `emulator-5554`.
- Device Masker UI showed `Protection Active`, `Module Enabled`, 2 protected apps, and 20 masked IDs.
- ADB smoke launched `com.mantle.verify`; `pidof` returned `14187`.
- LSPosed/logcat showed `XposedEntry loaded for process: com.mantle.verify`.
- LSPosed/logcat showed `All hooks registered for: com.mantle.verify`.
- Spoof events included ANDROID_ID, CARRIER_MCC_MNC, NETWORK_OPERATOR, IMEI, BLUETOOTH_MAC, WIFI_MAC, WIFI_SSID, and PHONE_NUMBER.
- Mobile MCP observed Mantle showing spoofed model `Nothing A065` and spoofed fingerprint `Nothing/Pong/Pong:14/AP31.240617.009/2409251803:user/release-keys`.
- `graphify update .` completed and refreshed `graphify-out`.

### 2026-05-06 R8 Libxposed Callback Runtime Fix

- Reproduced the release Mantle crash after the partial revert: R8 release built and installed, but `com.mantle.verify` died with `AbstractMethodError` from libxposed `XposedInterface.Hooker.intercept(...)`.
- Tested smaller R8 variants first. `-dontobfuscate` alone failed, and `-dontobfuscate` plus `-dontoptimize` still failed, so the root cause was the direct Kotlin SAM callback shape passed to libxposed, not only name obfuscation or optimization.
- Added `StableHooker`/`stableHooker` in `:xposed` and mechanically routed production hook registrations through `intercept(stableHooker { ... })`.
- Added `R8HookerAbiTest` to prevent runtime hookers from reintroducing direct `.intercept { ... }` callbacks and to verify R8 rules keep the callback package.
- Verification passed: `.\gradlew.bat spotlessApply spotlessCheck :xposed:testDebugUnitTest :app:assembleRelease --no-daemon`.
- Signed and installed the full R8 release APK. Size stayed in the target range: `app-release-unsigned.apk` 4,007,831 bytes and `app-release-debugkey-signed.apk` 4,069,566 bytes.
- Runtime smoke passed on `emulator-5554`: `com.mantle.verify` PID `7437`, `flar2.devcheck` PID `7955`; both showed `XposedEntry loaded`, target selection, `All hooks registered`, spoof events, and no `AbstractMethodError`/`FATAL EXCEPTION` in the checked log windows.
- User confirmed the R8 build also works on a real Android 16 device.

### 2026-05-06 StrictMode And Detekt Guardrails

- Added debug-only `StrictModeGuard` in `:app`; it installs thread and VM StrictMode policies with `penaltyLog()` only.
- Installed StrictMode from `DeviceMaskerApp` only. `:xposed` production sources remain StrictMode-free and are covered by `StrictModeIsolationTest`.
- Added Detekt 2.0.0-alpha.3 plus Compose Detekt rules 0.5.8 to the version catalog.
- Configured Detekt centrally for Android modules with reports enabled and module baselines.
- Created `config/detekt.yml` from Detekt 2.0.0-alpha.3 generated schema, preserving the Android Ninja template's policy intent where compatible.
- Added `xposed/detekt.yml` overrides for defensive hook patterns.
- Gated Compose compiler reports and metrics behind Gradle properties.
- Added Detekt to CI after Spotless.

## User-Owned External Validation

Accepted as user-owned completion for master-plan closure:

- Validate after emulator/device reboot.
- Validate after LSPosed module disable/enable cycle.
- Validate at least two additional target apps.
- Confirm actual returned values inside target apps, not only spoof event logs.
- Confirm disabled/missing/malformed values pass through.
- Confirm anti-detection behavior with current safer surfaces.
- Validate per-app risky-hook and class lookup hiding opt-ins in LSPosed target processes.
- Test package visibility hiding with API 33+ flag object overloads.
- Export logs after target hook events and verify exported output is useful.
- Smoke-test Basic, Full Debug, and Root Maximum bundle export on device.
- Reboot emulator/device and verify `BootCaptureReceiver` creates a `boot` root capture.
- Complete light/dark/AMOLED/dynamic-color visual matrix, large-font, high-contrast, and TalkBack/Accessibility Scanner audits.
- Manually validate full Navigation 3 stack restoration after process death.

Engineering cleanup accepted as deferred/user-owned:

- Clean AGP 10 deprecation warnings.
- Add more hook helper tests.
- Keep docs and Memory Bank current after every runtime validation result.
