package com.astrixforge.devicemasker.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.astrixforge.devicemasker.ui.screens.ThemeMode

/**
 * AMOLED Dark Color Scheme.
 *
 * Uses pure black background for OLED displays to save battery and provide maximum contrast.
 */
private val AmoledDarkColorScheme =
    darkColorScheme(
        // Primary
        primary = PrimaryDark,
        onPrimary = Color.Black,
        primaryContainer = PrimaryContainer,
        onPrimaryContainer = OnPrimaryContainer,

        // Secondary
        secondary = SecondaryDark,
        onSecondary = Color.Black,
        secondaryContainer = SecondaryContainer,
        onSecondaryContainer = OnSecondaryContainer,

        // Tertiary
        tertiary = TertiaryDark,
        onTertiary = Color.Black,
        tertiaryContainer = TertiaryContainer,
        onTertiaryContainer = OnTertiaryContainer,

        // Error
        error = ErrorDark,
        onError = Color.Black,
        errorContainer = ErrorContainer,
        onErrorContainer = OnErrorContainer,

        // Background & Surface (AMOLED Black)
        background = AmoledBlack,
        onBackground = OnSurfaceDark,
        surface = AmoledBlack,
        onSurface = OnSurfaceDark,
        surfaceVariant = AmoledSurfaceVariant,
        onSurfaceVariant = OnSurfaceVariantDark,

        // Surface containers
        surfaceContainer = AmoledSurfaceContainer,
        surfaceContainerHigh = AmoledSurfaceContainerHigh,
        surfaceContainerHighest = AmoledSurfaceContainerHighest,
        surfaceContainerLow = AmoledSurface,
        surfaceContainerLowest = AmoledBlack,

        // Other
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        inverseSurface = OnSurfaceDark,
        inverseOnSurface = AmoledSurfaceVariant,
        inversePrimary = PrimaryLight,
        scrim = Color.Black,
    )

/** Light Color Scheme for users who prefer light mode. */
private val LightColorScheme =
    lightColorScheme(
        // Primary
        primary = PrimaryLight,
        onPrimary = Color.White,
        primaryContainer = PrimaryContainerLight,
        onPrimaryContainer = OnPrimaryContainerLight,

        // Secondary
        secondary = SecondaryLight,
        onSecondary = Color.White,
        secondaryContainer = SecondaryContainerLight,
        onSecondaryContainer = OnSecondaryContainerLight,

        // Tertiary
        tertiary = TertiaryLight,
        onTertiary = Color.White,
        tertiaryContainer = TertiaryContainerLight,
        onTertiaryContainer = OnTertiaryContainerLight,

        // Error
        error = ErrorLight,
        onError = Color.White,
        errorContainer = ErrorContainerLight,
        onErrorContainer = OnErrorContainerLight,
    )

/**
 * Device Masker Theme composable with ThemeMode support.
 *
 * This is the recommended way to apply theming - pass the ThemeMode directly and let the theme
 * handle system dark mode detection internally.
 *
 * @param themeMode The theme mode (SYSTEM, LIGHT, DARK). Defaults to SYSTEM.
 * @param amoledBlack Whether to use AMOLED pure black when in dark mode. Defaults to true.
 * @param dynamicColor Whether to use dynamic colors on Android 12+. Defaults to true.
 * @param content The composable content to theme.
 */
@Composable
fun DeviceMaskerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    amoledBlack: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Determine dark theme based on mode
    // IMPORTANT: isSystemInDarkTheme() is evaluated inside this composable
    // to ensure it's in the correct composition context
    val darkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    DeviceMaskerThemeInternal(
        darkTheme = darkTheme,
        amoledBlack = amoledBlack,
        dynamicColor = dynamicColor,
        content = content,
    )
}

/**
 * Internal Device Masker Theme implementation.
 *
 * Use [DeviceMaskerTheme] with ThemeMode parameter instead for proper system theme detection.
 */
