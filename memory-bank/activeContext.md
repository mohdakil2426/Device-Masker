# Active Context: PrivacyShield

## Current Work Focus

### Active Change: `implement-privacy-shield-module`

**Status**: ✅ Proposal Created & Validated  
**Location**: `openspec/changes/implement-privacy-shield-module/`  
**Tasks**: 0/136 completed  
**Next Action**: Await approval, then begin Phase 1 implementation

### What's Being Built

Complete implementation of PrivacyShield LSPosed module from empty Android Studio scaffold:

1. **Core Infrastructure** - Build config, manifest, hook entry
2. **Device Spoofing** - 24+ device identifier hooks
3. **Anti-Detection** - Stack trace/ClassLoader/proc maps hiding
4. **Data Management** - Profiles, per-app config, generators
5. **User Interface** - Material 3 Expressive with 6 screens

## Recent Changes

### December 14, 2025

| Time | Change | Status |
|------|--------|--------|
| 18:27 | Created OpenSpec change `implement-privacy-shield-module` | ✅ |
| 18:27 | Created `proposal.md` with 6 phases | ✅ |
| 18:27 | Created `design.md` with 7 architectural decisions | ✅ |
| 18:27 | Created `tasks.md` with 136 tasks | ✅ |
| 18:27 | Created 5 spec delta files (core, device, anti-detect, data, ui) | ✅ |
| 18:30 | Updated `openspec/project.md` with PrivacyShield context | ✅ |
| 18:31 | Validated proposal with `openspec validate --strict` | ✅ |
| 18:37 | Setting up Memory Bank | 🔄 In Progress |

### Project State Before Changes

- Empty Android Studio project scaffolded
- Basic `build.gradle.kts` with Android Gradle Plugin 8.13.2, Kotlin 2.0.21
- Minimal `AndroidManifest.xml` (no LSPosed metadata)
- Empty source directory at `com/akil/privacyshield/`
- PRD documents present at `docs/prd/`
- OpenSpec directory initialized but empty

## Next Steps

### Immediate (After Approval)

1. **Phase 1.1**: Update `gradle/libs.versions.toml` with all dependencies
2. **Phase 1.1**: Update root and app `build.gradle.kts`
3. **Phase 1.1**: Add Xposed repository to `settings.gradle.kts`
4. **Phase 1.2**: Update `AndroidManifest.xml` with LSPosed metadata
5. **Phase 1.3**: Create source directory structure
6. **Phase 1.3**: Create `PrivacyShieldApp.kt`
7. **Phase 1.4**: Create `HookEntry.kt`
8. Verify module appears in LSPosed Manager

### Short-Term (Week 1-2)

- Complete Phase 1 (Core Infrastructure)
- Begin Phase 2 (Device Spoofing Hooks)
- Implement value generators (IMEI, Serial, MAC)
- Test basic hooking with device info app

### Medium-Term (Week 2-4)

- Complete all spoofing hooks
- Implement anti-detection layer
- Set up data persistence

### Long-Term (Week 4-8)

- Build complete UI
- Testing and polish
- Documentation

## Active Decisions & Considerations

### Decision: Start with Build Configuration

**Rationale**: Without proper build configuration, no code can compile. Dependencies like YukiHookAPI, Compose, and DataStore are prerequisites for all subsequent work.

**Order of Operations**:
1. libs.versions.toml (dependency versions)
2. settings.gradle.kts (repositories)
3. build.gradle.kts files (plugins and dependencies)
4. AndroidManifest.xml (LSPosed metadata)
5. Source code

### Decision: Use kotlin/ Directory Instead of java/

**Rationale**: Project is 100% Kotlin. Using `kotlin/` source set is more idiomatic and clearly signals the language choice.

**Implementation**: Create `app/src/main/kotlin/com/akil/privacyshield/` structure

### Consideration: Kotlin 2.2.21 vs Available Version

**Issue**: PRD specifies Kotlin 2.2.21 which may not exist yet (December 2025 projection). May need to use latest available version.

**Resolution**: Will check available versions during implementation. Use 2.1.x or 2.2.x as available.

### Consideration: Compose BOM 2025.12.00

**Issue**: PRD specifies December 2025 Compose BOM. May not exist if implementing before that date.

**Resolution**: Use latest stable Compose BOM available. Update when 2025.12.00 releases.

## Important Patterns & Preferences

### Code Style Preferences

- **Package**: `com.akil.privacyshield`
- **Naming**: `*Hooker.kt`, `*Generator.kt`, `*Screen.kt`, `*ViewModel.kt`
- **State**: Immutable `data class` with `.copy()`
- **Async**: Coroutines with `StateFlow`
- **DI**: Manual dependency injection (no Hilt/Koin for simplicity)

### Architecture Preferences

- **Hook Layer**: One `YukiBaseHooker` per domain
- **Data Layer**: Repository pattern with DataStore
- **UI Layer**: MVVM with Compose
- **Navigation**: Compose Navigation with sealed class destinations

### UI/UX Preferences

- **Theme**: AMOLED first (pure black background)
- **Colors**: Teal/Cyan primary (privacy theme)
- **Motion**: Spring animations, not duration-based
- **Density**: Comfortable touch targets, clear spacing

## Learnings & Project Insights

### YukiHookAPI Insights

1. **Entry annotation** `@InjectYukiHookWithXposed` generates Xposed entry class
2. **Hook order matters** - load anti-detection first
3. **Method overloads** - use `paramCount` to distinguish
4. **Optional methods** - use `.optional()` for uncertain APIs
5. **DataChannel** for cross-process communication (module ↔ host)

### LSPosed Insights

1. **Module scope** - define in `xposed_scope` array resource
2. **Min version** - set to 82 for modern LSPosed
3. **Hooks run in target process** - not module process
4. **Reboot often required** - or soft reboot for some changes

### Material 3 Insights

1. **Dynamic colors** - only Android 12+ (`Build.VERSION.SDK_INT >= 31`)
2. **AMOLED black** - override background/surface in dark scheme
3. **Spring animations** - `spring()` instead of `tween()`

### Value Generation Insights

1. **IMEI** - 15 digits, Luhn checksum, realistic TAC prefix
2. **MAC** - unicast bit (clear LSB of first byte)
3. **Fingerprint** - complex format: `brand/device/device:version/buildid:type/keys`
4. **Android ID** - 16 hex characters
5. **Advertising ID** - UUID format (8-4-4-4-12)

## Open Questions

1. **Q: Should we support import/export in v1.0?**
   - A: PRD marks as "Low" priority. Defer to v1.1.

2. **Q: How to handle hot-reload of profile changes?**
   - A: Use YukiHookAPI DataChannel. App restart as fallback.

3. **Q: Test on emulator or physical device?**
   - A: Physical device required (Magisk/LSPosed don't work on most emulators).

## Files to Watch

| File | Reason |
|------|--------|
| `app/build.gradle.kts` | Main build configuration |
| `gradle/libs.versions.toml` | Dependency versions |
| `app/src/main/AndroidManifest.xml` | LSPosed metadata |
| `hook/HookEntry.kt` | Module entry point |
| `hook/hooker/AntiDetectHooker.kt` | Must load first |
| `data/SpoofDataStore.kt` | Central data access |
| `ui/theme/Theme.kt` | Material 3 theme definition |
