package com.astrixforge.devicemasker.xposed

import android.os.IBinder
import com.astrixforge.devicemasker.common.Constants
import com.astrixforge.devicemasker.common.IDeviceMaskerService
import com.astrixforge.devicemasker.common.SpoofType

/**
 * Service Proxy - Provides cross-process access to DeviceMaskerService for hooks.
 *
 * This object solves the critical issue where hooks running in target app processes
 * cannot access the DeviceMaskerService singleton (which runs in system_server).
 *
 * **Problem:**
 * - DeviceMaskerService.init() is called in system_server only
 * - Each app is a separate process with its own JVM
 * - DeviceMaskerService.instance is always null in hooked apps
 *
 * **Solution:**
 * - This proxy uses Binder IPC to call the service in system_server
 * - Hooks call ServiceProxy.getSpoofValue() instead of accessing the local singleton
 * - The service binder is obtained via ServiceManager.getService() using reflection
 */
object ServiceProxy {

    private const val TAG = "ServiceProxy"

    /** Cached service connection - lazily initialized. */
    @Volatile
    private var cachedService: IDeviceMaskerService? = null

    /** Lock for thread-safe service initialization. */
    private val serviceLock = Any()

    /** Cached ServiceManager class access via reflection. */
    private val serviceManagerClass by lazy {
        runCatching { Class.forName("android.os.ServiceManager") }.getOrNull()
    }

    /** Cached getService method via reflection. */
    private val getServiceMethod by lazy {
        serviceManagerClass?.getMethod("getService", String::class.java)
    }

    /** Cached addService method via reflection. */
    private val addServiceMethod by lazy {
        serviceManagerClass?.getMethod("addService", String::class.java, IBinder::class.java)
    }

    /**
     * Gets a service from ServiceManager via reflection.
     * ServiceManager is a hidden API, so we use reflection to access it.
     */
    private fun getServiceFromManager(name: String): IBinder? {
        return runCatching {
            getServiceMethod?.invoke(null, name) as? IBinder
        }.getOrNull()
    }

    /**
     * Adds a service to ServiceManager via reflection.
     * Only works from system_server context.
     */
    fun addServiceToManager(name: String, binder: IBinder) {
        runCatching {
            addServiceMethod?.invoke(null, name, binder)
            DualLog.info(TAG, "Service registered in ServiceManager: $name")
        }.onFailure { e ->
            DualLog.error(TAG, "Failed to register in ServiceManager: ${e.message}", e)
        }
    }

    /**
     * Gets the DeviceMaskerService via Binder IPC.
     * 
     * The service is registered in system_server's ServiceManager under
     * the name defined in Constants.SERVICE_NAME.
     * 
     * @return The AIDL service interface, or null if unavailable
     */
    fun getService(): IDeviceMaskerService? {
        // Fast path: return cached service if available
        cachedService?.let { 
            // Verify the binder is still alive
            runCatching {
                if (it.asBinder().isBinderAlive) {
                    return it
                }
            }
            // Binder died or error, clear cache
            cachedService = null
        }

        // Slow path: get service from ServiceManager
        synchronized(serviceLock) {
            // Double-check in synchronized block
            cachedService?.let { 
                runCatching {
                    if (it.asBinder().isBinderAlive) return it
                }
            }

            return runCatching {
                val binder: IBinder? = getServiceFromManager(Constants.SERVICE_NAME)
                if (binder == null) {
                    DualLog.debug(TAG, "Service not found in ServiceManager: ${Constants.SERVICE_NAME}")
                    return null
                }

                val service = IDeviceMaskerService.Stub.asInterface(binder)
                cachedService = service
                service
            }.getOrElse { e ->
                DualLog.warn(TAG, "Failed to get service: ${e.message}")
                null
            }
        }
    }

    /**
     * Gets the spoofed value for a specific spoof type and package.
     *
     * This is the main entry point for hooks. It handles:
     * 1. Service lookup via Binder IPC
     * 2. Error handling and fallback
     * 3. Caching the service connection
     *
     * @param packageName The package name of the hooked app
     * @param type The type of spoof value to get
     * @param fallback A fallback value generator if spoofing is not configured
     * @return The spoofed value, or fallback if unavailable
     */
    fun getSpoofValue(packageName: String, type: SpoofType, fallback: () -> String): String {
        return runCatching {
            val service = getService()
            if (service == null) {
                DualLog.debug(TAG, "Service unavailable, using fallback for ${type.name}")
                return fallback()
            }

            // Call the service via AIDL
            val value = service.getSpoofValue(packageName, type.name)
            if (value.isNullOrEmpty()) {
                DualLog.debug(TAG, "No spoof value configured for ${type.name} in $packageName")
                return fallback()
            }

            value
        }.getOrElse { e ->
            DualLog.warn(TAG, "Failed to get spoof value: ${e.message}")
            fallback()
        }
    }

    /**
     * Checks if the service is available and the module is enabled.
     */
    fun isModuleEnabled(): Boolean {
        return runCatching {
            getService()?.isModuleEnabled ?: false
        }.getOrDefault(false)
    }

    /**
     * Increments the hook count for diagnostics.
     */
    fun incrementHookCount() {
        // Note: Hook count tracking is kept local per-hooker for now
        // Could be extended to use IPC if needed
    }

    /**
     * Clears the cached service connection.
     * Call this if the service restarts.
     */
    fun clearCache() {
        synchronized(serviceLock) {
            cachedService = null
        }
    }
}
