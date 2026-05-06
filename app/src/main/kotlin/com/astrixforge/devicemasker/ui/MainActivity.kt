package com.astrixforge.devicemasker.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.astrixforge.devicemasker.DeviceMaskerApp
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.data.SettingsDataStore
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.service.ShareableLogResult
import com.astrixforge.devicemasker.service.diagnostics.RootAccessManager
import com.astrixforge.devicemasker.service.diagnostics.RootAccessState
import com.astrixforge.devicemasker.service.diagnostics.RootLogCaptureService
import com.astrixforge.devicemasker.ui.navigation.BottomNavBar
import com.astrixforge.devicemasker.ui.navigation.DeviceMaskerDeepLinks
import com.astrixforge.devicemasker.ui.navigation.DeviceMaskerNavigator
import com.astrixforge.devicemasker.ui.navigation.NavDestination
import com.astrixforge.devicemasker.ui.navigation.bottomNavItems
import com.astrixforge.devicemasker.ui.navigation.rememberDeviceMaskerNavigationState
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
import com.astrixforge.devicemasker.ui.theme.ThemeMode
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
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {
    private var pendingDeepLinkIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDeepLinkIntent = intent

        // Initial edge-to-edge setup
        enableEdgeToEdge()

        Timber.d("MainActivity created, module active: ${DeviceMaskerApp.isXposedModuleActive}")

        setContent {
            val windowSizeClass = calculateWindowSizeClass(activity = this)
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
                        windowSizeClass = windowSizeClass,
                        deepLinkIntent = pendingDeepLinkIntent,
                        onDeepLinkIntentHandled = { pendingDeepLinkIntent = null },
                        onExitApp = { finish() },
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLinkIntent = intent
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
    windowSizeClass: WindowSizeClass,
    deepLinkIntent: Intent? = null,
    onDeepLinkIntentHandled: () -> Unit = {},
    onExitApp: () -> Unit,
) {
    val navigationState = rememberDeviceMaskerNavigationState()
    val navigator = remember(navigationState) { DeviceMaskerNavigator(navigationState) }
    val context = LocalContext.current
    val activity = context as? Activity
    val reduceMotion = AppMotion.shouldReduceMotion()
    val listDetailSceneStrategy: SceneStrategy<NavDestination>? =
        if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
            null
        } else {
            rememberListDetailSceneStrategy<NavDestination>()
        }
    val sceneStrategies = listDetailSceneStrategy?.let { listOf(it) } ?: emptyList()
    val entryDecorators =
        listOf(
            rememberSaveableStateHolderNavEntryDecorator<NavDestination>(),
            rememberViewModelStoreNavEntryDecorator<NavDestination>(),
        )

    val showNavRail =
        windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact &&
            !navigationState.isFocusScreen
    val showBottomBar =
        windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact &&
            !navigationState.isFocusScreen
    val navigationBackHandler = {
        if (navigator.goBack()) {
            activity?.finish() ?: onExitApp()
        }
    }

    LaunchedEffect(deepLinkIntent) {
        val intent = deepLinkIntent ?: return@LaunchedEffect
        if (intent.action == Intent.ACTION_VIEW) {
            DeviceMaskerDeepLinks.parse(intent.dataString)?.let(navigator::navigateDeepLink)
        }
        onDeepLinkIntentHandled()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentDestination = navigationState.topLevelDestination,
                    onNavigate = { destination -> navigator.navigateTopLevel(destination) },
                )
            }
        },
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize()) {
            if (showNavRail) {
                NavRail(
                    currentDestination = navigationState.topLevelDestination,
                    onNavigate = { destination -> navigator.navigateTopLevel(destination) },
                )
            }
            NavDisplay(
                backStack = navigationState.visibleBackStack,
                modifier =
                    if (showNavRail) {
                        Modifier.padding(innerPadding).weight(1f)
                    } else {
                        Modifier.padding(innerPadding)
                    },
                onBack = navigationBackHandler,
                entryDecorators = entryDecorators,
                sceneStrategies = sceneStrategies,
                transitionSpec = { navForwardTransform(reduceMotion) },
                popTransitionSpec = { navBackTransform(reduceMotion) },
                predictivePopTransitionSpec = { navBackTransform(reduceMotion) },
                entryProvider =
                    entryProvider {
                        entry<NavDestination.Home> {
                            val homeViewModel = viewModel { HomeViewModel(repository) }
                            HomeScreen(
                                viewModel = homeViewModel,
                                onNavigateToSpoof = {
                                    navigator.navigateTopLevel(NavDestination.Groups)
                                },
                                onRegenerateAll = {},
                                onNavigateToGroup = navigator::navigateToGroup,
                            )
                        }

                        entry<NavDestination.Settings> {
                            val application =
                                (context.applicationContext as android.app.Application)
                            val settingsViewModel = viewModel {
                                SettingsViewModel(application, settingsStore)
                            }
                            val settingsState by
                                settingsViewModel.state.collectAsStateWithLifecycle()
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
                                                        putExtra(
                                                            Intent.EXTRA_SUBJECT,
                                                            shareLogsChooserTitle,
                                                        )
                                                        addFlags(
                                                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        )
                                                    }
                                                context.startActivity(
                                                    Intent.createChooser(
                                                        shareIntent,
                                                        shareLogsChooserTitle,
                                                    )
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
                                onNavigateToDiagnostics = navigator::navigateToDiagnostics,
                                generateLogFileName = { settingsViewModel.generateLogFileName() },
                            )
                        }

                        // Group Spoofing Screen - Per-group spoof values and app assignment
                        entry<NavDestination.GroupSpoofing>(
                            metadata = ListDetailSceneStrategy.detailPane()
                        ) { destination ->
                            val groupSpoofingViewModel = viewModel {
                                GroupSpoofingViewModel(repository, destination.groupId)
                            }
                            GroupSpoofingScreen(
                                viewModel = groupSpoofingViewModel,
                                onNavigateBack = navigationBackHandler,
                            )
                        }

                        entry<NavDestination.Groups>(
                            metadata =
                                ListDetailSceneStrategy.listPane(
                                    detailPlaceholder = {
                                        Box(modifier = Modifier.fillMaxSize().wrapContentSize()) {
                                            Text(
                                                text = stringResource(R.string.group_empty_title),
                                                style = MaterialTheme.typography.titleMedium,
                                            )
                                        }
                                    }
                                )
                        ) {
                            val groupsViewModel = viewModel { GroupsViewModel(repository) }
                            GroupsScreen(
                                viewModel = groupsViewModel,
                                onGroupClick = { group -> navigator.navigateToGroup(group.id) },
                            )
                        }

                        entry<NavDestination.Diagnostics> {
                            val application =
                                (context.applicationContext as android.app.Application)
                            val diagnosticsViewModel = viewModel {
                                DiagnosticsViewModel(application, repository)
                            }
                            DiagnosticsScreen(
                                viewModel = diagnosticsViewModel,
                                onNavigateBack = navigationBackHandler,
                            )
                        }
                    },
            )
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

