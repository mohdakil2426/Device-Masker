package com.astrixforge.devicemasker.xposed

import android.util.Log

/**
 * Dual Logger — logs to Android logcat and mirrors structured entries to the diagnostics service.
 *
 * ## Migration note (libxposed API 100)
 *
 * Previously used `YLog` from YukiHookAPI which routed to LSPosed Manager's log screen. With
 * libxposed API 100, we route to the standard `android.util.Log` instead. LSPosed captures module
 * process logcat output and displays it in its log screen, so all logs remain visible in LSPosed
 * Manager as before.
 *
 * Structured logs are also forwarded to the diagnostics AIDL service when it is available so the
 * UI sees the same failures that reach logcat.
 *
 * Usage:
 * ```kotlin
 * DualLog.debug("MyHooker", "Hook applied successfully")
 * DualLog.info("MyHooker", "Started hooks for: com.example.app")
 * DualLog.error("MyHooker", "Failed to hook", exception)
 * ```
 */
@Suppress("unused") // Logging utility — all methods are API surface
object DualLog {

    // ═══════════════════════════════════════════════════════════
    // LOGGING METHODS
    // ═══════════════════════════════════════════════════════════

    /** Debug level log — for detailed debugging info. */
    fun debug(tag: String, message: String) {
        Log.d(tag, message)
        report("D", tag, message, Log.DEBUG)
    }

    /** Info level log — for important events. */
    fun info(tag: String, message: String) {
        Log.i(tag, message)
        report("I", tag, message, Log.INFO)
    }

    /** Warning level log — for recoverable issues. */
    fun warn(tag: String, message: String) {
        Log.w(tag, message)
        report("W", tag, message, Log.WARN)
    }

    /** Warning level log with exception. */
    fun warn(tag: String, message: String, throwable: Throwable) {
        val fullMessage = "$message: ${throwable.message}"
        Log.w(tag, fullMessage, throwable)
        report("W", tag, fullMessage, Log.WARN)
    }

    /** Error level log — for failures. */
    fun error(tag: String, message: String) {
        Log.e(tag, message)
        report("E", tag, message, Log.ERROR)
    }

    /** Error level log with exception. */
    fun error(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
        report("E", tag, "$message: ${throwable.message}", Log.ERROR)
        throwable.stackTrace.take(5).forEach { element ->
            report("E", tag, "  at $element", Log.ERROR)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    private fun report(level: String, tag: String, message: String, priority: Int) {
        val entry = "[$level][$tag] $message"
        runCatching { XposedEntry.instance.reportLog(tag, entry, priority) }
    }
}

/**
 * Hook Metrics — Tracks hook registration success/failure rates.
 *
 * Used by DiagnosticsViewModel to display hook health information.
 */
object HookMetrics {
    private val successCount = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val failureCount = java.util.concurrent.ConcurrentHashMap<String, Int>()

    fun recordSuccess(hookerName: String, methodName: String) {
        val key = "$hookerName.$methodName"
        successCount[key] = successCount.getOrDefault(key, 0) + 1
    }

    fun recordFailure(hookerName: String, methodName: String) {
        val key = "$hookerName.$methodName"
        failureCount[key] = failureCount.getOrDefault(key, 0) + 1
    }

    fun getSuccessCount(): Int = successCount.values.sum()

    fun getFailureCount(): Int = failureCount.values.sum()

    fun getSummary(): String = buildString {
        appendLine("=== Hook Metrics ===")
        appendLine("Success: ${getSuccessCount()} calls")
        appendLine("Failures: ${getFailureCount()} calls")
        if (failureCount.isNotEmpty()) {
            appendLine("Failed methods:")
            failureCount.forEach { (method, count) -> appendLine("  - $method: $count") }
        }
    }

    fun dumpToLog() {
        DualLog.info("HookMetrics", getSummary())
    }
}
