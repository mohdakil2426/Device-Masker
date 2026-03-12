## 1. Dependency & Build Setup (Phase 0)

- [ ] 1.1 Update `gradle/libs.versions.toml`: Remove `yukihookapi`, `kavaref`, `ksp` versions and libraries. Add `libxposed-api = "100"` and `libxposed-service = "100-1.0.0"` versions + library entries. **Ref:** `DEVICE_MASKER_LIBXPOSED_API100_PLAN.md` §5.3
- [ ] 1.2 Update `xposed/build.gradle.kts`: Replace `implementation(libs.yukihookapi.api)`, `implementation(libs.kavaref.core)`, `implementation(libs.kavaref.extension)`, `compileOnly(libs.xposed.api)` with `compileOnly(libs.libxposed.api)`. Add `resources.srcDirs("src/main/resources")` source set for META-INF files. Set `buildFeatures { aidl = false }`. **Ref:** §5.4
- [ ] 1.3 Update `app/build.gradle.kts`: Remove `alias(libs.plugins.ksp)` plugin. Remove `ksp(libs.yukihookapi.ksp.xposed)`, `implementation(libs.yukihookapi.api)`, `implementation(libs.kavaref.*)` dependencies. Add `implementation(libs.libxposed.service)`. **Ref:** §5.5
- [ ] 1.4 Remove KSP plugin from root `build.gradle.kts` if no other KSP users remain (check for Room, Hilt, etc.)
- [ ] 1.5 Run `./gradlew :xposed:dependencies` and `./gradlew :app:dependencies` to verify no YukiHookAPI/KavaRef/xposed-api:82 artifacts appear
- [ ] 1.6 Run `./gradlew :xposed:compileDebugKotlin :app:compileDebugKotlin :common:compileDebugKotlin` — expect compilation errors from removed YukiHookAPI imports (these are resolved in subsequent phases)

## 2. Entry Point Migration (Phase 1)

- [ ] 2.1 Delete `xposed/src/main/assets/xposed_init` (legacy entry point file)
- [ ] 2.2 Create `xposed/src/main/resources/META-INF/xposed/java_init.list` with content: `com.astrixforge.devicemasker.xposed.XposedEntry`
- [ ] 2.3 Create `xposed/src/main/resources/META-INF/xposed/module.prop` with: `minApiVersion=100`, `targetApiVersion=100`, `staticScope=false`
- [ ] 2.4 Create `xposed/src/main/resources/META-INF/xposed/scope.list` with content: `android`
- [ ] 2.5 Update `xposed/src/main/AndroidManifest.xml`: Change `xposedminversion` meta-data value from `93` to `100`. Keep backward-compat meta-data tags (`xposedmodule`, `xposedsharedprefs`). **Ref:** §6.5
- [ ] 2.6 Delete `app/src/main/kotlin/.../hook/HookEntry.kt` (YukiHookAPI KSP-generated entry — no longer needed)
- [ ] 2.7 Rewrite `xposed/src/main/kotlin/.../XposedEntry.kt`: Extend `XposedModule(base, param)`. Implement `onSystemServerLoaded()` (calls SystemServiceHooker) and `onPackageLoaded()` (checks RemotePreferences, registers all hookers via `hookSafely()`). Include singleton `instance` reference, `SKIP_PACKAGES` set, `PREFS_GROUP` constant. **Ref:** §6.6 — full code sample in planning doc
- [ ] 2.8 Verify: Build `./gradlew :xposed:assembleDebug` — should compile (other files will have errors, but XposedEntry itself should compile against libxposed-api)

## 3. RemotePreferences & PrefsHelper (Phase 2)

