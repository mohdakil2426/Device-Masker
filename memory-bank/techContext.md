# Technical Context: Device Masker

## Stack

| Area | Current |
| --- | --- |
| Language | Kotlin 2.3.0 |
| Android | compile SDK 37, target SDK 36, min SDK 26 |
| Build | Android Gradle Plugin 9.2.0 |
| Java/Kotlin toolchain | JVM 17 for Android modules |
| UI | Jetpack Compose BOM 2026.04.01 |
| Material | Material 3 1.5.0-alpha18 |
| Navigation | Navigation 3 runtime/ui 1.1.1, lifecycle-viewmodel-navigation3 2.10.0 |
| Adaptive navigation | material3-adaptive-navigation3 1.3.0-alpha10 |
| Lifecycle | Lifecycle 2.10.0 |
| Hooking | libxposed API 101.0.1 |
| App-side Xposed service | libxposed service/interface 101.0.0 |
| Config bridge | libxposed RemotePreferences |
| Local config | JSON in app `filesDir` |
| IPC | AIDL diagnostics only |
| Serialization | kotlinx.serialization JSON 1.10.0 |
| Coroutines | kotlinx.coroutines 1.10.2 |
| Logging | Timber structured JSONL in `:app`, DualLog/XposedModule structured sink in `:xposed` |
| Root collection | libsu core 6.0.0 for startup root grant, boot/startup capture, and Root Maximum support bundles |
| Image loading | Coil Compose 3.4.0 |
| Static analysis | Detekt 2.0.0-alpha.3 with Compose rules 0.5.8 |

## Modules

| Module | Role |
| --- | --- |
| `:app` | UI, app state, local config, RemotePreferences writes, rootless logs, diagnostics views |
| `:common` | Shared models, generators, key builders, AIDL contract |
| `:xposed` | libxposed entry point, target-process hooks, diagnostics service, anti-detection |

## Xposed Metadata

Files:
- `xposed/src/main/resources/META-INF/xposed/java_init.list`
- `xposed/src/main/resources/META-INF/xposed/module.prop`
- `xposed/src/main/resources/META-INF/xposed/scope.list`

Current expectations:
- Entry point: `com.astrixforge.devicemasker.xposed.XposedEntry`
- `minApiVersion=101`
- `targetApiVersion=101`
- `staticScope=false`
- Default scope includes `android` and `system`

## Important Files

| File | Role |
| --- | --- |
| `app/src/main/kotlin/com/astrixforge/devicemasker/DeviceMaskerApp.kt` | App initialization and wiring |
| `app/src/main/kotlin/com/astrixforge/devicemasker/data/XposedPrefs.kt` | App-side libxposed service binding and RemotePreferences access |
| `app/src/main/kotlin/com/astrixforge/devicemasker/data/ConfigSync.kt` | Flattens config into RemotePreferences |
| `app/src/main/kotlin/com/astrixforge/devicemasker/service/ConfigManager.kt` | JSON config persistence and state |
| `app/src/main/kotlin/com/astrixforge/devicemasker/service/AppLogStore.kt` | Rootless app log storage |
| `app/src/main/kotlin/com/astrixforge/devicemasker/service/LogManager.kt` | Support bundle export bridge |
| `app/src/main/kotlin/com/astrixforge/devicemasker/service/diagnostics/JsonlDiagnosticStore.kt` | Rotating diagnostic JSONL store |
| `app/src/main/kotlin/com/astrixforge/devicemasker/service/diagnostics/SupportBundleBuilder.kt` | Local support bundle ZIP builder |
| `app/src/main/kotlin/com/astrixforge/devicemasker/service/diagnostics/RootLogCollector.kt` | Opt-in root maximum artifact collector and command manifest writer |
| `app/src/main/kotlin/com/astrixforge/devicemasker/service/diagnostics/RootAccessManager.kt` | Central root grant state and startup root request |
| `app/src/main/kotlin/com/astrixforge/devicemasker/service/diagnostics/RootLogCaptureService.kt` | Foreground service for bounded root startup/boot capture |
| `app/src/main/kotlin/com/astrixforge/devicemasker/service/diagnostics/BootCaptureReceiver.kt` | Starts root capture after `BOOT_COMPLETED` when Android allows it |
| `app/src/main/kotlin/com/astrixforge/devicemasker/data/repository/SpoofRepository.kt` | UI-facing config repository |
| `app/src/main/kotlin/com/astrixforge/devicemasker/ui/navigation/NavDestination.kt` | Navigation 3 `NavKey` destination model |
| `app/src/main/kotlin/com/astrixforge/devicemasker/ui/navigation/DeviceMaskerNavigationState.kt` | App-owned Navigation 3 top-level stacks and navigator |
| `app/src/main/kotlin/com/astrixforge/devicemasker/ui/navigation/DeviceMaskerDeepLinks.kt` | Navigation 3 deep-link URI parsing and synthetic stack definitions |
| `common/src/main/kotlin/com/astrixforge/devicemasker/common/JsonConfig.kt` | Root config model and migration helpers |
| `common/src/main/kotlin/com/astrixforge/devicemasker/common/SharedPrefsKeys.kt` | Preference key single source of truth |
| `common/src/main/aidl/com/astrixforge/devicemasker/IDeviceMaskerService.aidl` | Diagnostics-only Binder contract |
| `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/XposedEntry.kt` | libxposed module entry |
| `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/PrefsReader.kt` | Hook-side preference reader |
| `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/BaseSpoofHooker.kt` | Shared hook utilities |
| `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/callback/StableHooker.kt` | R8-safe libxposed `XposedInterface.Hooker` adapter for runtime hook callbacks |
| `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/AntiDetectHooker.kt` | Safer anti-detection hooks |
| `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker/WebViewHooker.kt` | Defensive WebView UA hook |
| `xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/service/DeviceMaskerService.kt` | Best-effort diagnostics service |
| `app/src/test/kotlin/com/astrixforge/devicemasker/MainDispatcherRule.kt` | Test coroutine dispatcher rule |
| `app/src/test/kotlin/com/astrixforge/devicemasker/testing/*.kt` | Fake implementations for testing |
| `app/src/test/kotlin/com/astrixforge/devicemasker/ui/screens/*/*ViewModelTest.kt` | ViewModel unit tests |
| `docs/reports/IMPLEMENTATION_COMPLETION_SUMMARY_2026-05-04.md` | Plan completion summary |

