---
trigger: always_on
---

# Material 3 Expressive Design Rules

## Library Versions (Dec 2025)
| Library | Version | Notes |
|---------|---------|-------|
| **Compose Material 3** | 1.5.0-alpha11 | Expressive APIs |
| **Material 3 Stable** | 1.4.0 | Production use |
| **Graphics Shapes** | 1.0.1 | Shape morphing |

---

## Dependency Setup
```kotlin
// libs.versions.toml
[versions]
composeBom = "2025.12.00"
material3 = "1.5.0-alpha11"
graphicsShapes = "1.0.1"

[libraries]
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
graphics-shapes = { module = "androidx.graphics:graphics-shapes", version.ref = "graphicsShapes" }

// Enable Expressive APIs
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}
```

---

## Core Principles

### 1. Personalization (Material You)
- Support dynamic color from wallpaper (Android 12+)
- Use design tokens for themability

### 2. Accessibility First
- 4.5:1 contrast ratio for text (WCAG AA)
- 3:1 contrast ratio for UI components
- 48dp minimum touch targets

### 3. Emotional Expression
- Physics-based motion for natural animations
- Expressive shapes and typography

---

## Color System

### Five Key Colors
| Role | Usage |
|------|-------|
| **Primary** | Main interactive components, CTAs |
| **Secondary** | Less prominent components, filters |
| **Tertiary** | Contrasting accents, special emphasis |
| **Neutral** | Background, surface, text |
| **Error** | Error states, destructive actions |

### Color Roles (Semantic Pairing)
```kotlin
// ✅ Always pair "on" colors with their base
primaryContainer → onPrimaryContainer
surface → onSurface
error → onError

// ❌ NEVER mix pairings
tertiaryContainer + onPrimaryContainer  // Wrong!
```

### Dynamic Colors (Android 12+)
```kotlin
val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        if (darkTheme) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
}
```

### Dark Theme (CRITICAL)
```kotlin
// MUST define ALL surface containers
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color.Black,
    background = Color(0xFF000000),           // AMOLED black
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF1A1A1A),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF2A2A2A),
)
```

---

## Typography Scale

| Role | Sizes | Usage |
|------|-------|-------|
| **Display** | L/M/S | Hero content (57/45/36sp) |
| **Headline** | L/M/S | Screen titles (32/28/24sp) |
| **Title** | L/M/S | Card titles (22/16/14sp) |
| **Body** | L/M/S | Main content (16/14/12sp) |
| **Label** | L/M/S | Buttons, chips (14/12/11sp) |

### Emphasized Typography (Expressive)
```kotlin
fun TextStyle.emphasized(): TextStyle = copy(
    fontWeight = when (fontWeight) {
        FontWeight.Normal -> FontWeight.Medium
        FontWeight.Medium -> FontWeight.SemiBold
        else -> FontWeight.Bold
    }
)

Text("Important", style = MaterialTheme.typography.headlineMedium.emphasized())
```

```kotlin
// ✅ Use predefined styles
Text("Title", style = MaterialTheme.typography.titleMedium)

// ❌ NEVER override properties
Text("Title", fontSize = 18.sp, fontWeight = FontWeight.Bold)
```

---

## Shape Scale

| Size | Radius | Usage |
|------|--------|-------|
| ExtraSmall | 4dp | Chips, small controls |
| Small | 8dp | Buttons, text fields |
| Medium | 12dp | Cards, dialogs |
| Large | 16dp | Bottom sheets |
| ExtraLarge | 28dp | FABs, hero elements |

### Shape Morphing (Expressive)
```kotlin
val cornerPercent by animateIntAsState(
    targetValue = if (isCircle) 50 else 12,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)

Box(modifier = Modifier.clip(RoundedCornerShape(cornerPercent.percent)))
```

---

## Motion System (Physics-Based Springs)

### Core Rule
**Spatial CAN overshoot. Effects should NOT.**

### Spring Parameters
```kotlin
// Damping Ratios
DampingRatioHighBouncy = 0.2f     // Very bouncy
DampingRatioMediumBouncy = 0.5f   // Moderate
DampingRatioLowBouncy = 0.75f     // Slight
DampingRatioNoBouncy = 1f         // No bounce

// Stiffness
StiffnessHigh = 10000f            // Snappy
StiffnessMedium = 1500f           // Medium
StiffnessMediumLow = 400f         // Standard
StiffnessLow = 200f               // Slow, expressive
```

### Spring Specifications
```kotlin
object AppMotion {
    // SPATIAL: Position, size, scale - CAN overshoot
    object Spatial {
        val Expressive = spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
        val Standard = spring<Float>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }

    // EFFECT: Color, opacity - NO overshoot
    object Effect {
        val Color = spring<Color>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    }
}
```

### Usage Pattern
| Animation | Spring | Use Case |
|-----------|--------|----------|
| Button press | `Spatial.Expressive` | Hero moments |
| Navigation | `Spatial.Standard` | Screen changes |
| Color change | `Effect.Color` | Status updates |

