# Technical Context: Device Masker

## Technology Stack

| Layer        | Library                | Version                 | Purpose                                |
| ------------ | ---------------------- | ----------------------- | -------------------------------------- |
| **Language** | Kotlin                 | 2.3.0                   | Primary language                       |
| **Platform** | Android SDK            | API 36 / minSdk 26      | Android 8.0–16                         |
| **Java**     | JVM                    | 21 (target) / 25 (host) | Foojay toolchain provisioning          |
| **Build**    | AGP + Gradle           | 9.1.0 / 9.3.1           | Multi-module build                     |
| **UI**       | Compose BOM            | 2026.02.01              | Jetpack Compose (Feb 2026)             |
| **UI**       | Material 3             | 1.4.0 (Stable)          | M3 Expressive components               |
| **UI**       | Navigation Compose     | 2.9.7                   | NavHost + animated transitions         |
| **UI**       | Lifecycle              | 2.10.0                  | `collectAsStateWithLifecycle`          |
| **Hooking**  | YukiHookAPI            | 1.3.1                   | Kotlin-first Hook API                  |
| **Hooking**  | KavaRef                | 1.0.2                   | Reflection engine (Yuki 1.3+ dep)      |
| **Hooking**  | AndroidHiddenApiBypass | 6.1                     | Hidden API access                      |
| **Hooking**  | YukiHookAPI KSP        | 2.3.6                   | Annotation processor (`:app` only)     |
| **Hooking**  | LSPosed                | API 82                  | Framework (external, user installs)    |
| **IPC**      | AIDL Binder            | —                       | system_server ↔ app real-time config   |
| **Data**     | kotlinx.serialization  | 1.10.0                  | JSON config persistence                |
| **Data**     | DataStore              | 1.2.0                   | UI-only settings (theme, AMOLED)       |
| **Data**     | Kotlinx Coroutines     | 1.10.2                  | Async + StateFlow                      |
| **Logging**  | Timber                 | 5.0.1                   | App-side logging (stripped in release) |
| **Image**    | Coil Compose           | 3.4.0                   | Installed app icons in UI              |

---

## Build Configuration

### Quality Gate — Run in Order

```bash
./gradlew spotlessApply      # 1. Auto-format (ktfmt 0.54)
./gradlew spotlessCheck      # 2. Verify formatting
./gradlew lint               # 3. Android Lint
./gradlew test               # 4. Unit tests
./gradlew assembleDebug      # 5. Debug build
./gradlew assembleRelease    # 6. Release build (R8 full mode validation)
```

> Full command reference with 60+ commands: see `GEMINI.md` → Commands section.

### R8 Release Build (Enabled Mar 12, 2026)

**Critical `gradle.properties` flags** (previously disabled — now correctly on):

```properties
android.r8.strictFullModeForKeepRules=true    # Build FAILS if -keep rules missing (catches bugs early)
android.r8.optimizedResourceShrinking=true    # AGP 9.x resource shrinker
org.gradle.jvmargs=-Xmx4096m -XX:+UseParallelGC -XX:MaxMetaspaceSize=1g
org.gradle.caching=true
org.gradle.parallel=true
```

**ProGuard files:**

| File                        | Purpose                                                            |
| --------------------------- | ------------------------------------------------------------------ |
| `app/proguard-rules.pro`    | Master rules: AIDL Binder, serialization, Timber strip, singletons |
| `xposed/consumer-rules.pro` | Hook/service layer preservation                                    |
| `common/consumer-rules.pro` | AIDL Stub/Proxy, generators, models                                |

**Critical ProGuard gotchas:**

- `object` is NOT a valid ProGuard keyword — use `class` for Kotlin `object` singletons
- `INSTANCE` field must be explicitly kept for Kotlin singletons in R8 full mode
- Timber `v/d/i` are stripped via `-assumenosideeffects` — no log strings in release APK
- AIDL `Stub` and `Proxy` inner classes must be explicitly kept

**Release signing** — env vars only, never commit keystore:
`KEYSTORE_PATH`, `KEYSTORE_PASS`, `KEY_ALIAS`, `KEY_PASS`

---

## Technical Constraints

### Platform & LSPosed Requirements

- **Min SDK**: API 26 (Android 8.0) — Max: API 36 (Android 16, target)
- **LSPosed scope**: Must be set to **"System Framework (android)"** for AIDL service to work
- **Fallback scope**: Any user app (XSharedPreferences only — requires target app restart)
- **Minimum Xposed API**: 93 (required for `xposedsharedprefs`)
- **Required `AndroidManifest.xml`** meta-data:
  ```xml
  <meta-data android:name="xposedmodule" android:value="true" />
  <meta-data android:name="xposedsharedprefs" android:value="true" />
  <meta-data android:name="xposedminversion" android:value="93" />
  ```

### Performance Rules

- Hook callbacks must execute in **< 1ms** — cache all spoof values at registration time, not inside `after {}`
- Use `toClassOrNull()` + `lazy {}` for class references — avoid repeated `loadClass()` calls
- `AntiDetectHooker` **must load first** in `loadApp {}` before any spoofing hooks
- `SystemServiceHooker` **must load in `loadSystem {}`** — not `loadApp`

### Security Rules

- **NEVER** use bare `after {}` — always double-wrap: outer `runCatching` around `.hook{}`, inner `runCatching` inside `after/before {}`
- **NEVER** crash `system_server` code — every line must be in `try-catch` (bootloop risk!)
- **ALWAYS** use `java.security.SecureRandom` — `java.util.Random` and `chars.random()` are NOT cryptographic
- **ALWAYS** use `toClassOrNull()` not `toClass()` — throws `ClassNotFoundException` on older Android versions
- `throwToApp()` is called as **`exception.throwToApp()`** (Throwable extension) — not `throwToApp(exception)`
- Release APK: `isDebuggable = false`, Timber `v/d/i` stripped

