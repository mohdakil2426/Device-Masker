# Active Context: Device Masker

## Current Focus

Release R8 is enabled and runtime-validated. Direct Kotlin SAM callbacks passed to libxposed `HookBuilder.intercept { ... }` caused Mantle release crashes with `AbstractMethodError`; the durable path is `StableHooker`/`stableHooker`, with production hookers using `intercept(stableHooker { ... })` or explicit named `XposedInterface.Hooker` implementations. Emulator smoke passed on `com.mantle.verify` and `flar2.devcheck`, and the user confirmed the same R8 build works on a real Android 16 device. Latest checked release APK size was about 4.0 MB unsigned.

## 2026-05-07 Dependency Candidate Updates

- Updated the agreed low-risk candidates after checking Google Developer Knowledge, web search, and Maven metadata: Gradle wrapper `9.5.0`, Spotless `8.4.0`, Compose BOM `2026.05.00`, Material3 `1.5.0-alpha19`, and `adaptive-navigation3` `1.3.0-beta01`.
- Catalog currently contains Kotlin `2.3.21`; this was present in the working diff during the update and was verified by the build, but it was not part of the agreed candidate set.
- `spotlessApply` was required because the dependency gate exposed existing formatting violations; it mechanically formatted touched Kotlin files.
- Verification passed: `.\gradlew.bat spotlessCheck detekt :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint assembleDebug --no-daemon` and `.\gradlew.bat assembleRelease :app:assembleCiRelease --no-daemon`.
- `graphify update .` refreshed the graph after source formatting changes.

## 2026-05-07 Single Root-Backed Export Implementation

- Settings export is now a single `Export Logs` sheet with Save and Share actions.
- Basic/Full/Root-mode selection was removed from production state and service APIs.
- Export always builds the maximum support bundle internally: app JSONL events, diagnostic snapshots, latest boot/startup root capture, and a fresh export-time root/logcat snapshot when root is granted.
- If root is unavailable, export still creates a ZIP and writes a root-unavailable manifest instead of disabling export.
- `RootLogCollector` no longer hardcodes `com.mantle.verify`; filtered logcat uses generic DeviceMasker/LSPosed/hook evidence terms plus an optional validated target package.
- Shared log URI grants now include `ClipData` with `FLAG_GRANT_READ_URI_PERMISSION`.

## 2026-05-07 Navigation 3 Audit Cleanup

- Hardened Navigation 3 click navigation by adding `dropUnlessResumed` to bottom navigation,
  navigation rail, group-card navigation, home configure navigation, and Settings diagnostics
  navigation callbacks.
- Fixed local `$navigation-3` recipe documentation corruption in `results-event.md` and
  `results-state.md` by removing embedded GitHub URLs from `PersonDetailsForm : NavKey` snippets.
- Clarified the local Navigation 3 migration guide deep-link wording: deep links are not covered by
  the migration guide and should follow the deep-link recipes.
- Updated `docs/internal/reports/NAVIGATION3_AUDIT_REPORT.md` to stop overclaiming full app
  production readiness. The report now scopes the verdict to the Navigation 3 layer and marks the
  local documentation defects as fixed.
- Verification: `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --tests com.astrixforge.devicemasker.ui.navigation.DeviceMaskerNavigatorTest --no-daemon` passed; `graphify update .` refreshed the graph.

## 2026-05-07 Custom AIDL Removal

- Removed the Diagnostics screen `Service Status` card because it surfaced the custom
  `user.devicemasker_diag` bridge as an error-prone live signal.
- Removed the custom AIDL architecture: app `ServiceClient`/`IServiceClient`, common
  `IDeviceMaskerService.aidl`, Xposed `DeviceMaskerService`, `DiagnosticsLogBuffer`, and
  `SystemServiceHooker`.
- `DiagnosticsViewModel` no longer injects or reads service state; diagnostics now show module
  connection, config-sync guidance, anti-detection checks, and local real/spoofed comparisons.
- Hook-side evidence now stays in LSPosed/logcat plus hook-health metrics. Support bundles contain
  app JSONL events, redacted snapshots, and optional root/logcat artifacts.
