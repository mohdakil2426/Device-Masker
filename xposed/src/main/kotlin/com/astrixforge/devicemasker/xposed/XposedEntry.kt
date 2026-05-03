package com.astrixforge.devicemasker.xposed

import android.util.Log
import com.astrixforge.devicemasker.common.SharedPrefsKeys
import com.astrixforge.devicemasker.xposed.hooker.AdvertisingHooker
import com.astrixforge.devicemasker.xposed.hooker.AntiDetectHooker
import com.astrixforge.devicemasker.xposed.hooker.DeviceHooker
import com.astrixforge.devicemasker.xposed.hooker.LocationHooker
import com.astrixforge.devicemasker.xposed.hooker.NetworkHooker
import com.astrixforge.devicemasker.xposed.hooker.PackageManagerHooker
import com.astrixforge.devicemasker.xposed.hooker.SensorHooker
import com.astrixforge.devicemasker.xposed.hooker.SubscriptionHooker
import com.astrixforge.devicemasker.xposed.hooker.SystemHooker
import com.astrixforge.devicemasker.xposed.hooker.SystemServiceHooker
import com.astrixforge.devicemasker.xposed.hooker.WebViewHooker
import com.astrixforge.devicemasker.xposed.service.DeviceMaskerService
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import io.github.libxposed.api.error.XposedFrameworkError
import java.util.concurrent.ConcurrentHashMap

/**
 * Device Masker entry point for libxposed API 101.
 *
 * Each target process gets a NEW instance of this class — LSPosed reads
 * META-INF/xposed/java_init.list, instantiates this class via reflection, then calls
 * onPackageReady() or onSystemServerStarting() as appropriate.
 *
 * Architecture decisions:
 * - `instance` singleton gives hookers access to the XposedModule log API
 * - RemotePreferences retrieved via `getRemotePreferences(PREFS_GROUP)` — live, no restart needed
 * - Each hooker is a stateless `object` with a `hook(cl, xi, prefs, pkg)` factory method
 * - All hook registrations wrapped in `hookSafely()` — one failed hooker never crashes others
 *
 * Config delivery path (post-migration): UI → ConfigSync → RemotePreferences → LSPosed DB →
 * getRemotePreferences() (in hooks)
 *
 * Diagnostics path: Hooks → oneway AIDL → DeviceMaskerService (system_server) →
 * DiagnosticsViewModel reads
 */
class XposedEntry : XposedModule() {

    companion object {
        const val TAG = "DeviceMasker"
        const val SELF_PACKAGE = "com.astrixforge.devicemasker"

        /**
         * Preferences group name matching the name written by XposedPrefs.kt in :app. Must match
         * exactly — this is the bridge between what the UI writes and what hooks read.
         */
        const val PREFS_GROUP = "device_masker_config"

        /**
         * Packages to skip entirely for app hooks. These are system-critical processes that must
         * never be intercepted. Note: "android" is skipped separately in onPackageReady() since it
         * is handled by onSystemServerStarting().
         */
        private val SKIP_PACKAGES =
            setOf(
                SELF_PACKAGE, // Own module app — never hook ourselves
                "com.android.systemui", // SystemUI — visual glitch risk
                "com.android.phone", // Phone/Dialer — call quality risk
                "com.google.android.gms", // GMS — integrity check interference risk
            )

        /**
         * Singleton reference to this module instance. Set once per process in the init block.
         * Hooker companion objects access this to call log() and reportSpoofEvent().
         */
        @Volatile
        lateinit var instance: XposedEntry
            private set

        private val hookedClassLoaders = ConcurrentHashMap.newKeySet<Int>()
    }

