package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.IDeviceMaskerService
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DualLog
import com.astrixforge.devicemasker.xposed.HookMetrics
import com.astrixforge.devicemasker.xposed.PrefsHelper
import com.astrixforge.devicemasker.xposed.service.DeviceMaskerService
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker

/**
 * Base hooker with common functionality shared across all spoof hookers.
 *
 * Supports HYBRID configuration delivery:
 * 1. **AIDL Service (Primary)** - Real-time config from DeviceMaskerService
 * 2. **XSharedPreferences (Fallback)** - Cached config when service unavailable
 *
 * Provides:
 * - Centralized spoof value retrieval (service or prefs)
 * - Consistent logging with DualLog + service logging
 * - Metrics recording with HookMetrics
 * - Filter count tracking via service
 *
 * Usage (Legacy - objects):
 * ```kotlin
 * object MyHooker : BaseSpoofHooker("MyHooker") {
 *     override fun onHook() {
 *         logStart()
 *         // Hook logic using getSpoofValue()
 *         recordSuccess()
 *     }
 * }
 * ```
 *
 * @param tag The log tag for this hooker
 */
abstract class BaseSpoofHooker(protected val tag: String) : YukiBaseHooker() {

    // ═══════════════════════════════════════════════════════════
    // SERVICE ACCESS
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets the DeviceMaskerService if available.
     *
     * This is a cached reference obtained once per hooker instance. Returns null if service is not
     * initialized (fallback to prefs).
     */
    protected val service: IDeviceMaskerService? by lazy {
        runCatching {
                if (DeviceMaskerService.isInitialized()) {
                    DeviceMaskerService.getInstance()
                } else {
                    null
                }
            }
            .getOrNull()
    }

    /** Returns true if AIDL service is available for config queries. */
    protected val isServiceAvailable: Boolean
        get() = service != null

    // ═══════════════════════════════════════════════════════════
    // SPOOF VALUE RETRIEVAL (Hybrid)
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets a spoof value using hybrid approach (service first, then prefs).
     *
     * @param type The type of value to spoof
     * @param fallback Generator function if no configured value exists
     * @return The configured spoof value or generated fallback
     */
    protected fun getSpoofValue(type: SpoofType, fallback: () -> String): String {
        // Try service first (real-time config)
        service?.let { svc ->
            runCatching {
                svc.getSpoofValue(packageName, type.name)?.let { value ->
                    if (value.isNotBlank()) {
                        incrementFilterCount()
                        return value
                    }
                }
            }
        }

        // Fallback to XSharedPreferences
        return PrefsHelper.getSpoofValue(prefs, packageName, type, fallback)
    }

    /**
     * Gets a spoof value ONLY from the AIDL service.
     *
     * @param key The spoof type key (e.g., SpoofType.IMEI.name)
     * @return The value from service, or null if unavailable
     */
    protected fun getSpoofValueFromService(key: String): String? {
        return runCatching { service?.getSpoofValue(packageName, key) }.getOrNull()
    }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS & LOGGING
    // ═══════════════════════════════════════════════════════════

    /**
     * Increments the filter count for this package via service.
     *
     * Called automatically when a spoof value is returned from getSpoofValue(). Safe to call even
     * if service is unavailable.
     */
    protected fun incrementFilterCount() {
        runCatching { service?.incrementFilterCount(packageName) }
    }

    /** Logs the start of hook registration for this package. */
    protected fun logStart() {
        val configSource = if (isServiceAvailable) "service" else "prefs"
        logDebug("Starting hooks for: $packageName (config: $configSource)")
    }

    /** Records successful hook initialization in metrics. */
    protected fun recordSuccess() {
        HookMetrics.recordSuccess(tag, "initialization")
    }

    /**
     * Records a hook failure in metrics.
     *
     * @param methodName Name of the method that failed to hook
     */
    protected fun recordFailure(methodName: String) {
        HookMetrics.recordFailure(tag, methodName)
    }

    /** Logs a debug message with the hooker's tag. */
    protected fun logDebug(message: String) {
        DualLog.debug(tag, message)
        logToService(message, LOG_LEVEL_DEBUG)
    }

    /** Logs an info message with the hooker's tag. */
    protected fun logInfo(message: String) {
        DualLog.info(tag, message)
        logToService(message, LOG_LEVEL_INFO)
    }

    /** Logs a warning message with the hooker's tag. */
    protected fun logWarn(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            DualLog.warn(tag, message, throwable)
        } else {
            DualLog.warn(tag, message)
        }
        logToService(message, LOG_LEVEL_WARN)
    }

    /** Logs an error message with the hooker's tag. */
    protected fun logError(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            DualLog.error(tag, message, throwable)
        } else {
            DualLog.error(tag, message)
        }
        logToService(message, LOG_LEVEL_ERROR)
    }

    /**
     * Logs a message to the centralized service log.
     *
     * @param message Log message
     * @param level Log level (0=INFO, 1=WARN, 2=ERROR, 3=DEBUG)
     */
    private fun logToService(message: String, level: Int) {
        runCatching { service?.log(tag, message, level) }
    }

    companion object {
        // Log levels matching IDeviceMaskerService.log()
        private const val LOG_LEVEL_INFO = 0
        private const val LOG_LEVEL_WARN = 1
        private const val LOG_LEVEL_ERROR = 2
        private const val LOG_LEVEL_DEBUG = 3
    }
}
