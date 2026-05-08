# :app Module Guide

Compose UI, ViewModels, config persistence, RemotePreferences writer, diagnostics, and root evidence collection. This is the user-facing Android application.

## Module Structure

```
app/src/main/kotlin/com/astrixforge/devicemasker/
├── data/                     RemotePreferences writer, config sync, settings, repositories
│   ├── models/               InstalledApp, TypeAliases
│   └── repository/           SpoofRepository, AppScopeRepository + interfaces
│
├── service/                  Config persistence, logging, support bundles
│   └── diagnostics/          Root access, root capture, support bundles, JSONL store
│
├── ui/                       All Compose UI
│   ├── navigation/           NavDestination, navigation state, deep links, bottom bar
│   ├── screens/
│   │   ├── home/             Dashboard: module toggle, stats, group selector
│   │   ├── groups/           Group list, CRUD, import/export
│   │   ├── groupspoofing/    Spoof config + app assignment
│   │   │   ├── categories/   SIM, DeviceHardware, Location category cards
│   │   │   ├── items/        Independent/Correlated spoof items
│   │   │   ├── tabs/         Spoof tab + Apps tab
│   │   │   └── model/        UIDisplayCategory
│   │   ├── settings/         Theme, root status, export, diagnostics link
│   │   └── diagnostics/      Module status, anti-detection tests, hook logs
│   ├── theme/                Color, Theme, Typography, Shapes, Motion (M3E)
│   └── components/           Reusable UI components
│       ├── expressive/       M3E expressive components (cards, switches, indicators)
│       └── dialog/           Timezone, country pickers, standard dialogs
│
└── utils/                    Image utilities
```

## Manual DI — No Hilt

All wiring is manual. `DeviceMaskerApp.onCreate()` creates singletons. `MainActivity` creates repositories via `remember`. Navigation entries pass dependencies to screen routes, and screens use explicit `viewModelFactory` helpers from `DeviceMaskerViewModelFactories.kt` as default ViewModel parameters. Service/repository contracts are split into narrow workflow interfaces with compatibility facades (`IConfigManager`, `ISpoofRepository`) for existing callers and test fakes.

**Singletons (objects):** `ConfigManager`, `XposedPrefs`, `ConfigSync`, `LogManager`, `RootAccessManager`, `RootCaptureStore`

**Instance singletons (in DeviceMaskerApp companion):** `appLogStore`

## Key Files

| File | Role |
|------|------|
| `DeviceMaskerApp.kt` | Application entry — plants Timber trees, inits ConfigManager/XposedPrefs/RootAccessManager, registers config sync on LSPosed bind |
| `data/XposedPrefs.kt` | Writes to RemotePreferences via `XposedService.getRemotePreferences()`. All keys delegate to `SharedPrefsKeys` in `:common`. Uses `commit()` not `apply()`. |
| `data/ConfigSync.kt` | Flattens `JsonConfig` → flat per-app SharedPreferences keys. `syncFromConfig()` for full sync, `syncApp()` for single app. Clears stale keys. |
| `data/ConfigSyncHelpers.kt` | App sync state and SharedPreferences editor helpers used by `ConfigSync`. |
| `service/ConfigManager.kt` | JSON config CRUD. Backs `AtomicFile` at `filesDir/config.json`. Exposes `config: StateFlow<JsonConfig>`. Uses `Mutex` for thread-safe saves. |
| `data/repository/SpoofRepository.kt` | Main repo for ViewModels. Correlation-aware value generation with `AtomicReference` caches for SIM/Location/DeviceHardware configs. Singleton via `getInstance()`. |
| `ui/navigation/DeviceMaskerViewModelFactories.kt` | Manual ViewModel factories used by screen default parameters. |

## Screen → ViewModel → Repository Mapping

