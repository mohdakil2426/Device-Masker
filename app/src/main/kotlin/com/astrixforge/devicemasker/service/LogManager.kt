package com.astrixforge.devicemasker.service

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.astrixforge.devicemasker.DeviceMaskerApp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Log manager for rootless structured export.
 *
 * The app cannot read global logcat without privileged access. Exports combine app-owned persistent
 * logs with the current diagnostics service buffer when that service is reachable.
 */
object LogManager {

    private const val LOG_FILE_PREFIX = "devicemasker_logs_"
    private const val LOG_FILE_EXTENSION = ".log"

    suspend fun exportLogsToUri(context: Context, uri: Uri): LogExportResult {
        return try {
            val logContent = buildLogContent(context)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(logContent.toByteArray())
            } ?: return LogExportResult.Error("Failed to open file for writing")

            LogExportResult.Success(uri.lastPathSegment ?: "logs.log", logContent.lines().size)
        } catch (e: Exception) {
            LogExportResult.Error("Export failed: ${e.message}")
        }
    }

    suspend fun createShareableLogFile(context: Context): ShareableLogResult {
        return try {
            if (!hasAnyLogs(context)) {
                return ShareableLogResult.NoLogs
            }

            val logsDir = File(context.cacheDir, "logs")
            if (!logsDir.exists()) {
                logsDir.mkdirs()
            }

            val fileName = generateLogFileName()
            val logFile = File(logsDir, fileName)
            logFile.writeText(buildLogContent(context))

            val uri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", logFile)

            ShareableLogResult.Success(uri, fileName, logFile.readLines().size)
        } catch (e: Exception) {
            ShareableLogResult.Error("Failed to create shareable log: ${e.message}")
        }
    }

    private suspend fun buildLogContent(context: Context): String {
        val appEntries = AppLogStore.from(context).readEntries()
        val serviceClient = DeviceMaskerApp.serviceClient
        if (!serviceClient.isConnected) {
            serviceClient.connect()
        }

        val serviceLogs =
            if (serviceClient.isConnected) {
                serviceClient.getLogs(500)
            } else {
                emptyList()
            }
        val diagnosticsStatus = serviceClient.connectionState.value.name.lowercase(Locale.US)

        return LogFileFormatter.build(
            appEntries = appEntries,
            serviceLogs = serviceLogs,
            diagnosticsStatus = diagnosticsStatus,
        )
    }

    suspend fun getLogCount(): Int =
        try {
            val serviceClient = DeviceMaskerApp.serviceClient
            if (!serviceClient.isConnected) {
                serviceClient.connect()
            }
            AppLogStore.from(DeviceMaskerApp.getInstance()).readEntries().size +
                if (serviceClient.isConnected) serviceClient.getLogs(500).size else 0
        } catch (_: Exception) {
            0
        }

    private suspend fun hasAnyLogs(context: Context): Boolean {
        val serviceClient = DeviceMaskerApp.serviceClient
        if (!serviceClient.isConnected) {
            serviceClient.connect()
        }
        return AppLogStore.from(context).readEntries().isNotEmpty() ||
            (serviceClient.isConnected && serviceClient.getLogs(1).isNotEmpty())
    }

    fun generateLogFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "$LOG_FILE_PREFIX$timestamp$LOG_FILE_EXTENSION"
    }
}

sealed class LogExportResult {
    data class Success(val filePath: String, val lineCount: Int) : LogExportResult()

    data class Error(val message: String) : LogExportResult()
}

sealed class ShareableLogResult {
    data class Success(val uri: Uri, val fileName: String, val lineCount: Int) :
        ShareableLogResult()

    data class Error(val message: String) : ShareableLogResult()

    data object NoLogs : ShareableLogResult()
}
