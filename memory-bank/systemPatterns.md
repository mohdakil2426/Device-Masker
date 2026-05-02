# System Patterns: Device Masker

## Module Layout

```text
:app
  Compose UI, ViewModels, repositories, config persistence, RemotePreferences writer,
  diagnostics client.

:common
  Shared contracts and models: SpoofType, JsonConfig, SpoofGroup, AppConfig,
  SharedPrefsKeys, generators, AIDL.

:xposed
  libxposed API 101 module entry, hookers, RemotePreferences reader, diagnostics
  service, anti-detection, DualLog.
```

## Current Architecture

```text
User/UI action
  -> ViewModel
  -> SpoofRepository
  -> ConfigManager
  -> filesDir/config.json
  -> ConfigSync
  -> XposedPrefs / libxposed RemotePreferences
  -> target process hookers read stored per-app keys
```

Diagnostics is separate:

```text
App Timber logs
  -> PersistentAppLogTree
  -> AppLogStore in filesDir/logs/structured.log

Hooker
  -> DualLog / reportSpoofEvent / reportPackageHooked
  -> DeviceMaskerService in system_server
  -> ServiceClient
  -> Diagnostics UI / LogManager export
```

Config must not be delivered through AIDL. AIDL is diagnostics-only.

## Source Of Truth Rules

- `JsonConfig.appConfigs` is the canonical app-scope table.
- `SpoofGroup.assignedApps` is legacy/display compatibility and must not override appConfigs.
- If a loaded development config has empty `appConfigs`, derive them once from legacy assigned apps.
- Deleting a group must remove app configs assigned to that group.
- `SharedPrefsKeys` in `:common` is the only source for RemotePreferences key generation.

## Config Sync Rules

`ConfigSync` flattens `JsonConfig` into per-app RemotePreferences keys:
- Global module enabled key.
- Enabled app package set.
- Per-app enabled key.
- Per-app, per-type enabled key.
- Per-app, per-type stored value key.
- Config version timestamp.

Full sync must clear stale package keys for apps removed since the previous sync.

Sync is triggered:
- After local config load.
- After config save/update.
- When libxposed service binds.

## Hook Loading Rules

`XposedEntry`:
- Registers system_server hooks from the system-server lifecycle.
- Registers app hooks from the package-ready lifecycle using the target classloader.
- Skips duplicate process package loads.
- Skips critical/system packages where app-process hooks are unsafe.
- Loads `AntiDetectHooker` before spoof hookers.

Default scope resources:
- `android`
- `system`

## Hook Safety Rules

Every hook registration should:
- Resolve classes/methods defensively.
- Use one `safeHook` block per target method or method family.
- Use `xi.hook(m).intercept { ... }`.
- Call `xi.deoptimize(m)`.
- Return original framework results when configuration is not valid.

Runtime hook callbacks must not:
- Generate fresh random identifiers.
- Return hardcoded fallback identifiers.
- Return fake values for malformed config.
- Mutate framework-returned lists in place.
- Crash target apps.

Intentional throws used for anti-detection package/class hiding must use `ExceptionMode.PASSTHROUGH`.

## Current Hook Areas

Hookers in `:xposed`:
- `AntiDetectHooker`
- `DeviceHooker`
- `NetworkHooker`
- `AdvertisingHooker`
- `SystemHooker`
- `LocationHooker`
- `SensorHooker`
- `WebViewHooker`
- `SubscriptionHooker`
- `PackageManagerHooker`
- `SystemServiceHooker`

`PackageManagerHooker` and anti-detection package filtering must cover both legacy `int` flags and modern API 33+ flag object overloads when possible.

## Value Correlation

Some values must be generated as a coherent set:
- SIM/card carrier values: IMSI, ICCID, phone number, carrier name, MCC/MNC, SIM/network country.
- Location values: timezone, locale, latitude, longitude.
- Hardware profile values: device profile, Build fields, serial/IMEI where applicable.

Generators live in `:common`. Hookers read stored values; they do not generate new identities.

## UI Patterns

- Compose + MVVM.
- `StateFlow` exposed from ViewModels.
- `collectAsStateWithLifecycle()` in composables.
- Navigation routes are string constants in `NavRoutes`, not sealed object routes.
- Home and Diagnostics observe libxposed service connection through `XposedPrefs.isServiceConnected`.

## Logging Patterns

- App logs are stored without root through `PersistentAppLogTree` and `AppLogStore`.
- App log storage lives in the app sandbox at `filesDir/logs/structured.log`.
- Export uses `LogManager` to combine app-owned persistent logs with the current diagnostics service buffer.
- The app must not depend on reading global logcat; `READ_LOGS` is privileged/not suitable for this app.
- Export files should stay minimal and structured: metadata, app entries, and xposed diagnostics entries.
- Xposed diagnostics logs remain in-memory in `DeviceMaskerService`; they are best-effort and reset when the service/process restarts.

## Build And Documentation Rules

- Keep Memory Bank current after architecture changes.
- Keep docs free of obsolete legacy hook-framework or preference-delivery wording.
- Do not format or edit `.agents` skill assets as part of project formatting.
- Lint must stay fail-fast.
