package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DeviceMaskerService
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * System Hooker - Spoofs Build.* and SystemProperties.
 *
 * Hooks for:
 * - Build.FINGERPRINT, MODEL, MANUFACTURER, BRAND, DEVICE, PRODUCT, BOARD
 */
object SystemHooker : YukiBaseHooker() {

    private fun getSpoofValue(type: SpoofType, fallback: () -> String): String {
        val service = DeviceMaskerService.instance ?: return fallback()
        val config = service.config
        val profile = config.getProfileForApp(packageName) ?: return fallback()

        if (!profile.isEnabled) return fallback()
        if (!profile.isTypeEnabled(type)) return fallback()

        return profile.getValue(type) ?: fallback()
    }

    override fun onHook() {
        YLog.debug("SystemHooker: Starting hooks for: $packageName")

        hookBuildFields()
        hookSystemProperties()

        DeviceMaskerService.instance?.incrementHookCount()
    }

    private fun hookBuildFields() {
        runCatching {
            val buildClass = "android.os.Build".toClass()

            // Map of field names to SpoofTypes
            val fieldMappings = mapOf(
                "FINGERPRINT" to SpoofType.BUILD_FINGERPRINT,
                "MODEL" to SpoofType.BUILD_MODEL,
                "MANUFACTURER" to SpoofType.BUILD_MANUFACTURER,
                "BRAND" to SpoofType.BUILD_BRAND,
                "DEVICE" to SpoofType.BUILD_DEVICE,
                "PRODUCT" to SpoofType.BUILD_PRODUCT,
                "BOARD" to SpoofType.BUILD_BOARD,
            )

            fieldMappings.forEach { (fieldName, spoofType) ->
                runCatching {
                    val currentValue = buildClass.field { name = fieldName }.get().string()
                    val spoofedValue = getSpoofValue(spoofType) { currentValue }
                    if (spoofedValue != currentValue) {
                        buildClass.field { name = fieldName }.get().set(spoofedValue)
                    }
                }
            }
        }
    }

    private fun hookSystemProperties() {
        runCatching {
            "android.os.SystemProperties".toClass().apply {
                // Build property mappings
                val propertyMappings = mapOf(
                    "ro.build.fingerprint" to SpoofType.BUILD_FINGERPRINT,
                    "ro.product.model" to SpoofType.BUILD_MODEL,
                    "ro.product.manufacturer" to SpoofType.BUILD_MANUFACTURER,
                    "ro.product.brand" to SpoofType.BUILD_BRAND,
                    "ro.product.device" to SpoofType.BUILD_DEVICE,
                    "ro.product.name" to SpoofType.BUILD_PRODUCT,
                    "ro.product.board" to SpoofType.BUILD_BOARD,
                )

                method {
                    name = "get"
                    param(StringClass)
                }.hook {
                    after {
                        val key = args(0).string()
                        propertyMappings[key]?.let { spoofType ->
                            val current = result as? String ?: ""
                            result = getSpoofValue(spoofType) { current }
                        }
                    }
                }

                method {
                    name = "get"
                    param(StringClass, StringClass)
                }.hook {
                    after {
                        val key = args(0).string()
                        propertyMappings[key]?.let { spoofType ->
                            val current = result as? String ?: ""
                            result = getSpoofValue(spoofType) { current }
                        }
                    }
                }
            }
        }
    }
}
