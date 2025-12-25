# xposed-performance Specification Delta

## ADDED Requirements

### Requirement: Class Lookup Caching

The xposed module SHALL provide a global LRU cache for Class lookups to optimize hook performance.

#### Scenario: Cache Hit
- **WHEN** a hooker requests a Class that was previously loaded
- **THEN** the cached Class instance is returned without ClassLoader.loadClass()
- **AND** the lookup completes in O(1) time

#### Scenario: Cache Miss
- **WHEN** a hooker requests a Class not in cache
- **THEN** the Class is loaded via ClassLoader
- **AND** the loaded Class is stored in cache for future use
- **AND** subsequent requests return the cached instance

#### Scenario: Cache Eviction
- **WHEN** the cache exceeds 100 entries
- **THEN** the least-recently-used Class is evicted
- **AND** frequently-used classes remain in cache

#### Scenario: Cache Key Uniqueness
- **WHEN** the same class name is requested with different ClassLoaders
- **THEN** each ClassLoader gets its own cached instance
- **AND** no cross-contamination occurs between app ClassLoaders

---

### Requirement: ClassCache API

The ClassCache object SHALL provide the following API:

1. `getClass(name: String, loader: ClassLoader?): Class<*>?` - Returns cached or loaded class, null if not found
2. `requireClass(name: String, loader: ClassLoader?): Class<*>` - Returns cached or loaded class, throws if not found
3. `clear()` - Evicts all cached classes
4. `stats(): String` - Returns cache statistics for debugging

#### Scenario: Null Safety
- **WHEN** a class cannot be found by the ClassLoader
- **THEN** `getClass()` returns null
- **AND** `requireClass()` throws ClassNotFoundException
- **AND** no null is stored in the cache

#### Scenario: Thread Safety
- **WHEN** multiple threads access the cache concurrently
- **THEN** all operations complete without data corruption
- **AND** LruCache's built-in synchronization handles concurrency
