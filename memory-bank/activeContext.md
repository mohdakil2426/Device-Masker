# Active Context: Device Masker

## Current Work Focus

### ✅ Completed Change: Material 3 Expressive Features

**Status**: Complete
**Completed**: December 18, 2025

#### Features Implemented
- ✅ Dependency Updates (Material 3 1.5.0-alpha11, graphics-shapes 1.0.1)
- ✅ Typography Extensions (emphasized(), deemphasized())
- ✅ New Expressive Components (MorphingShape, ExpressiveLoadingIndicator, QuickActionGroup)
- ✅ ExpressivePullToRefresh (reusable component with M3 LoadingIndicator)
- ✅ HomeScreen QuickActionGroup integration
- ✅ DiagnosticsScreen pull-to-refresh with expressive indicator
- ✅ ProfileScreen scroll-aware FAB
- ✅ BottomNavBar expressive animations & M3 1.4.0+ label colors

## Recent Achievements
- **Material 3 Expressive Integration**: Updated to M3 1.5.0-alpha11 with expressive features
- **New Reusable Components**: Created 4 new expressive components in `ui/components/expressive/`
- **ExpressivePullToRefresh**: Reusable pull-to-refresh with morphing LoadingIndicator
- **Spring Physics Motion**: All animations now use AppMotion.Spatial.* and AppMotion.Effect.*
- **Scroll-Aware FAB**: ProfileScreen FAB collapses on scroll, expands at top

## Architecture Overview

### New Expressive Components Structure
```
ui/components/expressive/
├── AnimatedSection.kt          # Animated expand/collapse sections
├── ExpressiveLoadingIndicator.kt # M3 LoadingIndicator wrapper
├── ExpressivePullToRefresh.kt  # Reusable pull-to-refresh component
├── MorphingShape.kt            # Animated corner radius utilities
└── QuickActionGroup.kt         # M3 ButtonGroup wrapper
```

### Motion System (AppMotion)
```kotlin
AppMotion.Spatial.*   // Position, size, scale - CAN overshoot
  - Expressive        // Hero moments, button presses
  - Standard          // Navigation, list animations
  - Snappy            // Toggle switches, quick feedback

AppMotion.Effect.*    // Color, opacity - NO overshoot
  - Color             // Background, icon tint changes
  - Alpha             // Fade in/out, visibility
  - Quick             // Immediate feedback
```

### Pull-to-Refresh Pattern
```kotlin
// Simple usage with reusable component
ExpressivePullToRefresh(
    isRefreshing = isRefreshing,
    onRefresh = { refresh() }
) {
    LazyColumn { /* content */ }
}
```

## Important Patterns & Preferences

### M3 Expressive APIs
All expressive features require `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`:
- `LoadingIndicator` - Morphing shapes indicator
- `ButtonGroup` - Connected button group

### Scroll-Aware FAB Pattern
```kotlin
val listState = rememberLazyListState()
val expandedFab by remember {
    derivedStateOf { listState.firstVisibleItemIndex == 0 }
}

ExtendedFloatingActionButton(
    expanded = expandedFab,
    // ...
)
```

### Animation Spec Selection
- **Position/Scale**: `AppMotion.Spatial.Standard` or `AppMotion.Spatial.Expressive`
- **Color transitions**: `AppMotion.Effect.Color`
- **Alpha/Opacity**: `AppMotion.Effect.Alpha`

## Next Steps (Optional Enhancements)
1. Add shape morphing on SpoofValueCard selection
2. Implement FAB Menu for advanced profile actions
3. Add Carousel for app list in ProfileDetailScreen
