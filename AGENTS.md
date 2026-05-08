# Device Masker — Agent Guide

Android LSPosed/libxposed module for per-app device identity spoofing. 3-module Gradle project: `:app` (Compose UI + config), `:common` (shared contracts + generators), `:xposed` (hooks running inside target app processes).

## Permanent Rules

- Config delivery is RemotePreferences-first. Do not add custom AIDL/Binder config or hook-evidence paths.
- `SharedPrefsKeys.kt` is the only source for RemotePreferences key names.
- `JsonConfig.appConfigs` is canonical for app scope; `SpoofGroup.assignedApps` is legacy/display-only.
- Generated identity values are created in app/common config flows, never inside target-process hooks.
- `:xposed` must return original values for disabled, missing, blank, malformed, unsafe, or unsupported config.
- `:xposed` must not use Timber, Compose, random generation, app-private JSON reads, or hardcoded preference keys.
- Runtime hooks must not use direct Kotlin SAM `.intercept { ... }` callbacks unless release R8/runtime validation explicitly proves it safe.
- App launch and app-side Xposed service connection do not prove hooks work.
- Hook success requires LSPosed/logcat evidence and, where possible, actual spoofed values inside target apps.
- Target app safety is more important than broad spoof coverage.

## Module Boundaries

| Module | What lives here | Key constraint |
|--------|----------------|----------------|
| `:app` | Compose UI, ViewModels, ConfigManager, ConfigSync, XposedPrefs, diagnostics, root capture | No hook logic. Timber OK here. |
| `:common` | JsonConfig, SpoofType, SharedPrefsKeys, generators, DevicePersona | No Android UI deps. Generators run at config time only. |
| `:xposed` | XposedEntry, all hookers, PrefsHelper, DualLog | **No Timber. No Compose. No random generation.** `libxposed:api` is `compileOnly`. |

## Architecture Reference

For full architecture, config flow, diagnostics flow, and module interaction details, read `docs/public/ARCHITECTURE.md`.

Root `AGENTS.md` only keeps permanent guardrails. Do not duplicate detailed architecture here; update the architecture doc and Memory Bank when architecture changes.

## Project Structure

```
devicemasker/
├── app/                  :app module — user-facing app, config, diagnostics, root capture
│   └── src/main/kotlin/com/astrixforge/devicemasker/
│       ├── data/             App data access, repositories, RemotePreferences bridge
│       ├── service/          App services, config persistence, logs, diagnostics
│       ├── ui/               Compose UI, navigation, theme, reusable components
│       └── utils/            App utility helpers
│
├── common/               :common module — shared contracts, models, generators
│   └── src/main/
│       ├── kotlin/.../common/
│       │   ├── models/       Shared data models and value objects
│       │   ├── generators/   Config-time identity generators
│       │   ├── diagnostics/  Shared diagnostics schema and redaction
│       │   └── (root)        Shared config contracts, key builders, constants
│
├── xposed/               :xposed module — hooks in target app processes
│   └── src/main/
│       ├── kotlin/.../xposed/
│       │   ├── hooker/       Target-process hook implementations and shared hook helpers
│       │   ├── diagnostics/  Hook-side diagnostics and health tracking
│       │   └── (root)        Module entry, preference reader, logging, deoptimization support
│       └── resources/META-INF/xposed/   libxposed module metadata
│
├── gradle/               Gradle version catalog and build dependency metadata
├── docs/                 Public docs, internal audits/reports, implementation plans
├── logs/                 Build logs, device evidence, temporary agent/user artifacts, temp build logs file
├── build.gradle.kts      Root Gradle conventions
├── settings.gradle.kts   Gradle module/repository settings
├── lint.xml              Android lint configuration
└── gradle.properties     Gradle/Android build properties
```

### File & Boundaries

| Type | Correct Location | Wrong |
| --- | --- | --- |
| User-facing docs | `docs/public/` | `docs/internal/` |
| Active internal reports/audits | `docs/internal/reports/active/` | project root or `docs/internal/reports/` root |
| Closed internal reports/audits | `docs/internal/reports/closed/` | project root or `docs/internal/reports/` root |
| Implementation plans | `docs/superpowers/plans/` | reports folders or project root |
| Build logs and command output | `logs/build/` | project root, docs, module dirs |
| Device testing logs, logcat, screenshots, captures, exported evidence | `logs/device/` | project root or docs |
| Agent/user temporary check artifacts | `logs/tmp/` | project root or source folders |

All agent-created and user-created build logs, device logs, temporary captures, smoke-test exports, and scratch evidence must stay under `logs/` using the closest matching subfolder. Do not scatter temporary evidence files in the project root.

Report lifecycle:
- Put reports with pending decisions, open remediation, or active analysis in `docs/internal/reports/active/`.
- Move reports to `docs/internal/reports/closed/` only after the decision is recorded or the remediation is complete.
- Do not leave report files directly under `docs/internal/reports/`.

## Commands And Rules

