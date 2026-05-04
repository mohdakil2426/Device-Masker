package com.astrixforge.devicemasker.testing

import android.content.Context
import android.net.Uri
import com.astrixforge.devicemasker.service.ILogManager
import com.astrixforge.devicemasker.service.LogExportResult
import com.astrixforge.devicemasker.service.ShareableLogResult
import com.astrixforge.devicemasker.service.diagnostics.SupportBundleMode

/** Fake [ILogManager] for Settings testing. */
class FakeLogManager : ILogManager {

    var exportResult: LogExportResult = LogExportResult.Success("/path/to/file.zip", 42)
    var shareableResult: ShareableLogResult = ShareableLogResult.Success(Uri.EMPTY, "file.zip", 42)
    var fileName: String = "devicemasker_support_20240101_000000.zip"

    override suspend fun exportLogsToUri(
        context: Context,
        uri: Uri,
        mode: SupportBundleMode,
    ): LogExportResult = exportResult

    override suspend fun createShareableLogFile(
        context: Context,
        mode: SupportBundleMode,
    ): ShareableLogResult = shareableResult

    override fun generateLogFileName(): String = fileName
}
