# Technical Context: Device Masker

## Technology Stack

| Layer        | Library                | Version                   | Purpose                                               |
| ------------ | ---------------------- | ------------------------- | ----------------------------------------------------- |
| **Language** | Kotlin                 | 2.3.0                     | Primary language                                      |
| **Platform** | Android SDK            | API 36 / minSdk 26        | Android 8.0–16                                        |
| **Java**     | JVM                    | 21 (target) / 25 (host)   | Foojay toolchain provisioning                         |
| **Build**    | AGP + Gradle           | 9.1.0 / 9.3.1             | Multi-module build                                    |
| **UI**       | Compose BOM            | 2026.02.01                | Jetpack Compose (Feb 2026)                            |
| **UI**       | Material 3             | 1.4.0 (Stable)            | M3 Expressive components                              |
| **UI**       | Navigation Compose     | 2.9.7                     | NavHost + animated transitions                        |
| **UI**       | Lifecycle              | 2.10.0                    | `collectAsStateWithLifecycle`                         |
| **Hooking**  | **libxposed-api**      | **100**                   | **Hook API (replaces YukiHookAPI 1.3.1)**             |
| **Hooking**  | **libxposed-service**  | **100-1.0.0**             | **App-side RemotePreferences + UI service binding**   |
| **Hooking**  | AndroidHiddenApiBypass | 6.1                       | Hidden API access                                     |
| **Hooking**  | LSPosed                | **API 100**               | Framework (external, user installs)                   |
| **IPC**      | AIDL Binder            | —                         | Diagnostics-only: event counts, logs, hooked packages |
| **Config**   | **RemotePreferences**  | **via libxposed-service** | **Live config delivery — no target app restart**      |
| **Data**     | kotlinx.serialization  | 1.10.0                    | JSON config persistence                               |
| **Data**     | DataStore              | 1.2.0                     | UI-only settings (theme, AMOLED)                      |
| **Data**     | Kotlinx Coroutines     | 1.10.2                    | Async + StateFlow                                     |
| **Logging**  | Timber                 | 5.0.1                     | App-side logging (stripped in release)                |
| **Image**    | Coil Compose           | 3.4.0                     | Installed app icons in UI                             |

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

### Folder Exclusions (Context & Meta)

The following folders are explicitly excluded from Spotless, Lint, and IDE Indexing to prevent conflicts and improve performance:

- `memory-bank`, `openspec`, `scripts`, `.agents`, `.claude`, `docs`

Configured in `build.gradle.kts` (root) and root `lint.xml`.

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

### Platform & LSPosed Requirements (Updated Mar 13, 2026)

- **Min SDK**: API 26 (Android 8.0) — Max: API 36 (Android 16, target)
- **LSPosed API requirement**: `minApiVersion=100` (in `META-INF/xposed/module.prop`)
- **Entry point**: `META-INF/xposed/java_init.list` (not `assets/xposed_init`)
- **Config delivery**: `ModulePreferences` via `libxposed-service` ContentProvider (live, no restart)
- **Required `AndroidManifest.xml`** in `:app`:
  ```xml
  <meta-data android:name="xposedmodule" android:value="true" />
  <meta-data android:name="xposedminversion" android:value="100" />
  <provider android:name="io.github.libxposed.service.ModulePreferencesProvider"
            android:authorities="${applicationId}.lspd_prefs" ... />
  ```

### Performance Rules

- Hook callbacks must execute in **< 1ms** — prefs read is fast (SharedPreferences, already loaded)
- `xi.deoptimize(method)` **MUST be called** after every `xi.hook()` — prevents ART inlining bypassing hooks
- `AntiDetectHooker.hook()` **must be called FIRST** in `onPackageLoaded()` before any spoofing hooks
- `SystemServiceHooker.hook()` **must be called in `onSystemServerLoaded()`** — not `onPackageLoaded()`
- Each method gets its own `safeHook()` block — one OEM signature gap cannot cascade to block others

### Security Rules

- **NEVER** use bare exceptions in `@AfterInvocation` / `@BeforeInvocation` — always wrap in `try-catch`
- **NEVER** crash `system_server` code — every line must be in `try-catch` (bootloop risk!)
- **ALWAYS** use `java.security.SecureRandom` — `java.util.Random` and `chars.random()` are NOT cryptographic
- **ALWAYS** use `loadClassOrNull()` not direct `cl.loadClass()` — latter throws on missing classes
- `callback.throwable = NameNotFoundException(...)` replaces YukiHookAPI's `throwToApp()` extension
- Release APK: `isDebuggable = false`, Timber `v/d/i` stripped

### Known Issues & Workarounds

