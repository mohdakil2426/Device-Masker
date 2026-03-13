package com.astrixforge.devicemasker.xposed.hooker

import android.util.Log
import com.astrixforge.devicemasker.xposed.DualLog
import com.astrixforge.devicemasker.xposed.service.DeviceMaskerService
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback

/**
 * System Framework Hooker — Initializes the DeviceMaskerService (diagnostics AIDL) in
 * system_server.
 *
 * This hooker is called via [XposedEntry.onSystemServerLoaded] when LSPosed scope includes
 * "android" (System Framework). It hooks into the system boot process to initialize the
 * diagnostics-only AIDL service before any app processes start.
 *
 * ## Hook Strategy
 * Hooks both AMS.systemReady() and SystemServer.run() — whichever fires first initializes the
 * service. A @Volatile `initialized` flag prevents duplicate initializations.
 *
 * ## CRITICAL SAFETY RULES
 * - ALL code MUST be in try-catch. Any uncaught exception here = system_server crash = bootloop.
 * - NEVER throw exceptions.
 * - Log errors but continue gracefully.
 *
 * ## API 100 change
 * - Old: Extends YukiBaseHooker, implements onHook(), uses toClass()/method{}/hook{after{}}
 * - New: Plain object with static hook(cl, xi) factory. @XposedHooker inner classes for callbacks.
 */
object SystemServiceHooker {

    private const val TAG = "SystemServiceHooker"

    @Volatile private var initialized = false

    /**
     * Registers hooks into the system boot sequence. Called from
     * XposedEntry.onSystemServerLoaded().
     *
     * @param cl The system_server ClassLoader
     * @param xi The XposedInterface hook engine
     */
    fun hook(cl: ClassLoader, xi: XposedInterface) {
        DualLog.info(TAG, "Registering system service hooks in: ${android.os.Process.myPid()}")

        // HOOK 1: ActivityManagerService.systemReady() — PRIMARY
        // Fires when the system is fully ready to accept binders
        try {
            val amsClass = cl.loadClass("com.android.server.am.ActivityManagerService")
            // systemReady() has varying number of parameters across Android versions (1-5)
            // Try common signatures; skip if not found
            listOf(
                    emptyArray<Class<*>>(),
                    arrayOf(Runnable::class.java),
                    arrayOf(Runnable::class.java, Any::class.java),
                )
                .forEach { params ->
                    try {
                        val method = amsClass.getDeclaredMethod("systemReady", *params)
                        method.isAccessible = true
                        xi.hook(method, AmsSystemReadyHooker::class.java)
                        DualLog.info(TAG, "AMS.systemReady(${params.size} params) hook registered")
                        return@forEach
                    } catch (_: NoSuchMethodException) {
                        // This signature doesn't exist on this Android version — try next
                    }
                }
        } catch (t: Throwable) {
            // AMS hook unavailable — fall back to SystemServer.run() hook
            Log.w(TAG, "AMS.systemReady() hook unavailable: ${t.message}")
        }

        // HOOK 2: SystemServer.run() — FALLBACK
        // Fires when the system server process starts its main loop
        try {
            val ssClass = cl.loadClass("com.android.server.SystemServer")
            val runMethod = ssClass.getDeclaredMethod("run").also { it.isAccessible = true }
            xi.hook(runMethod, SystemServerRunHooker::class.java)
            DualLog.info(TAG, "SystemServer.run() hook registered")
        } catch (t: Throwable) {
            Log.w(TAG, "SystemServer.run() hook unavailable: ${t.message}")
        }
    }

    /** Initializes the diagnostics service, guarded by double-check locking. */
    internal fun initializeServiceSafely(source: String) {
        if (initialized) {
            DualLog.debug(TAG, "Already initialized (triggered again by $source)")
            return
        }
        synchronized(SystemServiceHooker) {
            if (initialized) return
            try {
                DualLog.info(TAG, "Initializing DeviceMaskerService from $source...")
                val service = DeviceMaskerService.getInstance()
                
                // Register with ServiceManager for discovery by hooked app processes.
                // Note: We use the "user." prefix to avoid conflict with official system services.
                runCatching {
                    val smClass = Class.forName("android.os.ServiceManager")
                    val addServiceMethod = smClass.getDeclaredMethod("addService", String::class.java, android.os.IBinder::class.java)
                    addServiceMethod.invoke(null, "user.devicemasker_diag", service)
                    DualLog.info(TAG, "Service registered with ServiceManager as 'user.devicemasker_diag'")
                }.onFailure { e ->
                    DualLog.warn(TAG, "Failed to register with ServiceManager (hooks won't report logs): ${e.message}")
                }

                if (service.isAlive) {
                    initialized = true
                    DualLog.info(
                        TAG,
                        "DeviceMaskerService initialized (v${DeviceMaskerService.VERSION})",
                    )
                } else {
                    DualLog.error(TAG, "Service created but isAlive=false")
                }
            } catch (t: Throwable) {
                // NOTE: Never rethrow — this is system_server context
                DualLog.error(TAG, "Service init failed from $source: ${t.message}")
            }
        }
    }

    fun isServiceInitialized(): Boolean = initialized

    // ─────────────────────────────────────────────────────────────
    // @XposedHooker inner classes — static, annotation-driven
    // ─────────────────────────────────────────────────────────────

    class AmsSystemReadyHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    initializeServiceSafely("AMS.systemReady")
                } catch (t: Throwable) {
                    Log.e(TAG, "AmsSystemReadyHooker.after() crashed: ${t.message}")
                }
            }
        }
    }

    class SystemServerRunHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    initializeServiceSafely("SystemServer.run")
                } catch (t: Throwable) {
                    Log.e(TAG, "SystemServerRunHooker.after() crashed: ${t.message}")
                }
            }
        }
    }
}
