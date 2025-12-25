# Device Masker - Architecture Enhancement Implementation Plan

**Date**: December 25, 2025  
**Based On**: architecture-enhancement-report.md  
**Scope**: High-priority improvements for v1.1  

---

## Executive Summary

This plan details the implementation steps for 6 immediate improvements identified in the research report. These optimizations require **~12 hours total effort** and provide significant performance, reliability, and user experience benefits.

---

## Phase 1: Immediate Optimizations (4-5 Hours)

### 1.1 LRU Cache for Class Lookups

**Goal**: Reduce class lookup overhead across all hookers

**File to Create**: `xposed/src/main/kotlin/.../xposed/utils/ClassCache.kt`

```kotlin
package com.astrixforge.devicemasker.xposed.utils

import android.util.LruCache

/**
 * Global LRU cache for Class lookups across all hookers.
 * 
 * Benefits:
 * - Avoids duplicate class lookups across hookers
 * - Automatically evicts least-recently-used entries
 * - Thread-safe via synchronized LruCache
 * 
 * Capacity: 100 classes (typical usage: 30-40 unique classes)
 */
object ClassCache {
    
    private const val MAX_ENTRIES = 100
    
    private val cache = object : LruCache<CacheKey, Class<*>>(MAX_ENTRIES) {
        override fun entryRemoved(evicted: Boolean, key: CacheKey, oldValue: Class<*>, newValue: Class<*>?) {
            // Optional: Log evictions for debugging
        }
    }
    
    data class CacheKey(val className: String, val loaderId: Int)
    
    /**
     * Gets a class, using cache if available.
     * 
     * @param name Fully-qualified class name
     * @param loader ClassLoader to use (null for system)
     * @return Class instance or null if not found
     */
    fun getClass(name: String, loader: ClassLoader? = null): Class<*>? {
        val effectiveLoader = loader ?: ClassLoader.getSystemClassLoader()
        val key = CacheKey(name, System.identityHashCode(effectiveLoader))
        
        // Check cache first
        cache[key]?.let { return it }
        
        // Load and cache
        return runCatching {
            effectiveLoader.loadClass(name)
        }.getOrNull()?.also { 
            cache.put(key, it) 
        }
    }
    
    /**
     * Gets a class, throwing if not found.
     */
    fun requireClass(name: String, loader: ClassLoader? = null): Class<*> {
        return getClass(name, loader) ?: throw ClassNotFoundException(name)
    }
    
    /**
     * Clears the cache. Call during cleanup if needed.
     */
    fun clear() = cache.evictAll()
    
    /**
     * Gets cache statistics for debugging.
     */
    fun stats(): String = "ClassCache: size=${cache.size()}, hits=${cache.hitCount()}, misses=${cache.missCount()}"
}
```

**Usage in Hookers**:
```kotlin
// Before:
private val telephonyClass by lazy { "android.telephony.TelephonyManager".toClassOrNull() }

// After (option 1 - still use lazy for local reference):
private val telephonyClass by lazy { ClassCache.getClass("android.telephony.TelephonyManager", appClassLoader) }

// After (option 2 - use directly):
ClassCache.getClass("android.telephony.TelephonyManager", appClassLoader)?.apply {
    method { name = "getDeviceId" }.hook { ... }
}
```

**Effort**: 2 hours

---

### 1.2 Stable Keys in All LazyColumns

**Goal**: Improve list performance and enable smooth animations

**Files to Audit**:
1. `app/ui/screens/groups/GroupsScreen.kt`
2. `app/ui/screens/groups/spoofing/GroupSpoofingScreen.kt`
3. `app/ui/screens/home/HomeScreen.kt`
4. Any other LazyColumn usage

**Pattern**:
```kotlin
// ❌ Before (no keys):
LazyColumn {
    items(groups) { group ->
        GroupCard(group)
    }
}

// ✅ After (stable keys):
LazyColumn {
    items(
        items = groups,
        key = { group -> group.id }
    ) { group ->
        GroupCard(group)
    }
}

// For apps list:
LazyColumn {
    items(
        items = apps,
        key = { app -> app.packageName }
    ) { app ->
        AppCard(app)
    }
}
```

**Effort**: 1 hour

---

### 1.3 Document Config Limitation

**Goal**: Set clear user expectations about config refresh behavior

**File 1**: `README.md` - Add section:
```markdown
## ⚠️ Important Notes

### Configuration Sync
Config changes require target apps to **restart** to take effect. This is due to how 
XSharedPreferences caches values - a system limitation, not a bug.

**How to apply changes**:
1. Make changes in Device Masker app
2. Either:
   - Force-close the target app (Settings → Apps → [App] → Force Stop)
   - Or restart your device

This applies to all spoofing changes (IMEI, MAC, etc.).
```

**File 2**: Add info card in `DiagnosticsScreen.kt`:
```kotlin
@Composable
private fun ConfigSyncInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Configuration Sync",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Changes require target apps to restart",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}
```

**Effort**: 1 hour

---

## Phase 2: Performance Optimizations (4-5 Hours)

### 2.1 derivedStateOf for Expensive Computations

**Goal**: Reduce unnecessary recomputations

**Pattern to Apply**:

