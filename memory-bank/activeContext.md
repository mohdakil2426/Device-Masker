# Active Context: Device Masker

## Current Work Focus

### ✅ COMPLETE: AIDL Architecture Migration (Jan 20, 2026)

**Status**: Implementation Complete ✅ (Testing Pending)  
**Scope**: Major refactor from XSharedPreferences to System-Wide AIDL Service

#### What Was Completed

| Phase       | Task                           | Status                                                              |
| ----------- | ------------------------------ | ------------------------------------------------------------------- |
| **Phase 1** | AIDL Interface & Common Module | ✅ `IDeviceMaskerService.aidl` with 15 methods                      |
| **Phase 2** | Xposed Service Implementation  | ✅ `DeviceMaskerService.kt`, `ConfigManager.kt`, `ServiceBridge.kt` |
| **Phase 3** | System Hook Implementation     | ✅ `SystemServiceHooker.kt`, `XposedEntry.kt` loadSystem            |
| **Phase 4** | Hooker Migration               | ✅ Hybrid `BaseSpoofHooker` (service + XSharedPrefs fallback)       |
| **Phase 5** | UI Integration                 | ✅ `ServiceClient.kt`, `DiagnosticsViewModel` service status        |
| **Phase 6** | Testing & Validation           | ⏳ Pending device deployment                                        |
| **Phase 7** | Documentation & Cleanup        | ✅ Complete                                                         |
| **Phase 8** | Dependency Modernization       | ✅ Complete (AGP 9.1.0, Gradle 9.3.1)                               |
| **Phase 9** | Stable M3 Migration            | ✅ Complete (Replaced alpha expressive components)                  |

#### New Architecture (Implemented)

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer (App)                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  ConfigManager (app) → saveConfigInternal()         │   │
│  │         ├── Local File (config.json)                │   │
│  │         ├── XposedPrefs (fallback)                  │   │
│  │         └── syncToAidlService() → ServiceClient     │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                          │ AIDL via ContentProvider
┌─────────────────────────────────────────────────────────────┐
│                     system_server                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  SystemServiceHooker → DeviceMaskerService          │   │
│  │         ├── Config (AtomicReference)                │   │
│  │         ├── Statistics (ConcurrentHashMap)          │   │
│  │         └── Logs (ConcurrentLinkedDeque)            │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                          │ Service queries
┌─────────────────────────────────────────────────────────────┐
│                     Target App Process                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  BaseSpoofHooker.getSpoofValue()                    │   │
│  │         ├── Try service.getSpoofValue() (real-time) │   │
│  │         └── Fallback to XSharedPreferences          │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

#### Key Benefits Achieved

- ✅ Real-time config updates (no app restart needed)
- ✅ Centralized logging in system_server
- ✅ Single LSPosed scope ("android" / System Framework)
- ✅ Filter count statistics per app
- ✅ Service health monitoring (version, uptime, hooked app count)
- ✅ Hybrid fallback to XSharedPreferences for backward compatibility

---

## New Files Created

### Xposed Module (`:xposed`)

| File                             | Purpose                             |
| -------------------------------- | ----------------------------------- |
| `service/DeviceMaskerService.kt` | AIDL service impl (~350 lines)      |
| `service/ConfigManager.kt`       | Atomic file config in `/data/misc/` |
| `service/ServiceBridge.kt`       | ContentProvider for IPC             |
| `hooker/SystemServiceHooker.kt`  | Boot-time service initialization    |

### Common Module (`:common`)

| File                             | Purpose                     |
| -------------------------------- | --------------------------- |
| `aidl/IDeviceMaskerService.aidl` | AIDL interface (15 methods) |

### App Module (`:app`)

| File                       | Purpose                         |
| -------------------------- | ------------------------------- |
| `service/ServiceClient.kt` | UI client for AIDL (~300 lines) |
| `DiagnosticsState.kt`      | Added ServiceStatus data class  |
| `DiagnosticsViewModel.kt`  | Service status integration      |

---

## Build Status

| Module   | Status     | Last Build   |
| -------- | ---------- | ------------ |
| :common  | ✅ SUCCESS | Mar 12, 2026 |
| :xposed  | ✅ SUCCESS | Mar 12, 2026 |
| :app     | ✅ SUCCESS | Mar 12, 2026 |
| Full APK | ✅ SUCCESS | Mar 12, 2026 |

---

## Next Steps

### Immediate (Testing)

1. ⬜ Deploy debug APK to device
2. ⬜ Set LSPosed scope to "System Framework (android)"
3. ⬜ Reboot and verify service initialization
4. ⬜ Test real-time config updates
5. ⬜ Verify hook statistics in Diagnostics screen

### Future Enhancements

- Add Dual-SIM UI section
- Dynamic fingerprint generation
- Cell Info Xposed hooks
- Carrier picker in group creation
- More device presets

---

## Important Files Reference

### AIDL Architecture Files (New)

| File                                    | Purpose                   |
| --------------------------------------- | ------------------------- |
| `common/aidl/IDeviceMaskerService.aidl` | AIDL interface definition |
| `xposed/service/DeviceMaskerService.kt` | Service in system_server  |
| `xposed/service/ConfigManager.kt`       | Config persistence        |
| `xposed/service/ServiceBridge.kt`       | ContentProvider bridge    |
| `xposed/hooker/SystemServiceHooker.kt`  | Boot-time hook            |
| `app/service/ServiceClient.kt`          | UI client                 |

### Sync Architecture Files (Hybrid - Both Active)

| File                        | Purpose                    |
| --------------------------- | -------------------------- |
| `common/SharedPrefsKeys.kt` | Preference keys (shared)   |
| `xposed/PrefsHelper.kt`     | XSharedPrefs fallback      |
| `app/data/XposedPrefs.kt`   | MODE_WORLD_READABLE writes |
| `app/data/ConfigSync.kt`    | Syncs to XposedPrefs       |

### Hook Entry Files

| File                               | Purpose                                       |
| ---------------------------------- | --------------------------------------------- |
| `xposed/XposedEntry.kt`            | Hook loader with loadSystem { } + loadApp { } |
| `xposed/hooker/BaseSpoofHooker.kt` | Hybrid config (service + prefs)               |
| `xposed/hooker/*.kt`               | Individual hookers                            |
