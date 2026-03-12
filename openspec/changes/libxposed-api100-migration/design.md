## Context

Device Masker is an LSPosed/Xposed module (3-module Gradle: `:app`, `:common`, `:xposed`) that spoofs 24+ device identifiers on Android 8-16. The current hook layer uses YukiHookAPI 1.3.1 (wrapping legacy Xposed API 82) with a hybrid AIDL + XSharedPreferences config delivery system.

**Current state problems (see `docs/planning/DEVICE_MASKER_LIBXPOSED_API100_PLAN.md` §3-4):**

- ART JIT/AOT inlines short hook targets — hooks silently never fire (no workaround in API 82)
- XSharedPreferences caches at process load — config changes need app restart
- AIDL service in `system_server` has bootloop risk and 50-200μs per-call binder overhead
- 10 spoofing gaps identified (SubscriptionManager, Build.\* fields, WebView static UA, etc.)
- `toClass()` usage causes crashes in isolated processes where classes don't exist

**Constraints:**

- Must maintain Android 8.0-16 (API 26-36) compatibility
- Module app UI (`:app`) stays on Jetpack Compose + Material 3 — no UI changes needed
- SharedPrefsKeys contract in `:common` is unchanged — key formats preserved
- LSPosed 1.10.2+ already required — supports modern API alongside legacy
- All changes scoped to `:xposed` module primarily; minimal `:app` and `:common` changes

**Reference documents:**

- `docs/planning/DEVICE_MASKER_LIBXPOSED_API100_PLAN.md` — Full 16-phase plan with code samples
- `docs/planning/DEVICE_MASKER_STORAGE_ARCHITECTURE_OPTIONS.md` — Option A vs B analysis

## Goals / Non-Goals

**Goals:**

1. Migrate from YukiHookAPI 1.3.1 / Xposed API 82 to libxposed API 100 (native modern API)
2. Enable `deoptimize()` on all critical hook targets to bypass ART inlining
3. Replace XSharedPreferences config delivery with libxposed `RemotePreferences` (live, no restart)
4. Demote AIDL service to diagnostics-only (Option B from storage architecture doc)
5. Close all 10 identified spoofing gaps with new/expanded hookers
6. Enrich `DeviceProfilePreset` with build metadata, TAC prefixes, hardware flags
7. Implement TAC-aware IMEI generation matching device profiles
8. Enforce full value correlation matrix across all 24 spoof types
9. Update ProGuard/R8 rules for API 100 patterns
10. Zero-crash guarantee: every hook callback wrapped in safety patterns

**Non-Goals:**

- No UI/UX changes — screens, navigation, theme, components are untouched
- No new SpoofType enum additions — existing 24 types cover all needs
- No changes to `:common` module's serialization models (JsonConfig, SpoofGroup, etc.)
- No changes to the group management/per-app assignment logic
- No root hiding, SafetyNet bypass, or Play Integrity bypass (out of scope per project brief)
- No Zygisk integration — LSPosed-only

## Decisions

### Decision 1: libxposed API 100 over YukiHookAPI continuation

**Choice:** Replace YukiHookAPI entirely with direct libxposed API 100.

**Why not keep YukiHookAPI?**

- YukiHookAPI wraps the legacy API — it cannot expose `deoptimize()` which is API 100 exclusive
- YukiHookAPI adds 3 transitive dependencies (kavaref-core, kavaref-extension, KSP processor)
- The DSL (`method { name = "..." }.hook { after { } }`) hides safety issues (bare `after{}` blocks)
- The modern API's static hooker pattern (`@XposedHooker` + `@BeforeInvocation`) is explicit and type-safe

**Why not a wrapper library around libxposed?**

- No mature Kotlin wrapper exists for API 100 (as of March 2026)
- Direct API usage gives full control over `deoptimize()`, `hookClassInitializer()`, `invokeOrigin()`
- The API surface is small — ~10 methods on `XposedInterface` — no wrapper needed

**Trade-off:** Every hooker must be rewritten. This is a one-time cost that eliminates the wrapper layer permanently.

### Decision 2: Option B — AIDL Demoted to Diagnostics Only

**Choice:** Keep AIDL service but strip all config methods, retaining only diagnostics (see `docs/planning/DEVICE_MASKER_STORAGE_ARCHITECTURE_OPTIONS.md` §5, §7).

**Why Option B over Option A (full AIDL removal)?**

