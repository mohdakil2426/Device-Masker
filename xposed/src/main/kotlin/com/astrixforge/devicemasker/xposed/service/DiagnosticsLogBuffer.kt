package com.astrixforge.devicemasker.xposed.service

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

class DiagnosticsLogBuffer(
    private val maxLogs: Int,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val logs = ConcurrentLinkedDeque<String>()
    private val droppedLogs = AtomicInteger(0)
    private val logDateFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

    val droppedCount: Int
        get() = droppedLogs.get()

    fun append(tag: String, message: String, level: Int) {
        val levelStr =
            when (level) {
                ERROR -> "E"
                WARN -> "W"
                INFO -> "I"
                else -> "D"
            }
        logs.addLast(
            "[${logDateFmt.format(Instant.ofEpochMilli(nowMillis()))}] $levelStr/$tag: $message"
        )
        while (logs.size > maxLogs) {
            logs.pollFirst()
            droppedLogs.incrementAndGet()
        }
    }

    fun getLogs(maxCount: Int): List<String> = logs.toList().takeLast(maxCount.coerceIn(1, maxLogs))

    fun clear() {
        logs.clear()
        droppedLogs.set(0)
        append("DMService", "Diagnostics cleared", INFO)
    }

    private companion object {
        private const val ERROR = 6
        private const val WARN = 5
        private const val INFO = 4
    }
}