- AIDL build features are disabled in `:app`, `:common`, and `:xposed`. R8 keep rules for the deleted
  Binder types were removed.
- Root cause confirmed again: target-process hook events are authoritative in LSPosed/logcat. The
  custom Binder path was unreliable and added dead architecture with no config-delivery role.

## 2026-05-06 Coroutines And Performance Audit Remediation

- Addressed live findings from `docs/internal/reports/coroutines-audit-report.md` and `docs/internal/reports/performance-audit-report.md` without changing Xposed config delivery or hook enablement semantics.
- Removed production `runBlocking` and busy `Thread.sleep` polling from `AppLogStore`; app log append now uses non-blocking channel send and flush waits on a monitor.
- Removed `runBlocking` from `ConfigManager.resetForTests()`.
- Removed the stored `Context` property and `StaticFieldLeak` suppression from `SpoofRepository`; singleton creation still uses `context.applicationContext`.
- Consolidated `HomeViewModel` collection through `Flow.combine` instead of separate redundant collectors.
- Hardened group import null stream handling and wrapped the group spoofing app-list load in a non-crashing `runCatching`.
- Precompiled common validation regexes in `SharedPrefsKeys` and `Utils`.
- Made `Country` `@Serializable`.
- Optimized Xposed hot paths: `SensorHooker` no longer reflects for `Sensor.getType()` inside the callback, and `AntiDetectHooker.filterStackTrace()` returns the original stack array when no hidden frame exists while using manual filtering for hidden frames.
- Audit findings intentionally not applied: `XposedPrefs` already uses `CopyOnWriteArrayList`, so the reported concurrent modification issue is stale; `useLegacyPackaging=true` remains because the project guide documents it as required for primary-dex Xposed class loading.
- Verification: `spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest --no-configuration-cache` passed; `lint test assembleDebug assembleRelease --no-configuration-cache` passed; static searches found no production `runBlocking`, `Thread.sleep`, sensor `getType` reflection lookup, `StaticFieldLeak` suppression, import `checkNotNull(inputStream)`, or stack-frame `filterNot` allocation. `graphify update .` refreshed the graph.

## 2026-05-06 StrictMode And Detekt Guardrails

- Added app-process-only `StrictModeGuard` for debug builds. It is installed from `DeviceMaskerApp` after the application singleton is assigned and uses `penaltyLog()` only.
- `:xposed` runtime code does not install StrictMode. `StrictModeIsolationTest` guards this boundary by scanning production Xposed Kotlin sources.
- Detekt is configured across `:app`, `:common`, and `:xposed` from root Gradle configuration with `config/detekt.yml`, module overrides, SARIF/HTML/checkstyle reports, and per-module baselines.
- The Android Ninja Detekt template used older Detekt rule property names. The live implementation uses a Detekt 2.0.0-alpha.3 generated config as the schema base, then reapplies the template's intended Android/Compose policy choices with validation enabled.
- `xposed/detekt.yml` relaxes generic exception, complexity, return-count, and magic-number rules for defensive hook code without changing hook runtime behavior.
- Compose compiler reports/metrics are no longer always emitted. They are gated behind `enableComposeCompilerReports` and `enableComposeCompilerMetrics`.
- CI quality now runs `detekt` after Spotless and uploads the existing module report directories.

## 2026-05-06 Build Audit, R8 Enablement, and Docs Reorganization

### R8 Enabled in Release
- Changed `release` build type from `isMinifyEnabled = false` to `isMinifyEnabled = true` and `isShrinkResources = true`.
- Root cause of the release Mantle crash: direct Kotlin SAM callbacks generated for `xi.hook(m).intercept { chain -> ... }` were not a durable libxposed callback ABI under release R8 and produced `AbstractMethodError` in target processes.
- Fix: Added `StableHooker`/`stableHooker` and converted production runtime hookers to `intercept(stableHooker { ... })`, so libxposed receives a concrete kept `XposedInterface.Hooker` object.
- Keep rules still preserve libxposed API callback types and `xposed.hooker.callback.**`, but keep rules alone were not enough; `-dontobfuscate` and `-dontoptimize` experiments still crashed.
- R8 release build passes and runtime smoke passed on emulator targets. User confirmed the same R8 build works on a real Android 16 device.