@Composable
private fun DeviceMaskerThemeInternal(
    darkTheme: Boolean,
    amoledBlack: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val contrast = rememberSystemContrast(context)
    val baseColorScheme =
        when {
            // Dynamic colors on Android 12+
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) {
                    if (amoledBlack) {
                        // Use dynamic dark colors but override with AMOLED black
                        dynamicDarkColorScheme(context)
                            .copy(
                                background = AmoledBlack,
                                surface = AmoledBlack,
                                surfaceContainer = AmoledSurfaceContainer,
                                surfaceContainerLow = AmoledSurface,
                                surfaceContainerLowest = AmoledBlack,
                            )
                    } else {
                        // Regular dynamic dark colors
                        dynamicDarkColorScheme(context)
                    }
                } else {
                    dynamicLightColorScheme(context)
                }
            }

            // Custom dark themes
            darkTheme -> {
                if (amoledBlack) {
                    AmoledDarkColorScheme
                } else {
                    // Regular dark theme (not pure black) - using Material 3 default dark with
                    // our primaries
                    darkColorScheme(
                        // Primary
                        primary = PrimaryDark,
                        onPrimary = Color.Black,
                        primaryContainer = PrimaryContainer,
                        onPrimaryContainer = OnPrimaryContainer,
                        // Secondary
                        secondary = SecondaryDark,
                        onSecondary = Color.Black,
                        secondaryContainer = SecondaryContainer,
                        onSecondaryContainer = OnSecondaryContainer,
                        // Tertiary
                        tertiary = TertiaryDark,
                        onTertiary = Color.Black,
                        tertiaryContainer = TertiaryContainer,
                        onTertiaryContainer = OnTertiaryContainer,
                        // Error
                        error = ErrorDark,
                        onError = Color.Black,
                        errorContainer = ErrorContainer,
                        onErrorContainer = OnErrorContainer,
                        // Background & Surface (dark gray, not pure black)
                        background = DarkSurface,
                        onBackground = OnSurfaceDark,
                        surface = DarkSurface,
                        onSurface = OnSurfaceDark,
                        surfaceVariant = DarkSurfaceVariant,
                        onSurfaceVariant = OnSurfaceVariantDark,
                        // Surface containers
                        surfaceContainer = DarkSurfaceContainer,
                        surfaceContainerHigh = DarkSurfaceContainerHigh,
                        surfaceContainerHighest = DarkSurfaceContainerHighest,
                        surfaceContainerLow = DarkSurfaceContainerLow,
                        surfaceContainerLowest = DarkSurfaceContainerLowest,
                        // Other
                        outline = OutlineDark,
                        outlineVariant = OutlineVariantDark,
                        inverseSurface = OnSurfaceDark,
                        inverseOnSurface = DarkSurfaceContainer,
                        inversePrimary = PrimaryLight,
                        scrim = Color.Black,
                    )
                }
            }

            // Custom light theme
            else -> LightColorScheme
        }
    val colorScheme = baseColorScheme.withContrastPreference(contrast, darkTheme)

    CompositionLocalProvider(
        LocalMotionPolicy provides rememberMotionPolicy(),
        LocalEmphasizedTypography provides AppEmphasizedTypography,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

@Composable
private fun rememberSystemContrast(context: Context): Float {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return 0f

    val uiModeManager = remember(context) { context.getSystemService(UiModeManager::class.java) }
    var contrast by remember(uiModeManager) { mutableFloatStateOf(uiModeManager?.contrast ?: 0f) }

    DisposableEffect(uiModeManager) {
        if (uiModeManager == null) return@DisposableEffect onDispose {}

        val listener =
            UiModeManager.ContrastChangeListener { nextContrast -> contrast = nextContrast }
        uiModeManager.addContrastChangeListener(context.mainExecutor, listener)
        onDispose { uiModeManager.removeContrastChangeListener(listener) }
    }

    return contrast
}

private fun ColorScheme.withContrastPreference(contrast: Float, darkTheme: Boolean): ColorScheme {
    if (contrast < 0.33f) return this

    return if (darkTheme) {
        copy(
            onSurface = OnSurfaceHighContrastDark,
            onSurfaceVariant = OnSurfaceVariantHighContrastDark,
            outline = OutlineHighContrastDark,
            outlineVariant = OutlineHighContrastDark,
        )
    } else {
        copy(
            onSurface = OnSurfaceHighContrastLight,
            onSurfaceVariant = OnSurfaceVariantHighContrastLight,
            outline = OutlineHighContrastLight,
            outlineVariant = OutlineHighContrastLight,
        )
    }
}
