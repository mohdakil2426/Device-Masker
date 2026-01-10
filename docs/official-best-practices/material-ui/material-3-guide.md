# Material Design 3 & Material 3 Expressive - Complete Best Practices & Rules Guide for AI/LLM Assistance

**Version**: 1.0  
**Design System**: Material Design 3 (M3) & Material 3 Expressive  
**Date**: December 2025  
**Purpose**: Comprehensive design system rules guide for AI-assisted UI/UX development  

---

## Table of Contents

1. [Core Material 3 Philosophy](#core-material-3-philosophy)
2. [Color System & Roles](#color-system--roles)
3. [Typography Scale](#typography-scale)
4. [Shapes & Roundness](#shapes--roundness)
5. [Elevation & Surfaces](#elevation--surfaces)
6. [Motion & Animation](#motion--animation)
7. [Components Overview](#components-overview)
8. [Button Components](#button-components)
9. [Navigation Components](#navigation-components)
10. [Interaction States](#interaction-states)
11. [Accessibility Guidelines](#accessibility-guidelines)
12. [Layout & Adaptive Design](#layout--adaptive-design)
13. [Dark Mode & Dynamic Color](#dark-mode--dynamic-color)
14. [Material 3 Expressive New Features](#material-3-expressive-new-features)
15. [Component Customization](#component-customization)
16. [Jetpack Compose Implementation](#jetpack-compose-implementation)
17. [Best Practices & Anti-Patterns](#best-practices--anti-patterns)
18. [Design Token System](#design-token-system)

---

## Core Material 3 Philosophy

### Principle 1: Personalization (Material You)
**RULE**: Design systems should allow for personalization and adaptability to individual user preferences.

**WHY**: Users want apps that reflect their personal style while maintaining familiarity and consistency.

**IMPLEMENTATION**:
- Support dynamic color generation from user's wallpaper
- Allow users to customize accent colors
- Maintain consistent component behavior across themes
- Use design tokens for themability

---

### Principle 2: Accessibility is Foundational
**RULE**: Accessibility is built into every component and design decision, not added as an afterthought.

**WHY**: Accessible design benefits all users, not just those with disabilities. It improves clarity, readability, and usability.

**IMPLEMENTATION**:
- 4.5:1 contrast ratio minimum for text (WCAG AA)
- 3:1 contrast ratio for UI components
- Sufficient touch target size (48dp minimum)
- Clear focus states for keyboard navigation
- Semantic structure in component hierarchy

---

### Principle 3: Emotional Expression
**RULE**: Design should evoke positive emotions and create delightful interactions.

**WHY**: Emotional resonance increases user engagement and brand loyalty.

**IMPLEMENTATION**:
- Use motion physics for natural, fluid animations
- Choose color palettes that reflect brand personality
- Create visual hierarchy that guides attention
- Design micro-interactions that feel responsive
- Use shapes and typography expressively

---

### Principle 4: Inclusive by Default
**RULE**: Every design decision should consider diverse user needs from the start.

**WHY**: Inclusive design is better design for everyone. It works across devices, contexts, and abilities.

**IMPLEMENTATION**:
- Test with users of different abilities
- Provide text alternatives for visual content
- Support multiple input methods (touch, keyboard, voice)
- Design for various screen sizes and orientations
- Consider color blindness in color choices

---

## Color System & Roles

### Rule 1: Five Key Colors Foundation
**RULE**: Material 3 color system is built on five key colors: Primary, Secondary, Tertiary, Neutral, and Neutral Variant.

**PRIMARY COLOR**:
- Base brand color
- Used for main interactive components (buttons, active states)
- Represents the most important actions
- Default: Extracted from wallpaper (Material You)

**SECONDARY COLOR**:
- Used for less prominent components (filter chips, secondary buttons)
- Complements primary color
- Expands opportunity for color expression

**TERTIARY COLOR**:
- Provides contrasting accents
- Balances primary and secondary
- Used for special emphasis

**NEUTRAL COLORS**:
- Background and surface colors
- Text and icon colors
- Creates structure and hierarchy

**NEUTRAL VARIANT**:
- Subtle variations of neutral
- Used for dividers, borders, disabled states

---

### Rule 2: Tonal Palettes (13 Tone Values)
**RULE**: Each key color has a tonal palette of 13 tones (0-100) for creating color combinations.

**WHY**: Tonal palettes ensure accessible color combinations by default.

**TONE VALUES**:
- **0**: Pure black
- **10, 20, 30, 40**: Dark tones (for dark theme backgrounds)
- **50**: Mid-tone
- **60, 70, 80, 90**: Light tones (for light theme backgrounds)
- **95**: Very light
- **100**: Pure white

**USAGE**:
```
Primary 0:    #000000 (black)
Primary 10:   #002618 (dark)
Primary 40:   #006D3B (standard)
Primary 50:   #008843 (bright)
Primary 80:   #A8E6C4 (light)
Primary 90:   #C5FBC7 (very light)
Primary 100:  #FFFFFF (white)
```

---

### Rule 3: Color Roles (Named Slots)
**RULE**: Use semantic color roles instead of generic color names.

**ROLE NAMING PATTERN**: `[accent]-[container?]-[state?]`

**PRIMARY ROLES**:
- `primary` - Base accent color
- `onPrimary` - Text/icons on primary
- `primaryContainer` - Secondary prominence (FABs, selected chips)
- `onPrimaryContainer` - Text/icons on primary container

**SECONDARY ROLES** (same pattern):
- `secondary`, `onSecondary`
- `secondaryContainer`, `onSecondaryContainer`

**TERTIARY ROLES** (same pattern):
- `tertiary`, `onTertiary`
- `tertiaryContainer`, `onTertiaryContainer`

**SURFACE ROLES**:
- `surface` - Default background
- `onSurface` - Text/icons on surface (primary text)
- `surfaceVariant` - Alternative surface
- `onSurfaceVariant` - Secondary text on surface

**ERROR ROLES**:
- `error` - Error state color
- `onError` - Text on error
- `errorContainer` - Error container background
- `onErrorContainer` - Text on error container

**BACKGROUND**:
- `background` - App background (beneath surface)
- `onBackground` - Text on background

---

### Rule 4: Emphasis Levels
**RULE**: Use color roles to create different emphasis levels without using custom colors.

**HIGH EMPHASIS**:
- Primary color on surface
- Use primary/secondary/tertiary roles

**MEDIUM EMPHASIS**:
- Surface-variant with on-surface-variant text
- Create subtle distinction from background

**LOW EMPHASIS**:
- On-surface with reduced opacity (38% or 50%)
- Disabled states use surface color with low opacity

✅ **CORRECT**:
```
// High emphasis - button
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
)

// Medium emphasis - secondary button  
Button(
    colors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
)

// Low emphasis - tertiary action
Button(
    colors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    )
)
```

❌ **WRONG**:
```
// Custom colors - breaks theming
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFF123456),  // Hard-coded color
        contentColor = Color.White
    )
)
```

---

### Rule 5: Accessible Color Combinations
**RULE**: Always pair "on" colors with their corresponding base colors.

**REQUIREMENTS**:
- `onPrimary` ONLY on `primary`
- `onPrimaryContainer` ONLY on `primaryContainer`
- `onSurface` ONLY on `surface`
- `onSurfaceVariant` ONLY on `surfaceVariant`

✅ **CORRECT**:
```
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
)
```

❌ **WRONG**:
```
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.primaryContainer  // Wrong pairing!
    )
)
```

---

### Rule 6: Dynamic Color
**RULE**: Support dynamic color on Android 12+ (API 31+) to extract colors from user's wallpaper.

**IMPLEMENTATION**:
```kotlin
val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
val colorScheme = when {
    dynamicColor -> dynamicColorScheme(LocalContext.current)
    darkTheme -> darkColorScheme()
    else -> lightColorScheme()
}
```

**BENEFITS**:
- User personalization
- Consistency with system theme
- Increased user engagement
- Better brand alignment with user preference

---

## Typography Scale

### Rule 1: Type Scale Organization
**RULE**: Material 3 type scale has 15 styles organized into 5 roles: Display, Headline, Title, Body, Label.

**STRUCTURE**:
Each role has three sizes: Large, Medium, Small

**5 ROLES × 3 SIZES = 15 BASELINE STYLES**

Plus 15 EMPHASIZED variants (in M3 Expressive) = 30 total styles

---

### Rule 2: Type Role Usage
**RULE**: Use the correct type role based on content hierarchy and purpose.

**DISPLAY (Largest, least frequent)**:
- Large displays of promotional or hero content
- App name/logo areas
- Maximum 3 lines typical
- High visual prominence

✅ **CORRECT**:
```kotlin
Text(
    text = "Welcome to MyApp",
    style = MaterialTheme.typography.displayLarge
)
```

**HEADLINE (Page/section heads)**:
- Main page headings
- Section titles
- More prominent than titles but less than display

✅ **CORRECT**:
```kotlin
Text(
    text = "Your Orders",
    style = MaterialTheme.typography.headlineMedium
)
```

**TITLE (Subsection heads)**:
- Card titles
- Dialog titles
- List item headers
- Frequently used

✅ **CORRECT**:
```kotlin
Text(
    text = "Order #12345",
    style = MaterialTheme.typography.titleMedium
)
```

**BODY (Main content text)**:
- Paragraph text
- Descriptions
- Most of reading content
- 14sp default size
- Line height 20sp for medium

✅ **CORRECT**:
```kotlin
Text(
    text = "Your order will arrive in 2-3 business days.",
    style = MaterialTheme.typography.bodyMedium
)
```

**LABEL (Small, supporting text)**:
- Button text (typically label medium)
- Tags/chips
- Annotations
- Smallest, tightest spacing

✅ **CORRECT**:
```kotlin
Button(
    onClick = { /* ... */ },
    content = {
        Text("Submit", style = MaterialTheme.typography.labelLarge)
    }
)
```

---

### Rule 3: Emphasized Typography (M3 Expressive)
**RULE**: Use emphasized variants for increased visual impact and expressiveness.

**WHEN TO USE EMPHASIZED**:
- Highlighted content that needs attention
- Important labels or CTAs
- Featured items or hero sections
- Brand expression

✅ **CORRECT**:
```kotlin
Text(
    text = "Limited Time Offer",
    style = MaterialTheme.typography.headlineMediumEmphasis  // Emphasized variant
)
```

---

### Rule 4: Type Scale Properties
**RULE**: Each type style defines: Font family, Size, Line height, Letter spacing, Font weight.

**STANDARD PROPERTIES**:
```
Display Large:
  - Font: Roboto (default)
  - Size: 57sp
  - Line Height: 64sp
  - Letter Spacing: 0sp
  - Weight: 400 (regular)

Headline Medium:
  - Font: Roboto
  - Size: 28sp
  - Line Height: 36sp
  - Letter Spacing: 0sp
  - Weight: 500 (medium)

Title Medium:
  - Font: Roboto
  - Size: 16sp
  - Line Height: 24sp
  - Letter Spacing: 0.15sp
  - Weight: 500 (medium)

Body Medium:
  - Font: Roboto
  - Size: 14sp
  - Line Height: 20sp
  - Letter Spacing: 0.25sp
  - Weight: 400 (regular)

Label Medium:
  - Font: Roboto
  - Size: 12sp
  - Line Height: 16sp
  - Letter Spacing: 0.5sp
  - Weight: 500 (medium)
```

---

### Rule 5: Never Override Type Properties
**RULE**: Always use the predefined type styles. Only override when absolutely necessary and documented.

✅ **CORRECT**:
```kotlin
Text(
    "Order Confirmed",
    style = MaterialTheme.typography.titleMedium
)
```

❌ **WRONG**:
```kotlin
Text(
    "Order Confirmed",
    fontSize = 18.sp,  // Custom size breaks consistency
    fontWeight = FontWeight.Bold,  // Custom weight
    lineHeight = 22.sp  // Custom line height
)
```

---

## Shapes & Roundness

### Rule 1: Shape Scale (5 Levels)
**RULE**: Material 3 has 5 shape sizes: ExtraSmall, Small, Medium, Large, ExtraLarge.

**CORNER RADIUS VALUES**:
```
ExtraSmall: 4dp  (very square)
Small:      8dp
Medium:     12dp (default for most components)
Large:      16dp
ExtraLarge: 28dp (very rounded, hero elements)
```

**USAGE BY SIZE**:
- **ExtraSmall**: Form fields, checkboxes, small controls
- **Small**: Buttons, chips, cards
- **Medium**: Standard components, cards, dialogs (most common)
- **Large**: Elevated components, bottom sheets
- **ExtraLarge**: FABs, large hero containers, shape emphasis

---

### Rule 2: Shape System Customization
**RULE**: Define shapes in theme for consistent rounding across app.

✅ **CORRECT**:
```kotlin
val shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

MaterialTheme(
    shapes = shapes,
    content = { /* ... */ }
)
```

---

### Rule 3: Shape Morphing (M3 Expressive)
**RULE**: Use shape morphing to create fluid transitions between different shape sizes.

**WHY**: Creates visual continuity and sophistication in animations.

✅ **CORRECT**:
```kotlin
// In M3 Expressive, shapes can animate smoothly between different sizes
Box(
    modifier = Modifier
        .animateAsState(
            if (isSelected) 
                MaterialTheme.shapes.large 
            else 
                MaterialTheme.shapes.medium
        )
        .clip(/* animated shape */)
)
```

---

## Elevation & Surfaces

### Rule 1: Tonal Elevation (Not Shadows)
**RULE**: Material 3 uses tonal color overlays for elevation, not shadows.

**WHY**: Creates cleaner look, better in dark mode, works with any color theme.

**ELEVATION LEVELS**:
```
Level 0:  Base surface (no elevation)
Level 1:  +5% tonal overlay (subtle)
Level 2:  +8% tonal overlay
Level 3:  +11% tonal overlay
Level 4:  +12% tonal overlay
Level 5:  +14% tonal overlay (most elevated)
```

---

### Rule 2: Surface Roles
**RULE**: Use surface roles to represent different elevation levels semantically.

**SURFACE COLORS**:
- `surface` - Default surface (no elevation overlay)
- `surfaceContainer` - Light elevation (replaces surface level 1)
- `surfaceContainerHighest` - Maximum elevation
- For other levels: apply tonal overlay to `surface` with primary color

✅ **CORRECT**:
```kotlin
Card(
    modifier = Modifier.shadow(elevation = 3.dp),
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    )
)
```

---

### Rule 3: Shadow Elevation Optional
**RULE**: Shadows are optional in Material 3. Prefer tonal elevation, but shadows can supplement on light backgrounds.

**SHADOW ELEVATION SCALE**:
```
Elevation 0dp:   No shadow
Elevation 1dp:   Small shadow (FAB)
Elevation 2dp:   Card elevation
Elevation 3dp:   Dialog elevation
Elevation 4dp:   Menu elevation
Elevation 6dp:   Dialog/sheet elevation
```

✅ **CORRECT** (Tonal + Shadow):
```kotlin
Surface(
    modifier = Modifier.elevation(3.dp),
    tonalElevation = 3.dp,
    shadowElevation = 2.dp  // Optional supplement
)
```

---

## Motion & Animation

### Rule 1: Motion Physics System (M3 Expressive)
**RULE**: Use physics-based motion instead of linear easing for natural, delightful animations.

**MOTION TYPES**:

**SPATIAL ANIMATIONS** (movement):
- Objects moving across screen
- Appearing/disappearing
- Changing position

- **Fast Spatial**: Quick movements (short distance)
- **Default Spatial**: Standard movements (medium distance)  
- **Slow Spatial**: Large area transitions (full screen)

**EFFECT ANIMATIONS** (state change):
- Color changes
- Size changes
- Opacity changes

- **Fast Effect**: Quick state changes
- **Default Effect**: Standard state changes
- **Slow Effect**: Emphasized state changes

---

### Rule 2: Easing Functions
**RULE**: Use asymmetric easing for natural motion (fast entrance, slower exit).

**STANDARD EASING**:
- **Emphasized**: Bouncy overshoot (friendlier, more expressive)
- **Standard**: Linear easing (traditional, less exciting)

✅ **CORRECT** (Emphasized - bouncy):
```kotlin
animateFloatAsState(
    targetValue = targetValue,
    animationSpec = spring(dampingRatio = 0.6f)  // Bouncy effect
)
```

✅ **CORRECT** (Standard - linear):
```kotlin
animateFloatAsState(
    targetValue = targetValue,
    animationSpec = tween(
        durationMillis = 300,
        easing = LinearEasing
    )
)
```

---

### Rule 3: Duration Principles
**RULE**: Motion duration should be proportional to distance traveled and type of change.

**DURATION GUIDELINES**:
- **Short Distance**: 150-200ms
- **Medium Distance**: 200-300ms
- **Full Screen**: 300-500ms
- **Emphasis Effects**: 200-400ms

✅ **CORRECT**:
```kotlin
// Fast for small change
animateIntAsState(
    targetValue = 100,
    animationSpec = tween(150)  // Short duration
)

// Slower for large change
animateIntAsState(
    targetValue = 1000,
    animationSpec = tween(400)  // Longer duration
)
```

---

### Rule 4: No Mechanical Motion
**RULE**: Avoid purely linear motion. Always use easing curves for natural feel.

✅ **CORRECT**:
```kotlin
// Natural acceleration/deceleration
transition(
    label = "enter",
    spec = spring(stiffness = Spring.StiffnessLow)
)
```

❌ **WRONG**:
```kotlin
// Mechanical, unnatural
transition(spec = tween(easing = LinearEasing))
```

---

## Components Overview

### Material 3 Component Categories

**ACTION COMPONENTS**:
- Button (filled, tonal, outlined, text, elevated)
- FAB (standard, extended, large)
- Icon Button
- Segmented Button
- Button Group (M3 Expressive)
- Split Button (M3 Expressive)

**SELECTION COMPONENTS**:
- Checkbox
- Radio Button
- Switch
- Chip (input, filter, suggestion, assist)

**TEXT INPUT COMPONENTS**:
- Text Field (filled, outlined)
- Search Bar (M3 Expressive)

**NAVIGATION COMPONENTS**:
- Navigation Bar (bottom nav)
- Navigation Drawer
- Navigation Rail (side nav for tablets/desktops)
- Tab

**DISCLOSURE COMPONENTS**:
- Bottom Sheet
- Date Picker
- Time Picker
- Menu
- Dialog
- Snackbar
- Tooltip

**SURFACE COMPONENTS**:
- Card
- Surface
- List Item (M3 Expressive)

**LOADING & PROGRESS**:
- Linear Progress Indicator
- Circular Progress Indicator  
- Loading Indicator (M3 Expressive)

**OTHERS**:
- Toolbar (M3 Expressive, replaces bottom app bar)
- Floating Toolbar (M3 Expressive)
- Slider
- Rating Bar

---

## Button Components

### Rule 1: Button Types & Usage
**RULE**: Choose button type based on prominence and context.

**FILLED BUTTON** (Most prominent):
- Primary action
- Use sparingly, max 1 per screen section
- High contrast on surface

✅ **WHEN TO USE**:
```kotlin
Button(
    onClick = { /* submit */ },
    content = { Text("Submit Order") }
)
```

**TONAL BUTTON** (Medium prominence):
- Secondary actions
- Important but less prominent than filled
- Balanced visibility

✅ **WHEN TO USE**:
```kotlin
Button(
    onClick = { /* save */ },
    colors = ButtonDefaults.filledTonalButtonColors(),
    content = { Text("Save Draft") }
)
```

**OUTLINED BUTTON** (Lower prominence):
- Alternative actions
- Outline indicates it's secondary
- Clear, minimal design

✅ **WHEN TO USE**:
```kotlin
OutlinedButton(
    onClick = { /* cancel */ },
    content = { Text("Cancel") }
)
```

**TEXT BUTTON** (Lowest prominence):
- Tertiary actions
- Minimal visual weight
- Use for dismissible or minor actions

✅ **WHEN TO USE**:
```kotlin
TextButton(
    onClick = { /* learn more */ },
    content = { Text("Learn More") }
)
```

**ELEVATED BUTTON** (Deprecated in M3):
- Avoid new implementations
- Use Filled or Tonal instead
- May remain in legacy code

---

### Rule 2: Button Content & Structure
**RULE**: Buttons have consistent padding, icons, and text formatting.

**BUTTON CONTENT RULES**:
- **Padding**: Horizontal 24dp, Vertical 12dp (standard)
- **Icon Size**: 18dp exactly (if included)
- **Icon + Text Spacing**: 8dp minimum
- **Text Style**: Label Large (semibold)
- **Min Height**: 40dp
- **Min Width**: 64dp

✅ **CORRECT**:
```kotlin
Button(
    onClick = { /* ... */ },
    modifier = Modifier
        .height(40.dp)
        .widthIn(min = 64.dp),
    content = {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text("Add Item")
    }
)
```

---

### Rule 3: Button States
**RULE**: Buttons have visual states: enabled, hovered, focused, pressed, disabled.

**STATE BEHAVIORS**:
- **Enabled**: Full opacity, interactive
- **Hovered**: Tonal overlay (10% darker)
- **Focused**: Outline or background change
- **Pressed**: Tonal overlay (12-14%)
- **Disabled**: 38% opacity, no interaction

---

## Navigation Components

### Rule 1: Navigation Bar (Bottom Navigation)
**RULE**: Use for mobile screens with 3-5 primary destinations.

**REQUIREMENTS**:
- Max height: 80dp (with label) or 56dp (icon only)
- Icons: 24dp
- Icons above label
- Labels always shown on mobile
- Active indicator: tonal color

✅ **CORRECT**:
```kotlin
NavigationBar {
    NavigationBarItem(
        selected = currentRoute == "home",
        onClick = { navigate("home") },
        icon = { Icon(Icons.Default.Home, null) },
        label = { Text("Home") }
    )
    NavigationBarItem(
        selected = currentRoute == "search",
        onClick = { navigate("search") },
        icon = { Icon(Icons.Default.Search, null) },
        label = { Text("Search") }
    )
}
```

---

### Rule 2: Navigation Rail (Sidebar)
**RULE**: Use for tablets/desktops with 3-7 destinations, or floating navigation.

**REQUIREMENTS**:
- Width: 80dp
- Icons: 24dp
- Optional labels (can show on hover/active)
- Compact: Hide labels until expanded
- Floating rail: 80dp with rounded corners

✅ **CORRECT**:
```kotlin
NavigationRail {
    NavigationRailItem(
        selected = currentRoute == "home",
        onClick = { navigate("home") },
        icon = { Icon(Icons.Default.Home, null) },
        label = { Text("Home") }
    )
}
```

---

### Rule 3: Navigation Drawer
**RULE**: Use for lateral navigation with 5+ destinations or for deep hierarchies.

**STRUCTURE**:
- Header (optional): Branding, user info
- Navigation items: List of destinations
- Divider between sections
- Footer: Settings, help, etc.

**REQUIREMENTS**:
- Width: 360dp (standard), 256dp-360dp range
- Items have icon + label
- Active item: highlight with tonal background
- Scrim: Semi-transparent overlay when open

---

## Interaction States

### Rule 1: State Layer System
**RULE**: Visual feedback for interactive elements uses state layers (color overlays).

**STATE LAYER OPACITY**:
- **Hover**: 8% opacity overlay
- **Focus**: 12% opacity overlay
- **Pressed**: 12% opacity overlay
- **Dragged**: 16% opacity overlay

**STATE LAYER COLOR**:
- Derived from component color (usually primary or on-surface)
- Applied as transparent overlay
- Consistent across all interactive elements

✅ **CORRECT**:
```kotlin
Button(
    modifier = Modifier
        .background(MaterialTheme.colorScheme.primary)
        .clip(RoundedCornerShape(6.dp))
        .indication(interactionSource, rippleIndication),
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
)
```

---

### Rule 2: Focus State (Keyboard Navigation)
**RULE**: Every interactive element must have a visible focus indicator.

**REQUIREMENTS**:
- **Visibility**: Clear outline or highlight
- **Contrast**: Minimum 3:1 against background
- **Size**: 2dp-4dp width outline
- **Color**: Primary color or secondary accent
- **No removal**: Focus must be always visible

✅ **CORRECT**:
```kotlin
Button(
    modifier = Modifier
        .focusable()
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                // Show focus indicator
            }
        }
)
```

---

### Rule 3: Disabled State
**RULE**: Disabled elements maintain sufficient contrast while appearing inactive.

**DISABLED STATE RULES**:
- **Opacity**: 38% opacity of enabled state
- **Contrast Maintained**: Still meet minimum 3:1
- **No Interaction**: Unclickable, no hover/focus response
- **Semantics**: Mark as disabled for accessibility

✅ **CORRECT**:
```kotlin
Button(
    onClick = { /* ... */ },
    enabled = isFormValid,  // Disabled when form invalid
    content = { Text("Submit") }
)
```

---

## Accessibility Guidelines

### Rule 1: Color Contrast Ratios
**RULE**: Maintain WCAG AA compliance (4.5:1 for text, 3:1 for UI elements).

**MINIMUM RATIOS**:
- **Normal Text**: 4.5:1 (WCAG AA)
- **Large Text** (18pt+): 3:1 (WCAG AA)
- **UI Components**: 3:1 minimum
- **AAA Standard**: 7:1 text, 4.5:1 UI (enhanced)

**WCAG DEFINITION**:
Contrast ratio = (L1 + 0.05) / (L2 + 0.05)
where L = relative luminance

✅ **CORRECT** (Sufficient contrast):
```kotlin
Text(
    text = "Important information",
    color = MaterialTheme.colorScheme.onSurface,  // High contrast
    style = MaterialTheme.typography.bodyMedium
)
```

❌ **WRONG** (Insufficient contrast):
```kotlin
Text(
    text = "Subtle hint",
    color = Color.Gray,  // Gray on white might be <4.5:1
    style = MaterialTheme.typography.bodySmall
)
```

---

### Rule 2: Touch Target Size
**RULE**: Interactive elements must be at least 48dp × 48dp for touch targets.

**SIZE REQUIREMENTS**:
- **Primary interactive**: 48dp × 48dp minimum
- **Icon buttons**: 40-48dp
- **Buttons**: 40dp height minimum
- **Spacing**: 8dp between adjacent touch targets

✅ **CORRECT**:
```kotlin
IconButton(
    onClick = { /* ... */ },
    modifier = Modifier.size(48.dp)  // Meets minimum
)
```

---

### Rule 3: Semantic Structure
**RULE**: Use semantic composables and proper labeling for screen reader navigation.

**SEMANTIC REQUIREMENTS**:
- Headings: Use `Text` with `Modifier.semantics { heading() }`
- Images: Always provide `contentDescription`
- Links: Mark with `semantics { onClick(...) }`
- Form fields: Label associated with input
- Lists: Use semantic list structure

✅ **CORRECT**:
```kotlin
Image(
    painter = painterResource(R.drawable.logo),
    contentDescription = "Company logo",  // Always provided
    modifier = Modifier.size(48.dp)
)

TextField(
    value = text,
    onValueChange = { text = it },
    label = { Text("Email address") }  // Labeled
)
```

---

### Rule 4: Focus Management
**RULE**: Ensure logical focus order for keyboard navigation.

**FOCUS REQUIREMENTS**:
- First focusable element: Clear starting point
- Tab order: Logical, left-to-right, top-to-bottom
- Focus visible: Always 100% visible indicator
- No focus traps: Can always navigate away
- Return focus: After dismissing overlays

✅ **CORRECT**:
```kotlin
LaunchedEffect(Unit) {
    focusRequester.requestFocus()  // Set initial focus
}

TextField(
    value = email,
    onValueChange = { email = it },
    modifier = Modifier
        .focusRequester(focusRequester)
        .onKeyEvent { event ->
            if (event.key == Key.Tab) {
                // Handle tab for custom focus logic
                true
            } else false
        }
)
```

---

## Layout & Adaptive Design

### Rule 1: Breakpoints for Responsive Layout
**RULE**: Design for specific window size classes, not fixed pixel breakpoints.

**MATERIAL 3 WINDOW CLASSES**:

**COMPACT** (Mobile):
- Width: < 600dp
- Single column layout
- Bottom navigation
- Full-width components

**MEDIUM** (Tablet landscape / Large phones):
- Width: 600dp - 839dp
- Can show 2 panes
- Navigation rail or bottom nav
- Wider components with margins

**EXPANDED** (Tablet/Desktop):
- Width: ≥ 840dp
- Can show 2-3 panes
- Navigation rail (side nav)
- Component constraints

✅ **CORRECT**:
```kotlin
BoxWithConstraints {
    when {
        maxWidth < 600.dp -> {
            // Compact layout
            CompactLayout()
        }
        maxWidth < 840.dp -> {
            // Medium layout
            MediumLayout()
        }
        else -> {
            // Expanded layout
            ExpandedLayout()
        }
    }
}
```

---

### Rule 2: Canonical Layouts
**RULE**: Use three canonical regions: Navigation, Body, Supplemental (optional).

**NAVIGATION REGION**:
- Bottom Navigation (compact)
- Navigation Rail (medium/expanded)
- Navigation Drawer (all sizes)

**BODY REGION**:
- Main content
- Takes up remaining space
- Can be divided into panes

**SUPPLEMENTAL REGION**:
- Additional details
- Available on expanded screens only
- Usually 40% width or 280-320dp

✅ **CORRECT** (Adaptive layout):
```kotlin
when (windowSizeClass) {
    WindowSizeClass.Compact -> {
        Column {
            BodyContent()  // Full width
            NavigationBar()  // Bottom
        }
    }
    WindowSizeClass.Medium -> {
        Row {
            NavigationRail()
            BodyContent()
        }
    }
    WindowSizeClass.Expanded -> {
        Row {
            NavigationRail()
            BodyContent(modifier = Modifier.weight(0.6f))
            SupplementalPane(modifier = Modifier.weight(0.4f))
        }
    }
}
```

---

### Rule 3: Responsive Component Behavior
**RULE**: Components adapt their layout and visibility based on available space.

**ADAPTATION PATTERNS**:
- **Reposition**: Move elements to better use space (e.g., horizontal card layout on large screens)
- **Reflow**: Adjust wrapping and grouping
- **Replace**: Use different components (tabs → rail navigation)
- **Reveal**: Show/hide details based on space

---

## Dark Mode & Dynamic Color

### Rule 1: Dark Theme Implementation
**RULE**: Provide both light and dark color schemes with appropriate contrast.

**DARK THEME PRINCIPLES**:
- **Background**: Dark surface (not pure black, typically #121212)
- **Surface**: Slightly lighter than background
- **Elevation**: Lighter surfaces for elevated elements
- **Text**: Light text on dark backgrounds (typically #FFFBFE)
- **Contrast**: Maintain 4.5:1 for text

✅ **CORRECT**:
```kotlin
val lightColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),
    surface = Color(0xFFFFFBFE)
)

val darkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    surface = Color(0xFF121212)
)

MaterialTheme(
    colorScheme = if (darkTheme) darkColorScheme else lightColorScheme
)
```

---

### Rule 2: Dynamic Color (Material You)
**RULE**: Extract accent colors from user's wallpaper on Android 12+.

**IMPLEMENTATION**:
```kotlin
val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
val colorScheme = when {
    dynamicColor && darkTheme -> 
        dynamicDarkColorScheme(LocalContext.current)
    dynamicColor -> 
        dynamicLightColorScheme(LocalContext.current)
    darkTheme -> darkColorScheme()
    else -> lightColorScheme()
}
```

**BENEFITS**:
- Personalization
- System consistency
- Enhanced engagement
- No additional user input needed

---

## Material 3 Expressive New Features

### Rule 1: Expressive Motion Physics
**RULE**: Use new motion physics system for more natural, delightful animations.

**MOTION SCHEME** (Replaces duration/easing):
- **Standard**: Conservative, linear-based (for professional apps)
- **Expressive**: Physics-based with overshoot (for engaging apps)

**MOTION TYPES**:
- **Spatial**: For position changes
- **Effect**: For property changes (color, opacity, size)

**THREE SPEEDS**:
- **Fast**: Quick, responsive
- **Default**: Normal pace
- **Slow**: Leisurely, emphasized

✅ **CORRECT**:
```kotlin
// Expressive motion with bounce
animateFloatAsState(
    targetValue = 1f,
    animationSpec = spring(
        dampingRatio = 0.6f,  // Creates bounce
        stiffness = Spring.StiffnessLow
    )
)
```

---

### Rule 2: Shape System Expansion
**RULE**: M3 Expressive adds 35 new shapes for greater visual expressiveness.

**NEW SHAPE FEATURES**:
- Shape morphing (smooth transitions)
- More corner radius options
- Emphasis shapes
- Dynamic shape combinations

---

### Rule 3: New Components
**RULE**: M3 Expressive adds new components for modern app patterns.

**NEW COMPONENTS**:
- **Toolbar** (replaces bottom app bar)
- **Floating Toolbar** (for contextual actions)
- **Button Group** (grouped button actions)
- **FAB Menu** (expandable FAB)
- **Split Button** (combined button + dropdown)
- **Loading Indicator** (animated loading)
- **List Item** (enhanced list component)

✅ **CORRECT** (Toolbar usage):
```kotlin
Toolbar(
    actions = {
        ToolbarAction(
            icon = Icons.Default.Save,
                label = "Save",
                onClick = { /* ... */ }
        )
    },
    modifier = Modifier
        .fillMaxWidth()
        .height(80.dp)
)
```

---

## Component Customization

### Rule 1: Color Customization
**RULE**: Use `Defaults` objects to customize component colors by state.

✅ **CORRECT**:
```kotlin
val customButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
)

Button(
    onClick = { /* ... */ },
    colors = customButtonColors
)
```

---

### Rule 2: Elevation Customization
**RULE**: Use tonal and shadow elevation parameters for custom elevation.

✅ **CORRECT**:
```kotlin
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(
        defaultElevation = 3.dp,
        pressedElevation = 2.dp,
        focusedElevation = 4.dp
    )
)
```

---

### Rule 3: Shape Customization
**RULE**: Define custom shapes in theme, not on individual components.

✅ **CORRECT**:
```kotlin
val customShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

MaterialTheme(shapes = customShapes)
```

---

## Jetpack Compose Implementation

### Rule 1: MaterialTheme Setup
**RULE**: Wrap app content in MaterialTheme with color, typography, shapes.

✅ **CORRECT**:
```kotlin
@Composable
fun MyAppTheme(
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
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
```

---

### Rule 2: Using Color Scheme
**RULE**: Always access colors via `MaterialTheme.colorScheme`, never hardcode.

✅ **CORRECT**:
```kotlin
Text(
    "Hello, Material 3!",
    color = MaterialTheme.colorScheme.onSurface
)
```

❌ **WRONG**:
```kotlin
Text(
    "Hello, Material 3!",
    color = Color.Black  // Hard-coded, ignores theme
)
```

---

### Rule 3: Component Usage
**RULE**: Always use Material 3 composables from `androidx.compose.material3` package.

✅ **CORRECT**:
```kotlin
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Card
```

❌ **WRONG**:
```kotlin
import androidx.compose.material.Button  // Material 2!
```

---

## Best Practices & Anti-Patterns

### ✅ BEST PRACTICES

**1. Use Design Tokens**:
- Define all colors, typography, shapes in theme
- Use `MaterialTheme` values throughout app
- Never hardcode color/size values

**2. Semantic Color Roles**:
- Always use "on" + "container" pairs
- Never mix color roles
- Primary on Primary, Secondary on Secondary

**3. Consistent Spacing**:
- Use 4dp, 8dp, 12dp, 16dp, 24dp, 32dp multiples
- Define spacing in theme
- Consistent padding across components

**4. Accessibility First**:
- 4.5:1 contrast minimum
- 48dp touch targets
- Semantic structure
- Focus management

**5. Dynamic Color**:
- Support wallpaper-based theming
- Graceful fallback for older Android
- Test with Material Theme Builder

**6. Motion Purpose**:
- Motion should have clear purpose (feedback, hierarchy, emotion)
- Duration proportional to distance
- Avoid purely decorative motion

---

### ❌ ANTI-PATTERNS - NEVER DO THIS

**FORBIDDEN 1: Hardcoded Colors**
```kotlin
// ❌ NEVER
Text("Hello", color = Color(0xFF123456))

// ✅ USE
Text("Hello", color = MaterialTheme.colorScheme.onSurface)
```

**FORBIDDEN 2: Mixing Color Roles**
```kotlin
// ❌ NEVER - mismatched "on" colors
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onPrimary  // Wrong!
    )
)

// ✅ USE
Button(
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary  // Correct
    )
)
```

**FORBIDDEN 3: Material 2 & 3 Mix**
```kotlin
// ❌ NEVER
import androidx.compose.material.Button  // M2
import androidx.compose.material3.Text    // M3

// ✅ USE - all Material 3
import androidx.compose.material3.Button
import androidx.compose.material3.Text
```

**FORBIDDEN 4: Custom Elevation Everywhere**
```kotlin
// ❌ NEVER
Card(elevation = 100.dp)  // Excessive
Button(elevation = 50.dp)  // Override defaults

// ✅ USE
Card()  // Default elevation
Card(elevation = CardDefaults.cardElevation(3.dp))  // Minimal customization
```

**FORBIDDEN 5: No Focus Handling**
```kotlin
// ❌ NEVER - no focus indicator
Button(onClick = { /* ... */ })

// ✅ USE - focus built-in
Button(onClick = { /* ... */ })  // Focus automatic with Material 3
```

**FORBIDDEN 6: Insufficient Contrast**
```kotlin
// ❌ NEVER - gray on light gray
Text("Subtle", color = Color.Gray)

// ✅ USE - sufficient contrast
Text("Clear", color = MaterialTheme.colorScheme.onSurface)
```

---

## Design Token System

### Rule 1: Token Naming Convention
**RULE**: Use semantic naming pattern for design tokens.

**PATTERN**: `[category]-[purpose]-[optional-state]`

**EXAMPLES**:
```
color-primary
color-primary-container
color-on-primary
color-surface
color-surface-variant
color-error

shape-small
shape-medium
shape-large

typography-display-large
typography-headline-medium
typography-body-medium
typography-label-small

elevation-level-0
elevation-level-3
elevation-level-5
```

---

### Rule 2: Token Organization
**RULE**: Organize tokens hierarchically by category and use case.

**HIERARCHY**:
1. **Primitive Tokens**: Base values (colors, sizes)
2. **Semantic Tokens**: Named by purpose (primary, surface)
3. **Component Tokens**: Component-specific (button-background)

✅ **CORRECT**:
```kotlin
// Level 1: Primitives (not typically exposed)
val md_theme_light_primary = Color(0xFF476810)

// Level 2: Semantic (theme-level)
val primary = Color(0xFF476810)
val onPrimary = Color(0xFFFFFFFF)
val primaryContainer = Color(0xFFC7F089)

// Level 3: Component (component-level)
val buttonBackground = primary
val buttonForeground = onPrimary
```

---

### Rule 3: Token Documentation
**RULE**: Document all tokens with usage guidelines.

✅ **CORRECT**:
```kotlin
/**
 * Primary brand color
 * Used for:
 * - Main action buttons
 * - Active states
 * - Focused elements
 * - Primary navigation
 */
val primary = Color(0xFF476810)
```

---

## Quick Reference

### Color Role Pairs

| Container | On Container | Usage |
|-----------|--------------|-------|
| primary | onPrimary | Main actions, highlights |
| primaryContainer | onPrimaryContainer | FAB, selected chips |
| secondary | onSecondary | Secondary buttons |
| secondaryContainer | onSecondaryContainer | Secondary highlights |
| tertiary | onTertiary | Accent, contrast |
| tertiaryContainer | onTertiaryContainer | Tertiary emphasis |
| surface | onSurface | Main content, text |
| surfaceVariant | onSurfaceVariant | Secondary content |
| error | onError | Error states |
| errorContainer | onErrorContainer | Error highlights |

---

### Typography Usage

| Style | Size | Usage |
|-------|------|-------|
| Display | 57sp | Hero content, app names |
| Headline | 28sp | Page titles, section heads |
| Title | 16sp | Card titles, dialog titles |
| Body | 14sp | Main content, descriptions |
| Label | 12sp | Button text, chips, tags |

---

### Motion Duration

| Distance | Duration | Animation Type |
|----------|----------|-----------------|
| Short | 150ms | State change, small movement |
| Medium | 300ms | Component transition |
| Long | 500ms | Full screen transition |

---

## Version Information

- **Material Design 3 Version**: Latest (December 2025)
- **Material 3 Expressive**: Included
- **Jetpack Compose**: 1.x+ (material3 module)
- **Android Target**: API 21+ (dynamic color: API 31+)
- **Document Version**: 1.0
- **Last Updated**: December 2025

---

## References

- [Material Design 3 Official](https://m3.material.io)
- [Material Design 3 Develop](https://m3.material.io/develop)
- [Jetpack Compose Material 3](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Material 3 for Wear](https://developer.android.com/design/ui/wear/guides/get-started/apply)
- [WCAG 2.1 Guidelines](https://www.w3.org/TR/WCAG21/)

---

**End of Document**
