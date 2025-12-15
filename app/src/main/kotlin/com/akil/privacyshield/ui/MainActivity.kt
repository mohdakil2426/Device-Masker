package com.akil.privacyshield.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.akil.privacyshield.PrivacyShieldApp
import com.akil.privacyshield.data.repository.SpoofRepository
import com.akil.privacyshield.ui.navigation.BottomNavBar
import com.akil.privacyshield.ui.navigation.NavDestination
import com.akil.privacyshield.ui.screens.HomeScreen
import com.akil.privacyshield.ui.screens.SettingsScreen
import com.akil.privacyshield.ui.screens.SpoofSettingsScreen
import com.akil.privacyshield.ui.theme.AppMotion
import com.akil.privacyshield.ui.theme.PrivacyShieldTheme
import timber.log.Timber

/**
 * Main Activity for PrivacyShield.
 *
 * Uses Jetpack Compose with Material 3 for the entire UI with edge-to-edge display.
 * Features:
 * - Bottom navigation with animated transitions
 * - Three main screens: Home, Spoof Settings, Settings
 * - Spring-based animations for smooth navigation
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        Timber.d("MainActivity created, module active: ${PrivacyShieldApp.isXposedModuleActive}")

        setContent {
            PrivacyShieldTheme {
                val repository = remember { SpoofRepository.getInstance(applicationContext) }
                PrivacyShieldApp(repository = repository)
            }
        }
    }
}

/**
 * Main app composable with navigation.
 */
@Composable
fun PrivacyShieldApp(
    repository: SpoofRepository,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavDestination.Home.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavBar(
                currentDestination = currentRoute,
                onNavigate = { destination ->
                    navController.navigate(destination.route) {
                        // Pop up to the start destination to avoid stacking
                        popUpTo(NavDestination.Home.route) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // Restore state when re-selecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = spring()) + slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = AppMotion.DefaultSpringOffset
                )
            },
            exitTransition = {
                fadeOut(animationSpec = spring()) + slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = AppMotion.DefaultSpringOffset
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = spring()) + slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = AppMotion.DefaultSpringOffset
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = spring()) + slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = AppMotion.DefaultSpringOffset
                )
            }
        ) {
            composable(NavDestination.Home.route) {
                HomeScreen(
                    repository = repository,
                    onNavigateToSpoof = {
                        navController.navigate(NavDestination.Spoof.route)
                    },
                    onRegenerateAll = {
                        // Will be connected to ViewModel
                        Timber.d("Regenerate all values requested")
                    }
                )
            }

            composable(NavDestination.Spoof.route) {
                SpoofSettingsScreen(
                    repository = repository,
                    onEditValue = { type, value ->
                        // Will open edit dialog
                        Timber.d("Edit $type: $value")
                    }
                )
            }

            composable(NavDestination.Settings.route) {
                SettingsScreen(
                    onDarkModeChange = { enabled ->
                        Timber.d("Dark mode changed: $enabled")
                    },
                    onDynamicColorChange = { enabled ->
                        Timber.d("Dynamic colors changed: $enabled")
                    },
                    onDebugLogChange = { enabled ->
                        Timber.d("Debug logging changed: $enabled")
                    }
                )
            }
        }
    }
}