### ProGuard Cleanup
- `common/consumer-rules.pro`: Reduced from 128 to 97 lines. Removed broad `*;` wildcard, redundant generator/INSTANCE/data-class rules.
- `xposed/consumer-rules.pro`: Removed redundant SystemServiceHooker and BaseSpoofHooker rules (covered by `hooker.**` wildcard). Added back PrefsHelper.

### Dependencies Verified
- `datastore-preferences` — used by `SettingsDataStore.kt` (theme settings). Keep.
- `material:1.13.0` — needed for XML theme parent. Keep.
- `kotlinx-serialization-json` in `:xposed` — used by `XposedDiagnosticEventSink.kt`. Keep.
- `maven.aliyun.com` — removed from `settings.gradle.kts`.

### CI Pipeline
- Added `cache-read-only` for downstream jobs in `ci.yml`.
- Added `--build-cache` flag to all Gradle commands.

### Documentation
- Reorganized `docs/` into `public/`, `internal/`, `superpowers/`.
- Deleted stale `RUNBOOK.md` (R8/YukiHookAPI references).
- Cleaned `ARCHITECTURE.md` to pure architecture.
- Created `docs/internal/reports/BUILD_AUDIT_AND_R8_ENABLEMENT_2026-05-06.md`.

### Agent Guides
- Created `AGENTS.md` (root), `app/AGENTS.md`, `common/AGENTS.md`, `xposed/AGENTS.md`.
- Each contains folder structure, key files, constraints, and module-specific guidance.

### Cleanup
- Removed stale `package-info.kt` files (3), `ExampleUnitTest.kt`.
- Cleaned `.gitignore` (YukiHookAPI remnants, dead refs, duplicates).
- SpoofType count corrected from 27 to 24 in all agent guides.
- NETWORK_COUNTRY_ISO moved from NETWORK to DEVICE category in agent guides.

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
- Release minification and resource shrinking are enabled; hook registration must avoid direct Kotlin SAM `.intercept { ... }` callbacks and use `StableHooker`/named `XposedInterface.Hooker` implementations.
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
- Root shell and root/logcat collector for bounded logcat, ANR, tombstone, dumpsys, and getprop artifacts.
- Redacted snapshots and single support bundle ZIP export path.

Root/logcat export still needs rooted-device smoke validation. Target LSPosed smoke has not been rerun after this diagnostics work.

## 2026-05-03 Diagnostics And Root Export Audit Fixes

Implemented the follow-up audit fixes for diagnostics and root evidence:
- Diagnostics UI state now distinguishes app-side framework connection from optional diagnostics service availability and hook evidence. Unavailable service-backed hook evidence is represented as unavailable/unknown instead of `0`.
- Support share/export no longer short-circuits as `NoLogs` before root collection.
- Support bundles now invoke `RootLogCollector`, include `root/` artifacts, and snapshot actual root/service availability.
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

Rooted-device export and target LSPosed runtime smoke still need to be rerun.

Runtime smoke rerun after these fixes:
- Installed `app/build/outputs/apk/debug/app-debug.apk` on `emulator-5554`.
- Force-stopped and launched `com.mantle.verify`.
- `pidof com.mantle.verify` returned `7537`.
- Logcat showed `XposedEntry loaded for process: com.mantle.verify`.
- Logcat showed `All hooks registered for: com.mantle.verify`.
- Logcat showed spoof events for LOCALE, ANDROID_ID, CARRIER_MCC_MNC, NETWORK_OPERATOR, IMEI, BLUETOOTH_MAC, WIFI_MAC, WIFI_SSID, PHONE_NUMBER, ADVERTISING_ID, MEDIA_DRM_ID, SIM_OPERATOR_NAME, and TIMEZONE.
- No matching final-window fatal signatures were found for `FATAL EXCEPTION`, `PatternSyntaxException`, `Cannot hook abstract`, `AbstractMethodError`, or `WorkManagerInitializer`.

Root/logcat in-app export still needs manual/UI validation on the rooted device.

## 2026-05-04 Startup Root And Boot Capture

