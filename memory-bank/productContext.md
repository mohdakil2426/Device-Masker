# Product Context: Device Masker

## Why This Project Exists

### The Problem

Modern Android applications extensively collect device identifiers for:
- **User Tracking**: Building groups across apps and sessions
- **Device Fingerprinting**: Creating unique identifiers even without explicit permissions
- **Multi-Account Detection**: Preventing users from having multiple accounts
- **Ban Evasion Detection**: Tracking devices across account changes
- **Analytics**: Understanding device demographics

These identifiers include IMEI, Serial Number, MAC Address, Android ID, Advertising ID, and many more. Even privacy-conscious users have limited control over this information leakage.

### The Solution

Device Masker intercepts system API calls that retrieve device identifiers and returns user-configured spoofed values instead. This allows users to:
- Present a different device identity to each app
- Regenerate identifiers at will
- Protect against device fingerprinting
- Maintain privacy without removing legitimate app functionality

### Why LSPosed/Xposed?

LSPosed (built on the Xposed framework) allows modifying app behavior without:
- Modifying APK files (no signature breaking)
- Root detection in most apps (with companion modules)
- Breaking app updates

This makes it ideal for privacy-focused modifications that need to work across many apps.

## Problems It Solves

| Problem | Solution |
|---------|----------|
| Apps track device across reinstalls | Spoof Android ID, Advertising ID, GSF ID |
| Apps detect multi-accounts by IMEI | Spoof IMEI per group |
| Location-based fingerprinting | Spoof GPS, Timezone, Locale |
| Hardware fingerprinting | Spoof Build properties, Serial |
| Network fingerprinting | Spoof MAC Address, SSID, BSSID |
| Xposed detection breaks app | Anti-detection layer hides hooks |

## How It Should Work

### User Journey

1. **Install & Enable**
   - User installs Device Masker APK
   - Opens LSPosed Manager, enables the module
   - Reboots device (or soft reboot for LSPosed)

2. **Configure Apps**
   - Open Device Masker app
   - Go to "Apps" screen
   - Select apps to protect (enable spoofing)
   - Optionally assign specific groups to apps

3. **Manage Groups**
   - Create named groups with different identities
   - Set one group as default
   - Assign groups to specific apps if needed

4. **Customize Values**
   - View/edit individual spoofed values
   - Regenerate values with one tap
   - Copy values for reference

5. **Verify Protection**
   - Use Diagnostics screen to verify spoofing
   - Check that detection apps don't find hooks

### Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     TARGET APP PROCESS                       │
│                                                              │
│   App Code → TelephonyManager.getImei()                     │
│                    │                                         │
│                    ▼                                         │
│   ┌─────────────────────────────────────────────────────┐   │
│   │            LSPOSED HOOK INTERCEPTION                 │   │
│   │                                                      │   │
│   │   1. AntiDetectHooker (runs first)                   │   │
│   │      └─→ Hides Xposed presence                       │   │
│   │                                                      │   │
│   │   2. DeviceHooker                                    │   │
│   │      └─→ Intercepts getImei()                        │   │
│   │      └─→ Reads from XSharedPreferences (prefs)       │   │
│   │      └─→ Returns spoofed IMEI                        │   │
│   └─────────────────────────────────────────────────────┘   │
│                    │                                         │
│                    ▼                                         │
│   App receives "358673912845672" (spoofed)                  │
└─────────────────────────────────────────────────────────────┘
               ↑ XSharedPreferences reads config
┌─────────────────────────────────────────────────────────────┐
│                       APP UI PROCESS                         │
│                                                              │
│   User configures spoofing → ConfigManager.saveConfig()     │
│                    ↓                                         │
│   1. Local file (config.json)                               │
│   2. ConfigSync → XposedPrefs (fallback)                    │
│   3. syncToAidlService() → DeviceMaskerService              │
│                                                              │
│   Config changes apply in real-time via AIDL (Jan 2026)     │
└─────────────────────────────────────────────────────────────┘
```

**Real-Time Updates (Jan 2026)**: The AIDL service in system_server now provides
instant config updates without requiring target app restart.

## User Experience Goals

### Primary UX Goals

1. **Simplicity First**
   - Default "just works" with minimal configuration
   - One-tap enable per app
   - One-tap regenerate all values

2. **Visual Clarity**
   - Clear status indicators (Active/Inactive)
   - Organized by category (Device, Network, Advertising, etc.)
   - Masked values for security on home screen

3. **Modern Feel**
   - Material 3 Expressive design language
   - Dynamic colors that adapt to wallpaper
   - Smooth spring-based animations (10 expressive components)
   - AMOLED pure black for battery efficiency

4. **Power User Options**
   - Per-app group assignment
   - Selective spoof types per app
   - Custom value entry with validation
   - Detailed diagnostics

### UI/UX Principles

| Principle | Implementation |
|-----------|----------------|
| **Immediate Feedback** | Changes save instantly, UI updates immediately |
| **Error Prevention** | Validate values before saving, show format hints |
| **Recognition over Recall** | Show current values, use icons for actions |
| **Flexibility** | Support both quick setup and detailed customization |
| **Aesthetic Integrity** | Consistent use of Material 3 components and motion |

## Target Audience Personas

### 1. Privacy-Conscious User (Primary)
- **Goal**: Protect device identity from apps
- **Technical Level**: Basic to intermediate
- **Usage**: Enable for social media, shopping apps
- **Needs**: Simple setup, group switching, clear status

### 2. Security Researcher
- **Goal**: Test app behavior with different identities
- **Technical Level**: Advanced
- **Usage**: Multiple groups, frequent regeneration
- **Needs**: Valid value formats, quick group switching

### 3. Developer/Learner
- **Goal**: Understand Xposed hooking and Android security
- **Technical Level**: Advanced
- **Usage**: Study code, extend functionality
- **Needs**: Clean architecture, good documentation

## Competition & Differentiation

### Similar Modules
- **Device ID Masker**: Basic spoofing, limited identifiers
- **XPrivacy/XPrivacyLua**: Comprehensive but complex, heavy
- **DeviceFaker**: Outdated, limited Android version support

### Device Masker Differentiators
1. **Modern Stack**: YukiHookAPI, Jetpack Compose, Material 3
2. **Anti-Detection Built-in**: Not just spoofing, but hiding the hooks
3. **Android 16 Support**: Latest API level compatibility
4. **Beautiful UI**: Modern design, not utilitarian
5. **Focused Scope**: Does spoofing well, doesn't try to do everything
6. **Active Development**: January 2026 tech stack
7. **Clean Architecture**: 3-module structure + Pure MVVM UI layer
8. **Hybrid Config Delivery**: AIDL service for real-time + XSharedPreferences fallback (Jan 2026)
9. **Centralized Logging**: All hook logs in system_server for easy debugging