**File**: `GroupSpoofingScreen.kt` - App filtering:
```kotlin
// ❌ Before:
val filteredApps = installedApps.filter { app ->
    app.name.contains(searchQuery, ignoreCase = true) ||
    app.packageName.contains(searchQuery, ignoreCase = true)
}

// ✅ After:
val filteredApps by remember(installedApps, searchQuery) {
    derivedStateOf {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { app ->
                app.name.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
}
```

**File**: `HomeScreen.kt` - Enabled apps count:
```kotlin
// ❌ Before:
val enabledCount = apps.count { it.enabled }

// ✅ After:
val enabledCount by remember(apps) {
    derivedStateOf { apps.count { it.enabled } }
}
```

**File**: `GroupsScreen.kt` - Groups with app counts:
```kotlin
// ✅ Pattern:
val groupsWithCounts by remember(groups, appConfigs) {
    derivedStateOf {
        groups.map { group ->
            group to appConfigs.count { it.groupId == group.id }
        }
    }
}
```

**Effort**: 3 hours (audit all screens)

---

### 2.2 Thread-Safe StateFlow Updates

**Audit all ViewModels for this pattern**:
```kotlin
// ❌ Not thread-safe:
_state.value = _state.value.copy(loading = true)

// ✅ Thread-safe:
_state.update { it.copy(loading = true) }
```

**Effort**: 1 hour

---

## Phase 3: Testing & Quality (4-5 Hours)

### 3.1 Unit Tests for Value Generators

**File to Create**: `common/src/test/kotlin/.../generators/GeneratorTests.kt`

```kotlin
package com.astrixforge.devicemasker.common.generators

import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class IMEIGeneratorTest {
    
    @Test
    fun `generates 15 digit IMEI`() {
        repeat(100) {
            val imei = IMEIGenerator.generate()
            assertEquals(15, imei.length, "IMEI should be 15 digits")
            assertTrue(imei.all { it.isDigit() }, "IMEI should only contain digits")
        }
    }
    
    @Test
    fun `generates valid Luhn checksum`() {
        repeat(100) {
            val imei = IMEIGenerator.generate()
            assertTrue(isValidLuhn(imei), "IMEI should pass Luhn check: $imei")
        }
    }
    
    @Test
    fun `generates with valid TAC prefix`() {
        val validPrefixes = listOf("35", "86", "01", "45", "49")
        repeat(100) {
            val imei = IMEIGenerator.generate()
            assertTrue(
                validPrefixes.any { imei.startsWith(it) },
                "IMEI should start with valid TAC: $imei"
            )
        }
    }
    
    private fun isValidLuhn(number: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in number.length - 1 downTo 0) {
            var n = number[i].digitToInt()
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }
}

class MACGeneratorTest {
    
    @Test
    fun `generates valid MAC format`() {
        repeat(100) {
            val mac = MACGenerator.generate()
            assertTrue(
                mac.matches(Regex("[0-9A-F]{2}(:[0-9A-F]{2}){5}")),
                "Invalid MAC format: $mac"
            )
        }
    }
    
    @Test
    fun `generates unicast MAC`() {
        repeat(100) {
            val mac = MACGenerator.generate()
            val firstByte = mac.substring(0, 2).toInt(16)
            assertTrue(
                (firstByte and 0x01) == 0,
                "MAC should be unicast (LSB of first byte = 0): $mac"
            )
        }
    }
}

class AndroidIdGeneratorTest {
    
    @Test
    fun `generates 16 character hex ID`() {
        repeat(100) {
            val id = AndroidIdGenerator.generate()
            assertEquals(16, id.length, "Android ID should be 16 chars")
            assertTrue(
                id.all { it in '0'..'9' || it in 'a'..'f' },
                "Android ID should be lowercase hex: $id"
            )
        }
    }
}
```

**Add to `common/build.gradle.kts`**:
```kotlin
dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.junit)
}
```

**Effort**: 3-4 hours

---

## Implementation Checklist

### Day 1 (4 hours)
- [ ] Create `ClassCache.kt` utility
- [ ] Update one hooker to use ClassCache as proof-of-concept
- [ ] Add stable keys to all LazyColumns
- [ ] Add config sync info to DiagnosticsScreen

### Day 2 (4 hours)
- [ ] Apply `derivedStateOf` to GroupSpoofingScreen
- [ ] Apply `derivedStateOf` to HomeScreen
- [ ] Apply `derivedStateOf` to GroupsScreen
- [ ] Audit ViewModels for thread-safe StateFlow updates

### Day 3 (4 hours)
- [ ] Set up test configuration in common module
- [ ] Write IMEI generator tests
- [ ] Write MAC generator tests
- [ ] Write Android ID generator tests
- [ ] Run all tests and fix any failures

---

## Success Metrics

After implementation, verify:

| Metric | Before | Target |
|--------|--------|--------|
| Compile time | Baseline | Same or better |
| LazyColumn scroll FPS | ~55-60 | Stable 60 |
| Class lookup cache hits | N/A | >80% after warmup |
| Test coverage (generators) | 0% | 100% |

---

## Rollback Plan

Each change is isolated:
1. **ClassCache**: Can be disabled by reverting to direct class lookups
2. **derivedStateOf**: Can revert to direct computation
3. **Stable keys**: Non-breaking, improves perf only
4. **Tests**: Don't affect production code

---

**Plan Created**: December 25, 2025  
**Estimated Effort**: 12 hours  
**Risk Level**: Low (all changes are additive/non-breaking)