- [ ] 3.1 Rewrite `xposed/src/main/kotlin/.../PrefsHelper.kt` (or create new): Remove all XSharedPreferences references. Implement `getSpoofValue(prefs, packageName, type, fallback)` and `isSpoofTypeEnabled(prefs, packageName, type)` using `SharedPreferences` from RemotePreferences. **Ref:** §7.4
- [ ] 3.2 Update `app/src/main/kotlin/.../data/XposedPrefs.kt`: Replace `context.getSharedPreferences("device_masker_config", Context.MODE_WORLD_READABLE)` with `ModulePreferences.from(context, "device_masker_config")`. Add `init(context)` method. **Ref:** §21.1
- [ ] 3.3 Update `app/src/main/AndroidManifest.xml`: Add libxposed-service `ContentProvider` declaration: `<provider android:name="io.github.libxposed.service.ModulePreferencesProvider" android:authorities="${applicationId}.lspd_prefs" .../>`. **Ref:** §20.2
- [ ] 3.4 Update `app/src/main/kotlin/.../DeviceMaskerApp.kt`: Call `XposedPrefs.init(this)` in `onCreate()`. Remove `ServiceClient` initialization if doing Option A tasks later; for Option B keep ServiceClient but note config methods will be removed.
- [ ] 3.5 Update `app/src/main/kotlin/.../service/ConfigManager.kt` (app-side): Remove `syncToAidlService(json)` call from `saveConfigInternal()`. Config save path becomes: write JSON file + ConfigSync → ModulePreferences only. **Ref:** Storage doc §10.2
- [ ] 3.6 Verify: `ConfigSync.kt` uses `XposedPrefs.getPrefs(context).edit()` — no changes needed to ConfigSync itself (it still writes the same SharedPrefsKeys keys)
- [ ] 3.7 Verify: `SharedPrefsKeys.kt` in `:common` — no changes needed (key format unchanged)

## 4. BaseSpoofHooker Rewrite (Phase 3)

- [ ] 4.1 Rewrite `xposed/src/main/kotlin/.../hooker/BaseSpoofHooker.kt`: Remove `YukiBaseHooker` inheritance. Make it an `abstract class` with `tag: String` constructor parameter. Implement `safeHook(methodName, block)`, `ClassLoader.loadClassOrNull(name)`, `Class.methodOrNull(name, vararg params)`. Remove hybrid `getSpoofValue()`, `service` reference, `isServiceAvailable`, `incrementFilterCount()`. For Option B: add `reportSpoofEvent(pkg, spoofType)` helper that calls `XposedEntry.instance.reportSpoofEvent()`. **Ref:** §8, Storage doc §9.9
- [ ] 4.2 Verify: BaseSpoofHooker compiles standalone with libxposed-api imports

## 5. DeoptimizeManager (Phase 4)

- [ ] 5.1 Create `xposed/src/main/kotlin/.../DeoptimizeManager.kt`: Implement `deoptimizeWithCallers(xi, target, callers)` and `deoptimizeAll(xi, methods)`. Each deoptimize call wrapped in `runCatching`. Log warnings on failure. **Ref:** §9.2

## 6. DeviceHooker Complete Rewrite (Phase 5)

- [ ] 6.1 Rewrite `xposed/src/main/kotlin/.../hooker/DeviceHooker.kt`: Convert to `object : BaseSpoofHooker("DeviceHooker")`. Add `HookState` object with `@Volatile` fields for `pkg`, `prefs`, `xi`. Implement `hook(cl, xi, prefs, pkg)` function.
- [ ] 6.2 Implement IMEI hooks: `getDeviceId()`, `getDeviceId(int)`, `getImei()`, `getImei(int)` — each in its own `safeHook()` block. Create `GetImeiHooker` inner class with `@XposedHooker`, `@BeforeInvocation`, `companion object`. Call `xi.deoptimize()` after each hook. **Ref:** §10 lines 917-940
- [ ] 6.3 Implement IMSI hooks: `getSubscriberId()`, `getSubscriberId(int)` → `GetImsiHooker`. Deoptimize after hook.
- [ ] 6.4 Implement ICCID hooks: `getSimSerialNumber()`, `getSimSerialNumber(int)` → `GetIccidHooker`. Deoptimize. **Ref:** §10 lines 957-968
- [ ] 6.5 Implement phone number hooks: `getLine1Number()`, `getLine1Number(String)` → `GetPhoneNumberHooker`. Deoptimize. **(NEW — was missing in original)**
- [ ] 6.6 Implement phone count hook: `getPhoneCount()` → `GetPhoneCountHooker`. Returns `preset.simCount` or `1`. Deoptimize. **(NEW)**
- [ ] 6.7 Implement SIM country ISO hook: `getSimCountryIso()` → `GetSimCountryIsoHooker`. Deoptimize.
- [ ] 6.8 Implement network type hooks: `getNetworkType()`, `getDataNetworkType()` → `GetNetworkTypeHooker`. Uses `NetworkTypeMapper.getForMccMnc()`. Deoptimize. **(NEW)**
- [ ] 6.9 Implement serial hook: `Build.getSerial()` → `GetSerialHooker`. Deoptimize. **Ref:** §10 lines 1014-1022
- [ ] 6.10 Implement Android ID hook: `Settings.Secure.getString()` where key=`"android_id"` → `GetAndroidIdHooker`. Deoptimize. **Ref:** §10 lines 1024-1032
- [ ] 6.11 Add `getPrefsAndPkg()` helper method returning `Pair<SharedPreferences, String>?`
- [ ] 6.12 Verify: Run `./gradlew :xposed:compileDebugKotlin` — DeviceHooker compiles

