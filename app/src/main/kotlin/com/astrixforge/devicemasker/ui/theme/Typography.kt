package com.astrixforge.devicemasker.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * DeviceMasker Typography.
 *
 * Uses Material 3 typography scale with default system font. Optimized for readability on mobile
 * devices.
 */
val AppTypography =
    Typography(
        // Display styles
        displayLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp,
            ),
        displayMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 45.sp,
                lineHeight = 52.sp,
                letterSpacing = 0.sp,
            ),
        displaySmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp,
            ),

        // Headline styles
        headlineLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),

        // Title styles
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),

        // Body styles
        bodyLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),

        // Label styles
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )

// ═══════════════════════════════════════════════════════════
// Material 3 Expressive Typography Extensions
// ═══════════════════════════════════════════════════════════

/**
 * Creates an emphasized version of this text style.
 * 
 * Material 3 Expressive uses heavier weights for visual hierarchy:
 * - Normal → Medium
 * - Medium → SemiBold
 * - SemiBold → Bold
 * - Bold → ExtraBold
 *
 * Usage:
 * ```kotlin
 * Text(
 *     text = "Important Label",
 *     style = MaterialTheme.typography.labelLarge.emphasized()
 * )
 * ```
 */
fun TextStyle.emphasized(): TextStyle = this.copy(
    fontWeight = when (this.fontWeight) {
        FontWeight.Thin -> FontWeight.Light
        FontWeight.ExtraLight -> FontWeight.Normal
        FontWeight.Light -> FontWeight.Normal
        FontWeight.Normal -> FontWeight.Medium
        FontWeight.Medium -> FontWeight.SemiBold
        FontWeight.SemiBold -> FontWeight.Bold
        FontWeight.Bold -> FontWeight.ExtraBold
        FontWeight.ExtraBold -> FontWeight.Black
        FontWeight.Black -> FontWeight.Black
        else -> FontWeight.Medium
    }
)

/**
 * Creates a de-emphasized version of this text style.
 * Useful for secondary text that should be less prominent.
 */
fun TextStyle.deemphasized(): TextStyle = this.copy(
    fontWeight = when (this.fontWeight) {
        FontWeight.Black -> FontWeight.ExtraBold
        FontWeight.ExtraBold -> FontWeight.Bold
        FontWeight.Bold -> FontWeight.SemiBold
        FontWeight.SemiBold -> FontWeight.Medium
        FontWeight.Medium -> FontWeight.Normal
        FontWeight.Normal -> FontWeight.Light
        FontWeight.Light -> FontWeight.ExtraLight
        FontWeight.ExtraLight -> FontWeight.Thin
        FontWeight.Thin -> FontWeight.Thin
        else -> FontWeight.Normal
    }
)
