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
 * Client for the Device Masker diagnostics service running in `system_server`.
 *
 * **Post-migration scope (Option B):**
 * - Config delivery: **removed** — use [XposedPrefs] / [ModulePreferences] exclusively
 * - Diagnostics reads: `getSpoofEventCount`, `getHookedPackages`, `getLogs`, `clearDiagnostics`
 * - Health check: `isAlive`, `connectionState` [StateFlow]
 *
 * This client is non-fatal: if the service is unavailable, [DiagnosticsViewModel] shows "Service
 * unavailable" and spoofing continues unaffected via RemotePreferences.
 *
 * Connection is established via the [ServiceBridge] ContentProvider which is dynamically registered
 * by [SystemServiceHooker] in `system_server` at boot.
 *
 * @param context Application context for [ContentResolver] access
 */
class ServiceClient(private val context: Context) {

    companion object {
        private const val TAG = "ServiceClient"

        /** ContentProvider authority — must match [ServiceBridge.AUTHORITY]. */
        private const val AUTHORITY = "com.astrixforge.devicemasker.service"

        private val CONTENT_URI = Uri.parse("content://$AUTHORITY")

        private const val METHOD_GET_BINDER = "getBinder"
        private const val METHOD_PING = "ping"
        private const val KEY_BINDER = "binder"
        private const val KEY_ALIVE = "alive"

        private const val MAX_RETRIES = 3
        private const val INITIAL_RETRY_DELAY_MS = 500L
    }

    // ═══════════════════════════════════════════════════════════
    // CONNECTION STATE
    // ═══════════════════════════════════════════════════════════

    @Volatile private var service: IDeviceMaskerService? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val isConnected: Boolean
        get() = _connectionState.value == ConnectionState.CONNECTED && service != null

    // ═══════════════════════════════════════════════════════════
    // CONNECTION MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Connects to the diagnostics service via [ServiceBridge] ContentProvider.
     *
     * Includes up to [MAX_RETRIES] attempts with exponential backoff. Connection is verified by
     * calling [IDeviceMaskerService.isAlive].
     *
     * @return `true` if connection succeeded, `false` otherwise
     */
    suspend fun connect(): Boolean =
        withContext(Dispatchers.IO) {
            if (isConnected) return@withContext true

            _connectionState.value = ConnectionState.CONNECTING
            var retryDelay = INITIAL_RETRY_DELAY_MS

            repeat(MAX_RETRIES) { attempt ->
                runCatching {
                        val binder = getBinder()
                        if (binder != null) {
                            val svc = IDeviceMaskerService.Stub.asInterface(binder)
                            if (svc.isAlive) {
                                service = svc
                                _connectionState.value = ConnectionState.CONNECTED
                                Timber.i(
                                    "[$TAG] Connected to DeviceMaskerService v${DeviceMaskerService.VERSION}"
                                )
                                return@withContext true
                            }
                        }
                    }
                    .onFailure { e ->
                        Timber.w("[$TAG] Connection attempt ${attempt + 1} failed: ${e.message}")
                    }

                if (attempt < MAX_RETRIES - 1) {
                    delay(retryDelay)
                    retryDelay *= 2
                }
            }

            _connectionState.value = ConnectionState.ERROR
            Timber.e(
                "[$TAG] Failed to connect after $MAX_RETRIES attempts — diagnostics unavailable"
            )
            false
        }

    /** Disconnects from the service and resets state. */
    fun disconnect() {
        service = null
        _connectionState.value = ConnectionState.DISCONNECTED
        Timber.d("[$TAG] Disconnected from DeviceMaskerService")
    }

    /**
     * Pings the ContentProvider to verify the service is still reachable.
     *
     * @return `true` if service responds, `false` otherwise
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
    // DIAGNOSTICS READS (no config methods post-migration)
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets the total spoof event count for a specific package this session.
     *
     * @param packageName Target app package name
     * @return Event count, or 0 if not connected / no data
     */
    suspend fun getSpoofEventCount(packageName: String): Int =
        withContext(Dispatchers.IO) {
            ensureConnected { service?.getSpoofEventCount(packageName) ?: 0 } ?: 0
        }

    /**
     * Gets all packages that have had hooks registered this boot session.
     *
     * @return List of package names, or empty if not connected
     */
    suspend fun getHookedPackages(): List<String> =
        withContext(Dispatchers.IO) {
            ensureConnected { service?.hookedPackages ?: emptyList() } ?: emptyList()
        }

    /**
     * Gets recent log entries from the service (newest last).
     *
     * @param maxCount Maximum number of entries to return
     * @return List of formatted log entries
     */
    suspend fun getLogs(maxCount: Int = 100): List<String> =
        withContext(Dispatchers.IO) {
            ensureConnected { service?.getLogs(maxCount) ?: emptyList() } ?: emptyList()
        }

    /**
     * Clears all diagnostic data in the service (logs, counts, hooked packages).
     *
     * @return `true` if successful, `false` if not connected
     */
    suspend fun clearDiagnostics(): Boolean =
        withContext(Dispatchers.IO) {
            ensureConnected {
                service?.clearDiagnostics()
                true
            } ?: false
        }

    /**
     * Health check — verifies the service binder is responding.
     *
     * @return `true` if service responds, `false` otherwise
     */
    suspend fun isAlive(): Boolean =
        withContext(Dispatchers.IO) { ensureConnected { service?.isAlive ?: false } ?: false }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ═══════════════════════════════════════════════════════════

    private fun getBinder(): IBinder? =
        runCatching {
                val bundle =
                    context.contentResolver.call(CONTENT_URI, METHOD_GET_BINDER, null, null)
                bundle?.getBinder(KEY_BINDER)
            }
            .getOrNull()

    /**
     * Ensures we're connected before executing [block]. Attempts a single synchronous reconnect if
     * not connected. Returns `null` if connection fails.
     */
    private inline fun <T> ensureConnected(block: () -> T): T? {
        if (!isConnected) {
            runCatching {
                val binder = getBinder()
                if (binder != null) {
                    val svc = IDeviceMaskerService.Stub.asInterface(binder)
                    if (svc.isAlive) {
                        service = svc
                        _connectionState.value = ConnectionState.CONNECTED
                    }
                }
            }
        }
        return if (isConnected) runCatching { block() }.getOrNull() else null
    }

    /** Observable connection state for UI. */
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR,
    }
}

/** Convenience alias so xposed module version is accessible from app-side. */
private val DeviceMaskerService = com.astrixforge.devicemasker.xposed.service.DeviceMaskerService
