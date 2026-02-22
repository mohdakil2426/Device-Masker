# Change: Refactor Xposed Architecture to System-Wide AIDL Service

## Why

The current Device Masker Xposed architecture uses **XSharedPreferences** for configuration delivery, which has significant limitations:

1. **No real-time updates**: Configuration changes require target app restart
2. **Per-app overhead**: Hooks load separately in each app process
3. **Limited IPC**: Can only read preferences, cannot call methods
4. **Complex LSPosed scope**: Users must manually select each target app
5. **No centralized logging**: Each app logs independently

This proposal adopts the **HMA-OSS (Hide My Applist)** production-proven architecture pattern:
- **System-wide framework hooking** via single "android" LSPosed scope
- **AIDL Binder service** running in `system_server` for real-time IPC
- **Centralized configuration** with instant updates

## What Changes

### **BREAKING** Changes
- LSPosed scope changes from multiple apps → single "android" (System Framework)
- Configuration storage moves from `shared_prefs/` → `/data/misc/devicemasker/config.json`
- XSharedPreferences removed, replaced by AIDL service queries

### New Components
- **IDeviceMaskerService.aidl**: AIDL interface for IPC
- **DeviceMaskerService**: Singleton service running in `system_server`
- **ConfigManager**: File-based configuration persistence
- **ServiceBridge**: ContentProvider for service discovery
- **SystemServiceHooker**: System boot hook for service initialization
- **ServiceClient**: UI-side service client

### Modified Components
- **XposedEntry.kt**: Add `loadSystem { }` for system framework hooking
- **BaseSpoofHooker.kt**: Service-aware base class with binder access
- **All 8 Hookers**: Migrate from PrefsHelper to service queries
- **ViewModels**: Use ServiceClient instead of XposedPrefs

### Removed Components
- **PrefsHelper.kt**: Replaced by service queries
- **XposedPrefs.kt**: Replaced by ServiceClient
- **PrefsReader.kt**: No longer needed

## Impact

### Affected Specs
- `core-infrastructure`: Hook entry point requirements change
- `xposed-architecture` (new): New spec for AIDL architecture

### Affected Code
- `xposed/` module: Major refactor (~80% of files)
- `app/` module: ViewModel updates, new ServiceClient
- `common/` module: Add AIDL interface

### User Impact
- **Positive**: Real-time config updates, simpler LSPosed setup
- **Negative**: Requires LSPosed scope reconfiguration after update

### Risk Level
- **High**: Modifying `system_server` can cause bootloops
- **Mitigations**: Extensive try-catch, rollback plan, backup folder

## Dependencies

- YukiHookAPI 1.3.1 (supports `loadSystem { }`)
- LSPosed API 82+ (system framework hooking)
- Backup created: `xposed-backup-2026-01-20/`

## References

- [HMA-OSS Repository](https://github.com/frknkrc44/HMA-OSS)
- [Architecture Migration Plan](../../../docs/ARCHITECTURE_MIGRATION_PLAN.md) - Detailed implementation plan with code examples
- [HMA-OSS Source Reference](../../../docs/oth-repo-projects/hma-oss.txt) - Full HMA-OSS codebase for architecture reference
- [YukiHookAPI loadSystem docs](https://highcapable.github.io/YukiHookAPI/en/guide/example)
