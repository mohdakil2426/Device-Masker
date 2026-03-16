# Common + Xposed Audit Report

Date: 2026-03-16  
Scope: `common/` and `xposed/`  
Auditor: Codex

## Executive Summary

This audit reviewed the full `common/` and `xposed/` source trees for code quality, architecture, correctness, concurrency safety, anti-detection robustness, spoofing consistency, documentation drift, testing depth, and operational observability.

The codebase has several strong foundations:

- Shared contracts and preference keys are centralized in `:common`.
- Generator code consistently uses `SecureRandom`.
- Hook registration is generally defensive, with per-hook try/catch boundaries.
- Diagnostics AIDL methods are correctly marked `oneway` for hook-side reporting.
- The migration to libxposed API 100 is mostly structurally sound.

However, the audit found several material issues:

- The hook state model is not safe under libxposed's documented multi-package-per-process callback behavior.
- The diagnostics service uses a shared `SimpleDateFormat` from concurrent binder threads.
- The `xposed/` package still contains stale Yuki-era entrypoint artifacts and dead bridge code.
- Several hook implementations have important edge-case gaps, especially around interface-specific spoofing and anti-detection coverage.
- Error handling and diagnostics are fragmented, which weakens production observability.
- Test depth is thin relative to the amount of spoofing and hook surface area.

## Methodology

This audit used:

- Full review of the memory bank:
  - `memory-bank/projectbrief.md`
  - `memory-bank/productContext.md`
  - `memory-bank/systemPatterns.md`
  - `memory-bank/techContext.md`
  - `memory-bank/activeContext.md`
  - `memory-bank/progress.md`
- Static review of all source files under:
  - `common/src/main`
  - `common/src/test`
  - `xposed/src/main`
- Review of local libxposed source and project-local docs under:
  - `docs/libxposed/README.md`
  - `docs/libxposed/libxposed-api/api/100/src/io/github/libxposed/api/XposedInterface.java`
  - `docs/libxposed/libxposed-api/api/100/src/io/github/libxposed/api/XposedModuleInterface.java`
  - `docs/libxposed/libxposed-service/README.md`
- External research via:
  - Google Developer MCP
  - Context7
  - official web sources

## External References Used

Official Android:

- https://developer.android.com/develop/ui/compose/state
- https://developer.android.com/develop/ui/compose/performance/baseline-profiles
- https://developer.android.com/develop/ui/compose/api-guidelines

Official libxposed / LSPosed:

- https://libxposed.github.io/api/io/github/libxposed/api/XposedInterface.Hooker.html
- https://libxposed.github.io/api/io/github/libxposed/api/XposedModuleInterface.html
- Local clone: `docs/libxposed/libxposed-api/api/100/src/io/github/libxposed/api/XposedModuleInterface.java`
- Local clone: `docs/libxposed/libxposed-service/README.md`

Key research conclusions applied in this audit:

- libxposed hookers are expected to use static `before` and `after` methods on `XposedInterface.Hooker`.
- `onPackageLoaded()` can be invoked multiple times for the same process on each loaded package.
- `deoptimize()` is a relevant tool for methods that may otherwise be bypassed by inlining.
- Android still recommends single-source-of-truth state ownership and explicit performance verification with Baseline Profiles and Macrobenchmark for app-facing surfaces.

## Scope Inventory

Source inventory reviewed:

- `common/src/main`: 26 Kotlin/source files plus AIDL and manifest
- `common/src/test`: 4 test files
- `xposed/src/main`: 19 Kotlin/source files plus module metadata/resources

Primary reviewed areas:

- Shared models and serialization contracts
- Identifier generators and correlation logic
- Shared prefs keying
- libxposed entrypoint and hook lifecycle
- spoof hookers
- anti-detection hookers
- diagnostics AIDL service and service bootstrap
- xposed packaging resources and migration residue

## Severity Summary

- Critical: 0
- High: 3
- Medium: 7
- Low: 5

## High Severity Findings

### 1. Hook state is process-global but libxposed explicitly allows multiple package loads in one process

Impact:

- Spoofed values can be resolved against the wrong package.
- Device profile mutations can bleed from one loaded package to another inside the same process.
- This can silently corrupt per-app isolation, which is a core privacy requirement.

