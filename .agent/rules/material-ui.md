---
trigger: always_on
---

# Material Design Rules for Android
# Compose Material 3 + Material Components Android

## Library Versions (Latest Stable - Dec 2025)

| Library | Version | Package |
|---------|---------|---------|
| **Compose Material 3** | 1.4.0 | `androidx.compose.material3:material3` |
| **Material Components Android** | 1.13.0 | `com.google.android.material:material` |

**Alpha/Preview:** Material 3 `1.5.0-alpha10`, Material Components `1.14.0-alpha07`

---

## 1. Compose Material 3 (Jetpack Compose)

### Dependency Setup
```kotlin
// libs.versions.toml
[versions]
composeBom = "2025.12.00"
material3 = "1.4.0"  // Or use BOM

[libraries]
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }

// app/build.gradle.kts
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
}
```

### MaterialTheme Setup
```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,  // Android 12+
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
```

### Color Scheme
```kotlin
// Color.kt - Generated from Material Theme Builder
val md_theme_light_primary = Color(0xFF6750A4)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFE8DEF8)
val md_theme_dark_primary = Color(0xFFD0BCFF)
val md_theme_dark_onPrimary = Color(0xFF381E72)
// ...

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    // ... all 29 color roles
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    // ...
)

// Usage
Text(
    text = "Hello",
    color = MaterialTheme.colorScheme.primary
)
```

### Typography Scale (M3)
```kotlin
val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
)

// Usage
Text(
    text = "Title",
    style = MaterialTheme.typography.titleLarge
)
```

### Shapes
```kotlin
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

// Usage
Card(shape = MaterialTheme.shapes.medium) { }
```

### Core Components
```kotlin
// Buttons (by emphasis)
ExtendedFloatingActionButton(onClick = {}) { Text("FAB") }  // Highest
FilledButton(onClick = {}) { Text("Primary") }             // High
FilledTonalButton(onClick = {}) { Text("Secondary") }      // Medium
OutlinedButton(onClick = {}) { Text("Outlined") }          // Low
TextButton(onClick = {}) { Text("Text") }                  // Lowest

// Cards
Card { content() }                    // Filled
ElevatedCard { content() }            // Elevated
OutlinedCard { content() }            // Outlined

// Top App Bar
TopAppBar(
    title = { Text("Title") },
    navigationIcon = { IconButton(onClick = {}) { Icon(Icons.Default.ArrowBack, null) } },
    actions = { IconButton(onClick = {}) { Icon(Icons.Default.Settings, null) } }
)

// Navigation
NavigationBar {
    NavigationBarItem(
        selected = true,
        onClick = {},
        icon = { Icon(Icons.Default.Home, null) },
        label = { Text("Home") }
    )
}

// Bottom Sheet
ModalBottomSheet(onDismissRequest = {}) { content() }

// Snackbar
val snackbarHostState = remember { SnackbarHostState() }
SnackbarHost(hostState = snackbarHostState)
```

### Elevation (Tonal + Shadow)
```kotlin
Surface(
    tonalElevation = 2.dp,    // Color tint based on primary
    shadowElevation = 4.dp,   // Drop shadow
) { content() }
```

### 1.4.0 Important Changes
- **Icons Library Removed**: Use Material Symbols from fonts.google.com/icons
- **MotionScheme**: Components now use new motion system
- **NavigationBarItem**: Active label color changed `onSurface` → `secondary`
- **No material-icons-core dependency**: Add explicitly if needed

---

## 2. Material Components Android (XML Views)

### Dependency Setup
```kotlin
// libs.versions.toml
[versions]
material = "1.13.0"

[libraries]
material = { group = "com.google.android.material", name = "material", version.ref = "material" }

// app/build.gradle.kts
dependencies {
    implementation(libs.material)
}
```

### Theme Setup
```xml
<!-- res/values/themes.xml -->
<style name="Theme.App" parent="Theme.Material3.DayNight.NoActionBar">
    <!-- Primary brand color -->
    <item name="colorPrimary">@color/md_theme_light_primary</item>
    <item name="colorOnPrimary">@color/md_theme_light_onPrimary</item>
    <item name="colorPrimaryContainer">@color/md_theme_light_primaryContainer</item>
    
    <!-- Secondary -->
    <item name="colorSecondary">@color/md_theme_light_secondary</item>
    <item name="colorOnSecondary">@color/md_theme_light_onSecondary</item>
    
    <!-- Background/Surface -->
    <item name="android:colorBackground">@color/md_theme_light_background</item>
    <item name="colorSurface">@color/md_theme_light_surface</item>
    
    <!-- Error -->
    <item name="colorError">@color/md_theme_light_error</item>
</style>
```

