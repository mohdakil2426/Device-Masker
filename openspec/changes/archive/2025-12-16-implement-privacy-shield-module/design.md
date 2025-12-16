# Design: PrivacyShield LSPosed Module Architecture

## Context

PrivacyShield is an LSPosed/Xposed module designed for device identifier spoofing with robust anti-detection capabilities. The module operates within the Xposed framework, intercepting API calls made by target applications and returning spoofed values instead of real device identifiers.

### Stakeholders
- **End Users**: Privacy-conscious users wanting to protect device fingerprinting
- **Security Researchers**: Testing application behavior with different device identities
- **Developers**: Learning about Android security and hook frameworks

### Constraints
- Must work with LSPosed 1.10.2+ on Magisk 30.6+
- Must support Android 8.0 (API 26) through Android 16 (API 36)
- Must not handle root/SafetyNet/Play Integrity (delegated to external modules)
- Must minimize performance overhead in hooked applications
- All spoofed values must be valid formats

## Goals / Non-Goals

### Goals
- ✅ Spoof 24+ device identifiers consistently
- ✅ Remain undetected by common anti-Xposed detection methods
- ✅ Provide per-app profile configuration
- ✅ Generate valid, realistic fake values
- ✅ Beautiful Material 3 Expressive UI
- ✅ Fast, responsive user experience

### Non-Goals
- ❌ Root detection bypass (use Shamiko)
- ❌ SafetyNet/Play Integrity bypass (use PIF, Tricky Store)
- ❌ Banking app compatibility (combination of external modules)
- ❌ Bootloader status bypass (hardware limitation)

## Architecture Decisions

### AD-1: Use YukiHookAPI Instead of Raw Xposed API

**Decision**: Use YukiHookAPI 1.3.1 as the hooking framework.

**Rationale**:
- Modern Kotlin DSL with type-safe method/field references
- `@InjectYukiHookWithXposed` annotation eliminates boilerplate
- Built-in `YukiBaseHooker` class for modular hook organization
- Better error handling and debugging capabilities
- KSP-based annotation processing for compile-time safety
- Active maintenance and documentation

**Trade-offs**:
- Additional dependency (~350KB)
- Slight learning curve for developers familiar with raw Xposed

### AD-2: Modular Hooker Architecture

**Decision**: Separate hookers by domain (Device, Network, Advertising, System, Location, AntiDetect).

**Structure**:
```
hook/
├── HookEntry.kt              # @InjectYukiHookWithXposed entry
└── hooker/
    ├── DeviceHooker.kt       # IMEI, Serial, Hardware
    ├── NetworkHooker.kt      # MAC, WiFi, Bluetooth
    ├── AdvertisingHooker.kt  # GSF, AdvID, Android ID
    ├── SystemHooker.kt       # Build.*, SystemProperties
    ├── LocationHooker.kt     # GPS, Timezone
    └── AntiDetectHooker.kt   # Stack trace, ClassLoader hiding
```

**Rationale**:
- Single Responsibility Principle
- Easy to disable/enable specific categories
- Testable in isolation
- Clear ownership and maintenance
- Reduced merge conflicts in team development

### AD-3: Anti-Detection First Loading Order

**Decision**: Always load `AntiDetectHooker` before any spoofing hookers.

**Implementation**:
```kotlin
override fun onHook() = encase {
    loadHooker(AntiDetectHooker)  // ⚠️ MUST BE FIRST
    loadHooker(DeviceHooker)
    loadHooker(NetworkHooker)
    // ...
}
```

**Rationale**:
- Apps may perform detection early in their lifecycle
- Stack trace filtering must be active before spoofing hooks execute
- ClassLoader hiding must block Xposed class discovery early

### AD-4: DataStore for Persistence

**Decision**: Use Jetpack DataStore (Preferences) instead of SharedPreferences.

**Rationale**:
- Asynchronous by design (Kotlin Flow)
- No ANRs from blocking UI thread
- Type-safe key definitions
- Coroutines integration
- Modern replacement recommended by Google

**Trade-offs**:
- More verbose than SharedPreferences for simple cases
- Requires coroutines for read/write operations

