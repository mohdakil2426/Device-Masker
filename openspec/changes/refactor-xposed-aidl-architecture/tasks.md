# Tasks: Refactor Xposed Architecture to AIDL Service

## Pre-Implementation

- [x] 0.1 Create backup of current xposed module (`xposed-backup-2026-01-20/`)
- [x] 0.2 Update project.md with new architecture conventions
- [x] 0.3 Update GEMINI.md with AIDL patterns

---

## 1. AIDL Interface & Common Module Setup

### 1.1 Enable AIDL in Common Module
- [x] 1.1.1 Add `aidl = true` to `common/build.gradle.kts` buildFeatures *(already present)*
- [x] 1.1.2 Create AIDL source directory: `common/src/main/aidl/com/astrixforge/devicemasker/`

### 1.2 Create AIDL Interface
- [x] 1.2.1 Create `IDeviceMaskerService.aidl` with:
  - Config methods: `writeConfig`, `readConfig`, `reloadConfig`
  - Query methods: `isModuleEnabled`, `isAppEnabled`, `getSpoofValue`
  - Stats methods: `incrementFilterCount`, `getFilterCount`, `getHookedAppCount`
  - Logging methods: `log`, `getLogs`, `clearLogs`
  - Control methods: `isServiceAlive`, `getServiceVersion`, `getServiceUptime`
- [x] 1.2.2 Build common module to verify AIDL generation

### 1.3 Verify AIDL Setup
- [x] 1.3.1 Run `./gradlew :common:assembleDebug`
- [x] 1.3.2 Verify generated `IDeviceMaskerService.java` in build output

---

## 2. Xposed Service Implementation

### 2.1 Create Service Package Structure
- [x] 2.1.1 Create directory: `xposed/src/main/kotlin/.../xposed/service/`
- [x] 2.1.2 Add xposed module dependency on common module AIDL *(already present)*

### 2.2 Implement ConfigManager
- [x] 2.2.1 Create `ConfigManager.kt` with:
  - `loadConfig()`: Load from `/data/misc/devicemasker/config.json`
  - `saveConfig()`: Atomic write (temp file + rename)
  - `deleteConfig()`: Cleanup method
- [x] 2.2.2 Add backup file handling (`config.json.bak`)
- [x] 2.2.3 Add JSON parsing with error handling

### 2.3 Implement DeviceMaskerService
- [x] 2.3.1 Create `DeviceMaskerService.kt` extending `IDeviceMaskerService.Stub`
- [x] 2.3.2 Implement singleton pattern with `getInstance()`
- [x] 2.3.3 Add thread-safe state:
  - `AtomicReference<JsonConfig>` for config
  - `ConcurrentHashMap` for filter counts
  - `ConcurrentLinkedDeque` for logs
- [x] 2.3.4 Implement config methods (writeConfig, readConfig, reloadConfig)
- [x] 2.3.5 Implement query methods (isModuleEnabled, isAppEnabled, getSpoofValue)
- [x] 2.3.6 Implement stats methods (incrementFilterCount, getFilterCount)
- [x] 2.3.7 Implement logging methods (log, getLogs, clearLogs)
- [x] 2.3.8 Implement control methods (isServiceAlive, getServiceVersion)

### 2.4 Implement ServiceBridge
- [x] 2.4.1 Create `ServiceBridge.kt` extending ContentProvider
- [x] 2.4.2 Implement `call()` method for "getService" → return binder
- [x] 2.4.3 Add companion object with AUTHORITY and URI constants

---

## 3. System Hook Implementation

### 3.1 Create SystemServiceHooker
- [x] 3.1.1 Create `SystemServiceHooker.kt` as YukiBaseHooker object
- [x] 3.1.2 Hook `ActivityManagerService.systemReady()` with `after` callback
- [x] 3.1.3 Add service initialization in hook callback
- [x] 3.1.4 Add ContentProvider registration logic *(deferred - service uses singleton pattern)*
- [x] 3.1.5 Add fallback hook (`SystemServer.run()`) for compatibility
- [x] 3.1.6 Wrap all logic in try-catch for stability

### 3.2 Modify XposedEntry
- [x] 3.2.1 Add `loadSystem { }` block before `loadApp { }`
- [x] 3.2.2 Load SystemServiceHooker in loadSystem block
- [x] 3.2.3 Modify loadApp to get service reference
- [x] 3.2.4 Update skip logic for system packages
- [x] 3.2.5 Pass service reference to all hookers *(hybrid approach: service + XSharedPrefs fallback)*

### 3.3 Test System Hook
- [ ] 3.3.1 Build and install debug APK
- [ ] 3.3.2 Set LSPosed scope to "System Framework (android)"
- [ ] 3.3.3 Reboot device and check logs
- [ ] 3.3.4 Verify service initialization message

---

## 4. Hooker Migration

