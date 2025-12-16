# Tasks: Rebrand to Device Masker

## Summary

Complete rebranding of the application from:
- **App Name**: PrivacyShield → Device Masker
- **Developer**: AKIL → AstrixForge  
- **Package**: com.akil.privacyshield → com.astrixforge.devicemasker

---

## Phase 1: Pre-Migration Cleanup

### 1.1 Clean Build Artifacts
- [ ] 1.1.1 Run `./gradlew clean` to remove build directories
- [ ] 1.1.2 Delete `.gradle/` cache directory
- [ ] 1.1.3 Delete `.kotlin/` cache directory
- [ ] 1.1.4 Delete `.idea/` IDE configuration (will be regenerated)
- [ ] 1.1.5 Delete `LogsOutput/` directory containing old package references

**Validation**: No build, cache, or IDE directories present

---

## Phase 2: Package Directory Restructuring

### 2.1 Create New Package Structure
- [ ] 2.1.1 Create directory `app/src/main/kotlin/com/astrixforge/devicemasker/`
- [ ] 2.1.2 Create subdirectory `data/` with all nested folders
- [ ] 2.1.3 Create subdirectory `hook/` with `hooker/` subfolder
- [ ] 2.1.4 Create subdirectory `ui/` with all nested folders
- [ ] 2.1.5 Create subdirectory `utils/`

### 2.2 Move Kotlin Source Files
- [ ] 2.2.1 Move `PrivacyShieldApp.kt` → `DeviceMaskerApp.kt` to new location
- [ ] 2.2.2 Move all files from `data/` to new `data/`
- [ ] 2.2.3 Move all files from `hook/` to new `hook/`
- [ ] 2.2.4 Move all files from `ui/` to new `ui/`
- [ ] 2.2.5 Move all files from `utils/` to new `utils/`
- [ ] 2.2.6 Delete empty old package directory `app/src/main/kotlin/com/akil/`

**Validation**: All 44 Kotlin files exist in new location, old directory deleted

---

## Phase 3: Gradle Configuration Updates

### 3.1 Update app/build.gradle.kts
- [ ] 3.1.1 Change `namespace = "com.akil.privacyshield"` → `"com.astrixforge.devicemasker"`
- [ ] 3.1.2 Change `applicationId = "com.akil.privacyshield"` → `"com.astrixforge.devicemasker"`

### 3.2 Update settings.gradle.kts
- [ ] 3.2.1 Change `rootProject.name = "PrivacyShield"` → `"DeviceMasker"`

**Validation**: Gradle sync succeeds (may have errors until source files updated)

---

## Phase 4: Source Code Updates

### 4.1 Update Package Declarations (all 44 files)
Each file needs `package com.akil.privacyshield.*` → `package com.astrixforge.devicemasker.*`

#### 4.1.1 Root Package
- [ ] `DeviceMaskerApp.kt` - Update package declaration

#### 4.1.2 Data Package (13 files)
- [ ] `SpoofDataStore.kt`
- [ ] `generators/FingerprintGenerator.kt`
- [ ] `generators/IMEIGenerator.kt`
- [ ] `generators/MACGenerator.kt`
- [ ] `generators/SerialGenerator.kt`
- [ ] `generators/UUIDGenerator.kt`
- [ ] `models/AppConfig.kt`
- [ ] `models/DeviceIdentifier.kt`
- [ ] `models/SpoofProfile.kt`
- [ ] `models/SpoofType.kt`
- [ ] `repository/AppScopeRepository.kt`
- [ ] `repository/ProfileRepository.kt`
- [ ] `repository/SpoofRepository.kt`

#### 4.1.3 Hook Package (7 files)
- [ ] `HookEntry.kt` - Also update debug log tag to "DeviceMasker"
- [ ] `hooker/AdvertisingHooker.kt`
- [ ] `hooker/AntiDetectHooker.kt` - **CRITICAL**: Update HIDDEN_PATTERNS to include new package
- [ ] `hooker/DeviceHooker.kt`
- [ ] `hooker/LocationHooker.kt`
- [ ] `hooker/NetworkHooker.kt`
- [ ] `hooker/SystemHooker.kt`

#### 4.1.4 UI Package (22 files)
- [ ] `MainActivity.kt`
- [ ] `components/AppListItem.kt`
- [ ] `components/ProfileCard.kt`
- [ ] `components/SpoofValueCard.kt`
- [ ] `components/StatusIndicator.kt`
- [ ] `components/ToggleButton.kt`
- [ ] `components/package-info.kt`
- [ ] `navigation/BottomNavBar.kt`
- [ ] `navigation/NavDestination.kt`
- [ ] `navigation/package-info.kt`
- [ ] `screens/AppSelectionScreen.kt`
- [ ] `screens/DiagnosticsScreen.kt`
- [ ] `screens/HomeScreen.kt`
- [ ] `screens/ProfileScreen.kt`
- [ ] `screens/SettingsScreen.kt`
- [ ] `screens/SpoofSettingsScreen.kt`
- [ ] `screens/package-info.kt`
- [ ] `theme/Color.kt`
- [ ] `theme/Motion.kt`
- [ ] `theme/Shapes.kt`
- [ ] `theme/Theme.kt` - Also rename `PrivacyShieldTheme` → `DeviceMaskerTheme`
- [ ] `theme/Typography.kt`

#### 4.1.5 Utils Package (1 file)
- [ ] `Constants.kt`

### 4.2 Update Import Statements
- [ ] 4.2.1 Update all imports from `com.akil.privacyshield.*` → `com.astrixforge.devicemasker.*`

