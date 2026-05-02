package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofType
import io.github.libxposed.api.XposedInterface

/**
 * Sensor Hooker — libxposed API 101 edition.
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
        // Load preset at hook-registration time. If DEVICE_PROFILE is disabled, missing, or
        // invalid, leave all sensor APIs untouched for this process.
        val presetId = getConfiguredSpoofValue(prefs, pkg, SpoofType.DEVICE_PROFILE) ?: return
        val preset = DeviceProfilePreset.findById(presetId) ?: return

        hookSensorManager(cl, xi, pkg)
        hookSensorMetadata(cl, xi, preset)
    }

    private fun hookSensorManager(cl: ClassLoader, xi: XposedInterface, pkg: String) {
        val smClass = cl.loadClassOrNull("android.hardware.SensorManager") ?: return
        safeHook("SensorManager.getSensorList(int)") {
            smClass.methodOrNull("getSensorList", Int::class.javaPrimitiveType!!)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val type = chain.args.firstOrNull() as? Int ?: return@intercept result
                    // Only filter TYPE_ALL to avoid breaking individual-type queries
                    if (type != -1) return@intercept result
                    @Suppress("UNCHECKED_CAST")
                    val sensors = result as? List<Any> ?: return@intercept result
                    if (sensors.size <= 10)
                        return@intercept result // Small list — no suspicious fingerprint risk
                    val filtered =
                        sensors.filter { sensor ->
                            runCatching {
                                    val sensorType =
                                        sensor.javaClass.getMethod("getType").invoke(sensor) as Int
                                    sensorType in ESSENTIAL_SENSOR_TYPES
                                }
                                .getOrDefault(true)
                        }

                    if (sensors.size != filtered.size) {
                        reportSpoofEvent(pkg, SpoofType.DEVICE_PROFILE)
                    }
                    filtered
                }
                xi.deoptimize(m)
            }
        }
    }

    private fun hookSensorMetadata(
        cl: ClassLoader,
        xi: XposedInterface,
        preset: DeviceProfilePreset?,
    ) {
        val sensorClass = cl.loadClassOrNull("android.hardware.Sensor") ?: return
        safeHook("Sensor.getVendor()") {
            sensorClass.methodOrNull("getVendor")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    if (preset != null && preset.manufacturer.isNotEmpty()) preset.manufacturer
                    else result
                }
                xi.deoptimize(m)
            }
        }
        safeHook("Sensor.getVersion()") {
            sensorClass.methodOrNull("getVersion")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val version = result as? Int ?: return@intercept result
                    if (version > 3) 1 else result
                }
                xi.deoptimize(m)
            }
        }
        safeHook("Sensor.getName()") {
            sensorClass.methodOrNull("getName")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    var name = result as? String ?: return@intercept result
                    val prefixes = listOf("Qualcomm ", "MediaTek ", "Samsung ")
                    for (prefix in prefixes) {
                        name = name.replace(prefix, "")
                    }
                    name
                }
                xi.deoptimize(m)
            }
        }
    }
}
