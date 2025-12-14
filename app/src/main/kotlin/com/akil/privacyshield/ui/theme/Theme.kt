package com.akil.privacyshield.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * AMOLED Dark Color Scheme.
 * 
 * Uses pure black background for OLED displays to save battery
 * and provide maximum contrast.
 */
private val AmoledDarkColorScheme = darkColorScheme(
    // Primary
    primary = PrimaryDark,
    onPrimary = Color.Black,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    
    // Secondary
    secondary = SecondaryDark,
    onSecondary = Color.Black,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = Color(0xFFCCFFFF),
    
    // Tertiary
    tertiary = TertiaryDark,
    onTertiary = Color.Black,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = Color(0xFFEDE7F6),
    
    // Error
    error = ErrorDark,
    onError = Color.Black,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFFFFDAD6),
    
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
    surfaceContainerHighest = Color(0xFF2A2A2A),
    surfaceContainerLow = AmoledSurface,
    surfaceContainerLowest = AmoledBlack,
    
    // Other
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = Color(0xFFE3E3E3),
    inverseOnSurface = Color(0xFF1A1A1A),
    inversePrimary = PrimaryLight,
    scrim = Color.Black
)

/**
 * Light Color Scheme for users who prefer light mode.
 */
private val LightColorScheme = lightColorScheme(
    // Primary
    primary = PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    
    // Secondary
    secondary = SecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = Color(0xFF00332C),
    
    // Tertiary
    tertiary = TertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = Color(0xFF21005D),
    
    // Error
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF410002)
)

/**
 * PrivacyShield Theme composable.
 * 
 * Features:
 * - Dynamic colors on Android 12+ (Material You)
 * - AMOLED black in dark mode
 * - Custom Teal/Cyan color scheme as fallback
 * 
 * @param darkTheme Whether to use dark theme. Defaults to system preference.
 * @param dynamicColor Whether to use dynamic colors on Android 12+. Defaults to true.
 * @param content The composable content to theme.
 */
@Composable
fun PrivacyShieldTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Dynamic colors on Android 12+
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                // Use dynamic dark colors but override with AMOLED black
                dynamicDarkColorScheme(context).copy(
                    background = AmoledBlack,
                    surface = AmoledBlack,
                    surfaceContainer = AmoledSurfaceContainer,
                    surfaceContainerLow = AmoledSurface,
                    surfaceContainerLowest = AmoledBlack
                )
            } else {
                dynamicLightColorScheme(context)
            }
        }
        
        // Custom dark theme (AMOLED)
        darkTheme -> AmoledDarkColorScheme
        
        // Custom light theme
        else -> LightColorScheme
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
