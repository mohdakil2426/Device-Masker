## ADDED Requirements

### Requirement: Live config via RemotePreferences

Hookers SHALL read configuration using `xposedInterface.getRemotePreferences("device_masker_config")` which returns a live `SharedPreferences` object that reflects the latest values written by the module app without requiring target app restart.

#### Scenario: Config change applies without restart

- **WHEN** user changes a spoof value in the UI and saves
- **THEN** the next hook invocation in the target app reads the updated value via RemotePreferences without the target app being restarted

#### Scenario: RemotePreferences unavailable

- **WHEN** `getRemotePreferences()` throws an exception (e.g., LSPosed not ready)
- **THEN** the hook logs "RemotePreferences unavailable for {pkg}, skipping hooks" and returns without registering any hooks for that package (no crash)

### Requirement: App-side ModulePreferences write

The `:app` module SHALL write config via `ModulePreferences.from(context, "device_masker_config")` instead of `Context.MODE_WORLD_READABLE` SharedPreferences.

#### Scenario: XposedPrefs.getPrefs returns ModulePreferences

- **WHEN** `XposedPrefs.getPrefs(context)` is called from ConfigSync
- **THEN** it returns a `SharedPreferences` backed by `ModulePreferences.from(context, "device_masker_config")`

#### Scenario: ConfigSync unchanged

- **WHEN** ConfigSync writes keys using `XposedPrefs.getPrefs(context).edit()`
- **THEN** the same `SharedPrefsKeys` key formats are used — no key format changes

### Requirement: Master switch check via RemotePreferences

The module SHALL check `prefs.getBoolean("module_enabled", true)` from RemotePreferences before registering hooks. If false, no hooks are registered for any package.

#### Scenario: Module disabled

- **WHEN** user disables the module master switch in the UI
- **THEN** `onPackageLoaded` reads `module_enabled=false` from RemotePreferences and returns without registering hooks

### Requirement: Per-app enable check via RemotePreferences

The module SHALL check `prefs.getBoolean("app_enabled_{pkg}", false)` before registering hooks for a specific package.

#### Scenario: App not enabled for spoofing

- **WHEN** `onPackageLoaded` fires for `com.example.bank` which has not been enabled
- **THEN** `app_enabled_com.example.bank` is false (default) and no hooks are registered

### Requirement: AIDL config methods removed

The AIDL interface SHALL NOT contain `writeConfig`, `readConfig`, `reloadConfig`, `isModuleEnabled`, `isAppEnabled`, or `getSpoofValue` methods. Config delivery is exclusively via RemotePreferences.

#### Scenario: No config binder calls from hooks

- **WHEN** a hook callback fires in a target app
- **THEN** it reads spoof values from RemotePreferences only — zero binder calls to system_server for config

### Requirement: ConfigManager removes AIDL sync

`ConfigManager.saveConfigInternal()` in `:app` SHALL NOT call `syncToAidlService()`. Config persistence uses local JSON file + `ConfigSync` → `ModulePreferences` only.

#### Scenario: Config save path

- **WHEN** user saves a config change in the UI
- **THEN** ConfigManager writes `config.json` locally and syncs to ModulePreferences — no AIDL service call for config
