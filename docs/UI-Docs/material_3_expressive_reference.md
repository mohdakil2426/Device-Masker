# Material 3 Expressive - Comprehensive Reference Guide

**Version**: December 2025  
**Status**: Official Google Design System  
**Android Support**: API 26+ (Full Expressive from Android 16/API 36)  
**Compose Material 3 Version**: 1.4.0+ (Expressive APIs marked @ExperimentalMaterial3ExpressiveApi)

---

## Table of Contents

1. [Overview](#overview)
2. [What is Material 3 Expressive](#what-is-material-3-expressive)
3. [New & Updated Components](#new--updated-components)
4. [Motion System (MotionScheme)](#motion-system-motionscheme)
5. [Shape System (35 New Shapes)](#shape-system-35-new-shapes)
6. [Typography Updates](#typography-updates)
7. [Color & Dynamic Theming](#color--dynamic-theming)
8. [Navigation Components](#navigation-components)
9. [Loading & Progress Indicators](#loading--progress-indicators)
10. [Official Code Examples](#official-code-examples)
11. [Migration Guide](#migration-guide)
12. [Official Resources](#official-resources)

---

## Overview

Material 3 Expressive (M3 Expressive) is the latest evolution of Google's Material Design system, introduced in **May 2025** and rolling out with **Android 16** in September 2025. It's not Material 4 - it's an enhancement of Material 3 focused on:

- **Emotional Engagement**: Creating UI that feels alive and responds naturally
- **Physics-Based Motion**: Spring animations with overshoot effects
- **Expanded Shapes**: 35 new shape options with smooth morphing
- **Enhanced Typography**: Emphasized text styles for visual hierarchy
- **AI-Assisted Design**: Dynamic theming and accessibility improvements

### Research Backing
Based on 46 studies with 18,000+ participants, Material Expressive designs showed:
- Higher usability ratings
- Increased emotional engagement
- Better user satisfaction

---

## What is Material 3 Expressive

### Core Pillars

| Pillar | Description |
|--------|-------------|
| **Motion** | Physics-based spring animations with overshoot |
| **Shape** | 35 shapes with dynamic morphing transitions |
| **Typography** | Emphasized styles for hierarchy and personality |
| **Color** | Enhanced dynamic theming with AI personalization |
| **Components** | 15+ new/updated UI components |

### Key Differences from Standard M3

| Feature | Standard M3 | M3 Expressive |
|---------|-------------|---------------|
| Animation | Duration-based (easing) | Physics-based (springs) |
| Shapes | 5 standard corner radii | 35 dynamic shapes |
| Motion Feel | Functional | Bouncy, alive |
| FAB Menu | Not supported | New component |
| Button Groups | Segmented buttons | Full button groups |
| Loading | Circular/Linear | Morphing indicators |

---

## New & Updated Components

### 🆕 NEW Components (M3 Expressive)

#### 1. **Button Groups** (Replaces Segmented Buttons)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ButtonGroupExample() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val options = listOf("Day", "Week", "Month")
    
    ButtonGroup {
        options.forEachIndexed { index, label ->
            ButtonGroupItem(
                selected = selectedIndex == index,
                onClick = { selectedIndex = index },
                label = { Text(label) },
            )
        }
    }
}
```

**Sizes**: XS, S, M, L, XL  
**Modes**: Single-select, Multi-select, Selection-required

#### 2. **Split Button**
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SplitButtonExample() {
    var expanded by remember { mutableStateOf(false) }
    
    SplitButtonLayout(
        leadingButton = {
            Button(onClick = { /* Primary action */ }) {
                Text("Save")
            }
        },
        trailingButton = {
            IconButton(onClick = { expanded = !expanded }) {
                Icon(Icons.Default.ArrowDropDown, null)
            }
        }
    )
}
```

#### 3. **FAB Menu** (Floating Action Button Menu)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FabMenuExample() {
    var expanded by remember { mutableStateOf(false) }
    
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
            onClick = { /* Action 1 */ },
            icon = { Icon(Icons.Default.Edit, null) },
            text = { Text("Edit") }
        )
        FloatingActionButtonMenuItem(
            onClick = { /* Action 2 */ },
            icon = { Icon(Icons.Default.Share, null) },
            text = { Text("Share") }
        )
    }
}
```

**Features**: 2-6 related actions, bouncy animation, overlay dismiss

#### 4. **Loading Indicator** (Morphing Shapes)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingIndicatorExample() {
    // Contained (inside a button/surface)
    LoadingIndicator()
    
    // Uncontained (standalone)
    LoadingIndicator(
        modifier = Modifier.size(48.dp),
        color = MaterialTheme.colorScheme.primary
    )
}
```

**Replaces**: Indeterminate CircularProgressIndicator for 200ms-5s waits  
**Animation**: Morphing between abstract shapes

#### 5. **Carousel** (New Layout)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CarouselExample() {
    val items = listOf("Item 1", "Item 2", "Item 3", "Item 4")
    
    Carousel(
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items.forEach { item ->
            CarouselItem {
                Card { 
                    Text(item, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
```

**Layouts**: Multi-browse, Uncontained, Hero, Full-screen  
**Features**: Parallax scrolling, shape morphing during scroll

#### 6. **Toolbars** (Docked & Floating)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DockedToolbarExample() {
    DockedToolbarLayout {
        IconButton(onClick = {}) { Icon(Icons.Default.FormatBold, "Bold") }
        IconButton(onClick = {}) { Icon(Icons.Default.FormatItalic, "Italic") }
        IconButton(onClick = {}) { Icon(Icons.Default.FormatUnderlined, "Underline") }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FloatingToolbarExample() {
    FloatingToolbarLayout {
        // Toolbar items
    }
}
```

---

### 🔄 UPDATED Components

#### 1. **Extended FAB** (Shape Animation)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnimatedExtendedFabExample() {
    var expanded by remember { mutableStateOf(true) }
    
    ExtendedFloatingActionButton(
        onClick = { /* action */ },
        expanded = expanded,
        icon = { Icon(Icons.Default.Add, null) },
        text = { Text("Create") },
        // Expressive: Shape morphs on expand/collapse
    )
}
```

**Sizes**: Now includes Medium FAB (Small FAB deprecated)

#### 2. **Icon Buttons** (Shape Morphing)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveIconButtonExample() {
    IconButton(
        onClick = { },
        // Animates shape on press
    ) {
        Icon(Icons.Default.Favorite, null)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveIconToggleButtonExample() {
    var checked by remember { mutableStateOf(false) }
    
    IconToggleButton(
        checked = checked,
        onCheckedChange = { checked = it },
        // Shape morphs between states
    ) {
        Icon(
            if (checked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            null
        )
    }
}
```

#### 3. **Buttons** (Animated Shapes)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveButtonExample() {
    FilledButton(
        onClick = { },
        // Button shape can animate on interaction
    ) {
        AnimatedVisibility(visible = true) {
            Icon(Icons.Default.Add, null)
        }
        Spacer(Modifier.width(8.dp))
        Text("Add Item")
    }
}
```

**New Sizes**: XS, S, M, L, XL button sizes available

#### 4. **Sliders** (Enhanced Touch)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveSliderExample() {
    var value by remember { mutableFloatStateOf(0.5f) }
    
    Slider(
        value = value,
        onValueChange = { value = it },
        // Enhanced drag feedback with expressive motion
    )
}
```

#### 5. **Progress Indicators** (Rounded Corners)
- Updated motion behaviors
- Rounded track and indicator corners
- Enhanced contrast

---

## Motion System (MotionScheme)

### Overview

M3 Expressive introduces a **physics-based motion system** replacing traditional easing/duration specs with **spring animations**.

### Motion Schemes

```kotlin
// Access via MaterialTheme
val motionScheme = MaterialTheme.motionScheme

// Two presets available:
// 1. Expressive - Bouncy, for hero moments
// 2. Standard - Subtle, for functional elements
```

| Scheme | Damping | Use Case |
|--------|---------|----------|
| **Expressive** | Low (bouncy) | Prominent interactions, hero moments |
| **Standard** | High (no bounce) | Functional elements, subtle feedback |

### Spring Tokens

#### Spatial Springs (Position, Size, Rotation)
```kotlin
// These OVERSHOOT their target values
val spatialSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)
```

#### Effect Springs (Color, Opacity)
```kotlin
// These do NOT overshoot
val effectSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)
```

### Spring Parameters

| Parameter | Values | Effect |
|-----------|--------|--------|
| **dampingRatio** | 0.0 - 1.0 | Lower = more bounce |
| **stiffness** | Low/Medium/High | Higher = faster |

```kotlin
object Spring {
    // Damping Ratios
    const val DampingRatioHighBouncy = 0.2f    // Very bouncy
    const val DampingRatioMediumBouncy = 0.5f  // Moderate bounce
    const val DampingRatioLowBouncy = 0.75f    // Slight bounce
    const val DampingRatioNoBouncy = 1f        // No bounce
    
    // Stiffness
    const val StiffnessHigh = 10000f      // Snappy
    const val StiffnessMedium = 1500f     // Medium
    const val StiffnessMediumLow = 400f   // Moderate
    const val StiffnessLow = 200f         // Slow
    const val StiffnessVeryLow = 50f      // Very slow
}
```

### Implementation Example

```kotlin
@Composable
fun ExpressiveAnimation() {
    var expanded by remember { mutableStateOf(false) }
    
    // Expressive spring for spatial changes
    val size by animateDpAsState(
        targetValue = if (expanded) 200.dp else 100.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    // Standard spring for color
    val color by animateColorAsState(
        targetValue = if (expanded) Color.Green else Color.Gray,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    Box(
        modifier = Modifier
            .size(size)
            .background(color)
            .clickable { expanded = !expanded }
    )
}
```

---

## Shape System (35 New Shapes)

### Overview

M3 Expressive introduces 35 new shapes beyond standard rounded rectangles, with smooth shape morphing capabilities.

### Shape Categories

| Category | Examples | Use Cases |
|----------|----------|-----------|
| **Rounded** | RoundedCornerShape | Buttons, Cards |
| **Cut** | CutCornerShape | Chips, Tags |
| **Soft Curves** | WavyShape | Avatars, Decorative |
| **Polygonal** | HexagonShape, OctagonShape | Icons, Badges |
| **Organic** | BlobShape, LeafShape | Expressive elements |

### Shape Morphing

```kotlin
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShapeMorphExample() {
    val startShape = remember { RoundedPolygon.square() }
    val endShape = remember { RoundedPolygon.circle() }
    
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val morphedShape = remember(animatedProgress) {
        Morph(startShape, endShape).toComposePath(animatedProgress)
    }
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(GenericShape { size, _ -> addPath(morphedShape) })
            .background(MaterialTheme.colorScheme.primary)
            .clickable { progress = if (progress == 0f) 1f else 0f }
    )
}
```

### Graphics-Shapes Library

```kotlin
// Add dependency
implementation("androidx.graphics:graphics-shapes:1.0.0")

// Create polygons
val square = RoundedPolygon.square()
val circle = RoundedPolygon.circle()
val hexagon = RoundedPolygon(numVertices = 6)
val star = RoundedPolygon.star(numVerticesPerRadius = 5)
```

### Standard Shape Scale (Material 3)

```kotlin
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // Chips, small buttons
    small = RoundedCornerShape(8.dp),        // Text fields, list items
    medium = RoundedCornerShape(12.dp),      // Cards, dialogs
    large = RoundedCornerShape(16.dp),       // Bottom sheets
    extraLarge = RoundedCornerShape(28.dp),  // Full-screen components
)
```

---

## Typography Updates

### Enhanced Type Scale

M3 Expressive adds **emphasized** variants to the standard type scale for creating visual hierarchy:

| Role | Sizes | Purpose |
|------|-------|---------|
| **Display** | Large, Medium, Small | Hero text, splash screens |
| **Headline** | Large, Medium, Small | Screen titles |
| **Title** | Large, Medium, Small | Section headers |
| **Body** | Large, Medium, Small | Body text |
| **Label** | Large, Medium, Small | Buttons, chips |

### Emphasized Styles

```kotlin
val AppTypography = Typography(
    // Display - EMPHASIZED (heavier weight)
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp,
    ),
    
    // Headline - EMPHASIZED
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    
    // Regular body
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)
```

### Typography Best Practices

1. **Use heavier weights for emphasis** - Bold/SemiBold for headlines
2. **Pair with color** - Combine with `primary` or `onSurface` roles
3. **Maintain hierarchy** - Display → Headline → Title → Body → Label
4. **Accessibility** - Ensure 4.5:1 contrast ratio minimum

---

## Color & Dynamic Theming

### Dynamic Colors (Android 12+)

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    MaterialTheme(colorScheme = colorScheme, content = content)
}
```

### Complete Color Roles

```kotlin
// All 29 color roles - MUST define for dark theme
private val DarkColorScheme = darkColorScheme(
    // Primary
    primary = Color(0xFF00E5FF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF004D56),
    onPrimaryContainer = Color(0xFFCCFFFF),
    
    // Secondary
    secondary = Color(0xFF64FFDA),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1D3D38),
    onSecondaryContainer = Color(0xFFCCFFFF),
    
    // Tertiary
    tertiary = Color(0xFFB388FF),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF311B92),
    onTertiaryContainer = Color(0xFFEDE7F6),
    
    // Error
    error = Color(0xFFFFB4AB),
    onError = Color.Black,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // Surface hierarchy (CRITICAL for dark theme)
    background = Color(0xFF000000),            // AMOLED black
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFC0C0C0),
    
    // Surface containers (tonal elevation)
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF2A2A2A),
    
    // Other
    outline = Color(0xFF8E8E8E),
    outlineVariant = Color(0xFF444444),
)
```

### Tonal Elevation

```kotlin
// Use tonal elevation for surface hierarchy
Surface(
    tonalElevation = 2.dp,    // Changes surface tint based on primary
    shadowElevation = 4.dp,   // Optional drop shadow
) {
    // Content
}
```

---

## Navigation Components

### Flexible Navigation Bar (NEW)

The original navigation bar is deprecated in favor of a **flexible navigation bar**:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlexibleNavigationBarExample() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    
    NavigationBar(
        // Shorter height than original
        // Supports horizontal items on medium screens
    ) {
        NavigationBarItem(
            selected = selectedIndex == 0,
            onClick = { selectedIndex = 0 },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") },
            // Active label color now uses 'secondary' instead of 'on-surface-variant'
        )
        // More items...
    }
}
```

**Changes in M3 1.4.0**:
- Active label color: `on-surface-variant` → `secondary`
- Shorter bar height
- Horizontal layout support for medium screens

### Updated Navigation Rail

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavigationRailExample() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    
    NavigationRail {
        NavigationRailItem(
            selected = selectedIndex == 0,
            onClick = { selectedIndex = 0 },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") },
            // Active label now uses 'secondary' color
        )
    }
}
```

**Configurations**:
- **Collapsed**: Wider than predecessor
- **Expanded**: Shows secondary destinations

**Note**: Navigation Drawer is deprecated - use Navigation Rail instead.

---

## Loading & Progress Indicators

### Loading Indicator (200ms - 5s waits)

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingState() {
    Box(contentAlignment = Alignment.Center) {
        // Morphs through abstract shapes
        LoadingIndicator(
            modifier = Modifier.size(48.dp)
        )
    }
}
```

### Enhanced Progress Indicators

```kotlin
// Circular - with rounded corners and better contrast
CircularProgressIndicator(
    modifier = Modifier.size(48.dp),
    strokeWidth = 4.dp,
)

// Linear - with rounded track ends
LinearProgressIndicator(
    modifier = Modifier.fillMaxWidth(),
    trackColor = MaterialTheme.colorScheme.surfaceVariant,
)
```

---

## Official Code Examples

### Complete Expressive Button Example

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveActionButton(
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Expressive scale animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    FilledButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        interactionSource = interactionSource,
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
```

### Shape Morphing Toggle

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val squareShape = RoundedCornerShape(8.dp)
    val circleShape = CircleShape
    
    val cornerPercent by animateIntAsState(
        targetValue = if (checked) 50 else 15,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val color by animateColorAsState(
        targetValue = if (checked) 
            MaterialTheme.colorScheme.primary
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)
    )
    
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(cornerPercent))
            .background(color)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (checked) Icons.Filled.Check else Icons.Outlined.Add,
            contentDescription = null,
            tint = if (checked) 
                MaterialTheme.colorScheme.onPrimary
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

---

## Migration Guide

### From Standard M3 to M3 Expressive

#### 1. Update Dependencies

```kotlin
// libs.versions.toml
[versions]
composeBom = "2025.12.00"
material3 = "1.4.0"

[libraries]
compose-material3 = { module = "androidx.compose.material3:material3", version.ref = "material3" }
```

#### 2. Add Opt-In Annotation

```kotlin
// For experimental APIs
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MyExpressiveScreen() {
    // Use expressive components
}

// Or at file level
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)
```

#### 3. Update Motion to Springs

```kotlin
// BEFORE: Duration-based
animateFloatAsState(
    targetValue = if (expanded) 1f else 0f,
    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
)

// AFTER: Spring-based
animateFloatAsState(
    targetValue = if (expanded) 1f else 0f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)
```

#### 4. Replace Components

| Old Component | New Component |
|---------------|---------------|
| SegmentedButton | ButtonGroup |
| IndeterminateCircularProgress (short) | LoadingIndicator |
| NavigationDrawer | NavigationRail |
| SmallFloatingActionButton | MediumFloatingActionButton |

---

## Official Resources

### Documentation

| Resource | URL |
|----------|-----|
| **Material 3 Design** | https://m3.material.io |
| **Material Expressive** | https://m3.material.io/blog/material-3-expressive-update |
| **Android Compose M3** | https://developer.android.com/develop/ui/compose/designsystems/material3 |
| **Compose Material 3 API** | https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary |
| **Release Notes** | https://developer.android.com/jetpack/androidx/releases/compose-material3 |

### Code Labs & Samples

| Resource | URL |
|----------|-----|
| **Compose Samples** | https://github.com/android/compose-samples |
| **Material Catalog** | https://github.com/emertozd/compose-material-3-expressive-catalog |
| **Now in Android** | https://github.com/android/nowinandroid |

### Design Tools

| Tool | Purpose |
|------|---------|
| **Material Theme Builder** | Generate color schemes from a seed color |
| **Relay for Figma** | Design-to-code workflow |
| **Material Symbols** | Icon library (replaces material-icons) |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.4.0 | Sep 2025 | M3 Expressive launch, Button Groups, Split Button, FAB Menu, Loading Indicator |
| 1.3.0 | Jun 2025 | MotionScheme preview, Shape morphing preview |
| 1.2.0 | Mar 2025 | Search bar updates, DatePicker improvements |
| 1.1.0 | Dec 2024 | Bottom sheets, Date/Time pickers stable |

---

**Document Maintained By**: DeviceMasker Team  
**Last Updated**: December 18, 2025
