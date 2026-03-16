package com.astrixforge.devicemasker.xposed.service

import android.util.Log
import com.astrixforge.devicemasker.IDeviceMaskerService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

/**
 * Diagnostics service running in `system_server` — libxposed API 100 / Option B.
 *
 * **What changed from the previous version:**
 * - ALL config-related state removed: `config: AtomicReference<JsonConfig>`, `ConfigManager`
 * - ALL config-related methods removed: `writeConfig`, `readConfig`, `reloadConfig`,
 *   `isModuleEnabled`, `isAppEnabled`, `getSpoofValue`
 * - Config delivery is now exclusively via `RemotePreferences` (libxposed API 100)
 *
 * **What remains:**
 * - `logs`: in-memory ring buffer of hook event messages
 * - `spoofCounts`: per-package spoof event counter
 * - `hookedPackages`: set of packages hooked this session
 *
 * **Thread safety:**
 * - `ConcurrentLinkedDeque` and `ConcurrentHashMap` handle concurrent writes from multiple target
 *   app processes simultaneously (each runs on a binder thread pool)
 * - `AtomicInteger` for lock-free counter increments
 * - No locks held in `oneway` callbacks — all writes are non-blocking
 *
 * **Boot safety:**
 * - If this service fails to initialise or register, hooks still work via RemotePreferences
 * - `DiagnosticsViewModel` shows "Service unavailable" gracefully — non-fatal
 *
 * @see com.astrixforge.devicemasker.xposed.hooker.SystemServiceHooker
 */
@Suppress("TooManyFunctions") // AIDL interface requires all 8 methods
class DeviceMaskerService private constructor() : IDeviceMaskerService.Stub() {

    companion object {
        private const val TAG = "DMService"

        /** Maximum log entries kept in memory. */
        private const val MAX_LOGS = 500

        /** Version used by DiagnosticsViewModel for display. */
        const val VERSION = "2.0.0" // bumped: diagnostics-only, libxposed API 100

        @Volatile private var instance: DeviceMaskerService? = null

        /**
         * Returns the singleton service instance, creating it on first call.
         *
         * Thread-safe via double-checked locking with `@Volatile` field. All initialisation is
         * deferred to creation — no file I/O, no config loading.
         */
        fun getInstance(): DeviceMaskerService =
            instance
                ?: synchronized(this) { instance ?: DeviceMaskerService().also { instance = it } }

        /** Whether the singleton has been created (i.e. the service is registered). */
        fun isInitialized(): Boolean = instance != null
    }

    // ═══════════════════════════════════════════════════════════
    // DIAGNOSTICS STATE — no config state
    // ═══════════════════════════════════════════════════════════

    /** In-memory ring buffer of formatted log entries. Newest entries at tail. */
    private val logs = ConcurrentLinkedDeque<String>()

    /**
     * Per-package spoof event counter. Key = package name. Value = total spoof events reported this
     * session.
     */
    private val spoofCounts = ConcurrentHashMap<String, AtomicInteger>()

    /** Packages that have had hooks registered during this boot session. */
    private val hookedPackages = ConcurrentHashMap.newKeySet<String>()

    private val logDateFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

    // ═══════════════════════════════════════════════════════════
    // REPORTING — oneway calls from hooked processes (non-blocking)
    // These run asynchronously on a binder thread in system_server.
    // ═══════════════════════════════════════════════════════════

    /**
     * Called by hook callbacks after returning a spoofed value. `oneway` in AIDL → runs
     * asynchronously, caller does not wait.
     */
    override fun reportSpoofEvent(packageName: String?, spoofType: String?) {
        if (packageName.isNullOrBlank() || spoofType.isNullOrBlank()) return
        runCatching {
            spoofCounts.getOrPut(packageName) { AtomicInteger(0) }.incrementAndGet()
            appendLog("SPOOF/$spoofType", "$packageName spoofed", Log.DEBUG)
        }
    }

    /**
     * Called by hookers to report events (errors, warnings, hook registrations). `oneway` → caller
     * does not block.
     */
    override fun reportLog(tag: String?, message: String?, level: Int) {
        if (tag.isNullOrBlank() || message.isNullOrBlank()) return
        runCatching { appendLog(tag, message, level) }
    }

    /**
     * Called by `XposedEntry.onPackageLoaded()` once per package per boot session. `oneway` → does
     * not block the hook registration flow.
     */
    override fun reportPackageHooked(packageName: String?) {
        if (packageName.isNullOrBlank()) return
        runCatching {
            hookedPackages.add(packageName)
            appendLog(TAG, "Hooks registered for: $packageName", Log.INFO)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // READS — synchronous calls from DiagnosticsViewModel (IO coroutines)
    // ═══════════════════════════════════════════════════════════

    override fun getSpoofEventCount(packageName: String?): Int {
        if (packageName.isNullOrBlank()) return 0
        return runCatching { spoofCounts[packageName]?.get() ?: 0 }.getOrElse { 0 }
    }

    override fun getHookedPackages(): List<String> =
        runCatching { hookedPackages.toList() }.getOrElse { emptyList() }

    override fun getLogs(maxCount: Int): List<String> =
        runCatching { logs.toList().takeLast(maxCount.coerceIn(1, MAX_LOGS)) }
            .getOrElse { emptyList() }

    override fun clearDiagnostics() {
        runCatching {
            logs.clear()
            spoofCounts.clear()
            hookedPackages.clear()
            appendLog(TAG, "Diagnostics cleared", Log.INFO)
        }
    }

    override fun isAlive(): Boolean = true

    // ═══════════════════════════════════════════════════════════
    // INTERNAL
    // ═══════════════════════════════════════════════════════════

    private fun appendLog(tag: String, message: String, level: Int) {
        val levelStr =
            when (level) {
                Log.ERROR -> "E"
                Log.WARN -> "W"
                Log.INFO -> "I"
                else -> "D"
            }
        val entry = "[${logDateFmt.format(Instant.now())}] $levelStr/$tag: $message"
        logs.addLast(entry)
        // Ring-buffer: trim oldest entries when over capacity
        while (logs.size > MAX_LOGS) {
            logs.pollFirst()
        }
        // Mirror to logcat for debugging (non-fatal if logcat is unavailable)
        runCatching {
            when (level) {
                Log.ERROR -> Log.e(tag, message)
                Log.WARN -> Log.w(tag, message)
                Log.INFO -> Log.i(tag, message)
                else -> Log.d(tag, message)
            }
        }
    }
}
