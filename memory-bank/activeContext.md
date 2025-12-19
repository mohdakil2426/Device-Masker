# Active Context: Device Masker

## Current Work Focus

### ✅ Session Complete: Reusable UI Component Extraction

**Status**: Complete
**Date**: December 19, 2025

#### Recent Changes
- ✅ Created 9 reusable UI components for code consistency
- ✅ Extracted utility functions to dedicated utils package
- ✅ Refactored 4 screens to use new components
- ✅ **FIXED**: Theme mode dialog not closing in SettingsScreen
- ✅ Reduced ProfileDetailScreen from 860 → 667 lines (~23%)
- ✅ Reduced code duplication across screens by ~275+ lines

#### New Components Created
| Component | File | Purpose |
|-----------|------|---------|
| `IconCircle` | `ui/components/IconCircle.kt` | Circular icon container |
| `ScreenHeader` | `ui/components/ScreenHeader.kt` | Consistent screen headers |
| `SettingsSection` | `ui/components/SettingsItem.kt` | Settings section wrapper |
| `SettingsSwitchItem` | `ui/components/SettingsItem.kt` | Switch row pattern |
| `SettingsClickableItem` | `ui/components/SettingsItem.kt` | Clickable row pattern |
| `SettingsInfoItem` | `ui/components/SettingsItem.kt` | Info display row |
| `EmptyState` | `ui/components/EmptyState.kt` | Empty list placeholder |
| `StatCard` | `ui/components/StatCard.kt` | Dashboard stat cards |
| `ValueRow` | `ui/components/ValueRow.kt` | Label + value pairs |
| `StandardDialogs` | `ui/components/dialog/StandardDialogs.kt` | Confirmation/delete dialogs |
| `AppListItem` | `ui/components/AppListItem.kt` | App selection rows |
| `ImageUtils` | `utils/ImageUtils.kt` | Drawable to Bitmap conversion |

#### Files Updated This Session
| File | Changes |
|------|---------|
| `HomeScreen.kt` | Replaced private StatCard with reusable, use IconCircle |
| `ProfileScreen.kt` | Use ScreenHeader, EmptyState |
| `SettingsScreen.kt` | Full refactor with Settings* components, fix dialog dismiss |
| `ProfileDetailScreen.kt` | Use IconCircle, EmptyState, AppListItem; remove 155 lines |

## Expressive Components Inventory

### Complete List (10 Components)

```
ui/components/expressive/
├── AnimatedSection.kt           # Animated expand/collapse sections
├── ExpressiveCard.kt            # Card with spring press feedback
├── ExpressiveIconButton.kt      # Icon button with spring scale animation
├── ExpressiveLoadingIndicator.kt # M3 LoadingIndicator wrapper (ACTIVE)
├── ExpressivePullToRefresh.kt   # Pull-to-refresh with morphing shapes (ACTIVE)
├── ExpressiveSwitch.kt          # M3 Switch with spring thumb animation
├── MorphingShape.kt             # Animated corner radius utilities (ACTIVE)
├── QuickActionGroup.kt          # M3 ButtonGroup wrapper
├── SectionHeader.kt             # Consistent section headers
└── StatusIndicator.kt           # Status dot indicators
```

### ExpressiveSwitch Features
- **Spring-animated thumb position** - Bouncy movement using `AppMotion.Spatial.SnappyDp`
- **Thumb size morphing** - 16dp → 24dp → 28dp based on state (unchecked/checked/pressed)
- **Theme-aware colors** - Uses `MaterialTheme.colorScheme.primary` for checked state
- **Dynamic color support** - Automatically adapts to Android 12+ wallpaper colors
- **Press feedback** - Scale animation on touch down
- **Accessibility** - Proper `Role.Switch` semantics

### ExpressiveIconButton Animation
- **On Press**: Shrinks to 85% scale
- **On Release**: Bounces back with overshoot (spring physics)
- **Centering**: All overlays now use `Alignment.Center` with `fillMaxSize()`
- **Z-Order**: Overlays are placed at the end of `Box` to ensure they appear on top
- **Spring Config**: `AppMotion.Spatial.Expressive` (dampingRatio: 0.5, stiffness: Low)

## Motion System (AppMotion)

```kotlin
AppMotion.Spatial.*   // Position, size, scale - CAN overshoot
  - Expressive        // Hero moments, FAB, icon buttons (0.5 damping, low stiffness)
  - Standard          // Navigation, list animations (0.75 damping, medium stiffness)
  - Snappy            // Toggle switches, quick feedback (0.75 damping, high stiffness)

AppMotion.Effect.*    // Color, opacity - NO overshoot
  - Color             // Background, track, thumb color changes
  - Alpha             // Fade in/out, visibility
  - Quick             // Immediate feedback
```

## Component Usage Patterns

### ExpressiveSwitch
```kotlin
// Basic usage
ExpressiveSwitch(
    checked = isEnabled,
    onCheckedChange = { onEnableChange(it) }
)

// Without thumb icon
ExpressiveSwitch(
    checked = checked,
    onCheckedChange = onCheckedChange,
    showThumbIcon = false
)
```

### ExpressiveIconButton
```kotlin
// Standard size (40dp button, 24dp icon)
ExpressiveIconButton(
    onClick = { onRefresh() },
    icon = Icons.Filled.Refresh,
    contentDescription = "Refresh",
    tint = MaterialTheme.colorScheme.primary
)

// Compact size for action rows (36dp button, 20dp icon)
CompactExpressiveIconButton(
    onClick = { onCopy() },
    icon = Icons.Filled.ContentCopy,
    contentDescription = "Copy"
)
```

## Important Patterns & Preferences

### Theme-Aware Components
All expressive components use `MaterialTheme.colorScheme` for theming:
- `primary` - Checked/active state track colors
- `onPrimary` - Checked thumb color
- `onPrimaryContainer` - Checked icon color
- `outline` - Unchecked thumb/border
- `surfaceContainerHighest` - Unchecked track

### M3 Expressive APIs
Components using experimental APIs require:
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
```

## Architecture Overview

### Current Navigation (3-Tab)
```
BottomNavBar
├── Home (HomeScreen)
├── Profiles (ProfileScreen → ProfileDetailScreen)
└── Settings (SettingsScreen → DiagnosticsScreen)
```

### Data Flow
```
SpoofProfile (with isEnabled)
        ↓
SpoofRepository
        ↓
UI State (collectAsState)
        ↓
Screens & Components (ExpressiveSwitch, Cards, etc.)
```

## Next Steps (Optional Enhancements)

1. Add shape morphing on SpoofValueCard selection
2. Implement FAB Menu for advanced profile actions
3. Add Carousel for app list in ProfileDetailScreen
4. Create ExpressiveChip component for filter/category selection
5. Add haptic feedback to ExpressiveSwitch toggle

## Session Notes

### Key Decisions Made
1. **Custom Switch Implementation** - Created custom `ExpressiveSwitch` because default M3 `Switch` doesn't support spring-animated thumb position
2. **Theme Colors Over Hardcoded** - All components use `MaterialTheme.colorScheme` for proper light/dark/dynamic color support
3. **Consistent Animation Pattern** - All interactive icons use `CompactExpressiveIconButton` for uniform spring feedback

### Build Status
- ✅ Build successful (Dec 18, 2025 23:00 IST)
- ✅ All components in `ui/components/expressive/` are now actively utilized and polished.
- ✅ Critical LSPosed anti-detection and hooking syntax errors resolved.