| Screen | ViewModel | Key Repository Methods |
|--------|-----------|----------------------|
| Home | `HomeViewModel(ISpoofRepository, isXposedActiveFlow)` | `setModuleEnabled()`, `setActiveGroup()`, `regenerateAllValues()` |
| Groups | `GroupsViewModel(ISpoofRepository)` | `createGroup()`, `deleteGroup()`, `setDefaultGroup()`, `exportGroups()`, `importGroups()` |
| GroupSpoofing | `GroupSpoofingViewModel(ISpoofRepository, groupId)` | `generateValue()`, `updateGroupWithCarrier()`, `updateGroupWithDeviceProfile()`, `addAppToGroup()`, `removeAppFromGroup()` |
| Settings | `SettingsViewModel(Application, ISettingsDataStore, ILogManager, ioDispatcher)` | `setThemeMode()`, `exportLogsToUri()`, `createShareableLogs()` |
| Diagnostics | `DiagnosticsViewModel(Application, ISpoofRepository)` | `refresh()`, `runDiagnosticTests()`, `runAntiDetectionTests()` |

## Navigation (Navigation 3)

- Framework: `navigation3-runtime/ui` 1.1.1, NOT Navigation Compose 2.x
- Destinations: `NavDestination` sealed interface with `@Serializable` `NavKey` types
- State: `DeviceMaskerNavigationState` — per-tab back stacks (Home, Groups, Settings)
- `DeviceMaskerNavigator` — imperative API for screens (`navigateToGroup()`, `navigateToDiagnostics()`, `goBack()`)
- Focus screens (`GroupSpoofing`, `Diagnostics`) hide bottom bar/nav rail
- Adaptive: `NavigationRail` for medium/expanded, bottom bar for compact
- Deep links: `devicemasker://open/{path}` — supports `home`, `groups`, `groups/{id}`, `settings`, `diagnostics`
- Scene strategies: `ListDetailSceneStrategy` for Groups→GroupSpoofing on wider screens

## Theme (Material 3 Expressive)

- `DeviceMaskerTheme` composable: supports SYSTEM/LIGHT/DARK, AMOLED black, dynamic colors
- Uses `MaterialExpressiveTheme` with `MotionScheme.expressive()`
- Named colors in `Color.kt` — no hardcoded `Color(0x...)` outside that file
- `LocalEmphasizedTypography` for bolder expressive weights
- `AppMotion` object: spring-based specs (Expressive, Standard, Snappy, ReducedMotion)
- `ElevationTokens` Level0–Level5

## Diagnostics & Root

- `AppLogStore`: JSONL events in `filesDir/logs/sessions/`
- `LogManager`: single `Export Logs` ZIP path that always builds the maximum available support bundle
- `StrictModeGuard`: debug-only app-process StrictMode policy. Never install StrictMode from `:xposed`.
- `RootAccessManager`: libsu root grant state, startup request
- `RootLogCaptureService`: foreground service for bounded root capture
- `BootCaptureReceiver`: `BOOT_COMPLETED` → starts root capture

## Testing

- `MainDispatcherRule` — swaps `Dispatchers.Main` with `UnconfinedTestDispatcher`
- Hand-written fakes for interfaces: `FakeSpoofRepository`, `FakeConfigManager`, `FakeSettingsDataStore`, `FakeLogManager`, `FakeAppScopeRepository`, `FakeSharedPreferences`
- Turbine for Flow emissions
- MockK only for Navigation 3 framework types
- `advanceUntilIdle()` required after async ops in `runTest`

## Build

- `compileSdk 37`, `targetSdk 36`, `minSdk 26`, JVM 17
- Release minification/resource shrinking is enabled. Runtime hook callbacks must stay R8-safe through the `:xposed` StableHooker/named `XposedInterface.Hooker` pattern.
- `aidl = false`, `buildConfig = true`, `compose = true`
- `useLegacyPackaging = true` for primary dex Xposed class loading
- Compose compiler reports/metrics are opt-in with `enableComposeCompilerReports` and `enableComposeCompilerMetrics`
- Signing from env vars: `KEYSTORE_PATH`, `KEYSTORE_PASS`, `KEY_ALIAS`, `KEY_PASS`

## Manifest

- Permissions: `QUERY_ALL_PACKAGES`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`
- `XposedProvider` at `${applicationId}.XposedService` — required for RemotePreferences bridge
- `MainActivity`: `singleTop`, `adjustResize`, deep link filter `devicemasker://open`
- `allowBackup = false`
