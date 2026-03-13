package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import android.util.Log
import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.PrefsHelper
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback

/**
 * Sensor Hooker — libxposed API 100 edition.
 *
 * Spoofs sensor-related information to match the active DeviceProfilePreset:
 * - SensorManager.getSensorList(int) — filters sensor list to mask unique hardware fingerprints
 * - Sensor.getVendor() — replaced with preset manufacturer for consistency
 * - Sensor.getName() — removes OEM-specific prefixes that leak device model
 * - Sensor.getVersion() — normalized to prevent version-based fingerprinting
 *
 * Sensor list filtering prevents apps from fingerprinting the number and type of sensors, which are
 * device-specific and hard to spoof individually.
 */
object SensorHooker : BaseSpoofHooker("SensorHooker") {

    // Sensor types that should always be present on modern Android devices
    private val ESSENTIAL_SENSOR_TYPES =
        setOf(
            1, // TYPE_ACCELEROMETER
            2, // TYPE_MAGNETIC_FIELD
            4, // TYPE_GYROSCOPE
            5, // TYPE_LIGHT
            8, // TYPE_PROXIMITY
            11, // TYPE_ROTATION_VECTOR
        )

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        HookState.prefs = prefs
        HookState.pkg = pkg

        // Load preset at hook-registration time — constant for this process
        val presetId = PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.DEVICE_PROFILE) { "" }
        if (presetId.isNotEmpty()) {
            HookState.preset = DeviceProfilePreset.findById(presetId)
        }

        hookSensorManager(cl, xi)
        hookSensorMetadata(cl, xi)
    }

    private fun hookSensorManager(cl: ClassLoader, xi: XposedInterface) {
        val smClass = cl.loadClassOrNull("android.hardware.SensorManager") ?: return
        safeHook("SensorManager.getSensorList(int)") {
            smClass.methodOrNull("getSensorList", Int::class.javaPrimitiveType!!)?.let { m ->
                xi.hook(m, GetSensorListHooker::class.java)
            }
        }
    }

    private fun hookSensorMetadata(cl: ClassLoader, xi: XposedInterface) {
        val sensorClass = cl.loadClassOrNull("android.hardware.Sensor") ?: return
        safeHook("Sensor.getVendor()") {
            sensorClass.methodOrNull("getVendor")?.let { m ->
                xi.hook(m, GetSensorVendorHooker::class.java)
            }
        }
        safeHook("Sensor.getVersion()") {
            sensorClass.methodOrNull("getVersion")?.let { m ->
                xi.hook(m, GetSensorVersionHooker::class.java)
            }
        }
        safeHook("Sensor.getName()") {
            sensorClass.methodOrNull("getName")?.let { m ->
                xi.hook(m, GetSensorNameHooker::class.java)
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
        val essentialTypes: Set<Int> = ESSENTIAL_SENSOR_TYPES
    }

    // ─────────────────────────────────────────────────────────────
    // @XposedHooker callback classes
    // ─────────────────────────────────────────────────────────────

    class GetSensorListHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val type = callback.args.firstOrNull() as? Int ?: return
                    // Only filter TYPE_ALL to avoid breaking individual-type queries
                    if (type != -1) return
                    @Suppress("UNCHECKED_CAST")
                    val sensors = callback.result as? List<Any> ?: return
                    if (sensors.size <= 10) return // Small list — no suspicious fingerprint risk
                    val pkg = HookState.pkg
                    callback.result =
                        sensors.filter { sensor ->
                            runCatching {
                                    val sensorType =
                                        sensor.javaClass.getMethod("getType").invoke(sensor) as Int
                                    sensorType in HookState.essentialTypes
                                }
                                .getOrDefault(true)
                        }
                    
                    if (sensors.size != (callback.result as List<*>).size) {
                         reportSpoofEvent(pkg, SpoofType.DEVICE_PROFILE)
                    }
                } catch (t: Throwable) {
                    Log.w("GetSensorListHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetSensorVendorHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val preset = HookState.preset ?: return
                    if (preset.manufacturer.isNotEmpty()) {
                        callback.result = preset.manufacturer
                    }
                } catch (t: Throwable) {
                    Log.w("GetSensorVendorHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetSensorVersionHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val version = callback.result as? Int ?: return
                    if (version > 3) callback.result = 1
                } catch (t: Throwable) {
                    Log.w("GetSensorVersionHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetSensorNameHooker : XposedInterface.Hooker {
        private companion object {
            private val OEM_PREFIXES = listOf("Qualcomm ", "MediaTek ", "Samsung ")

            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    var name = callback.result as? String ?: return
                    for (prefix in OEM_PREFIXES) {
                        name = name.replace(prefix, "")
                    }
                    callback.result = name
                } catch (t: Throwable) {
                    Log.w("GetSensorNameHooker", "after() failed: ${t.message}")
                }
            }
        }
    }
}
