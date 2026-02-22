// IDeviceMaskerService.aidl
package com.astrixforge.devicemasker;

/**
 * AIDL interface for Device Masker service running in system_server.
 * Provides real-time configuration updates and centralized logging.
 *
 * This service is registered at boot time by SystemServiceHooker and
 * exposed via ServiceBridge ContentProvider for UI app communication.
 */
interface IDeviceMaskerService {

    // ==================== Configuration ====================

    /**
     * Write complete configuration as JSON string.
     * The service will parse, validate, and persist the config.
     * @param json JSON representation of JsonConfig
     */
    void writeConfig(in String json);

    /**
     * Read current configuration as JSON string.
     * @return JSON representation of current JsonConfig
     */
    String readConfig();

    /**
     * Force reload configuration from disk.
     * Useful after external modifications or for recovery.
     */
    void reloadConfig();

    // ==================== Query Methods ====================

    /**
     * Check if the module is globally enabled.
     * @return true if module is enabled in settings
     */
    boolean isModuleEnabled();

    /**
     * Check if spoofing is enabled for a specific app.
     * @param packageName target app package name
     * @return true if app has spoofing enabled
     */
    boolean isAppEnabled(in String packageName);

    /**
     * Get a specific spoof value for an app.
     * @param packageName target app package name
     * @param key spoof type key (e.g., "IMEI", "MAC_ADDRESS")
     * @return spoofed value, or null if not configured
     */
    String getSpoofValue(in String packageName, in String key);

    // ==================== Statistics ====================

    /**
     * Increment the filter count for an app.
     * Called by hookers when a value is successfully spoofed.
     * @param packageName target app package name
     */
    void incrementFilterCount(in String packageName);

    /**
     * Get the filter count for an app.
     * @param packageName target app package name
     * @return number of times values were spoofed for this app
     */
    int getFilterCount(in String packageName);

    /**
     * Get the total number of apps with active hooks.
     * @return count of hooked apps
     */
    int getHookedAppCount();

    // ==================== Logging ====================

    /**
     * Log a message to the centralized service log.
     * @param tag log tag (typically hooker name)
     * @param message log message
     * @param level log level: 0=INFO, 1=WARN, 2=ERROR, 3=DEBUG
     */
    void log(in String tag, in String message, int level);

    /**
     * Get recent log entries.
     * @param maxCount maximum number of entries to return
     * @return list of formatted log entries
     */
    List<String> getLogs(int maxCount);

    /**
     * Clear all log entries.
     */
    void clearLogs();

    // ==================== Control ====================

    /**
     * Check if the service is alive and responding.
     * @return always true if reachable
     */
    boolean isServiceAlive();

    /**
     * Get the service version string.
     * @return version string (e.g., "1.0.0")
     */
    String getServiceVersion();

    /**
     * Get service uptime in milliseconds.
     * @return milliseconds since service initialization
     */
    long getServiceUptime();
}
