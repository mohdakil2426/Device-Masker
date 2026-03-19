package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import android.location.Location
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DualLog
import com.astrixforge.devicemasker.xposed.PrefsHelper
import io.github.libxposed.api.XposedInterface


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
        hookLocation(cl, xi, prefs, pkg)
        hookLocationManager(cl, xi, prefs, pkg)
        hookTimeZone(cl, xi, prefs, pkg)
        hookLocale(cl, xi, prefs, pkg)
    }

    private fun hookLocation(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        val locationClass = cl.loadClassOrNull("android.location.Location") ?: return
        safeHook("Location.getLatitude()") {
            locationClass.methodOrNull("getLatitude")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val current = (result as? Double) ?: 0.0
                    val spoofed = getSpoofValue(prefs, pkg, SpoofType.LOCATION_LATITUDE) { current.toString() }
                    val finalVal = spoofed.toDoubleOrNull() ?: current
                    if (finalVal != current) reportSpoofEvent(pkg, SpoofType.LOCATION_LATITUDE)
                    finalVal
                }
                xi.deoptimize(m)
            }
        }
        safeHook("Location.getLongitude()") {
            locationClass.methodOrNull("getLongitude")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val current = (result as? Double) ?: 0.0
                    val spoofed = getSpoofValue(prefs, pkg, SpoofType.LOCATION_LONGITUDE) { current.toString() }
                    val finalVal = spoofed.toDoubleOrNull() ?: current
                    if (finalVal != current) reportSpoofEvent(pkg, SpoofType.LOCATION_LONGITUDE)
                    finalVal
                }
                xi.deoptimize(m)
            }
        }
    }

    private fun hookLocationManager(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        val lmClass = cl.loadClassOrNull("android.location.LocationManager") ?: return
        safeHook("LocationManager.getLastKnownLocation(String)") {
            lmClass.methodOrNull("getLastKnownLocation", String::class.java)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val location = result as? Location ?: return@intercept result
                    val latStr = getSpoofValue(prefs, pkg, SpoofType.LOCATION_LATITUDE) { "" }
                    val lonStr = getSpoofValue(prefs, pkg, SpoofType.LOCATION_LONGITUDE) { "" }

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
                    result
                }
            }
        }
    }

    private fun hookTimeZone(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        val tzClass = cl.loadClassOrNull("java.util.TimeZone") ?: return
        safeHook("TimeZone.getDefault()") {
            tzClass.methodOrNull("getDefault")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val tzId = getSpoofValue(prefs, pkg, SpoofType.TIMEZONE) { "" }
                    if (tzId.isNotBlank()) {
                        reportSpoofEvent(pkg, SpoofType.TIMEZONE)
                        java.util.TimeZone.getTimeZone(tzId)
                    } else {
                        result
                    }
                }
                xi.deoptimize(m)
            }
        }
        safeHook("TimeZone.getID()") {
            tzClass.methodOrNull("getID")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val tzId = getSpoofValue(prefs, pkg, SpoofType.TIMEZONE) { "" }
                    if (tzId.isNotBlank()) {
                        reportSpoofEvent(pkg, SpoofType.TIMEZONE)
                        tzId
                    } else {
                        result
                    }
                }
                xi.deoptimize(m)
            }
        }
    }

    private fun hookLocale(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        val localeClass = cl.loadClassOrNull("java.util.Locale") ?: return
        safeHook("Locale.getDefault()") {
            localeClass.methodOrNull("getDefault")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val current = result as? java.util.Locale ?: return@intercept result
                    val localeStr = getSpoofValue(prefs, pkg, SpoofType.LOCALE) { "" }
                    if (localeStr.isNotBlank()) {
                        reportSpoofEvent(pkg, SpoofType.LOCALE)
                        runCatching { java.util.Locale.forLanguageTag(localeStr.replace('_', '-')) }.getOrElse { current }
                    } else {
                        result
                    }
                }
                xi.deoptimize(m)
            }
        }
        safeHook("Locale.toString()") {
            localeClass.methodOrNull("toString")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val localeStr = getSpoofValue(prefs, pkg, SpoofType.LOCALE) { "" }
                    if (localeStr.isNotBlank()) {
                        reportSpoofEvent(pkg, SpoofType.LOCALE)
                        localeStr
                    } else {
                        result
                    }
                }
            }
        }
    }

    private fun buildLocale(localeStr: String, current: java.util.Locale): java.util.Locale {
        return runCatching { java.util.Locale.forLanguageTag(localeStr.replace('_', '-')) }.getOrElse { current }
    }
}