- `DiagnosticsViewModel` already exists and uses hook event counts, log aggregation, service health
- Users see live hook statistics — this provides real value and is a differentiator
- Option A would gut the diagnostics screen to static-only (module active/inactive)
- The diagnostics AIDL service is now **non-fatal** — if it fails, hooks still work via RemotePreferences

**Why not keep AIDL for config too?**

- RemotePreferences has ~5μs latency vs 50-200μs AIDL binder calls
- RemotePreferences is LSPosed-managed — no bootloop risk from service init failures
- Eliminates the `ConfigManager.kt` in `:xposed` and the `/data/misc/` file persistence
- Removes the "config cascade" complexity (AIDL first → XSharedPreferences fallback)

**Architecture after migration:**

```
Config path:  UI → ConfigSync → ModulePreferences → LSPosed DB → RemotePreferences (in hooks)
Diag. path:   Hooks → oneway AIDL → DeviceMaskerService (system_server) → UI reads
```

### Decision 3: Static Hooker Pattern with Companion Object State

**Choice:** Each hooker is a Kotlin `object` with static inner `@XposedHooker` classes. State (prefs, package name, XposedInterface) stored in a `HookState` object with `@Volatile` fields.

**Why this pattern?**

- libxposed API 100 requires `@BeforeInvocation`/`@AfterInvocation` methods to be `@JvmStatic`
- Cannot capture outer class fields from static methods — need explicit shared state
- `@Volatile` ensures visibility across threads (hook callbacks may fire on binder threads)
- State is set once in `hook()` at module load time — not per-call

**Alternative considered:** ThreadLocal for per-thread state isolation.
**Rejected:** Overkill for our use case — we set state once per process, not per-call. ThreadLocal adds allocation overhead on every hook callback.

### Decision 4: deoptimize() on Every Spoofed Method

**Choice:** Call `xi.deoptimize(method)` after every `xi.hook(method, ...)` for all TelephonyManager, Build, Settings.Secure, and SubscriptionInfo methods.

**Why deoptimize everything?**

- ART inlines any method shorter than ~20 bytecodes
- All identity getters (getImei, getModel, etc.) are short passthrough methods
- Banking/e-commerce apps heavily JIT-compile TelephonyManager paths
- The cost is one-time at hook registration — forces recompilation, then hooks fire reliably

**Risk:** Slight app startup slowdown (~10-50ms) from forcing recompilation. Acceptable trade-off for guaranteed hook delivery.

### Decision 5: Individual try-catch per Hook Registration

**Choice:** Every single `xi.hook()` call is wrapped in its own `safeHook("methodName") { }` block.

**Why not group related hooks?**

- The current codebase has a bug where a single `runCatching` wraps multiple `.hook()` calls
- If ANY method lookup fails, ALL subsequent hooks in that block are silently skipped
- Individual wrapping ensures one missing method (e.g., `getImei(int)` on some OEMs) doesn't prevent other hooks

### Decision 6: Enriched DeviceProfilePreset with TAC Prefixes

**Choice:** Add `tacPrefixes`, `buildTime`, `securityPatch`, `buildId`, `incremental`, `supportedAbis`, `simCount`, `hasNfc`, `has5G` to `DeviceProfilePreset`.

**Why in the preset?**

- TAC (Type Allocation Code) is the first 8 digits of IMEI and identifies the device model globally
- Fraud SDKs cross-reference IMEI TAC against `Build.MODEL` — mismatches are detectable
- Build metadata (`TIME`, `SECURITY_PATCH`, etc.) must be consistent with the device model
- Having all correlated data in one preset object ensures atomic consistency

### Decision 7: oneway AIDL for Hook Reporting

**Choice:** All hook→service calls (`reportSpoofEvent`, `reportLog`, `reportPackageHooked`) declared as `oneway` in AIDL.

**Why oneway?**

- Without `oneway`, each binder call blocks the hook callback for 50-200μs
- Hooks fire hundreds of times per second in active apps
- `oneway` = fire-and-forget: ~5μs non-blocking, returns immediately
- If service is unavailable, call is silently dropped — no impact on spoofing

## Risks / Trade-offs