Implemented root-first diagnostics flow:
- `RootAccessManager` now owns root state: unknown, requesting, granted, denied, unavailable.
- First app launch/startup requests root from `MainActivity`; denial/unavailable shows a warning dialog explaining that root logging, boot capture, and privileged diagnostics will not work.
- Settings shows root access status while keeping the single export action available when root is not granted.
- `RootShell` no longer probes with `Shell.getShell()` during export. Export uses root only when `Shell.isAppGrantedRoot()` already reports granted, avoiding the late root prompt after folder selection.
- Added `RootLogCaptureService` as a `specialUse` foreground service for bounded root capture.
- Added `BootCaptureReceiver` for `BOOT_COMPLETED`; it starts the foreground root capture service after device boot.
- Startup root grant starts the same capture service immediately with trigger `startup`.
- Export packages the latest captured root artifacts and can add an export snapshot only if root is already granted.

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

**Phase 2 (M3E Theme):** Extracted named colors to `Color.kt`, removed hardcoded `Color(0x...)` outside `Color.kt` under `ui/`, added surface container roles to `LightColorScheme`, AMOLED KDoc, 10-step shape scale + asymmetric variants, 15 emphasized typography styles + `LocalEmphasizedTypography`, `UIDisplayCategory.themeColor()` mapping, and API 34 contrast preference tracking with high-contrast role overrides. High-contrast visual validation remains user-owned.

**Phase 3 (Architecture):** SavedStateHandle injected into all 5 ViewModels, `@Immutable` + `ImmutableList` on all State classes, `Flow.combine` in HomeViewModel, redundant suspend modifiers removed, `importGroups` returns `Result<Unit>`, loading overlays replaced, pager/tab sync fixed, contentDescription added, SimpleDateFormat cached, rememberSaveable for dialogs, AnimatedVisibility for loading.

**Phase 4 (Motion):** `MotionTokens` Expressive/Standard hierarchy, `ElevationTokens`, `MotionScheme.expressive()`, springs updated in components, touch targets ≥48dp in touched components, `graphicsLayer` instead of `scale()`, `surfaceColorAtElevation()` for the remaining custom elevated surface, and raw tonal-elevation dp values replaced with elevation tokens. Reduced-motion manual validation with animation scale disabled remains open.

**Phase 5 (Dependencies/M3E components):** Upgraded BOM to `2026.04.01` and material3 to `1.5.0-alpha18`. Adopted `MaterialExpressiveTheme`, `MotionScheme.expressive()`, native `LoadingIndicator`, native `ContainedLoadingIndicator`, native `ButtonGroup`, `SplitButtonLayout` for export actions, `FloatingActionButtonMenu` for group quick actions, `HorizontalFloatingToolbar` for group editing, and `MaterialShapes.SoftBurst` in the Home hero. Removed the unused legacy `ToggleButton` component.

**Phase 6 (Navigation):** Navigation 3 `NavKey` `NavDestination`, `NavDisplay` + `entryProvider`, explicit top-level stacks in `DeviceMaskerNavigationState`, M3E transition specs, adaptive Groups -> Group Detail list-detail metadata, and navigator unit tests.

**Phase 7 (Build):** `ciRelease` build type with minification, Compose compiler metrics, Spotless ktfmt in version catalog, turbine/mockk test deps, `windowSoftInputMode="adjustResize"`, redundant daemon property removed. On 2026-05-05, Gradle cleanup also removed deprecated Spotless `indentWithSpaces`, disabled unused `resValues` and `shaders` build features globally, removed unused `BuildConfig` generation from `:common`, and removed obsolete `android.uniquePackageNames=false`.

**16 KB page-size support:** Current Android docs were checked through Google Developer MCP and web search. The app has no first-party native code, but the APK includes transitive native libraries from AndroidX/DataStore. Added `scripts/verify-16kb-page-support.ps1` to verify `zipalign -c -P 16` and ELF `PT_LOAD` 16 KB alignment for packaged `.so` files. The rebuilt debug APK passed this check for all 8 native library entries.

