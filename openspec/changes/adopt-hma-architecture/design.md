# Design: HMA-OSS Production Architecture

## Context

Device Masker is an LSPosed module for device identifier spoofing. The current single-module architecture using Jetpack DataStore has critical failures (bootloops, crashes). HMA-OSS (Hide My Applist) is a production-grade Xposed module that successfully solves similar cross-process configuration challenges using a 3-module architecture with AIDL IPC.

### Goals
1. Adopt HMA-OSS architecture 100%
2. Keep YukiHookAPI as hook framework
3. Keep Material 3 Expressive UI
4. Keep all existing spoof types
5. Eliminate all DataStore/SharedPreferences complexity

### Non-Goals
1. Copy HMA-OSS code directly (we adapt patterns)
2. Change UI design
3. Add new spoof types (scope creep)
4. Multi-user support (future scope)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    DEVICE MASKER - HMA-OSS ARCHITECTURE                      │
│                           with YukiHookAPI                                   │
└─────────────────────────────────────────────────────────────────────────────┘

                         ┌─────────────────────┐
                         │   Single APK Build  │
                         └──────────┬──────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            │                       │                       │
            ▼                       ▼                       ▼
     ┌─────────────┐         ┌─────────────┐         ┌─────────────┐
     │    :app     │         │   :common   │         │   :xposed   │
     │  (UI App)   │         │  (Shared)   │         │  (Hooks)    │
     ├─────────────┤         ├─────────────┤         ├─────────────┤
     │             │         │             │         │             │
     │ ConfigMgr   │────────►│ AIDL Iface  │◄────────│ Service     │
     │ ServiceClnt │         │ JsonConfig  │         │ YukiHookers │
     │ UI Screens  │         │ Models      │         │ Entry Point │
     │             │         │             │         │             │
     └─────────────┘         └─────────────┘         └─────────────┘
            │                       △                       │
            │                       │                       │
            │                       │                       │
            └───────────────────────┴───────────────────────┘
                        All modules depend on :common
```

---

## Module Design

### Module 1: `:common` (Android Library)

**Purpose**: Shared code between UI app and xposed module

**Contents**:
```
common/src/main/
├── aidl/com/astrixforge/devicemasker/common/
│   └── IDeviceMaskerService.aidl
└── kotlin/com/astrixforge/devicemasker/common/
    ├── Constants.kt
    ├── JsonConfig.kt
    ├── SpoofProfile.kt
    ├── SpoofType.kt
    ├── SpoofCategory.kt
    └── Utils.kt
```

**Dependencies**:
- `kotlinx-serialization-json`
- No Android UI dependencies

**Key Design**:
- AIDL interface defines all IPC methods
- `JsonConfig` is `@Serializable` and holds entire app config
- Models are shared to ensure consistency

---

### Module 2: `:app` (Android Application)

**Purpose**: User interface and local configuration management

**Contents**:
```
app/src/main/kotlin/com/astrixforge/devicemasker/
├── DeviceMaskerApp.kt
├── service/
│   ├── ConfigManager.kt          # Local JSON file + AIDL sync
│   ├── ServiceClient.kt          # AIDL client proxy
│   └── ServiceProvider.kt        # ContentProvider for binder
└── ui/
    ├── MainActivity.kt
    ├── screens/
    │   ├── HomeScreen.kt
    │   ├── ProfileScreen.kt
    │   ├── ProfileDetailScreen.kt
    │   ├── SettingsScreen.kt
    │   └── DiagnosticsScreen.kt
    ├── components/
    │   ├── common/
    │   └── expressive/
    └── theme/
```

**Dependencies**:
- `:common` module
- Jetpack Compose
- Material 3
- Navigation

**Storage**:
- Location: `/data/data/com.astrixforge.devicemasker/files/config.json`
- Format: JSON text
- Access: `File.readText()` / `File.writeText()`

**Key Components**:

| Component | Role |
|-----------|------|
| `ConfigManager` | Read/write local config, sync via ServiceClient |
| `ServiceClient` | AIDL proxy, implements `IDeviceMaskerService` |
| `ServiceProvider` | ContentProvider that receives binder from xposed |

---

### Module 3: `:xposed` (Android Library)

**Purpose**: YukiHookAPI entry point and system service

**Contents**:
```
xposed/src/main/
├── assets/
│   └── xposed_init
└── kotlin/com/astrixforge/devicemasker/xposed/
    ├── XposedEntry.kt              # @InjectYukiHookWithXposed
    ├── DeviceMaskerService.kt      # IDeviceMaskerService.Stub
    ├── UserService.kt              # Service registration
    ├── Logcat.kt                   # Safe logging
    └── hooker/
        ├── AntiDetectHooker.kt
        ├── DeviceHooker.kt
        ├── NetworkHooker.kt
        ├── AdvertisingHooker.kt
        ├── LocationHooker.kt
        └── SystemHooker.kt
