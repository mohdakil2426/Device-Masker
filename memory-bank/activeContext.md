# Active Context: PrivacyShield

## Current Work Focus

### Active Change: `implement-privacy-shield-module`

**Status**: ✅ Phase 1 Complete / 🔄 Phase 2 In Progress
**Location**: `openspec/changes/implement-privacy-shield-module/`
**Current Phase**: Phase 2 - Device Spoofing
**Next Action**: Implement `DeviceHooker` and `DeviceGenerators`

### What's Being Built

Complete implementation of PrivacyShield LSPosed module from empty Android Studio scaffold:

1. **Core Infrastructure** - Build config, manifest, hook entry ✅ **DONE**
2. **Device Spoofing** - 24+ device identifier hooks ← **CURRENT**
3. **Anti-Detection** - Stack trace/ClassLoader/proc maps hiding
4. **Data Management** - Profiles, per-app config, generators
5. **User Interface** - Material 3 Expressive with 6 screens

## Recent Changes

### December 15, 2025

| Time | Change | Status |
|------|--------|--------|
| 10:45 | Resolved Java 25 / Gradle Build Issues | ✅ |
| 10:50 | Upgraded Gradle to 9.1.0 | ✅ |
| 10:55 | Added Foojay Toolchain Resolver (1.0.0) | ✅ |
| 11:00 | Verified LSPosed API dependency (repo.lsposed.foundation) | ✅ |
| 11:05 | Successful `assembleDebug` build | ✅ |
| 11:30 | Modernized `app/build.gradle.kts` (deprecated options removed) | ✅ |
| 12:00 | Created YukiHookAPI research documentation | ✅ |
| 12:00 | Created `.cursorrules` file for YukiHookAPI development | ✅ |
| 12:00 | Created `docs/YukiHookAPI_Reference.md` comprehensive guide | ✅ |

### December 14, 2025

| Time | Change | Status |
|------|--------|--------|
| 18:44 | Started Phase 1 implementation | ✅ |
| 18:45 | Updated `gradle/libs.versions.toml` with all dependencies | ✅ |
| 18:45 | Updated root `build.gradle.kts` with plugins | ✅ |
| 18:45 | Updated `settings.gradle.kts` with Xposed repo | ✅ |
| 18:46 | Rewrote `app/build.gradle.kts` | ✅ |

### Files Created in Phase 1

```
app/src/main/kotlin/com/akil/privacyshield/
├── PrivacyShieldApp.kt          ✅ Created
├── hook/
│   ├── HookEntry.kt             ✅ Created
│   └── hooker/
│       └── package-info.kt      ✅ Placeholder
├── data/
│   ├── package-info.kt          ✅ Placeholder
│   ├── models/
│   │   └── package-info.kt      ✅ Placeholder
│   └── generators/
│       └── package-info.kt      ✅ Placeholder
├── ui/
│   ├── MainActivity.kt          ✅ Created
├── ui/theme/                    ✅ Created (Color, Type, Theme, etc.)
└── utils/
    └── Constants.kt             ✅ Created

app/src/main/res/values/
├── arrays.xml                   ✅ Created (xposed_scope)
├── strings.xml                  ✅ Updated (all app strings)
└── themes.xml                   ✅ Updated (Material3 theme)
```

## Next Steps

### Immediate (Phase 2)

1. **Create Generators**: Implement `IMEIGenerator`, `MacAddressGenerator`, etc.
2. **Implement DeviceHooker**: Use YukiHookAPI to hook `TelephonyManager`, `Build`, `Settings.Secure`.
3. **Connect Hooks**: Register `DeviceHooker` in `HookEntry`. 

### Short-Term

4. Test spoofing on physical device
5. Verify values availability in target apps

## Active Decisions & Considerations

### Decision: Upgrade to Gradle 9.1.0

**Rationale**: The host environment is running Java 25. Older Gradle versions (8.x) cannot run on Java 25.

**Resolution**: 
- Upgraded Gradle Wrapper to 9.1.0.
- Added `org.gradle.toolchains.foojay-resolver-convention` plugin (v1.0.0) to handle Java 21 toolchain provisioning automatically.

### Decision: API Selection

**Rationale**: Both standard Xposed API and YukiHookAPI are needed.
- **Xposed API (`de.robv.android.xposed:api:82`)**: The fundamental interface for the LSPosed framework.
- **YukiHookAPI**: The high-level Kotlin wrapper for writing hooks efficiently.

**Status**: Verified dependencies. Both are correctly configured and building.

### Issue: Build Failure (RESOLVED)

**Status**: ✅ Fixed
**Resolution**: Toolchain issue resolved by Foojay plugin and Gradle upgrade. Build is now successful (`EXIT CODE 0`).

## Important Patterns & Preferences

### Code Style Preferences

- **Package**: `com.akil.privacyshield`
- **Source Set**: `kotlin/` (not `java/`)
- **Naming**: `*Hooker.kt`, `*Generator.kt`, `*Screen.kt`
- **Theme**: AMOLED black, Teal/Cyan primary

### Architecture Preferences

- **Hook Layer**: YukiBaseHooker per domain with `loadHooker()`
- **Data Layer**: DataStore preferences (Phase 4)
- **UI Layer**: Jetpack Compose + Material 3 (Phase 5)
- **Entry Point**: `@InjectYukiHookWithXposed` annotation

## Files to Watch

| File | Reason |
|------|--------|
| `gradle/libs.versions.toml` | Dependency versions |
| `app/build.gradle.kts` | Build configuration |
| `hook/HookEntry.kt` | Module entry point |
| `PrivacyShieldApp.kt` | Application class |
| `ui/theme/Theme.kt` | Material 3 theme definition |