**2026-05-05 verification:** Full Gradle gate passed with `spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug assembleRelease :app:assembleCiRelease --warning-mode all --no-daemon`. Mobile MCP installed and launched the rebuilt debug APK on `emulator-5554`; Device Masker showed `Protection Active` and `Module Enabled`. ADB target smoke for `com.mantle.verify` returned a live PID, LSPosed logs showed `XposedEntry loaded`, `All hooks registered`, and spoof events for Android ID, carrier/network operator, IMEI, Bluetooth MAC, Wi-Fi MAC/SSID, and phone number. Mobile MCP observed Mantle displaying spoofed device model `Nothing A065` and spoofed fingerprint `Nothing/Pong/Pong:14/AP31.240617.009/2409251803:user/release-keys`.

**Phase 8 (Polish):** @Preview added to touched tab/components, window size class adaptation with NavigationRail for medium/expanded screens, compact Mobile MCP smoke across Home/Configure/Group Detail/Apps/Groups/Settings, and advanced M3E smoke for Settings export split buttons, Groups FAB menu, and Group Detail horizontal toolbar.

**Phase 9 (Validation):** Full gate passes — `spotlessCheck`, all unit tests (`:common`, `:app`, `:xposed`), `lint`, `test`, `assembleDebug`, `assembleRelease`, and `:app:assembleCiRelease`. ADB smoke on 2026-05-04 showed `com.mantle.verify` and `flar2.devcheck` alive with `XposedEntry`, `All hooks registered`, and spoof events; no matching fatal crash signatures appeared in the tested log windows.

**2026-05-06 implementation cleanup:** Added API 34 contrast preference support, replaced remaining raw elevation literals in touched UI with `ElevationTokens`, removed the unused legacy `ToggleButton`, and added per-app risky-hook/class-lookup opt-ins. Config sync now writes both opt-in keys to RemotePreferences, and `AntiDetectHooker` only registers class lookup hiding when both app-level switches are enabled.

**2026-05-06 master plan closure:** `docs/superpowers/plans/MASTER_IMPLEMENTATION_PLAN_2026-05-04.md` is closed at 100% for implementation. Remaining manual/device/testing checks are marked complete as user-owned external validation, not as newly executed evidence from this agent.

**2026-05-06 R8/libxposed release crash fix:** Release R8 remained enabled, but direct Kotlin SAM callbacks passed to `HookBuilder.intercept { ... }` reproduced the Mantle crash with `AbstractMethodError` in `com.mantle.verify`. The fix is a small `StableHooker` adapter in `xposed.hooker.callback`: production hookers now call `intercept(stableHooker { ... })`, so libxposed receives a concrete kept `XposedInterface.Hooker` object while existing hook logic remains unchanged. Temporary `-dontobfuscate` and `-dontoptimize` experiments did not fix the crash and were removed. Verified release APK size stayed about 4.0 MB unsigned, and full R8 runtime smoke passed on `com.mantle.verify` and `flar2.devcheck` with live PIDs, `XposedEntry loaded`, `All hooks registered`, and spoof events. User later confirmed the same R8 build path works on a real Android 16 device.

## User-Owned Validation Items

High priority:
- Repeat `com.mantle.verify` validation after reboot and LSPosed module toggle.
- Validate at least two more target apps.
- Confirm configured spoof values match UI-generated stored values.
- Confirm disabled/missing/malformed values pass through to originals.
- Verify LSPosed log export after target hook events.
- Verify PackageManager hiding on API 33+ flag object overloads.
- Verify anti-detection behavior with class lookup hooks disabled by default and enabled only through the per-app opt-in path.
- Revisit M3E component migration when material3 1.5.0 reaches stable.

Medium priority:
- Add more unit tests for hook conversion helpers.
- Continue monitoring AGP warnings during full gates.
- Improve UI wording around service connected vs target hooked vs spoof observed.

These items are accepted as user-owned completion for plan closure. Do not cite them as agent-run verification until the user provides runtime/test evidence.

## Working Assumptions

- This is a development build.
- Stability beats spoof breadth.
- LSPosed runtime validation is required before stability claims.
- The first working base should be protected from broad refactors.
- New hook areas should start disabled or pass-through safe until proven.
