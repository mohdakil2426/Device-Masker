// IDeviceMaskerService.aidl
package com.astrixforge.devicemasker;

/**
 * AIDL interface for Device Masker diagnostics service (Option B — libxposed API 100).
 *
 * Config delivery has been moved entirely to RemotePreferences (libxposed API 100).
 * This service is now DIAGNOSTICS ONLY — hooks report events to it, and the UI reads
 * aggregated statistics and log entries.
 *
 * Methods removed (replaced by RemotePreferences):
 *   writeConfig, readConfig, reloadConfig,
 *   isModuleEnabled, isAppEnabled, getSpoofValue
 *
 * Hook → service calls MUST be oneway (fire-and-forget, non-blocking, ~5μs).
 * UI → service reads are synchronous (blocking, always called from IO coroutines).
 */
interface IDeviceMaskerService {

    // ═══ REPORTING — called from hook callbacks  ═══
    // ALL must be oneway: hook callbacks must not block waiting for system_server.

    /**
     * Report that a spoof event occurred for a package.
     * Called after each successful hook interception — MUST NOT BLOCK.
     * @param packageName  Target app package name
     * @param spoofType    SpoofType enum name (e.g. "IMEI", "ANDROID_ID")
     */
    oneway void reportSpoofEvent(in String packageName, in String spoofType);

    /**
     * Report a structured log entry from a hook.
     * Called from hook callbacks — MUST NOT BLOCK.
     * @param tag      Log tag (e.g. "DeviceHooker")
     * @param message  Log message
     * @param level    Android log priority: Log.DEBUG=3, Log.INFO=4, Log.WARN=5, Log.ERROR=6
     */
    oneway void reportLog(in String tag, in String message, int level);

    /**
     * Report that hooks were registered for a package this session.
     * Called once per package load from XposedEntry.onPackageLoaded().
     * @param packageName  The loaded package
     */
    oneway void reportPackageHooked(in String packageName);

    // ═══ READS — called by DiagnosticsViewModel on the IO dispatcher ═══

    /**
     * Get the total number of spoof events recorded for a specific package this session.
     * @param packageName  Target app package name
     * @return  Event count, or 0 if no hooks fired yet
     */
    int getSpoofEventCount(in String packageName);

    /**
     * Get all packages that have had hooks registered this session.
     * @return  List of package names
     */
    List<String> getHookedPackages();

    /**
     * Get recent log entries (newest last).
     * @param maxCount  Maximum entries to return (capped at 500 internally)
     * @return  List of formatted log strings
     */
    List<String> getLogs(int maxCount);

    /**
     * Clear all diagnostic data: logs, spoof counts, hooked package set.
     */
    void clearDiagnostics();

    /**
     * Health check — always returns true if the service is reachable.
     * Used by DiagnosticsViewModel to display service connection status.
     */
    boolean isAlive();
}