### Known Issues & Workarounds

| Issue                               | Symptom                                                                    | Fix                                                                                                    |
| ----------------------------------- | -------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| **Android 16 Nav Crash**            | `NullPointerException` in `NavDestination.getRoute()` during recomposition | Use `object NavRoutes { const val HOME = "home" }` + `data class NavItem` — never sealed class objects |
| **R8 full mode INSTANCE stripping** | Runtime `NullPointerException` on Kotlin `object` access                   | Add `-keepclassmembers class ** { public static final *** INSTANCE; }` in ProGuard                     |
| **AIDL Stub stripped by R8**        | `ClassNotFoundException` at runtime for AIDL service                       | Keep `IDeviceMaskerService$Stub` and `$Stub$Proxy` explicitly                                          |

---

## External Companion Modules (User Installed)

> Device Masker is **fully offline** — no network requests, no external API calls.

| Module             | Purpose                             |
| ------------------ | ----------------------------------- |
| Shamiko            | Root hiding + Zygisk deny list      |
| Play Integrity Fix | Pass Play Integrity API checks      |
| Tricky Store       | Hardware attestation spoofing       |
| Zygisk-Next        | Zygisk support on KernelSU / APatch |

---

## File Structure (Mar 12, 2026)

```
devicemasker/
├── app/                    # :app — UI + LSPosed hook entry point
│   ├── hook/               # ⭐ HookEntry.kt (@InjectYukiHookWithXposed — LSPosed loads this)
│   ├── service/            # ConfigManager (StateFlow config + AIDL sync, ⭐ key file)
│   │                       # ServiceClient (AIDL client) · ServiceProvider (binder ContentProvider)
│   ├── data/               # XposedPrefs (MODE_WORLD_READABLE writer) · ConfigSync (JsonConfig→prefs)
│   │   ├── models/         # InstalledApp · TypeAliases
│   │   └── repository/     # SpoofRepository (bridge to ConfigManager) · AppScopeRepository
│   └── ui/
│       ├── theme/          # AppMotion springs · ColorScheme · Shapes
│       ├── screens/        # 5 screens: home/ groups/ groupspoofing/ settings/ diagnostics/
│       │                   # Each screen folder: Screen.kt + State.kt + ViewModel.kt
│       ├── components/     # Reusable M3 Expressive: Card, Switch, IconButton, AnimatedSection…
│       └── navigation/     # NavRoutes (string constants) · spring-based transitions
│
├── common/                 # :common — Shared contract (no Android deps, pure Kotlin)
│   ├── aidl/               # ⭐ IDeviceMaskerService.aidl (14-method AIDL IPC contract)
│   ├── [root]              # ⭐ SharedPrefsKeys (pref key SSoT) · SpoofType (24-type enum)
│   │                       # JsonConfig · SpoofGroup · AppConfig · DeviceProfilePreset (10 presets)
│   ├── models/             # SIMConfig · LocationConfig · DeviceHardwareConfig · Carrier (65+)
│   └── generators/         # ⭐ 9 generators — ALL use SecureRandom
│                           # IMEI (Luhn) · MAC (unicast) · IMSI · ICCID · Serial
│                           # UUID (AndroidID/GSF/AdvertisingID) · SIM · Fingerprint
│
├── xposed/                 # :xposed — Hook layer (YukiHookAPI, no KSP)
│   ├── [root]              # ⭐ XposedEntry.kt — loadSystem{} + loadApp{} hook orchestration
│   ├── service/            # ⭐ DeviceMaskerService (AIDL impl in system_server)
│   │                       # ConfigManager (atomic JSON → /data/misc/devicemasker/)
│   │                       # ServiceBridge (ContentProvider IPC binder bridge)
│   ├── hooker/             # ⭐ BaseSpoofHooker (AIDL-first + XSharedPrefs fallback base class)
│   │                       # ⭐ AntiDetectHooker (LOAD FIRST — stack trace/maps/PM hiding)
│   │                       # SystemServiceHooker (boot-time AIDL registration in system_server)
│   │                       # 7 domain hookers: Device · Network · Advertising · System
│   │                       #                   Location · Sensor · WebView
│   └── utils/              # ⭐ DualLog (YLog + ring buffer → AIDL log export)
│                           # ClassCache (LRU-100) · HookMetrics · ValueGenerators (⚠️ legacy)
│
├── gradle.properties       # ⭐ R8 full mode · 4 GB heap · parallel · build cache
├── gradle/libs.versions.toml  # ⭐ All dependency versions (single source)
├── app/proguard-rules.pro  # ⭐ Master R8: AIDL Binder · serialization · Timber strip
├── xposed/consumer-rules.pro  # Service + hooker + utils layer preservation
└── common/consumer-rules.pro  # AIDL Stub/Proxy · generators · @Serializable models
```

## Module Dependencies

```kotlin
// settings.gradle.kts — 3-module structure
include(":app", ":common", ":xposed")

// :app depends on both (KSP annotation processor here only)
dependencies {
    implementation(project(":common"))
    implementation(project(":xposed"))
    ksp(libs.yukihookapi.ksp.xposed)
}

// :xposed depends on :common only (no KSP)
dependencies {
    implementation(project(":common"))
    implementation(libs.yukihookapi.api)
}

// :common — no project dependencies (pure Kotlin library)
dependencies {
    implementation(libs.kotlinx.serialization.json)
}
```

> Repository sources: `google()`, `mavenCentral()`, `jitpack.io`, `repo.lsposed.foundation`