| Risk                                                                              | Severity | Mitigation                                                                                                                                               |
| --------------------------------------------------------------------------------- | -------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Entry point misconfiguration** — `java_init.list` wrong = module won't load     | High     | Test immediately after Phase 1. Verify APK contains `META-INF/xposed/` files with `unzip -l`. Keep backward-compat `AndroidManifest.xml` meta-data tags. |
| **Hooker rewrite regression** — one wrong pattern = hooks silently fail           | High     | Phase-by-phase testing with IMEI checker apps. Each hooker tested individually before moving on. `DualLog` reports hook registration success/failure.    |
| **RemotePreferences unavailability** — LSPosed bug = no config in hooks           | Medium   | `getRemotePreferences()` wrapped in `runCatching` with graceful skip. Module logs "RemotePreferences unavailable, skipping hooks" — doesn't crash.       |
| **App startup slowdown** from mass `deoptimize()`                                 | Low      | One-time cost per process load (~10-50ms). Acceptable for guaranteed hook delivery. Monitor via `HookMetrics`.                                           |
| **AIDL diagnostics service fails to register**                                    | Low      | Non-fatal — hooks work via RemotePreferences regardless. Diagnostics screen shows "Service unavailable" gracefully.                                      |
| **R8 strips hooker classes** — companion static methods removed                   | Medium   | Comprehensive ProGuard rules: `-keep @XposedHooker class * { *; }` + keep all `XposedInterface.Hooker` implementors. Verify with `mapping.txt` grep.     |
| **OEM-specific method signatures** — `getImei(int)` varies on Xiaomi/OPPO/Samsung | Medium   | `methodOrNull()` returns null if method doesn't exist — hook is safely skipped. Test on multiple OEM ROMs if possible.                                   |

## Migration Plan

### Execution Order (16 Phases)

The phases are designed to be **incrementally testable** — after each phase, a subset of functionality can be verified.

```
Phase 0: Dependency & Build Setup          — builds compile, no functionality change
Phase 1: Entry Point Migration             — module loads via new API, no hooks yet
Phase 2: ModuleContext & RemotePreferences  — prefs readable in hook processes
Phase 3: BaseSpoofHooker Rewrite           — new base class, no hookers converted yet
Phase 4: DeoptimizeManager                 — utility ready, not yet called
Phase 5: DeviceHooker Rewrite              — IMEI/IMSI/Serial spoofing works
Phase 6: NetworkHooker Completion          — MAC/BSSID/carrier spoofing works
Phase 7: NEW SubscriptionHooker            — SubscriptionManager gap closed
Phase 8: SystemHooker Hardening            — Build.* fields fully covered
Phase 9: WebViewHooker Completion          — UA spoofing complete
Phase 10: NEW PackageManagerHooker         — hasSystemFeature matches profile
Phase 11: AntiDetectHooker Hardening       — ClassLoader/Runtime.exec/RunningServices
Phase 12: TAC-Aware IMEI Generator         — IMEI matches device profile TAC
Phase 13: DeviceProfilePreset Enrichment   — All presets get new fields
Phase 14: Full Correlation Matrix          — NetworkTypeMapper + cross-validation
Phase 15: ProGuard & Manifest              — Release build with API 100 rules
Phase 16: RemotePreferences in :app        — XposedPrefs write side → ModulePreferences
```

### Rollback Strategy

- Git branch per phase group (Phases 0-4 = foundation, 5-11 = hookers, 12-14 = correlation, 15-16 = release)
- Each phase is a separate commit — can revert individual phases
- If entire migration fails: revert to YukiHookAPI branch, restore `assets/xposed_init`
- AIDL diagnostics (Option B) is independent — can be reverted without affecting config delivery

## Open Questions

1. **libxposed-service `ModulePreferences.from()` exact API** — The planning doc notes the exact API depends on the libxposed-service version. Need to verify the constructor/factory method by reading the actual library source or javadoc before Phase 16.
2. **KSP plugin removal** — If `:app` module has other KSP users (e.g., Room, Hilt), the KSP plugin cannot be removed. Currently only YukiHookAPI uses KSP. Verify no other KSP processors exist before removing the plugin.
3. **Backward compatibility** — Should the module support both API 82 and API 100 simultaneously (dual entry point)? The planning doc suggests no — modern API replaces legacy entirely. LSPosed 1.10.2+ supports both, but our module declares `minApiVersion=100`.
4. **Sensor list spoofing fidelity** — `SensorHooker` currently filters the sensor list. With device profiles now carrying `hasNfc`/`has5G`, should sensor lists also be profile-aware? Deferred to a future enhancement.
