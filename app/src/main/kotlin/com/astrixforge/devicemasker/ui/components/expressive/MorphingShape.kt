package com.astrixforge.devicemasker.ui.components.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.ui.theme.AppMotion

/**
 * Material 3 Expressive Shape Morphing Utilities.
 *
 * These utilities enable smooth shape transitions with physics-based spring animations,
 * following Material 3 Expressive design principles.
 *
 * Use cases:
 * - Card corner radius changes on selection
 * - Button shape morphing on press
 * - Icon container shape transitions
 */

// ═══════════════════════════════════════════════════════════
// Animated Corner Radius
// ═══════════════════════════════════════════════════════════

/**
 * Creates an animated RoundedCornerShape that morphs between corner radii.
 *
 * Uses expressive spring physics for bouncy, natural-feeling transitions.
 *
 * @param targetRadius The target corner radius to animate to
 * @param label Animation label for debugging
 * @return An animated Shape that transitions smoothly
 *
 * Usage:
 * ```kotlin
 * val shape = animatedRoundedCornerShape(
 *     targetRadius = if (isSelected) 24.dp else 12.dp
 * )
 * Card(shape = shape) { ... }
 * ```
 */
@Composable
fun animatedRoundedCornerShape(
    targetRadius: Dp,
    label: String = "cornerRadiusMorph"
): Shape {
    val animatedRadius by animateFloatAsState(
        targetValue = targetRadius.value,
        animationSpec = AppMotion.Spatial.Expressive,
        label = label
    )
    return RoundedCornerShape(animatedRadius.dp)
}

/**
 * Creates an animated RoundedCornerShape using corner percentage.
 *
 * @param targetCornerPercent The target corner radius as percentage (0-50)
 * @param label Animation label for debugging
 * @return An animated Shape that transitions smoothly
 *
 * Usage:
 * ```kotlin
 * val shape = animatedRoundedCornerShapePercent(
 *     targetCornerPercent = if (isCircular) 50 else 15
 * )
 * Box(modifier = Modifier.clip(shape)) { ... }
 * ```
 */
@Composable
fun animatedRoundedCornerShapePercent(
    targetCornerPercent: Int,
    label: String = "cornerPercentMorph"
): Shape {
    val animatedPercent by animateFloatAsState(
        targetValue = targetCornerPercent.toFloat(),
        animationSpec = AppMotion.Spatial.Expressive,
        label = label
    )
    return RoundedCornerShape(animatedPercent.toInt())
}

// ═══════════════════════════════════════════════════════════
// Asymmetric Corner Shapes
// ═══════════════════════════════════════════════════════════

/**
 * Creates an animated asymmetric RoundedCornerShape.
 * Useful for cards with different corner radii on each side.
 *
 * @param topStart Target radius for top-start corner
 * @param topEnd Target radius for top-end corner
 * @param bottomEnd Target radius for bottom-end corner
 * @param bottomStart Target radius for bottom-start corner
 *
 * Usage:
 * ```kotlin
 * val shape = animatedAsymmetricCornerShape(
 *     topStart = if (expanded) 24.dp else 12.dp,
 *     topEnd = if (expanded) 24.dp else 12.dp,
 *     bottomEnd = 0.dp,
 *     bottomStart = 0.dp
 * )
 * ```
 */
@Composable
fun animatedAsymmetricCornerShape(
    topStart: Dp = 0.dp,
    topEnd: Dp = 0.dp,
    bottomEnd: Dp = 0.dp,
    bottomStart: Dp = 0.dp,
    label: String = "asymmetricCornerMorph"
): Shape {
    val animatedTopStart by animateFloatAsState(
        targetValue = topStart.value,
        animationSpec = AppMotion.Spatial.Expressive,
        label = "${label}_topStart"
    )
    val animatedTopEnd by animateFloatAsState(
        targetValue = topEnd.value,
        animationSpec = AppMotion.Spatial.Expressive,
        label = "${label}_topEnd"
    )
    val animatedBottomEnd by animateFloatAsState(
        targetValue = bottomEnd.value,
        animationSpec = AppMotion.Spatial.Expressive,
        label = "${label}_bottomEnd"
    )
    val animatedBottomStart by animateFloatAsState(
        targetValue = bottomStart.value,
        animationSpec = AppMotion.Spatial.Expressive,
        label = "${label}_bottomStart"
    )
    
    return RoundedCornerShape(
        topStart = animatedTopStart.dp,
        topEnd = animatedTopEnd.dp,
        bottomEnd = animatedBottomEnd.dp,
        bottomStart = animatedBottomStart.dp
    )
}

// ═══════════════════════════════════════════════════════════
// Predefined Shape Presets
// ═══════════════════════════════════════════════════════════

/**
 * Common shape presets for Material 3 Expressive design.
 */
object ExpressiveShapePresets {
    /** Standard card - 16dp corners */
    val CardDefault = 16.dp
    
    /** Selected/pressed card - larger corners for emphasis */
    val CardSelected = 24.dp
    
    /** Hero card - maximum expressiveness */
    val CardHero = 28.dp
    
    /** Compact elements - 8dp corners */
    val Compact = 8.dp
    
    /** Pill shape - full rounding */
    const val Pill = 50 // percentage
    
    /** Square - no rounding */
    val Square = 0.dp
}
