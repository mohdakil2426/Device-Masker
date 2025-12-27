package com.astrixforge.devicemasker.service

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.astrixforge.devicemasker.BuildConfig
import com.highcapable.yukihookapi.hook.log.YLog
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Log Manager - Comprehensive log export with industry-standard formatting.
 *
 * Two separate export options (both always enabled in Beta):
 * 1. Export Logs - YLog in-memory data (app process logs)
 * 2. Capture Logcat - System logcat output (all hooked app logs)
 *
 * Both support:
 * - Default save to Downloads folder
 * - Custom save location via SAF file picker
 * - Industry-standard, well-structured output
 */
object LogManager {

    private const val TAG = "LogManager"
    private const val LOG_FILE_PREFIX = "devicemasker_logs_"
    private const val LOGCAT_FILE_PREFIX = "devicemasker_logcat_"
    private const val LOG_FILE_EXTENSION = ".log"
    private const val MIME_TYPE = "text/plain"

    // Logcat filter tags for DeviceMasker
    private val LOGCAT_TAGS = listOf(
        "DeviceMasker",
        "DeviceHooker",
        "NetworkHooker",
        "AntiDetectHooker",
        "SystemHooker",
        "LocationHooker",
        "AdvertisingHooker",
        "SensorHooker",
        "WebViewHooker",
        "ConfigSync",
        "ConfigManager",
        "DeviceMaskerApp",
        "PrefsReader",
        "HookHelper",
        "ClassCache"
    )

    // Industry-standard log separators
    private const val HEADER_LINE = "════════════════════════════════════════════════════════════════════════════════"
    private const val SECTION_LINE = "────────────────────────────────────────────────────────────────────────────────"
    private const val SUBSECTION_LINE = "┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄"

