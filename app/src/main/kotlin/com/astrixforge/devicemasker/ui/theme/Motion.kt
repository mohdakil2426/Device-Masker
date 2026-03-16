package com.astrixforge.devicemasker.ui.theme

import android.animation.ValueAnimator
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * DeviceMasker Motion specifications.
 *
 * Uses spring-based animations as recommended by Material 3 Expressive. Spring animations feel more
 * natural and responsive than duration-based animations.
 */
@Stable data class MotionPolicy(val reduceMotion: Boolean = false)

val LocalMotionPolicy = staticCompositionLocalOf { MotionPolicy() }

@Composable
fun rememberMotionPolicy(): MotionPolicy {
    LocalView.current
    return MotionPolicy(reduceMotion = !ValueAnimator.areAnimatorsEnabled())
}

object AppMotion {
    @Composable fun policy(): MotionPolicy = LocalMotionPolicy.current

    @Composable
    fun <T> spatial(
        spec: FiniteAnimationSpec<T>,
        reduced: FiniteAnimationSpec<T>,
    ): FiniteAnimationSpec<T> = if (policy().reduceMotion) reduced else spec

    @Composable fun shouldReduceMotion(): Boolean = policy().reduceMotion

    fun <T> noBounce(stiffness: Float): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = stiffness)

    val ReducedAlpha: SpringSpec<Float> = noBounce(stiffness = Spring.StiffnessMedium)
    val ReducedOffset: SpringSpec<IntOffset> = noBounce(stiffness = Spring.StiffnessMedium)
    val ReducedIntSize: SpringSpec<IntSize> = noBounce(stiffness = Spring.StiffnessMedium)
    val ReducedDp: SpringSpec<Dp> = noBounce(stiffness = Spring.StiffnessMedium)

    object Spatial {
        val Expressive: SpringSpec<Float> =
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

        val Standard: SpringSpec<Float> =
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )

        val StandardIntSize: SpringSpec<IntSize> =
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )

        val Snappy: SpringSpec<Float> =
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)

        val SnappyDp: SpringSpec<Dp> =
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
    }

    object Effect {
        val Color: SpringSpec<Color> =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )

        val Alpha: SpringSpec<Float> =
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    }

    @Deprecated(
        message = "Use AppMotion.spatial(Spatial.Standard, ReducedOffset) for position animations"
    )
    val DefaultSpringOffset: SpringSpec<IntOffset> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

    @Deprecated(
        message = "Use AppMotion.spatial(Spatial.Snappy, ReducedAlpha) for quick scale animations"
    )
    val FastSpring: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    @Deprecated(message = "Use AppMotion.spatial(Spatial.SnappyDp, ReducedDp) for size animations")
    val BouncySpringDp: SpringSpec<Dp> =
        spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow)
}
