# Spec: Project Identity

This specification defines the project's brand identity, naming conventions, and package structure.

## ADDED Requirements

### Requirement: Brand Identity

The application SHALL be identified with the following brand attributes:

| Attribute | Value |
|-----------|-------|
| App Name | Device Masker |
| Developer Name | AstrixForge |
| Package Name | com.astrixforge.devicemasker |

#### Scenario: App name displayed correctly
- **WHEN** the application is installed on a device
- **THEN** the app name SHALL appear as "Device Masker" in the launcher
- **AND** the app name SHALL appear as "Device Masker" in system settings
- **AND** the app name SHALL appear as "Device Masker" in LSPosed Manager

#### Scenario: Package identification
- **WHEN** the application is queried for its package name
- **THEN** it SHALL return `com.astrixforge.devicemasker`
- **AND** the package name SHALL be used consistently across all Android components

#### Scenario: Developer attribution
- **WHEN** viewing application credits or documentation
- **THEN** the developer name SHALL be displayed as "AstrixForge"

---

### Requirement: Package Structure

The source code SHALL be organized under the package `com.astrixforge.devicemasker` with the following structure:

```
com.astrixforge.devicemasker/
├── DeviceMaskerApp.kt           # Application class
├── data/
│   ├── SpoofDataStore.kt
│   ├── generators/
│   ├── models/
│   └── repository/
├── hook/
│   ├── HookEntry.kt
│   └── hooker/
├── ui/
│   ├── MainActivity.kt
│   ├── components/
│   ├── navigation/
│   ├── screens/
│   └── theme/
└── utils/
```

#### Scenario: All source files use correct package
- **WHEN** any Kotlin source file is examined
- **THEN** the package declaration SHALL be `com.astrixforge.devicemasker` or a subpackage thereof
- **AND** no references to `com.akil.privacyshield` SHALL exist

#### Scenario: Import statements are consistent
- **WHEN** any source file imports project classes
- **THEN** the import statements SHALL use `com.astrixforge.devicemasker.*` paths

---

### Requirement: Theme Naming

The application theme SHALL be named consistently with the new brand.

#### Scenario: Theme resources use new name
- **WHEN** theme resources are defined in `themes.xml`
- **THEN** the theme name SHALL be `Theme.DeviceMasker`

#### Scenario: Theme composable uses new name
- **WHEN** the Compose theme composable is referenced
- **THEN** it SHALL be named `DeviceMaskerTheme`

---

### Requirement: Anti-Detection Package Hiding

The anti-detection module SHALL hide the new package name from detection mechanisms.

#### Scenario: Package hidden from Class.forName
- **WHEN** a target app attempts to load `com.astrixforge.devicemasker` via Class.forName
- **THEN** a ClassNotFoundException SHALL be thrown

#### Scenario: Package hidden from stack traces
- **WHEN** a target app examines stack traces
- **THEN** no stack frames containing `com.astrixforge.devicemasker` SHALL be visible

#### Scenario: Hidden patterns list includes new package
- **WHEN** the HIDDEN_PATTERNS list in AntiDetectHooker is examined
- **THEN** it SHALL include `"com.astrixforge.devicemasker"`

---

### Requirement: Debug Logging Identity

The module's debug logs SHALL use the new brand name.

#### Scenario: YukiHookAPI log tag
- **WHEN** debug logs are written via YukiHookAPI
- **THEN** the log tag SHALL be `"DeviceMasker"`

---

## REMOVED Requirements

### Requirement: PrivacyShield Brand Identity

**Reason**: Replaced by new Device Masker brand identity.

**Migration**: All references to "PrivacyShield", "com.akil.privacyshield", "akil", and "AKIL" must be updated to use the new brand values.

### Requirement: PrivacyShield Theme

**Reason**: Replaced by DeviceMasker theme naming.

**Migration**: Update `Theme.PrivacyShield` → `Theme.DeviceMasker` and `PrivacyShieldTheme` → `DeviceMaskerTheme`.