## Build Commands

Primary gate:

```powershell
.\gradlew.bat spotlessApply spotlessCheck detekt :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug assembleRelease --no-daemon
```

Targeted gates:

```powershell
.\gradlew.bat :xposed:testDebugUnitTest --no-daemon
.\gradlew.bat :xposed:testDebugUnitTest --tests com.astrixforge.devicemasker.xposed.hooker.R8HookerAbiTest --no-daemon
.\gradlew.bat :app:testDebugUnitTest --no-daemon
.\gradlew.bat :common:testDebugUnitTest --no-daemon
.\gradlew.bat detekt --no-daemon
.\gradlew.bat assembleDebug --no-daemon
```

Navigation 3 targeted gate:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.astrixforge.devicemasker.ui.navigation.DeviceMaskerNavigatorTest --no-daemon
.\gradlew.bat spotlessCheck :app:testDebugUnitTest lint assembleDebug --no-daemon
adb shell am start -W -a android.intent.action.VIEW -d "devicemasker://open/diagnostics" com.astrixforge.devicemasker
```

16 KB page-size verification:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\verify-16kb-page-support.ps1 app\build\outputs\apk\debug\app-debug.apk
```

Install debug APK:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Target smoke launch:

```powershell
adb shell am force-stop com.mantle.verify
adb logcat -c
adb shell monkey -p com.mantle.verify -c android.intent.category.LAUNCHER 1
adb shell pidof com.mantle.verify
adb logcat -d -t 1200
```

## Static Safety Greps

These should return no matches unless intentionally updated:

```powershell
Get-ChildItem -Path xposed/src -Recurse -Filter '*.kt' | Select-String '@XposedHooker|@BeforeInvocation|@AfterInvocation|AfterHookCallback'
Get-ChildItem -Path app/src,xposed/src -Recurse -Filter '*.kt' | Select-String '"module_enabled"|"app_enabled_"|"spoof_value_"|"spoof_enabled_"'
Get-ChildItem -Path common/src -Recurse -Filter '*.kt' | Select-String 'Random\(\)' | Where-Object { $_ -notmatch 'SecureRandom' }
Get-ChildItem -Path xposed/src -Recurse -Filter '*.kt' | Select-String 'Timber\.'
Get-ChildItem -Path common/src,xposed/src -Recurse -Filter '*.kt' | Select-String 'import androidx.compose'
Get-ChildItem -Path xposed/src/main/kotlin -Recurse -Filter '*.kt' | Select-String 'IMEIGenerator|IMSIGenerator|ICCIDGenerator|MACGenerator|UUIDGenerator|PhoneNumberGenerator|SerialGenerator|\{ "(us|Carrier|310260|HomeNetwork)" \}|ByteArray\(32\)|\?: 310|\?: 260'
Get-ChildItem -Path xposed/src/main/kotlin -Recurse -Filter '*.kt' | Select-String '\(\?<=|UA_DEVICE_REGEX|hookClassForName\(cl, xi\)|hookClassLoaderLoadClass\(cl, xi\)'
Get-ChildItem -Path xposed/src/main/kotlin/com/astrixforge/devicemasker/xposed/hooker -Filter '*.kt' | Where-Object { $_.Name -ne 'BaseSpoofHooker.kt' } | Select-String '\.intercept\s*\{'
```

## Runtime Requirements

Runtime validation needs:
- Rooted device or emulator.
- LSPosed with libxposed API 101 support.
- Device Masker enabled as an LSPosed module.
- Scope includes `android`, `system`, and the selected target app.
- Target app force-stopped after scope or module changes.

## Known Technical Warnings

- `material3-adaptive-navigation3` is on `1.3.0-alpha10`, which required moving the project to compile SDK 37.
- Release shrinking is enabled and was validated on emulator targets plus user-reported real
  Android 16 hardware. Do not bypass `StableHooker` for libxposed runtime callbacks.
- In-app diagnostics Binder can be unavailable under SELinux; LSPosed logs remain the practical runtime source.