## 7. NetworkHooker Expansion (Phase 6)

- [ ] 7.1 Convert `NetworkHooker.kt` to API 100 pattern: `object : BaseSpoofHooker("NetworkHooker")` with `HookState`, `@XposedHooker` inner classes
- [ ] 7.2 Migrate existing hooks (WiFi MAC, Bluetooth MAC, SSID, carrier name, carrier MCC/MNC) to API 100 pattern with deoptimize
- [ ] 7.3 Add BSSID hook: `WifiInfo.getBSSID()` → `GetBssidHooker`. Deoptimize. **(NEW)** **Ref:** §11
- [ ] 7.4 Add `NetworkInterface.getHardwareAddress()` hook → `MacAddressHooker`. Deoptimize. **(NEW)** — covers `java.net` MAC access
- [ ] 7.5 Add `ConnectivityManager.getActiveNetworkInfo()` hook → `NetworkInfoHooker`. **(NEW)**
- [ ] 7.6 Add `WifiManager.getConnectionInfo()` hook → `WifiConnectionInfoHooker`. **(NEW)**
- [ ] 7.7 Verify: Compile clean

## 8. NEW SubscriptionHooker (Phase 7)

- [ ] 8.1 Create `xposed/src/main/kotlin/.../hooker/SubscriptionHooker.kt`: `object : BaseSpoofHooker("SubscriptionHooker")` with `HookState` object. **Ref:** §12 — full code sample
- [ ] 8.2 Implement `getActiveSubscriptionInfoCount()` hook → `SubCountHooker` (returns `1` or `preset.simCount`)
- [ ] 8.3 Implement `SubscriptionInfo.getIccId()` hook → `SubIccIdHooker`. Deoptimize.
- [ ] 8.4 Implement `SubscriptionInfo.getNumber()` hook → `SubNumberHooker`. Deoptimize.
- [ ] 8.5 Implement `SubscriptionInfo.getDisplayName()` hook → `SubDisplayNameHooker`. Deoptimize.
- [ ] 8.6 Implement `SubscriptionInfo.getCountryIso()` hook → `SubCountryIsoHooker`. Returns lowercase. Deoptimize.
- [ ] 8.7 Add `getPrefsAndPkg()` helper. Each hook in its own `safeHook()` block.
- [ ] 8.8 Verify: Compile clean

## 9. SystemHooker Hardening (Phase 8)

- [ ] 9.1 Convert `SystemHooker.kt` to API 100 pattern with `HookState`
- [ ] 9.2 Implement `Build` class initializer hook via `xi.hookClassInitializer(buildClass, BuildClassInitHooker::class.java)` — sets all field values before any app code reads them. **Ref:** §13.1
- [ ] 9.3 Add `Build.TIME` field mutation via reflection in `safeHook("Build.TIME")` — set to `preset.buildTime`
- [ ] 9.4 Add `Build.ID` field mutation → set to `preset.buildId`
- [ ] 9.5 Add `Build.TAGS` field mutation → always set to `"release-keys"`
- [ ] 9.6 Add `Build.TYPE` field mutation → always set to `"user"`
- [ ] 9.7 Add `Build.VERSION.SECURITY_PATCH` field mutation → set to `preset.securityPatch`. Load `Build$VERSION` class via `loadClassOrNull()`. **Ref:** §13.2 lines 1466-1472
- [ ] 9.8 Add `Build.SUPPORTED_ABIS` field mutation → set to `preset.supportedAbis.toTypedArray()`
- [ ] 9.9 Migrate existing `Build.MODEL`, `MANUFACTURER`, `BRAND`, `DEVICE`, `PRODUCT`, `BOARD`, `FINGERPRINT` mutations to use `safeHook()` wrapping pattern
- [ ] 9.10 Verify: Compile clean