Use Windows Gradle wrapper commands from repo root.

Before writing or changing code, read `docs/public/AGENTS_CODING_RULES.md` and apply those rules.

```powershell
.\gradlew.bat spotlessApply spotlessCheck detekt --no-daemon
.\gradlew.bat :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest --no-daemon
.\gradlew.bat spotlessCheck detekt lint test assembleDebug --no-daemon
.\gradlew.bat spotlessCheck detekt :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint assembleRelease :app:assembleCiRelease --no-daemon
.\gradlew.bat detektBaseline --no-daemon
```

Rules:
- Format with Spotless; do not hand-format unrelated code.
- Run Spotless and Detekt together after Kotlin/Compose changes.
- Run module tests for touched modules.
- Run the release/R8 gate before release or hook-safety claims.
- Run `detektBaseline` separately from `detekt`; update baselines only for accepted, documented existing debt.
- Do not claim hook success without LSPosed/logcat evidence and target-app value checks where possible.
- On build failures, find the root cause first; use relevant skills, official docs, Google-developer-knowledge MCP for Android/Google APIs, and web search when current docs are needed.

## Xposed Safety

**Every hook must:**
- Return original value when config is disabled, missing, blank, malformed, or unsafe
- Register callbacks with `intercept(stableHooker { ... })` or explicit named `XposedInterface.Hooker` implementations
- Call `xi.deoptimize(m)` after hooking (prevents ART/JIT inlining from bypassing hook)
- Use `safeHook()` isolation — one failed method never blocks other hooks
- Skip abstract methods before calling `xi.hook(m)`

**Never do in `:xposed`:**
- Generate random/fallback identifiers
- Use Timber (use `DualLog` instead)
- Read app-private JSON files
- Hardcode RemotePreferences key strings
- Mutate `chain.args` in place — it's immutable. Copy and call `chain.proceed(Object[])`
- Use direct Kotlin SAM intercept callbacks; release R8 caused `AbstractMethodError` from that callback shape
- Re-enable global `Class.forName` / `ClassLoader.loadClass` hooks without per-app kill switch

**libxposed API 101 specifics:**
- Lifecycle: `onModuleLoaded` → `onPackageReady` (app hooks) / `onSystemServerStarting` (system_server setup)
- `chain.thisObject` can be `null` for static methods
- `HookFailedError` extends `XposedFrameworkError` extends `Error` — rethrow before generic `Throwable` catches
- Legacy XposedBridge patterns compile but fail silently at runtime — always load the libxposed skill first (`.agents/skills/libxposed/SKILL.md`)

## Runtime Validation

Before claiming hooks work:
- Run the relevant unit/static tests and build/install the APK variant being validated.
- For release/R8 claims, validate the minified release APK with R8 enabled.
- LSPosed scope must include `android`, `system`, and the target app.
- Force-stop and relaunch target apps after any scope/module/config change.
- Check LSPosed/logcat for module load, target selection, hook registration, and spoof events.
- Verify actual spoofed values inside target apps when possible, not only app launch or service connection.
- After hook safety or R8 callback changes, run the R8 hook ABI guard and validate more than one target app before stability claims.

## Module Guides

Detailed per-module guides with folder structures, APIs, and constraints:

- `app/AGENTS.md` — Compose UI, ViewModels, navigation, diagnostics, build config
- `common/AGENTS.md` — data models, generators, SharedPrefsKeys, config contracts
- `xposed/AGENTS.md` — hookers, hook patterns, anti-detection, ProGuard, metadata

## Skills

Critical rule: Load the `libxposed` skill before any Xposed work: `.agents/skills/libxposed/SKILL.md` becouse all you need about lsposed/xposed/libxposed are out-dated this skill have the latest official documentations and raw github cloned repo and javadoc, full imformation so befor any xposed work load/read the skill first its critical.

Other available skills: `claude-android-ninja`, `edge-to-edge`, `material-3-expressive`, `navigation-3`, `r8-analyzer`.

## MCP Usage

- Use `Google-developer-knowledge` MCP for Android, Google, Material 3 Expressive, Firebase, Play, Web, or Google Cloud and all the google documentations. this is best for that things, dont use context7 for these.
- Use `mobile_mcp` for emulator/device to controll the emulator and manual app tests.
- Use `context7` for current library/framework/API documentation before changing code that depends on external APIs.

## graphify

This project has a graphify knowledge graph at graphify-out/.

Rules:

- Before answering architecture or codebase questions, read graphify-out/GRAPH_REPORT.md for god nodes and community structure
- If graphify-out/wiki/index.md exists, navigate it instead of reading raw files
- For cross-module "how does X relate to Y" questions, prefer `graphify query "<question>"`, `graphify path "<A>" "<B>"`, or `graphify explain "<concept>"` over grep — these traverse the graph's EXTRACTED + INFERRED edges instead of scanning files
- After modifying code files in this session, run `graphify update .` to keep the graph current (AST-only, no API cost).
