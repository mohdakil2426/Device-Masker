package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.generators.UUIDGenerator
import com.astrixforge.devicemasker.xposed.DualLog
import com.astrixforge.devicemasker.xposed.PrefsHelper
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback

/**
 * Advertising Identifier Hooker — libxposed API 100 edition.
 *
 * Spoofs advertising and tracking identifiers:
 * - Google Advertising ID (AdvertisingIdClient.Info.getId())
 * - GSF ID via Gservices.getString/getLong("android_id")
 * - MediaDrm device unique ID (getPropertyByteArray("deviceUniqueId"))
 */
object AdvertisingHooker : BaseSpoofHooker("AdvertisingHooker") {

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        HookState.prefs = prefs
        HookState.pkg = pkg

        hookAdvertisingIdClient(cl, xi)
        hookGservices(cl, xi)
        hookMediaDrm(cl, xi)
    }

    private fun hookAdvertisingIdClient(cl: ClassLoader, xi: XposedInterface) {
        safeHook("AdvertisingIdClient.Info.getId()") {
            val infoClass =
                cl.loadClassOrNull(
                    "com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info"
                ) ?: return@safeHook
            infoClass.methodOrNull("getId")?.let { m ->
                xi.hook(m, GetAdvertisingIdHooker::class.java)
                xi.deoptimize(m)
            }
        }
    }

    private fun hookGservices(cl: ClassLoader, xi: XposedInterface) {
        val gservicesClass = cl.loadClassOrNull("com.google.android.gsf.Gservices") ?: return
        safeHook("Gservices.getString(ContentResolver, String)") {
            // Method with ContentResolver + String key + default (3 params)
            gservicesClass
                .getDeclaredMethods()
                .filter { it.name == "getString" && it.parameterCount == 3 }
                .forEach { m ->
                    m.isAccessible = true
                    xi.hook(m, GetGsfStringHooker::class.java)
                }
        }
        safeHook("Gservices.getLong(ContentResolver, String, long)") {
            gservicesClass
                .getDeclaredMethods()
                .filter { it.name == "getLong" && it.parameterCount == 3 }
                .forEach { m ->
                    m.isAccessible = true
                    xi.hook(m, GetGsfLongHooker::class.java)
                }
        }
    }

    private fun hookMediaDrm(cl: ClassLoader, xi: XposedInterface) {
        safeHook("MediaDrm.getPropertyByteArray(String)") {
            val drmClass = cl.loadClassOrNull("android.media.MediaDrm") ?: return@safeHook
            drmClass.methodOrNull("getPropertyByteArray", String::class.java)?.let { m ->
                xi.hook(m, GetMediaDrmIdHooker::class.java)
                xi.deoptimize(m)
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

    // ─────────────────────────────────────────────────────────────
    // @XposedHooker callback classes
    // ─────────────────────────────────────────────────────────────

    class GetAdvertisingIdHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.ADVERTISING_ID) {
                            UUIDGenerator.generateAdvertisingId()
                        }
                    reportSpoofEvent(pkg, SpoofType.ADVERTISING_ID)
                } catch (t: Throwable) {
                    DualLog.warn("GetAdvertisingIdHooker", "after() failed", t)
                }
            }
        }
    }

    class GetGsfStringHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    // Args: (ContentResolver, String key, String default)
                    val key = callback.args.getOrNull(1) as? String ?: return
                    if (key != "android_id") return
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.GSF_ID) {
                            UUIDGenerator.generateGSFId()
                        }
                    reportSpoofEvent(pkg, SpoofType.GSF_ID)
                } catch (t: Throwable) {
                    DualLog.warn("GetGsfStringHooker", "after() failed", t)
                }
            }
        }
    }

    class GetGsfLongHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    // Args: (ContentResolver, String key, long default)
                    val key = callback.args.getOrNull(1) as? String ?: return
                    if (key != "android_id") return
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val spoofed =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.GSF_ID) {
                            UUIDGenerator.generateGSFId()
                        }
                    // GSF ID must be returned as a Long (hex string → Long)
                    val finalVal =
                        runCatching { spoofed.toLong(16) }.getOrElse { callback.result as Long }
                    callback.result = finalVal
                    reportSpoofEvent(pkg, SpoofType.GSF_ID)
                } catch (t: Throwable) {
                    DualLog.warn("GetGsfLongHooker", "after() failed", t)
                }
            }
        }
    }

    class GetMediaDrmIdHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val property = callback.args.firstOrNull() as? String ?: return
                    if (property != "deviceUniqueId") return
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val spoofed =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.MEDIA_DRM_ID) {
                            UUIDGenerator.generateMediaDrmId()
                        }
                    callback.result = hexToBytes(spoofed)
                    reportSpoofEvent(pkg, SpoofType.MEDIA_DRM_ID)
                } catch (t: Throwable) {
                    DualLog.warn("GetMediaDrmIdHooker", "after() failed", t)
                }
            }

            private fun hexToBytes(hex: String): ByteArray =
                runCatching { hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray() }
                    .getOrElse { ByteArray(32) }
        }
    }
}
