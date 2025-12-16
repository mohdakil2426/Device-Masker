# Change: Rebrand to Device Masker

## Why

The project requires a complete rebranding from **PrivacyShield** to **Device Masker** with a new developer identity. This includes changing the app name, package name, and all references to the old brand throughout the codebase, documentation, and configuration files. The rebranding establishes a new identity while maintaining all existing functionality.

## What Changes

### Brand Identity Changes
- **App Name**: `PrivacyShield` → `Device Masker`
- **Developer Name**: `AKIL / akil` → `AstrixForge`
- **Package Name**: `com.akil.privacyshield` → `com.astrixforge.devicemasker`

### Code Changes
- Rename package directory structure from `com/akil/privacyshield/` to `com/astrixforge/devicemasker/`
- Update all `package` declarations in 44 Kotlin source files
- Update all import statements referencing old package
- Rename `PrivacyShieldApp.kt` to `DeviceMaskerApp.kt`
- Rename `PrivacyShieldTheme` composable to `DeviceMaskerTheme`
- Update debug log tag from `"PrivacyShield"` to `"DeviceMasker"`
- Update anti-detection hidden patterns to include new package name

### Configuration Changes
- Update `namespace` and `applicationId` in `app/build.gradle.kts`
- Update `rootProject.name` in `settings.gradle.kts`
- Update theme names in `themes.xml` and `values-night/themes.xml`
- Update app name and description strings in `strings.xml`
- Update application class reference in `AndroidManifest.xml`

### Documentation Changes
- Update `README.md` with new app name, developer, and package references
- Update all memory-bank files (6 files)
- Update OpenSpec project.md and change documentation
- Update any remaining documentation files

## Impact

### Affected Specs
- None (this is a branding change, not a functional change)

### Affected Code
- `app/build.gradle.kts` - namespace, applicationId
- `settings.gradle.kts` - rootProject.name
- `app/src/main/AndroidManifest.xml` - application class, theme references
- `app/src/main/kotlin/com/akil/privacyshield/` - entire source tree (44 files)
- `app/src/main/res/values/strings.xml` - app_name, xposed_description
- `app/src/main/res/values/themes.xml` - Theme.PrivacyShield
- `app/src/main/res/values-night/themes.xml` - Theme.PrivacyShield

### Breaking Changes
- **BREAKING**: Package name change requires uninstall/reinstall of the app
- **BREAKING**: Module must be re-enabled in LSPosed after installation
- **BREAKING**: DataStore preferences will be lost (different package = different data directory)

### Post-Implementation Requirements
- Clean build required (`./gradlew clean`)
- Delete `.idea/`, `.gradle/`, `.kotlin/` cache directories
- Delete `LogsOutput/` containing old package references
- Re-enable module in LSPosed Manager with new package name

## Risks

1. **LSPosed Registration**: After package name change, module needs re-registration
2. **User Data Loss**: Existing spoofed values and profiles will be lost
3. **Anti-Detection**: New package name must be added to hidden patterns immediately
4. **Build Failures**: KSP-generated files reference old package until clean rebuild
