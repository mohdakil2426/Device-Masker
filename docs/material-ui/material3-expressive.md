# Material Design Rules for Android
# Compose Material 3 Expressive + Material Components Android

## Library Versions (Latest - Dec 2025)

| Library | Version | Package |
|---------|---------|---------|
| **Compose Material 3** | 1.5.0-alpha11 | `androidx.compose.material3:material3` |
| **Graphics Shapes** | 1.0.1 | `androidx.graphics:graphics-shapes` |
| **Material Components Android** | 1.13.0 | `com.google.android.material:material` |

**Stable:** Material 3 `1.4.0`, **Alpha (Expressive):** `1.5.0-alpha11`

---

## 1. Material 3 Expressive (Compose)

### What's New in M3 Expressive

| Feature | Standard M3 | M3 Expressive |
|---------|-------------|---------------|
| Animation | Duration-based (easing) | Physics-based (springs) |
| Shapes | 5 standard radii | 35 dynamic shapes + morphing |
| Motion Feel | Functional | Bouncy, alive |
| FAB Menu | Not supported | New component |
| Button Groups | Segmented buttons | Full button groups |
| Loading | Circular/Linear | Morphing LoadingIndicator |

### Dependency Setup
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

// app/build.gradle.kts
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.graphics.shapes)
}

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

## 2. Motion System (Physics-Based Springs)

### Core Principle
**Spatial animations CAN overshoot. Effect animations should NOT overshoot.**

### Spring Specifications

```kotlin
object AppMotion {
    // SPATIAL: Position, size, scale, rotation - CAN overshoot
    object Spatial {
        val Expressive: SpringSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // 0.5
            stiffness = Spring.StiffnessLow                 // 200
        )
        val Standard: SpringSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,    // 0.75
            stiffness = Spring.StiffnessMediumLow           // 400
        )
        val Snappy: SpringSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium              // 1500
        )
    }

    // EFFECT: Color, opacity, blur - NO overshoot
    object Effect {
        val Color: SpringSpec<Color> = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,     // 1.0
            stiffness = Spring.StiffnessMediumLow
        )
        val Alpha: SpringSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }
}
```

### Usage Pattern

| Animation Type | Spring | Example Use |
|----------------|--------|-------------|
| Button press scale | `Spatial.Expressive` | Hero moments, FAB |
| Navigation transition | `Spatial.Standard` | Screen changes |
| Toggle switch | `Spatial.Snappy` | Quick feedback |
| Background color | `Effect.Color` | Status changes |
| Fade in/out | `Effect.Alpha` | Visibility |

```kotlin
// ✅ CORRECT: Use appropriate spring for animation type
val scale by animateFloatAsState(
    targetValue = if (pressed) 0.95f else 1f,
    animationSpec = AppMotion.Spatial.Expressive  // CAN overshoot
)

val color by animateColorAsState(
    targetValue = if (active) ActiveColor else InactiveColor,
    animationSpec = AppMotion.Effect.Color  // NO overshoot
)

// ❌ WRONG: Using bouncy spring for color
animateColorAsState(
    animationSpec = spring(dampingRatio = 0.5f)  // Color bouncing looks wrong!
)
```

---

## 3. New Expressive Components

### ButtonGroup (Replaces Segmented Buttons)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuickActions() {
    var selected by remember { mutableIntStateOf(0) }
    
    ButtonGroup {
        listOf("Day", "Week", "Month").forEachIndexed { index, label ->
            ButtonGroupItem(
                selected = selected == index,
                onClick = { selected = index },
                label = { Text(label) },
            )
        }
    }
}
```

### LoadingIndicator (Morphing Shapes)
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingState() {
    // For 200ms - 5s loading states (replaces indeterminate CircularProgressIndicator)
    LoadingIndicator(
        modifier = Modifier.size(48.dp),
        color = MaterialTheme.colorScheme.primary
    )
}
```

### FAB Menu
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActionMenu() {
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
            onClick = { /* action */ },
            icon = { Icon(Icons.Default.Edit, null) },
            text = { Text("Edit") }
        )
    }
}
```

### Scroll-Aware FAB
```kotlin
@Composable
fun ProfileList() {
    val listState = rememberLazyListState()
    val expandedFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState) { /* items */ }
        
        ExtendedFloatingActionButton(
            onClick = { },
            expanded = expandedFab,  // Collapses on scroll
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("New") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}
```

### Pull-to-Refresh with Expressive Indicator
```kotlin
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RefreshableList(
    isRefreshing: Boolean,
    onRefresh: () -> Unit, 
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        indicator = {
            // Custom indicator with LoadingIndicator
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                LoadingIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        content = content,
    )
}
```

---

## 4. Shape System

### Standard M3 Shapes
```kotlin
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),  // Chips, small buttons
    small = RoundedCornerShape(8.dp),       // Text fields, list items
    medium = RoundedCornerShape(12.dp),     // Cards, dialogs
    large = RoundedCornerShape(16.dp),      // Bottom sheets
    extraLarge = RoundedCornerShape(28.dp), // Full-screen components
)
```

### Shape Morphing (Expressive)
```kotlin
// Add graphics-shapes dependency
implementation("androidx.graphics:graphics-shapes:1.0.1")

