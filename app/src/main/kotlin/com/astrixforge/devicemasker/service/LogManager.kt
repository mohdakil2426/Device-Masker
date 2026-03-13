package com.astrixforge.devicemasker.service

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.astrixforge.devicemasker.BuildConfig
import com.astrixforge.devicemasker.DeviceMaskerApp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Log Manager - Comprehensive log export with industry-standard formatting.
 *
 * Exports logs from the diagnostics service (system_server) via SAF file picker.
 */
object LogManager {

    private const val LOG_FILE_PREFIX = "devicemasker_logs_"
    private const val LOG_FILE_EXTENSION = ".log"

    // Industry-standard log separators
    private const val HEADER_LINE =
        "════════════════════════════════════════════════════════════════════════════════"
    private const val SECTION_LINE =
        "────────────────────────────────────────────────────────────────────────────────"
    private const val SUBSECTION_LINE =
        "┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄"

    // ═══════════════════════════════════════════════════════════════════
    // EXPORT LOGS (Service Data)
    // ═══════════════════════════════════════════════════════════════════

    /** Exports service logs to a custom URI location. */
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

    /**
     * Creates a shareable log file in the cache directory.
     *
     * @param context Application context
     * @return Pair of URI for FileProvider and the file path, or null if failed
     */
    suspend fun createShareableLogFile(context: Context): ShareableLogResult {
        return try {
            val logContent = buildLogContent(context)
            if (logContent.lines().size <= 10) {
                // Minimal content means no real logs
                return ShareableLogResult.NoLogs
            }

            // Create logs directory in cache
            val logsDir = File(context.cacheDir, "logs")
            if (!logsDir.exists()) {
                logsDir.mkdirs()
            }

            // Create the log file
            val fileName = generateLogFileName()
            val logFile = File(logsDir, fileName)
            logFile.writeText(logContent)

            // Get URI via FileProvider
            val uri =
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", logFile)

            ShareableLogResult.Success(uri, fileName, logContent.lines().size)
        } catch (e: Exception) {
            ShareableLogResult.Error("Failed to create shareable log: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // LOG CONTENT BUILDER
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun buildLogContent(context: Context): String {
        val builder = StringBuilder()
        val exportTime = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US)
        dateFormat.timeZone = TimeZone.getDefault()

        // Header
        builder.appendLine(HEADER_LINE)
        builder.appendLine("              DEVICE MASKER - LOG EXPORT")
        builder.appendLine("                    libxposed API 100")
        builder.appendLine(HEADER_LINE)
        builder.appendLine()

        // Metadata
        appendMetadata(builder, context, "DIAGNOSTICS LOGS", dateFormat, exportTime)

        // Log Content
        builder.appendLine()
        builder.appendLine("[LOG ENTRIES]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine()
        builder.appendLine("Source: DeviceMaskerService (system_server)")
        builder.appendLine("Scope: All hooked processes + System Server")
        builder.appendLine(SUBSECTION_LINE)
        builder.appendLine()

        try {
            val serviceClient = DeviceMaskerApp.serviceClient
            if (!serviceClient.isConnected) {
                serviceClient.connect()
            }

            val logs =
                if (serviceClient.isConnected) {
                    serviceClient.getLogs(500)
                } else {
                    emptyList()
                }

            if (logs.isNotEmpty()) {
                builder.appendLine("Total Entries: ${logs.size}")
                builder.appendLine()
                logs.forEach { builder.appendLine(it) }
            } else {
                builder.appendLine("(No logs available)")
                builder.appendLine()
                builder.appendLine("Possible reasons:")
                builder.appendLine("  • No hooks have fired since last reboot")
                builder.appendLine("  • Service is not connected")
                builder.appendLine("  • Log buffer was cleared")
            }
        } catch (e: Exception) {
            builder.appendLine("(Failed to read logs: ${e.message})")
        }

        // Footer
        appendFooter(builder, dateFormat)

        return builder.toString()
    }

    // ═══════════════════════════════════════════════════════════════════
    // SHARED CONTENT BUILDERS
    // ═══════════════════════════════════════════════════════════════════

    private fun appendMetadata(
        builder: StringBuilder,
        context: Context,
        logType: String,
        dateFormat: SimpleDateFormat,
        exportTime: Date,
    ) {
        builder.appendLine("[EXPORT METADATA]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine("Export Type      : $logType")
        builder.appendLine("Export Time      : ${dateFormat.format(exportTime)}")
        builder.appendLine("Timezone         : ${TimeZone.getDefault().id}")
        builder.appendLine("Log Format       : Device Masker Debug Log v1.1")
        builder.appendLine()

        builder.appendLine("[APPLICATION INFO]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine("App Name         : Device Masker")
        builder.appendLine("Package          : ${context.packageName}")
        builder.appendLine(
            "Version          : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
        builder.appendLine("Build Type       : ${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE"}")
        builder.appendLine()

        builder.appendLine("[DEVICE INFO]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine("Manufacturer     : ${Build.MANUFACTURER}")
        builder.appendLine("Model            : ${Build.MODEL}")
        builder.appendLine("Device           : ${Build.DEVICE}")
        builder.appendLine(
            "Android          : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        )
        builder.appendLine("Security Patch   : ${Build.VERSION.SECURITY_PATCH}")
        builder.appendLine("Build            : ${Build.DISPLAY}")
    }

    private fun appendFooter(builder: StringBuilder, dateFormat: SimpleDateFormat) {
        builder.appendLine()
        builder.appendLine(HEADER_LINE)
        builder.appendLine("                         END OF LOG EXPORT")
        builder.appendLine(HEADER_LINE)
        builder.appendLine()
        builder.appendLine("For support, include this log file in your bug report.")
        builder.appendLine("GitHub: https://github.com/AstrixForge/DeviceMasker")
        builder.appendLine()
        builder.appendLine("Export completed: ${dateFormat.format(Date())}")
    }

    // ═══════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════

    suspend fun getLogCount(): Int =
        try {
            val serviceClient = DeviceMaskerApp.serviceClient
            if (serviceClient.isConnected) serviceClient.getLogs(1).size else 0
        } catch (_: Exception) {
            0
        }

    fun generateLogFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "$LOG_FILE_PREFIX$timestamp$LOG_FILE_EXTENSION"
    }
}

/** Result of log export operation. */
sealed class LogExportResult {
    data class Success(val filePath: String, val lineCount: Int) : LogExportResult()

    data class Error(val message: String) : LogExportResult()
}

/** Result of shareable log creation. */
sealed class ShareableLogResult {
    data class Success(val uri: Uri, val fileName: String, val lineCount: Int) :
        ShareableLogResult()

    data class Error(val message: String) : ShareableLogResult()

    data object NoLogs : ShareableLogResult()
}