### AD-5: Unidirectional Data Flow (UDF) Pattern

**Decision**: Use StateFlow + immutable data classes for UI state.

**Pattern**:
```
UI Events → ViewModel → Repository → DataStore
                ↓
         StateFlow<UiState>
                ↓
              UI (Compose)
```

**Rationale**:
- Predictable state management
- Easy debugging and testing
- Compose recomposition optimization
- Single source of truth

### AD-6: Profile-Based Configuration

**Decision**: Use named profiles that can be assigned per-app.

**Structure**:
- `SpoofProfile`: Named collection of spoofed values
- `AppConfig`: Links package name to profile + enabled spoofs
- Default profile applied to apps without explicit config

**Rationale**:
- Users may want different identities for different app categories
- Easy bulk apply (assign profile to multiple apps)
- Import/export capability for backup

### AD-7: Material 3 Expressive Design System

**Decision**: Use Material 3 with Expressive enhancements and AMOLED optimization.

**Color Strategy**:
- Dynamic colors on Android 12+ (Material You)
- Custom Teal/Cyan accent (privacy theme) as fallback
- AMOLED pure black (`#000000`) background in dark mode

**Motion Strategy**:
- Spring-based animations instead of duration-based
- `Spring.DampingRatioMediumBouncy` for most transitions
- Minimum animation for performance-critical paths

## Component Communication

```
┌────────────────────────────────────────────────────────────┐
│                     TARGET APPLICATION                      │
│                                                             │
│   getImei() ─────► LSPosed Framework ─────► HookEntry      │
│                                              │               │
│                              ┌───────────────┴───────────┐  │
│                              ▼                           ▼  │
│                     AntiDetectHooker            DeviceHooker │
│                              │                           │  │
│                              └───────────┬───────────────┘  │
│                                          ▼                  │
│                                   SpoofDataStore            │
│                                   (XPC to module)           │
└────────────────────────────────────────────────────────────┘
                                          │
                                          ▼
┌────────────────────────────────────────────────────────────┐
│                    MODULE APPLICATION                       │
│                                                             │
│   MainActivity ──► ViewModel ──► Repository ──► DataStore  │
│        │                              ▲                     │
│        └─────── Compose UI ───────────┘                     │
│                                                             │
└────────────────────────────────────────────────────────────┘
```

## Data Flow for XPosed Module

### Host Process (Target App):
1. YukiHookAPI intercepts API call
2. AntiDetect layer ensures detection checks fail
3. Spoofing hooks read values from DataStore (XSharedPreferences/DataChannel)
4. Spoofed value returned to target app

### Module Process (PrivacyShield App):
1. User configures values through Compose UI
2. ViewModel updates state via Repository
3. Repository persists to DataStore
4. Next hook invocation reads new values

## Risks / Trade-offs

### Risk: Detection by Sophisticated Apps

**Mitigation**:
- Comprehensive pattern list for stack trace filtering
- Native /proc/maps filtering
- ClassLoader interception
- Regular updates to detection patterns

### Risk: Performance Impact

**Mitigation**:
- Minimize hook overhead (direct field access where possible)
- Cache spoofed values in memory
- Lazy hook initialization
- Profile-based selective hooking

### Risk: Value Validation Failures

**Mitigation**:
- Luhn checksum for IMEI generation
- Unicast bit for MAC addresses
- Realistic format templates for fingerprints
- Comprehensive validation before saving

### Risk: Cross-Process Data Sync

**Mitigation**:
- YukiHookAPI DataChannel for real-time sync
- XSharedPreferences as fallback
- Restart hooks on value change if needed

## Migration Plan

N/A - This is a new implementation from empty scaffold.

## Open Questions

1. **Q: Should we support Android 7.x (API 24-25)?**
   - A: PRD specifies minSdk 26. Lower versions lack Xposed framework features.

2. **Q: How to handle hot-reload of profile changes?**
   - A: Use YukiHookAPI DataChannel for live sync. App restart as fallback.

3. **Q: Should we implement import/export in v1.0?**
   - A: Marked as "Low" priority in PRD. Defer to v1.1.