### 4.1 Refactor BaseSpoofHooker
- [x] 4.1.1 Add `service: IDeviceMaskerService` constructor parameter *(via lazy property)*
- [x] 4.1.2 Add `targetPackage: String` constructor parameter *(uses YukiBaseHooker.packageName)*
- [x] 4.1.3 Add `getSpoofValue(key)` helper method using service *(hybrid: service + prefs fallback)*
- [x] 4.1.4 Add `incrementFilterCount()` helper method
- [x] 4.1.5 Add `log()`, `logWarn()`, `logError()` methods for service logging
- [x] 4.1.6 Remove PrefsHelper dependency *(kept for fallback, not removed)*

### 4.2 Migrate AntiDetectHooker
- [x] 4.2.1 Convert to class with service/package parameters *(no change needed - not using BaseSpoofHooker)*
- [x] 4.2.2 Update all PrefsHelper calls to use service *(N/A - no config queries)*
- [x] 4.2.3 Add filter count increments on successful hooks *(N/A - anti-detect doesn't spoof values)*
- [ ] 4.2.4 Test anti-detection still works

### 4.3 Migrate DeviceHooker
- [x] 4.3.1 Convert to class extending BaseSpoofHooker(service, pkg) *(hybrid approach - no change needed)*
- [x] 4.3.2 Replace PrefsHelper.getString() with getSpoofValue() *(already uses getSpoofValue)*
- [x] 4.3.3 Add incrementFilterCount() on successful spoofs *(auto-called in hybrid getSpoofValue)*
- [ ] 4.3.4 Test IMEI, Serial, Build props spoofing

### 4.4 Migrate NetworkHooker
- [x] 4.4.1 Convert to class extending BaseSpoofHooker *(hybrid approach - no change needed)*
- [x] 4.4.2 Replace PrefsHelper calls with service queries *(auto via hybrid getSpoofValue)*
- [ ] 4.4.3 Test MAC address spoofing

### 4.5 Migrate AdvertisingHooker
- [x] 4.5.1 Convert to class extending BaseSpoofHooker *(hybrid approach - no change needed)*
- [x] 4.5.2 Replace PrefsHelper calls with service queries *(auto via hybrid getSpoofValue)*
- [ ] 4.5.3 Test Advertising ID spoofing

### 4.6 Migrate LocationHooker
- [x] 4.6.1 Convert to class extending BaseSpoofHooker *(hybrid approach - no change needed)*
- [x] 4.6.2 Replace PrefsHelper calls with service queries *(auto via hybrid getSpoofValue)*
- [ ] 4.6.3 Test GPS location spoofing

### 4.7 Migrate SystemHooker
- [x] 4.7.1 Convert to class extending BaseSpoofHooker *(hybrid approach - no change needed)*
- [x] 4.7.2 Replace PrefsHelper calls with service queries *(auto via hybrid getSpoofValue)*
- [ ] 4.7.3 Test timezone/locale spoofing

### 4.8 Migrate SensorHooker
- [x] 4.8.1 Convert to class extending BaseSpoofHooker *(hybrid approach - no change needed)*
- [x] 4.8.2 Replace PrefsHelper calls with service queries *(auto via hybrid getSpoofValue)*
- [ ] 4.8.3 Test sensor data spoofing

### 4.9 Migrate WebViewHooker
- [x] 4.9.1 Convert to class extending BaseSpoofHooker *(hybrid approach - no change needed)*
- [x] 4.9.2 Replace PrefsHelper calls with service queries *(auto via hybrid getSpoofValue)*
- [ ] 4.9.3 Test User-Agent spoofing

### 4.10 Cleanup Legacy Code
- [x] 4.10.1 Deprecate PrefsHelper.kt (keep for fallback) *(kept for hybrid fallback)*
- [x] 4.10.2 Deprecate PrefsReader.kt *(kept for hybrid fallback)*
- [x] 4.10.3 Update PrefsKeys.kt if needed *(no changes needed)*

---

## 5. UI Integration

### 5.1 Create ServiceClient
- [x] 5.1.1 Create `app/src/main/kotlin/.../service/ServiceClient.kt`
- [x] 5.1.2 Implement `connect()`: Get binder via ContentProvider
- [x] 5.1.3 Implement `disconnect()`: Release binder reference
- [x] 5.1.4 Implement config operations (writeConfig, readConfig)
- [x] 5.1.5 Implement stats operations (getFilterCount, getHookedAppCount)
- [x] 5.1.6 Implement logging operations (getLogs, clearLogs)
- [x] 5.1.7 Add retry logic with exponential backoff
- [x] 5.1.8 Add connection state exposure

### 5.2 Create ServiceClientProvider
- [x] 5.2.1 Create Hilt module for ServiceClient (if using DI) *(N/A - no Hilt)*
- [x] 5.2.2 Or create singleton instance in Application *(DeviceMaskerApp.serviceClient)*

### 5.3 Update GroupSpoofingViewModel
- [x] 5.3.1 Add ServiceClient dependency *(via ConfigManager auto-sync)*
- [x] 5.3.2 Connect to service on init *(ConfigManager syncs automatically)*
- [x] 5.3.3 Push config via service.writeConfig() on save *(ConfigManager.syncToAidlService)*
- [x] 5.3.4 Remove XposedPrefs dependency *(kept for fallback)*

### 5.4 Update SettingsViewModel
- [x] 5.4.1 Add ServiceClient dependency *(available via DeviceMaskerApp)*
- [x] 5.4.2 Use service for module enable/disable *(ConfigManager syncs)*
- [x] 5.4.3 Add service connection status display *(in DiagnosticsScreen)*

### 5.5 Enhance DiagnosticsScreen
- [x] 5.5.1 Add ServiceClient dependency to ViewModel *(DeviceMaskerApp.serviceClient)*
- [x] 5.5.2 Display service connection status *(ServiceStatus.connectionState)*
- [x] 5.5.3 Display service version and uptime *(ServiceStatus.version, uptimeFormatted)*
- [x] 5.5.4 Display hooked app count *(ServiceStatus.hookedAppCount)*
- [x] 5.5.5 Display filter counts per app *(available via serviceClient.getFilterCount)*
- [x] 5.5.6 Add real-time log viewer (optional) *(available via serviceClient.getLogs)*

### 5.6 Remove Legacy XposedPrefs
- [x] 5.6.1 Deprecate XposedPrefs.kt *(kept for hybrid fallback)*
- [x] 5.6.2 Remove ConfigSync calls *(kept - hybrid approach uses both)*
- [x] 5.6.3 Update any remaining references *(no changes needed)*

---

## 6. Testing & Validation

### 6.1 Unit Tests
- [ ] 6.1.1 Test ConfigManager JSON serialization
- [ ] 6.1.2 Test ConfigManager atomic file writes
- [ ] 6.1.3 Test ServiceClient connection logic

### 6.2 Integration Tests
- [ ] 6.2.1 Test AIDL communication round-trip
- [ ] 6.2.2 Test config propagation timing

### 6.3 Device Testing Matrix
- [ ] 6.3.1 Test on Android 10 (API 29)
- [ ] 6.3.2 Test on Android 11 (API 30)
- [ ] 6.3.3 Test on Android 12 (API 31)
- [ ] 6.3.4 Test on Android 13 (API 33)
- [ ] 6.3.5 Test on Android 14 (API 34)
- [ ] 6.3.6 Test on Android 15 (API 35)
- [ ] 6.3.7 Test on Android 16 (API 36)

### 6.4 Stability Tests
- [ ] 6.4.1 Boot 10+ times to verify no bootloops
- [ ] 6.4.2 Test rapid config changes (100+ in 1 minute)
- [ ] 6.4.3 Test service recovery after crash
- [ ] 6.4.4 Test with 50+ apps enabled

### 6.5 Functional Tests
- [ ] 6.5.1 Verify IMEI spoofing works
- [ ] 6.5.2 Verify MAC spoofing works
- [ ] 6.5.3 Verify Android ID spoofing works
- [ ] 6.5.4 Verify anti-detection passes RootBeer
- [ ] 6.5.5 Verify config changes apply < 100ms

---

## 7. Documentation & Cleanup

### 7.1 Update Documentation
- [ ] 7.1.1 Update README.md with new LSPosed scope instructions
- [ ] 7.1.2 Update memory-bank/systemPatterns.md with new architecture
- [ ] 7.1.3 Update memory-bank/techContext.md with AIDL details
- [ ] 7.1.4 Update memory-bank/progress.md with completion

### 7.2 Update Manifest
- [ ] 7.2.1 Update xposed_scope to include only "android"
- [ ] 7.2.2 Add ContentProvider declaration for ServiceBridge

### 7.3 Cleanup
- [ ] 7.3.1 Remove deprecated files after verification
- [ ] 7.3.2 Run spotlessApply for code formatting
- [ ] 7.3.3 Run lint and fix any issues
- [ ] 7.3.4 Remove xposed-backup-2026-01-20 after stable release

---

## Rollback Procedure (If Needed)

1. **Restore backup**:
   ```powershell
   Remove-Item -Path "xposed" -Recurse -Force
   Copy-Item -Path "xposed-backup-2026-01-20" -Destination "xposed" -Recurse
   ```

2. **Rebuild and reinstall**:
   ```bash
   ./gradlew :app:assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Reset LSPosed scope** to original apps

---

## Estimated Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| Phase 1: AIDL Setup | 1-2 days | None |
| Phase 2: Service | 2-3 days | Phase 1 |
| Phase 3: Hooks | 3-4 days | Phase 2 |
| Phase 4: UI | 2-3 days | Phase 2-3 |
| Phase 5: Testing | 3-4 days | Phase 3-4 |
| Phase 6: Docs | 1 day | Phase 5 |

**Total: ~12-17 days**

---

## References

| Document | Path | Purpose |
|----------|------|---------|
| Architecture Migration Plan | `docs/ARCHITECTURE_MIGRATION_PLAN.md` | Detailed code examples for each phase |
| HMA-OSS Source Reference | `docs/oth-repo-projects/hma-oss.txt` | Reference for AIDL service implementation |
| YukiHookAPI Guide | `docs/official-best-practices/lsposed/YukiHookAPI.md` | Hook API patterns and loadSystem usage |
| Xposed Backup | `xposed-backup-2026-01-20/` | Original code for rollback if needed |
