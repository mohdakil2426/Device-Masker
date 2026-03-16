package com.astrixforge.devicemasker.data

import android.content.Context
import androidx.core.content.edit
import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.common.SharedPrefsKeys
import com.astrixforge.devicemasker.common.SpoofType
import timber.log.Timber

/**
 * Syncs [JsonConfig] to [XposedPrefs] / `getRemotePreferences()` for live cross-process delivery.
 *
 * The UI stores configuration as a group-based [JsonConfig] (JSON file). Hooks read flat per-app
 * [SharedPreferences] keys via `getRemotePreferences()`. This object bridges the gap.
 *
 * ## Sync Flow (libxposed API 100):
 * ```
 * UI Change → ConfigManager → JsonConfig → ConfigSync.syncFromConfig()
 *                                                ↓
 *                                         XposedPrefs.getPrefs()?.edit()
 *                                                ↓
 *                                      XposedService.getRemotePreferences()
 *                                                ↓
 *                                         LSPosed (live — no app restart needed)
 *                                                ↓
 *                                         getRemotePreferences() in hooks
 * ```
 *
 * **Null-safety:** If the module is not active (LSPosed not running), [XposedPrefs.getPrefs]
 * returns `null` and all writes are silently no-op'd. Config is still persisted locally via
 * [ConfigManager]'s JSON file and will sync on next module activation.
 *
 * Call [syncFromConfig] whenever the config changes in the UI.
 */
object ConfigSync {

    private const val TAG = "ConfigSync"

    /**
     * Syncs everything from [config] to [XposedPrefs] (RemotePreferences).
     *
     * Context parameter retained for API compatibility, but no longer used for prefs I/O.
     *
     * @param context Unused after migration — retained for call-site compatibility
     * @param config The [JsonConfig] to sync
     */
    fun syncFromConfig(@Suppress("UNUSED_PARAMETER") context: Context, config: JsonConfig) {
        val prefs = XposedPrefs.getPrefs()
        if (prefs == null) {
            Timber.tag(TAG).d("XposedService not connected — config will sync on next activation")
            return
        }

        Timber.tag(TAG).d("Syncing config to RemotePreferences...")

        prefs.edit {
            // Master switch
            putBoolean(SharedPrefsKeys.KEY_MODULE_ENABLED, config.isModuleEnabled)

            // Sync each group's apps
            for (group in config.groups.values) {
                for (packageName in group.assignedApps) {
                    val appEnabled = config.isModuleEnabled && group.isEnabled
                    putBoolean(SharedPrefsKeys.getAppEnabledKey(packageName), appEnabled)

                    // Sync each spoof type for this package
                    for (type in SpoofType.entries) {
                        val typeEnabled = appEnabled && group.isTypeEnabled(type)
                        val value = if (typeEnabled) group.getValue(type) else null

                        putBoolean(SharedPrefsKeys.getSpoofEnabledKey(packageName, type), typeEnabled)

                        if (value != null) {
                            putString(SharedPrefsKeys.getSpoofValueKey(packageName, type), value)
                        } else {
                            remove(SharedPrefsKeys.getSpoofValueKey(packageName, type))
                        }
                    }
                }
            }
        }
        Timber.tag(TAG).d("Config synced to RemotePreferences — live delivery active")
    }

    /**
     * Quick sync for a single package.
     *
     * @param context Unused after migration — retained for call-site compatibility
     * @param config The [JsonConfig] to sync from
     * @param packageName The package to sync
     */
    fun syncApp(
        @Suppress("UNUSED_PARAMETER") context: Context,
        config: JsonConfig,
        packageName: String,
    ) {
        val prefs = XposedPrefs.getPrefs()
        if (prefs == null) {
            Timber.tag(TAG).d("XposedService not connected — skipping syncApp for $packageName")
            return
        }

        val group = config.getGroupForApp(packageName)
        if (group == null) {
            prefs.edit { putBoolean(SharedPrefsKeys.getAppEnabledKey(packageName), false) }
            Timber.tag(TAG).d("App $packageName removed from spoofing (no group)")
            return
        }

        val appEnabled = config.isModuleEnabled && group.isEnabled
        prefs.edit {
            putBoolean(SharedPrefsKeys.getAppEnabledKey(packageName), appEnabled)

            for (type in SpoofType.entries) {
                val typeEnabled = appEnabled && group.isTypeEnabled(type)
                val value = if (typeEnabled) group.getValue(type) else null

                putBoolean(SharedPrefsKeys.getSpoofEnabledKey(packageName, type), typeEnabled)

                if (value != null) {
                    putString(SharedPrefsKeys.getSpoofValueKey(packageName, type), value)
                } else {
                    remove(SharedPrefsKeys.getSpoofValueKey(packageName, type))
                }
            }
        }
        Timber.tag(TAG).d("App $packageName synced to RemotePreferences")
    }

    /**
     * Clears all stored spoof keys for a package from [XposedPrefs].
     *
     * @param context Unused after migration — retained for call-site compatibility
     * @param packageName The package to clear
     */
    fun clearApp(@Suppress("UNUSED_PARAMETER") context: Context, packageName: String) {
        val prefs = XposedPrefs.getPrefs()
        if (prefs == null) {
            Timber.tag(TAG).d("XposedService not connected — skipping clearApp for $packageName")
            return
        }

        prefs.edit {
            remove(SharedPrefsKeys.getAppEnabledKey(packageName))

            for (type in SpoofType.entries) {
                remove(SharedPrefsKeys.getSpoofEnabledKey(packageName, type))
                remove(SharedPrefsKeys.getSpoofValueKey(packageName, type))
            }
        }
        Timber.tag(TAG).d("App $packageName cleared from RemotePreferences")
    }
}