## 10. WebViewHooker Completion (Phase 9)

- [ ] 10.1 Convert `WebViewHooker.kt` to API 100 pattern with `HookState`
- [ ] 10.2 Migrate existing `WebSettings.getUserAgentString()` hook to `@XposedHooker` pattern with deoptimize
- [ ] 10.3 Add `WebView.getDefaultUserAgent(Context)` hook → `GetDefaultUserAgentHooker`. Deoptimize. Uses regex to replace model in UA. **(NEW)** **Ref:** §14.1-14.2
- [ ] 10.4 Add `System.getProperty(String)` hook → `SystemPropertyHooker`. Only intercepts `key="http.agent"`. Deoptimize. **(NEW)** **Ref:** §14.2 lines 1540-1557
- [ ] 10.5 Verify: Compile clean

## 11. NEW PackageManagerHooker (Phase 10)

- [ ] 11.1 Create `xposed/src/main/kotlin/.../hooker/PackageManagerHooker.kt`: `object : BaseSpoofHooker("PackageManagerHooker")` with `HookState`, `IDENTITY_FEATURES` set. **Ref:** §15 — full code sample
- [ ] 11.2 Implement `hasSystemFeature(String)` hook → `HasSystemFeatureHooker`. Only intercepts identity-relevant features. Returns value based on preset's `hasNfc`, `has5G` flags.
- [ ] 11.3 Implement `hasSystemFeature(String, int)` hook — reuses same `HasSystemFeatureHooker`
- [ ] 11.4 Implement `featureExistsOnPreset(preset, feature)` private helper that maps feature strings to preset boolean fields
- [ ] 11.5 Verify: Compile clean

## 12. AntiDetectHooker Hardening (Phase 11)

- [ ] 12.1 Convert `AntiDetectHooker.kt` to API 100 pattern (if not already using plain Kotlin — it may use `YukiBaseHooker` directly)
- [ ] 12.2 Migrate existing hooks (stack trace filtering, Class.forName, /proc/maps, PackageManager) to `@XposedHooker` pattern
- [ ] 12.3 Add `ClassLoader.loadClass(String)` hook → `LoadClassHooker`. Throws `ClassNotFoundException` for Xposed class names in `XPOSED_CLASSES` blocklist. **(NEW)** **Ref:** §16.1-16.2
- [ ] 12.4 Add `Runtime.exec(String)` hook → `RuntimeExecHooker`. Intercepts shell commands checking for Xposed files. **(NEW)**
- [ ] 12.5 Add `ActivityManager.getRunningServices(int)` hook → `RunningServicesHooker`. Filters module/Xposed services from result list. **(NEW)**
- [ ] 12.6 Verify: Compile clean

## 13. Remaining Hooker Migrations (Phase 5-continued)

- [ ] 13.1 Convert `AdvertisingHooker.kt` to API 100 pattern: `@XposedHooker` inner classes for AdvertisingIdClient, Gservices, MediaDrm hooks. Add deoptimize.
- [ ] 13.2 Convert `LocationHooker.kt` to API 100 pattern: `@XposedHooker` inner classes for Location, LocationManager, TimeZone, Locale hooks. Add deoptimize.
- [ ] 13.3 Convert `SensorHooker.kt` to API 100 pattern: `@XposedHooker` inner classes for SensorManager, Sensor hooks. Add deoptimize where applicable.
- [ ] 13.4 Verify: All hooker files compile clean with `./gradlew :xposed:compileDebugKotlin`

## 14. TAC-Aware IMEI Generation (Phase 12)

