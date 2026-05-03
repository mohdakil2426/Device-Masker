package com.astrixforge.devicemasker.service

import android.content.Context
import android.util.Log
import com.astrixforge.devicemasker.common.diagnostics.DiagnosticEvent
import com.astrixforge.devicemasker.common.diagnostics.DiagnosticEventType
import com.astrixforge.devicemasker.common.diagnostics.DiagnosticRedactor
import com.astrixforge.devicemasker.common.diagnostics.DiagnosticSeverity
import com.astrixforge.devicemasker.common.diagnostics.DiagnosticSource
import com.astrixforge.devicemasker.common.diagnostics.RedactionMode
import com.astrixforge.devicemasker.service.diagnostics.JsonlDiagnosticStore
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
    private val sessionId = "app-log"
    private val bootId = "unknown"
    private val redactor = DiagnosticRedactor(RedactionMode.REDACTED)
    private val eventStore = JsonlDiagnosticStore(sessionDir = file.toSessionDir())

    @Synchronized
    fun append(entry: AppLogEntry) {
        appendEvent(entry.toDiagnosticEvent())
    }

    @Synchronized
    fun appendEvent(event: DiagnosticEvent) {
        eventStore.append(redactor.redactEvent(event))
    }

    @Synchronized
    fun readEntries(): List<AppLogEntry> =
        readDiagnosticEvents().takeLast(maxEntries).map { event ->
            AppLogEntry(
                timestampMillis = event.timestampWallMillis,
                level = event.severity.toLevel(),
                source = event.source.name.lowercase(Locale.US),
                tag = event.hooker ?: event.extras["tag"].orEmpty().ifBlank { "DeviceMasker" },
                message = event.message,
            )
        }

    @Synchronized fun readDiagnosticEvents(): List<DiagnosticEvent> = eventStore.readEvents()

    @Synchronized
    fun clear() {
        file.toSessionDir().listFiles { candidate ->
            candidate.isFile && (candidate.extension == "jsonl" || candidate.name == "store_state.json")
        }?.forEach { it.delete() }
    }

    fun appStartEvent(timestampMillis: Long = System.currentTimeMillis()): DiagnosticEvent =
        baseEvent(
            timestampMillis = timestampMillis,
            severity = DiagnosticSeverity.INFO,
            eventType = DiagnosticEventType.APP_START,
            tag = "DeviceMaskerApp",
            message = "Device Masker Application initialised",
        )

    private fun AppLogEntry.toDiagnosticEvent(): DiagnosticEvent =
        baseEvent(
            timestampMillis = timestampMillis,
            severity = level.toSeverity(),
            eventType = DiagnosticEventType.APP_LOG,
            tag = tag.cleanField(),
            message = message.cleanField(),
        )

    private fun baseEvent(
        timestampMillis: Long,
        severity: DiagnosticSeverity,
        eventType: DiagnosticEventType,
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ): DiagnosticEvent =
        DiagnosticEvent(
            eventId = DiagnosticEvent.nextEventId(timestampMillis),
            timestampWallMillis = timestampMillis,
            timestampElapsedMillis = System.nanoTime() / 1_000_000,
            sessionId = sessionId,
            bootId = bootId,
            source = DiagnosticSource.APP,
            severity = severity,
            eventType = eventType,
            threadName = Thread.currentThread().name,
            hooker = tag,
            message = message.cleanField(),
            throwableClass = throwable?.javaClass?.simpleName,
            stacktrace = throwable?.stackTraceToString()?.lines().orEmpty(),
            extras = mapOf("tag" to tag.cleanField()),
        )

    private fun String.cleanField(): String =
        replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim()

    private fun String.toSeverity(): DiagnosticSeverity =
        when (this) {
            "E" -> DiagnosticSeverity.ERROR
            "W" -> DiagnosticSeverity.WARN
            "I" -> DiagnosticSeverity.INFO
            "V" -> DiagnosticSeverity.VERBOSE
            else -> DiagnosticSeverity.DEBUG
        }

    private fun DiagnosticSeverity.toLevel(): String =
        when (this) {
            DiagnosticSeverity.ERROR,
            DiagnosticSeverity.FATAL -> "E"
            DiagnosticSeverity.WARN -> "W"
            DiagnosticSeverity.INFO -> "I"
            DiagnosticSeverity.VERBOSE -> "V"
            DiagnosticSeverity.DEBUG -> "D"
        }

    private fun File.toSessionDir(): File =
        if (extension == "log") {
            parentFile ?: File(".")
        } else {
            this
        }

    companion object {
        private const val DEFAULT_MAX_ENTRIES = 500

        fun from(context: Context): AppLogStore =
            AppLogStore(File(File(File(context.filesDir, "logs"), "sessions"), "session_app"))
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
            val timestampMillis = System.currentTimeMillis()
            store.appendEvent(
                DiagnosticEvent(
                    eventId = DiagnosticEvent.nextEventId(timestampMillis),
                    timestampWallMillis = timestampMillis,
                    timestampElapsedMillis = System.nanoTime() / 1_000_000,
                    sessionId = "app-log",
                    bootId = "unknown",
                    source = DiagnosticSource.APP,
                    severity = priority.toSeverity(),
                    eventType = DiagnosticEventType.APP_LOG,
                    threadName = Thread.currentThread().name,
                    hooker = tag ?: "DeviceMasker",
                    message = fullMessage,
                    throwableClass = t?.javaClass?.simpleName,
                    stacktrace = t?.stackTraceToString()?.lines().orEmpty(),
                    extras = mapOf("tag" to (tag ?: "DeviceMasker")),
                )
            )
        }
    }

    private fun Int.toSeverity(): DiagnosticSeverity =
        when (this) {
            Log.ERROR -> DiagnosticSeverity.ERROR
            Log.WARN -> DiagnosticSeverity.WARN
            Log.INFO -> DiagnosticSeverity.INFO
            Log.VERBOSE -> DiagnosticSeverity.VERBOSE
            else -> DiagnosticSeverity.DEBUG
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
