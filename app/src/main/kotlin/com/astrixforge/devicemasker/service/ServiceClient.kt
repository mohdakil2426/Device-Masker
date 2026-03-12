package com.astrixforge.devicemasker.service

import android.content.Context
import android.net.Uri
import android.os.IBinder
import com.astrixforge.devicemasker.IDeviceMaskerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Client for communicating with DeviceMaskerService running in system_server.
 *
 * This client connects to the service via ContentProvider (ServiceBridge) and provides a simplified
 * API for UI components to use. It handles connection management, retry logic, and exposes
 * connection state as a StateFlow.
 *
 * Usage:
 * ```kotlin
 * val client = ServiceClient(context)
 * client.connect()
 *
 * if (client.isConnected.value) {
 *     client.writeConfig(jsonConfig)
 *     val stats = client.getHookedAppCount()
 * }
 *
 * client.disconnect()
 * ```
 *
 * @param context Application context for ContentResolver access
 */
class ServiceClient(private val context: Context) {

    companion object {
        private const val TAG = "ServiceClient"

        /** ContentProvider authority (must match ServiceBridge) */
        private const val AUTHORITY = "com.astrixforge.devicemasker.service"

        /** URI for accessing the ServiceBridge */
        private val CONTENT_URI = Uri.parse("content://$AUTHORITY")

        /** Method names matching ServiceBridge */
        private const val METHOD_GET_BINDER = "getBinder"
        private const val METHOD_PING = "ping"

        /** Bundle keys matching ServiceBridge */
        private const val KEY_BINDER = "binder"
        private const val KEY_ALIVE = "alive"

        /** Retry configuration */
        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 500L
    }

    // ═══════════════════════════════════════════════════════════
    // CONNECTION STATE
    // ═══════════════════════════════════════════════════════════

    /** Current service reference (null if disconnected) */
    @Volatile private var service: IDeviceMaskerService? = null

    /** Connection state as observable flow */
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /** Convenience property for checking connection */
    val isConnected: Boolean
        get() = _connectionState.value == ConnectionState.CONNECTED && service != null

    // ═══════════════════════════════════════════════════════════
    // CONNECTION MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Connects to the DeviceMaskerService via ContentProvider.
     *
     * This method fetches the service binder from the ServiceBridge ContentProvider and converts it
     * to the AIDL interface. It includes retry logic with exponential backoff for robustness.
     *
     * @return true if connection successful, false otherwise
     */
    suspend fun connect(): Boolean =
        withContext(Dispatchers.IO) {
            if (isConnected) {
                Timber.d("Already connected")
                return@withContext true
            }

            _connectionState.value = ConnectionState.CONNECTING

            var retryDelay = INITIAL_RETRY_DELAY_MS

            repeat(MAX_RETRIES) { attempt ->
                runCatching {
                        val binder = getBinder()
                        if (binder != null) {
                            service = IDeviceMaskerService.Stub.asInterface(binder)

                            // Verify connection by pinging the service
                            if (service?.isServiceAlive == true) {
                                _connectionState.value = ConnectionState.CONNECTED
                                Timber.i(
                                    "Connected to DeviceMaskerService (v${service?.serviceVersion})"
                                )
                                return@withContext true
                            }
                        }
                    }
                    .onFailure { e ->
                        Timber.w("Connection attempt ${attempt + 1} failed: ${e.message}")
                    }

                if (attempt < MAX_RETRIES - 1) {
                    delay(retryDelay)
                    retryDelay *= 2 // Exponential backoff
                }
            }

            _connectionState.value = ConnectionState.ERROR
            Timber.e("Failed to connect after $MAX_RETRIES attempts")
            return@withContext false
        }

    /** Disconnects from the service. */
    fun disconnect() {
        service = null
        _connectionState.value = ConnectionState.DISCONNECTED
        Timber.d("Disconnected from DeviceMaskerService")
    }

