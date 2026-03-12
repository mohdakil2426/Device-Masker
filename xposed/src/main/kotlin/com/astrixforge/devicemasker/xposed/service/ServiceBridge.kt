package com.astrixforge.devicemasker.xposed.service

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/**
 * ContentProvider that acts as a bridge between the UI app and DeviceMaskerService.
 *
 * This ContentProvider is dynamically registered in system_server by SystemServiceHooker after the
 * DeviceMaskerService is initialized. It allows the UI app to discover and connect to the service
 * running in system_server.
 *
 * The UI app can retrieve the service binder by calling:
 * ```kotlin
 * val bundle = contentResolver.call(ServiceBridge.URI, ServiceBridge.METHOD_GET_BINDER, null, null)
 * val binder = bundle?.getBinder(ServiceBridge.KEY_BINDER)
 * val service = IDeviceMaskerService.Stub.asInterface(binder)
 * ```
 *
 * @see DeviceMaskerService
 */
class ServiceBridge : ContentProvider() {

    companion object {
        /** ContentProvider authority */
        const val AUTHORITY = "com.astrixforge.devicemasker.service"

        /** URI for accessing this ContentProvider */
        val URI: Uri = Uri.parse("content://$AUTHORITY")

        /** Method name for getting the service binder */
        const val METHOD_GET_BINDER = "getBinder"

        /** Bundle key for the service binder */
        const val KEY_BINDER = "binder"

        /** Method for checking if service is available */
        const val METHOD_PING = "ping"

        /** Bundle key for ping response */
        const val KEY_ALIVE = "alive"
    }

    /**
     * Called when the ContentProvider is created.
     *
     * Note: This is called in the UI app's process, not system_server. The actual service runs in
     * system_server and this provider just returns the binder to it.
     */
    override fun onCreate(): Boolean {
        return true
    }

    /**
     * Handles call() requests from clients.
     *
     * Supported methods:
     * - METHOD_GET_BINDER: Returns the DeviceMaskerService binder
     * - METHOD_PING: Returns alive status for health check
     *
     * @param method The method name to call
     * @param arg Optional string argument (unused)
     * @param extras Optional Bundle with additional parameters (unused)
     * @return Bundle containing the response, or null if method unknown
     */
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method) {
            METHOD_GET_BINDER -> {
                // Return the service binder
                Bundle().apply { putBinder(KEY_BINDER, DeviceMaskerService.getInstance()) }
            }
            METHOD_PING -> {
                // Health check
                Bundle().apply { putBoolean(KEY_ALIVE, DeviceMaskerService.isInitialized()) }
            }
            else -> null
        }
    }

    // ═══════════════════════════════════════════════════════════
    // REQUIRED CONTENTPROVIDER METHODS (Not Used)
    // ═══════════════════════════════════════════════════════════

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