- [ ] 14.1 Add `DEVICE_TAC_PREFIXES` map to value generators in `:common` (or `:xposed/utils`): Maps preset IDs to lists of valid 8-digit TAC prefixes. Include entries for all 10 device presets + "generic" fallback. **Ref:** §17 lines 1752-1769
- [ ] 14.2 Implement `imeiForPreset(presetId)` function: Picks random TAC from preset's list, generates 6-digit SNR with `SecureRandom`, appends Luhn check digit. **Ref:** §17 lines 1777-1807
- [ ] 14.3 Implement `generateImeiWithTac(tac)` private helper and `luhnCheckDigit(number)` utility
- [ ] 14.4 Update `GetImeiHooker` in DeviceHooker to call `imeiForPreset(getPresetId(prefs, pkg))` instead of random IMEI generation
- [ ] 14.5 Add unit tests for `imeiForPreset()`: Verify TAC prefix correctness, Luhn validation, 15-digit length, SecureRandom usage. Add to `common/src/test/`

## 15. DeviceProfilePreset Enrichment (Phase 13)

- [ ] 15.1 Add new fields to `DeviceProfilePreset` data class in `:common`: `buildTime: Long`, `securityPatch: String`, `buildId: String`, `incremental: String`, `supportedAbis: List<String>`, `tacPrefixes: List<String>`, `simCount: Int = 1`, `hasNfc: Boolean = true`, `has5G: Boolean = true`. **Ref:** §13.2 lines 1406-1427
- [ ] 15.2 Update Pixel 9 Pro preset with accurate enriched data: `buildTime=1728100000000L`, `securityPatch="2024-10-05"`, `buildId="AP3A.241005.015"`, etc. **Ref:** §18 lines 1818-1838
- [ ] 15.3 Update Samsung Galaxy S24 Ultra preset with accurate enriched data: `simCount=2`, etc. **Ref:** §18 lines 1841-1860
- [ ] 15.4 Update remaining 8 presets (Pixel 8, Pixel 8 Pro, Samsung S24, OnePlus 12, Xiaomi 14 Pro, etc.) with accurate enriched data
- [ ] 15.5 Add `findById(id)` companion object method if not already present
- [ ] 15.6 Verify: `:common` module compiles, existing tests pass with `./gradlew :common:test`

## 16. NetworkTypeMapper & Correlation Matrix (Phase 14)

- [ ] 16.1 Create `NetworkTypeMapper.kt` (in `:xposed/utils` or `:common`): Maps MCC/MNC prefixes to `NETWORK_TYPE_LTE` (13) or `NETWORK_TYPE_NR` (20). Cover US ("310"/"311" → NR), India ("40440"/"40450"/"40410" → NR), UK ("234" → LTE), Germany ("262" → LTE), default → LTE. **Ref:** §19.1 lines 1898-1922
- [ ] 16.2 Verify `GetNetworkTypeHooker` in DeviceHooker correctly calls `NetworkTypeMapper.getForMccMnc()` with the spoofed carrier MCC/MNC
- [ ] 16.3 Verify correlation matrix: IMSI prefix matches CARRIER_MCC_MNC, ICCID country digits match carrier country, GPS coordinates in carrier country, timezone consistent. **Ref:** §19 full matrix table — this is a documentation/audit task, not code
- [ ] 16.4 Add unit test for `NetworkTypeMapper.getForMccMnc()` in `:common:test`

## 17. AIDL Diagnostics Service — Option B (Phase 2/8 combined)