    private var processName: String = ""

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        super.onModuleLoaded(param)
        instance = this
        processName = param.processName
        log(Log.INFO, TAG, "XposedEntry loaded for process: ${param.processName}", null)
    }

    /**
     * Called when the System Server (android process) loads. This is the ONLY place where the AIDL
     * diagnostics service is initialized.
     *
     * CRITICAL SAFETY: Every single line here must be in its own try-catch. Any uncaught exception
     * here causes a system_server crash = device bootloop.
     */
    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        log(
            Log.INFO,
            TAG,
            "System server loading — initializing diagnostics service hooks...",
            null,
        )
        try {
            SystemServiceHooker.hook(param.classLoader, this)
        } catch (e: XposedFrameworkError) {
            throw e
        } catch (t: Throwable) {
            // Non-fatal — if service registration fails, hooks still work via RemotePreferences
            log(
                Log.WARN,
                TAG,
                "SystemServiceHooker registration failed (diagnostics unavailable): ${t.message}",
                t,
            )
        }
    }

    /**
     * Called when a target package's ClassLoader is ready. Hooks set here intercept identifier
     * reads by the app while avoiding PackageLoadedParam.defaultClassLoader, which is only
     * available on Android 10+.
     *
     * @param param Contains packageName, classLoader, and process metadata.
     */
    override fun onPackageReady(param: PackageReadyParam) {
        val pkg = param.packageName

        // System server is handled by onSystemServerStarting — do not double-hook
        if (pkg == "android") return

        // Skip own app and critical system processes
        if (pkg in SKIP_PACKAGES) return

        // Obtain live RemotePreferences from LSPosed.
        // These reflect the latest values written by the module app — no restart needed.
        // getRemotePreferences() is fast (cached by LSPosed) and safe to call per-package load.
        val prefs =
            try {
                getRemotePreferences(PREFS_GROUP)
            } catch (e: XposedFrameworkError) {
                throw e
            } catch (t: Throwable) {
                log(
                    Log.WARN,
                    TAG,
                    "RemotePreferences unavailable for $pkg — skipping hooks: ${t.message}",
                    t,
                )
                return
            }

        // Master kill-switch — if module is disabled globally, skip all hooks
        if (!prefs.getBoolean(SharedPrefsKeys.KEY_MODULE_ENABLED, true)) return

        val hookPackage = selectHookPackage(pkg, prefs) ?: return

        // Per-app toggle — only hook apps that the user explicitly enabled
        if (!prefs.getBoolean(SharedPrefsKeys.getAppEnabledKey(hookPackage), false)) return

        val cl = param.classLoader
        val classLoaderKey = System.identityHashCode(cl)
        if (!hookedClassLoaders.add(classLoaderKey)) {
            log(Log.DEBUG, TAG, "Hooks already registered for classloader of $hookPackage", null)
            return
        }

        // ═══════════════════════════════════════════════════════════
        // HOOK ORDER IS CRITICAL — do not reorder
        // ═══════════════════════════════════════════════════════════
        // 1. AntiDetect MUST load first — detection apps check for Xposed at launch.
        //    If spoofing hooks run before AntiDetect, Xposed evidence is visible in that window.
        // 2. SubscriptionHooker alongside DeviceHooker — both hook telephony paths.
        //    Apps cross-check TelephonyManager and SubscriptionManager results.
        // 3. Remaining hookers can run in any order.
        // ═══════════════════════════════════════════════════════════
        hookSafely(hookPackage, "AntiDetectHooker") {
            AntiDetectHooker.hook(cl, this, prefs, hookPackage)
        }
        hookSafely(hookPackage, "DeviceHooker") { DeviceHooker.hook(cl, this, prefs, hookPackage) }
        hookSafely(hookPackage, "SubscriptionHooker") {
            SubscriptionHooker.hook(cl, this, prefs, hookPackage)
        }
        hookSafely(hookPackage, "NetworkHooker") {
            NetworkHooker.hook(cl, this, prefs, hookPackage)
        }
        hookSafely(hookPackage, "SystemHooker") { SystemHooker.hook(cl, this, prefs, hookPackage) }
        hookSafely(hookPackage, "LocationHooker") {
            LocationHooker.hook(cl, this, prefs, hookPackage)
        }
        hookSafely(hookPackage, "SensorHooker") { SensorHooker.hook(cl, this, prefs, hookPackage) }
        hookSafely(hookPackage, "AdvertisingHooker") {
            AdvertisingHooker.hook(cl, this, prefs, hookPackage)
        }
        hookSafely(hookPackage, "WebViewHooker") {
            WebViewHooker.hook(cl, this, prefs, hookPackage)
        }
        hookSafely(hookPackage, "PackageManagerHooker") {
            PackageManagerHooker.hook(cl, this, prefs, hookPackage)
        }

        // Report to diagnostics service (fire-and-forget, oneway AIDL — does not block hooks)
        reportPackageHooked(hookPackage)

        log(Log.INFO, TAG, "All hooks registered for: $hookPackage", null)
    }

    private fun selectHookPackage(
        loadedPackage: String,
        prefs: android.content.SharedPreferences,
    ): String? {
        val processBasePackage = processName.substringBefore(':').takeIf { it.isNotBlank() }
        val candidates = listOfNotNull(loadedPackage, processBasePackage).distinct()
        return candidates.firstOrNull { candidate ->
            candidate !in SKIP_PACKAGES &&
                prefs.getBoolean(SharedPrefsKeys.getAppEnabledKey(candidate), false)
        }
    }

    /**
     * Wraps a hooker registration block in try-catch so that one failed hooker cannot prevent
     * subsequent hookers from registering.
     *
     * If the block throws (e.g., class not found, method signature mismatch on this OEM), the error
     * is logged and execution continues with the next hooker.
     */
    private fun hookSafely(pkg: String, name: String, block: () -> Unit) {
        try {
            block()
        } catch (e: XposedFrameworkError) {
            throw e
        } catch (t: Throwable) {
            log(
                Log.ERROR,
                TAG,
                "[$name] Hook registration failed for $pkg: ${t.javaClass.simpleName}: ${t.message}",
                t,
            )
        }
    }

    /** Cached reference to the diagnostics service binder. */
    @Volatile
    private var diagnosticService: com.astrixforge.devicemasker.IDeviceMaskerService? = null

    /**
     * Retrieves the diagnostics service binder when this module is running inside system_server.
     *
     * Target apps must not discover the custom diagnostics service through ServiceManager. Android
     * user builds deny that lookup for untrusted app domains, and repeated denied lookups add
     * startup noise to every scoped process. Hook-side diagnostics therefore fall back to the
     * LSPosed log until a supported libxposed/app bridge exists.
     */
    private fun getService(): com.astrixforge.devicemasker.IDeviceMaskerService? {
        // 1. Return cached binder if still alive
        diagnosticService?.let { if (it.asBinder().isBinderAlive) return it }

        // 2. If in system_server, use the local singleton instance
        if (DeviceMaskerService.isInitialized()) {
            diagnosticService = DeviceMaskerService.getInstance()
            return diagnosticService
        }

        return null
    }

    /**
     * Reports that this package was hooked to the diagnostics service. This is a fire-and-forget
     * call — if the service is unavailable, it fails silently. The diagnostics service is
     * initialized in system_server by SystemServiceHooker.
     */
    private fun reportPackageHooked(pkg: String) {
        log(Log.INFO, TAG, "Hooks registered for: $pkg", null)
        runCatching { getService()?.reportPackageHooked(pkg) }
    }

    /**
     * Reports a spoof event to the diagnostics service after a hooker returns a spoofed value.
     * Declared here so hookers can call XposedEntry.instance.reportSpoofEvent().
     */
    fun reportSpoofEvent(pkg: String, spoofTypeName: String) {
        log(Log.DEBUG, TAG, "Spoof event: $pkg/$spoofTypeName", null)
        runCatching { getService()?.reportSpoofEvent(pkg, spoofTypeName) }
    }

    fun reportLog(tag: String, message: String, level: Int) {
        log(level, tag, message, null)
        runCatching { getService()?.reportLog(tag, message, level) }
    }
}
