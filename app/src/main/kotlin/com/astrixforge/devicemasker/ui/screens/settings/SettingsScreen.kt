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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.BuildConfig
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.ui.components.ScreenHeader
import com.astrixforge.devicemasker.ui.components.SettingsClickableItem
import com.astrixforge.devicemasker.ui.components.SettingsClickableItemWithValue
import com.astrixforge.devicemasker.ui.components.SettingsInfoItem
import com.astrixforge.devicemasker.ui.components.SettingsSection
import com.astrixforge.devicemasker.ui.components.SettingsSwitchItem
import com.astrixforge.devicemasker.ui.screens.ThemeMode
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme

/**
 * Settings screen for app preferences.
 *
 * Provides options for:
 * - Theme settings (theme mode, AMOLED dark mode, dynamic colors)
 * - Debug options (log export, logcat capture with file picker)
 * - About information
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    amoledDarkMode: Boolean = true,
    dynamicColors: Boolean = true,
    isExportingLogs: Boolean = false,
    isCapturingLogcat: Boolean = false,
    hasRootAccess: Boolean = false,
    exportResult: ExportResult? = null,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onAmoledDarkModeChange: (Boolean) -> Unit = {},
    onDynamicColorChange: (Boolean) -> Unit = {},
    onExportLogs: () -> Unit = {},
    onExportLogsToUri: (Uri) -> Unit = {},
    onCaptureLogcat: () -> Unit = {},
    onCaptureLogcatToUri: (Uri) -> Unit = {},
    onClearExportResult: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    generateLogFileName: () -> String = { "devicemasker_logs.log" },
    generateLogcatFileName: () -> String = { "devicemasker_logcat.log" },
) {
    var showThemeModeDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showLogcatDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val exportNoLogsMessage = stringResource(R.string.settings_export_logs_no_logs)

    val isSystemDark = isSystemInDarkTheme()
    val isDarkModeActive = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    // File picker launchers
    val exportLogsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { onExportLogsToUri(it) }
    }

    val captureLogcatLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let { onCaptureLogcatToUri(it) }
    }

    // Handle export result
    LaunchedEffect(exportResult) {
        when (exportResult) {
            is ExportResult.Success -> {
                val typeLabel = if (exportResult.isLogcat) "Logcat captured" else "Logs exported"
                val message = "$typeLabel (${exportResult.lineCount} lines)\n\nSaved to: ${exportResult.filePath}"
                snackbarHostState.showSnackbar(message)
                onClearExportResult()
            }
            is ExportResult.NoLogs -> {
                snackbarHostState.showSnackbar(exportNoLogsMessage)
                onClearExportResult()
            }
            is ExportResult.Error -> {
                snackbarHostState.showSnackbar("Failed: ${exportResult.message}")
                onClearExportResult()
            }
            null -> { }
        }
    }

    androidx.compose.foundation.layout.Box(modifier = modifier) {
        SettingsScreenContent(
            modifier = Modifier,
            themeMode = themeMode,
            amoledDarkMode = amoledDarkMode,
            dynamicColors = dynamicColors,
            isExportingLogs = isExportingLogs,
            isCapturingLogcat = isCapturingLogcat,
            hasRootAccess = hasRootAccess,
            isDarkModeActive = isDarkModeActive,
            onThemeModeClick = { showThemeModeDialog = true },
            onAmoledDarkModeChange = onAmoledDarkModeChange,
            onDynamicColorChange = onDynamicColorChange,
            onExportLogsClick = { showExportDialog = true },
            onCaptureLogcatClick = { showLogcatDialog = true },
            onNavigateToDiagnostics = onNavigateToDiagnostics,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    // Theme Mode Selection Dialog
    if (showThemeModeDialog) {
        ThemeModeDialog(
            currentMode = themeMode,
            onModeSelected = { mode ->
                onThemeModeChange(mode)
                showThemeModeDialog = false
            },
            onDismiss = { showThemeModeDialog = false }
        )
    }

    // Export Logs Dialog (save location choice)
    if (showExportDialog) {
        SaveLocationDialog(
            title = stringResource(R.string.settings_export_logs),
            onDownloads = {
                showExportDialog = false
                onExportLogs()
            },
            onCustomLocation = {
                showExportDialog = false
                exportLogsLauncher.launch(generateLogFileName())
            },
            onDismiss = { showExportDialog = false }
        )
    }

    // Capture Logcat Dialog (save location choice)
    if (showLogcatDialog) {
        SaveLocationDialog(
            title = stringResource(R.string.settings_capture_logcat),
            onDownloads = {
                showLogcatDialog = false
                onCaptureLogcat()
            },
            onCustomLocation = {
                showLogcatDialog = false
                captureLogcatLauncher.launch(generateLogcatFileName())
            },
            onDismiss = { showLogcatDialog = false }
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
    isCapturingLogcat: Boolean,
    hasRootAccess: Boolean,
    isDarkModeActive: Boolean,
    onThemeModeClick: () -> Unit,
    onAmoledDarkModeChange: (Boolean) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onExportLogsClick: () -> Unit,
    onCaptureLogcatClick: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            ScreenHeader(title = stringResource(id = R.string.settings_title))
        }

        // Appearance Section
        item(key = "appearance") {
            SettingsSection(title = stringResource(id = R.string.settings_appearance)) {
                SettingsClickableItemWithValue(
                    icon = when (themeMode) {
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
                SettingsClickableItem(
                    icon = Icons.Outlined.FileDownload,
                    title = stringResource(id = R.string.settings_export_logs),
                    description = if (isExportingLogs) {
                        stringResource(id = R.string.settings_exporting)
                    } else {
                        stringResource(id = R.string.settings_export_logs_description)
                    },
                    onClick = { if (!isExportingLogs) onExportLogsClick() },
                    trailingContent = if (isExportingLogs) {
                        { LoadingIndicator() }
                    } else null
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Capture Logcat
                SettingsClickableItem(
                    icon = Icons.Outlined.Terminal,
                    title = stringResource(id = R.string.settings_capture_logcat),
                    description = if (isCapturingLogcat) {
                        stringResource(id = R.string.settings_capturing)
                    } else if (hasRootAccess) {
                        stringResource(id = R.string.settings_capture_logcat_description)
                    } else {
                        stringResource(id = R.string.settings_capture_logcat_no_root)
                    },
                    onClick = { if (!isCapturingLogcat) onCaptureLogcatClick() },
                    trailingContent = if (isCapturingLogcat) {
                        { LoadingIndicator() }
                    } else null
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
                    value = stringResource(
                        id = R.string.settings_version_info,
                        BuildConfig.VERSION_NAME
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsInfoItem(
                    icon = Icons.Outlined.Code,
                    title = "Build Type",
                    value = if (BuildConfig.DEBUG) "Debug" else "Release (Beta)",
                )

                Spacer(modifier = Modifier.height(8.dp))

                SettingsClickableItem(
                    icon = Icons.Outlined.Tune,
                    title = "Module Info",
                    description = "YukiHookAPI 1.3.1 • LSPosed Module",
                    onClick = { },
                )
            }
        }

        item(key = "bottom_spacing") { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun LoadingIndicator() {
    CircularProgressIndicator(
        modifier = Modifier
            .padding(end = 8.dp)
            .width(24.dp)
            .height(24.dp),
        strokeWidth = 2.dp
    )
}

/** Dialog for choosing save location: Downloads or Custom */
@Composable
private fun SaveLocationDialog(
    title: String,
    onDownloads: () -> Unit,
    onCustomLocation: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.settings_save_location_prompt),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onCustomLocation) {
                Text(stringResource(R.string.settings_save_custom))
            }
        },
        dismissButton = {
            TextButton(onClick = onDownloads) {
                Text(stringResource(R.string.settings_save_downloads))
            }
        },
    )
}

@Composable
private fun ThemeModeDialog(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.settings_theme_mode),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = mode == currentMode,
                                onClick = { onModeSelected(mode) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = mode == currentMode,
                            onClick = null,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(id = mode.displayNameRes),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            if (mode == ThemeMode.SYSTEM) {
                                Text(
                                    text = stringResource(id = R.string.settings_theme_follow_system),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_cancel))
            }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SettingsScreenPreview() {
    DeviceMaskerTheme { SettingsScreen() }
}
