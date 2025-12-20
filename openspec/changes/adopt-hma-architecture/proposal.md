# Change: Adopt HMA-OSS Production Architecture

## Summary

Complete architectural refactoring of Device Masker to adopt the **HMA-OSS production-grade architecture** exactly. This involves migrating from a single-module structure with DataStore to a **3-module Gradle project** with **AIDL-based IPC** and **JSON file storage**.

HMA-OSS (Hide My Applist) is a battle-tested, production-grade Xposed module that hides apps from detection. Our project (Device Masker) does similar work but for device identifier spoofing. Since both modules share the same architectural requirements (cross-process config sync, hook management, anti-detection), adopting HMA-OSS's proven architecture will provide stability and reliability.

## Why

### Current Problems
1. **DataStore in hook context** causes deadlocks and crashes
2. **Single module** structure makes code organization unclear
3. **XSharedPreferences approach** has sync delays and file permission issues
4. **Hooks run in target processes** limiting system-level capabilities

### Why HMA-OSS Architecture?
1. **Production-proven** - HMA-OSS is widely used and stable
2. **Same problem domain** - Cross-process Xposed module with config management
3. **Instant config sync** - AIDL provides real-time updates
4. **No file I/O in hooks** - Config lives in memory
5. **Clean separation** - 3 modules with clear responsibilities
6. **System-level hooks** - Running in system_server enables more capabilities

## What Changes

### **BREAKING**: Complete Architecture Refactoring

#### Gradle Structure
- **FROM**: Single `:app` module
- **TO**: Three modules (`:app`, `:common`, `:xposed`)

#### Storage Mechanism
- **FROM**: Jetpack DataStore (Protobuf)
- **TO**: Plain JSON files (like HMA-OSS)

#### IPC Mechanism
- **FROM**: XSharedPreferences (file-based)
- **TO**: AIDL Binder (memory-based, instant)

#### Hook Execution Location
- **FROM**: Target app processes (WhatsApp, etc.)
- **TO**: system_server process (with YukiHookAPI)

#### Hook Framework
- **UNCHANGED**: YukiHookAPI (we keep this, HMA-OSS uses raw Xposed)

#### UI Framework
- **UNCHANGED**: Jetpack Compose + Material 3 Expressive

### New Files (Major)

| Module | File | Purpose |
|--------|------|---------|
| `:common` | `IDeviceMaskerService.aidl` | AIDL interface definition |
| `:common` | `JsonConfig.kt` | Serializable config model |
| `:app` | `ConfigManager.kt` | JSON file read/write + AIDL calls |
| `:app` | `ServiceClient.kt` | AIDL client proxy |
| `:app` | `ServiceProvider.kt` | ContentProvider for binder |
| `:xposed` | `XposedEntry.kt` | YukiHookAPI entry point |
| `:xposed` | `DeviceMaskerService.kt` | AIDL server implementation |
| `:xposed` | `UserService.kt` | Service registration helper |

### Removed Files

| File | Reason |
|------|--------|
| `data/datastore/SpoofDataStore.kt` | Replaced by JSON file storage |
| `data/repository/ProfileRepository.kt` | Integrated into ConfigManager |
| `sync/PreferenceSyncManager.kt` | Not needed with AIDL |
| `hook/provider/HookDataProvider.kt` | Replaced by in-memory config |

### Moved Files

| From | To | Notes |
|------|-----|-------|
| `data/models/*.kt` | `:common` module | Shared between UI and xposed |
| `hook/hooker/*.kt` | `:xposed` module | All hookers move to xposed module |
| `ui/**/*.kt` | `:app` module | UI unchanged, just relocated |

## Impact

### Affected Specs
- **core-infrastructure**: Complete restructuring
- **data-management**: New storage mechanism
- **anti-detection**: Hook execution context changes

### Affected Code
- **Everything** - This is a complete refactoring

### Migration Path
- User data migration from DataStore to JSON on first launch after update
- Backward-compatible config format

### Risk Assessment
- **HIGH RISK**: Complete architecture change
- **MITIGATED BY**: Following proven HMA-OSS patterns exactly

## Technical Decisions

### Decision 1: Keep YukiHookAPI
- HMA-OSS uses raw Xposed API
- We keep YukiHookAPI for cleaner Kotlin DSL
- YukiHookAPI's `loadSystem {}` supports system_server hooks

### Decision 2: Remove DataStore Completely
- HMA-OSS uses plain JSON files
- Simpler, no async complexity
- Direct file read/write with `File.readText()` / `File.writeText()`

### Decision 3: AIDL for All IPC
- No SharedPreferences, no XSharedPreferences
- Binder calls for instant sync
- ContentProvider as binder transport

### Decision 4: In-Memory Config in system_server
- `DeviceMaskerService.config` holds current config
- All hooks read from this object directly
- Zero file I/O during hook execution

## Success Criteria

1. Module builds successfully as 3-module Gradle project
2. LSPosed recognizes and loads module
3. Service starts in system_server on boot
4. UI can connect to service via AIDL
5. Config changes reflect in hooks instantly
6. All existing spoof functions work
7. Anti-detection passes RootBeer test
8. No bootloops, no crashes
