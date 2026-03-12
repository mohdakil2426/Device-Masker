package com.astrixforge.devicemasker.xposed

import com.astrixforge.devicemasker.xposed.hooker.AdvertisingHooker
import com.astrixforge.devicemasker.xposed.hooker.AntiDetectHooker
import com.astrixforge.devicemasker.xposed.hooker.DeviceHooker
import com.astrixforge.devicemasker.xposed.hooker.LocationHooker
import com.astrixforge.devicemasker.xposed.hooker.NetworkHooker
import com.astrixforge.devicemasker.xposed.hooker.SensorHooker
import com.astrixforge.devicemasker.xposed.hooker.SystemHooker
import com.astrixforge.devicemasker.xposed.hooker.SystemServiceHooker
import com.astrixforge.devicemasker.xposed.hooker.WebViewHooker
import com.astrixforge.devicemasker.xposed.service.DeviceMaskerService
import com.astrixforge.devicemasker.xposed.utils.ClassCache
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker

/**
 * Xposed Module Hook Loader - Hybrid Architecture (AIDL + XSharedPreferences Fallback).
 *
 * This module supports TWO configuration delivery mechanisms:
 * 1. AIDL Service (Primary - Real-time):
 *     - DeviceMaskerService runs in system_server
 *     - Initialized by SystemServiceHooker at boot
 *     - Query via service.getSpoofValue() for real-time config
 *     - Requires LSPosed scope to include "android" (System Framework)
 * 2. XSharedPreferences (Fallback - Cached):
 *     - UI writes to SharedPreferences with MODE_WORLD_READABLE
 *     - Hooks read via prefs property (YukiHookAPI XSharedPreferences)
 *     - Used when service is unavailable
 *     - Changes require target app restart
 *
 * LSPosed Configuration:
 * - MUST include "android" (System Framework) for AIDL service
 * - Individual apps no longer need to be added manually
 *
 * Hook Loading Order (CRITICAL):
 * 1. SystemServiceHooker (in loadSystem) - Initialize service in system_server
 * 2. AntiDetectHooker (in loadApp) - MUST load first to hide Xposed presence
 * 3. Device/Network/Advertising/System/Location/Sensor/WebView Hookers
 */
object XposedHookLoader : YukiBaseHooker() {

    private const val TAG = "DeviceMasker"
    private const val SELF_PACKAGE = "com.astrixforge.devicemasker"

    /**
     * Packages to SKIP for app hooks (but NOT for system hooks). These are system-critical
     * processes that should never be spoofed.
     */
    private val SKIP_PACKAGES =
        setOf(
            SELF_PACKAGE, // Our own app
            "com.android.systemui", // SystemUI - can cause UI glitches
            "com.android.phone", // Phone/Dialer - critical for calls
        )

    /**
     * Common classes used across multiple hookers. Pre-loading these into ClassCache improves hook
     * performance.
     */
    private val COMMON_CLASSES =
        arrayOf(
            // Used by DeviceHooker, NetworkHooker, SystemHooker
            "android.telephony.TelephonyManager",
            // Used by DeviceHooker, SystemHooker
            "android.os.Build",
            "android.os.SystemProperties",
            // Used by DeviceHooker
            "android.provider.Settings\$Secure",
            "android.content.ContentResolver",
            // Used by NetworkHooker
            "android.net.wifi.WifiInfo",
            "android.bluetooth.BluetoothAdapter",
            // Used by LocationHooker
            "java.util.TimeZone",
            "java.util.Locale",
            // Used by AntiDetectHooker
            "java.lang.Thread",
            "java.lang.Throwable",
        )

    override fun onHook() {
        // ═══════════════════════════════════════════════════════════
        // SYSTEM FRAMEWORK HOOK (loadSystem)
        // ═══════════════════════════════════════════════════════════
        // This runs when LSPosed scope includes "android" (System Framework).
        // Initializes DeviceMaskerService in system_server at boot.
        loadSystem {
            DualLog.info(TAG, "=== loadSystem: Initializing system hooks ===")

            // Load SystemServiceHooker to initialize our AIDL service
            loadHooker(SystemServiceHooker)

            DualLog.info(TAG, "=== loadSystem: System hooks registered ===")
        }

        // ═══════════════════════════════════════════════════════════
        // APP PROCESS HOOKS (loadApp)
        // ═══════════════════════════════════════════════════════════
        // This runs for each app process in the LSPosed scope.
        // With system-wide hooking, this runs for ALL apps.
        loadApp { loadAppHooks() }
    }

