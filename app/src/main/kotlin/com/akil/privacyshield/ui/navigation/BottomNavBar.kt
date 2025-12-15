package com.akil.privacyshield.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.akil.privacyshield.ui.theme.AppMotion

/**
 * Bottom navigation bar for PrivacyShield.
 *
 * Uses Material 3 NavigationBar with animated icons and labels.
 * Supports spring-based animations for smooth transitions.
 *
 * @param currentDestination Current active destination route
 * @param onNavigate Callback when user selects a destination
 * @param modifier Optional modifier
 */
@Composable
fun BottomNavBar(
    currentDestination: String,
    onNavigate: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        NavDestination.bottomNavItems.forEach { destination ->
            val selected = currentDestination == destination.route

            // Animate icon scale for selection
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.1f else 1.0f,
                animationSpec = AppMotion.FastSpring,
                label = "iconScale"
            )

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    AnimatedNavIcon(
                        selectedIcon = destination.selectedIcon,
                        unselectedIcon = destination.unselectedIcon,
                        isSelected = selected,
                        modifier = Modifier.scale(scale)
                    )
                },
                label = {
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Animated icon that transitions between selected and unselected states.
 */
@Composable
private fun AnimatedNavIcon(
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(),
        label = "iconColor"
    )

    Icon(
        imageVector = if (isSelected) selectedIcon else unselectedIcon,
        contentDescription = null,
        modifier = modifier.size(24.dp),
        tint = iconColor
    )
}
