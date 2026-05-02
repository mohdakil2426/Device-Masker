package com.astrixforge.devicemasker.service

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import timber.log.Timber

data class AppLogEntry(
    val timestampMillis: Long,
    val level: String,
    val source: String,
    val tag: String,
    val message: String,
)

class AppLogStore(private val file: File, private val maxEntries: Int = DEFAULT_MAX_ENTRIES) {

    @Synchronized
    fun append(entry: AppLogEntry) {
        ensureParent()
        val entries = readEntriesInternal().toMutableList()
        entries.add(entry.sanitized())
        writeEntries(entries.takeLast(maxEntries))
    }

    @Synchronized fun readEntries(): List<AppLogEntry> = readEntriesInternal()

    @Synchronized
    fun clear() {
        if (file.exists()) {
            file.writeText("")
        }
    }

    private fun readEntriesInternal(): List<AppLogEntry> {
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            val parts = line.split('\t', limit = 5)
            if (parts.size != 5) return@mapNotNull null
            val timestamp = parts[0].toLongOrNull() ?: return@mapNotNull null
            AppLogEntry(
                timestampMillis = timestamp,
                level = parts[1],
                source = parts[2],
                tag = parts[3],
                message = parts[4],
            )
        }
    }

    private fun writeEntries(entries: List<AppLogEntry>) {
        ensureParent()
        file.writeText(entries.joinToString(separator = "\n") { it.toLine() })
        if (entries.isNotEmpty()) {
            file.appendText("\n")
        }
    }

    private fun ensureParent() {
        file.parentFile?.mkdirs()
    }

    private fun AppLogEntry.toLine(): String =
        listOf(
                timestampMillis.toString(),
                level.cleanField(),
                source.cleanField(),
                tag.cleanField(),
                message.cleanField(),
            )
            .joinToString(separator = "\t")

    private fun AppLogEntry.sanitized(): AppLogEntry =
        copy(
            level = level.cleanField(),
            source = source.cleanField(),
            tag = tag.cleanField(),
            message = message.cleanField(),
        )

    private fun String.cleanField(): String =
        replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim()

    companion object {
        private const val DEFAULT_MAX_ENTRIES = 500

        fun from(context: Context): AppLogStore =
            AppLogStore(File(File(context.filesDir, "logs"), "structured.log"))
    }
}

class PersistentAppLogTree(private val store: AppLogStore) : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val fullMessage =
            if (t == null) {
                message
            } else {
                "$message: ${t.javaClass.simpleName}: ${t.message.orEmpty()}"
            }
        runCatching {
            store.append(
                AppLogEntry(
                    timestampMillis = System.currentTimeMillis(),
                    level = priority.toLevel(),
                    source = "app",
                    tag = tag ?: "DeviceMasker",
                    message = fullMessage,
                )
            )
        }
    }

    private fun Int.toLevel(): String =
        when (this) {
            Log.ERROR -> "E"
            Log.WARN -> "W"
            Log.INFO -> "I"
            Log.DEBUG -> "D"
            Log.VERBOSE -> "V"
            else -> "D"
        }
}

object LogFileFormatter {
    private val dateFormat =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
    private val timeFormat =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).apply { timeZone = TimeZone.getDefault() }

    fun build(
        appEntries: List<AppLogEntry>,
        serviceLogs: List<String>,
        diagnosticsStatus: String,
        exportedAtMillis: Long = System.currentTimeMillis(),
    ): String {
        val cleanServiceLogs = serviceLogs.map { it.trim() }.filter { it.isNotEmpty() }
        return buildString {
            appendLine("Device Masker Logs")
            appendLine("exported=${dateFormat.format(Date(exportedAtMillis))}")
            appendLine("diagnostics=$diagnosticsStatus")
            appendLine("app_entries=${appEntries.size}")
            appendLine("service_entries=${cleanServiceLogs.size}")
            appendLine()

            appendLine("[app]")
            if (appEntries.isEmpty()) {
                appendLine("none")
            } else {
                appEntries.forEach { entry ->
                    appendLine(
                        "${timeFormat.format(Date(entry.timestampMillis))} ${entry.level} ${entry.tag} ${entry.message}"
                    )
                }
            }
            appendLine()

            appendLine("[xposed]")
            if (cleanServiceLogs.isEmpty()) {
                appendLine("none")
            } else {
                cleanServiceLogs.forEach { appendLine(it) }
            }
        }
    }
}