### 4.3 Update Class/Function Names
- [ ] 4.3.1 Rename `PrivacyShieldTheme` composable → `DeviceMaskerTheme` in `Theme.kt`
- [ ] 4.3.2 Update `PrivacyShieldTheme` usages in `MainActivity.kt`
- [ ] 4.3.3 Update debug log tag in `HookEntry.kt`: `"PrivacyShield"` → `"DeviceMasker"`

### 4.4 Update Anti-Detection (CRITICAL)
- [ ] 4.4.1 In `AntiDetectHooker.kt`, update HIDDEN_PATTERNS:
  - Change `"com.akil.privacyshield"` → `"com.astrixforge.devicemasker"`

**Validation**: No references to `com.akil.privacyshield` in any Kotlin file

---

## Phase 5: Android Resource Updates

### 5.1 Update strings.xml
- [ ] 5.1.1 Change `app_name` from `"PrivacyShield"` → `"Device Masker"`
- [ ] 5.1.2 Update `xposed_description` to remove "Privacy Shield" reference

### 5.2 Update themes.xml
- [ ] 5.2.1 In `values/themes.xml`: Rename `Theme.PrivacyShield` → `Theme.DeviceMasker`
- [ ] 5.2.2 In `values-night/themes.xml`: Rename `Theme.PrivacyShield` → `Theme.DeviceMasker`

### 5.3 Update AndroidManifest.xml
- [ ] 5.3.1 Change `android:name=".PrivacyShieldApp"` → `".DeviceMaskerApp"`
- [ ] 5.3.2 Change `android:theme="@style/Theme.PrivacyShield"` → `"@style/Theme.DeviceMasker"` (2 occurrences)

**Validation**: grep for "PrivacyShield" in res/ returns no results

---

## Phase 6: Documentation Updates

### 6.1 Update README.md
- [ ] 6.1.1 Replace all "PrivacyShield" → "Device Masker"
- [ ] 6.1.2 Replace all "com.akil.privacyshield" → "com.astrixforge.devicemasker"
- [ ] 6.1.3 Replace "Made with ❤️ by Akil" → "Made with ❤️ by AstrixForge"
- [ ] 6.1.4 Update project structure references

### 6.2 Update GEMINI.md
- [ ] 6.2.1 Update any package or project name references

### 6.3 Update OpenSpec project.md
- [ ] 6.3.1 Replace "PrivacyShield" → "Device Masker"
- [ ] 6.3.2 Replace `com.akil.privacyshield.*` → `com.astrixforge.devicemasker.*`

### 6.4 Update Memory Bank (6 files)
- [ ] 6.4.1 `activeContext.md` - Update all package/name references
- [ ] 6.4.2 `productContext.md` - Update all package/name references
- [ ] 6.4.3 `projectbrief.md` - Update all package/name references
- [ ] 6.4.4 `progress.md` - Update all package/name references
- [ ] 6.4.5 `systemPatterns.md` - Update HIDDEN_PATTERNS reference and package names
- [ ] 6.4.6 `techContext.md` - Update all package/name references

### 6.5 Update OpenSpec Change Documentation
- [ ] 6.5.1 Update `implement-privacy-shield-module/proposal.md` references
- [ ] 6.5.2 Update `implement-privacy-shield-module/design.md` references
- [ ] 6.5.3 Update `implement-privacy-shield-module/tasks.md` references

**Validation**: grep for "PrivacyShield", "akil", "AKIL" across all .md files returns no results (except historical archive)

---

## Phase 7: Final Verification

### 7.1 Search for Remaining References
- [ ] 7.1.1 Run `rg -i "privacyshield" --type-not md` - should return no results
- [ ] 7.1.2 Run `rg "com.akil" --type-not md` - should return no results
- [ ] 7.1.3 Run `rg -i "akil" --type-not md` - should return no results (excluding local.properties path)

### 7.2 Build Verification
- [ ] 7.2.1 Run `./gradlew clean`
- [ ] 7.2.2 Run `./gradlew assembleDebug`
- [ ] 7.2.3 Verify build succeeds without errors

### 7.3 Install and Test
- [ ] 7.3.1 Install APK on test device
- [ ] 7.3.2 Verify app launches with new name "Device Masker"
- [ ] 7.3.3 Enable module in LSPosed Manager (new package: com.astrixforge.devicemasker)
- [ ] 7.3.4 Select target apps in LSPosed scope
- [ ] 7.3.5 Reboot device
- [ ] 7.3.6 Verify spoofing works on target apps
- [ ] 7.3.7 Test anti-detection with RootBeer or similar app

---

## Task Summary

| Phase | Tasks | Description |
|-------|-------|-------------|
| 1 | 5 | Pre-migration cleanup |
| 2 | 7 | Package directory restructuring |
| 3 | 3 | Gradle configuration |
| 4 | 51 | Source code updates |
| 5 | 5 | Android resources |
| 6 | 12 | Documentation |
| 7 | 10 | Verification |
| **Total** | **93** | |

---

## Dependencies

```
Phase 1 ──→ Phase 2 ──→ Phase 3 ──→ Phase 4 ──→ Phase 5 ──→ Phase 6 ──→ Phase 7
(cleanup)   (dirs)      (gradle)   (source)    (res)       (docs)      (verify)
```

All phases must be executed sequentially. Phase 4 (source updates) is the largest and most critical.

## Parallelizable Work

Within Phase 4, the following can be done in parallel:
- 4.1.2 Data files
- 4.1.3 Hook files
- 4.1.4 UI files
- 4.1.5 Utils files

Within Phase 6, all documentation updates can be parallelized.

## Rollback Plan

If any phase fails:
1. `git checkout .` to restore all files
2. Re-run Phase 1 cleanup
3. Start fresh from Phase 2
