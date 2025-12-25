package com.astrixforge.devicemasker.service

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.highcapable.yukihookapi.hook.log.YLog
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Log Manager - Handles log export to Downloads folder.
 *
 * This utility uses:
 * - MediaStore API for Android 10+ (Scoped Storage compliant)
 * - Direct file access for Android 9 and below
 *
 * Logs are collected from:
 * - YLog.inMemoryData (if enabled via isRecord = true)
 * - YLog.contents (formatted log string)
 *
 * Usage:
 * ```kotlin
 * val result = LogManager.exportLogs(context)
 * when (result) {
 *     is LogExportResult.Success -> showToast("Saved to: ${result.filePath}")
 *     is LogExportResult.Error -> showError(result.message)
 * }
 * ```
 */
object LogManager {

    private const val TAG = "LogManager"
    private const val LOG_FILE_PREFIX = "devicemasker_logs_"
    private const val LOG_FILE_EXTENSION = ".txt"
    private const val MIME_TYPE = "text/plain"

    /**
     * Exports logs to Downloads folder.
     *
     * @param context Application context
     * @return LogExportResult indicating success or failure
     */
    fun exportLogs(context: Context): LogExportResult {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "$LOG_FILE_PREFIX$timestamp$LOG_FILE_EXTENSION"

            // Get log content
            val logContent = buildLogContent()

            if (logContent.isEmpty()) {
                return LogExportResult.Error("No logs available to export")
            }

            // Save to Downloads folder
            val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, fileName, logContent)
            } else {
                saveToDownloadsLegacy(fileName, logContent)
            }

            LogExportResult.Success(filePath, logContent.lines().size)
        } catch (e: Exception) {
            LogExportResult.Error("Export failed: ${e.message}")
        }
    }

    /**
     * Builds the log content from YLog and DualLog sources.
     */
    private fun buildLogContent(): String {
        val builder = StringBuilder()

        // Add header
        builder.appendLine("═══════════════════════════════════════════════════════════")
        builder.appendLine("           Device Masker - Log Export")
        builder.appendLine("═══════════════════════════════════════════════════════════")
        builder.appendLine("Exported: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        builder.appendLine("Android SDK: ${Build.VERSION.SDK_INT}")
        builder.appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        builder.appendLine("═══════════════════════════════════════════════════════════")
        builder.appendLine()

        // Add YLog in-memory data if available
        try {
            val ylogData = YLog.inMemoryData
            if (ylogData.isNotEmpty()) {
                builder.appendLine("=== YukiHookAPI Logs (${ylogData.size} entries) ===")
                ylogData.forEach { entry ->
                    val priorityStr = entry.priority.toString()
                    val priority = when {
                        priorityStr.contains("2") || priorityStr.contains("VERBOSE", true) -> "V"
                        priorityStr.contains("3") || priorityStr.contains("DEBUG", true) -> "D"
                        priorityStr.contains("4") || priorityStr.contains("INFO", true) -> "I"
                        priorityStr.contains("5") || priorityStr.contains("WARN", true) -> "W"
                        priorityStr.contains("6") || priorityStr.contains("ERROR", true) -> "E"
                        else -> "?"
                    }
                    builder.appendLine("[${entry.time}][$priority] ${entry.msg}")
                    entry.throwable?.let { t ->
                        builder.appendLine("  Exception: ${t.message}")
                        t.stackTrace.take(5).forEach { se ->
                            builder.appendLine("    at $se")
                        }
                    }
                }
                builder.appendLine()
            } else {
                builder.appendLine("=== No YLog in-memory data available ===")
                builder.appendLine("(Logs may be captured via Logcat: adb logcat -s DeviceMasker)")
                builder.appendLine()
            }
        } catch (e: Exception) {
            builder.appendLine("=== YLog data unavailable: ${e.message} ===")
            builder.appendLine()
        }

        // Add YLog contents if available
        try {
            val contents = YLog.contents
            if (contents.isNotEmpty()) {
                builder.appendLine("=== YLog Contents ===")
                builder.appendLine(contents)
                builder.appendLine()
            }
        } catch (e: Exception) {
            // YLog.contents may not be available
        }

        // Add footer with logcat command
        builder.appendLine("═══════════════════════════════════════════════════════════")
        builder.appendLine("For real-time logs, use:")
        builder.appendLine("  adb logcat -s DeviceMasker DeviceHooker NetworkHooker AntiDetectHooker")
        builder.appendLine("═══════════════════════════════════════════════════════════")

        return builder.toString()
    }

    /**
     * Saves log file using MediaStore API (Android 10+).
     */
    private fun saveWithMediaStore(context: Context, fileName: String, content: String): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("Failed to create file in Downloads")

        resolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
        } ?: throw Exception("Failed to write to file")

        // Mark file as complete
        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

        return "Downloads/$fileName"
    }

    /**
     * Saves log file directly to Downloads (Android 9 and below).
     */
    @Suppress("DEPRECATION")
    private fun saveToDownloadsLegacy(fileName: String, content: String): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { outputStream ->
            outputStream.write(content.toByteArray())
        }

        return file.absolutePath
    }

    /**
     * Gets the count of available log entries.
     */
    fun getLogCount(): Int {
        return try {
            YLog.inMemoryData.size
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Clears in-memory logs.
     */
    fun clearLogs() {
        // YLog doesn't have a clear method, but we can note this
        // The logs will be cleared on app restart
    }
}

/**
 * Result of log export operation.
 */
sealed class LogExportResult {
    data class Success(val filePath: String, val lineCount: Int) : LogExportResult()
    data class Error(val message: String) : LogExportResult()
}
