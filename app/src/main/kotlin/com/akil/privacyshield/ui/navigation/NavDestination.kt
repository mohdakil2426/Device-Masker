package com.akil.privacyshield.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for PrivacyShield MVP.
 *
 * Sealed class providing type-safe navigation with associated icons
 * and labels for each destination.
 */
sealed class NavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    /**
     * Home screen - Module status and quick overview.
     */
    data object Home : NavDestination(
        route = "home",
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    /**
     * Spoof settings - Configure spoof values.
     */
    data object Spoof : NavDestination(
        route = "spoof",
        label = "Spoof",
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    )

    /**
     * Settings screen - App preferences.
     */
    data object Settings : NavDestination(
        route = "settings",
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    companion object {
        /**
         * All bottom navigation destinations.
         */
        val bottomNavItems = listOf(Home, Spoof, Settings)

        /**
         * Get destination by route.
         */
        fun fromRoute(route: String?): NavDestination {
            return when (route) {
                Home.route -> Home
                Spoof.route -> Spoof
                Settings.route -> Settings
                else -> Home
            }
        }
    }
}
