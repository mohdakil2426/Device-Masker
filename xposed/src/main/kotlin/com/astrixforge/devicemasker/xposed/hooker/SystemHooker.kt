package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DualLog
import com.astrixforge.devicemasker.xposed.PrefsHelper
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import java.lang.reflect.Field

/**
 * System Hooker — spoofs Build.* static fields and SystemProperties using DeviceProfilePreset.
 *
 * ## Strategy change: field mutation + method hooks
 *
 * YukiHookAPI version: directly mutated Build static fields at hook time (`field {
 * }.get().set(...)`). This was unreliable because ART may have already constant-folded the field
 * values into app code.
 *
 * libxposed API 100 version:
 * 1. Directly modify the static fields on Build class (same as before, catches early reads)
 * 2. Hook SystemProperties.get() to intercept later reads via reflection/system property queries
 *
 * Additionally, Build.getSerial() is **not** hooked here — it's handled by DeviceHooker to maintain
 * the clear separation: DeviceHooker owns hardware identifiers, SystemHooker owns device model /
 * fingerprint metadata.
 */
object SystemHooker : BaseSpoofHooker("SystemHooker") {

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        HookState.prefs = prefs
        HookState.pkg = pkg
        HookState.xi = xi

        // Get the device profile preset once — it's constant for the lifetime of this process
        val presetId = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.DEVICE_PROFILE) { "" }
        if (presetId.isEmpty()) return
        val preset = DeviceProfilePreset.findById(presetId) ?: return

        HookState.preset = preset

        applyBuildFieldOverrides(cl, preset)
        hookSystemProperties(cl, xi, preset)
    }

    // ─────────────────────────────────────────────────────────────
    // Direct Build static field mutation
    // ─────────────────────────────────────────────────────────────

    private fun applyBuildFieldOverrides(cl: ClassLoader, preset: DeviceProfilePreset) {
        val buildClass = cl.loadClassOrNull("android.os.Build") ?: return
        val fieldMappings =
            mapOf(
                "FINGERPRINT" to preset.fingerprint,
                "MODEL" to preset.model,
                "MANUFACTURER" to preset.manufacturer,
                "BRAND" to preset.brand,
                "DEVICE" to preset.device,
                "PRODUCT" to preset.product,
                "BOARD" to preset.board,
                "HARDWARE" to preset.board,
                "TAGS" to "release-keys",
                "TYPE" to "user",
            )
        fieldMappings.forEach { (fieldName, value) ->
            if (value.isNotEmpty()) {
                runCatching {
                        val f: Field = buildClass.getDeclaredField(fieldName)
                        f.isAccessible = true
                        f.set(null, value)
                    }
                    .onFailure { t ->
                        DualLog.warn("SystemHooker", "Could not set Build.$fieldName", t)
                    }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SystemProperties.get() hooks (catches reads via reflection / JNI path)
    // ─────────────────────────────────────────────────────────────

    private fun hookSystemProperties(
        cl: ClassLoader,
        xi: XposedInterface,
        preset: DeviceProfilePreset,
    ) {
        val spClass = cl.loadClassOrNull("android.os.SystemProperties") ?: return

        val propertyMappings = buildMap {
            put("ro.build.fingerprint", preset.fingerprint)
            put("ro.product.model", preset.model)
            put("ro.product.manufacturer", preset.manufacturer)
            put("ro.product.brand", preset.brand)
            put("ro.product.device", preset.device)
            put("ro.product.name", preset.product)
            put("ro.product.board", preset.board)
            put("ro.build.product", preset.product)
            put("ro.vendor.product.model", preset.model)
            put("ro.vendor.product.brand", preset.brand)
            put("ro.vendor.product.device", preset.device)
            put("ro.vendor.product.manufacturer", preset.manufacturer)
            put("ro.vendor.product.name", preset.product)
            put("ro.bootimage.build.fingerprint", preset.fingerprint)
            put("ro.system.build.fingerprint", preset.fingerprint)
            put("ro.vendor.build.fingerprint", preset.fingerprint)
        }
        HookState.propertyMappings = propertyMappings

        safeHook("SystemProperties.get(String)") {
            spClass.methodOrNull("get", String::class.java)?.let { m ->
                xi.hook(m, GetSystemPropertyHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("SystemProperties.get(String, String)") {
            spClass.methodOrNull("get", String::class.java, String::class.java)?.let { m ->
                xi.hook(m, GetSystemPropertyHooker::class.java)
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
        @Volatile var xi: XposedInterface? = null
        @Volatile var preset: DeviceProfilePreset? = null
        @Volatile var propertyMappings: Map<String, String> = emptyMap()
    }

    // ─────────────────────────────────────────────────────────────
    // @XposedHooker callback class
    // ─────────────────────────────────────────────────────────────

    class GetSystemPropertyHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val key = callback.args.firstOrNull() as? String ?: return
                    val mapped =
                        HookState.propertyMappings[key]?.takeIf { it.isNotEmpty() } ?: return
                    val pkg = HookState.pkg
                    callback.result = mapped
                    reportSpoofEvent(pkg, SpoofType.DEVICE_PROFILE)
                } catch (t: Throwable) {
                    DualLog.warn("GetSystemPropertyHooker", "after() failed", t)
                }
            }
        }
    }
}
