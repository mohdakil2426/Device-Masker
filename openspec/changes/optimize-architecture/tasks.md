# Architecture Optimization - Implementation Tasks

## Overview

| Phase | Tasks | Effort | Priority |
|-------|-------|--------|----------|
| Phase 1 | Quick Wins (Keys, Docs, Thread Safety) | 2-3 hours | 🔴 HIGH |
| Phase 2 | Performance (Cache, derivedStateOf) | 3-4 hours | 🔴 HIGH |
| Phase 3 | Testing (Generator Tests) | 3-4 hours | 🟡 MEDIUM |

**Total Estimated Effort**: 10-12 hours

---

## Phase 1: Quick Wins (Do First)

### 1.1 Add Stable Keys to LazyColumns
- [ ] 1.1.1 Update `GroupsScreen.kt` - Add `key = { group.id }` to groups LazyColumn
- [ ] 1.1.2 Update `GroupSpoofingScreen.kt` - Add `key = { app.packageName }` to apps LazyColumn
- [ ] 1.1.3 Update `HomeScreen.kt` - Add keys to any LazyColumn usage
- [ ] 1.1.4 Search for other LazyColumn usages and add keys
- [ ] 1.1.5 Verify scrolling remains smooth in all screens

**Validation**: Scroll through long lists, verify no visual jank

---

### 1.2 Document Config Sync Behavior
- [ ] 1.2.1 Create `ConfigSyncInfoCard` composable in DiagnosticsScreen
- [ ] 1.2.2 Add card to DiagnosticsScreen UI above other cards
- [ ] 1.2.3 Update README.md with "Important Notes" section about restart requirement
- [ ] 1.2.4 Verify info card is visible and text is clear

**Validation**: Open DiagnosticsScreen, verify card is prominent

---

### 1.3 Thread-Safe StateFlow Updates
- [ ] 1.3.1 Audit `HomeViewModel.kt` - Replace `_state.value = ...copy()` with `_state.update { }`
- [ ] 1.3.2 Audit `GroupsViewModel.kt` - Replace unsafe updates
- [ ] 1.3.3 Audit `GroupSpoofingViewModel.kt` - Replace unsafe updates
- [ ] 1.3.4 Audit `SettingsViewModel.kt` - Replace unsafe updates
- [ ] 1.3.5 Audit `DiagnosticsViewModel.kt` - Replace unsafe updates
- [ ] 1.3.6 Verify all ViewModels use `.update {}` pattern

**Validation**: Run app, trigger multiple rapid actions, verify no state corruption

---

## Phase 2: Performance Optimizations

### 2.1 Create ClassCache Utility
- [ ] 2.1.1 Create `xposed/src/main/kotlin/.../xposed/utils/ClassCache.kt`
- [ ] 2.1.2 Implement LruCache with capacity 100
- [ ] 2.1.3 Add `getClass(name, loader)` method with caching
- [ ] 2.1.4 Add `requireClass(name, loader)` for non-optional classes
- [ ] 2.1.5 Add `stats()` method for debugging
- [ ] 2.1.6 Add `clear()` method for cleanup
- [ ] 2.1.7 Document usage patterns in KDoc

**Validation**: Build succeeds, no runtime errors

---

### 2.2 Apply derivedStateOf to Screens
- [ ] 2.2.1 Update `GroupSpoofingScreen.kt` - Wrap app filtering in derivedStateOf
- [ ] 2.2.2 Update `HomeScreen.kt` - Wrap enabled count in derivedStateOf
- [ ] 2.2.3 Update `GroupsScreen.kt` - Wrap group counts in derivedStateOf
- [ ] 2.2.4 Identify and wrap any other expensive computations
- [ ] 2.2.5 Verify UI still works correctly after changes

**Validation**: Filter apps quickly, verify no lag or incorrect results

---

## Phase 3: Testing Infrastructure

### 3.1 Setup Test Configuration
- [ ] 3.1.1 Add test dependencies to `common/build.gradle.kts`:
  - `testImplementation(kotlin("test"))`
  - `testImplementation("junit:junit:4.13.2")`
- [ ] 3.1.2 Create test source directory: `common/src/test/kotlin/.../generators/`
- [ ] 3.1.3 Verify test task runs: `./gradlew :common:test`

**Validation**: Empty test task completes successfully

---

### 3.2 Write Generator Tests
- [ ] 3.2.1 Create `IMEIGeneratorTest.kt`:
  - Test 15-digit length
  - Test Luhn checksum validity
  - Test TAC prefix validity
- [ ] 3.2.2 Create `MACGeneratorTest.kt`:
  - Test format XX:XX:XX:XX:XX:XX
  - Test unicast bit (LSB of first byte = 0)
- [ ] 3.2.3 Create `AndroidIdGeneratorTest.kt`:
  - Test 16-character length
  - Test lowercase hex format
- [ ] 3.2.4 Create `SerialGeneratorTest.kt`:
  - Test alphanumeric format
  - Test length 8-16 characters
- [ ] 3.2.5 Run all tests: `./gradlew :common:test`
- [ ] 3.2.6 Verify 100% pass rate

**Validation**: All tests pass, no failures

---

## Final Validation

### Build Verification
- [ ] F.1 Run `./gradlew :common:assembleDebug` - Must succeed
- [ ] F.2 Run `./gradlew :xposed:assembleDebug` - Must succeed
- [ ] F.3 Run `./gradlew :app:assembleDebug` - Must succeed
- [ ] F.4 Run `./gradlew :common:test` - All tests must pass

### Manual Testing
- [ ] F.5 Install APK on test device
- [ ] F.6 Navigate through all screens - Verify no crashes
- [ ] F.7 Scroll long lists rapidly - Verify smooth 60fps
- [ ] F.8 Open DiagnosticsScreen - Verify info card visible
- [ ] F.9 Enable spoofing on target app - Verify hooks work
- [ ] F.10 Check logcat for any new errors

### Documentation
- [ ] F.11 Verify README changes are accurate
- [ ] F.12 Update memory bank with implementation details

---

## Task Dependencies

```
Phase 1 (Quick Wins) ─────────────────┐
├── 1.1 Stable Keys ──────────────────┤
├── 1.2 Documentation ────────────────┤ Can be done in parallel
├── 1.3 Thread-Safe Updates ──────────┘
                     │
                     ▼
Phase 2 (Performance) ────────────────┐
├── 2.1 ClassCache ───────────────────┤ Sequential (cache first)
└── 2.2 derivedStateOf ───────────────┘
                     │
                     ▼
Phase 3 (Testing) ────────────────────┐
├── 3.1 Setup ────────────────────────┤ Sequential (setup first)
└── 3.2 Write Tests ──────────────────┘
                     │
                     ▼
           Final Validation
```

---

## Notes

- All Phase 1 tasks can be done in parallel
- Phase 2 should be done after Phase 1 to verify baseline
- Phase 3 is independent but lower priority
- ClassCache is optional for hookers - start with proof-of-concept
- Tests don't change production code - safe to add anytime