    /**
     * Pings the service to check if it's still alive.
     *
     * @return true if service responds, false otherwise
     */
    suspend fun ping(): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                    val bundle = context.contentResolver.call(CONTENT_URI, METHOD_PING, null, null)
                    bundle?.getBoolean(KEY_ALIVE, false) ?: false
                }
                .getOrElse { false }
        }

    // ═══════════════════════════════════════════════════════════
    // CONFIGURATION OPERATIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Writes complete configuration to the service.
     *
     * @param json JSON string of the configuration
     * @return true if successful, false otherwise
     */
    suspend fun writeConfig(json: String): Boolean =
        withContext(Dispatchers.IO) {
            ensureConnected {
                service?.writeConfig(json)
                Timber.d("Config written to service")
                true
            } ?: false
        }

    /**
     * Reads current configuration from the service.
     *
     * @return Configuration JSON string, or null if failed
     */
    suspend fun readConfig(): String? =
        withContext(Dispatchers.IO) { ensureConnected { service?.readConfig() } }

    /** Forces the service to reload configuration from disk. */
    suspend fun reloadConfig(): Boolean =
        withContext(Dispatchers.IO) {
            ensureConnected {
                service?.reloadConfig()
                true
            } ?: false
        }

    // ═══════════════════════════════════════════════════════════
    // QUERY OPERATIONS
    // ═══════════════════════════════════════════════════════════

    /** Checks if the module is globally enabled. */
    suspend fun isModuleEnabled(): Boolean =
        withContext(Dispatchers.IO) {
            ensureConnected { service?.isModuleEnabled ?: false } ?: false
        }

    /** Checks if spoofing is enabled for a specific app. */
    suspend fun isAppEnabled(packageName: String): Boolean =
        withContext(Dispatchers.IO) {
            ensureConnected { service?.isAppEnabled(packageName) ?: false } ?: false
        }

    /** Gets a spoof value for an app. */
    suspend fun getSpoofValue(packageName: String, key: String): String? =
        withContext(Dispatchers.IO) { ensureConnected { service?.getSpoofValue(packageName, key) } }

    // ═══════════════════════════════════════════════════════════
    // STATISTICS OPERATIONS
    // ═══════════════════════════════════════════════════════════

    /** Gets the filter count for a specific app. */
    suspend fun getFilterCount(packageName: String): Int =
        withContext(Dispatchers.IO) {
            ensureConnected { service?.getFilterCount(packageName) ?: 0 } ?: 0
        }

    /** Gets the total number of hooked apps. */
    suspend fun getHookedAppCount(): Int =
        withContext(Dispatchers.IO) { ensureConnected { service?.hookedAppCount ?: 0 } ?: 0 }

    /** Gets service uptime in milliseconds. */
    suspend fun getServiceUptime(): Long =
        withContext(Dispatchers.IO) { ensureConnected { service?.serviceUptime ?: 0L } ?: 0L }

    /** Gets service version string. */
    suspend fun getServiceVersion(): String? =
        withContext(Dispatchers.IO) { ensureConnected { service?.serviceVersion } }

    // ═══════════════════════════════════════════════════════════
    // LOGGING OPERATIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets recent log entries from the service.
     *
     * @param maxCount Maximum number of entries to return
     * @return List of formatted log entries
     */
    suspend fun getLogs(maxCount: Int = 100): List<String> =
        withContext(Dispatchers.IO) {
            ensureConnected { service?.getLogs(maxCount) ?: emptyList() } ?: emptyList()
        }

    /** Clears all log entries in the service. */
    suspend fun clearLogs(): Boolean =
        withContext(Dispatchers.IO) {
            ensureConnected {
                service?.clearLogs()
                true
            } ?: false
        }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ═══════════════════════════════════════════════════════════

    /** Gets the service binder from ContentProvider. */
    private fun getBinder(): IBinder? {
        return runCatching {
                val bundle =
                    context.contentResolver.call(CONTENT_URI, METHOD_GET_BINDER, null, null)
                bundle?.getBinder(KEY_BINDER)
            }
            .getOrNull()
    }

    /**
     * Ensures we're connected before executing an operation.
     *
     * If not connected, attempts to reconnect once.
     */
    private inline fun <T> ensureConnected(block: () -> T): T? {
        if (!isConnected) {
            // Try to reconnect synchronously (caller should already be on IO dispatcher)
            runCatching {
                val binder = getBinder()
                if (binder != null) {
                    service = IDeviceMaskerService.Stub.asInterface(binder)
                    if (service?.isServiceAlive == true) {
                        _connectionState.value = ConnectionState.CONNECTED
                    }
                }
            }
        }

        return if (isConnected) {
            runCatching { block() }.getOrNull()
        } else {
            null
        }
    }

    /** Connection state enum for UI observation. */
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR,
    }
}