@Composable
fun MorphingShape() {
    var isCircle by remember { mutableStateOf(false) }
    
    val cornerRadius by animateIntAsState(
        targetValue = if (isCircle) 50 else 12,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(cornerRadius.percent))
            .background(MaterialTheme.colorScheme.primary)
            .clickable { isCircle = !isCircle }
    )
}
```

---

## 5. Color & Theming

### Complete Dark Color Scheme (CRITICAL)
```kotlin
// M3 darkColorScheme() does NOT provide sensible defaults!
// You MUST define ALL surface containers for dark theme
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    
    secondary = SecondaryDark,
    onSecondary = Color.Black,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    
    // CRITICAL: Surface containers for proper dark theme
    background = Color(0xFF000000),           // AMOLED black
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFC0C0C0),
    
    // Tonal surface hierarchy
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1E1E1E),
    surfaceContainerHighest = Color(0xFF2A2A2A),
)
```

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

---

## 6. Navigation (M3 1.4.0+ Changes)

### Bottom Navigation Bar
```kotlin
// M3 1.4.0+: Active label uses 'secondary' color (not 'onSurface')
NavigationBar {
    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = { Icon(icon, null) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedTextColor = MaterialTheme.colorScheme.secondary,  // NEW in 1.4.0
            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
        )
    )
}
```

### Navigation Drawer Deprecated
Use **Navigation Rail** instead for tablet/desktop layouts.

---

## 7. Typography (Expressive Emphasis)

### Enhanced Type Scale
```kotlin
// Add emphasis with weight
fun TextStyle.emphasized(): TextStyle = copy(
    fontWeight = when (fontWeight) {
        FontWeight.Normal -> FontWeight.Medium
        FontWeight.Medium -> FontWeight.SemiBold
        FontWeight.SemiBold -> FontWeight.Bold
        else -> FontWeight.ExtraBold
    }
)

// Usage
Text(
    text = "Important Title",
    style = MaterialTheme.typography.headlineMedium.emphasized()
)
```

---

## 8. Best Practices

### ✅ DO

```kotlin
// Use physics-based springs for all animations
animateFloatAsState(animationSpec = spring(...))

// Use appropriate spring type for animation
// Spatial (position/scale) vs Effect (color/opacity)

// Use LoadingIndicator for 200ms-5s waits
LoadingIndicator()  // Not CircularProgressIndicator

// Use scroll-aware FAB
ExtendedFloatingActionButton(expanded = expandedFab)

// Use ButtonGroup for related choices
ButtonGroup { ButtonGroupItem(...) }

// Define ALL surface colors in dark theme
surfaceContainerHigh = Color(0xFF1E1E1E)

// Use tonal elevation for hierarchy
Surface(tonalElevation = 2.dp)
```

### ❌ DON'T

```kotlin
// ❌ Duration-based animations
animateFloatAsState(animationSpec = tween(300))

// ❌ Bouncy springs for color/opacity
animateColorAsState(animationSpec = spring(dampingRatio = 0.5f))

// ❌ CircularProgressIndicator for short waits
CircularProgressIndicator()  // Use LoadingIndicator

// ❌ Hardcoded colors
Text(color = Color(0xFF000000))

// ❌ Missing dark theme surface colors
darkColorScheme(primary = ..., secondary = ...)  // Missing surfaces!

// ❌ Mixing M2 and M3
import androidx.compose.material.Button  // Wrong!
```

---

## 9. Experimental APIs Opt-In

```kotlin
// Required for Expressive components
@OptIn(ExperimentalMaterial3ExpressiveApi::class)

// Components requiring opt-in:
// - LoadingIndicator
// - ButtonGroup / ButtonGroupItem
// - FloatingActionButtonMenu / FloatingActionButtonMenuItem
// - SplitButtonLayout
// - Carousel / CarouselItem
// - DockedToolbarLayout / FloatingToolbarLayout
```

---

## 10. Reusable Component Patterns

### Expressive Pull-to-Refresh
```kotlin
@Composable
fun ExpressivePullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = { ExpressiveRefreshIndicator(state, isRefreshing) },
        content = content,
    )
}
```

### Animated Section
```kotlin
@Composable
fun AnimatedSection(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = AppMotion.Effect.Alpha) +
                expandVertically(animationSpec = AppMotion.Spatial.Standard),
        exit = fadeOut(animationSpec = AppMotion.Effect.Alpha) +
               shrinkVertically(animationSpec = AppMotion.Spatial.Standard)
    ) {
        content()
    }
}
```

---

## Official Resources

### Compose Material 3
- [Developer Guide](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [API Reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary)
- [Release Notes](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- [M3 Samples](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/samples/)

### Material Design
- [Material 3 Guidelines](https://m3.material.io/)
- [Material Theme Builder](https://material.io/material-theme-builder)
- [Motion Guidelines](https://m3.material.io/styles/motion/overview)
