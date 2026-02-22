package com.astrixforge.devicemasker

import com.astrixforge.devicemasker.service.ConfigManager
import com.astrixforge.devicemasker.service.ServiceClient
import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication
import timber.log.Timber

/**
 * Device Masker Application class.
 *
 * Extends [ModuleApplication] from YukiHookAPI to properly initialize the module in both the module
 * app process and hooked app processes.
 *
 * Architecture:
 * - ConfigManager: JSON-based local configuration storage
 * - ServiceClient: AIDL client for communicating with system_server service
 *
 * Responsibilities:
 * - Initialize Timber logging in debug builds
 * - Initialize ConfigManager for local configuration management
 * - Provide ServiceClient for AIDL service communication
 * - Provide module status information via YukiHookAPI
 */
class DeviceMaskerApp : ModuleApplication() {

    /** ServiceClient for AIDL communication with system_server */
    private lateinit var _serviceClient: ServiceClient

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize Timber logging for debug builds
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Device Masker Application initialized")
        }

        // Initialize ConfigManager (local JSON config storage)
        ConfigManager.init(this)
        Timber.d("ConfigManager initialized")

        // Initialize ServiceClient (AIDL communication with system_server)
        _serviceClient = ServiceClient(this)
        Timber.d("ServiceClient initialized")

        // Log module activation status
        Timber.i(
            "Device Masker Module Status: ${if (isXposedModuleActive) "Active" else "Inactive"}"
        )
    }

    companion object {
        @Volatile
        private var instance: DeviceMaskerApp? = null

        /**
         * Get the application instance.
         *
         * @throws IllegalStateException if called before onCreate()
         */
        fun getInstance(): DeviceMaskerApp {
            return instance ?: throw IllegalStateException(
                "DeviceMaskerApp not initialized. Ensure Application.onCreate() has been called."
            )
        }

        /**
         * Get the global ServiceClient instance.
         *
         * This client is used for AIDL communication with the DeviceMaskerService
         * running in system_server. ViewModels should use this for:
         * - Writing/reading configuration
         * - Getting hook statistics
         * - Viewing centralized logs
         */
        val serviceClient: ServiceClient
            get() = getInstance()._serviceClient

        /**
         * Check if the Xposed module is currently active.
         * This is set by YukiHookAPI when the module is properly loaded.
         */
        val isXposedModuleActive: Boolean
            get() = com.highcapable.yukihookapi.YukiHookAPI.Status.isModuleActive
    }
}

