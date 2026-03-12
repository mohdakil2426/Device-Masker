package com.astrixforge.devicemasker.xposed.service

import com.astrixforge.devicemasker.IDeviceMaskerService
import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DualLog
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * AIDL service implementation running in system_server.
 *
 * This service provides real-time configuration access to all hooked app processes and centralized
 * logging/statistics. It is initialized by SystemServiceHooker when the system boots and exposed
 * via ServiceBridge ContentProvider.
 *
 * Thread Safety:
 * - Config is held in AtomicReference for lock-free reads
 * - Filter counts use ConcurrentHashMap with AtomicInteger values
 * - Logs use ConcurrentLinkedDeque for efficient append operations
 *
 * Usage:
 * - Hookers call getSpoofValue() for config queries
 * - UI connects via ServiceBridge to write/read config
 * - All operations wrapped in try-catch to prevent system_server crashes
 *
 * @see IDeviceMaskerService
 * @see ConfigManager
 * @see ServiceBridge
 */
@Suppress("TooManyFunctions") // AIDL interface requires many methods
class DeviceMaskerService private constructor() : IDeviceMaskerService.Stub() {

    companion object {
        private const val TAG = "DeviceMaskerService"
        const val VERSION = "1.0.0"
        private const val MAX_LOGS = 1000

        @Volatile private var instance: DeviceMaskerService? = null

        /**
         * Gets or creates the singleton service instance.
         *
         * Thread-safe double-checked locking with volatile instance.
         */
        fun getInstance(): DeviceMaskerService {
            return instance
                ?: synchronized(this) {
                    instance
                        ?: DeviceMaskerService().also {
                            instance = it
                            it.initialize()
                        }
                }
        }

        /** Checks if the service instance exists without creating it. */
        fun isInitialized(): Boolean = instance != null
    }

    // ═══════════════════════════════════════════════════════════
    // STATE (Thread-Safe)
    // ═══════════════════════════════════════════════════════════

    /** Current configuration (lock-free reads via AtomicReference) */
    private val config = AtomicReference<JsonConfig>(JsonConfig.createDefault())

    /** Service start time for uptime calculation */
    private val startTime = AtomicLong(System.currentTimeMillis())

    /** Centralized log buffer (newest entries at end) */
    private val logs = ConcurrentLinkedDeque<String>()

    /** Per-app filter counts (package name -> count) */
    private val filterCounts = ConcurrentHashMap<String, AtomicInteger>()

    /** Set of apps that have been hooked this session */
    private val hookedApps = ConcurrentHashMap.newKeySet<String>()

    // ═══════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Initializes the service by loading configuration from disk.
     *
     * Called once when getInstance() creates the singleton. All initialization is wrapped in
     * try-catch to prevent bootloops.
     */
    private fun initialize() {
        runCatching {
                startTime.set(System.currentTimeMillis())

                // Load config from disk
                val loadedConfig = ConfigManager.loadConfig()
                if (loadedConfig != null) {
                    config.set(loadedConfig)
                    log(
                        TAG,
                        "Service initialized with ${loadedConfig.getAllGroups().size} groups",
                        LOG_LEVEL_INFO,
                    )
                } else {
                    log(TAG, "Using default config (load failed or first run)", LOG_LEVEL_WARN)
                }
            }
            .onFailure { e -> log(TAG, "Initialization error: ${e.message}", LOG_LEVEL_ERROR) }
    }

    // ═══════════════════════════════════════════════════════════
    // CONFIGURATION METHODS
    // ═══════════════════════════════════════════════════════════

    /**
     * Writes complete configuration from JSON string.
     *
     * Called by UI app when user saves settings.
     */
    override fun writeConfig(json: String?) {
        runCatching {
                if (json.isNullOrBlank()) {
                    log(TAG, "writeConfig: Empty JSON received", LOG_LEVEL_WARN)
                    return
                }

                val newConfig = JsonConfig.parse(json)
                config.set(newConfig)
                ConfigManager.saveConfig(newConfig)
                log(TAG, "Config updated: ${newConfig.getAllGroups().size} groups", LOG_LEVEL_INFO)
            }
            .onFailure { e -> log(TAG, "writeConfig failed: ${e.message}", LOG_LEVEL_ERROR) }
    }

    /** Reads current configuration as JSON string. */
    override fun readConfig(): String? {
        return runCatching { config.get().toJsonString() }
            .getOrElse { e ->
                log(TAG, "readConfig failed: ${e.message}", LOG_LEVEL_ERROR)
                null
            }
    }

    /**
     * Force reloads configuration from disk.
     *
     * Useful after external modifications or for recovery.
     */
    override fun reloadConfig() {
        runCatching {
                val reloadedConfig = ConfigManager.loadConfig()
                if (reloadedConfig != null) {
                    config.set(reloadedConfig)
                    log(TAG, "Config reloaded from disk", LOG_LEVEL_INFO)
                }
            }
            .onFailure { e -> log(TAG, "reloadConfig failed: ${e.message}", LOG_LEVEL_ERROR) }
    }

