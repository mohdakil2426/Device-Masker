# Device Masker — Ultra-Comprehensive Master Implementation Plan

**Date:** 2026-05-04  
**Merged From:**
- `docs/reports/comprehensive_audit_report_2026-05-03.md` (General Audit)
- `docs/reports/M3E_IMPLEMENTATION_PLAN_2026-05-04.md` (Material 3 Expressive Plan)

**Scope:** Entire `:app` module (UI, data, service, diagnostics), build system, tests  
**Modules Excluded:** `common/`, `xposed/` (per user request)  
**Overall App Grade:** C+  
**M3E Compliance Score:** 38/100  
**Target M3E Compliance:** ≥ 75/100  
**Target M3E Version:** androidx.compose.material3:material3:1.5.0-alpha18  

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Findings Master Index](#2-findings-master-index)
3. [Phase 0: Safety & Stability (Critical Fixes)](#phase-0-safety--stability-critical-fixes)
4. [Phase 1: Testing Infrastructure](#phase-1-testing-infrastructure)
5. [Phase 2: M3E Theme Foundation (No Dependency Change)](#phase-2-m3e-theme-foundation-no-dependency-change)
6. [Phase 3: Architecture & State Management](#phase-3-architecture--state-management)
7. [Phase 4: Motion & Component Token Alignment](#phase-4-motion--component-token-alignment)
8. [Phase 5: Dependency Upgrade & M3E Component Migration](#phase-5-dependency-upgrade--m3e-component-migration)
9. [Phase 6: Navigation Modernization](#phase-6-navigation-modernization)
10. [Phase 7: Build Hardening & Optimization](#phase-7-build-hardening--optimization)
11. [Phase 8: Polish & Advanced M3E Features](#phase-8-polish--advanced-m3e-features)
12. [Phase 9: Final Validation & Testing](#phase-9-final-validation--testing)
13. [Testing Roadmap](#13-testing-roadmap)
14. [File Modification Index](#14-file-modification-index)
15. [Dependency Upgrade Matrix](#15-dependency-upgrade-matrix)
16. [Risk Assessment](#16-risk-assessment)
17. [Success Criteria](#17-success-criteria)
18. [Appendices](#18-appendices)

---

## 1. Executive Summary

This master plan merges the comprehensive code audit and Material 3 Expressive (M3E) research into a single, sequenced implementation roadmap. The plan contains **9 phases**, **100+ individual tasks**, and covers every finding from both reports without omission.

### Severity Summary
| Severity | Count | Status |
|----------|-------|--------|
| CRITICAL | 10 | Must fix in Phase 0 |
| HIGH | 15 | Must fix in Phases 0-3 |
| MEDIUM | 15 | Fix in Phases 2-5 |
| LOW | 10 | Fix in Phases 6-8 |
| M3E Critical | 3 | Fix in Phase 2 |
| M3E High | 4 | Fix in Phases 2-4 |
| M3E Medium | 3 | Fix in Phases 4-5 |

### Implementation Philosophy
- **Stability first:** All CRITICAL concurrency, ANR, and security fixes come before any UI changes
- **Test-driven:** Add testing infrastructure before modifying logic-heavy components
- **Incremental M3E:** Theme and token fixes happen before dependency upgrade
- **No surprise dependency changes:** Build file modifications require explicit user confirmation
- **Preserve working base:** LSPosed hook validation must pass after every phase

---

## 2. Findings Master Index

### CRITICAL Findings (Must Fix in Phase 0)

| ID | Finding | File | Line | Phase |
|----|---------|------|------|-------|
| CRIT-001 | ConfigManager lost-update race on `_config.value` | `service/ConfigManager.kt` | 149-153 | Phase 0 |
| CRIT-002 | PersistentAppLogTree synchronous file I/O on every log call | `service/AppLogStore.kt` | 33-41, 148-178 | Phase 0 |
| CRIT-003 | Zero ViewModel and Repository test coverage | All `*ViewModel.kt`, `SpoofRepository.kt`, `AppScopeRepository.kt` | — | Phase 1 |
| CRIT-004 | Release minification completely disabled | `app/build.gradle.kts` | 59 | Phase 7 |
| CRIT-005 | Exported XposedProvider without permission gate | `app/src/main/AndroidManifest.xml` | 78-82 | Phase 0 |
| CRIT-006 | SpoofRepository correlation caches not thread-safe | `data/repository/SpoofRepository.kt` | 53-55 | Phase 0 |
| CRIT-007 | RootShell can deadlock on timeout | `service/diagnostics/RootShell.kt` | 93-109 | Phase 0 |
| CRIT-008 | AndroidIdGeneratorTest tests its own helper, not production | `common/src/test/.../AndroidIdGeneratorTest.kt` | 67-72 | Phase 1 |
| CRIT-009 | No SavedStateHandle in any ViewModel | All `*ViewModel.kt` | — | Phase 3 |
| CRIT-010 | ConfigManager double-init race launches duplicate loaders | `service/ConfigManager.kt` | 59-73 | Phase 0 |

### HIGH Findings

| ID | Finding | File | Line | Phase |
|----|---------|------|------|-------|
| HIGH-001 | Full-screen loading overlays replace content | `screens/home/HomeScreen.kt`, `screens/groupspoofing/GroupSpoofingScreen.kt` | 202, 156 | Phase 3 |
| HIGH-002 | Touch targets below 48dp minimum | `components/expressive/ExpressiveIconButton.kt`, `CompactExpressiveIconButton.kt` | 63-64, 106-117 | Phase 4 |
| HIGH-003 | Compose State classes not marked Immutable | `screens/home/HomeState.kt`, `screens/groups/GroupsState.kt`, etc. | — | Phase 3 |
| HIGH-004 | String-based navigation without type safety | `MainActivity.kt`, `navigation/NavDestination.kt` | 290-302 | Phase 6 |
| HIGH-005 | No IME inset handling with edge-to-edge | `MainActivity.kt` | Scaffold setup | Phase 3 |
| HIGH-006 | Race condition in bidirectional pager/tab sync | `screens/groupspoofing/GroupSpoofingScreen.kt` | 70-79 | Phase 3 |
| HIGH-007 | Custom ToggleButton lacks all accessibility semantics | `components/ToggleButton.kt` | 46-115 | Phase 4 |
| HIGH-008 | `allowBackup=true` exposes sensitive config | `app/src/main/AndroidManifest.xml` | 20 | Phase 0 |
| HIGH-009 | LogManager file I/O on caller thread (no dispatcher switch) | `service/LogManager.kt` | 79-131, 133-152 | Phase 0 |
| HIGH-010 | AppScopeRepository cache invalidation lacks sync | `data/repository/AppScopeRepository.kt` | 27, 88-90 | Phase 0 |
| HIGH-011 | RootShell `execute()` blocks calling thread without dispatcher guard | `service/diagnostics/RootShell.kt` | 93-109 | Phase 0 |
| HIGH-012 | RootLogCollector unescaped shell interpolation | `service/diagnostics/RootLogCollector.kt` | 18, 29 | Phase 0 |
| HIGH-013 | JsonlDiagnosticStore unsynchronized concurrent read/write | `service/diagnostics/JsonlDiagnosticStore.kt` | 41-44 | Phase 0 |
| HIGH-014 | GroupSpoofingScreen no state restoration or deletion handling | `screens/groupspoofing/GroupSpoofingScreen.kt` | 56-162 | Phase 3 |
| HIGH-015 | ConfigSync public synchronous-commit APIs block caller thread | `data/ConfigSync.kt` | 93, 151 | Phase 0 |

### MEDIUM Findings

| ID | Finding | File | Line | Phase |
|----|---------|------|------|-------|
| MED-001 | Hardcoded category colors outside theme | `screens/groupspoofing/model/UIDisplayCategory.kt` | 39, 58, 67, 81, 95 | Phase 2 |
| MED-002 | Hardcoded status colors used directly in composables | `screens/diagnostics/DiagnosticsScreen.kt`, `screens/home/HomeScreen.kt` | 229, 220 | Phase 2 |
| MED-003 | `scale()` modifier used instead of `graphicsLayer` | `components/expressive/ExpressiveCard.kt`, `ExpressiveIconButton.kt`, `ToggleButton.kt` | 87, 78, 110 | Phase 4 |
| MED-004 | No contrast preference support (API 34+) | `theme/Theme.kt` | — | Phase 2 |
| MED-005 | Inline lambdas in screen content cause unstable recompositions | `MainActivity.kt`, `screens/home/HomeScreen.kt`, `screens/groups/GroupsScreen.kt` | 241-253, 106, 169-183 | Phase 3 |
| MED-006 | AppLogStore inefficient `takeLast` after full read | `service/AppLogStore.kt` | 44-45 | Phase 0 |
| MED-007 | ConfigManager no corrupted-config recovery / backup preservation | `service/ConfigManager.kt` | 82-101 | Phase 0 |
| MED-008 | SupportBundleBuilder OOM risk reading large root artifacts | `service/diagnostics/SupportBundleBuilder.kt` | 62-67 | Phase 0 |
| MED-009 | SpoofRepository `suspend` modifiers with no actual suspension | `data/repository/SpoofRepository.kt` | Various | Phase 3 |
| MED-010 | SpoofRepository.importGroups() silently swallows all exceptions | `data/repository/SpoofRepository.kt` | 595-603 | Phase 3 |
| MED-011 | ViewModels collect redundant/overlapping Flows | `screens/home/HomeViewModel.kt` | 38-78 | Phase 3 |
| MED-012 | Missing `contentDescription` on multiple icons | `screens/home/HomeScreen.kt`, `components/AppListItem.kt` | 273, 152 | Phase 3 |
| MED-013 | `SimpleDateFormat` instantiated in composable scope | `components/GroupCard.kt`, `screens/groups/GroupsScreen.kt` | 313, 178 | Phase 3 |
| MED-014 | RootLogCollector `grep -E '...|'` regex matches everything when target empty | `service/diagnostics/RootLogCollector.kt` | 18 | Phase 0 |
| MED-015 | DiagnosticSnapshotBuilderTest only tests REDACTED mode | `app/src/test/.../DiagnosticSnapshotBuilderTest.kt` | 10-52 | Phase 1 |

### LOW Findings

| ID | Finding | File | Line | Phase |
|----|---------|------|------|-------|
| LOW-001 | Missing previews for tab content and item components | `screens/groupspoofing/tabs/*.kt` | — | Phase 8 |
| LOW-002 | `remember` used for state that should survive config change | `screens/home/HomeScreen.kt`, `screens/groupspoofing/categories/SIMCardContent.kt` | 361, 65 | Phase 3 |
| LOW-003 | `Modifier.alpha()` on entire column for loading state | `screens/home/HomeScreen.kt` | 147 | Phase 3 |
| LOW-004 | `NavController` passed as default parameter in composable | `MainActivity.kt` | 151 | Phase 6 |
| LOW-005 | SettingsScreen pending export mode may drift from prop | `screens/settings/SettingsScreen.kt` | 133 | Phase 3 |
| LOW-006 | Missing `contentType` in some LazyColumns | `screens/diagnostics/DiagnosticsScreen.kt` | — | Phase 3 |
| LOW-007 | Redundant daemon property | `gradle.properties` | 24 | Phase 7 |
| LOW-008 | IDE-specific build logic | `build.gradle.kts` | 62-68 | Phase 7 |
| LOW-009 | Missing build-logic/convention plugins | All module `build.gradle.kts` | — | Phase 7 |
| LOW-010 | Spotless ktfmt not in version catalog | `build.gradle.kts` | 35 | Phase 7 |

### M3E Theme Compliance Findings

| Category | Score | Key Violations | Phase |
|----------|-------|----------------|-------|
| Color | 30/100 | Hardcoded colors in ColorScheme, missing surface containers in light theme, AMOLED breaks tonal elevation | Phase 2 |
| Shape | 35/100 | Only 5/10 corner steps, no asymmetric shapes | Phase 2 |
| Typography | 25/100 | Missing 15 emphasized styles, no variable fonts | Phase 2 |
| Motion | 45/100 | Custom springs don't match M3E tokens, no MotionScheme | Phase 4 |
| Elevation | 40/100 | No `surfaceColorAtElevation()`, raw dp values | Phase 4 |

---

## Phase 0: Safety & Stability (Critical Fixes)

**Goal:** Fix all CRITICAL and HIGH concurrency, ANR, security, and data integrity issues before any UI or architectural changes.

**Estimated Effort:** 12-16 hours  
**Risk:** Medium — changes to core config/logging infrastructure  
**Validation:** Full gate must pass; LSPosed smoke test on `com.mantle.verify`

---

### Task 0.1: ConfigManager Concurrency Hardening

**Subtask 0.1.1:** Fix lost-update race (CRIT-001)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/service/ConfigManager.kt`
- **Lines:** 149-153
- **Action:** Replace non-atomic read-modify-write with `MutableStateFlow.update { transform(it) }` (atomic CAS loop)
- **Reference:** [Kotlin StateFlow.update docs](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/update.html)
- **Code change:**
  ```kotlin
  // BEFORE
  private fun updateConfig(transform: (JsonConfig) -> JsonConfig) {
      val newConfig = transform(_config.value)
      _config.value = newConfig
      saveConfig()
  }
  
  // AFTER
  private val configMutex = Mutex()
  
  private fun updateConfig(transform: (JsonConfig) -> JsonConfig) {
      _config.update { current -> transform(current) }
      // saveConfig launched separately or use Mutex
  }
  ```

**Subtask 0.1.2:** Fix double-init race (CRIT-010)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/service/ConfigManager.kt`
- **Lines:** 59-73
- **Action:** Use `AtomicBoolean` or `synchronized` guard; set initialized flag BEFORE launching coroutine
- **Code change:**
  ```kotlin
  private val initLock = Any()
  
  fun init() {
      synchronized(initLock) {
          if (_isInitialized.value) return
          _isInitialized.value = true
      }
      scope.launch { loadConfig() }
  }
  ```

**Subtask 0.1.3:** Add corrupted-config recovery with backup preservation (MED-007)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/service/ConfigManager.kt`
- **Lines:** 82-101
- **Action:** On parse failure, rename corrupted file to `config.json.corrupted.$timestamp` before writing default
- **Code change:**
  ```kotlin
  try {
      // parse config
  } catch (e: Exception) {
      val corruptedName = "config.json.corrupted.${System.currentTimeMillis()}"
      atomicFile.baseFile.renameTo(File(filesDir, corruptedName))
      Timber.e(e, "Config corrupted; backed up to $corruptedName")
      // write default
  }
  ```

**Subtask 0.1.4:** Serialize file writes with Mutex
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/service/ConfigManager.kt`
- **Lines:** 129-136 (saveConfig area)
- **Action:** Wrap `AtomicFile` write operations in `Mutex.withLock { }`
- **Rationale:** Prevents concurrent save jobs from corrupting the file

---

### Task 0.2: AppLogStore & DiagnosticStore Concurrency

**Subtask 0.2.1:** Replace synchronous file I/O with async channel-based logging (CRIT-002)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/service/AppLogStore.kt`
- **Lines:** 33-41, 148-178
- **Action:**
  1. Add `Channel<DiagnosticEvent>(Channel.BUFFERED)` for in-memory queue
  2. Launch a dedicated coroutine (scope = `SupervisorJob() + Dispatchers.IO`) that drains the channel
  3. Remove `@Synchronized` from `append()`; replace with `channel.trySend(event)`
  4. The drain coroutine batches writes (e.g., flush every 50ms or 100 events)
- **Reference:** Kotlin Channels — [kotlinlang.org/docs/channels.html](https://kotlinlang.org/docs/channels.html)

**Subtask 0.2.2:** Fix inefficient `takeLast` (MED-006)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/service/AppLogStore.kt`
- **Lines:** 44-45
- **Action:** Read JSONL file in reverse (using `BufferedReader` with `ReversedLinesFileReader` or read all lines but limit parsing)

**Subtask 0.2.3:** Synchronize JsonlDiagnosticStore readEvents (HIGH-013)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/service/diagnostics/JsonlDiagnosticStore.kt`
- **Lines:** 41-44
- **Action:** Add `@Synchronized` to `readEvents()` or use `ReentrantReadWriteLock` with write lock in `append()` and read lock in `readEvents()`

---

### Task 0.3: Repository Thread Safety

**Subtask 0.3.1:** Fix SpoofRepository correlation caches (CRIT-006)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/data/repository/SpoofRepository.kt`
- **Lines:** 53-55
- **Action:** Replace `var` caches with `AtomicReference<SIMConfig>`, `AtomicReference<LocationConfig>`, `AtomicReference<DeviceHardwareConfig>`
- **Code change:**
  ```kotlin
  private val cachedSIMConfig = AtomicReference<SIMConfig?>(null)
  private val cachedLocationConfig = AtomicReference<LocationConfig?>(null)
  private val cachedDeviceHardwareConfig = AtomicReference<DeviceHardwareConfig?>(null)
  ```

**Subtask 0.3.2:** Fix AppScopeRepository cache invalidation (HIGH-010)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/data/repository/AppScopeRepository.kt`
- **Lines:** 27, 88-90
- **Action:** Replace `var isCacheValid: Boolean` with `AtomicBoolean` or protect with existing `cacheMutex`

---

### Task 0.4: RootShell & RootLogCollector Security

**Subtask 0.4.1:** Fix RootShell deadlock on timeout (CRIT-007)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/service/diagnostics/RootShell.kt`
- **Lines:** 93-109
- **Action:** Read stdout/stderr in separate threads BEFORE calling `waitFor()`, or use `ProcessBuilder.redirectOutput(File)`
- **Code change:**
  ```kotlin
  val stdoutBuffer = StringBuilder()
  val stderrBuffer = StringBuilder()
  
  val stdoutThread = thread { process.inputStream.bufferedReader().forEachLine { stdoutBuffer.appendLine(it) } }
  val stderrThread = thread { process.errorStream.bufferedReader().forEachLine { stderrBuffer.appendLine(it) } }
  
  val completed = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
  if (!completed) {
      process.destroyForcibly()
  }
  stdoutThread.join(100)
  stderrThread.join(100)
  ```

**Subtask 0.4.2:** Add dispatcher guard to RootShell (HIGH-011)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/service/diagnostics/RootShell.kt`
- **Action:** Make `execute()` a `suspend` function wrapped in `withContext(Dispatchers.IO)`, or document that callers must switch to IO

**Subtask 0.4.3:** Fix unescaped shell interpolation (HIGH-012)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/service/diagnostics/RootLogCollector.kt`
- **Lines:** 18, 29
- **Action:** Use `ProcessBuilder` with argument lists instead of string concatenation, or validate `targetPackage` with strict regex `[a-zA-Z0-9._]+` before interpolation

**Subtask 0.4.4:** Fix grep regex matching everything when target empty (MED-014)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/service/diagnostics/RootLogCollector.kt`
- **Line:** 18
- **Action:** Build grep pattern conditionally:
  ```kotlin
  val pattern = buildString {
      append("XposedEntry|DeviceMasker|LSPosed|Spoof event")
      if (!targetPackage.isNullOrBlank()) {
          append("|$targetPackage")
      }
  }
  ```

---

### Task 0.5: LogManager Dispatcher Fix

**Subtask 0.5.1:** Wrap file I/O in withContext(Dispatchers.IO) (HIGH-009)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/service/LogManager.kt`
- **Lines:** 79-131, 133-152
- **Action:** Wrap body of `buildSupportBundle()`, `getLogCount()`, `hasAnyLogs()` in `withContext(Dispatchers.IO)`

---

### Task 0.6: Security & Manifest Fixes

**Subtask 0.6.1:** Set allowBackup=false (HIGH-008)
- **File:** `app/src/main/AndroidManifest.xml`
- **Line:** 20
- **Action:** Change `android:allowBackup="true"` to `android:allowBackup="false"`
- **Rationale:** Prevents `adb backup` from extracting sensitive spoof config

**Subtask 0.6.2:** Document XposedProvider export (CRIT-005)
- **File:** `app/src/main/AndroidManifest.xml`
- **Lines:** 78-82
- **Action:** Add XML comment documenting that LSPosed requires cross-process access and enforces signature checks. If LSPosed does NOT enforce signature checks, investigate adding `android:permission` attribute.
- **Also add:** AGENT NOTE in manifest and a security document noting this as accepted architectural risk.

---

### Task 0.7: SupportBundleBuilder OOM Fix

**Subtask 0.7.1:** Stream files into ZipOutputStream (MED-008)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/service/diagnostics/SupportBundleBuilder.kt`
- **Lines:** 62-67
- **Action:** Replace `artifact.readText()` with buffered streaming:
  ```kotlin
  zip.putNextEntry(ZipEntry(artifact.name))
  artifact.inputStream().use { input ->
      input.copyTo(zip)
  }
  zip.closeEntry()
  ```

---

### Task 0.8: ConfigSync Dispatcher Fix

**Subtask 0.8.1:** Document or suspend ConfigSync APIs (HIGH-015)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/data/ConfigSync.kt`
- **Lines:** 93, 151
- **Action:**
  1. Add KDoc documenting that `syncApp()` and `clearApp()` perform synchronous disk I/O
  2. Add `suspend` variants:
     ```kotlin
     suspend fun syncAppAsync(packageName: String, config: AppConfig) = 
         withContext(Dispatchers.IO) { syncApp(packageName, config) }
     ```

---

### Phase 0 Checklist

- [ ] CRIT-001: ConfigManager uses atomic CAS update + Mutex for saves
- [ ] CRIT-010: ConfigManager init is synchronized, no double-launch
- [ ] MED-007: Corrupted config renamed before overwrite
- [ ] CRIT-002: AppLogStore uses Channel-based async logging
- [ ] MED-006: AppLogStore reads reverse or limits parsing
- [ ] HIGH-013: JsonlDiagnosticStore readEvents synchronized
- [ ] CRIT-006: SpoofRepository caches use AtomicReference
- [ ] HIGH-010: AppScopeRepository isCacheValid uses AtomicBoolean
- [ ] CRIT-007: RootShell reads stdout/stderr before waitFor
- [ ] HIGH-011: RootShell execute is suspend with IO dispatcher
- [ ] HIGH-012: RootLogCollector validates/escapes targetPackage
- [ ] MED-014: RootLogCollector grep pattern excludes empty alternatives
- [ ] HIGH-009: LogManager wraps file I/O in Dispatchers.IO
- [ ] HIGH-008: Manifest allowBackup=false
- [ ] CRIT-005: XposedProvider export documented as accepted risk
- [ ] MED-008: SupportBundleBuilder streams files to ZIP
- [ ] HIGH-015: ConfigSync has suspend IO variants or documentation
- [ ] Build passes: `./gradlew.bat spotlessCheck :app:testDebugUnitTest :common:testDebugUnitTest lint test assembleDebug --no-daemon`
- [ ] LSPosed smoke test passes on `com.mantle.verify`

---

## Phase 1: Testing Infrastructure

**Goal:** Establish comprehensive testing framework and add missing unit tests.

**Estimated Effort:** 16-20 hours  
**Risk:** Low — purely additive  
**Validation:** All new tests pass; coverage reporting enabled

---

### Task 1.1: Add Testing Dependencies

**Subtask 1.1.1:** Add Turbine and MockK to version catalog
- **File:** `gradle/libs.versions.toml`
- **Action:** Add:
  ```toml
  turbine = "1.2.0"
  mockk = "1.13.12"
  ```
  And corresponding library entries.

**Subtask 1.1.2:** Add test dependencies to app module
- **File:** `app/build.gradle.kts` (test dependencies block)
- **Action:** Add:
  ```kotlin
  testImplementation(libs.turbine)
  testImplementation(libs.mockk)
  testImplementation(libs.kotlinx.coroutines.test)
  ```

**Subtask 1.1.3:** Create MainDispatcherRule
- **File:** `app/src/test/kotlin/com/astrixforge/devicemasker/MainDispatcherRule.kt`
- **Action:** Create JUnit4 rule:
  ```kotlin
  @ExperimentalCoroutinesApi
  class MainDispatcherRule(
      private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
  ) : TestWatcher() {
      override fun starting(description: Description?) {
          Dispatchers.setMain(testDispatcher)
      }
      override fun finished(description: Description?) {
          Dispatchers.resetMain()
      }
  }
  ```

---

### Task 1.2: Fix Existing Tests

**Subtask 1.2.1:** Fix AndroidIdGeneratorTest (CRIT-008)
- **File:** `common/src/test/kotlin/com/astrixforge/devicemasker/common/generators/AndroidIdGeneratorTest.kt`
- **Lines:** 67-72
- **Action:** Remove local `generateAndroidId()` helper; import and test actual production `UUIDGenerator.generateAndroidId()`

**Subtask 1.2.2:** Add UNREDACTED mode test (MED-015)
- **File:** `app/src/test/kotlin/com/astrixforge/devicemasker/service/diagnostics/DiagnosticSnapshotBuilderTest.kt`
- **Action:** Add test verifying `RedactionMode.UNREDACTED` preserves raw identifiers

---

### Task 1.3: ViewModel Tests

**Subtask 1.3.1:** HomeViewModel tests
- **File:** `app/src/test/kotlin/com/astrixforge/devicemasker/ui/screens/home/HomeViewModelTest.kt`
- **Scenarios:**
  - [ ] Module toggle updates state
  - [ ] Group selection updates selectedGroup
  - [ ] Regeneration triggers config update
  - [ ] Xposed connection state reflects service binding
  - [ ] Flow combination produces correct enabledAppsCount

**Subtask 1.3.2:** GroupsViewModel tests
- **File:** `app/src/test/kotlin/com/astrixforge/devicemasker/ui/screens/groups/GroupsViewModelTest.kt`
- **Scenarios:**
  - [ ] Create group with valid name succeeds
  - [ ] Delete group removes from state
  - [ ] Export produces valid JSON
  - [ ] Import with malformed JSON returns error

**Subtask 1.3.3:** GroupSpoofingViewModel tests
- **File:** `app/src/test/kotlin/com/astrixforge/devicemasker/ui/screens/groupspoofing/GroupSpoofingViewModelTest.kt`
- **Scenarios:**
  - [ ] Tab switching updates selectedTab
  - [ ] Value regeneration produces correlated values
  - [ ] Carrier update syncs timezone
  - [ ] App assignment adds to group
  - [ ] Group deletion triggers navigation event

**Subtask 1.3.4:** SettingsViewModel tests
- **File:** `app/src/test/kotlin/com/astrixforge/devicemasker/ui/screens/settings/SettingsViewModelTest.kt`
- **Scenarios:**
  - [ ] Theme change persists to DataStore
  - [ ] Export success/failure states
  - [ ] Clear logs resets log count

**Subtask 1.3.5:** DiagnosticsViewModel tests
- **File:** `app/src/test/kotlin/com/astrixforge/devicemasker/ui/screens/diagnostics/DiagnosticsViewModelTest.kt`
- **Scenarios:**
  - [ ] Service connection state updates
  - [ ] Refresh triggers diagnostics collection
  - [ ] Anti-detection test results

---

### Task 1.4: Repository Tests

**Subtask 1.4.1:** SpoofRepository tests
- **File:** `app/src/test/kotlin/com/astrixforge/devicemasker/data/repository/SpoofRepositoryTest.kt`
- **Scenarios:**
  - [ ] Correlation consistency: IMEI + IMSI + ICCID from same config
  - [ ] SIM-only regeneration preserves other values
  - [ ] Carrier update syncs timezone automatically
  - [ ] Import malformed JSON throws with details
  - [ ] Import valid JSON merges groups

**Subtask 1.4.2:** AppScopeRepository tests
- **File:** `app/src/test/kotlin/com/astrixforge/devicemasker/data/repository/AppScopeRepositoryTest.kt`
- **Scenarios:**
  - [ ] PackageManager failure handled gracefully
  - [ ] Cache invalidation clears cached list
  - [ ] System app filtering excludes system packages
  - [ ] Search query filters correctly

---

### Task 1.5: Config and Sync Tests

**Subtask 1.5.1:** ConfigManager tests
- **File:** `app/src/test/kotlin/com/astrixforge/devicemasker/service/ConfigManagerTest.kt`
- **Scenarios:**
  - [ ] Concurrent modifications don't lose updates
  - [ ] Corrupted JSON triggers recovery with backup
  - [ ] File write failure surfaces error
  - [ ] Double init doesn't launch duplicate loaders

**Subtask 1.5.2:** ConfigSync tests
- **File:** `app/src/test/kotlin/com/astrixforge/devicemasker/data/ConfigSyncTest.kt`
- **Scenarios:**
  - [ ] Commit failure handled
  - [ ] Null prefs handled gracefully
  - [ ] Empty config produces minimal keys
  - [ ] Large config snapshot doesn't ANR

**Subtask 1.5.3:** XposedPrefs tests
- **File:** `app/src/test/kotlin/com/astrixforge/devicemasker/data/XposedPrefsTest.kt`
- **Scenarios:**
  - [ ] Service bind/unbind callbacks delivered
  - [ ] getPrefs() failure handled

---

### Phase 1 Checklist

- [ ] Turbine and MockK added to version catalog and app build
- [ ] MainDispatcherRule created
- [ ] CRIT-008: AndroidIdGeneratorTest tests production code
- [ ] MED-015: DiagnosticSnapshotBuilderTest covers UNREDACTED mode
- [ ] HomeViewModel tests: 5+ scenarios passing
- [ ] GroupsViewModel tests: 4+ scenarios passing
- [ ] GroupSpoofingViewModel tests: 5+ scenarios passing
- [ ] SettingsViewModel tests: 3+ scenarios passing
- [ ] DiagnosticsViewModel tests: 3+ scenarios passing
- [ ] SpoofRepository tests: 5+ scenarios passing
- [ ] AppScopeRepository tests: 4+ scenarios passing
- [ ] ConfigManager tests: 4+ scenarios passing
- [ ] ConfigSync tests: 4+ scenarios passing
- [ ] XposedPrefs tests: 2+ scenarios passing
- [ ] `./gradlew.bat :app:testDebugUnitTest --no-daemon` passes

---

## Phase 2: M3E Theme Foundation (No Dependency Change)

**Goal:** Fix all M3E theme compliance issues without upgrading material3 dependency.

**Estimated Effort:** 8-10 hours  
**Risk:** Low — no dependency changes  
**Validation:** Visual inspection across light/dark/AMOLED themes; no regressions

---

### Task 2.1: Color Token Compliance

**Subtask 2.1.1:** Extract all hardcoded colors from Theme.kt to Color.kt
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/theme/Theme.kt` (source)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/theme/Color.kt` (destination)
- **Lines to extract:**
  - `Color(0xFFCCFFFF)` → `val OnSecondaryContainerDark = Color(0xFFCCFFFF)`
  - `Color(0xFFEDE7F6)` → `val OnTertiaryContainerDark = Color(0xFFEDE7F6)`
  - `Color(0xFFFFDAD6)` → `val OnErrorContainerDark = Color(0xFFFFDAD6)`
  - `Color(0xFF2A2A2A)` → `val SurfaceContainerHighestDark = Color(0xFF2A2A2A)`
  - `Color(0xFFE3E3E3)` → `val InverseSurfaceDark = Color(0xFFE3E3E3)`
  - `Color(0xFF1A1A1A)` → `val InverseOnSurfaceDark = Color(0xFF1A1A1A)`
  - `Color(0xFF00332C)` → `val OnSecondaryContainerLight = Color(0xFF00332C)`
  - `Color(0xFF21005D)` → `val OnTertiaryContainerLight = Color(0xFF21005D)`
  - `Color(0xFF410002)` → `val OnErrorContainerLight = Color(0xFF410002)`
  - `Color(0xFF121212)` → `val BackgroundRegularDark = Color(0xFF121212)`
  - `Color(0xFF1E1E1E)` → `val SurfaceVariantRegularDark = Color(0xFF1E1E1E)`
  - `Color(0xFFC0C0C0)` → `val OnSurfaceVariantRegularDark = Color(0xFFC0C0C0)`
  - `Color(0xFF242424)` → `val SurfaceContainerHighRegularDark = Color(0xFF242424)`
  - `Color(0xFF2E2E2E)` → `val SurfaceContainerHighestRegularDark = Color(0xFF2E2E2E)`
  - `Color(0xFF161616)` → `val SurfaceContainerLowRegularDark = Color(0xFF161616)`
  - `Color(0xFF0E0E0E)` → `val SurfaceContainerLowestRegularDark = Color(0xFF0E0E0E)`
- **Reference:** M3E color system — `.agents/skills/material-3-expressive/references/m3-color-system.md`

**Subtask 2.1.2:** Add complete surface container roles to LightColorScheme
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/theme/Theme.kt`
- **Lines:** 71-96
- **Action:** Add missing roles:
  ```kotlin
  background = Color(0xFFFDFDFD),           // ~tone 98
  onBackground = Color(0xFF1F1F1F),
  surface = Color(0xFFFDFDFD),
  onSurface = Color(0xFF1F1F1F),
  surfaceVariant = Color(0xFFE3E3E3),       // ~tone 90
  onSurfaceVariant = Color(0xFF444746),
  surfaceContainerLowest = Color(0xFFFFFFFF), // tone 100
  surfaceContainerLow = Color(0xFFF8FAFD),    // tone 98
  surfaceContainer = Color(0xFFF0F4F9),       // tone 94
  surfaceContainerHigh = Color(0xFFE9EEF5),   // tone 92
  surfaceContainerHighest = Color(0xFFE3E8F0),// tone 90
  outline = Color(0xFF747775),
  outlineVariant = Color(0xFFC4C7C5),
  inverseSurface = Color(0xFF303030),
  inverseOnSurface = Color(0xFFF2F2F2),
  inversePrimary = PrimaryDark,
  scrim = Color.Black,
  ```
- **Reference:** M3E color foundation tokens — `.agents/skills/material-3-expressive/references/m3-color-foundation-tokens.md`

**Subtask 2.1.3:** Document AMOLED theme as intentional M3E deviation
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/theme/Theme.kt`
- **Action:** Add KDoc comment above AMOLED scheme:
  ```kotlin
  /**
   * AMOLED Dark Color Scheme.
   *
   * NOTE: This scheme intentionally uses pure black (#000000) for background and surface
   * to maximize OLED battery efficiency. This deviates from M3E tonal elevation semantics
   * where elevation is communicated through surface tone shifts rather than shadows.
   * All surface container roles are mapped to near-black tones to preserve this behavior
   * while maintaining role completeness.
   */
  ```

**Subtask 2.1.4:** Derive category colors from theme (MED-001)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/groupspoofing/model/UIDisplayCategory.kt`
- **Lines:** 39, 58, 67, 81, 95
- **Action:** Replace hardcoded hex colors with semantic `MaterialTheme.colorScheme` roles:
  - Device Profile → `primary`
  - Telephony → `secondary`
  - SIM/Network → `tertiary`
  - Location/Sensors → `error` (or custom `surfaceVariant` tint)
  - WebView/Other → `outline`

**Subtask 2.1.5:** Map status colors to semantic theme colors (MED-002)
- **Files:**
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/diagnostics/DiagnosticsScreen.kt` (line 229)
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/home/HomeScreen.kt` (line 220)
- **Action:** Replace direct `StatusActive`/`StatusInactive`/`StatusWarning` usage with:
  ```kotlin
  val statusColor = when (status) {
      Status.ACTIVE -> MaterialTheme.colorScheme.primary
      Status.INACTIVE -> MaterialTheme.colorScheme.error
      Status.WARNING -> MaterialTheme.colorScheme.tertiary
  }
  ```

**Subtask 2.1.6:** Add contrast preference support (API 34+) (MED-004)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/theme/Theme.kt`
- **Action:** Check `ContrastLevel` when available:
  ```kotlin
  dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
      val contrastLevel = context.resources.configuration.contrastLevel
      dynamicDarkColorScheme(context, contrastLevel)
  }
  ```
- **Reference:** Android 14 contrast APIs — search Google Developer docs for "contrast level dynamic color"

---

### Task 2.2: Shape Scale Expansion

**Subtask 2.2.1:** Expand to full 10-step symmetric scale
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/theme/Shapes.kt`
- **Action:** Replace current 5-step with 10-step:
  ```kotlin
  val AppShapes = Shapes(
      extraSmall = RoundedCornerShape(4.dp),     // chips, small buttons
      small = RoundedCornerShape(8.dp),          // text fields, list items
      medium = RoundedCornerShape(12.dp),        // cards, dialogs
      large = RoundedCornerShape(16.dp),         // bottom sheets
      extraLarge = RoundedCornerShape(28.dp),    // full-screen dialogs
  )
  
  // NEW: Expressive shape tokens beyond Material Shapes
  object ExpressiveShapes {
      val none = RoundedCornerShape(0.dp)
      val extraSmall = RoundedCornerShape(4.dp)
      val small = RoundedCornerShape(8.dp)
      val medium = RoundedCornerShape(12.dp)
      val large = RoundedCornerShape(16.dp)
      val largeIncreased = RoundedCornerShape(20.dp)
      val extraLarge = RoundedCornerShape(28.dp)
      val extraLargeIncreased = RoundedCornerShape(32.dp)
      val extraExtraLarge = RoundedCornerShape(48.dp)
      val full = RoundedCornerShape(50) // or CircleShape
      
      // Asymmetric variants
      val extraSmallTop = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
      val largeStart = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
      val largeEnd = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
      val extraLargeTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
  }
  ```
- **Reference:** M3E shape spec — `.agents/skills/material-3-expressive/references/m3-shape.md`

---

### Task 2.3: Emphasized Typography

**Subtask 2.3.1:** Add 15 emphasized type styles
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/theme/Typography.kt`
- **Current:** Ends at line 150 with comment `// Material 3 Expressive Typography Extensions`
- **Action:** Add after line 150:
  ```kotlin
  // Emphasized variants — higher weight for primary actions, selection, headlines
  val AppTypographyEmphasized = Typography(
      displayLarge = AppTypography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
      displayMedium = AppTypography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
      displaySmall = AppTypography.displaySmall.copy(fontWeight = FontWeight.Bold),
      headlineLarge = AppTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
      headlineMedium = AppTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
      headlineSmall = AppTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
      titleLarge = AppTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
      titleMedium = AppTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      titleSmall = AppTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
      bodyLarge = AppTypography.bodyLarge.copy(fontWeight = FontWeight.Medium),
      bodyMedium = AppTypography.bodyMedium.copy(fontWeight = FontWeight.Medium),
      bodySmall = AppTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
      labelLarge = AppTypography.labelLarge.copy(fontWeight = FontWeight.Bold),
      labelMedium = AppTypography.labelMedium.copy(fontWeight = FontWeight.Bold),
      labelSmall = AppTypography.labelSmall.copy(fontWeight = FontWeight.Bold),
  )
  ```
- **Reference:** M3E typography spec — `.agents/skills/material-3-expressive/references/m3-typography.md`

**Subtask 2.3.2:** Create expressive typography provider
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/theme/Typography.kt`
- **Action:** Add composition local for emphasized typography access:
  ```kotlin
  val LocalEmphasizedTypography = staticCompositionLocalOf { AppTypographyEmphasized }
  
  @Composable
  fun emphasizedTypography(): Typography = LocalEmphasizedTypography.current
  ```

---

### Phase 2 Checklist

- [ ] All hardcoded colors extracted to named constants in Color.kt
- [ ] LightColorScheme has complete surface container roles
- [ ] AMOLED theme documented as intentional deviation
- [ ] Category colors derived from MaterialTheme.colorScheme
- [ ] Status colors mapped to semantic theme roles
- [ ] Contrast preference support added (API 34+)
- [ ] Shape scale expanded to 10 symmetric steps + asymmetric variants
- [ ] 15 emphasized typography styles added
- [ ] Emphasized typography accessible via composition local
- [ ] No visual regressions in light/dark/AMOLED themes
- [ ] `./gradlew.bat spotlessCheck assembleDebug --no-daemon` passes

---

## Phase 3: Architecture & State Management

**Goal:** Fix architectural issues: SavedStateHandle, Compose stability, navigation type safety preparation, IME handling, screen state management.

**Estimated Effort:** 14-18 hours  
**Risk:** Medium — touches ViewModels and screens  
**Validation:** All tests pass; process death survival verified; no ANRs

---

### Task 3.1: SavedStateHandle in ViewModels (CRIT-009)

**Subtask 3.1.1:** Add SavedStateHandle to all ViewModels
- **Files:** All `*ViewModel.kt` files
- **Action:** Inject `SavedStateHandle` via constructor:
  ```kotlin
  class HomeViewModel(
      private val repository: SpoofRepository,
      private val savedStateHandle: SavedStateHandle,
  ) : ViewModel() {
      // Persist critical UI state
      var selectedTab by savedStateHandle.saveable { mutableStateOf(0) }
          private set
  }
  ```

**Subtask 3.1.2:** Persist and restore critical UI state
- **State to persist per ViewModel:**
  - `HomeViewModel`: selected group, search query
  - `GroupSpoofingViewModel`: selected tab index, expanded categories
  - `GroupsViewModel`: search query, create/edit dialog visibility
  - `SettingsViewModel`: pending export mode, expanded sections
  - `DiagnosticsViewModel`: selected test filters

**Subtask 3.1.3:** Update ViewModel factory/creation
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/ViewModelFactory.kt` (or wherever ViewModels are created)
- **Action:** Pass `SavedStateHandle` to constructors when creating ViewModels

---

### Task 3.2: Compose State Stability (HIGH-003)

**Subtask 3.2.1:** Mark State classes as @Immutable
- **Files:**
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/home/HomeState.kt`
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/groups/GroupsState.kt`
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/groupspoofing/GroupSpoofingState.kt`
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/settings/SettingsState.kt`
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/diagnostics/DiagnosticsState.kt`
- **Action:** Add `@Immutable` annotation and import `kotlinx.collections.immutable.ImmutableList`
- **Code change:**
  ```kotlin
  @Immutable
  data class HomeState(
      val groups: ImmutableList<SpoofGroup> = persistentListOf(),
      // ...
  )
  ```

**Subtask 3.2.2:** Replace inline lambdas with stable callbacks (MED-005)
- **Files:**
  - `app/src/main/kotlin/com/astrixforge/devicemasker/MainActivity.kt` (lines 241-253)
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/home/HomeScreen.kt` (line 106)
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/groups/GroupsScreen.kt` (lines 169-183)
- **Action:** Hoist lambdas with `remember`:
  ```kotlin
  val onThemeChange = remember { { mode: ThemeMode -> settingsViewModel.setThemeMode(mode) } }
  ```

---

### Task 3.3: Screen State & UX Fixes

**Subtask 3.3.1:** Replace full-screen loading overlays (HIGH-001)
- **Files:**
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/home/HomeScreen.kt` (line 202)
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/groupspoofing/GroupSpoofingScreen.kt` (line 156)
- **Action:** Replace `AnimatedLoadingOverlay` with inline `CircularProgressIndicator` or skeleton placeholders:
  ```kotlin
  if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
  } else {
      // content
  }
  ```

**Subtask 3.3.2:** Fix bidirectional pager/tab sync (HIGH-006)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/groupspoofing/GroupSpoofingScreen.kt`
- **Lines:** 70-79
- **Action:** Use `snapshotFlow` with `distinctUntilChanged()`:
  ```kotlin
  LaunchedEffect(pagerState) {
      snapshotFlow { pagerState.currentPage }
          .distinctUntilChanged()
          .collect { page ->
              viewModel.setSelectedTab(page)
          }
  }
  
  // Only animate from ViewModel to pager, not reverse
  LaunchedEffect(selectedTab) {
      if (pagerState.currentPage != selectedTab) {
          pagerState.animateScrollToPage(selectedTab)
      }
  }
  ```

**Subtask 3.3.3:** Add state restoration to GroupSpoofingScreen (HIGH-014)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/groupspoofing/GroupSpoofingScreen.kt`
- **Lines:** 56-162
- **Action:**
  1. Use `rememberSaveable { mutableStateOf(0) }` for pager state or persist in ViewModel SavedStateHandle
  2. Add `LaunchedEffect(group)` to detect null group (deleted) and trigger `onNavigateBack()`:
     ```kotlin
     LaunchedEffect(group) {
         if (group == null) {
             onNavigateBack()
         }
     }
     ```

**Subtask 3.3.4:** Fix IME insets with edge-to-edge (HIGH-005)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/MainActivity.kt`
- **Action:**
  1. Add `android:windowSoftInputMode="adjustResize"` to `MainActivity` in manifest
  2. Add `Modifier.imePadding()` to scrollable containers containing text fields:
     ```kotlin
     LazyColumn(
         modifier = Modifier.imePadding()
     ) { /* ... */ }
     ```

**Subtask 3.3.5:** Add contentDescription to icons (MED-012)
- **Files:**
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/home/HomeScreen.kt` (line 273)
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/AppListItem.kt` (line 152)
- **Action:** Provide dynamic descriptions:
  ```kotlin
  Icon(
      imageVector = Icons.Default.Shield,
      contentDescription = if (isActive) "Module active" else "Module inactive",
  )
  ```

**Subtask 3.3.6:** Cache SimpleDateFormat (MED-013)
- **Files:**
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/GroupCard.kt` (line 313)
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/groups/GroupsScreen.kt` (line 178)
- **Action:** Use `remember` or move to ViewModel:
  ```kotlin
  val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
  ```

**Subtask 3.3.7:** Fix remember vs rememberSaveable (LOW-002)
- **Files:**
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/home/HomeScreen.kt` (line 361)
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/groupspoofing/categories/SIMCardContent.kt` (line 65)
- **Action:** Change `remember { mutableStateOf(...) }` to `rememberSaveable { mutableStateOf(...) }` for dialog visibility flags

**Subtask 3.3.8:** Fix alpha modifier on loading column (LOW-003)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/home/HomeScreen.kt` (line 147)
- **Action:** Replace `Modifier.alpha(if (isLoading) 0f else 1f)` with `AnimatedVisibility(visible = !isLoading)` or conditional composition

**Subtask 3.3.9:** Add contentType to LazyColumns (LOW-006)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/diagnostics/DiagnosticsScreen.kt`
- **Action:** Add `contentType` parameter:
  ```kotlin
  LazyColumn {
      item(contentType = "header") { /* header */ }
      items(diagnostics, contentType = { "card" }) { /* card */ }
  }
  ```

**Subtask 3.3.10:** Fix SettingsScreen export mode drift (LOW-005)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/settings/SettingsScreen.kt` (line 133)
- **Action:** Add synchronization:
  ```kotlin
  var pendingExportMode by remember { mutableStateOf(exportMode) }
  LaunchedEffect(exportMode) { pendingExportMode = exportMode }
  ```

---

### Task 3.4: Repository Architecture Fixes

**Subtask 3.4.1:** Remove redundant suspend modifiers (MED-009)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/data/repository/SpoofRepository.kt`
- **Action:** Remove `@Suppress("RedundantSuspendModifier")` and `suspend` from methods that don't actually suspend. Add `suspend` only when calling `withContext(Dispatchers.IO)` or other suspend functions.

**Subtask 3.4.2:** Fix importGroups exception swallowing (MED-010)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/data/repository/SpoofRepository.kt`
- **Lines:** 595-603
- **Action:** Distinguish error types and log:
  ```kotlin
  suspend fun importGroups(jsonString: String): Result<Unit> {
      return try {
          val config = JsonConfig.parse(jsonString)
          config.getAllGroups().forEach { group ->
              ConfigManager.updateGroup(group)
          }
          Result.success(Unit)
      } catch (e: SerializationException) {
          Timber.e(e, "Import failed: invalid JSON format")
          Result.failure(ImportException("Invalid JSON format", e))
      } catch (e: Exception) {
          Timber.e(e, "Import failed: unexpected error")
          Result.failure(e)
      }
  }
  ```

**Subtask 3.4.3:** Combine redundant Flows in ViewModels (MED-011)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/home/HomeViewModel.kt`
- **Lines:** 38-78
- **Action:** Use `Flow.combine`:
  ```kotlin
  val homeState = combine(
      repository.groups,
      repository.activeGroup,
      _xposedConnectionState
  ) { groups, activeGroup, connectionState ->
      HomeState(groups, activeGroup, connectionState, /* derived counts */)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())
  ```

---

### Phase 3 Checklist

- [ ] CRIT-009: SavedStateHandle injected in all 5 ViewModels
- [ ] Critical UI state persisted across process death
- [ ] HIGH-003: All State classes marked @Immutable with ImmutableList
- [ ] MED-005: Inline lambdas hoisted with remember
- [ ] HIGH-001: Full-screen overlays replaced with inline indicators
- [ ] HIGH-006: Pager/tab sync uses snapshotFlow + distinctUntilChanged
- [ ] HIGH-014: GroupSpoofingScreen restores state, navigates back on null group
- [ ] HIGH-005: IME insets handled with imePadding + adjustResize
- [ ] MED-012: contentDescription added to all critical icons
- [ ] MED-013: SimpleDateFormat cached with remember
- [ ] LOW-002: Dialog visibility uses rememberSaveable
- [ ] LOW-003: Loading state uses AnimatedVisibility not alpha
- [ ] LOW-006: contentType added to LazyColumns
- [ ] LOW-005: SettingsScreen export mode synced with LaunchedEffect
- [ ] MED-009: Redundant suspend modifiers removed
- [ ] MED-010: importGroups returns Result with typed errors
- [ ] MED-011: Redundant Flows combined
- [ ] `./gradlew.bat :app:testDebugUnitTest lint assembleDebug --no-daemon` passes

---

## Phase 4: Motion & Component Token Alignment

**Goal:** Align custom motion specs to M3E tokens; fix accessibility and performance in custom components.

**Estimated Effort:** 6-8 hours  
**Risk:** Low — numeric and modifier changes  
**Validation:** Animations feel natural; touch targets ≥ 48dp; reduced motion works

---

### Task 4.1: Motion Token Refactor

**Subtask 4.1.1:** Refactor AppMotion to M3E token hierarchy
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/theme/Motion.kt`
- **Action:** Replace flat namespace with structured tokens:
  ```kotlin
  object MotionTokens {
      object Expressive {
          val spatialDefault = spring<Float>(dampingRatio = 0.9f, stiffness = 700f)
          val spatialFast = spring<Float>(dampingRatio = 0.9f, stiffness = 1400f)
          val spatialSlow = spring<Float>(dampingRatio = 0.9f, stiffness = 300f)
          val effectsDefault = spring<Float>(dampingRatio = 1.0f, stiffness = 1600f)
          val effectsFast = spring<Float>(dampingRatio = 1.0f, stiffness = 3800f)
          val effectsSlow = spring<Float>(dampingRatio = 1.0f, stiffness = 800f)
      }
      object Standard {
          val spatialDefault = spring<Float>(dampingRatio = 1.0f, stiffness = 380f)
          val spatialFast = spring<Float>(dampingRatio = 1.0f, stiffness = 380f)
          val spatialSlow = spring<Float>(dampingRatio = 1.0f, stiffness = 380f)
          val effectsDefault = spring<Float>(dampingRatio = 1.0f, stiffness = 1600f)
          val effectsFast = spring<Float>(dampingRatio = 1.0f, stiffness = 3800f)
          val effectsSlow = spring<Float>(dampingRatio = 1.0f, stiffness = 800f)
      }
  }
  
  object AppMotion {
      @Composable
      fun <T> spatial(spec: SpringSpec<T>, reduced: SpringSpec<T>): FiniteAnimationSpec<T> =
          if (policy().reduceMotion) reduced else spec
      
      object Spatial {
          val Expressive = MotionTokens.Expressive.spatialDefault
          val ExpressiveFast = MotionTokens.Expressive.spatialFast
          val Standard = MotionTokens.Standard.spatialDefault
      }
      object Effect {
          val Color = MotionTokens.Expressive.effectsDefault
          val Alpha = MotionTokens.Expressive.effectsFast
      }
  }
  ```
- **Reference:** M3E motion physics — `.agents/skills/material-3-expressive/references/m3-motion-physics.md`

**Subtask 4.1.2:** Add elevation level constants
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/theme/Theme.kt` (or new file)
- **Action:**
  ```kotlin
  object ElevationTokens {
      val Level0 = 0.dp
      val Level1 = 1.dp
      val Level2 = 3.dp
      val Level3 = 6.dp
      val Level4 = 8.dp
      val Level5 = 12.dp
  }
  ```

---

### Task 4.2: Component Accessibility & Performance

**Subtask 4.2.1:** Fix ExpressiveIconButton touch target (HIGH-002)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/expressive/ExpressiveIconButton.kt`
- **Lines:** 63-64
- **Action:** Increase default `buttonSize` to 48.dp or add `minimumInteractiveComponentSize()` modifier

**Subtask 4.2.2:** Fix CompactExpressiveIconButton touch target (HIGH-002)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/expressive/CompactExpressiveIconButton.kt`
- **Lines:** 106-117
- **Action:** Add `minimumInteractiveComponentSize()` modifier to ensure 48dp touch target even with 36dp visual size

**Subtask 4.2.3:** Fix ToggleButton accessibility (HIGH-007)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/ToggleButton.kt`
- **Lines:** 46-115
- **Action:** Add semantics:
  ```kotlin
  Modifier
      .toggleable(
          value = isChecked,
          onValueChange = onCheckedChange,
          role = Role.Switch
      )
      .semantics {
          stateDescription = if (isChecked) "On" else "Off"
      }
  ```

**Subtask 4.2.4:** Replace scale() with graphicsLayer (MED-003)
- **Files:**
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/expressive/ExpressiveCard.kt` (line 87)
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/expressive/ExpressiveIconButton.kt` (line 78)
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/ToggleButton.kt` (line 110)
- **Action:** Replace `Modifier.scale(animatedScale)` with:
  ```kotlin
  Modifier.graphicsLayer {
      scaleX = animatedScale
      scaleY = animatedScale
  }
  ```

**Subtask 4.2.5:** Update ExpressiveSwitch spring spec
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/expressive/ExpressiveSwitch.kt`
- **Action:** Update thumb animation spring to `MotionTokens.Expressive.spatialFast` (damping 0.9, stiffness 1400)

**Subtask 4.2.6:** Update ExpressiveCard spring spec
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/expressive/ExpressiveCard.kt`
- **Action:** Update press-scale animation to `MotionTokens.Expressive.spatialFast`

**Subtask 4.2.7:** Update ExpressiveIconButton spring spec
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/expressive/ExpressiveIconButton.kt`
- **Action:** Update press-scale animation to `MotionTokens.Expressive.spatialFast`

---

### Task 4.3: Elevation Compliance

**Subtask 4.3.1:** Use surfaceColorAtElevation where appropriate
- **Files:** Various component files using hardcoded dark grays
- **Action:** Replace hardcoded `Color(0xFF1E1E1E)` etc. with:
  ```kotlin
  MaterialTheme.colorScheme.surfaceColorAtElevation(ElevationTokens.Level2)
  ```
- **Note:** Only apply in non-AMOLED branches; AMOLED documented as elevation-opt-out

**Subtask 4.3.2:** Use elevation level tokens
- **Files:** Components using raw `tonalElevation = 2.dp`
- **Action:** Replace with `ElevationTokens.Level2` (3.dp per M3E spec)

---

### Phase 4 Checklist

- [ ] AppMotion refactored to M3E token hierarchy (Expressive/Standard × spatial/effects × default/fast/slow)
- [ ] ElevationTokens defined (Level 0-5)
- [ ] HIGH-002: ExpressiveIconButton default size ≥ 48dp
- [ ] HIGH-002: CompactExpressiveIconButton uses minimumInteractiveComponentSize()
- [ ] HIGH-007: ToggleButton has full accessibility semantics
- [ ] MED-003: All scale() modifiers replaced with graphicsLayer
- [ ] ExpressiveSwitch uses fast spatial spring (damping 0.9, stiffness 1400)
- [ ] ExpressiveCard uses fast spatial spring
- [ ] ExpressiveIconButton uses fast spatial spring
- [ ] surfaceColorAtElevation() used instead of hardcoded dark grays
- [ ] Raw tonalElevation dp values replaced with ElevationTokens
- [ ] Reduced motion fallback still functional
- [ ] `./gradlew.bat :app:testDebugUnitTest lint assembleDebug --no-daemon` passes

---

## Phase 5: Dependency Upgrade & M3E Component Migration

**Goal:** Upgrade to material3 1.5.0-alpha18 and migrate custom components to M3E built-ins.

**Estimated Effort:** 10-14 hours  
**Risk:** Medium — alpha dependency, experimental APIs  
**Validation:** Build passes; all modified components tested across themes; LSPosed smoke test

---

### Task 5.1: Dependency Upgrade

**Subtask 5.1.1:** Update version catalog
- **File:** `gradle/libs.versions.toml`
- **Lines:** 15 (material3), 14 (composeBom)
- **Action:**
  ```toml
  composeBom = "2026.04.01"
  material3 = "1.5.0-alpha18"
  ```
- **Note:** Requires user confirmation per project rules

**Subtask 5.1.2:** Update build files if needed
- **File:** `app/build.gradle.kts`
- **Action:** Verify no breaking changes in BOM 2026.04.01; update any deprecated API calls

**Subtask 5.1.3:** Fix breaking changes from 1.4.0 → 1.5.0-alpha18
- **Files:** Any using affected APIs
- **Changes needed:**
  - `Scrim()` → `LevitatedPaneScrim()`
  - `rememberWithGapSearchBarState()` → `rememberSearchBarWithGapState()`
  - PullToRefreshDefaults: `shape` → `indicatorShape`, `containerColor` → `indicatorContainerColor`

---

### Task 5.2: Component Migration

**Subtask 5.2.1:** Migrate ExpressiveLoadingIndicator → LoadingIndicator
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/expressive/ExpressiveLoadingIndicator.kt`
- **Action:**
  1. Replace `CircularProgressIndicator` with `LoadingIndicator`
  2. Use `LoadingIndicator(contained = true)` inside surfaces
  3. Use `LoadingIndicator(contained = false)` for inline loading
  4. Default size: 48dp
  5. Add `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` if needed
- **Reference:** M3E loading indicator specs — `.agents/skills/material-3-expressive/references/m3-loading-indicator-specs-tokens.md`

**Subtask 5.2.2:** Update ExpressivePullToRefresh indicator reference
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/expressive/ExpressivePullToRefresh.kt`
- **Action:** Replace inner `ExpressiveLoadingIndicator` with `LoadingIndicator`

**Subtask 5.2.3:** Migrate QuickActionGroup → ButtonGroup
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/expressive/QuickActionGroup.kt`
- **Action:**
  1. Replace custom `Row` with `ButtonGroup`
  2. Use `ButtonGroupScope` for items
  3. Configure `overflowIndicator` for collapsed items
  4. Spacing: 8dp (M3E default)
  5. Add `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`
- **Reference:** M3E button group specs — `.agents/skills/material-3-expressive/references/m3-button-groups-specs-tokens.md`

**Subtask 5.2.4:** Deprecate and remove ToggleButton
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/components/ToggleButton.kt`
- **Action:**
  1. Mark `@Deprecated("Use ExpressiveSwitch or standard Switch")`
  2. Find all callers via IDE usage search
  3. Migrate callers to `ExpressiveSwitch` or `Switch`
  4. Delete file after all callers migrated

---

### Task 5.3: Theme Integration

**Subtask 5.3.1:** Adopt MotionScheme in theme
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/theme/Theme.kt`
- **Action:** If `MotionScheme` is stable in 1.5.0-alpha18:
  ```kotlin
  MaterialTheme(
      colorScheme = colorScheme,
      typography = typography,
      shapes = shapes,
      // motionScheme = MotionScheme.expressive(), // if available
  ) { content }
  ```
- **Note:** Only if stable; if experimental, skip or wrap behind wrapper

**Subtask 5.3.2:** Test all themes after upgrade
- **Scenarios:**
  - [ ] Light theme renders correctly
  - [ ] Dark theme renders correctly
  - [ ] AMOLED theme renders correctly
  - [ ] Dynamic color works on Android 12+
  - [ ] No color contrast regressions

---

### Phase 5 Checklist

- [ ] User confirmed dependency upgrade
- [ ] `gradle/libs.versions.toml` updated: material3=1.5.0-alpha18, BOM=2026.04.01
- [ ] Breaking changes fixed: Scrim→LevitatedPaneScrim, PullToRefreshDefaults renames
- [ ] ExpressiveLoadingIndicator migrated to LoadingIndicator
- [ ] ExpressivePullToRefresh uses LoadingIndicator internally
- [ ] QuickActionGroup migrated to ButtonGroup
- [ ] ToggleButton deprecated, callers migrated, file removed
- [ ] @OptIn(ExperimentalMaterial3ExpressiveApi) added where needed
- [ ] MotionScheme adopted if stable
- [ ] All themes render correctly
- [ ] `./gradlew.bat spotlessCheck :app:testDebugUnitTest lint test assembleDebug --no-daemon` passes
- [ ] LSPosed smoke test on `com.mantle.verify` passes

---

## Phase 6: Navigation Modernization

**Goal:** Migrate from string-based Navigation 2.x to type-safe Navigation 2.8+ or Navigation3.

**Estimated Effort:** 10-14 hours  
**Risk:** Medium — affects entire navigation graph  
**Validation:** All screens reachable; deep links work; back stack behaves correctly

---

### Task 6.1: Type-Safe Route Definitions

**Subtask 6.1.1:** Define @Serializable route types
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/navigation/NavDestination.kt`
- **Action:** Replace string constants:
  ```kotlin
  @Serializable
  data object Home
  
  @Serializable
  data object Groups
  
  @Serializable
  data class GroupSpoofing(val groupId: String)
  
  @Serializable
  data object Settings
  
  @Serializable
  data object Diagnostics
  ```

**Subtask 6.1.2:** Update NavHost
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/MainActivity.kt`
- **Lines:** 290-302
- **Action:** Use type-safe navigation:
  ```kotlin
  NavHost(navController = navController, startDestination = Home) {
      composable<Home> { /* ... */ }
      composable<Groups> { /* ... */ }
      composable<GroupSpoofing> { backStackEntry ->
          val route = backStackEntry.toRoute<GroupSpoofing>()
          GroupSpoofingScreen(groupId = route.groupId)
      }
  }
  ```

**Subtask 6.1.3:** Fix NavController default parameter (LOW-004)
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/MainActivity.kt`
- **Line:** 151
- **Action:** Hoist `val navController = rememberNavController()` in `setContent` block, pass explicitly

---

### Task 6.2: Navigation3 Evaluation (Future)

**Subtask 6.2.1:** Evaluate Navigation3 migration
- **File:** Research Navigation3 APIs
- **Action:** Document decision:
  - Navigation3 provides true multiple back stacks and `NavDisplay`
  - Migration is larger effort; can be Phase 6.5 after type-safe routes are working
  - Keep Navigation 2.8+ type-safe as immediate target
- **Reference:** `navigation-3` skill — `.agents/skills/navigation-3/SKILL.md`

---

### Phase 6 Checklist

- [ ] HIGH-004: @Serializable route types defined
- [ ] NavHost uses type-safe composable<> destinations
- [ ] Manual navArgument extraction removed
- [ ] LOW-004: NavController hoisted, not default parameter
- [ ] Bottom nav state preserved across configuration changes
- [ ] Deep links still functional
- [ ] Back button behavior unchanged
- [ ] `./gradlew.bat :app:testDebugUnitTest lint assembleDebug --no-daemon` passes

---

## Phase 7: Build Hardening & Optimization

**Goal:** Fix build configuration issues, add CI ProGuard validation, secure manifest.

**Estimated Effort:** 6-8 hours  
**Risk:** Low — build file changes  
**Validation:** Build passes; ProGuard validation job runs; APK size tracked

---

### Task 7.1: ProGuard CI Validation

**Subtask 7.1.1:** Add CI ProGuard build type (CRIT-004)
- **File:** `app/build.gradle.kts`
- **Lines:** 59 area
- **Action:** Add `proguardValidation` build type:
  ```kotlin
  buildTypes {
      release {
          isMinifyEnabled = false
          isShrinkResources = false
      }
      create("proguardValidation") {
          initWith(getByName("release"))
          isMinifyEnabled = true
          isShrinkResources = true
          signingConfig = signingConfigs.getByName("debug") // or null
      }
  }
  ```
- **CI Command:** `./gradlew.bat :app:assembleProguardValidation --no-daemon`

---

### Task 7.2: Manifest Security

**Subtask 7.2.1:** Add windowSoftInputMode (HIGH-005)
- **File:** `app/src/main/AndroidManifest.xml`
- **Action:** Add to MainActivity:
  ```xml
  <activity
      android:name=".MainActivity"
      android:windowSoftInputMode="adjustResize"
      ... />
  ```

**Subtask 7.2.2:** Verify allowBackup=false (HIGH-008)
- **File:** `app/src/main/AndroidManifest.xml`
- **Already done in Phase 0; verify here**

---

### Task 7.3: Build Cleanup

**Subtask 7.3.1:** Remove redundant daemon property (LOW-007)
- **File:** `gradle.properties`
- **Line:** 24
- **Action:** Remove `org.gradle.daemon=true`

**Subtask 7.3.2:** Move IDE-specific logic (LOW-008)
- **File:** `build.gradle.kts`
- **Lines:** 62-68
- **Action:** Move `idea { module { excludeDirs... } }` to `.idea/` or remove

**Subtask 7.3.3:** Add build-logic/convention plugins (LOW-009)
- **Action:** Create `build-logic/` module with convention plugins for:
  - `compileSdk`, `minSdk`, `jvmToolchain(17)`, `JavaVersion.VERSION_17`
  - Lint config
- **Reference:** Gradle convention plugins — [docs.gradle.org](https://docs.gradle.org/current/samples/sample_convention_plugins.html)

**Subtask 7.3.4:** Move Spotless ktfmt to version catalog (LOW-010)
- **File:** `build.gradle.kts`
- **Line:** 35
- **Action:** Add `ktfmt = "0.54"` to `gradle/libs.versions.toml`, reference in build script

**Subtask 7.3.5:** Add Compose compiler metrics
- **File:** `app/build.gradle.kts`
- **Action:** Add to `composeOptions` or `buildFeatures`:
  ```kotlin
  composeCompiler {
      reportsDestination = layout.buildDirectory.dir("compose_compiler")
      metricsDestination = layout.buildDirectory.dir("compose_compiler")
  }
  ```
- **Reference:** Compose compiler metrics — [developer.android.com](https://developer.android.com/develop/ui/compose/performance/stability/diagnose)

---

### Phase 7 Checklist

- [ ] CRIT-004: ProGuard validation build type added
- [ ] CI can run `:app:assembleProguardValidation` successfully
- [ ] HIGH-005: windowSoftInputMode="adjustResize" in manifest
- [ ] LOW-007: Redundant daemon property removed
- [ ] LOW-008: IDE-specific build logic moved/removed
- [ ] LOW-009: Build-logic convention plugins created (or documented as future work)
- [ ] LOW-010: Spotless ktfmt version in catalog
- [ ] Compose compiler metrics enabled
- [ ] `./gradlew.bat spotlessCheck :app:testDebugUnitTest lint test assembleDebug assembleProguardValidation --no-daemon` passes

---

## Phase 8: Polish & Advanced M3E Features

**Goal:** Leverage advanced M3E components and patterns; add previews; window size adaptation.

**Estimated Effort:** 8-12 hours  
**Risk:** Low — additive features  
**Validation:** New features functional; no regressions

---

### Task 8.1: Evaluate Advanced M3E Components

**Subtask 8.1.1:** Evaluate SplitButtonLayout
- **Potential use case:** Export button with dropdown (Basic/Full Debug/Root Maximum)
- **File to modify:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/settings/SettingsScreen.kt`
- **Action:** Prototype replacing export button + dropdown with `SplitButtonLayout`
- **Reference:** M3E split button specs — `.agents/skills/material-3-expressive/references/m3-split-button-specs-tokens.md`

**Subtask 8.1.2:** Evaluate FloatingActionButtonMenu
- **Potential use case:** Main screen quick actions (regenerate all, export, settings)
- **File to modify:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/home/HomeScreen.kt`
- **Action:** Prototype adding FAB menu with 3-4 actions
- **Reference:** M3E FAB menu specs — `.agents/skills/material-3-expressive/references/m3-fab-menu-specs-tokens.md`

**Subtask 8.1.3:** Evaluate HorizontalFloatingToolbar
- **Potential use case:** Rich editing in group spoofing (add app, regenerate, copy, paste)
- **File to modify:** `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/groupspoofing/GroupSpoofingScreen.kt`
- **Action:** Prototype docked toolbar above keyboard or floating toolbar near selected items
- **Reference:** M3E toolbar specs — `.agents/skills/material-3-expressive/references/m3-toolbars-specs-tokens.md`

**Subtask 8.1.4:** Evaluate MaterialShapes for decorative moments
- **Potential use case:** Hero graphic shapes, avatar masks, export success illustration
- **Action:** Add `MaterialShapes.Circle`, `MaterialShapes.Burst`, etc. to decorative composables
- **Reference:** M3E shape library — `.agents/skills/material-3-expressive/references/m3-shape.md`

---

### Task 8.2: Developer Experience

**Subtask 8.2.1:** Add @Previews (LOW-001)
- **Files:**
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/groupspoofing/tabs/SpoofTabContent.kt`
  - `app/src/main/kotlin/com/astrixforge/devicemasker/ui/screens/groupspoofing/tabs/AppsTabContent.kt`
  - Other tab content files
- **Action:** Add `@Preview` for key states (empty, populated, loading)

---

### Task 8.3: Window Size Class Adaptation

**Subtask 8.3.1:** Add window size class awareness
- **File:** `app/src/main/kotlin/com/astrixforge/devicemasker/MainActivity.kt`
- **Action:** Use `calculateWindowSizeClass()` to adapt layouts:
  ```kotlin
  val windowSizeClass = calculateWindowSizeClass(activity = this)
  // Compact: bottom nav
  // Medium: navigation rail
  // Expanded: navigation rail + detail pane
  ```
- **Reference:** Window size classes — [developer.android.com](https://developer.android.com/develop/ui/compose/layouts/adaptive)

---

### Phase 8 Checklist

- [ ] SplitButtonLayout prototyped for export actions
- [ ] FloatingActionButtonMenu prototyped for home quick actions
- [ ] HorizontalFloatingToolbar prototyped for group editing
- [ ] MaterialShapes used in at least 1 decorative moment
- [ ] LOW-001: @Preview added to tab content components
- [ ] Window size class adaptation implemented
- [ ] No regressions in Compact layout
- [ ] `./gradlew.bat :app:testDebugUnitTest lint assembleDebug --no-daemon` passes

---

## Phase 9: Final Validation & Testing

**Goal:** Run complete validation suite; verify all success criteria.

**Estimated Effort:** 4-6 hours  
**Risk:** Low — validation only  
**Validation:** All success criteria met

---

### Task 9.1: Comprehensive Testing

**Subtask 9.1.1:** Run full gate
```powershell
.\gradlew.bat spotlessApply spotlessCheck :common:testDebugUnitTest :app:testDebugUnitTest :xposed:testDebugUnitTest lint test assembleDebug assembleRelease --no-daemon
```

**Subtask 9.1.2:** LSPosed smoke test
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am force-stop com.mantle.verify
adb logcat -c
adb shell monkey -p com.mantle.verify -c android.intent.category.LAUNCHER 1
adb shell pidof com.mantle.verify
adb logcat -d -t 1200 | Select-String "XposedEntry|All hooks|Spoof event|FATAL EXCEPTION|PatternSyntaxException|Cannot hook abstract|AbstractMethodError|WorkManagerInitializer"
```

**Subtask 9.1.3:** Visual regression testing
- [ ] Light theme: all screens
- [ ] Dark theme: all screens
- [ ] AMOLED theme: all screens
- [ ] Dynamic color: Android 12+ device
- [ ] Reduced motion: system animation scale = 0
- [ ] Large font: font scale 1.5x
- [ ] High contrast: API 34+ contrast level

**Subtask 9.1.4:** Accessibility audit
- [ ] Touch targets ≥ 48dp everywhere
- [ ] Color contrast ≥ 4.5:1 for text
- [ ] Color contrast ≥ 3:1 for non-text
- [ ] Screen reader navigates all interactive elements
- [ ] Content descriptions on all critical icons

**Subtask 9.1.5:** Performance check
- [ ] No ANRs in 10-minute usage
- [ ] No dropped frames during animations
- [ ] Compose compiler metrics reviewed for recomposition hotspots
- [ ] APK size tracked (compare to pre-migration)

---

### Phase 9 Checklist

- [ ] Full gate passes: BUILD SUCCESSFUL
- [ ] LSPosed smoke test: XposedEntry loaded, hooks registered, spoof events emitted
- [ ] No crash signatures: FATAL EXCEPTION, PatternSyntaxException, Cannot hook abstract, AbstractMethodError, WorkManagerInitializer
- [ ] Visual regression: light/dark/AMOLED all correct
- [ ] Accessibility: all touch targets, contrast, screen reader verified
- [ ] Performance: no ANRs, no janky animations
- [ ] M3E compliance score ≥ 75/100
- [ ] All CRITICAL and HIGH findings resolved
- [ ] User acceptance: app feels stable and polished

---

## 13. Testing Roadmap

### Already Covered in Phase 1
See Phase 1 tasks for detailed ViewModel, Repository, Config, and Service tests.

### Additional Integration Tests (Phase 9)

| Component | Test Scenario | Phase |
|-----------|--------------|-------|
| Theme switching | Light → Dark → AMOLED at runtime | 9 |
| Process death | Kill app, relaunch, verify state restored | 9 |
| Deep linking | Navigate to GroupSpoofing from external intent | 9 |
| Export flow | Basic → Full Debug → Root Maximum | 9 |
| Import flow | Valid JSON, malformed JSON, large JSON | 9 |
| Config corruption | Simulate corrupt config.json, verify recovery | 9 |
| Concurrent config | Rapid toggle 50x, verify no lost updates | 9 |
| Root capture | Startup capture creates manifest and artifacts | 9 |
| Boot receiver | Verify BootCaptureReceiver registered | 9 |

### UI Instrumented Tests (Future)

| Screen | Key Scenarios |
|--------|--------------|
| HomeScreen | Group selection, module toggle, regeneration, search |
| GroupsScreen | Create group, delete group, set default, import/export |
| GroupSpoofingScreen | Tab switch, regenerate value, assign app, navigate back on deletion |
| SettingsScreen | Theme change, export logs, root grant dialog |
| DiagnosticsScreen | Service connection, refresh, anti-detection tests |

---

## 14. File Modification Index

### Modified Files (Alphabetical)

| File | Phases | Key Changes |
|------|--------|-------------|
| `app/build.gradle.kts` | 1, 5, 7 | Test deps, material3 version, ProGuard validation build type, Compose metrics |
| `app/src/main/AndroidManifest.xml` | 0, 7 | allowBackup=false, windowSoftInputMode, XposedProvider docs |
| `app/src/main/kotlin/.../MainActivity.kt` | 3, 6 | IME padding, type-safe NavHost, NavController hoisting |
| `app/src/main/kotlin/.../data/ConfigSync.kt` | 0 | Suspend IO variants, KDoc |
| `app/src/main/kotlin/.../data/XposedPrefs.kt` | 1 | Tests |
| `app/src/main/kotlin/.../data/repository/AppScopeRepository.kt` | 0 | AtomicBoolean cache |
| `app/src/main/kotlin/.../data/repository/SpoofRepository.kt` | 0, 3 | AtomicReference caches, Result types, suspend cleanup |
| `app/src/main/kotlin/.../service/AppLogStore.kt` | 0 | Channel-based async logging |
| `app/src/main/kotlin/.../service/ConfigManager.kt` | 0 | Atomic updates, Mutex, corrupted backup |
| `app/src/main/kotlin/.../service/LogManager.kt` | 0 | Dispatchers.IO wrapper |
| `app/src/main/kotlin/.../service/ServiceClient.kt` | 1 | Tests |
| `app/src/main/kotlin/.../service/diagnostics/JsonlDiagnosticStore.kt` | 0 | Synchronized reads |
| `app/src/main/kotlin/.../service/diagnostics/RootLogCollector.kt` | 0 | Shell escaping, grep pattern fix |
| `app/src/main/kotlin/.../service/diagnostics/RootShell.kt` | 0 | Timeout fix, suspend IO |
| `app/src/main/kotlin/.../service/diagnostics/SupportBundleBuilder.kt` | 0 | Streaming ZIP |
| `app/src/main/kotlin/.../ui/components/AppListItem.kt` | 3 | contentDescription |
| `app/src/main/kotlin/.../ui/components/GroupCard.kt` | 3 | Cached SimpleDateFormat |
| `app/src/main/kotlin/.../ui/components/ToggleButton.kt` | 4, 5 | Accessibility, deprecation, removal |
| `app/src/main/kotlin/.../ui/components/expressive/CompactExpressiveIconButton.kt` | 4 | Touch target |
| `app/src/main/kotlin/.../ui/components/expressive/ExpressiveCard.kt` | 4 | graphicsLayer, spring spec |
| `app/src/main/kotlin/.../ui/components/expressive/ExpressiveIconButton.kt` | 4 | Touch target, graphicsLayer, spring |
| `app/src/main/kotlin/.../ui/components/expressive/ExpressiveLoadingIndicator.kt` | 5 | Migrated to LoadingIndicator |
| `app/src/main/kotlin/.../ui/components/expressive/ExpressivePullToRefresh.kt` | 5 | Indicator reference |
| `app/src/main/kotlin/.../ui/components/expressive/ExpressiveSwitch.kt` | 4 | Spring spec |
| `app/src/main/kotlin/.../ui/components/expressive/QuickActionGroup.kt` | 5 | Migrated to ButtonGroup |
| `app/src/main/kotlin/.../ui/navigation/NavDestination.kt` | 6 | @Serializable routes |
| `app/src/main/kotlin/.../ui/screens/diagnostics/DiagnosticsScreen.kt` | 2, 3 | Semantic status colors, contentType, imePadding |
| `app/src/main/kotlin/.../ui/screens/diagnostics/DiagnosticsState.kt` | 3 | @Immutable |
| `app/src/main/kotlin/.../ui/screens/diagnostics/DiagnosticsViewModel.kt` | 1, 3 | SavedStateHandle, tests |
| `app/src/main/kotlin/.../ui/screens/groups/GroupsScreen.kt` | 3 | Cached SimpleDateFormat, stable lambdas |
| `app/src/main/kotlin/.../ui/screens/groups/GroupsState.kt` | 3 | @Immutable |
| `app/src/main/kotlin/.../ui/screens/groups/GroupsViewModel.kt` | 1, 3 | SavedStateHandle, tests |
| `app/src/main/kotlin/.../ui/screens/groupspoofing/GroupSpoofingScreen.kt` | 3 | Pager sync, state restoration, deletion handling, imePadding |
| `app/src/main/kotlin/.../ui/screens/groupspoofing/GroupSpoofingState.kt` | 3 | @Immutable |
| `app/src/main/kotlin/.../ui/screens/groupspoofing/GroupSpoofingViewModel.kt` | 1, 3 | SavedStateHandle, tests |
| `app/src/main/kotlin/.../ui/screens/groupspoofing/model/UIDisplayCategory.kt` | 2 | Semantic theme colors |
| `app/src/main/kotlin/.../ui/screens/home/HomeScreen.kt` | 2, 3 | Semantic status colors, inline loading, rememberSaveable, stable lambdas |
| `app/src/main/kotlin/.../ui/screens/home/HomeState.kt` | 3 | @Immutable |
| `app/src/main/kotlin/.../ui/screens/home/HomeViewModel.kt` | 1, 3 | SavedStateHandle, combined Flows, tests |
| `app/src/main/kotlin/.../ui/screens/settings/SettingsScreen.kt` | 3 | Export mode sync, SplitButtonLayout prototype |
| `app/src/main/kotlin/.../ui/screens/settings/SettingsState.kt` | 3 | @Immutable |
| `app/src/main/kotlin/.../ui/screens/settings/SettingsViewModel.kt` | 1, 3 | SavedStateHandle, tests |
| `app/src/main/kotlin/.../ui/theme/Color.kt` | 2 | Named constants for all hardcoded colors |
| `app/src/main/kotlin/.../ui/theme/Motion.kt` | 4 | M3E token hierarchy |
| `app/src/main/kotlin/.../ui/theme/Shapes.kt` | 2 | 10-step scale + asymmetric variants |
| `app/src/main/kotlin/.../ui/theme/Theme.kt` | 2, 4 | Surface containers, AMOLED docs, contrast support, ElevationTokens |
| `app/src/main/kotlin/.../ui/theme/Typography.kt` | 2 | 15 emphasized styles, composition local |
| `build.gradle.kts` | 7 | IDE logic removal, ktfmt from catalog |
| `gradle.properties` | 7 | Daemon removal |
| `gradle/libs.versions.toml` | 1, 5 | Turbine, MockK, material3 1.5.0-alpha18, BOM 2026.04.01 |

### New Test Files

| File | Phase | Purpose |
|------|-------|---------|
| `app/src/test/kotlin/.../MainDispatcherRule.kt` | 1 | Test coroutine dispatcher |
| `app/src/test/kotlin/.../ui/screens/home/HomeViewModelTest.kt` | 1 | HomeViewModel tests |
| `app/src/test/kotlin/.../ui/screens/groups/GroupsViewModelTest.kt` | 1 | GroupsViewModel tests |
| `app/src/test/kotlin/.../ui/screens/groupspoofing/GroupSpoofingViewModelTest.kt` | 1 | GroupSpoofingViewModel tests |
| `app/src/test/kotlin/.../ui/screens/settings/SettingsViewModelTest.kt` | 1 | SettingsViewModel tests |
| `app/src/test/kotlin/.../ui/screens/diagnostics/DiagnosticsViewModelTest.kt` | 1 | DiagnosticsViewModel tests |
| `app/src/test/kotlin/.../data/repository/SpoofRepositoryTest.kt` | 1 | SpoofRepository tests |
| `app/src/test/kotlin/.../data/repository/AppScopeRepositoryTest.kt` | 1 | AppScopeRepository tests |
| `app/src/test/kotlin/.../service/ConfigManagerTest.kt` | 1 | ConfigManager tests |
| `app/src/test/kotlin/.../data/ConfigSyncTest.kt` | 1 | ConfigSync tests |
| `app/src/test/kotlin/.../data/XposedPrefsTest.kt` | 1 | XposedPrefs tests |

---

## 15. Dependency Upgrade Matrix

| Dependency | Current | Phase 2 Target | Phase 5 Target | Notes |
|------------|---------|----------------|----------------|-------|
| AGP | 9.2.0 | 9.2.0 | Verify latest stable | Keep unless issues |
| Kotlin | 2.3.0 | 2.3.0 | Verify latest | Compose compiler integrated |
| Compose BOM | 2026.02.01 | 2026.02.01 | **2026.04.01** | Required for M3E |
| material3 | 1.4.0 | 1.4.0 | **1.5.0-alpha18** | M3E components |
| Navigation Compose | 2.9.7 | 2.9.7 | 2.9.7+ | Type-safe routes in 2.8+ |
| DataStore | 1.2.0 | 1.2.0 | Verify latest | Check for fixes |
| Coroutines | 1.10.2 | 1.10.2 | Verify latest | Turbine compatibility |
| Coil | 3.4.0 | 3.4.0 | Verify latest | Active 3.x branch |
| Timber | 5.0.1 | 5.0.1 | 5.0.1 | No newer stable |
| Spotless | 8.3.0 | 8.3.0 | Verify latest | ktfmt compatibility |
| libxposed api | 101.0.1 | 101.0.1 | 101.0.1 | KEEP AS-IS |
| libxposed service | 101.0.0 | 101.0.0 | 101.0.1 | Align patch with api |
| hiddenapibypass | 6.1 | 6.1 | Verify latest | Check for updates |
| **NEW: Turbine** | — | 1.2.0 | 1.2.0 | Flow testing |
| **NEW: MockK** | — | 1.13.12 | 1.13.12 | Mocking |

---

## 16. Risk Assessment

| Risk | Likelihood | Impact | Phase | Mitigation |
|------|------------|--------|-------|------------|
| ConfigManager concurrent fix introduces deadlocks | Low | Critical | 0 | Extensive unit tests; Mutex timeout; gradual rollout |
| Async logging loses events on crash | Medium | Medium | 0 | Channel with `Channel.CONFLATED` fallback; flush on app background |
| SavedStateHandle increases bundle size | Low | Low | 3 | Only persist primitive/simple state; size limit 1MB |
| material3 1.5.0-alpha18 APIs change | High | Medium | 5 | Pin exact version; isolate behind wrappers |
| ButtonGroup overflow doesn't fit use cases | Medium | Medium | 5 | Keep QuickActionGroup as fallback implementation |
| Type-safe navigation breaks deep links | Medium | High | 6 | Test all deep link patterns; keep Navigation2 fallback |
| ProGuard validation build fails | Medium | High | 7 | Add incrementally; fix rules before enabling in release |
| AMOLED users reject tonal surfaces | Low | Medium | 2 | Documented deviation; never force on AMOLED users |
| Spring spec changes feel wrong | Medium | Low | 4 | A/B test on device; keep old specs as fallback |
| Build time increases with convention plugins | Low | Low | 7 | Measure; revert if >10% increase |

---

## 17. Success Criteria

### Functional
- [ ] All 10 CRITICAL findings resolved
- [ ] All 15 HIGH findings resolved
- [ ] All 15 MEDIUM findings resolved
- [ ] All 10 LOW findings resolved
- [ ] No ANRs in 10-minute usage test
- [ ] No crashes in LSPosed smoke test
- [ ] Process death survival: all critical UI state restored

### M3E Compliance
- [ ] Theme compliance score ≥ 75/100 (from 38/100)
- [ ] All hardcoded colors removed from ColorScheme constructors
- [ ] Complete surface container roles in all themes
- [ ] 15 emphasized typography styles available
- [ ] 10-step shape scale + asymmetric variants defined
- [ ] Motion tokens match M3E spec values
- [ ] `LoadingIndicator` replaces custom implementation
- [ ] `ButtonGroup` replaces custom implementation
- [ ] Touch targets ≥ 48dp everywhere

### Quality
- [ ] ViewModel test coverage ≥ 60%
- [ ] Repository test coverage ≥ 60%
- [ ] Build passes full gate every time
- [ ] Spotless formatting passes
- [ ] Lint passes with zero errors
- [ ] Compose compiler metrics reviewed

### User Experience
- [ ] No visual regressions in light/dark/AMOLED
- [ ] Reduced motion fallback functional
- [ ] Accessibility: contrast, touch targets, screen reader
- [ ] IME doesn't obscure text fields
- [ ] Loading states preserve layout context (no full-screen overlays)

---

## 18. Appendices

### Appendix A: Reference Files

| Reference | Path |
|-----------|------|
| M3E Skill | `.agents/skills/material-3-expressive/SKILL.md` |
| M3E Token Index | `.agents/skills/material-3-expressive/references/m3-expressive-specs-tokens-index.md` |
| M3E Components | `.agents/skills/material-3-expressive/references/m3-expressive-components.md` |
| M3E Color System | `.agents/skills/material-3-expressive/references/m3-color-system.md` |
| M3E Shape | `.agents/skills/material-3-expressive/references/m3-shape.md` |
| M3E Typography | `.agents/skills/material-3-expressive/references/m3-typography.md` |
| M3E Motion Physics | `.agents/skills/material-3-expressive/references/m3-motion-physics.md` |
| M3E Button Tokens | `.agents/skills/material-3-expressive/references/m3-buttons-specs-tokens.md` |
| M3E Compose Mapping | `.agents/skills/material-3-expressive/references/compose-mapping.md` |
| Navigation3 Skill | `.agents/skills/navigation-3/SKILL.md` |
| Original Audit Report | `docs/reports/comprehensive_audit_report_2026-05-03.md` |
| Original M3E Plan | `docs/reports/M3E_IMPLEMENTATION_PLAN_2026-05-04.md` |

### Appendix B: M3E Token Quick Reference

**Motion Springs:**
| Token | Damping | Stiffness |
|-------|---------|-----------|
| expressive.default.spatial | 0.9 | 700 |
| expressive.fast.spatial | 0.9 | 1400 |
| expressive.slow.spatial | 0.9 | 300 |
| expressive.default.effects | 1.0 | 1600 |
| standard.default.spatial | 1.0 | 380 |

**Shape Corner Scale:**
| Token | Value |
|-------|-------|
| none | 0dp |
| extra-small | 4dp |
| small | 8dp |
| medium | 12dp |
| large | 16dp |
| large-increased | 20dp |
| extra-large | 28dp |
| extra-large-increased | 32dp |
| extra-extra-large | 48dp |
| full | 50% |

**Elevation Levels:**
| Level | dp |
|-------|-----|
| 0 | 0dp |
| 1 | 1dp |
| 2 | 3dp |
| 3 | 6dp |
| 4 | 8dp |
| 5 | 12dp |

### Appendix C: Implementation Order Rationale

**Why Phase 0 (Safety) first?**
- CRITICAL concurrency bugs can corrupt user config or cause ANRs
- Security issues (exported provider, allowBackup) are immediate risks
- Fixing these first ensures a stable base for all other changes

**Why Phase 1 (Testing) before UI changes?**
- ViewModels and Repositories will be modified in Phases 2-4
- Having tests first prevents regressions during refactoring
- TDD approach for new SavedStateHandle logic

**Why Phase 2 (Theme) before Phase 5 (M3E Components)?**
- Theme fixes (colors, shapes, typography) don't require dependency upgrade
- Resolving hardcoded colors and missing roles is foundational
- M3E components will inherit theme tokens automatically

**Why Phase 5 (Dependency Upgrade) after theme?**
- Minimizes scope of breaking changes
- Theme is already compliant when M3E components are introduced
- Easier to debug: theme issues vs component issues

**Why Phase 6 (Navigation) after core UI?**
- Navigation changes are large but lower risk than concurrency fixes
- Type-safe routes benefit from stable screen implementations
- Can be deferred if other phases take longer

**Why Phase 7 (Build) late?**
- ProGuard validation requires stable code
- Build optimizations are nice-to-have, not blocking
- Convention plugins are structural, not functional

---

*End of Ultra-Comprehensive Master Implementation Plan*

**Original Reports Preserved:**
- `docs/reports/comprehensive_audit_report_2026-05-03.md`
- `docs/reports/M3E_IMPLEMENTATION_PLAN_2026-05-04.md`

**This Plan:** `docs/reports/MASTER_IMPLEMENTATION_PLAN_2026-05-04.md`