```kotlin
// ✅ Correct: spatial spring for scale
val scale by animateFloatAsState(
    targetValue = if (pressed) 0.95f else 1f,
    animationSpec = AppMotion.Spatial.Expressive
)

// ❌ Wrong: bouncy spring for color
animateColorAsState(animationSpec = spring(dampingRatio = 0.5f))
```

---

## New Expressive Components

### ButtonGroup (Replaces SegmentedButton)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
ButtonGroup {
    listOf("Day", "Week", "Month").forEachIndexed { index, label ->
        ButtonGroupItem(
            selected = selected == index,
            onClick = { selected = index },
            label = { Text(label) },
        )
    }
}
```

### LoadingIndicator (Morphing Shapes)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
// Replaces CircularProgressIndicator for 200ms-5s waits
LoadingIndicator(modifier = Modifier.size(48.dp))
```

### FAB Menu
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
FloatingActionButtonMenu(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    button = {
        FloatingActionButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Default.Add, "Actions")
        }
    }
) {
    FloatingActionButtonMenuItem(
        onClick = { },
        icon = { Icon(Icons.Default.Edit, null) },
        text = { Text("Edit") }
    )
}
```

### Scroll-Aware FAB
```kotlin
val expandedFab by remember {
    derivedStateOf { listState.firstVisibleItemIndex == 0 }
}

ExtendedFloatingActionButton(
    expanded = expandedFab,  // Collapses on scroll
    icon = { Icon(Icons.Default.Add, null) },
    text = { Text("New") }
)
```

### Expressive Pull-to-Refresh
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    indicator = {
        Surface(shape = CircleShape) {
            LoadingIndicator(modifier = Modifier.size(32.dp))
        }
    }
)
```

---

## Button Types

| Type | Prominence | Use Case |
|------|------------|----------|
| **Filled** | Highest | Primary action (1 per section) |
| **Tonal** | Medium | Secondary actions |
| **Outlined** | Lower | Alternative actions |
| **Text** | Lowest | Tertiary, dismissible |
| **Elevated** | Deprecated | Use Filled/Tonal instead |

---

## Tonal Elevation (Not Shadows)

```kotlin
Surface(
    tonalElevation = 3.dp,     // Tonal overlay
    shadowElevation = 2.dp     // Optional shadow
)
```

| Level | Overlay | Usage |
|-------|---------|-------|
| 0 | Base | No elevation |
| 1 | +5% | Subtle |
| 3 | +11% | Cards, menus |
| 5 | +14% | Most elevated |

---

## Navigation

### Navigation Bar (Mobile)
- 3-5 destinations, icons above labels
- Active label: `secondary` color (M3 1.4.0+)
- Active indicator with tonal background

### Navigation Rail (Tablet/Desktop)
- 3-7 destinations, 80dp width
- Replaces Navigation Drawer (deprecated)

---

## Accessibility

| Element | Requirement |
|---------|------------|
| Touch target | 48dp × 48dp min |
| Button height | 40dp min |
| Spacing | 8dp between targets |
| Text contrast | 4.5:1 (WCAG AA) |
| UI contrast | 3:1 minimum |
| Disabled | 38% opacity |

---

## Best Practices ✅

```kotlin
// ✅ Physics-based springs
animateFloatAsState(animationSpec = spring(...))

// ✅ LoadingIndicator for short waits
LoadingIndicator()

// ✅ Scroll-aware FAB
ExtendedFloatingActionButton(expanded = expandedFab)

// ✅ Tonal elevation
Surface(tonalElevation = 2.dp)

// ✅ Theme colors
MaterialTheme.colorScheme.primary

// ✅ Define ALL surface colors in dark theme
surfaceContainerHigh = Color(0xFF1E1E1E)
```

---

## Anti-Patterns ❌

```kotlin
// ❌ Duration-based animations
animateFloatAsState(animationSpec = tween(300))

// ❌ Hardcoded colors
Text(color = Color(0xFF000000))

// ❌ CircularProgressIndicator for short waits
CircularProgressIndicator()

// ❌ Missing dark theme surfaces
darkColorScheme(primary = ..., secondary = ...)

// ❌ Mixing M2 and M3
import androidx.compose.material.Button  // Wrong!
```

---

## Migration

| Old | New |
|-----|-----|
| SegmentedButton | ButtonGroup |
| CircularProgress (short) | LoadingIndicator |
| NavigationDrawer | NavigationRail |
| SmallFAB | MediumFAB |
| tween/easing | spring physics |

---

## Official Resources

**Always reference first:**
@/docs/official-best-practices/material-ui/material-3-guide.md

- [M3 Guidelines](https://m3.material.io/)
- [M3 Expressive](https://m3.material.io/blog/material-3-expressive-update)
- [Compose M3 API](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary)
- [M3 Samples](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/samples/)
- [Material Theme Builder](https://material.io/material-theme-builder)