package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import android.util.Log
import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.PrefsHelper
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.BeforeHookCallback

/**
 * WebView Hooker — libxposed API 100 edition.
 *
 * Spoofs WebView User-Agent string to match the active DeviceProfilePreset. WebView UA is a rich
 * source of device fingerprint data: it contains the Android version, device model, and WebView
 * build version.
 *
 * Hooks:
 * - WebSettings.getUserAgentString() — spoof UA returned to app code
 * - WebSettings.setUserAgentString(String) — intercept custom UA before it is set
 * - WebView.getDefaultUserAgent(Context) — static method returning the system default UA
 *
 * UA format: "Mozilla/5.0 (Linux; Android 16; Pixel 9 Pro) AppleWebKit/537.36 ..." We replace the
 * "Android XX; DEVICE" segment with the preset model.
 *
 * ## deoptimize() note
 * getUserAgentString() is called by JavaScript-to-Java bridge during WebView init — often AOT
 * compiled. Must deoptimize to ensure our hook fires consistently.
 */
object WebViewHooker : BaseSpoofHooker("WebViewHooker") {

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        HookState.prefs = prefs
        HookState.pkg = pkg

        // Load preset once at hook-registration time
        val presetId = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.DEVICE_PROFILE) { "" }
        if (presetId.isNotEmpty()) {
            HookState.preset = DeviceProfilePreset.findById(presetId)
        }

        hookWebSettings(cl, xi)
        hookWebViewDefaultUA(cl, xi)
    }

    private fun hookWebSettings(cl: ClassLoader, xi: XposedInterface) {
        val wsClass = cl.loadClassOrNull("android.webkit.WebSettings") ?: return
        safeHook("WebSettings.getUserAgentString()") {
            wsClass.methodOrNull("getUserAgentString")?.let { m ->
                xi.hook(m, GetUserAgentStringHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("WebSettings.setUserAgentString(String)") {
            wsClass.methodOrNull("setUserAgentString", String::class.java)?.let { m ->
                xi.hook(m, SetUserAgentStringHooker::class.java)
            }
        }
    }

    private fun hookWebViewDefaultUA(cl: ClassLoader, xi: XposedInterface) {
        safeHook("WebView.getDefaultUserAgent(Context)") {
            val webViewClass = cl.loadClassOrNull("android.webkit.WebView") ?: return@safeHook
            val contextClass = cl.loadClassOrNull("android.content.Context") ?: return@safeHook
            webViewClass.methodOrNull("getDefaultUserAgent", contextClass)?.let { m ->
                xi.hook(m, GetDefaultUserAgentHooker::class.java)
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
        @Volatile var preset: DeviceProfilePreset? = null
    }

    private val UA_DEVICE_REGEX = Regex("""(?<=Android \d+; )([^)]+)""")

    internal fun modifyUserAgent(originalUA: String, model: String): String {
        if (model.isBlank()) return originalUA
        return UA_DEVICE_REGEX.replace(originalUA, model)
    }

    // ─────────────────────────────────────────────────────────────
    // @XposedHooker callback classes
    // ─────────────────────────────────────────────────────────────

    class GetUserAgentStringHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val pkg = HookState.pkg
                    val model = HookState.preset?.model ?: return
                    val ua = callback.result as? String ?: return
                    val spoofed = modifyUserAgent(ua, model)
                    callback.result = spoofed
                    if (spoofed != ua) reportSpoofEvent(pkg, SpoofType.DEVICE_PROFILE)
                } catch (t: Throwable) {
                    android.util.Log.w("GetUserAgentStringHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class SetUserAgentStringHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun before(callback: BeforeHookCallback) {
                try {
                    val pkg = HookState.pkg
                    val model = HookState.preset?.model ?: return
                    val ua = callback.args.firstOrNull() as? String ?: return
                    if (ua.isNotEmpty() && !ua.contains(model)) {
                        callback.args[0] = modifyUserAgent(ua, model)
                        reportSpoofEvent(pkg, SpoofType.DEVICE_PROFILE)
                    }
                } catch (t: Throwable) {
                    android.util.Log.w("SetUserAgentStringHooker", "before() failed: ${t.message}")
                }
            }
        }
    }

    class GetDefaultUserAgentHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val pkg = HookState.pkg
                    val model = HookState.preset?.model ?: return
                    val ua = callback.result as? String ?: return
                    val spoofed = modifyUserAgent(ua, model)
                    callback.result = spoofed
                    if (spoofed != ua) reportSpoofEvent(pkg, SpoofType.DEVICE_PROFILE)
                } catch (t: Throwable) {
                    android.util.Log.w("GetDefaultUserAgentHooker", "after() failed: ${t.message}")
                }
            }
        }
    }
}
