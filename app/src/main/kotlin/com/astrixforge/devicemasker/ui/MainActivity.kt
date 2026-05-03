package com.astrixforge.devicemasker.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.astrixforge.devicemasker.DeviceMaskerApp
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.data.SettingsDataStore
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.service.ShareableLogResult
import com.astrixforge.devicemasker.service.diagnostics.RootAccessManager
import com.astrixforge.devicemasker.service.diagnostics.RootAccessState
import com.astrixforge.devicemasker.service.diagnostics.RootLogCaptureService
import com.astrixforge.devicemasker.ui.navigation.BottomNavBar
import com.astrixforge.devicemasker.ui.navigation.NavRoutes
import com.astrixforge.devicemasker.ui.screens.ThemeMode
import com.astrixforge.devicemasker.ui.screens.diagnostics.DiagnosticsScreen
import com.astrixforge.devicemasker.ui.screens.diagnostics.DiagnosticsViewModel
import com.astrixforge.devicemasker.ui.screens.groups.GroupsScreen
import com.astrixforge.devicemasker.ui.screens.groups.GroupsViewModel
import com.astrixforge.devicemasker.ui.screens.groupspoofing.GroupSpoofingScreen
import com.astrixforge.devicemasker.ui.screens.groupspoofing.GroupSpoofingViewModel
import com.astrixforge.devicemasker.ui.screens.home.HomeScreen
import com.astrixforge.devicemasker.ui.screens.home.HomeViewModel
import com.astrixforge.devicemasker.ui.screens.settings.SettingsScreen
import com.astrixforge.devicemasker.ui.screens.settings.SettingsViewModel
import com.astrixforge.devicemasker.ui.theme.AppMotion
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import com.astrixforge.devicemasker.ui.theme.LocalMotionPolicy
import com.astrixforge.devicemasker.ui.theme.rememberMotionPolicy
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main Activity for DeviceMasker.
 *
 * Uses Jetpack Compose with Material 3 for the entire UI with edge-to-edge display. Features:
 * - Bottom navigation with animated transitions
 * - Theme settings persistence (AMOLED, Dynamic Colors)
 * - Spring-based animations for smooth navigation
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initial edge-to-edge setup
        enableEdgeToEdge()

        Timber.d("MainActivity created, module active: ${DeviceMaskerApp.isXposedModuleActive}")

        setContent {
            val settingsStore = remember { SettingsDataStore(applicationContext) }
            val repository = remember { SpoofRepository.getInstance(applicationContext) }

            // Collect theme settings from SettingsDataStore
            val themeMode by
                settingsStore.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val amoledMode by
                settingsStore.amoledMode.collectAsStateWithLifecycle(initialValue = true)
            val dynamicColors by
                settingsStore.dynamicColors.collectAsStateWithLifecycle(initialValue = true)
            val rootAccessState by RootAccessManager.state.collectAsStateWithLifecycle()
            val motionPolicy = rememberMotionPolicy()
            var showRootWarning by rememberSaveable { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                val result = RootAccessManager.requestRootAccess(applicationContext)
                if (result == RootAccessState.GRANTED) {
                    RootLogCaptureService.start(
                        applicationContext,
                        RootLogCaptureService.TRIGGER_STARTUP,
                    )
                }
            }

            LaunchedEffect(rootAccessState) {
                if (
                    rootAccessState == RootAccessState.DENIED ||
                        rootAccessState == RootAccessState.UNAVAILABLE
                ) {
                    showRootWarning = true
                }
            }

            CompositionLocalProvider(LocalMotionPolicy provides motionPolicy) {
                // Determine actual dark state for edge-to-edge styling
                val isSystemDark = isSystemInDarkTheme()
                val isDarkModeActive =
                    when (themeMode) {
                        ThemeMode.SYSTEM -> isSystemDark
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                    }

                val activity = this@MainActivity
                DisposableEffect(isDarkModeActive) {
                    activity.enableEdgeToEdge(
                        statusBarStyle =
                            if (isDarkModeActive) {
                                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                            } else {
                                SystemBarStyle.light(
                                    android.graphics.Color.TRANSPARENT,
                                    android.graphics.Color.TRANSPARENT,
                                )
                            },
                        navigationBarStyle =
                            if (isDarkModeActive) {
                                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                            } else {
                                SystemBarStyle.light(
                                    android.graphics.Color.TRANSPARENT,
                                    android.graphics.Color.TRANSPARENT,
                                )
                            },
                    )
                    onDispose {}
                }

                DeviceMaskerTheme(
                    themeMode = themeMode,
                    amoledBlack = amoledMode,
                    dynamicColor = dynamicColors,
                ) {
                    DeviceMaskerMainApp(
                        repository = repository,
                        settingsStore = settingsStore,
                        themeMode = themeMode,
                        amoledMode = amoledMode,
                        dynamicColors = dynamicColors,
                        rootAccessState = rootAccessState,
                    )

                    if (showRootWarning) {
                        RootAccessWarningDialog(
                            rootAccessState = rootAccessState,
                            onRetry = {
                                coroutineScope.launch {
                                    val result =
                                        RootAccessManager.requestRootAccess(
                                            applicationContext,
                                            force = true,
                                        )
                                    if (result == RootAccessState.GRANTED) {
                                        showRootWarning = false
                                        RootLogCaptureService.start(
                                            applicationContext,
                                            RootLogCaptureService.TRIGGER_STARTUP,
                                        )
                                    }
                                }
                            },
                            onDismiss = { showRootWarning = false },
                        )
                    }
                }
            }
        }
    }
}

