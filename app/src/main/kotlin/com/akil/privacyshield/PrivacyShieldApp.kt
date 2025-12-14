package com.akil.privacyshield

import android.app.Application
import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication
import timber.log.Timber

/**
 * PrivacyShield Application class.
 * 
 * Extends [ModuleApplication] from YukiHookAPI to properly initialize
 * the module in both the module app process and hooked app processes.
 * 
 * Responsibilities:
 * - Initialize Timber logging in debug builds
 * - Initialize DataStore for preferences storage
 * - Provide module status information via YukiHookAPI
 */
class PrivacyShieldApp : ModuleApplication() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber logging for debug builds
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("PrivacyShield Application initialized")
        }
        
        // Log module activation status
        Timber.i("PrivacyShield Module Status: ${if (isXposedModuleActive) "Active" else "Inactive"}")
    }
    
    companion object {
        /**
         * Check if the Xposed module is currently active.
         * This is set by YukiHookAPI when the module is properly loaded.
         */
        val isXposedModuleActive: Boolean
            get() = com.highcapable.yukihookapi.YukiHookAPI.Status.isModuleActive
    }
}