- [ ] 17.1 Rewrite `common/src/main/aidl/.../IDeviceMaskerService.aidl`: Reduce from 15 methods to 8. Mark `reportSpoofEvent`, `reportLog`, `reportPackageHooked` as `oneway`. Keep `getSpoofEventCount`, `getHookedPackages`, `getLogs`, `clearDiagnostics`, `isAlive` as blocking reads. Remove all config methods. **Ref:** Storage doc §9.2
- [ ] 17.2 Rewrite `xposed/service/DeviceMaskerService.kt`: Remove `config: AtomicReference<JsonConfig>`, remove `ConfigManager` reference, remove all 6 config methods. Keep `logs`, `spoofCounts`, `hookedPackages` state. Implement 8 new method signatures. **Ref:** Storage doc §9.3 — full code sample
- [ ] 17.3 Delete `xposed/service/ConfigManager.kt` (xposed-side config — replaced by RemotePreferences)
- [ ] 17.4 Keep `xposed/service/ServiceBridge.kt` (still needed for binder discovery) — verify no config references
- [ ] 17.5 Simplify `xposed/hooker/SystemServiceHooker.kt`: Remove config loading logic, keep only service registration in AMS.main() hook. Convert to API 100 `@XposedHooker` pattern. **Ref:** Storage doc §9.4
- [ ] 17.6 Rewrite `app/service/ServiceClient.kt`: Remove `writeConfig()`, `readConfig()`, `isModuleEnabled()`, `isAppEnabled()`, `getSpoofValue()`, `incrementFilterCount()`, `log()`. Keep `connect()`, `disconnect()`, `getSpoofEventCount()`, `getHookedPackages()`, `getLogs()`, `clearDiagnostics()`, `isAlive()`. **Ref:** Storage doc §9.7
- [ ] 17.7 Update `app/ui/screens/diagnostics/DiagnosticsViewModel.kt`: Remove config-related service calls. Use simplified ServiceClient for diagnostics reads only. **Ref:** Storage doc §9.8
- [ ] 17.8 Add `reportSpoofEvent(pkg, spoofType)` and `reportPackageHooked(pkg)` to `XposedEntry.kt`: Fire-and-forget calls to diagnostics service via lazy cached reference. **Ref:** Storage doc §9.5
- [ ] 17.9 Update all hooker `@BeforeInvocation`/`@AfterInvocation` methods: Add `XposedEntry.instance.reportSpoofEvent(pkg, spoofTypeName)` call after returning spoofed value. **Ref:** Storage doc §9.6
- [ ] 17.10 Verify: `common` AIDL compiles, `xposed` service compiles, `app` ServiceClient compiles

## 18. ProGuard & Release Build (Phase 15)

- [ ] 18.1 Rewrite `xposed/consumer-rules.pro`: Add `-keep class com.astrixforge.devicemasker.xposed.XposedEntry { *; }`. Add `-keep class * implements io.github.libxposed.api.XposedInterface$Hooker { *; }`. Add `-keep @io.github.libxposed.api.annotations.XposedHooker class * { *; }`. Keep `@BeforeInvocation`/`@AfterInvocation` annotated methods. Keep full `com.astrixforge.devicemasker.xposed.**` package. Remove all YukiHookAPI keep rules. **Ref:** §20.1
- [ ] 18.2 Update `app/proguard-rules.pro`: Remove YukiHookAPI-specific rules. Add libxposed-service rules (keep `ModulePreferencesProvider`). Keep existing AIDL Binder, serialization, Timber strip rules.
- [ ] 18.3 Update `common/consumer-rules.pro`: Update AIDL keep rules for new 8-method interface (if method signatures changed). Keep existing generator, model, SharedPrefsKeys rules.
- [ ] 18.4 Run `./gradlew assembleRelease` — verify R8 passes with new ProGuard rules
- [ ] 18.5 Verify `mapping.txt`: `Select-String -Path 'app\build\outputs\mapping\release\mapping.txt' -Pattern 'XposedEntry|DeviceMaskerService|AntiDetect|DeviceHooker|SubscriptionHooker|PackageManagerHooker'` — all critical classes must appear (kept, not stripped)
- [ ] 18.6 Verify APK contents: `META-INF/xposed/java_init.list`, `META-INF/xposed/module.prop`, `META-INF/xposed/scope.list` exist. `assets/xposed_init` does NOT exist.

## 19. Cleanup & Deletion