    // ═══════════════════════════════════════════════════════════
    // QUERY METHODS (Called by Hookers)
    // ═══════════════════════════════════════════════════════════

    /** Checks if the module is globally enabled. */
    override fun isModuleEnabled(): Boolean {
        return runCatching { config.get().isModuleEnabled }.getOrElse { false }
    }

    /**
     * Checks if spoofing is enabled for a specific app.
     *
     * An app is considered enabled if:
     * 1. Module is globally enabled
     * 2. App has an AppConfig with isEnabled = true, OR
     * 3. App is not configured but a default group exists
     */
    override fun isAppEnabled(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false

        return runCatching {
                val currentConfig = config.get()

                // Module must be enabled
                if (!currentConfig.isModuleEnabled) return false

                // Check if app has a group (explicit or default)
                val group = currentConfig.getGroupForApp(packageName)
                group != null && group.isEnabled
            }
            .getOrElse { false }
    }

    /**
     * Gets a specific spoof value for an app.
     *
     * @param packageName The target app package
     * @param key The spoof type key (e.g., "IMEI", "MAC_ADDRESS")
     * @return The spoofed value, or null if not configured
     */
    override fun getSpoofValue(packageName: String?, key: String?): String? {
        if (packageName.isNullOrBlank() || key.isNullOrBlank()) return null

        return runCatching {
                val currentConfig = config.get()

                // Get the group for this app
                val group = currentConfig.getGroupForApp(packageName) ?: return null

                // Check if group is enabled
                if (!group.isEnabled) return null

                // Get the spoof type from key
                val spoofType = runCatching { SpoofType.valueOf(key) }.getOrNull() ?: return null

                // Check if this type is enabled in the group
                if (!group.isTypeEnabled(spoofType)) return null

                // Return the value
                group.getValue(spoofType)
            }
            .getOrNull()
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS METHODS
    // ═══════════════════════════════════════════════════════════

    /**
     * Increments the filter count for an app.
     *
     * Called by hookers when a value is successfully spoofed.
     */
    override fun incrementFilterCount(packageName: String?) {
        if (packageName.isNullOrBlank()) return

        runCatching {
            filterCounts.computeIfAbsent(packageName) { AtomicInteger(0) }.incrementAndGet()
            hookedApps.add(packageName)
        }
    }

    /** Gets the filter count for a specific app. */
    override fun getFilterCount(packageName: String?): Int {
        if (packageName.isNullOrBlank()) return 0
        return filterCounts[packageName]?.get() ?: 0
    }

    /** Gets the total number of apps with active hooks. */
    override fun getHookedAppCount(): Int {
        return hookedApps.size
    }

    // ═══════════════════════════════════════════════════════════
    // LOGGING METHODS
    // ═══════════════════════════════════════════════════════════

    /**
     * Logs a message to the centralized service log.
     *
     * @param tag Log tag (typically hooker name)
     * @param message Log message
     * @param level Log level: 0=INFO, 1=WARN, 2=ERROR, 3=DEBUG
     */
    override fun log(tag: String?, message: String?, level: Int) {
        if (tag.isNullOrBlank() || message.isNullOrBlank()) return

        runCatching {
            val levelStr =
                when (level) {
                    LOG_LEVEL_INFO -> "I"
                    LOG_LEVEL_WARN -> "W"
                    LOG_LEVEL_ERROR -> "E"
                    LOG_LEVEL_DEBUG -> "D"
                    else -> "?"
                }

            val timestamp = System.currentTimeMillis()
            val entry = "$timestamp|$levelStr|$tag|$message"

            logs.addLast(entry)

            // Trim log buffer if too large
            while (logs.size > MAX_LOGS) {
                logs.pollFirst()
            }

            // Also log to DualLog for debugging
            DualLog.info("$tag", message)
        }
    }

    /**
     * Gets recent log entries.
     *
     * @param maxCount Maximum number of entries to return
     * @return List of formatted log entries (newest last)
     */
    override fun getLogs(maxCount: Int): List<String> {
        return runCatching {
                val count = maxCount.coerceIn(1, MAX_LOGS)
                logs.toList().takeLast(count)
            }
            .getOrElse { emptyList() }
    }

    /** Clears all log entries. */
    override fun clearLogs() {
        logs.clear()
        log(TAG, "Logs cleared", LOG_LEVEL_INFO)
    }

    // ═══════════════════════════════════════════════════════════
    // CONTROL METHODS
    // ═══════════════════════════════════════════════════════════

    /**
     * Checks if the service is alive and responding.
     *
     * @return Always true if reachable
     */
    override fun isServiceAlive(): Boolean = true

    /** Gets the service version string. */
    override fun getServiceVersion(): String = VERSION

    /** Gets service uptime in milliseconds. */
    override fun getServiceUptime(): Long {
        return System.currentTimeMillis() - startTime.get()
    }
}

// Log level constants
private const val LOG_LEVEL_INFO = 0
private const val LOG_LEVEL_WARN = 1
private const val LOG_LEVEL_ERROR = 2
private const val LOG_LEVEL_DEBUG = 3
