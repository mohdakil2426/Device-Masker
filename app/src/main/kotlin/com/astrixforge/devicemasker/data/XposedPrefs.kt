package com.astrixforge.devicemasker.data

import android.content.SharedPreferences
import android.util.Log
import com.astrixforge.devicemasker.common.SharedPrefsKeys
import com.astrixforge.devicemasker.common.SpoofType
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

/**
 * Xposed Preferences Writer — libxposed API 100 / XposedService edition.
 *
 * Writes to [SharedPreferences] obtained from [XposedService.getRemotePreferences]. LSPosed
 * delivers these writes live to the hooked process via `getRemotePreferences()` in
 * [com.astrixforge.devicemasker.xposed.XposedEntry] — **no app restart required**.
 *
 * **Architecture contract:**
 * - [XposedService] is obtained asynchronously via [XposedServiceHelper.registerListener]. Writes
 *   are silently dropped if the service is not yet connected (module not active).
 * - All key generation DELEGATES to [SharedPrefsKeys] in `:common` — same keys both sides.
 * - The hook side reads via `xi.getRemotePreferences(PREFS_GROUP)`.
 *
 * **Usage:**
 * 1. Call [XposedPrefs.init] once in `DeviceMaskerApp.onCreate()`.
 * 2. Write config via [ConfigSync] (which calls [getPrefs] internally). Writes no-op gracefully if
 *    the module is not active.
 */
object XposedPrefs {

    private const val TAG = "XposedPrefs"

    /**
     * Preference group name — **must match**
     * [com.astrixforge.devicemasker.xposed.XposedEntry.PREFS_GROUP]. This string identifies the
     * SharedPreferences file/bucket on the LSPosed side.
     */
    const val PREFS_GROUP = "device_masker_config"

    @Volatile private var xposedService: XposedService? = null

    /**
     * Registers the [XposedServiceHelper] listener.
     *
     * Must be called once in `DeviceMaskerApp.onCreate()`. Safe to call multiple times.
     */
    fun init() {
        XposedServiceHelper.registerListener(
            object : XposedServiceHelper.OnServiceListener {
                override fun onServiceBind(service: XposedService) {
                    xposedService = service
                    Log.i(TAG, "XposedService connected (${service.frameworkName})")
                }

                override fun onServiceDied(service: XposedService) {
                    xposedService = null
                    Log.w(TAG, "XposedService died")
                }
            }
        )
    }

    /**
     * Returns the [SharedPreferences] instance backed by [XposedService.getRemotePreferences].
     *
     * Returns `null` if the module is not active (LSPosed not running, or module disabled). Callers
     * should silently skip writes when `null` is returned.
     */
    fun getPrefs(): SharedPreferences? {
        return runCatching { xposedService?.getRemotePreferences(PREFS_GROUP) }
            .onFailure { e ->
                Log.w(TAG, "getRemotePreferences failed (module inactive?): ${e.message}")
            }
            .getOrNull()
    }

    // ═══════════════════════════════════════════════════════════
    // KEY GENERATORS — Delegate to SharedPrefsKeys (:common)
    // Never define keys here; SharedPrefsKeys is the single source of truth.
    // ═══════════════════════════════════════════════════════════

    fun getAppEnabledKey(packageName: String): String =
        SharedPrefsKeys.getAppEnabledKey(packageName)

    fun getSpoofEnabledKey(packageName: String, type: SpoofType): String =
        SharedPrefsKeys.getSpoofEnabledKey(packageName, type)

    fun getSpoofValueKey(packageName: String, type: SpoofType): String =
        SharedPrefsKeys.getSpoofValueKey(packageName, type)

    // ═══════════════════════════════════════════════════════════
    // GLOBAL SETTINGS
    // ═══════════════════════════════════════════════════════════

    /** Master enable switch. Default: `true`. */
    fun isModuleEnabled(): Boolean =
        getPrefs()?.getBoolean(SharedPrefsKeys.KEY_MODULE_ENABLED, true) ?: true

    fun setModuleEnabled(enabled: Boolean) {
        getPrefs()?.edit()?.putBoolean(SharedPrefsKeys.KEY_MODULE_ENABLED, enabled)?.apply()
    }

    /** Debug logging flag. Default: `false`. */
    fun isDebugEnabled(): Boolean =
        getPrefs()?.getBoolean(SharedPrefsKeys.KEY_DEBUG_ENABLED, false) ?: false

    fun setDebugEnabled(enabled: Boolean) {
        getPrefs()?.edit()?.putBoolean(SharedPrefsKeys.KEY_DEBUG_ENABLED, enabled)?.apply()
    }

    // ═══════════════════════════════════════════════════════════
    // PER-APP SETTINGS
    // ═══════════════════════════════════════════════════════════

    fun setAppEnabled(packageName: String, enabled: Boolean) {
        getPrefs()?.edit()?.putBoolean(getAppEnabledKey(packageName), enabled)?.apply()
    }

    fun isAppEnabled(packageName: String): Boolean =
        getPrefs()?.getBoolean(getAppEnabledKey(packageName), false) ?: false

    fun setSpoofTypeEnabled(packageName: String, type: SpoofType, enabled: Boolean) {
        getPrefs()?.edit()?.putBoolean(getSpoofEnabledKey(packageName, type), enabled)?.apply()
    }

    fun isSpoofTypeEnabled(packageName: String, type: SpoofType): Boolean =
        getPrefs()?.getBoolean(getSpoofEnabledKey(packageName, type), false) ?: false

    fun setSpoofValue(packageName: String, type: SpoofType, value: String?) {
        val editor = getPrefs()?.edit() ?: return
        if (value != null) {
            editor.putString(getSpoofValueKey(packageName, type), value)
        } else {
            editor.remove(getSpoofValueKey(packageName, type))
        }
        editor.apply()
    }

    fun getSpoofValue(packageName: String, type: SpoofType): String? =
        getPrefs()?.getString(getSpoofValueKey(packageName, type), null)
}