    /**
     * Loads hooks for individual app processes.
     *
     * Uses a hybrid approach:
     * 1. Try to get config from AIDL service (real-time)
     * 2. Fall back to XSharedPreferences (cached)
     */
    private fun YukiBaseHooker.loadAppHooks() {
        // Skip our own module to avoid infinite loops
        if (packageName == SELF_PACKAGE || processName.startsWith(SELF_PACKAGE)) {
            DualLog.debug(TAG, "Skipping SELF: $packageName")
            return
        }

        // Skip system-critical processes
        if (packageName in SKIP_PACKAGES || processName in SKIP_PACKAGES) {
            DualLog.debug(TAG, "Skipping system process: $packageName")
            return
        }

        // ═══════════════════════════════════════════════════════════
        // Check Module Enabled (Hybrid: Service or XSharedPreferences)
        // ═══════════════════════════════════════════════════════════
        val service = getServiceOrNull()
        val moduleEnabled: Boolean
        val appEnabled: Boolean

        if (service != null) {
            // Use AIDL service for real-time config
            moduleEnabled = service.isModuleEnabled
            appEnabled = service.isAppEnabled(packageName)
            DualLog.debug(TAG, "[$packageName] Config from AIDL service")
        } else {
            // Fallback to XSharedPreferences
            moduleEnabled = PrefsHelper.isModuleEnabled(prefs)
            appEnabled = PrefsHelper.isAppEnabled(prefs, packageName)
            DualLog.debug(TAG, "[$packageName] Config from XSharedPreferences (fallback)")
        }

        if (!moduleEnabled) {
            DualLog.debug(TAG, "Module disabled globally, skipping: $packageName")
            return
        }

        if (!appEnabled) {
            DualLog.debug(TAG, "Spoofing disabled for: $packageName")
            return
        }

        DualLog.info(TAG, "Starting hooks for: $packageName (process: $processName)")

        // ═══════════════════════════════════════════════════════════
        // Pre-load common classes into cache for better hook performance
        // ═══════════════════════════════════════════════════════════
        val preloadedCount = ClassCache.preload(appClassLoader, *COMMON_CLASSES)
        DualLog.debug(TAG, "ClassCache preloaded $preloadedCount/${COMMON_CLASSES.size} classes")

        // ═══════════════════════════════════════════════════════════
        // AntiDetectHooker must load FIRST to hide our presence
        // ═══════════════════════════════════════════════════════════
        loadHooker(AntiDetectHooker)

        // ═══════════════════════════════════════════════════════════
        // Device Spoofing Hookers
        // ═══════════════════════════════════════════════════════════
        loadHooker(DeviceHooker)
        loadHooker(NetworkHooker)
        loadHooker(AdvertisingHooker)
        loadHooker(SystemHooker)
        loadHooker(LocationHooker)

        // ═══════════════════════════════════════════════════════════
        // Anti-Fingerprinting Hookers
        // ═══════════════════════════════════════════════════════════
        loadHooker(SensorHooker)
        loadHooker(WebViewHooker)

        // Log cache performance stats
        val stats = ClassCache.stats()
        DualLog.debug(TAG, "ClassCache stats: $stats")

        // Increment filter count if service is available
        service?.incrementFilterCount(packageName)

        DualLog.info(TAG, "Hooks registered for: $packageName")
    }

    /**
     * Attempts to get the DeviceMaskerService instance.
     *
     * Returns null if service is not initialized (e.g., when LSPosed scope doesn't include
     * "android", or during early boot before service init).
     */
    private fun getServiceOrNull(): DeviceMaskerService? {
        return runCatching {
                if (DeviceMaskerService.isInitialized()) {
                    DeviceMaskerService.getInstance()
                } else {
                    null
                }
            }
            .getOrNull()
    }
}