- [ ] 19.1 Delete `app/src/main/kotlin/.../hook/HookEntry.kt` (YukiHookAPI KSP entry — replaced by XposedEntry)
- [ ] 19.2 Delete or archive `xposed/src/main/kotlin/.../utils/ClassCache.kt` if it exists (class caching replaced by `loadClassOrNull()` pattern)
- [ ] 19.3 Delete `xposed/src/main/kotlin/.../utils/HookHelper.kt` if it wraps YukiHookAPI utilities
- [ ] 19.4 Remove any remaining YukiHookAPI imports across all files: `grep -rn "import com.highcapable" xposed/src app/src --include='*.kt'` — must return 0 results
- [ ] 19.5 Remove any remaining KavaRef imports: `grep -rn "import com.highcapable.kavaref" xposed/src --include='*.kt'` — must return 0 results
- [ ] 19.6 Remove any remaining legacy Xposed API imports: `grep -rn "import de.robv.android.xposed" xposed/src --include='*.kt'` — must return 0 results
- [ ] 19.7 Clean up `_proposal_instructions.json`, `_design_instructions.json`, `_specs_instructions.json`, `_tasks_instructions.json` temp files from openspec change directory

## 20. Full Quality Gate Verification

- [ ] 20.1 Run `./gradlew spotlessApply` then `./gradlew spotlessCheck` — zero formatting violations
- [ ] 20.2 Run `./gradlew :app:compileDebugKotlin :common:compileDebugKotlin :xposed:compileDebugKotlin` — zero compilation errors
- [ ] 20.3 Run `./gradlew lint` — zero errors (warnings acceptable if baseline'd)
- [ ] 20.4 Run `./gradlew test` — all tests pass (existing 29 + new TAC/NetworkTypeMapper tests)
- [ ] 20.5 Run `./gradlew assembleDebug` — debug APK built successfully
- [ ] 20.6 Run `./gradlew assembleRelease` — release APK built successfully with R8

## 21. Xposed Safety Grep Checks

- [ ] 21.1 Unprotected hook callbacks: `grep -rn 'after {\|before {\|replaceAny {' xposed/src --include='*.kt' | grep -v 'runCatching'` — must return 0 results (all hooks use API 100 pattern with `safeHook()` now)
- [ ] 21.2 Hardcoded pref keys: `grep -rn '"module_enabled"\|"app_enabled_"\|"spoof_value_"\|"spoof_enabled_"' app/src xposed/src --include='*.kt'` — must return 0 results (all via SharedPrefsKeys)
- [ ] 21.3 Insecure random: `grep -rn 'Random()' common/src --include='*.kt' | grep -v 'SecureRandom'` — must return 0 results
- [ ] 21.4 Timber in xposed: `grep -rn 'Timber\.' xposed/src --include='*.kt'` — must return 0 results (use DualLog or libxposed log)
- [ ] 21.5 Compose in xposed/common: `grep -rn 'import androidx.compose' common/src xposed/src --include='*.kt'` — must return 0 results
- [ ] 21.6 YukiHookAPI remnants: `grep -rn 'yukihookapi\|YukiHookAPI\|toClass()\b' xposed/src app/src --include='*.kt'` — must return 0 results
- [ ] 21.7 Legacy Xposed API remnants: `grep -rn 'de.robv.android.xposed\|IXposedHookLoadPackage\|XC_MethodHook' xposed/src --include='*.kt'` — must return 0 results

## 22. Documentation & Memory Bank Update

- [ ] 22.1 Update `memory-bank/activeContext.md`: Mark migration as in-progress or complete, update current work focus
- [ ] 22.2 Update `memory-bank/techContext.md`: Replace YukiHookAPI/KavaRef/API 82 references with libxposed API 100. Update dependency table. Update file structure.
- [ ] 22.3 Update `memory-bank/systemPatterns.md`: Replace hook loading order from `loadApp/loadSystem` YukiHookAPI to `onPackageLoaded/onSystemServerLoaded`. Update BaseSpoofHooker pattern. Update hook safety pattern from `method { }.hook { after { } }` to `@XposedHooker` pattern.
- [ ] 22.4 Update `memory-bank/productContext.md`: Update data flow diagram — RemotePreferences instead of XSharedPreferences, diagnostics-only AIDL.
- [ ] 22.5 Update `memory-bank/progress.md`: Add libxposed API 100 migration milestone
- [ ] 22.6 Update `GEMINI.md`: Update tech stack table, architecture diagram, command sections, coding patterns to reflect API 100
