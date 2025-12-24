package com.astrixforge.devicemasker.xposed

import com.astrixforge.devicemasker.common.Constants
import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.common.SpoofType
import java.io.File

/**
 * Config Reader - Reads configuration from shared file.
 *
 * This provides read-only access to the config stored at:
 * /data/system/devicemasker/config.json
 *
 * The config is written by the UI app (via root shell) and read
 * by each hooked process. This avoids the need for IPC which
 * doesn't work reliably due to SELinux restrictions.
 *
 * Config is cached per-process and can be refreshed if needed.
 */
object ConfigReader {

    private const val TAG = "ConfigReader"

    /** Cached config for this process. */
    @Volatile
    private var cachedConfig: JsonConfig? = null

    /** Lock for thread-safe config loading. */
    private val configLock = Any()

    /** Possible config file locations, in priority order. */
    private val configPaths = listOf(
        File(Constants.SYSTEM_DATA_DIR, Constants.CONFIG_FILE_NAME),
        File("/data/local/tmp/devicemasker", Constants.CONFIG_FILE_NAME),
    )

    /**
     * Gets the current config. Loads from file if not cached.
     */
    fun getConfig(): JsonConfig {
        cachedConfig?.let { return it }

        synchronized(configLock) {
            cachedConfig?.let { return it }

            val config = loadConfigFromFile()
            cachedConfig = config
            return config
        }
    }

    /**
     * Forces a reload of the config from file.
     * Call this after the UI app saves new config.
     */
    fun reloadConfig(): JsonConfig {
        synchronized(configLock) {
            cachedConfig = loadConfigFromFile()
            return cachedConfig!!
        }
    }

    /**
     * Loads config from the first available config file.
     */
    private fun loadConfigFromFile(): JsonConfig {
        for (configFile in configPaths) {
            if (configFile.exists() && configFile.canRead()) {
                return runCatching {
                    val json = configFile.readText()
                    val config = JsonConfig.parse(json)
                    DualLog.debug(TAG, "Loaded config from: ${configFile.absolutePath}")
                    config
                }.getOrElse { e ->
                    DualLog.warn(TAG, "Failed to parse config from ${configFile.absolutePath}: ${e.message}")
                    continue
                }
            }
        }

        // No config file found - return default
        DualLog.debug(TAG, "No config file found, using defaults")
        return JsonConfig.createDefault()
    }

    /**
     * Gets the spoofed value for a specific type and package.
     * This is the main entry point for hookers.
     */
    fun getSpoofValue(packageName: String, type: SpoofType, fallback: () -> String): String {
        val config = getConfig()
        
        // Check if module is enabled
        if (!config.isModuleEnabled) {
            return fallback()
        }

        // Get the group for this app
        val group = config.getGroupForApp(packageName) ?: return fallback()

        // Check if group is enabled
        if (!group.isEnabled) return fallback()

        // Check if this type is enabled
        if (!group.isTypeEnabled(type)) return fallback()

        // Return the value
        return group.getValue(type) ?: fallback()
    }

    /**
     * Checks if the module is enabled globally.
     */
    fun isModuleEnabled(): Boolean {
        return getConfig().isModuleEnabled
    }

    /**
     * Clears the cached config.
     */
    fun clearCache() {
        synchronized(configLock) {
            cachedConfig = null
        }
    }
}
