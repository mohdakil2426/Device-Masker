# Design: System-Wide AIDL Service Architecture

## Context

Device Masker currently uses XSharedPreferences for cross-process configuration, which caches values at first read and requires target app restart for updates. This is a known limitation of the Xposed ecosystem.

HMA-OSS (Hide My Applist) has solved this problem with a production-proven architecture that:
1. Hooks into `system_server` at boot
2. Registers an AIDL binder service
3. Exposes service via ContentProvider bridge
4. Provides real-time configuration updates

This design adapts HMA-OSS's architecture while keeping YukiHookAPI for its Kotlin DSL benefits.

## Goals / Non-Goals

### Goals
- Real-time configuration updates without app restart
- Centralized logging and diagnostics
- Simplified LSPosed configuration (single scope)
- Maintain YukiHookAPI's Kotlin DSL syntax
- Support Android 10-16 (primary), 8-9 (secondary)

### Non-Goals
- Switch hook framework (keep YukiHookAPI)
- Add new spoofing capabilities (scope creep)
- Modify anti-detection logic (separate concern)

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        NEW ARCHITECTURE                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌─────────────┐      AIDL Binder      ┌─────────────────────────┐  │
│  │   App UI    │ ◀────────────────────▶│  DeviceMaskerService    │  │
│  │  (Compose)  │    (real-time IPC)    │  (in system_server)     │  │
│  └─────────────┘                        └───────────┬─────────────┘  │
│        │                                            │                 │
│        │ ContentProvider                            │ Manages         │
│        │ "getService"                               │                 │
│        ▼                                            ▼                 │
│  ┌─────────────┐                        ┌─────────────────────────┐  │
│  │ServiceClient│                        │  /data/misc/devicemasker│  │
│  │  (binder)   │                        │  └── config.json        │  │
│  └─────────────┘                        └─────────────────────────┘  │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐ │
│  │                     LSPosed Framework                            │ │
│  │  ┌─────────────────────────────────────────────────────────────┐│ │
│  │  │              System Framework Hook (android)                 ││ │
│  │  │                                                             ││ │
│  │  │   loadSystem() → SystemServiceHooker                        ││ │
│  │  │                  └── Registers DeviceMaskerService          ││ │
│  │  │                                                             ││ │
│  │  │   loadApp(*) → Hookers query DeviceMaskerService            ││ │
│  │  │               └── DeviceHooker(service)                     ││ │
│  │  │               └── NetworkHooker(service)                    ││ │
│  │  │               └── ...                                       ││ │
│  │  └─────────────────────────────────────────────────────────────┘│ │
│  └─────────────────────────────────────────────────────────────────┘ │
│                                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

## Decisions

### Decision 1: Keep YukiHookAPI

**What**: Continue using YukiHookAPI instead of switching to EzXHelper (used by HMA-OSS)

**Why**:
- YukiHookAPI fully supports `loadSystem { }` for system framework hooking
- Provides `onXposedEvent()` for raw Xposed API access when needed
- Maintains Kotlin DSL benefits and KSP auto-generation
- Reduces migration effort and risk

**Alternatives Considered**:
- EzXHelper: More direct but loses DSL benefits
- Raw Xposed API: Maximum control but verbose

### Decision 2: AIDL via ContentProvider Bridge

**What**: Use ContentProvider to expose service binder to UI app

**Why**:
- ContentProvider is stable across Android versions
- No need to modify system services registry
- Already proven by HMA-OSS

**Alternatives Considered**:
- Direct ServiceManager registration: Requires SELinux modifications
- Broadcast-based IPC: Not suitable for binder passing

### Decision 3: Config Storage in /data/misc/

**What**: Store configuration in `/data/misc/devicemasker/config.json`

**Why**:
- Accessible by `system_server` with default SELinux context
- Survives app uninstall (if migrating data)
- Standard location for system services

**Alternatives Considered**:
- `/data/data/com.astrixforge.devicemasker/`: Not accessible from system_server
- `/data/system/`: Requires additional SELinux context

### Decision 4: Atomic Config Updates

**What**: Write config to temp file, then rename

**Why**:
- Prevents corruption on power loss
- Maintains backup of previous config
- Standard file safety pattern

## Data Flow

### Configuration Update Flow

```
1. User changes IMEI in UI
2. ViewModel.saveConfig() called
3. ServiceClient.writeConfig(json) via AIDL binder
4. DeviceMaskerService.writeConfig():
   a. Parse JSON to JsonConfig
   b. Update AtomicReference<JsonConfig>
   c. Write to temp file, rename to config.json
   d. Notify hooks (if needed)
5. Next hook read gets new value immediately
```

### Hook Query Flow

```
1. App starts, XposedEntry.onHook() runs
2. loadSystem { } initializes DeviceMaskerService (if system_server)
3. loadApp { } runs for each app:
   a. Get service reference: DeviceMaskerService.getInstance()
   b. Query: service.isAppEnabled(packageName)
   c. If enabled, load hookers with service reference
4. Hooker.onHook():
   a. Query: service.getSpoofValue(pkg, KEY_IMEI)
   b. Apply hook with returned value
   c. Increment: service.incrementFilterCount(pkg)
```

