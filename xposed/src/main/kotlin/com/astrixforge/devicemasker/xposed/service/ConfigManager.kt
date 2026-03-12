package com.astrixforge.devicemasker.xposed.service

import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.xposed.DualLog
import java.io.File

/**
 * Manages configuration persistence for the DeviceMaskerService.
 *
 * Configuration is stored in /data/misc/devicemasker/ which is accessible by system_server without
 * requiring additional SELinux policy modifications.
 *
 * Features:
 * - Atomic file writes (temp file + rename) to prevent corruption
 * - Backup file maintained for recovery
 * - JSON parsing with error handling
 *
 * @see DeviceMaskerService
 */
object ConfigManager {

    private const val TAG = "ConfigManager"

    // Use /data/misc/ - accessible by system_server with default SELinux context
    private val CONFIG_DIR = File("/data/misc/devicemasker")
    private val CONFIG_FILE = File(CONFIG_DIR, "config.json")
    private val BACKUP_FILE = File(CONFIG_DIR, "config.json.bak")
    private val TEMP_FILE = File(CONFIG_DIR, "config.json.tmp")

    /**
     * Loads configuration from disk.
     *
     * On first run, creates the config directory and saves a default config. If the config file is
     * corrupted, attempts to restore from backup.
     *
     * @return Loaded JsonConfig, or null if loading fails completely
     */
    fun loadConfig(): JsonConfig? {
        return runCatching {
                // Ensure directory exists
                if (!CONFIG_DIR.exists()) {
                    DualLog.info(TAG, "Creating config directory: ${CONFIG_DIR.absolutePath}")
                    CONFIG_DIR.mkdirs()
                }

                // Check if config file exists
                if (!CONFIG_FILE.exists()) {
                    DualLog.info(TAG, "No config file found, creating default")
                    val defaultConfig = JsonConfig.createDefault()
                    saveConfig(defaultConfig)
                    return defaultConfig
                }

                // Try to load existing config
                val json = CONFIG_FILE.readText()
                val config = JsonConfig.parse(json)
                DualLog.debug(TAG, "Config loaded: ${config.getAllGroups().size} groups")
                config
            }
            .getOrElse { e ->
                DualLog.error(TAG, "Failed to load config: ${e.message}")
                // Try to restore from backup
                restoreFromBackup()
            }
    }

    /**
     * Saves configuration to disk using atomic write pattern.
     *
     * The write process:
     * 1. Backup existing config (if exists)
     * 2. Write to temp file
     * 3. Rename temp file to config file (atomic on most filesystems)
     *
     * @param config The configuration to save
     * @return true if save was successful, false otherwise
     */
    fun saveConfig(config: JsonConfig): Boolean {
        return runCatching {
                // Ensure directory exists
                CONFIG_DIR.mkdirs()

                // Backup existing config
                if (CONFIG_FILE.exists()) {
                    runCatching { CONFIG_FILE.copyTo(BACKUP_FILE, overwrite = true) }
                        .onFailure { DualLog.warn(TAG, "Failed to create backup: ${it.message}") }
                }

                // Atomic write: write to temp, then rename
                val json = config.toPrettyJsonString()
                TEMP_FILE.writeText(json)

                // Rename (atomic operation on most filesystems)
                val renamed = TEMP_FILE.renameTo(CONFIG_FILE)
                if (!renamed) {
                    // Fallback: copy and delete
                    TEMP_FILE.copyTo(CONFIG_FILE, overwrite = true)
                    TEMP_FILE.delete()
                }

                DualLog.info(TAG, "Config saved successfully")
                true
            }
            .getOrElse { e ->
                DualLog.error(TAG, "Failed to save config: ${e.message}")
                false
            }
    }

    /**
     * Deletes all configuration files.
     *
     * Used for complete reset or uninstallation cleanup.
     */
    fun deleteConfig() {
        runCatching {
                CONFIG_FILE.delete()
                BACKUP_FILE.delete()
                TEMP_FILE.delete()
                DualLog.info(TAG, "Config files deleted")
            }
            .onFailure { DualLog.error(TAG, "Failed to delete config: ${it.message}") }
    }

    /**
     * Attempts to restore configuration from backup file.
     *
     * @return Restored JsonConfig, or default config if restore fails
     */
    private fun restoreFromBackup(): JsonConfig? {
        return runCatching {
                if (!BACKUP_FILE.exists()) {
                    DualLog.warn(TAG, "No backup file available")
                    return null
                }

                DualLog.info(TAG, "Attempting to restore from backup")
                val json = BACKUP_FILE.readText()
                val config = JsonConfig.parse(json)

                // Restore was successful, save it as the main config
                saveConfig(config)
                DualLog.info(TAG, "Config restored from backup")
                config
            }
            .getOrElse { e ->
                DualLog.error(TAG, "Failed to restore from backup: ${e.message}")
                null
            }
    }

    /**
     * Checks if the config directory and file exist.
     *
     * @return true if config file exists
     */
    fun configExists(): Boolean = CONFIG_FILE.exists()

    /**
     * Gets the path to the config directory.
     *
     * @return Absolute path to config directory
     */
    fun getConfigDir(): String = CONFIG_DIR.absolutePath
}
