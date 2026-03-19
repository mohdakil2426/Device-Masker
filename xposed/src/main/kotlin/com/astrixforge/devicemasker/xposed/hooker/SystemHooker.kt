package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DualLog
import com.astrixforge.devicemasker.xposed.PrefsHelper
import io.github.libxposed.api.XposedInterface
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
        // Get the device profile preset once — it's constant for the lifetime of this process
        val presetId = getSpoofValue(prefs, pkg, SpoofType.DEVICE_PROFILE) { "" }
        if (presetId.isEmpty()) return
        val preset = DeviceProfilePreset.findById(presetId) ?: return

        applyBuildFieldOverrides(cl, preset)
        hookSystemProperties(cl, xi, preset, pkg)
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
        pkg: String,
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

        safeHook("SystemProperties.get(String)") {
            spClass.methodOrNull("get", String::class.java)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val key = chain.args.firstOrNull() as? String ?: return@intercept result
                    val mapped = propertyMappings[key]?.takeIf { it.isNotEmpty() } ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.DEVICE_PROFILE)
                    mapped
                }
                xi.deoptimize(m)
            }
        }
        safeHook("SystemProperties.get(String, String)") {
            spClass.methodOrNull("get", String::class.java, String::class.java)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val key = chain.args.firstOrNull() as? String ?: return@intercept result
                    val mapped = propertyMappings[key]?.takeIf { it.isNotEmpty() } ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.DEVICE_PROFILE)
                    mapped
                }
                xi.deoptimize(m)
            }
        }
    }
}
