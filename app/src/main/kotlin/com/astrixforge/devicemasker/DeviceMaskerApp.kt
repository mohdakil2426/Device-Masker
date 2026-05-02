package com.astrixforge.devicemasker

import android.app.Application
import com.astrixforge.devicemasker.data.XposedPrefs
import com.astrixforge.devicemasker.service.ConfigManager
import com.astrixforge.devicemasker.service.ServiceClient
import timber.log.Timber

/**
 * Device Masker Application class — libxposed API 101 edition.
 *
 * Extends standard [Application] (YukiHookAPI's `ModuleApplication` no longer used after libxposed
 * migration). Initialises only what is needed at startup:
 * - **Timber**: debug logging (debug builds only)
 * - **ConfigManager**: local JSON configuration storage
 * - **XposedPrefs.init**: initialises libxposed [ModulePreferences] so `ModulePreferences.from()`
 *   is callable from [com.astrixforge.devicemasker.data.ConfigSync]
 * - **ServiceClient**: AIDL client for the *diagnostics-only* service in `system_server`
 */
class DeviceMaskerApp : Application() {

    /** AIDL client for the diagnostics service (hook event counts, logs, health). */
    private lateinit var _serviceClient: ServiceClient

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Debug logging — release builds strip Timber via R8 -assumenosideeffects
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Device Masker Application initialised")
        }

        // Register XposedService listener — enables getRemotePreferences() write path.
        // Safe to call multiple times (no-op on reconnect).
        XposedPrefs.init()
        Timber.d("XposedPrefs (XposedService) listener registered")

        // Local JSON config — loads existing config.json from filesDir
        ConfigManager.init(this)
        XposedPrefs.addServiceBindCallback { ConfigManager.syncCurrentConfig() }
        Timber.d("ConfigManager initialised")

        // Diagnostics-only AIDL client — non-fatal if service is unavailable
        _serviceClient = ServiceClient(this)
        Timber.d("ServiceClient initialised")

        Timber.i("Device Masker module active: $isXposedModuleActive")
    }

    companion object {
        @Volatile private var instance: DeviceMaskerApp? = null

        /**
         * Returns the application singleton.
         *
         * @throws IllegalStateException if called before [onCreate] completes.
         */
        fun getInstance(): DeviceMaskerApp =
            instance
                ?: throw IllegalStateException(
                    "DeviceMaskerApp not initialised. Has Application.onCreate() run?"
                )

        /**
         * Diagnostics-only [ServiceClient].
         *
         * Post-migration the service only exposes hook event counts, log aggregation, and a
         * health-check. Config delivery is via [XposedPrefs] / [ModulePreferences].
         */
        val serviceClient: ServiceClient
            get() = getInstance()._serviceClient

        /** Whether the app is currently connected to LSPosed's libxposed service. */
        val isXposedModuleActive: Boolean
            get() = XposedPrefs.isConnected()
    }
}
