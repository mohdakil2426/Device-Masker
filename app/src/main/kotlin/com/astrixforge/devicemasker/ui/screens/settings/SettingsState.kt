package com.astrixforge.devicemasker.ui.screens.settings

import com.astrixforge.devicemasker.ui.screens.ThemeMode

/**
 * UI state for the Settings screen.
 */
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledMode: Boolean = true,
    val dynamicColors: Boolean = true,
    val showThemeModeDialog: Boolean = false,
    // Log export states (separate operations)
    val isExportingLogs: Boolean = false,
    val isCapturingLogcat: Boolean = false,
    val exportResult: ExportResult? = null,
    val hasRootAccess: Boolean = false
)

/**
 * Result of a log export operation.
 */
sealed class ExportResult {
    data class Success(val filePath: String, val lineCount: Int, val isLogcat: Boolean = false) : ExportResult()
    data class Error(val message: String) : ExportResult()
    data object NoLogs : ExportResult()
}
