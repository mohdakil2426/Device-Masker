package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofType
import io.github.libxposed.api.XposedInterface

/**
 * WebView Hooker — libxposed API 101 edition.
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
        // Load preset once at hook-registration time
        val presetId = getSpoofValue(prefs, pkg, SpoofType.DEVICE_PROFILE) { "" }
        val preset = if (presetId.isNotEmpty()) DeviceProfilePreset.findById(presetId) else null

        hookWebSettings(cl, xi, preset, pkg)
        hookWebViewDefaultUA(cl, xi, preset, pkg)
    }

    private fun hookWebSettings(
        cl: ClassLoader,
        xi: XposedInterface,
        preset: DeviceProfilePreset?,
        pkg: String,
    ) {
        val wsClass = cl.loadClassOrNull("android.webkit.WebSettings") ?: return
        safeHook("WebSettings.getUserAgentString()") {
            wsClass.methodOrNull("getUserAgentString")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val ua = result as? String ?: return@intercept result
                    val model = preset?.model ?: return@intercept result
                    val spoofed = modifyUserAgent(ua, model)
                    if (spoofed != ua) reportSpoofEvent(pkg, SpoofType.DEVICE_PROFILE)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        safeHook("WebSettings.setUserAgentString(String)") {
            wsClass.methodOrNull("setUserAgentString", String::class.java)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val model = preset?.model ?: return@intercept chain.proceed()
                    val ua = chain.args.firstOrNull() as? String ?: return@intercept chain.proceed()
                    if (ua.isNotEmpty() && !ua.contains(model)) {
                        chain.args[0] = modifyUserAgent(ua, model)
                        reportSpoofEvent(pkg, SpoofType.DEVICE_PROFILE)
                    }
                    chain.proceed()
                }
                xi.deoptimize(m)
            }
        }
    }

    private fun hookWebViewDefaultUA(
        cl: ClassLoader,
        xi: XposedInterface,
        preset: DeviceProfilePreset?,
        pkg: String,
    ) {
        safeHook("WebView.getDefaultUserAgent(Context)") {
            val webViewClass = cl.loadClassOrNull("android.webkit.WebView") ?: return@safeHook
            val contextClass = cl.loadClassOrNull("android.content.Context") ?: return@safeHook
            webViewClass.methodOrNull("getDefaultUserAgent", contextClass)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val model = preset?.model ?: return@intercept result
                    val ua = result as? String ?: return@intercept result
                    val spoofed = modifyUserAgent(ua, model)
                    if (spoofed != ua) reportSpoofEvent(pkg, SpoofType.DEVICE_PROFILE)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
    }

    private val UA_DEVICE_REGEX = Regex("""(?<=Android \d+; )([^)]+)""")

    internal fun modifyUserAgent(originalUA: String, model: String): String {
        if (model.isBlank()) return originalUA
        return UA_DEVICE_REGEX.replace(originalUA, model)
    }
}
