package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import android.location.Location
import android.util.Log
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.PrefsHelper
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback

/**
 * Location Hooker — libxposed API 100 edition.
 *
 * Spoofs:
 * - GPS latitude (Location.getLatitude())
 * - GPS longitude (Location.getLongitude())
 * - LocationManager.getLastKnownLocation() — mutates the returned Location object
 * - TimeZone.getDefault() / TimeZone.getID()
 * - Locale.getDefault() / Locale.toString()
 */
object LocationHooker : BaseSpoofHooker("LocationHooker") {

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        HookState.prefs = prefs
        HookState.pkg = pkg

        hookLocation(cl, xi)
        hookLocationManager(cl, xi)
        hookTimeZone(cl, xi)
        hookLocale(cl, xi)
    }

    private fun hookLocation(cl: ClassLoader, xi: XposedInterface) {
        val locationClass = cl.loadClassOrNull("android.location.Location") ?: return
        safeHook("Location.getLatitude()") {
            locationClass.methodOrNull("getLatitude")?.let { m ->
                xi.hook(m, GetLatitudeHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("Location.getLongitude()") {
            locationClass.methodOrNull("getLongitude")?.let { m ->
                xi.hook(m, GetLongitudeHooker::class.java)
                xi.deoptimize(m)
            }
        }
    }

    private fun hookLocationManager(cl: ClassLoader, xi: XposedInterface) {
        val lmClass = cl.loadClassOrNull("android.location.LocationManager") ?: return
        safeHook("LocationManager.getLastKnownLocation(String)") {
            lmClass.methodOrNull("getLastKnownLocation", String::class.java)?.let { m ->
                xi.hook(m, GetLastKnownLocationHooker::class.java)
            }
        }
    }

    private fun hookTimeZone(cl: ClassLoader, xi: XposedInterface) {
        val tzClass = cl.loadClassOrNull("java.util.TimeZone") ?: return
        safeHook("TimeZone.getDefault()") {
            tzClass.methodOrNull("getDefault")?.let { m ->
                xi.hook(m, GetTimeZoneDefaultHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("TimeZone.getID()") {
            tzClass.methodOrNull("getID")?.let { m ->
                xi.hook(m, GetTimeZoneIdHooker::class.java)
                xi.deoptimize(m)
            }
        }
    }

    private fun hookLocale(cl: ClassLoader, xi: XposedInterface) {
        val localeClass = cl.loadClassOrNull("java.util.Locale") ?: return
        safeHook("Locale.getDefault()") {
            localeClass.methodOrNull("getDefault")?.let { m ->
                xi.hook(m, GetLocaleDefaultHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("Locale.toString()") {
            localeClass.methodOrNull("toString")?.let { m ->
                xi.hook(m, GetLocaleToStringHooker::class.java)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Shared state
    // ─────────────────────────────────────────────────────────────

    internal object HookState {
        @Volatile var prefs: SharedPreferences? = null
        @Volatile var pkg: String = ""
    }

    private fun buildLocale(localeStr: String): java.util.Locale? {
        val parts = localeStr.split("_", "-")
        return when (parts.size) {
            1 -> java.util.Locale(parts[0])
            2 -> java.util.Locale(parts[0], parts[1])
            else -> java.util.Locale(parts[0], parts[1], parts[2])
        }
    }

    // ─────────────────────────────────────────────────────────────
    // @XposedHooker callback classes
    // ─────────────────────────────────────────────────────────────

    class GetLatitudeHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val current = (callback.result as? Double) ?: 0.0
                    val spoofed =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.LOCATION_LATITUDE) {
                            current.toString()
                        }
                    val finalVal = spoofed.toDoubleOrNull() ?: current
                    callback.result = finalVal
                    if (finalVal != current) reportSpoofEvent(pkg, SpoofType.LOCATION_LATITUDE)
                } catch (t: Throwable) {
                    Log.w("GetLatitudeHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetLongitudeHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val current = (callback.result as? Double) ?: 0.0
                    val spoofed =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.LOCATION_LONGITUDE) {
                            current.toString()
                        }
                    val finalVal = spoofed.toDoubleOrNull() ?: current
                    callback.result = finalVal
                    if (finalVal != current) reportSpoofEvent(pkg, SpoofType.LOCATION_LONGITUDE)
                } catch (t: Throwable) {
                    Log.w("GetLongitudeHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetLastKnownLocationHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val location = callback.result as? Location ?: return
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val latStr =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.LOCATION_LATITUDE) { "" }
                    val lonStr =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.LOCATION_LONGITUDE) { "" }

                    var changed = false
                    latStr.toDoubleOrNull()?.let {
                        location.latitude = it
                        changed = true
                    }
                    lonStr.toDoubleOrNull()?.let {
                        location.longitude = it
                        changed = true
                    }

                    if (changed) reportSpoofEvent(pkg, SpoofType.LOCATION_LATITUDE)
                } catch (t: Throwable) {
                    Log.w("GetLastKnownLocationHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetTimeZoneDefaultHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val tzId = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.TIMEZONE) { "" }
                    if (tzId.isNotBlank()) {
                        callback.result = java.util.TimeZone.getTimeZone(tzId)
                        reportSpoofEvent(pkg, SpoofType.TIMEZONE)
                    }
                } catch (t: Throwable) {
                    Log.w("GetTimeZoneDefaultHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetTimeZoneIdHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val tzId = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.TIMEZONE) { "" }
                    if (tzId.isNotBlank()) {
                        callback.result = tzId
                        reportSpoofEvent(pkg, SpoofType.TIMEZONE)
                    }
                } catch (t: Throwable) {
                    Log.w("GetTimeZoneIdHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetLocaleDefaultHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val current = callback.result as? java.util.Locale ?: return
                    val localeStr = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.LOCALE) { "" }
                    if (localeStr.isNotBlank()) {
                        callback.result =
                            runCatching { buildLocale(localeStr) }.getOrElse { current }
                        reportSpoofEvent(pkg, SpoofType.LOCALE)
                    }
                } catch (t: Throwable) {
                    Log.w("GetLocaleDefaultHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetLocaleToStringHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val localeStr = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.LOCALE) { "" }
                    if (localeStr.isNotBlank()) {
                        callback.result = localeStr
                        reportSpoofEvent(pkg, SpoofType.LOCALE)
                    }
                } catch (t: Throwable) {
                    Log.w("GetLocaleToStringHooker", "after() failed: ${t.message}")
                }
            }
        }
    }
}
