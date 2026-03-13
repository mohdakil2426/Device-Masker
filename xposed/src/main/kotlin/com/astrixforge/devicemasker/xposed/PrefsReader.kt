package com.astrixforge.devicemasker.xposed

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.SharedPrefsKeys
import com.astrixforge.devicemasker.common.SpoofType

/**
 * Thread-safe preference reading for libxposed API 100 RemotePreferences.
 *
 * ## What changed from the YukiHookAPI (XSharedPreferences) version
 *
 * Old: Took `YukiHookPrefsBridge` from `prefs` property in YukiHookAPI's `PackageParam`. New: Takes
 * a standard `SharedPreferences` obtained via `XposedInterface.getRemotePreferences(PREFS_GROUP)`.
 *
 * Key advantages of RemotePreferences over XSharedPreferences:
 * - **Live**: reflects the latest values written by the UI app — no target app restart needed
 * - **Safe**: read-only in hooked processes (enforced by libxposed)
 * - **No reload()**: XSharedPreferences required manual reload() calls; RemotePreferences does not
 *
 * ## Key format (unchanged)
 *
 * The `SharedPrefsKeys` class in `:common` remains the single source of truth for all key names. No
 * key format changes are needed — only the delivery mechanism changed.
 *
 * ## Thread safety
 *
 * RemotePreferences returned by libxposed are backed by a snapshot that is safe to read on any
 * thread. Each read reflects the latest committed values from the module app process.
 */
object PrefsHelper {

    /**
     * Gets the spoof value for a type, or invokes the fallback generator if not configured.
     *
     * Check cascade: spoof type enabled? → get value → non-blank? → return value ↓ ↓ fallback()
     * fallback()
     *
     * @param prefs RemotePreferences from XposedEntry.getRemotePreferences(PREFS_GROUP)
     * @param packageName Target app package name (the app being hooked)
     * @param type The SpoofType to look up
     * @param fallback Called if the type is disabled or no value is stored
     * @return The configured spoof value, or fallback() if not configured
     */
    fun getSpoofValue(
        prefs: SharedPreferences,
        packageName: String,
        type: SpoofType,
        fallback: () -> String,
    ): String {
        // If this spoof type is not enabled for this package, return real value
        val typeEnabled =
            prefs.getBoolean(SharedPrefsKeys.getSpoofEnabledKey(packageName, type), false)
        if (!typeEnabled) return fallback()

        // Return the stored spoof value, or generate fallback if not set
        val stored = prefs.getString(SharedPrefsKeys.getSpoofValueKey(packageName, type), null)
        return stored?.takeIf { it.isNotBlank() } ?: fallback()
    }

    /**
     * Checks if a specific spoof type is explicitly enabled for a package.
     *
     * Returns false if the key is absent (spoofing types default to disabled — opt-in model).
     *
     * @param prefs RemotePreferences from XposedEntry.getRemotePreferences()
     * @param packageName Target app package name
     * @param type The SpoofType to check
     */
    fun isSpoofTypeEnabled(
        prefs: SharedPreferences,
        packageName: String,
        type: SpoofType,
    ): Boolean = prefs.getBoolean(SharedPrefsKeys.getSpoofEnabledKey(packageName, type), false)
}
