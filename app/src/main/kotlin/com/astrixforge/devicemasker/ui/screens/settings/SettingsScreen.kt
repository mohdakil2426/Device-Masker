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
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
    rootAccessState: RootAccessState = RootAccessState.UNKNOWN,
    exportResult: ExportResult? = null,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onAmoledDarkModeChange: (Boolean) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    onExportLogsToUri: (Uri) -> Unit = {},
    onShareLogs: () -> Unit = {},
    onClearExportResult: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    generateLogFileName: () -> String = { "devicemasker_logs.log" },
) {
    var showThemeModeDialog by rememberSaveable { mutableStateOf(false) }
    var showExportSheet by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val exportSheetTitle = stringResource(R.string.settings_export_sheet_title)
    val saveTitle = stringResource(R.string.settings_export_save)
    val saveDesc = stringResource(R.string.settings_export_save_desc)
    val shareTitle = stringResource(R.string.settings_export_share)
    val shareDesc = stringResource(R.string.settings_export_share_desc)
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

    val exportLogsLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { uri ->
            uri?.let { onExportLogsToUri(it) }
        }

    val exportMessage =
        when (exportResult) {
            is ExportResult.Success ->
                stringResource(
                    R.string.settings_export_logs_success,
                    exportResult.lineCount,
                    exportResult.filePath,
                )
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

    if (showExportSheet) {
        ExportActionsBottomSheetContent(
            title = exportSheetTitle,
            saveLabel = saveTitle,
            saveDescription = saveDesc,
            shareLabel = shareTitle,
            shareDescription = shareDesc,
            onSave = { exportLogsLauncher.launch(generateLogFileName()) },
            onShare = onShareLogs,
            onDismiss = { showExportSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("FunctionNaming")
private fun ExportActionsBottomSheetContent(
    title: String,
    saveLabel: String,
    saveDescription: String,
    shareLabel: String,
    shareDescription: String,
    onSave: () -> Unit,
    onShare: () -> Unit,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        onSave()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(imageVector = Icons.Outlined.Save, contentDescription = saveDescription)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(saveLabel)
                }

                FilledTonalButton(
                    onClick = {
                        onShare()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(imageVector = Icons.Outlined.Share, contentDescription = shareDescription)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(shareLabel)
                }
            }
        }
    }
}

@Composable
private fun SettingsScreenContent(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode,
    amoledDarkMode: Boolean,
    dynamicColors: Boolean,
    isExportingLogs: Boolean,
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
                            stringResource(id = R.string.settings_export_logs_description)
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
