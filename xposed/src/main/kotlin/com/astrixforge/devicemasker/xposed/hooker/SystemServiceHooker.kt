package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.xposed.DualLog
import com.astrixforge.devicemasker.xposed.service.DeviceMaskerService
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method

/**
 * System Framework Hooker - Initializes DeviceMaskerService in system_server.
 *
 * This hooker is loaded via `loadSystem { }` when LSPosed scope includes "android"
 * (System Framework). It hooks into the system boot process to initialize our
 * centralized AIDL service before any apps start.
 *
 * Hook Strategy:
 * - Hooks both AMS.systemReady() and SystemServer.run()
 * - Whichever fires first initializes the service
 * - Double-check locking prevents duplicate initialization
 *
 * CRITICAL SAFETY RULES:
 * - ALL code MUST be wrapped in try-catch (crashes = bootloop!)
 * - NEVER throw exceptions
 * - Log errors but continue gracefully
 *
 * @see DeviceMaskerService
 */
object SystemServiceHooker : YukiBaseHooker() {

    private const val TAG = "SystemServiceHooker"

    /** Flag to prevent multiple initializations */
    @Volatile
    private var initialized = false

    override fun onHook() {
        DualLog.info(TAG, "SystemServiceHooker.onHook() called in process: $processName")

        // ═══════════════════════════════════════════════════════════
        // HOOK 1: ActivityManagerService.systemReady() (PRIMARY)
        // ═══════════════════════════════════════════════════════════
        runCatching {
            "com.android.server.am.ActivityManagerService".toClass().apply {
                method {
                    name = "systemReady"
                    paramCount(1..5)
                }.hook {
                    after { initializeServiceSafely("AMS.systemReady") }
                }
            }
            DualLog.info(TAG, "AMS.systemReady() hook registered")
        }.onFailure { e ->
            DualLog.error(TAG, "AMS hook failed: ${e.message}")
        }

        // ═══════════════════════════════════════════════════════════
        // HOOK 2: SystemServer.run() (FALLBACK)
        // ═══════════════════════════════════════════════════════════
        runCatching {
            "com.android.server.SystemServer".toClass().apply {
                method {
                    name = "run"
                    emptyParam()
                }.hook {
                    after { initializeServiceSafely("SystemServer.run") }
                }
            }
            DualLog.info(TAG, "SystemServer.run() hook registered")
        }.onFailure { e ->
            DualLog.error(TAG, "SystemServer hook failed: ${e.message}")
        }
    }

    /**
     * Safely initializes the DeviceMaskerService.
     *
     * Multiple calls are safe due to the initialized flag check.
     *
     * @param source The hook point that triggered this call
     */
    private fun initializeServiceSafely(source: String) {
        if (initialized) {
            DualLog.debug(TAG, "Already initialized (triggered by $source)")
            return
        }

        synchronized(this) {
            if (initialized) return

            runCatching {
                DualLog.info(TAG, "Initializing DeviceMaskerService from $source...")
                val service = DeviceMaskerService.getInstance()

                if (service.isServiceAlive) {
                    initialized = true
                    DualLog.info(TAG, "Service initialized! (v${service.serviceVersion})")
                } else {
                    DualLog.error(TAG, "Service created but not responding")
                }
            }.onFailure { e ->
                DualLog.error(TAG, "Service init failed: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /** Checks if the service has been initialized. */
    fun isServiceInitialized(): Boolean = initialized
}



