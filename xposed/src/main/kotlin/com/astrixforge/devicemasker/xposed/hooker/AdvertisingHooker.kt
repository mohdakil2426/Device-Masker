package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.SpoofType
import io.github.libxposed.api.XposedInterface

/**
 * Advertising Identifier Hooker — libxposed API 101 edition.
 *
 * Spoofs advertising and tracking identifiers:
 * - Google Advertising ID (AdvertisingIdClient.Info.getId())
 * - GSF ID via Gservices.getString/getLong("android_id")
 * - MediaDrm device unique ID (getPropertyByteArray("deviceUniqueId"))
 */
object AdvertisingHooker : BaseSpoofHooker("AdvertisingHooker") {

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        hookAdvertisingIdClient(cl, xi, prefs, pkg)
        hookGservices(cl, xi, prefs, pkg)
        hookMediaDrm(cl, xi, prefs, pkg)
    }

    private fun hookAdvertisingIdClient(
        cl: ClassLoader,
        xi: XposedInterface,
        prefs: SharedPreferences,
        pkg: String,
    ) {
        safeHook("AdvertisingIdClient.Info.getId()") {
            val infoClass =
                cl.loadClassOrNull(
                    "com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info"
                ) ?: return@safeHook
            infoClass.methodOrNull("getId")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.ADVERTISING_ID)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.ADVERTISING_ID)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
    }

    private fun hookGservices(
        cl: ClassLoader,
        xi: XposedInterface,
        prefs: SharedPreferences,
        pkg: String,
    ) {
        val gservicesClass = cl.loadClassOrNull("com.google.android.gsf.Gservices") ?: return
        safeHook("Gservices.getString(ContentResolver, String)") {
            // Method with ContentResolver + String key + default (3 params)
            gservicesClass
                .getDeclaredMethods()
                .filter { it.name == "getString" && it.parameterCount == 3 }
                .forEach { m ->
                    m.isAccessible = true
                    xi.hook(m).intercept { chain ->
                        val result = chain.proceed()
                        val key = chain.args.getOrNull(1) as? String ?: return@intercept result
                        if (key != "android_id") return@intercept result
                        val spoofed =
                            getConfiguredSpoofValue(prefs, pkg, SpoofType.GSF_ID)
                                ?: return@intercept result
                        reportSpoofEvent(pkg, SpoofType.GSF_ID)
                        spoofed
                    }
                    xi.deoptimize(m)
                }
        }
        safeHook("Gservices.getLong(ContentResolver, String, long)") {
            gservicesClass
                .getDeclaredMethods()
                .filter { it.name == "getLong" && it.parameterCount == 3 }
                .forEach { m ->
                    m.isAccessible = true
                    xi.hook(m).intercept { chain ->
                        val result = chain.proceed()
                        val key = chain.args.getOrNull(1) as? String ?: return@intercept result
                        if (key != "android_id") return@intercept result
                        val spoofed =
                            getConfiguredSpoofValue(prefs, pkg, SpoofType.GSF_ID)
                                ?: return@intercept result
                        val finalVal =
                            runCatching { spoofed.toLong(16) }.getOrElse { result as Long }
                        reportSpoofEvent(pkg, SpoofType.GSF_ID)
                        finalVal
                    }
                    xi.deoptimize(m)
                }
        }
    }

    private fun hookMediaDrm(
        cl: ClassLoader,
        xi: XposedInterface,
        prefs: SharedPreferences,
        pkg: String,
    ) {
        safeHook("MediaDrm.getPropertyByteArray(String)") {
            val drmClass = cl.loadClassOrNull("android.media.MediaDrm") ?: return@safeHook
            drmClass.methodOrNull("getPropertyByteArray", String::class.java)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val property = chain.args.firstOrNull() as? String ?: return@intercept result
                    if (property != "deviceUniqueId") return@intercept result
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.MEDIA_DRM_ID)
                            ?: return@intercept result
                    val bytes = hexToBytes(spoofed) ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.MEDIA_DRM_ID)
                    bytes
                }
                xi.deoptimize(m)
            }
        }
    }

    private fun hexToBytes(hex: String): ByteArray? {
        val cleanHex = hex.trim()
        if (cleanHex.length < 2 || cleanHex.length % 2 != 0) return null
        return runCatching { cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray() }
            .getOrNull()
    }
}
