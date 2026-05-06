package com.astrixforge.devicemasker.ui.screens.settings

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.BuildConfig
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.service.diagnostics.RootAccessState
import com.astrixforge.devicemasker.ui.components.ScreenHeader
import com.astrixforge.devicemasker.ui.components.SettingsClickableItem
import com.astrixforge.devicemasker.ui.components.SettingsClickableItemWithValue
import com.astrixforge.devicemasker.ui.components.SettingsInfoItem
import com.astrixforge.devicemasker.ui.components.SettingsSection
import com.astrixforge.devicemasker.ui.components.SettingsSwitchItem
import com.astrixforge.devicemasker.ui.components.dialog.ThemeModeDialog
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import com.astrixforge.devicemasker.ui.theme.ThemeMode

/**
 * Settings screen for app preferences.
 *
 * Provides options for:
 * - Theme settings (theme mode, AMOLED dark mode, dynamic colors)
 * - Debug options (log export with save/share options)
 * - About information
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    amoledDarkMode: Boolean = true,
    dynamicColors: Boolean = true,
    isExportingLogs: Boolean = false,
    exportMode: BundleExportMode = BundleExportMode.BASIC,
    rootAccessState: RootAccessState = RootAccessState.UNKNOWN,
    exportResult: ExportResult? = null,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onAmoledDarkModeChange: (Boolean) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    onExportModeChange: (BundleExportMode) -> Unit = {},
    onExportLogsToUri: (Uri, BundleExportMode) -> Unit = { _, _ -> },
    onShareLogs: (BundleExportMode) -> Unit = {},
    onClearExportResult: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    generateLogFileName: () -> String = { "devicemasker_logs.log" },
) {
    var showThemeModeDialog by rememberSaveable { mutableStateOf(false) }
    var showExportSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val exportNoLogsMessage = stringResource(R.string.settings_export_logs_no_logs)
    val exportSheetTitle = stringResource(R.string.settings_export_sheet_title)
    val saveTitle = stringResource(R.string.settings_export_save)
    val saveDesc = stringResource(R.string.settings_export_save_desc)
    val shareTitle = stringResource(R.string.settings_export_share)
    val shareDesc = stringResource(R.string.settings_export_share_desc)
    val basicExportTitle = stringResource(R.string.settings_export_basic)
    val fullExportTitle = stringResource(R.string.settings_export_full_debug)
    val rootExportTitle = stringResource(R.string.settings_export_root_maximum)
    val redactedDefault = stringResource(R.string.settings_export_redacted_default)
    val rootMaximumAvailable = rootAccessState == RootAccessState.GRANTED
    val buildTypeValue =
        stringResource(
            if (BuildConfig.DEBUG) {
                R.string.settings_build_type_debug
            } else {
                R.string.settings_build_type_release
            }
        )
    val moduleInfoValue = stringResource(R.string.settings_module_info_value)

    val isSystemDark = isSystemInDarkTheme()
    val isDarkModeActive =
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemDark
            ThemeMode.DARK -> true
            ThemeMode.LIGHT -> false
        }

    // File picker launcher - opens native folder picker
    var pendingExportMode by remember { mutableStateOf(exportMode) }
    LaunchedEffect(exportMode) { pendingExportMode = exportMode }
    val exportLogsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { uri ->
            uri?.let { onExportLogsToUri(it, pendingExportMode) }
        }

    val exportMessage =
        when (exportResult) {
            is ExportResult.Success ->
                stringResource(
                    R.string.settings_export_logs_success,
                    exportResult.lineCount,
                    exportResult.filePath,
                )
            is ExportResult.NoLogs -> exportNoLogsMessage
            is ExportResult.Error ->
                stringResource(R.string.settings_export_logs_error, exportResult.message)
            null -> null
        }

    // Handle export result
    LaunchedEffect(exportMessage) {
        exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            onClearExportResult()
        }
    }

    androidx.compose.foundation.layout.Box(modifier = modifier) {
        SettingsScreenContent(
            modifier = Modifier,
            themeMode = themeMode,
            amoledDarkMode = amoledDarkMode,
            dynamicColors = dynamicColors,
            isExportingLogs = isExportingLogs,
            exportMode = exportMode,
            rootAccessState = rootAccessState,
            isDarkModeActive = isDarkModeActive,
            buildTypeValue = buildTypeValue,
            moduleInfoValue = moduleInfoValue,
            onThemeModeClick = { showThemeModeDialog = true },
            onAmoledDarkModeChange = onAmoledDarkModeChange,
            onDynamicColorChange = onDynamicColorChange,
            onExportLogsClick = {
                // Show bottom sheet with save/share options
                if (!isExportingLogs) {
                    showExportSheet = true
                }
            },
            onNavigateToDiagnostics = onNavigateToDiagnostics,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // Theme Mode Selection Dialog
    if (showThemeModeDialog) {
        ThemeModeDialog(
            currentMode = themeMode,
            onModeSelect = { mode ->
                onThemeModeChange(mode)
                showThemeModeDialog = false
            },
            onDismiss = { showThemeModeDialog = false },
        )
    }

    // Export Options Bottom Sheet
    if (showExportSheet) {
        ExportActionsBottomSheet(
            title = exportSheetTitle,
            saveLabel = saveTitle,
            shareLabel = shareTitle,
            modes =
                listOf(
                    ExportModeAction(
                        title = basicExportTitle,
                        description = redactedDefault,
                        enabled = true,
                        onSave = {
                            pendingExportMode = BundleExportMode.BASIC
                            onExportModeChange(BundleExportMode.BASIC)
                            exportLogsLauncher.launch(generateLogFileName())
                        },
                        onShare = {
                            onExportModeChange(BundleExportMode.BASIC)
                            onShareLogs(BundleExportMode.BASIC)
                        },
                    ),
                    ExportModeAction(
                        title = fullExportTitle,
                        description = saveDesc,
                        enabled = true,
                        onSave = {
                            pendingExportMode = BundleExportMode.FULL_DEBUG
                            onExportModeChange(BundleExportMode.FULL_DEBUG)
                            exportLogsLauncher.launch(generateLogFileName())
                        },
                        onShare = {
                            onExportModeChange(BundleExportMode.FULL_DEBUG)
                            onShareLogs(BundleExportMode.FULL_DEBUG)
                        },
                    ),
                    ExportModeAction(
                        title = rootExportTitle,
                        description =
                            if (rootMaximumAvailable) {
                                stringResource(R.string.settings_export_unredacted_warning)
                            } else {
                                stringResource(R.string.root_access_required_for_root_export)
                            },
                        enabled = rootMaximumAvailable,
                        onSave = {
                            pendingExportMode = BundleExportMode.ROOT_MAXIMUM
                            onExportModeChange(BundleExportMode.ROOT_MAXIMUM)
                            exportLogsLauncher.launch(generateLogFileName())
                        },
                        onShare = {
                            onExportModeChange(BundleExportMode.ROOT_MAXIMUM)
                            onShareLogs(BundleExportMode.ROOT_MAXIMUM)
                        },
                    ),
                ),
            onDismiss = { showExportSheet = false },
        )
    }
}

private data class ExportModeAction(
    val title: String,
    val description: String,
    val enabled: Boolean,
    val onSave: () -> Unit,
    val onShare: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExportActionsBottomSheet(
    title: String,
    saveLabel: String,
    shareLabel: String,
    modes: List<ExportModeAction>,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            modes.forEach { mode ->
                ExportModeSplitButton(
                    mode = mode,
                    saveLabel = saveLabel,
                    shareLabel = shareLabel,
                    onDismiss = onDismiss,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExportModeSplitButton(
    mode: ExportModeAction,
    saveLabel: String,
    shareLabel: String,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = mode.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color =
                if (mode.enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        Text(
            text = mode.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        )

        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.TonalLeadingButton(
                    enabled = mode.enabled,
                    onClick = {
                        mode.onSave()
                        onDismiss()
                    },
                ) {
                    Icon(imageVector = Icons.Outlined.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(saveLabel)
                }
            },
            trailingButton = {
                SplitButtonDefaults.TonalTrailingButton(
                    checked = false,
                    enabled = mode.enabled,
                    onCheckedChange = {
                        mode.onShare()
                        onDismiss()
                    },
                ) {
                    Icon(imageVector = Icons.Outlined.Share, contentDescription = shareLabel)
                }
            },
        )
    }
}

@Composable
private fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode,
    amoledDarkMode: Boolean,
    dynamicColors: Boolean,
    isExportingLogs: Boolean,
    exportMode: BundleExportMode,
    rootAccessState: RootAccessState,
    isDarkModeActive: Boolean,
    buildTypeValue: String,
    moduleInfoValue: String,
    onThemeModeClick: () -> Unit,
    onAmoledDarkModeChange: (Boolean) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onExportLogsClick: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") { ScreenHeader(title = stringResource(id = R.string.settings_title)) }

        // Appearance Section
        item(key = "appearance") {
            SettingsSection(title = stringResource(id = R.string.settings_appearance)) {
                SettingsClickableItemWithValue(
                    icon =
                        when (themeMode) {
                            ThemeMode.DARK -> Icons.Outlined.DarkMode
                            ThemeMode.LIGHT -> Icons.Outlined.LightMode
                            ThemeMode.SYSTEM -> Icons.Outlined.Contrast
                        },
                    title = stringResource(id = R.string.settings_theme_mode),
                    description = stringResource(id = themeMode.displayNameRes),
                    onClick = onThemeModeClick,
                )

                AnimatedVisibility(
                    visible = isDarkModeActive,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Contrast,
                            title = stringResource(id = R.string.settings_amoled_mode),
                            description = stringResource(id = R.string.settings_amoled_description),
                            checked = amoledDarkMode,
                            onCheckedChange = onAmoledDarkModeChange,
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSwitchItem(
                        icon = Icons.Outlined.Palette,
                        title = stringResource(id = R.string.settings_dynamic_colors),
                        description = stringResource(id = R.string.settings_dynamic_description),
                        checked = dynamicColors,
                        onCheckedChange = onDynamicColorChange,
                    )
                }
            }
        }

        // Debug Section
        item(key = "debug") {
            SettingsSection(title = stringResource(id = R.string.settings_debug)) {

                // Export In-Memory Logs
                SettingsInfoItem(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(id = R.string.root_access_status_title),
                    value = stringResource(id = rootAccessState.labelRes()),
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsClickableItem(
                    icon = Icons.Outlined.FileDownload,
                    title = stringResource(id = R.string.settings_export_logs),
                    description =
                        if (isExportingLogs) {
                            stringResource(id = R.string.settings_exporting)
                        } else {
                            "${stringResource(id = R.string.settings_export_logs_description)} • ${exportMode.name}"
                        },
                    onClick = { if (!isExportingLogs) onExportLogsClick() },
                    trailingContent =
                        if (isExportingLogs) {
                            { LoadingIndicator() }
                        } else null,
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsClickableItem(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(id = R.string.settings_diagnostics),
                    description = stringResource(id = R.string.settings_diagnostics_description),
                    onClick = onNavigateToDiagnostics,
                )
            }
        }

        // About Section
        item(key = "about") {
            SettingsSection(title = stringResource(id = R.string.settings_about)) {
                SettingsInfoItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(id = R.string.settings_version),
                    value =
                        stringResource(
                            id = R.string.settings_version_info,
                            BuildConfig.VERSION_NAME,
                        ),
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsInfoItem(
                    icon = Icons.Outlined.Code,
                    title = stringResource(id = R.string.settings_build_type),
                    value = buildTypeValue,
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsInfoItem(
                    icon = Icons.Outlined.Tune,
                    title = stringResource(id = R.string.settings_module_info),
                    value = moduleInfoValue,
                )
            }
        }

        item(key = "bottom_spacing") { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

private fun RootAccessState.labelRes(): Int =
    when (this) {
        RootAccessState.UNKNOWN -> R.string.root_access_status_unknown
        RootAccessState.REQUESTING -> R.string.root_access_status_requesting
        RootAccessState.GRANTED -> R.string.root_access_status_granted
        RootAccessState.DENIED -> R.string.root_access_status_denied
        RootAccessState.UNAVAILABLE -> R.string.root_access_status_unavailable
    }

@Composable
private fun LoadingIndicator() {
    ContainedLoadingIndicator(modifier = Modifier.padding(end = 8.dp).width(24.dp).height(24.dp))
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SettingsScreenPreview() {
    DeviceMaskerTheme { SettingsScreen() }
}
