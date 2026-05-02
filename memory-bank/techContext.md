# Technical Context: Device Masker

## Current Stack

| Area | Current |
| --- | --- |
| Language | Kotlin 2.3.0 |
| Android | compile/target SDK 36, min SDK 26 |
| Build | Android Gradle Plugin 9.2.0, Gradle wrapper |
| Java toolchain | JVM target 21, host may be newer |
| UI | Jetpack Compose BOM 2026.02.01 |
| Material | Material 3 1.4.0 |
| Navigation | Navigation Compose 2.9.7 |
| Lifecycle | Lifecycle 2.10.0 |
| Hooking | libxposed API 101.0.1 |
| App-side Xposed service | libxposed service/interface 101.0.0 |
| Config | libxposed RemotePreferences plus local JSON |
| IPC | AIDL diagnostics only |
| Serialization | kotlinx.serialization JSON 1.10.0 |
| Coroutines | kotlinx.coroutines 1.10.2 |
| Settings | DataStore Preferences 1.2.0 |
| Logging | Timber in app, DualLog in xposed |
| Images | Coil Compose 3.4.0 |

## Xposed Metadata

Files:
- `xposed/src/main/resources/META-INF/xposed/java_init.list`
- `xposed/src/main/resources/META-INF/xposed/module.prop`
- `xposed/src/main/resources/META-INF/xposed/scope.list`

Current metadata:
- Entry point: `com.astrixforge.devicemasker.xposed.XposedEntry`
- `minApiVersion=101`
- `targetApiVersion=101`
- `staticScope=false`
- Default scope: `android`, `system`

## Build Commands

Primary full gate:

```powershell
.\gradlew.bat spotlessApply spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug assembleRelease --no-daemon
```

Useful targeted commands:

```powershell
.\gradlew.bat :xposed:compileDebugKotlin --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :common:testDebugUnitTest --no-daemon
.\gradlew.bat :xposed:testDebugUnitTest --no-daemon
.\gradlew.bat assembleDebug --no-daemon
```

## Static Safety Greps

All should return no matches:

```powershell
Get-ChildItem -Path xposed/src -Recurse -Filter '*.kt' | Select-String '@XposedHooker|@BeforeInvocation|@AfterInvocation|AfterHookCallback'
Get-ChildItem -Path app/src,xposed/src -Recurse -Filter '*.kt' | Select-String '"module_enabled"|"app_enabled_"|"spoof_value_"|"spoof_enabled_"'
Get-ChildItem -Path common/src -Recurse -Filter '*.kt' | Select-String 'Random\(\)' | Where-Object { $_ -notmatch 'SecureRandom' }
Get-ChildItem -Path xposed/src -Recurse -Filter '*.kt' | Select-String 'Timber\.'
Get-ChildItem -Path common/src,xposed/src -Recurse -Filter '*.kt' | Select-String 'import androidx.compose'
Get-ChildItem -Path xposed/src/main/kotlin -Recurse -Filter '*.kt' | Select-String 'IMEIGenerator|IMSIGenerator|ICCIDGenerator|MACGenerator|UUIDGenerator|PhoneNumberGenerator|SerialGenerator|\{ "(us|Carrier|310260|HomeNetwork)" \}|ByteArray\(32\)|\?: 310|\?: 260'
```

## Important Project Files

| File | Role |
| --- | --- |
| `app/src/main/kotlin/com/astrixforge/devicemasker/DeviceMaskerApp.kt` | App initialization, XposedPrefs/ConfigManager/ServiceClient wiring |
| `app/src/main/kotlin/com/astrixforge/devicemasker/data/XposedPrefs.kt` | App-side libxposed service binding and RemotePreferences access |
| `app/src/main/kotlin/com/astrixforge/devicemasker/data/ConfigSync.kt` | Flattens JsonConfig into RemotePreferences |
| `app/src/main/kotlin/com/astrixforge/devicemasker/service/ConfigManager.kt` | Local JSON config, StateFlow, app/group mutation |
| `app/src/main/kotlin/com/astrixforge/devicemasker/service/AppLogStore.kt` | Rootless persistent app log store, Timber tree, export formatter |
| `app/src/main/kotlin/com/astrixforge/devicemasker/service/LogManager.kt` | Minimal log export from app log store plus diagnostics service buffer |
| `app/src/main/kotlin/com/astrixforge/devicemasker/data/repository/SpoofRepository.kt` | UI-facing config/app repository |
| `common/src/main/kotlin/com/astrixforge/devicemasker/common/SharedPrefsKeys.kt` | RemotePreferences key single source of truth |
| `common/src/main/kotlin/com/astrixforge/devicemasker/common/JsonConfig.kt` | Root config model and migration helpers |
| `common/src/main/aidl/com/astrixforge/devicemasker/IDeviceMaskerService.aidl` | Diagnostics-only Binder contract |
| `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/XposedEntry.kt` | libxposed module entry |
| `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/PrefsReader.kt` | Hook-side pref reader |
| `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/BaseSpoofHooker.kt` | Shared hook utilities |
| `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/service/DeviceMaskerService.kt` | system_server diagnostics service |

## Known Warnings

Current Gradle output includes deprecation warnings for AGP properties and Spotless `indentWithSpaces`. These do not currently fail the gate but should be cleaned before the AGP 10 upgrade path.

## Runtime Requirements

To validate spoofing, a test device/emulator must have:
- Root.
- LSPosed with libxposed API 101 support.
- Device Masker enabled as an LSPosed module.
- Required scope enabled.
- Target app force-stopped/restarted after scope or module changes.

App launch alone does not prove target-process hook behavior.
