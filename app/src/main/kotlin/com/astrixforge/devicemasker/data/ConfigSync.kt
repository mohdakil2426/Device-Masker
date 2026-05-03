package com.astrixforge.devicemasker.data

import android.content.Context
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
 * ## Sync Flow (libxposed API 101):
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

    data class Snapshot(
        val booleans: Map<String, Boolean>,
        val strings: Map<String, String>,
        val stringSets: Map<String, Set<String>>,
        val longs: Map<String, Long>,
        val removeKeys: Set<String>,
        val currentApps: Set<String>,
        val removedApps: Set<String>,
    )

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
        val previousEnabledApps =
            prefs.getStringSet(SharedPrefsKeys.KEY_ENABLED_APPS, emptySet()) ?: emptySet()
        val snapshot = buildSnapshot(config, previousEnabledApps)

        val committed =
            prefs
                .edit()
                .apply {
                    snapshot.removeKeys.forEach { remove(it) }
                    snapshot.booleans.forEach { (key, value) -> putBoolean(key, value) }
                    snapshot.strings.forEach { (key, value) -> putString(key, value) }
                    snapshot.stringSets.forEach { (key, value) -> putStringSet(key, value) }
                    snapshot.longs.forEach { (key, value) -> putLong(key, value) }
                }
                .commit()
        if (!committed) {
            Timber.tag(TAG).w("RemotePreferences commit failed during full sync")
            return
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
            prefs.edit().putBoolean(SharedPrefsKeys.getAppEnabledKey(packageName), false).commit()
            Timber.tag(TAG).d("App $packageName removed from spoofing (no group)")
            return
        }

        val configApp = config.getAppConfig(packageName)
        val appEnabled = config.isModuleEnabled && configApp?.isEnabled == true && group.isEnabled
        val committed =
            prefs
                .edit()
                .apply {
                    putBoolean(SharedPrefsKeys.getAppEnabledKey(packageName), appEnabled)

                    for (type in SpoofType.entries) {
                        val typeEnabled = appEnabled && group.isTypeEnabled(type)
                        val value =
                            if (typeEnabled) group.getValue(type)?.takeIf { it.isNotBlank() }
                            else null

                        putBoolean(
                            SharedPrefsKeys.getSpoofEnabledKey(packageName, type),
                            typeEnabled && value != null,
                        )

                        if (value != null) {
                            putString(SharedPrefsKeys.getSpoofValueKey(packageName, type), value)
                        } else {
                            remove(SharedPrefsKeys.getSpoofValueKey(packageName, type))
                        }
                    }
                }
                .commit()
        if (!committed) {
            Timber.tag(TAG).w("RemotePreferences commit failed during syncApp for $packageName")
            return
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

        val committed =
            prefs
                .edit()
                .apply {
                    remove(SharedPrefsKeys.getAppEnabledKey(packageName))

                    for (type in SpoofType.entries) {
                        remove(SharedPrefsKeys.getSpoofEnabledKey(packageName, type))
                        remove(SharedPrefsKeys.getSpoofValueKey(packageName, type))
                    }
                }
                .commit()
        if (!committed) {
            Timber.tag(TAG).w("RemotePreferences commit failed during clearApp for $packageName")
            return
        }
        Timber.tag(TAG).d("App $packageName cleared from RemotePreferences")
    }

    fun buildSnapshot(config: JsonConfig, previousEnabledApps: Set<String>): Snapshot {
        val booleans = linkedMapOf<String, Boolean>()
        val strings = linkedMapOf<String, String>()
        val stringSets = linkedMapOf<String, Set<String>>()
        val longs = linkedMapOf<String, Long>()
        val removeKeys = linkedSetOf<String>()
        val currentApps = config.appConfigs.keys.toSortedSet()
        val removedApps = previousEnabledApps - currentApps

        booleans[SharedPrefsKeys.KEY_MODULE_ENABLED] = config.isModuleEnabled
        stringSets[SharedPrefsKeys.KEY_ENABLED_APPS] = currentApps
        longs[SharedPrefsKeys.KEY_CONFIG_VERSION] = System.currentTimeMillis()

        for (packageName in removedApps) {
            removeKeys += keysForPackage(packageName)
        }

        for ((packageName, appConfig) in config.appConfigs.toSortedMap()) {
            val group = appConfig.groupId?.let(config::getGroup) ?: config.getDefaultGroup()
            val appEnabled =
                config.isModuleEnabled && appConfig.isEnabled && group?.isEnabled == true
            booleans[SharedPrefsKeys.getAppEnabledKey(packageName)] = appEnabled

            for (type in SpoofType.entries) {
                val typeEnabled = appEnabled && group.isTypeEnabled(type)
                val value =
                    if (typeEnabled) group.getValue(type)?.takeIf { it.isNotBlank() } else null
                booleans[SharedPrefsKeys.getSpoofEnabledKey(packageName, type)] =
                    typeEnabled && value != null

                val valueKey = SharedPrefsKeys.getSpoofValueKey(packageName, type)
                if (value != null) {
                    strings[valueKey] = value
                } else {
                    removeKeys += valueKey
                }
            }
        }

        return Snapshot(
            booleans = booleans,
            strings = strings,
            stringSets = stringSets,
            longs = longs,
            removeKeys = removeKeys,
            currentApps = currentApps,
            removedApps = removedApps,
        )
    }

    private fun keysForPackage(packageName: String): Set<String> {
        return buildSet {
            add(SharedPrefsKeys.getAppEnabledKey(packageName))
            for (type in SpoofType.entries) {
                add(SharedPrefsKeys.getSpoofEnabledKey(packageName, type))
                add(SharedPrefsKeys.getSpoofValueKey(packageName, type))
            }
            add(SharedPrefsKeys.getPersonaBlobKey(packageName))
            add(SharedPrefsKeys.getPersonaVersionKey(packageName))
        }
    }
}