/** Main app composable with navigation. */
@Composable
fun DeviceMaskerMainApp(
    repository: SpoofRepository,
    settingsStore: SettingsDataStore,
    themeMode: ThemeMode,
    amoledMode: Boolean,
    dynamicColors: Boolean,
    rootAccessState: RootAccessState,
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoutes.HOME
    val context = LocalContext.current

    // Hide bottom nav on group spoofing and diagnostics screens for a cleaner focused experience
    val showBottomBar =
        !currentRoute.startsWith(NavRoutes.GROUP_SPOOFING) && currentRoute != NavRoutes.DIAGNOSTICS

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            // Pop up to the start destination to avoid stacking
                            popUpTo(NavRoutes.HOME) { saveState = true }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when re-selecting a previously selected item
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = spring()) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = AppMotion.ReducedOffset,
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = spring()) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = AppMotion.ReducedOffset,
                    )
            },
            popEnterTransition = {
                fadeIn(animationSpec = spring()) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = AppMotion.ReducedOffset,
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = spring()) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = AppMotion.ReducedOffset,
                    )
            },
        ) {
            composable(NavRoutes.HOME) {
                val homeViewModel = viewModel { HomeViewModel(repository) }
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToSpoof = { navController.navigate(NavRoutes.GROUPS) },
                    onRegenerateAll = {},
                    onNavigateToGroup = { groupId ->
                        navController.navigate(NavRoutes.groupSpoofingRoute(groupId))
                    },
                )
            }

            composable(NavRoutes.SETTINGS) {
                val application = (context.applicationContext as android.app.Application)
                val settingsViewModel = viewModel { SettingsViewModel(application, settingsStore) }
                val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
                val shareLogsChooserTitle =
                    stringResource(R.string.settings_export_logs_share_chooser)

                SettingsScreen(
                    themeMode = themeMode,
                    amoledDarkMode = amoledMode,
                    dynamicColors = dynamicColors,
                    isExportingLogs = settingsState.isExportingLogs,
                    exportMode = settingsState.exportMode,
                    rootAccessState = rootAccessState,
                    exportResult = settingsState.exportResult,
                    onThemeModeChange = { mode ->
                        Timber.d("Theme mode changed: $mode")
                        settingsViewModel.setThemeMode(mode)
                    },
                    onAmoledDarkModeChange = { enabled ->
                        Timber.d("AMOLED mode changed: $enabled")
                        settingsViewModel.setAmoledMode(enabled)
                    },
                    onDynamicColorChange = { enabled ->
                        Timber.d("Dynamic colors changed: $enabled")
                        settingsViewModel.setDynamicColors(enabled)
                    },
                    onExportModeChange = settingsViewModel::setExportMode,
                    onExportLogsToUri = { uri, mode ->
                        Timber.d("Exporting logs to: $uri")
                        settingsViewModel.exportLogsToUri(uri, mode)
                    },
                    onShareLogs = { mode ->
                        Timber.d("Sharing logs...")
                        settingsViewModel.createShareableLogs(mode) { result ->
                            when (result) {
                                is ShareableLogResult.Success -> {
                                    val shareIntent =
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "application/zip"
                                            putExtra(Intent.EXTRA_STREAM, result.uri)
                                            putExtra(Intent.EXTRA_SUBJECT, shareLogsChooserTitle)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, shareLogsChooserTitle)
                                    )
                                }
                                is ShareableLogResult.NoLogs -> {
                                    Timber.d("No logs to share")
                                }
                                is ShareableLogResult.Error -> {
                                    Timber.e("Failed to share logs: ${result.message}")
                                }
                            }
                        }
                    },
                    onClearExportResult = { settingsViewModel.clearExportResult() },
                    onNavigateToDiagnostics = { navController.navigate(NavRoutes.DIAGNOSTICS) },
                    generateLogFileName = { settingsViewModel.generateLogFileName() },
                )
            }

            // Group Spoofing Screen - Per-group spoof values and app assignment
            composable(
                route = NavRoutes.GROUP_SPOOFING_PATTERN,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                val groupSpoofingViewModel = viewModel {
                    GroupSpoofingViewModel(repository, groupId)
                }
                GroupSpoofingScreen(
                    viewModel = groupSpoofingViewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(NavRoutes.GROUPS) {
                val groupsViewModel = viewModel { GroupsViewModel(repository) }
                GroupsScreen(
                    viewModel = groupsViewModel,
                    onGroupClick = { group ->
                        navController.navigate(NavRoutes.groupSpoofingRoute(group.id))
                    },
                )
            }

            composable(NavRoutes.DIAGNOSTICS) {
                val application = (context.applicationContext as android.app.Application)
                val diagnosticsViewModel = viewModel {
                    DiagnosticsViewModel(application, repository)
                }
                DiagnosticsScreen(
                    viewModel = diagnosticsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun RootAccessWarningDialog(
    rootAccessState: RootAccessState,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.root_access_warning_title)) },
        text = {
            Text(
                text =
                    when (rootAccessState) {
                        RootAccessState.UNAVAILABLE ->
                            stringResource(R.string.root_access_unavailable_message)
                        else -> stringResource(R.string.root_access_denied_message)
                    }
            )
        },
        confirmButton = {
            TextButton(onClick = onRetry) { Text(stringResource(R.string.root_access_retry)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_confirm)) }
        },
    )
}