## Component Design

### IDeviceMaskerService.aidl

```
interface IDeviceMaskerService {
    // Config
    void writeConfig(in String json);
    String readConfig();
    void reloadConfig();
    
    // Queries
    boolean isModuleEnabled();
    boolean isAppEnabled(in String packageName);
    String getSpoofValue(in String packageName, in String key);
    
    // Stats
    void incrementFilterCount(in String packageName);
    int getFilterCount(in String packageName);
    int getHookedAppCount();
    
    // Logging
    void log(in String tag, in String message, int level);
    List<String> getLogs(int maxCount);
    
    // Control
    boolean isServiceAlive();
    String getServiceVersion();
}
```

### DeviceMaskerService Thread Safety

```kotlin
class DeviceMaskerService : IDeviceMaskerService.Stub() {
    // Thread-safe state management
    private val config = AtomicReference<JsonConfig>(JsonConfig())
    private val filterCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val logs = ConcurrentLinkedDeque<String>()
    private val hookedApps = ConcurrentHashMap.newKeySet<String>()
}
```

## Risks / Trade-offs

### Risk 1: System Instability

**Risk**: Bugs in system_server hooks can cause bootloops  
**Probability**: Medium (HMA-OSS is production-proven)  
**Impact**: Critical  
**Mitigation**:
- Wrap all system hooks in try-catch
- Log errors but don't throw
- Provide recovery documentation
- Create pre-migration backup (done: `xposed-backup-2026-01-20/`)

### Risk 2: SELinux Denials

**Risk**: system_server cannot access config files  
**Probability**: Low (using /data/misc/)  
**Impact**: High  
**Mitigation**:
- Test with `setenforce 0` first
- Document SELinux context requirements
- Consider Magisk module for SELinux fix if needed

### Risk 3: Android Version Differences

**Risk**: System APIs differ across Android 8-16  
**Probability**: Medium  
**Impact**: Medium  
**Mitigation**:
- Use optional() for version-specific methods
- Test matrix covering Android 10-16
- Fallback to legacy PrefsHelper if service unavailable

### Risk 4: Service Connection Failures

**Risk**: UI app cannot connect to service  
**Probability**: Low  
**Impact**: Medium  
**Mitigation**:
- Retry with exponential backoff
- Show clear error message in UI
- Fallback to local-only mode

## Migration Plan

### Phase 1: Foundation (3-4 days)
1. Create AIDL interface in `:common` module
2. Implement DeviceMaskerService skeleton
3. Implement ConfigManager for file I/O
4. Create ServiceBridge ContentProvider
5. Create SystemServiceHooker for boot hook

### Phase 2: Hook Refactor (4-5 days)
1. Modify XposedEntry for `loadSystem { }`
2. Refactor BaseSpoofHooker to use service
3. Migrate all 8 hookers one by one
4. Remove PrefsHelper when complete

### Phase 3: UI Integration (2-3 days)
1. Create ServiceClient in `:app` module
2. Update ViewModels to use ServiceClient
3. Add service status in Diagnostics screen
4. Add real-time log viewer (optional)

### Phase 4: Testing (3-4 days)
1. Test on Android 10-16 matrix
2. Test boot stability
3. Test config propagation
4. Stress test rapid config changes

### Rollback Plan

1. **Feature Flag**: `FeatureFlags.USE_AIDL_SERVICE = false`
2. **Code Fallback**: Legacy mode loads PrefsHelper if service unavailable
3. **Full Revert**: Restore from `xposed-backup-2026-01-20/`

## Open Questions

1. **Q**: Do we need SELinux policy modifications?  
   **A**: Test first with /data/misc/ path; likely not needed

2. **Q**: How to handle apps started before service?  
   **A**: Service initializes at AMS.systemReady(), before most apps

3. **Q**: Should we keep XSharedPreferences as fallback?  
   **A**: Yes, for robustness on service failure

## Success Metrics

- [ ] Service runs stable on Android 10-16
- [ ] Config changes apply < 100ms (no restart)
- [ ] No bootloops in 100 boot cycles
- [ ] All 8 hookers work with new architecture
- [ ] Filter count tracking functional

---

## References

| Document | Path | Purpose |
|----------|------|--------|
| Architecture Migration Plan | `docs/ARCHITECTURE_MIGRATION_PLAN.md` | Detailed implementation plan with code examples |
| HMA-OSS Source Reference | `docs/oth-repo-projects/hma-oss.txt` | Full HMA-OSS codebase for architecture reference |
| YukiHookAPI Guide | `docs/official-best-practices/lsposed/YukiHookAPI.md` | Hook API best practices |
| Xposed Backup | `xposed-backup-2026-01-20/` | Pre-migration backup for rollback |
