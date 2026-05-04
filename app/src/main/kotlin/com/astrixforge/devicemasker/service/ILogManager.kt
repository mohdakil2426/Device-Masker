package com.astrixforge.devicemasker.service

import android.content.Context
import android.net.Uri
import com.astrixforge.devicemasker.service.diagnostics.SupportBundleMode

interface ILogManager {
    suspend fun exportLogsToUri(
        context: Context,
        uri: Uri,
        mode: SupportBundleMode = SupportBundleMode.BASIC,
    ): LogExportResult

    suspend fun createShareableLogFile(
        context: Context,
        mode: SupportBundleMode = SupportBundleMode.BASIC,
    ): ShareableLogResult

    fun generateLogFileName(): String
}