    // ═══════════════════════════════════════════════════════════════════
    // EXPORT LOGS (In-Memory YLog Data)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Exports YLog in-memory data to Downloads folder.
     * Contains logs from Device Masker app process only.
     */
    fun exportLogs(context: Context): LogExportResult {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "$LOG_FILE_PREFIX$timestamp$LOG_FILE_EXTENSION"
            val logContent = buildInMemoryLogContent(context)

            val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, fileName, logContent)
            } else {
                saveToDownloadsLegacy(fileName, logContent)
            }

            LogExportResult.Success(filePath, logContent.lines().size, isLogcat = false)
        } catch (e: Exception) {
            LogExportResult.Error("Export failed: ${e.message}")
        }
    }

    /**
     * Exports YLog in-memory data to a custom URI location.
     */
    fun exportLogsToUri(context: Context, uri: Uri): LogExportResult {
        return try {
            val logContent = buildInMemoryLogContent(context)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(logContent.toByteArray())
            } ?: return LogExportResult.Error("Failed to open file for writing")

            LogExportResult.Success(uri.lastPathSegment ?: "logs.log", logContent.lines().size, isLogcat = false)
        } catch (e: Exception) {
            LogExportResult.Error("Export failed: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // CAPTURE LOGCAT (System Logs from All Hooked Apps)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Captures logcat output for DeviceMasker tags to Downloads folder.
     * Contains logs from ALL hooked app processes.
     */
    fun captureLogcat(context: Context, lineLimit: Int = 2000): LogExportResult {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "$LOGCAT_FILE_PREFIX$timestamp$LOG_FILE_EXTENSION"
            val logContent = buildLogcatContent(context, lineLimit)

            val filePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, fileName, logContent)
            } else {
                saveToDownloadsLegacy(fileName, logContent)
            }

            LogExportResult.Success(filePath, logContent.lines().size, isLogcat = true)
        } catch (e: Exception) {
            LogExportResult.Error("Logcat capture failed: ${e.message}")
        }
    }

    /**
     * Captures logcat output to a custom URI location.
     */
    fun captureLogcatToUri(context: Context, uri: Uri, lineLimit: Int = 2000): LogExportResult {
        return try {
            val logContent = buildLogcatContent(context, lineLimit)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(logContent.toByteArray())
            } ?: return LogExportResult.Error("Failed to open file for writing")

            LogExportResult.Success(uri.lastPathSegment ?: "logcat.log", logContent.lines().size, isLogcat = true)
        } catch (e: Exception) {
            LogExportResult.Error("Logcat capture failed: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // IN-MEMORY LOG CONTENT BUILDER
    // ═══════════════════════════════════════════════════════════════════

    private fun buildInMemoryLogContent(context: Context): String {
        val builder = StringBuilder()
        val exportTime = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US)
        dateFormat.timeZone = TimeZone.getDefault()

        // Header
        builder.appendLine(HEADER_LINE)
        builder.appendLine("              DEVICE MASKER - IN-MEMORY LOG EXPORT")
        builder.appendLine("                    YukiHookAPI YLog Data")
        builder.appendLine(HEADER_LINE)
        builder.appendLine()

        // Metadata
        appendMetadata(builder, context, "IN-MEMORY LOGS", dateFormat, exportTime)

        // Log Content
        builder.appendLine()
        builder.appendLine("[LOG ENTRIES]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine()
        builder.appendLine("Source: YukiHookAPI YLog.inMemoryData")
        builder.appendLine("Scope: Device Masker app process only")
        builder.appendLine("Note: For logs from hooked apps, use 'Capture Logcat'")
        builder.appendLine(SUBSECTION_LINE)
        builder.appendLine()

        try {
            val ylogData = YLog.inMemoryData
            if (ylogData.isNotEmpty()) {
                builder.appendLine("Total Entries: ${ylogData.size}")
                builder.appendLine()
                builder.appendLine("TIMESTAMP                    | LVL | MESSAGE")
                builder.appendLine(SECTION_LINE)

                ylogData.forEach { entry ->
                    val level = parseLogLevel(entry.priority.toString())
                    builder.appendLine("${entry.time.padEnd(28)} | $level | ${entry.msg}")

                    entry.throwable?.let { t ->
                        builder.appendLine("                             |     | Exception: ${t.javaClass.simpleName}: ${t.message}")
                        t.stackTrace.take(5).forEach { se ->
                            builder.appendLine("                             |     |   at $se")
                        }
                    }
                }
            } else {
                builder.appendLine("(No in-memory logs available)")
                builder.appendLine()
                builder.appendLine("Possible reasons:")
                builder.appendLine("  • App was just launched (no operations performed yet)")
                builder.appendLine("  • Memory was cleared")
                builder.appendLine("  • Use 'Capture Logcat' for hook logs from other apps")
            }
        } catch (e: Exception) {
            builder.appendLine("(Failed to read in-memory logs: ${e.message})")
        }

        // Footer
        appendFooter(builder, dateFormat)

        return builder.toString()
    }

    // ═══════════════════════════════════════════════════════════════════
    // LOGCAT CONTENT BUILDER
    // ═══════════════════════════════════════════════════════════════════

    private fun buildLogcatContent(context: Context, lineLimit: Int): String {
        val builder = StringBuilder()
        val exportTime = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US)
        dateFormat.timeZone = TimeZone.getDefault()

        // Header
        builder.appendLine(HEADER_LINE)
        builder.appendLine("               DEVICE MASKER - LOGCAT CAPTURE")
        builder.appendLine("                  System Logcat Output")
        builder.appendLine(HEADER_LINE)
        builder.appendLine()

        // Metadata
        appendMetadata(builder, context, "LOGCAT CAPTURE", dateFormat, exportTime)

        // Logcat Info
        builder.appendLine()
        builder.appendLine("[CAPTURE INFO]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine("Line Limit       : $lineLimit")
        builder.appendLine("Filter Tags      : ${LOGCAT_TAGS.size} tags")
        builder.appendLine("Root Access      : ${if (hasRootAccess()) "Available" else "Not Available"}")
        builder.appendLine()
        builder.appendLine("Filtered Tags:")
        LOGCAT_TAGS.chunked(4).forEach { chunk ->
            builder.appendLine("  ${chunk.joinToString(", ")}")
        }
        builder.appendLine()

        // Logcat Content
        builder.appendLine("[LOGCAT OUTPUT]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine()
        builder.appendLine("Source: Android Logcat (filtered)")
        builder.appendLine("Scope: ALL app processes with DeviceMasker hooks")
        builder.appendLine(SUBSECTION_LINE)
        builder.appendLine()

        val logcatOutput = captureLogcatRaw(lineLimit)
        if (logcatOutput != null && logcatOutput.isNotEmpty()) {
            val lines = logcatOutput.lines().filter { it.isNotBlank() }
            builder.appendLine("Total Lines: ${lines.size}")
            builder.appendLine()
            builder.appendLine("LOGCAT OUTPUT:")
            builder.appendLine(SECTION_LINE)
            builder.appendLine(logcatOutput)
        } else {
            builder.appendLine("(No logcat entries captured)")
            builder.appendLine()
            builder.appendLine("Possible reasons:")
            builder.appendLine("  • Root/Shell access not available")
            builder.appendLine("  • No DeviceMasker hooks have been triggered yet")
            builder.appendLine("  • Target apps haven't been launched since module activation")
            builder.appendLine()
            builder.appendLine("Manual capture command:")
            builder.appendLine("  adb logcat -d -s ${LOGCAT_TAGS.take(5).joinToString(" -s ")} ...")
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
        exportTime: Date
    ) {
        builder.appendLine("[EXPORT METADATA]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine("Export Type      : $logType")
        builder.appendLine("Export Time      : ${dateFormat.format(exportTime)}")
        builder.appendLine("Timezone         : ${TimeZone.getDefault().id}")
        builder.appendLine("Log Format       : Device Masker Debug Log v1.0")
        builder.appendLine()

        builder.appendLine("[APPLICATION INFO]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine("App Name         : Device Masker")
        builder.appendLine("Package          : ${context.packageName}")
        builder.appendLine("Version          : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        builder.appendLine("Build Type       : ${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE (Beta)"}")
        builder.appendLine()

        builder.appendLine("[DEVICE INFO]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine("Manufacturer     : ${Build.MANUFACTURER}")
        builder.appendLine("Model            : ${Build.MODEL}")
        builder.appendLine("Device           : ${Build.DEVICE}")
        builder.appendLine("Android          : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
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

    private fun parseLogLevel(priorityStr: String): String {
        return when {
            priorityStr.contains("2") || priorityStr.contains("VERBOSE", true) -> "VRB"
            priorityStr.contains("3") || priorityStr.contains("DEBUG", true) -> "DBG"
            priorityStr.contains("4") || priorityStr.contains("INFO", true) -> "INF"
            priorityStr.contains("5") || priorityStr.contains("WARN", true) -> "WRN"
            priorityStr.contains("6") || priorityStr.contains("ERROR", true) -> "ERR"
            else -> "UNK"
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // LOGCAT CAPTURE METHODS
    // ═══════════════════════════════════════════════════════════════════

    private fun captureLogcatRaw(lineLimit: Int): String? {
        val tagFilter = LOGCAT_TAGS.joinToString(" ") { "$it:*" } + " *:S"
        val logcatCommand = "logcat -d -v time -t $lineLimit $tagFilter"

        val methods = listOf(
            { executeWithSu(logcatCommand) },
            { executeWithRuntime(logcatCommand) },
            { executeSimpleLogcat(lineLimit) }
        )

        for (method in methods) {
            try {
                val result = method()
                if (result != null && result.isNotEmpty()) {
                    return result
                }
            } catch (_: Exception) {
                // Try next method
            }
        }
        return null
    }

    private fun executeWithSu(command: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            process.waitFor()
            if (process.exitValue() == 0) output else null
        } catch (_: Exception) { null }
    }

    private fun executeWithRuntime(command: String): String? {
        return try {
            val process = Runtime.getRuntime().exec(command.split(" ").toTypedArray())
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            process.waitFor()
            output.ifEmpty { null }
        } catch (_: Exception) { null }
    }

    private fun executeSimpleLogcat(lineLimit: Int): String? {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", lineLimit.toString()))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            process.waitFor()

            val filtered = output.lines().filter { line ->
                LOGCAT_TAGS.any { tag -> line.contains(tag, ignoreCase = true) }
            }.joinToString("\n")

            filtered.ifEmpty { null }
        } catch (_: Exception) { null }
    }

    // ═══════════════════════════════════════════════════════════════════
    // FILE SAVING HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private fun saveWithMediaStore(context: Context, fileName: String, content: String): String {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw Exception("Failed to create file")

        resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
            ?: throw Exception("Failed to write")

        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

        return "Downloads/$fileName"
    }

    @Suppress("DEPRECATION")
    private fun saveToDownloadsLegacy(fileName: String, content: String): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { it.write(content.toByteArray()) }
        return file.absolutePath
    }

    // ═══════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════

    fun getLogCount(): Int = try { YLog.inMemoryData.size } catch (_: Exception) { 0 }

    fun hasRootAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            process.waitFor()
            output.contains("uid=0")
        } catch (_: Exception) { false }
    }

    fun generateLogFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "$LOG_FILE_PREFIX$timestamp$LOG_FILE_EXTENSION"
    }

    fun generateLogcatFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "$LOGCAT_FILE_PREFIX$timestamp$LOG_FILE_EXTENSION"
    }
}

/**
 * Result of log export operation.
 */
sealed class LogExportResult {
    data class Success(val filePath: String, val lineCount: Int, val isLogcat: Boolean = false) : LogExportResult()
    data class Error(val message: String) : LogExportResult()
}
