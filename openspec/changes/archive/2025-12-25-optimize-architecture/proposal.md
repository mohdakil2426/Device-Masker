# Change: Optimize Architecture for Performance and Reliability

## Why

The research report (docs/research/architecture-enhancement-report.md) identified 6 improvements that can enhance the Device Masker module's performance, reliability, and code quality. These optimizations target:

1. **Hook execution speed** - LRU caching for class lookups
2. **UI smoothness** - derivedStateOf and stable keys in Compose
3. **Thread safety** - StateFlow update patterns
4. **Code reliability** - Unit tests for value generators
5. **User experience** - Clear documentation about config sync behavior

## What Changes

### 1. Xposed Performance (xposed module)
- **ADDED**: `ClassCache.kt` - Global LRU cache for Class lookups across all hookers
- **MODIFIED**: Hookers can optionally use ClassCache for shared class references
- Impact: 10-50x faster class lookups, reduced memory fragmentation

### 2. UI Performance (app module)
- **MODIFIED**: Add `derivedStateOf` for expensive computations in screens
- **MODIFIED**: Add stable `key` parameters to all LazyColumn usages
- **MODIFIED**: Thread-safe StateFlow updates using `.update {}` pattern
- Impact: Smoother scrolling, fewer recompositions, no race conditions

### 3. Testing Infrastructure (common module)
- **ADDED**: Unit tests for IMEI, MAC, Android ID generators
- **ADDED**: Test dependencies in common module
- Impact: Catches regressions, validates Luhn/unicast correctness

### 4. Documentation (app module + README)
- **ADDED**: Info card in DiagnosticsScreen about config sync behavior
- **MODIFIED**: README to document restart requirement
- Impact: Reduced user confusion about config changes

## Impact

### Affected Specs
- `core-infrastructure` - Thread-safe state updates, documentation
- `device-spoofing` (indirectly) - Hooker performance improvements
- New potential specs: `ui-performance`, `xposed-performance`, `testing`

### Affected Code

| Module | Files |
|--------|-------|
| :xposed | `utils/ClassCache.kt` (new), potentially all hookers |
| :app | `ui/screens/*/ViewModel.kt`, screens with LazyColumn |
| :app | `ui/screens/diagnostics/DiagnosticsScreen.kt` |
| :common | `src/test/kotlin/.../generators/GeneratorTests.kt` (new) |
| root | `README.md` |

### Breaking Changes
None - all changes are additive or internal optimizations.

### Risk Assessment
- **Low risk**: All changes are non-breaking, easily reversible
- **High benefit**: Performance improvements, user experience, code quality

## Success Criteria

1. ✅ All existing functionality continues to work
2. ✅ LazyColumn scrolling maintains 60fps
3. ✅ Generator tests pass with 100% coverage
4. ✅ Thread-safe StateFlow updates across all ViewModels
5. ✅ Users understand config sync behavior (info card visible)

## Dependencies

- None - uses existing libraries and patterns
- Test dependencies added to common module (kotlin-test, junit)

## Rollback Plan

Each optimization is independent:
1. ClassCache: Revert to direct class lookups
2. derivedStateOf: Revert to inline computations
3. Stable keys: Non-breaking, can leave in place
4. Tests: Don't affect production code
5. Documentation: Can update messaging anytime