| Issue                               | Symptom                                                                    | Fix                                                                                                    |
| ----------------------------------- | -------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| **Android 16 Nav Crash**            | `NullPointerException` in `NavDestination.getRoute()` during recomposition | Use `object NavRoutes { const val HOME = "home" }` + `data class NavItem` — never sealed class objects |
| **R8 full mode INSTANCE stripping** | Runtime `NullPointerException` on Kotlin `object` access                   | Add `-keepclassmembers class ** { public static final *** INSTANCE; }` in ProGuard                     |
| **AIDL Stub stripped by R8**        | `ClassNotFoundException` at runtime for AIDL service                       | Keep `IDeviceMaskerService$Stub` and `$Stub$Proxy` explicitly                                          |
| **Multi-package hook callbacks**    | Later `onPackageLoaded()` calls can corrupt process-global hook state      | Only register hooks when `PackageLoadedParam.isFirstPackage()` is true                                 |

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

## File Structure (Updated Mar 13, 2026 — libxposed API 100)

```
devicemasker/
├── app/                    # :app — UI + libxposed-service (RemotePreferences provider)
│   ├── service/            # ConfigManager (StateFlow config, uses XposedServiceHelper)
│   │                       # ServiceClient (diagnostics-only AIDL client)
│   ├── data/               # XposedPrefs (RemotePreferences) · ConfigSync
│   │   ├── models/         # InstalledApp · TypeAliases
│   │   └── repository/     # SpoofRepository · AppScopeRepository
│   └── ui/
│       ├── theme/          # AppMotion springs · ColorScheme · Shapes
│       ├── screens/        # 5 screens: home/ groups/ groupspoofing/ settings/ diagnostics/
│       ├── components/     # Reusable M3 Expressive: Card, Switch, IconButton, AnimatedSection…
│       └── navigation/     # NavRoutes (string constants) · spring-based transitions
│
├── common/                 # :common — Shared contract (no Android deps, pure Kotlin)
│   ├── aidl/               # IDeviceMaskerService.aidl (8 diagnostic methods)
│   ├── [root]              # SharedPrefsKeys (pref key SSoT) · SpoofType (24-type enum)
│   │                       # JsonConfig · SpoofGroup · AppConfig · DeviceProfilePreset (10 presets)
│   ├── models/             # SIMConfig · LocationConfig · DeviceHardwareConfig · Carrier (65+)
│   └── generators/         # 9 generators — ALL use SecureRandom
│                           # IMEI (Luhn) · MAC (unicast) · IMSI · ICCID · Serial
│                           # UUID (AndroidID/GSF/AdvertisingID) · SIM · Fingerprint
│
├── xposed/                 # :xposed — Hook layer (libxposed API 100)
│   ├── resources/          # META-INF/xposed/java_init.list (entry point)
│   │                       # META-INF/xposed/module.prop (minApiVersion=100)
│   │                       # META-INF/xposed/scope.list (android)
│   ├── [root]              # XposedEntry.kt (XposedModule, onPackageLoaded/onSystemServerLoaded)
│   │                       # DeoptimizeManager.kt · PrefsHelper.kt · DualLog.kt · PrefsKeys.kt
│   ├── service/            # DeviceMaskerService (diagnostics AIDL)
│   │                       # Binder is discovered directly via ServiceManager from :app
│   ├── hooker/             # BaseSpoofHooker (safeHook/loadClassOrNull/methodOrNull)
│   │                       # AntiDetectHooker (LOAD FIRST — API 100, +ClassLoader hook)
│   │                       # SystemServiceHooker (AMS + SystemServer boot hooks)
│   │                       # 9 domain hookers (all API 100):
│   │                       # DeviceHooker · NetworkHooker · AdvertisingHooker · SystemHooker
│   │                       # LocationHooker · SensorHooker · WebViewHooker
│   │                       # SubscriptionHooker (NEW) · PackageManagerHooker (NEW)
│   └── utils/              # DualLog (android.util.Log + diagnostics forwarding)
│
├── gradle.properties       # ⭐ R8 full mode · 4 GB heap · parallel · build cache
├── gradle/libs.versions.toml  # ⭐ All dependency versions (single source), libxposed API 100
├── app/proguard-rules.pro  # Master R8: AIDL Binder · serialization · Timber strip
├── xposed/consumer-rules.pro  # Hook/service layer preservation
└── common/consumer-rules.pro  # AIDL Stub/Proxy · generators · @Serializable models
```

## Module Dependencies (Updated Mar 13, 2026)

```kotlin
// settings.gradle.kts — 3-module structure (unchanged)
include(":app", ":common", ":xposed")

// :app — UI + libxposed-service (no KSP, no YukiHookAPI)
dependencies {
    implementation(project(":common"))
    implementation(project(":xposed"))
    implementation(libs.libxposed.service)  // ModulePreferences provider
}

// :xposed — Hook layer (libxposed-api compileOnly, no KSP, no KavaRef)
dependencies {
    implementation(project(":common"))
    compileOnly(libs.libxposed.api)     // Provided at runtime by LSPosed
    implementation(libs.hiddenapibypass)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}

// :common — no project dependencies (pure Kotlin library)
dependencies {
    implementation(libs.kotlinx.serialization.json)
}
```

> Repository sources: `google()`, `mavenCentral()`, `jitpack.io`, `repo.lsposed.foundation`
