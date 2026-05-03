package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DualLog
import com.astrixforge.devicemasker.xposed.PrefsHelper
import com.astrixforge.devicemasker.xposed.XposedEntry
import io.github.libxposed.api.error.XposedFrameworkError
import java.lang.reflect.Method

/**
 * Base class for all Device Masker spoof hookers — libxposed API 101 edition.
 *
 * ## Design contract
 *
 * Each hooker is a stateless Kotlin `object` that extends this class. The lifecycle is:
 * 1. XposedEntry.onPackageLoaded() calls `Hooker.hook(cl, xi, prefs, pkg)`
 * 2. `hook()` calls [safeHook] for each method it wants to intercept
 * 3. Inside each [safeHook] block: resolve the [Method], call `xi.hook().intercept {}`, then
 *    `xi.deoptimize()`
 * 4. The lambda passed to `intercept` handles the actual callback via `chain.proceed()` + return
 *    value.
 *
 * ## Why no instance state
 *
 * libxposed API 101 creates a NEW XposedModule instance per target process. Hooker objects are
 * registered once per process load and their callbacks fire on whatever thread the target app uses.
 * Shared state must stay process-stable. `XposedEntry` registers hooks only for the first package
 * loaded in a process to avoid later package loads corrupting process-global state.
 *
 * ## Hook safety pattern (API 101)
 *
 * Every single method hook must be in its own [safeHook] block. One failed method lookup (e.g.,
 * `getImei(int)` absent on some OEM firmware) must NOT prevent other hooks from registering.
 *
 * ```kotlin
 * object DeviceHooker : BaseSpoofHooker("DeviceHooker") {
 *     fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
 *         val tmClass = cl.loadClassOrNull("android.telephony.TelephonyManager") ?: return
 *         // Each method in its own safeHook — one failure cannot cascade
 *         safeHook("getImei()") {
 *             tmClass.methodOrNull("getImei")?.let { m ->
 *                 xi.hook(m).intercept { chain ->
 *                     val original = chain.proceed()
 *                     getConfiguredSpoofValue(prefs, pkg, SpoofType.IMEI) ?: original
 *                 }
 *                 xi.deoptimize(m)  // bypass ART inlining — CRITICAL for guaranteed delivery
 *             }
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseSpoofHooker(protected val tag: String) {

    /**
     * Wraps a single hook registration in a try-catch.
     *
     * If the block throws for any reason (class not found, method not found, hook engine error),
     * the error is logged via XposedEntry.instance.log() and execution continues with the next
     * hook. This ensures one missing method on an OEM firmware never silently skips ALL subsequent
     * hooks.
     *
     * @param methodName Human-readable name for the method (used in error logs only)
     * @param block The hook registration code: resolve method → xi.hook() → xi.deoptimize()
     */
    protected fun safeHook(methodName: String, block: () -> Unit) {
        try {
            block()
        } catch (e: XposedFrameworkError) {
            throw e
        } catch (t: Throwable) {
            val message = "safeHook($methodName) failed: ${t.javaClass.simpleName}: ${t.message}"
            DualLog.warn(tag, message, t)
            runCatching { XposedEntry.instance.log(android.util.Log.WARN, tag, message, null) }
        }
    }

    /**
     * Loads a class from this ClassLoader, returning null if not found. Safe for use in any process
     * — isolated renderers, webview sandboxes, etc. may not have all Android classes in their
     * ClassLoader.
     */
    protected fun ClassLoader.loadClassOrNull(name: String): Class<*>? =
        try {
            loadClass(name)
        } catch (_: ClassNotFoundException) {
            null
        }

    /**
     * Looks up a declared method by name and exact parameter types, returning null if missing.
     * Automatically makes the method accessible for reflection. Safe for OEM firmware variations
     * (e.g., `getImei(int)` may be absent on some devices).
     */
    protected fun Class<*>.methodOrNull(name: String, vararg params: Class<*>): Method? =
        try {
            getDeclaredMethod(name, *params).also { it.isAccessible = true }
        } catch (_: NoSuchMethodException) {
            null
        }

    /**
     * Gets a spoof value from RemotePreferences for a given type. Delegates to PrefsHelper which
     * understands the SharedPrefsKeys key format.
     *
     * @param prefs The RemotePreferences for this process (from XposedEntry.getRemotePreferences)
     * @param pkg The target app's package name
     * @param type The SpoofType being spoofed
     * @param fallback Generator called if no value is configured in prefs
     */
    protected fun getSpoofValue(
        prefs: SharedPreferences,
        pkg: String,
        type: SpoofType,
        fallback: () -> String,
    ): String = PrefsHelper.getSpoofValue(prefs, pkg, type, fallback)

    protected fun getConfiguredSpoofValue(
        prefs: SharedPreferences,
        pkg: String,
        type: SpoofType,
    ): String? = PrefsHelper.getStoredSpoofValue(prefs, pkg, type)

    /**
     * Checks if a spoof type is explicitly enabled for a package in RemotePreferences. Returns
     * false if the key is not present (spoof type defaults to disabled).
     */
    protected fun isSpoofTypeEnabled(
        prefs: SharedPreferences,
        pkg: String,
        type: SpoofType,
    ): Boolean = PrefsHelper.isSpoofTypeEnabled(prefs, pkg, type)

    /**
     * Reports a successful spoof event to the diagnostics-only AIDL service. Fire-and-forget — if
     * the service is unavailable, this call fails silently. Hookers should call this after
     * returning a spoofed value.
     */
    protected fun reportSpoofEvent(pkg: String, type: SpoofType) {
        runCatching { XposedEntry.instance.reportSpoofEvent(pkg, type.name) }
    }
}
