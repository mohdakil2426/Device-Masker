# Architecture Optimization - Design Document

## Context

Device Masker is a production-ready LSPosed module. Research identified 6 improvements that can enhance performance at multiple levels:

1. **Hook Layer**: Class lookup caching across 8 hookers
2. **UI Layer**: Compose recomposition optimization
3. **State Layer**: Thread-safe StateFlow updates
4. **Testing Layer**: Automated validation for value generators

### Stakeholders
- End users: Faster app startup, smoother UI
- Developers: Cleaner code, safer refactoring
- Maintainers: Documented behavior, tested generators

### Constraints
- Must maintain backward compatibility with Android 8.0+
- Cannot require additional permissions or dependencies
- All changes must be non-breaking

## Goals / Non-Goals

### Goals
1. Improve hook execution performance by 10-50x for class lookups
2. Reduce UI recompositions by 50%+ for list operations
3. Eliminate potential race conditions in state updates
4. Achieve 100% test coverage for value generators
5. Document XSharedPreferences caching behavior to users

### Non-Goals
- Real-time config updates (XSharedPreferences limitation accepted)
- Complete architecture rewrite
- Adding new features
- Changing external APIs

## Decisions

### Decision 1: LRU Cache for Class Lookups

**What**: Global singleton cache for Class<?> instances with LruCache(100)

**Why**: 
- 8 hookers often look up same classes (TelephonyManager, Build, etc.)
- Each lookup involves ClassLoader.loadClass() - non-trivial cost
- LruCache automatically evicts least-used entries

**Alternatives Considered**:
| Option | Pros | Cons | Decision |
|--------|------|------|----------|
| Per-hooker lazy | Simple | Duplicate classes in memory | ❌ Current |
| HashMap | Fast lookup | Unbounded growth | ❌ |
| **LruCache(100)** | Auto-eviction, bounded | Slightly complex | ✅ Selected |
| WeakHashMap | Auto-collect unused | Unpredictable eviction | ❌ |

---

### Decision 2: derivedStateOf for Expensive Computations

**What**: Wrap filter/sort operations in `remember { derivedStateOf { } }`

**Why**:
- Compose recomposes entire function when any state changes
- derivedStateOf only recomputes when dependencies change
- Filter 500 apps runs on every recompose without this

**Pattern**:
```kotlin
// Before (runs every recompose)
val filtered = apps.filter { it.name.contains(query) }

// After (runs only when apps or query changes)
val filtered by remember(apps, query) {
    derivedStateOf { apps.filter { it.name.contains(query) } }
}
```

**Where to Apply**:
- GroupSpoofingScreen: App filtering by search query
- HomeScreen: Enabled apps count
- GroupsScreen: Groups with app counts

---

### Decision 3: Stable Keys for LazyColumn

**What**: Add `key = { item.id }` to all LazyColumn items calls

**Why**:
- Without keys, Compose tracks items by position
- Adding/removing items causes O(n) recompositions
- With keys, Compose tracks by identity - O(1) updates

**Pattern**:
```kotlin
LazyColumn {
    items(groups, key = { it.id }) { group ->
        GroupCard(group)
    }
}
```

---

### Decision 4: Thread-Safe StateFlow Updates

**What**: Replace `_state.value = ...` with `_state.update { ... }`

**Why**:
- Multiple coroutines can race to update state
- `.value = .value.copy()` is not atomic - can lose updates
- `.update {}` provides atomic read-modify-write

**Pattern**:
```kotlin
// Not thread-safe
_state.value = _state.value.copy(loading = true)

// Thread-safe
_state.update { it.copy(loading = true) }
```

---

### Decision 5: Unit Tests for Generators

**What**: JUnit tests for IMEI, MAC, Android ID generators

**Why**:
- Generators produce values that must be valid (Luhn, unicast bit)
- Invalid values can cause app bans or detection
- Tests catch regressions and document expected behavior

**Test Coverage**:
| Generator | Tests |
|-----------|-------|
| IMEIGenerator | Length=15, Luhn valid, TAC prefix |
| MACGenerator | Format, unicast bit |
| AndroidIdGenerator | Length=16, lowercase hex |
| SerialGenerator | Alphanumeric, length 8-16 |

---

### Decision 6: Document Config Sync Behavior

**What**: Add info card in DiagnosticsScreen + README section

**Why**:
- XSharedPreferences caches values in target app
- Users confused why changes don't take effect immediately
- Documenting sets correct expectations

**Implementation**:
- DiagnosticsScreen: Card with icon + text
- README: Section explaining restart requirement

## Risks / Trade-offs

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| LruCache adds overhead | Low | Low | Only 100 entries, fast lookups |
| derivedStateOf complexity | Low | Low | Well-documented pattern |
| Tests increase build time | Low | Low | Only run on demand |
| Documentation may be ignored | Medium | Low | Prominent placement |

## Migration Plan

### Phase 1: Immediate (2-3 hours)
1. Add stable keys to all LazyColumns
2. Add info card in DiagnosticsScreen
3. Update README
4. Audit ViewModels for thread-safe updates

### Phase 2: Performance (3-4 hours)
1. Create ClassCache utility
2. Apply derivedStateOf to screens
3. Verify improvements with profiler

### Phase 3: Testing (3-4 hours)
1. Add test dependencies to common module
2. Write generator tests
3. Integrate with CI (future)

### Rollback
Each change is independent and easily reversible.

## Open Questions

1. **Q**: Should ClassCache be used by all hookers or opt-in?
   **A**: Opt-in initially, can migrate gradually.

2. **Q**: Should we add Compose compiler metrics?
   **A**: Future consideration for v1.2.

3. **Q**: Any CI integration for tests?
   **A**: Out of scope for this change, future enhancement.
