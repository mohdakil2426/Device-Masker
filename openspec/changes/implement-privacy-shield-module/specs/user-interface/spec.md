## ADDED Requirements

### Requirement: Material 3 Expressive Theme

The app SHALL implement Material 3 Expressive design with dynamic colors and AMOLED optimization.

#### Scenario: Dynamic Colors

- **WHEN** the app runs on Android 12+
- **THEN** colors adapt to the system wallpaper (Material You)
- **AND** the user can disable dynamic colors in settings

#### Scenario: AMOLED Dark Mode

- **WHEN** dark theme is enabled
- **THEN** the background is pure black (#000000)
- **AND** surfaces use near-black (#0A0A0A)
- **AND** power savings are maximized on OLED screens

#### Scenario: Spring Animations

- **WHEN** UI elements animate
- **THEN** spring-based animations are used instead of duration-based
- **AND** animations feel natural and responsive

---

### Requirement: Home Screen

The Home Screen SHALL display module status and quick access to key functions.

#### Scenario: Module Status Display

- **WHEN** the user opens the app
- **THEN** they see whether the module is ACTIVE or INACTIVE
- **AND** the count of protected apps is displayed

#### Scenario: Quick Stats

- **WHEN** on the home screen
- **THEN** the current IMEI, Serial, and MAC are displayed (partially masked)
- **AND** a "Regenerate All" button is available

#### Scenario: Active Profile

- **WHEN** on the home screen
- **THEN** the currently active profile name is displayed
- **AND** a "Switch Profile" button is available

---

### Requirement: App Selection Screen

The App Selection Screen SHALL allow users to enable/disable spoofing per app.

#### Scenario: App List Display

- **WHEN** the user navigates to app selection
- **THEN** a scrollable list of installed apps is shown
- **AND** each app shows icon, name, package name, and enabled status

#### Scenario: Enable/Disable Toggle

- **WHEN** the user taps an app's checkbox
- **THEN** spoofing is enabled/disabled for that app
- **AND** the change persists immediately

#### Scenario: Search Functionality

- **WHEN** the user types in the search bar
- **THEN** the app list filters by name or package

#### Scenario: Bulk Actions

- **WHEN** the user taps "Select All" or "Clear All"
- **THEN** all visible apps are enabled or disabled respectively

---

### Requirement: Spoof Settings Screen

The Spoof Settings Screen SHALL allow editing of individual spoofed values.

#### Scenario: Categorized Display

- **WHEN** the user views spoof settings
- **THEN** values are organized by category: Device, Network, Advertising, System, Location

#### Scenario: Value Card

- **WHEN** viewing a spoofed value
- **THEN** the user sees:
  - Label (e.g., "IMEI")
  - Current value
  - Regenerate button (🔄)
  - Edit button (✏️)
  - Copy button (📋)

#### Scenario: Edit Value

- **WHEN** the user taps edit
- **THEN** a dialog appears for manual entry
- **AND** validation feedback is shown for invalid formats

#### Scenario: Regenerate Value

- **WHEN** the user taps regenerate
- **THEN** a new valid value is generated and saved
- **AND** the UI updates immediately

---

### Requirement: Profile Screen

The Profile Screen SHALL manage saved spoof profiles.

#### Scenario: Profile List

- **WHEN** the user navigates to profiles
- **THEN** all saved profiles are displayed
- **AND** the default profile is visually indicated

#### Scenario: Create Profile

- **WHEN** the user taps the FAB (Floating Action Button)
- **THEN** a new profile is created with default values
- **AND** the user can name the profile

#### Scenario: Delete Profile

- **WHEN** the user deletes a profile
- **THEN** a confirmation dialog appears
- **AND** deletion cannot be undone

#### Scenario: Set Default

- **WHEN** the user sets a profile as default
- **THEN** it becomes the fallback for apps without explicit assignment

---

### Requirement: Diagnostics Screen

The Diagnostics Screen SHALL verify spoofing is working correctly.

#### Scenario: Value Comparison

- **WHEN** the user views diagnostics
- **THEN** they see current detected values alongside configured spoof values
- **AND** matches are highlighted in green, mismatches in red

#### Scenario: Detection Tests

- **WHEN** the user runs detection tests
- **THEN** results show whether anti-detection is working
- **AND** any detected Xposed signatures are flagged

---

### Requirement: Settings Screen

The Settings Screen SHALL provide app-level configuration options.

#### Scenario: Theme Settings

- **WHEN** the user accesses settings
- **THEN** they can choose: System Default, Light, Dark, AMOLED Dark

#### Scenario: Dynamic Colors Toggle

- **WHEN** on Android 12+
- **THEN** the user can enable/disable Material You dynamic colors

#### Scenario: Debug Logging

- **WHEN** in debug mode
- **THEN** the user can enable verbose logging for troubleshooting

---

### Requirement: Bottom Navigation

The app SHALL use a bottom navigation bar for primary navigation.

#### Scenario: Navigation Items

- **WHEN** viewing the bottom bar
- **THEN** the following items are visible: Home, Apps, Spoof, Profile

#### Scenario: Active Indicator

- **WHEN** navigating between screens
- **THEN** the active item is highlighted with animation
- **AND** the transition is smooth

---

### Requirement: Responsive Layout

The UI SHALL adapt to different screen sizes and orientations.

#### Scenario: Phone Layout

- **WHEN** running on a phone screen
- **THEN** the layout is optimized for single-column vertical scrolling

#### Scenario: Tablet Layout

- **WHEN** running on a tablet
- **THEN** the layout uses available width efficiently
- **AND** may use multi-column layouts where appropriate
