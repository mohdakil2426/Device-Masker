package com.akil.privacyshield.hook

import com.akil.privacyshield.BuildConfig
import com.akil.privacyshield.hook.hooker.AdvertisingHooker
import com.akil.privacyshield.hook.hooker.AntiDetectHooker
import com.akil.privacyshield.hook.hooker.DeviceHooker
import com.akil.privacyshield.hook.hooker.LocationHooker
import com.akil.privacyshield.hook.hooker.NetworkHooker
import com.akil.privacyshield.hook.hooker.SystemHooker
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import timber.log.Timber

/**
 * LSPosed/Xposed module entry point.
 * 
 * This class is automatically detected by LSPosed via the @InjectYukiHookWithXposed
 * annotation. YukiHookAPI's KSP processor generates the necessary Xposed init class.
 * 
 * Hook Loading Order (CRITICAL):
 * 1. AntiDetectHooker - MUST load first to hide Xposed presence before detection
 * 2. DeviceHooker - IMEI, Serial, Hardware IDs
 * 3. NetworkHooker - MAC, WiFi, Bluetooth
 * 4. AdvertisingHooker - GSF ID, Advertising ID, Android ID
 * 5. SystemHooker - Build.*, SystemProperties
 * 6. LocationHooker - GPS, Timezone, Locale
 */
@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {
    
    /**
     * Called once when the module is first loaded.
     * Used to configure YukiHookAPI settings.
     */
    override fun onInit() = YukiHookAPI.configs {
        // Debug logging configuration
        debugLog {
            tag = "PrivacyShield"
            isEnable = BuildConfig.DEBUG
        }
        
        // General debug mode
        isDebug = BuildConfig.DEBUG
    }
    
    /**
     * Called for each app process where the module is active.
     * This is where all hooking logic is registered.
     */
    override fun onHook() = encase {
        // Log when we start hooking an app
        Timber.d("PrivacyShield: Starting hooks for package: $packageName")
        
        // ═══════════════════════════════════════════════════════════
        // CRITICAL: Load anti-detection hooks FIRST
        // This ensures detection checks that run early cannot find us
        // ═══════════════════════════════════════════════════════════
        loadHooker(AntiDetectHooker)
        
        // ═══════════════════════════════════════════════════════════
        // Device Spoofing Hookers - Phase 2
        // ═══════════════════════════════════════════════════════════
        loadHooker(DeviceHooker)
        loadHooker(NetworkHooker)
        loadHooker(AdvertisingHooker)
        loadHooker(SystemHooker)
        loadHooker(LocationHooker)
        
        Timber.d("PrivacyShield: Hooks registered for package: $packageName")
    }
}
