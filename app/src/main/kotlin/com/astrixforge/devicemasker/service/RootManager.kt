package com.astrixforge.devicemasker.service

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * RootManager - Handles root access detection and requests.
 *
 * Usage:
 * ```kotlin
 * // Check if root is available
 * val hasRoot = RootManager.isRootAvailable()
 *
 * // Request root access (shows Magisk/SuperSU prompt)
 * val granted = RootManager.requestRootAccess()
 *
 * // Execute command as root
 * val output = RootManager.executeAsRoot("logcat -d")
 * ```
 */
object RootManager {

    private var rootAccessGranted: Boolean? = null

    /**
     * Checks if root binary exists on the device.
     */
    fun isRootAvailable(): Boolean {
        val paths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/su/bin/su"
        )
        return paths.any { java.io.File(it).exists() }
    }

    /**
     * Checks if root access has been granted to this app.
     * Caches the result for performance.
     */
    fun isRootGranted(): Boolean {
        rootAccessGranted?.let { return it }
        
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            process.waitFor()
            
            val granted = output.contains("uid=0")
            rootAccessGranted = granted
            granted
        } catch (e: Exception) {
            rootAccessGranted = false
            false
        }
    }

    /**
     * Requests root access from the user.
     * This will trigger the Magisk/SuperSU permission dialog.
     *
     * @return true if root access was granted, false otherwise
     */
    suspend fun requestRootAccess(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Clear cached state
            rootAccessGranted = null
            
            // Execute a simple root command to trigger the permission dialog
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            
            // Run 'id' command to verify root
            outputStream.writeBytes("id\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            
            // Read the output
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            
            val exitCode = process.waitFor()
            
            val granted = exitCode == 0 && output.contains("uid=0")
            rootAccessGranted = granted
            granted
        } catch (e: Exception) {
            rootAccessGranted = false
            false
        }
    }

    /**
     * Executes a command as root.
     *
     * @param command The command to execute
     * @return The command output, or null if failed
     */
    suspend fun executeAsRoot(command: String): String? = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            
            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) output else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clears the cached root access state.
     * Call this to force a re-check on next isRootGranted() call.
     */
    fun clearCache() {
        rootAccessGranted = null
    }
}
