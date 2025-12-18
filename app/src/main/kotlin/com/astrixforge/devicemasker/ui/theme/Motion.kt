package com.astrixforge.devicemasker.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * DeviceMasker Motion specifications.
 *
 * Uses spring-based animations as recommended by Material 3 Expressive. Spring animations feel more
 * natural and responsive than duration-based animations.
 *
 * ## Material 3 Expressive Motion Categories:
 *
 * - **Spatial**: For position, size, rotation, scale - these CAN overshoot target values
 * - **Effect**: For color, opacity, blur - these should NOT overshoot
 *
 * Use [Spatial] springs for interactive elements that need expressive feedback.
 * Use [Effect] springs for smooth visual transitions without bounce.
 */
object AppMotion {

    // ╔═══════════════════════════════════════════════════════════════════════════╗
    // ║                    MATERIAL 3 EXPRESSIVE SPRINGS                           ║
    // ╚═══════════════════════════════════════════════════════════════════════════╝

    /**
     * Spatial springs for animating position, size, rotation, and scale.
     * These springs CAN overshoot their target values for expressive feedback.
     */
    object Spatial {
        /**
         * Expressive spring for hero moments and prominent interactions.
         * High bounce, low stiffness - creates noticeable overshoot.
         * Use for: Button presses, card selection, FAB animations.
         */
        val Expressive: SpringSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // 0.5 - moderate bounce
            stiffness = Spring.StiffnessLow                 // 200 - slow, expressive
        )

        val ExpressiveDp: SpringSpec<Dp> = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )

        /**
         * Standard spring for typical interactions.
         * Subtle bounce, medium stiffness - minimal overshoot.
         * Use for: Navigation transitions, list item animations.
         */
        val Standard: SpringSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,    // 0.75 - slight bounce
            stiffness = Spring.StiffnessMediumLow          // 400 - moderate speed
        )

        val StandardDp: SpringSpec<Dp> = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )

        val StandardOffset: SpringSpec<IntOffset> = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )

        val StandardIntSize: SpringSpec<IntSize> = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        )

        /**
         * Snappy spring for quick feedback.
         * Minimal bounce, high stiffness - fast response.
         * Use for: Toggle switches, quick icon animations.
         */
        val Snappy: SpringSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,    // 0.75 - slight bounce
            stiffness = Spring.StiffnessMedium             // 1500 - fast
        )

        val SnappyDp: SpringSpec<Dp> = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }

    /**
     * Effect springs for animating color, opacity, and blur.
     * These springs should NOT overshoot their target values.
     */
    object Effect {
        /**
         * Smooth spring for color transitions.
         * No bounce, medium stiffness - smooth color change.
         * Use for: Background color, icon tint, status colors.
         */
        val Color: SpringSpec<Color> = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,     // 1.0 - no bounce
            stiffness = Spring.StiffnessMediumLow          // 400 - smooth transition
        )

        /**
         * Smooth spring for opacity/alpha transitions.
         * No bounce, medium stiffness - smooth fade.
         * Use for: Fade in/out, alpha changes, visibility.
         */
        val Alpha: SpringSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,     // 1.0 - no bounce
            stiffness = Spring.StiffnessMedium             // 1500 - responsive
        )

        /**
         * Quick spring for immediate visual feedback.
         * No bounce, high stiffness - instant response.
         * Use for: Hover states, immediate color feedback.
         */
        val Quick: SpringSpec<Float> = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,     // 1.0 - no bounce
            stiffness = Spring.StiffnessHigh               // 10000 - instant
        )
    }

    // ╔═══════════════════════════════════════════════════════════════════════════╗
    // ║                         LEGACY SPRINGS (Deprecated)                        ║
    // ║              Use Spatial.* or Effect.* for new code                        ║
    // ╚═══════════════════════════════════════════════════════════════════════════╝

    // Default Spring - For most transitions
    @Deprecated(
        message = "Use Spatial.Standard for position/scale or Effect.Color for colors",
        replaceWith = ReplaceWith("Spatial.Standard")
    )
    val DefaultSpring: SpringSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        )

    @Deprecated(
        message = "Use Spatial.StandardDp for position/size animations",
        replaceWith = ReplaceWith("Spatial.StandardDp")
    )
    val DefaultSpringDp: SpringSpec<Dp> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

    @Deprecated(
        message = "Use Spatial.StandardOffset for position animations",
        replaceWith = ReplaceWith("Spatial.StandardOffset")
    )
    val DefaultSpringOffset: SpringSpec<IntOffset> =
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        )

    // Fast Spring - For quick feedback
    @Deprecated(
        message = "Use Spatial.Snappy for quick scale or Effect.Quick for colors",
        replaceWith = ReplaceWith("Spatial.Snappy")
    )
    val FastSpring: SpringSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        )

    @Deprecated(
        message = "Use Spatial.SnappyDp for quick size animations",
        replaceWith = ReplaceWith("Spatial.SnappyDp")
    )
    val FastSpringDp: SpringSpec<Dp> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    // Bouncy Spring - For playful interactions
    @Deprecated(
        message = "Use Spatial.Expressive for bouncy scale/position animations",
        replaceWith = ReplaceWith("Spatial.Expressive")
    )
    val BouncySpring: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow)

    @Deprecated(
        message = "Use Spatial.ExpressiveDp for bouncy size animations",
        replaceWith = ReplaceWith("Spatial.ExpressiveDp")
    )
    val BouncySpringDp: SpringSpec<Dp> =
        spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow)

    // Gentle Spring - For subtle, slow animations
    @Deprecated(
        message = "Use Spatial.Standard for subtle animations",
        replaceWith = ReplaceWith("Spatial.Standard")
    )
    val GentleSpring: SpringSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow,
        )

    @Deprecated(
        message = "Use Spatial.StandardDp for subtle size animations",
        replaceWith = ReplaceWith("Spatial.StandardDp")
    )
    val GentleSpringDp: SpringSpec<Dp> =
        spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow)

    // Snappy Spring - For instant feedback
    @Deprecated(
        message = "Use Spatial.Snappy for quick scale or Effect.Quick for instant feedback",
        replaceWith = ReplaceWith("Spatial.Snappy")
    )
    val SnappySpring: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

    @Deprecated(
        message = "Use Spatial.SnappyDp for quick size animations",
        replaceWith = ReplaceWith("Spatial.SnappyDp")
    )
    val SnappySpringDp: SpringSpec<Dp> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
}
