package com.astrixforge.devicemasker

import android.app.Application
import com.astrixforge.devicemasker.data.XposedPrefs
import com.astrixforge.devicemasker.service.AppLogStore
import com.astrixforge.devicemasker.service.ConfigManager
import com.astrixforge.devicemasker.service.PersistentAppLogTree
import com.astrixforge.devicemasker.service.ServiceClient
import timber.log.Timber

/**
 * Device Masker Application class — libxposed API 101 edition.
 *
 * Extends standard [Application] (YukiHookAPI's `ModuleApplication` no longer used after libxposed
 * migration). Initialises only what is needed at startup:
 * - **Timber**: debug logging (debug builds only)
 * - **ConfigManager**: local JSON configuration storage
 * - **XposedPrefs.init**: registers the libxposed service listener so RemotePreferences writes are
 *   available to [com.astrixforge.devicemasker.data.ConfigSync]
 * - **ServiceClient**: AIDL client for the *diagnostics-only* service in `system_server`
 */
class DeviceMaskerApp : Application() {

    /** AIDL client for the diagnostics service (hook event counts, logs, health). */
    private lateinit var _serviceClient: ServiceClient
    private lateinit var _appLogStore: AppLogStore

    override fun onCreate() {
        super.onCreate()
        instance = this

        _appLogStore = AppLogStore.from(this)
        Timber.plant(PersistentAppLogTree(_appLogStore))

        // Debug logging — release builds strip DebugTree logcat calls via R8.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("Device Masker Application initialised")

        // Register XposedService listener once — enables getRemotePreferences() write path.
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
         * health-check. Config delivery is via [XposedPrefs] / RemotePreferences.
         */
        val serviceClient: ServiceClient
            get() = getInstance()._serviceClient

        val appLogStore: AppLogStore
            get() = getInstance()._appLogStore

        /** Whether the app is currently connected to LSPosed's libxposed service. */
        val isXposedModuleActive: Boolean
            get() = XposedPrefs.isConnected()
    }
}