### Material Components (XML)
```xml
<!-- Button -->
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Button"
    style="@style/Widget.Material3.Button" />

<!-- Filled Tonal Button -->
<com.google.android.material.button.MaterialButton
    style="@style/Widget.Material3.Button.TonalButton" />

<!-- Outlined Button -->
<com.google.android.material.button.MaterialButton
    style="@style/Widget.Material3.Button.OutlinedButton" />

<!-- Text Field -->
<com.google.android.material.textfield.TextInputLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    style="@style/Widget.Material3.TextInputLayout.OutlinedBox">
    <com.google.android.material.textfield.TextInputEditText
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Label" />
</com.google.android.material.textfield.TextInputLayout>

<!-- Card -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp">
    <!-- content -->
</com.google.android.material.card.MaterialCardView>

<!-- Chip -->
<com.google.android.material.chip.Chip
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Chip"
    style="@style/Widget.Material3.Chip.Assist" />
```

### New in 1.13.0
```xml
<!-- DockedToolbarLayout -->
<com.google.android.material.dockedtoolbar.DockedToolbarLayout />

<!-- FloatingToolbarLayout -->
<com.google.android.material.floatingtoolbar.FloatingToolbarLayout />

<!-- LoadingIndicator -->
<com.google.android.material.loadingindicator.LoadingIndicator />

<!-- MaterialSplitButton -->
<com.google.android.material.button.MaterialSplitButton />

<!-- MaterialButtonGroup -->
<com.google.android.material.button.MaterialButtonGroup />
```

### Component Theming
```xml
<!-- Global theming via theme attributes -->
<style name="Theme.App" parent="Theme.Material3.DayNight">
    <item name="chipStyle">@style/Widget.App.Chip</item>
    <item name="materialButtonStyle">@style/Widget.App.Button</item>
</style>

<style name="Widget.App.Chip" parent="Widget.Material3.Chip.Input">
    <item name="chipBackgroundColor">@color/primary_container</item>
    <item name="chipStrokeWidth">1dp</item>
</style>

<!-- Per-component theming via style attribute -->
<com.google.android.material.button.MaterialButton
    style="@style/Widget.App.Button" />
```

---

## Best Practices

### DO ✅
```kotlin
// Use semantic colors
Text(color = MaterialTheme.colorScheme.onSurface)

// Use typography scale
Text(style = MaterialTheme.typography.bodyLarge)

// Use shape system
Card(shape = MaterialTheme.shapes.medium)

// Use dynamic colors on Android 12+
val colorScheme = if (dynamicColor) dynamicLightColorScheme(context) else LightColorScheme

// Use tonal elevation for surface hierarchy
Surface(tonalElevation = 2.dp)

// Use Material Theme Builder for consistent palettes
// https://material.io/material-theme-builder
```

### DON'T ❌
```kotlin
// ❌ Hardcoded colors
Text(color = Color(0xFF000000))

// ❌ Fixed font sizes outside typography
Text(fontSize = 14.sp)

// ❌ Hardcoded corner radius
Card(shape = RoundedCornerShape(8.dp))

// ❌ Mixing M2 and M3 components
import androidx.compose.material.Button  // ❌ M2
import androidx.compose.material3.Button // ✅ M3
```

### Experimental APIs
```kotlin
// Add opt-in for experimental APIs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen() {
    TopAppBar(...)  // Some components are experimental
}

// Or in build.gradle.kts
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
}
```

---

## When to Use Each Library

| Scenario | Library | Reason |
|----------|---------|--------|
| **New Compose UI** | Compose Material 3 | Modern, declarative, recommended |
| **Xposed/LSPosed Module Activity** | Material Components | YukiHookAPI compatibility |
| **Legacy XML Views** | Material Components | XML-based styling |
| **Mixed Compose + Views** | Both | Use together safely |
| **Dynamic Colors (Android 12+)** | Compose Material 3 | Built-in support |

---

## Official Resources

### Compose Material 3
- [Developer Guide](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [API Reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary)
- [Release Notes](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- [Samples](https://github.com/android/compose-samples)

### Material Components Android
- [GitHub](https://github.com/material-components/material-components-android)
- [Component Catalog](https://github.com/material-components/material-components-android/tree/master/catalog)
- [Documentation](https://m3.material.io/develop/android)