Evidence:

- `docs/libxposed/libxposed-api/api/100/src/io/github/libxposed/api/XposedModuleInterface.java:93`
  - libxposed documents that `onPackageLoaded()` can be invoked multiple times for the same process.
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/XposedEntry.kt:115`
  - package hooks are registered from `onPackageLoaded`.
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/DeviceHooker.kt:48`
  - writes `prefs` and `pkg` into shared hook state.
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/DeviceHooker.kt:274`
  - shared `HookState` is static and process-wide.
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/NetworkHooker.kt:27`
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/LocationHooker.kt:23`
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/AdvertisingHooker.kt:21`
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/SystemHooker.kt:31`

Assessment:

- This is an inference from official libxposed lifecycle docs plus the current shared-state implementation.
- The risk is highest for `SystemHooker`, because `Build.*` overrides are process-global by definition.

Recommendation:

- Stop storing target package state in global mutable hooker singletons.
- Resolve package identity per callback from the actual process/package context when possible.
- If package-scoped state must be cached, key it by package name or classloader instead of a single mutable slot.
- Treat `SystemHooker` as the first redesign target because it mutates process-global state.

### 2. `DeviceMaskerService` uses a shared `SimpleDateFormat` from concurrent binder threads

Impact:

- Timestamp formatting is not thread-safe.
- Under concurrent `oneway` report calls, timestamps can be corrupted or throw intermittently.
- This directly affects diagnostics reliability in the most concurrent part of the xposed layer.

Evidence:

- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/service/DeviceMaskerService.kt:83`
  - `private val logDateFmt = SimpleDateFormat(...)`
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/service/DeviceMaskerService.kt:86`
  - comments acknowledge concurrent binder-thread reporting.
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/service/DeviceMaskerService.kt:154`
  - `appendLog()` uses that shared formatter on each call.

Recommendation:

- Replace `SimpleDateFormat` with a thread-safe alternative.
- On Android, the cleanest fix is usually `java.time` where available, or per-call formatter creation if the volume is low.

### 3. Stale Yuki-era entrypoint metadata remains in `xposed/` and conflicts with the documented libxposed API 100 packaging model

Impact:

- Packaging and migration intent are unclear.
- Future release or tooling changes can accidentally revive the wrong entrypoint path.
- This increases maintenance risk for the module bootstrap path, which is one of the most fragile parts of any LSPosed module.

Evidence:

- `xposed/src/main/assets/xposed_init:1`
  - still points to `com.astrixforge.devicemasker.xposed._YukiHookXposedInit`
- `xposed/src/main/resources/META-INF/xposed/java_init.list:1`
  - points to `com.astrixforge.devicemasker.xposed.XposedEntry`
- `memory-bank/techContext.md`
  - documents `META-INF/xposed/java_init.list` as the correct libxposed entrypoint.

Recommendation:

- Remove `xposed/src/main/assets/xposed_init` if it is no longer part of the supported bootstrap path.
- Keep a single authoritative entrypoint declaration and document it once.

## Medium Severity Findings

### 4. `NetworkInterface.getHardwareAddress()` spoofs every interface with the Wi-Fi MAC

Impact:

- Apps inspecting `rmnet`, `eth0`, `lo`, `p2p`, or VPN interfaces can receive unrealistic values.
- The same spoof value is reused for interfaces that should either differ or return `null`.
- This weakens realism and can create easy fingerprint inconsistencies.

Evidence:

- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/NetworkHooker.kt:169`
  - callback ignores which `NetworkInterface` is being queried.
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/NetworkHooker.kt:173`
  - always resolves `SpoofType.WIFI_MAC`.

Recommendation:

- Inspect `callback.thisObject` and only spoof Wi-Fi-like interfaces with the Wi-Fi MAC.
- Preserve existing behavior for unrelated interfaces unless there is a deliberate spoofing policy for them.

### 5. Anti-detection coverage is partial for class-loading probes

Impact:

- Apps can still probe for framework presence through class-loading paths not currently intercepted.
- Anti-detection claims are stronger than the actual implementation surface.

Evidence:

- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/AntiDetectHooker.kt:129`
  - only hooks `ClassLoader.loadClass(String)`.
- No equivalent hook was found for:
  - `ClassLoader.loadClass(String, boolean)`
  - common `Class.forName(...)` paths

Assessment:

- This is a correctness gap, not proof of immediate bypass in every app.
- It matters because anti-detection is a headline feature of the project.

Recommendation:

- Expand coverage to the common alternate load paths.
- Keep the current allowlist discipline when doing so.

### 6. Diagnostics are fragmented: `reportLog()` exists but is effectively unused, while most failures only hit plain logcat

Impact:

- Diagnostics UI cannot reliably reflect actual hook failures.
- Production troubleshooting is harder than the architecture suggests.

Evidence:

- `common/src/main/aidl/com/astrixforge/devicemasker/IDeviceMaskerService.aidl:38`
  - diagnostics API includes `reportLog`.
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/service/DeviceMaskerService.kt:106`
  - service implements `reportLog`.
- Audit search found no active `reportLog(...)` callers in `xposed/src/main/kotlin`.
- Many callback failures use only `Log.w(...)`, for example:
  - `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/DeviceHooker.kt:298`
  - `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/NetworkHooker.kt:183`
  - `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/AntiDetectHooker.kt:137`
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/DualLog.kt:36`
  - maintains a separate in-memory buffer that is also not the same as the AIDL service log store.

Recommendation:

- Pick one diagnostics path and use it consistently.
- Either route hook failures into `reportLog()` or remove the dead API.
- Avoid keeping separate log buffers unless they serve distinct, documented purposes.

### 7. `SystemServiceHooker`'s `systemReady()` probe logic is narrower than its own comments claim

Impact:

- Service initialization can fail silently on Android variants whose `systemReady()` signature is outside the probed set.
- This affects diagnostics bootstrapping.

Evidence:

- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/SystemServiceHooker.kt:50`
  - comment says `systemReady()` varies from 1-5 parameters.
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/SystemServiceHooker.kt:52`
  - only probes 0, 1, and 2 parameter variants.

Recommendation:

- Either broaden the probe set to match the documented intent or reduce the claim in comments.
- Prefer exhaustive signature handling for boot-critical hooks.

### 8. `ServiceBridge` is dead or stale architecture residue

Impact:

- It adds cognitive load and misleads future maintenance.
- The file claims a provider-based bridge that is not actually declared in the xposed manifest.

Evidence:

- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/service/ServiceBridge.kt:12`
  - comments describe dynamic/provider bridge usage.
- `xposed/src/main/AndroidManifest.xml:1`
  - no provider is declared.
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/SystemServiceHooker.kt:97`
  - actual discovery path is `ServiceManager.addService(...)`, not the provider bridge.

Recommendation:

- Remove `ServiceBridge` if it is no longer used.
- If it is intended to remain, wire it fully and update the manifest and service client contract.

### 9. `JsonConfig.parseOrDefault()` silently resets to a default config on any parse failure

Impact:

- A malformed or forward-incompatible config can degrade into silent data loss.
- Recovery and diagnosis become harder because failure is swallowed.

Evidence:

- `common/src/main/kotlin/com/astrixforge/devicemasker/common/JsonConfig.kt:198`
  - parse failure path always returns `createDefault()`.

Recommendation:

- Return a typed error, or at minimum surface parse failures to callers.
- Consider preserving the raw payload or a backup copy before defaulting.

### 10. Regeneration semantics rely on `null` values and hook-time fallback generation, which can destabilize identity if callers persist nulls

Impact:

- A group can stop being a stable identity and become "generate on read".
- Different identifiers or repeated calls can drift if the app layer does not eagerly materialize and save regenerated values.

Evidence:

- `common/src/main/kotlin/com/astrixforge/devicemasker/common/DeviceIdentifier.kt:36`
  - default value contract is "null means auto-generate on use".
- `common/src/main/kotlin/com/astrixforge/devicemasker/common/SpoofGroup.kt:98`
  - `regenerateAll()` resets values to `null`.
- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/PrefsReader.kt:56`
  - `PrefsHelper.getSpoofValue()` falls back to generator when no stored value exists.

Assessment:

