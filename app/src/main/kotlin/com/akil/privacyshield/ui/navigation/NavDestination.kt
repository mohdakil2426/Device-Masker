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
 * Navigation routes as constants.
 * Using simple string routes to avoid sealed class initialization issues.
 */
object NavRoutes {
    const val HOME = "home"
    const val SPOOF = "spoof"
    const val SETTINGS = "settings"
}

/**
 * Navigation item data class for bottom navigation.
 */
data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * All bottom navigation items.
 */
val bottomNavItems: List<NavItem> = listOf(
    NavItem(
        route = NavRoutes.HOME,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    NavItem(
        route = NavRoutes.SPOOF,
        label = "Spoof",
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune
    ),
    NavItem(
        route = NavRoutes.SETTINGS,
        label = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
)