```

**Dependencies**:
- `:common` module
- YukiHookAPI
- Xposed API (compileOnly)

**Storage**:
- Location: `/data/system/devicemasker/config.json`
- Format: JSON text
- Access: In-memory `JsonConfig` object

**Key Components**:

| Component | Role |
|-----------|------|
| `XposedEntry` | YukiHookAPI entry, loads system hooks |
| `DeviceMaskerService` | AIDL server, holds in-memory config |
| `UserService` | Registers service with ServiceManager |
| `hooker/*` | All YukiBaseHooker implementations |

---

## AIDL Interface Design

### IDeviceMaskerService.aidl

```
interface IDeviceMaskerService {
    // Service management
    int getServiceVersion();
    void stopService(boolean cleanEnv);
    
    // Configuration
    String readConfig();
    void writeConfig(String json);
    
    // Logging
    String[] getLogs();
    void clearLogs();
    void log(int level, String tag, String message);
    
    // Status
    boolean isModuleEnabled();
    int getHookCount();
    
    // Direct value access (optional optimization)
    String getSpoofValue(String packageName, String spoofType);
}
```

---

## Data Flow Design

### Flow 1: UI Saves Config

```
User edits profile in UI
         │
         ▼
ConfigManager.updateProfile(profile)
         │
         ├──► config object updated
         │
         ▼
ConfigManager.saveConfig()
         │
         ├──► configFile.writeText(config.toString())  [local backup]
         │
         └──► ServiceClient.writeConfig(json)  [AIDL to xposed]
                      │
                      ▼
              DeviceMaskerService.writeConfig(json)
                      │
                      ├──► config = JsonConfig.parse(json)  [in-memory]
                      │
                      └──► configFile.writeText(json)  [system backup]
                                │
                                ▼
                      All hooks immediately see new config
```

### Flow 2: System Boot

```
Device boots
         │
         ▼
Zygote starts, LSPosed loads modules
         │
         ▼
XposedEntry.onHook() executes
         │
         ▼
loadSystem { } block runs in system_server
         │
         ▼
UserService.register() called
         │
         ▼
DeviceMaskerService instance created
         │
         ├──► searchDataDir()  [find/create /data/system/devicemasker/]
         │
         ├──► loadConfig()  [read JSON into memory]
         │
         └──► installHooks()  [load all YukiBaseHookers]
                      │
                      ▼
              Service registered with ServiceManager
                      │
                      ▼
              Hooks now active, read from in-memory config
```

### Flow 3: UI Connects to Service

```
UI app starts (user opens app)
         │
         ▼
ServiceProvider.onCreate()
         │
         ▼
DeviceMaskerService detects app UID active
         │
         ▼
Service sends binder via ActivityManager hook
         │
         ▼
ServiceProvider.call() receives binder in extras
         │
         ▼
ServiceClient.linkService(binder)
         │
         ▼
UI can now call ServiceClient methods
```

---

## Migration Strategy

### Phase 1: Create Multi-Module Structure
1. Create `common/`, `xposed/` directories
2. Update `settings.gradle.kts` to include all modules
3. Create `build.gradle.kts` for each module

### Phase 2: Move Shared Code to `:common`
1. Create AIDL interface
2. Move models (SpoofProfile, SpoofType, etc.)
3. Create JsonConfig with serialization

### Phase 3: Implement `:xposed` Module
1. Create XposedEntry with YukiHookAPI
2. Implement DeviceMaskerService
3. Move all hookers from current location
4. Adapt hookers to read from in-memory config

### Phase 4: Refactor `:app` Module
1. Remove DataStore dependency
2. Implement ConfigManager (JSON file storage)
3. Implement ServiceClient and ServiceProvider
4. Update UI to use ConfigManager

### Phase 5: Data Migration
1. On first launch after update
2. Read old DataStore data
3. Convert to JsonConfig format
4. Save as new JSON file
5. Delete old DataStore files

### Phase 6: Testing
1. Clean build and install
2. Verify module loads in LSPosed
3. Verify service starts in system_server
4. Verify UI connects via AIDL
5. Verify hooks work correctly
6. Test anti-detection

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Build fails with 3 modules | High | Follow HMA-OSS gradle config exactly |
| AIDL generation issues | Medium | Test AIDL in isolation first |
| Service not starting | High | Add extensive logging in XposedEntry |
| Binder not received by UI | Medium | Debug ContentProvider carefully |
| YukiHookAPI + system_server | Medium | Use `loadSystem {}` correctly |
| Data migration fails | Medium | Keep backup of old DataStore |

---

## File Mapping (Old → New)

| Old Location | New Location | Action |
|-------------|--------------|--------|
| `app/.../DeviceMaskerApp.kt` | `app/.../DeviceMaskerApp.kt` | Update |
| `app/.../data/models/*.kt` | `common/.../common/*.kt` | Move |
| `app/.../data/SpoofDataStore.kt` | (deleted) | Remove |
| `app/.../data/generators/*.kt` | `app/.../generator/*.kt` | Keep in app |
| `app/.../hook/HookEntry.kt` | `xposed/.../XposedEntry.kt` | Rewrite |
| `app/.../hook/hooker/*.kt` | `xposed/.../hooker/*.kt` | Move + adapt |
| `app/.../ui/**/*.kt` | `app/.../ui/**/*.kt` | Unchanged |
| (new) | `app/.../service/ConfigManager.kt` | Create |
| (new) | `app/.../service/ServiceClient.kt` | Create |
| (new) | `app/.../service/ServiceProvider.kt` | Create |
| (new) | `xposed/.../DeviceMaskerService.kt` | Create |
| (new) | `common/.../IDeviceMaskerService.aidl` | Create |

---

## Questions Resolved

| Question | Decision |
|----------|----------|
| Keep YukiHookAPI? | Yes, better Kotlin DSL than raw Xposed |
| Keep Material 3 UI? | Yes, UI layer unchanged |
| Remove DataStore? | Yes, replace with JSON files |
| Use XSharedPreferences? | No, use AIDL for all IPC |
| Multi-user support? | No, future scope |
| Backward data migration? | Yes, migrate on first launch |

---

## References

- [HMA-OSS Source Code](https://github.com/frknkrc44/HMA-OSS)
- [YukiHookAPI Documentation](https://highcapable.github.io/YukiHookAPI/en/)
- [Android AIDL Guide](https://developer.android.com/develop/background-work/services/aidl)