@Composable
private fun NavRail(
    currentDestination: NavDestination,
    onNavigate: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationRail(
        modifier = modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentDestination == item.destination
            NavigationRailItem(
                selected = isSelected,
                onClick = { onNavigate(item.destination) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = stringResource(item.labelRes),
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors =
                    NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        }
    }
}

private fun navForwardTransform(reduceMotion: Boolean): ContentTransform =
    if (reduceMotion) {
        fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
    } else {
        fadeIn(animationSpec = spring()) +
            slideInHorizontally(
                initialOffsetX = { it / 5 },
                animationSpec = AppMotion.ReducedOffset,
            ) togetherWith
            fadeOut(animationSpec = spring()) +
                slideOutHorizontally(
                    targetOffsetX = { -it / 5 },
                    animationSpec = AppMotion.ReducedOffset,
                )
    }

private fun navBackTransform(reduceMotion: Boolean): ContentTransform =
    if (reduceMotion) {
        fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
    } else {
        fadeIn(animationSpec = spring()) +
            slideInHorizontally(
                initialOffsetX = { -it / 5 },
                animationSpec = AppMotion.ReducedOffset,
            ) togetherWith
            fadeOut(animationSpec = spring()) +
                slideOutHorizontally(
                    targetOffsetX = { it / 5 },
                    animationSpec = AppMotion.ReducedOffset,
                )
    }
