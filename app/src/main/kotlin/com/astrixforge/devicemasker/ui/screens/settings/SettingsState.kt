package com.astrixforge.devicemasker.ui.screens.settings

import com.astrixforge.devicemasker.ui.screens.ThemeMode

/** UI state for the Settings screen. */
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledMode: Boolean = true,
    val dynamicColors: Boolean = true,
    // Log export state
    val isExportingLogs: Boolean = false,
    val exportMode: BundleExportMode = BundleExportMode.BASIC,
    val redactionChoice: RedactionChoice = RedactionChoice.REDACTED,
    val exportResult: ExportResult? = null,
)

enum class BundleExportMode {
    BASIC,
    FULL_DEBUG,
    ROOT_MAXIMUM,
}

enum class RedactionChoice {
    REDACTED,
    UNREDACTED_REQUIRES_CONFIRMATION,
}

/** Result of a log export operation. */
sealed class ExportResult {
    data class Success(val filePath: String, val lineCount: Int) : ExportResult()

    data class Error(val message: String) : ExportResult()

    data object NoLogs : ExportResult()
}