- This is an architectural risk. Whether it becomes a user-visible bug depends on how the app layer materializes regenerated values before sync.

Recommendation:

- Prefer explicit regeneration that produces concrete values immediately.
- Reserve hook-time fallback generation for bootstrap/default cases only.

## Low Severity Findings

### 11. Test coverage is light relative to the spoofing and hook surface

Evidence:

- `common/src/test` contains 4 test files.
- `common/src/main` contains 26 source files.
- No xposed-side tests were found for hook callback logic, anti-detection behavior, or diagnostics service concurrency.

Recommendation:

- Add tests for:
  - `ICCIDGenerator`
  - `IMSIGenerator`
  - `UUIDGenerator`
  - `SIMGenerator`
  - `LocationConfig`
  - `SharedPrefsKeys`
  - `JsonConfig` parse failure behavior
- Add JVM tests around hook callback helper logic where direct Android hooking is not testable.

### 12. `PackageManagerHooker` documents `queryIntentActivities()` coverage that is not implemented

Evidence:

- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/PackageManagerHooker.kt:29`
  - comment claims `queryIntentActivities(Intent, int)`.
- No such hook implementation exists in the file.

Recommendation:

- Either implement it or remove the claim.

### 13. Several comments still describe the old migration state rather than the current one

Evidence:

- `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/BaseSpoofHooker.kt:19`
  - comment still references annotation-driven API wording that no longer matches the implementation.
- `common/src/main/kotlin/com/astrixforge/devicemasker/common/JsonConfig.kt:16`
  - comments still describe older storage paths that are no longer the main runtime path.

Recommendation:

- Clean up migration residue in comments so the documentation matches the code that actually ships.

### 14. Dual-SIM support is explicitly unfinished in shared hardware modeling

Evidence:

- `common/src/main/kotlin/com/astrixforge/devicemasker/common/models/DeviceHardwareConfig.kt:49`
  - `isDualSIM` is hardcoded false with a TODO.
- `common/src/main/kotlin/com/astrixforge/devicemasker/common/DeviceProfilePreset.kt:92`
  - presets already model `simCount`.

Recommendation:

- Either wire `simCount` through hardware modeling and hookers or narrow the documented claim until dual-SIM correlation is fully implemented.

### 15. `IMEIGenerator.generateForPreset()` has an awkward early-return fallback branch

Impact:

- Behavior appears correct, but the implementation is hard to reason about and easy to break in future edits.

Evidence:

- `common/src/main/kotlin/com/astrixforge/devicemasker/common/generators/IMEIGenerator.kt:223`

Recommendation:

- Simplify the fallback branch into a direct explicit return with no nested `also { return ... }`.

## Positive Findings

- `SharedPrefsKeys` remains the clear single source of truth for cross-module key generation.
- Generator code consistently uses `SecureRandom`.
- Hook registration is generally isolated per method, which is the right failure-containment strategy for OEM variation.
- `IDeviceMaskerService` correctly uses `oneway` for hook-side reports.
- `SystemServiceHooker` correctly treats boot-context safety as a first-order concern and does not rethrow from `system_server`.

## Recommended Remediation Order

1. Redesign hook state ownership so it is safe for multiple package loads in the same process.
2. Replace shared `SimpleDateFormat` in `DeviceMaskerService`.
3. Remove stale `xposed_init` and other migration residue.
4. Fix `NetworkInterface.getHardwareAddress()` to be interface-aware.
5. Consolidate diagnostics logging so hook failures reach a single observable path.
6. Expand anti-detection class-loading coverage.
7. Decide whether `ServiceBridge` is real or dead and clean it up accordingly.
8. Strengthen tests around generators, config parsing, and hook helper behavior.

## Verification Notes

Static analysis for this audit is comprehensive across the reviewed source trees. I also attempted local Gradle verification for `:common` and `:xposed`, but this shell environment would not launch the wrapper batch file correctly, so I could not add a fresh compile/test result to this report from the current session.

That limitation does not change the source-based findings above, but it does mean this report should be paired with a real local run of:

```powershell
./gradlew spotlessApply
./gradlew spotlessCheck
./gradlew :common:test
./gradlew :common:compileDebugKotlin :xposed:compileDebugKotlin
./gradlew lint
```

