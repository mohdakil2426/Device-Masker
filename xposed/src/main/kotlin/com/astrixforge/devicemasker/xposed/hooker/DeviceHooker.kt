package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DeviceMaskerService
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.method
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.type.java.IntType
import com.highcapable.yukihookapi.hook.type.java.StringClass

/**
 * Device Identifier Hooker - Spoofs hardware and device identifiers.
 *
 * Hooks TelephonyManager, Build class, and Settings.Secure to spoof:
 * - IMEI, MEID, IMSI, ICCID
 * - Serial number
 * - Android ID
 *
 * Uses DeviceMaskerService.instance?.config for values (HMA-OSS architecture).
 */
object DeviceHooker : YukiBaseHooker() {

    // Fallback values generated lazily
    private val fallbackImei by lazy { generateImei() }
    private val fallbackMeid by lazy { generateImei() }
    private val fallbackSerial by lazy { generateSerial() }
    private val fallbackAndroidId by lazy { generateAndroidId() }

    /**
     * Gets spoof value from service config or generates fallback.
     */
    private fun getSpoofValue(type: SpoofType, fallback: () -> String): String {
        val service = DeviceMaskerService.instance ?: return fallback()
        val config = service.config
        val profile = config.getProfileForApp(packageName) ?: return fallback()

        if (!profile.isEnabled) return fallback()
        if (!profile.isTypeEnabled(type)) return fallback()

        return profile.getValue(type) ?: fallback()
    }

    override fun onHook() {
        YLog.debug("DeviceHooker: Starting hooks for: $packageName")

        hookTelephonyManager()
        hookBuildClass()
        hookSettingsSecure()

        DeviceMaskerService.instance?.incrementHookCount()
    }

    private fun hookTelephonyManager() {
        runCatching {
            "android.telephony.TelephonyManager".toClass().apply {
                // getDeviceId()
                method {
                    name = "getDeviceId"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.IMEI) { fallbackImei }
                    }
                }

                method {
                    name = "getDeviceId"
                    param(IntType)
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.IMEI) { fallbackImei }
                    }
                }

                // getImei()
                method {
                    name = "getImei"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.IMEI) { fallbackImei }
                    }
                }

                method {
                    name = "getImei"
                    param(IntType)
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.IMEI) { fallbackImei }
                    }
                }

                // getMeid()
                runCatching {
                    method {
                        name = "getMeid"
                        emptyParam()
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.MEID) { fallbackMeid }
                        }
                    }
                }

                runCatching {
                    method {
                        name = "getMeid"
                        param(IntType)
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.MEID) { fallbackMeid }
                        }
                    }
                }

                // getSubscriberId() - IMSI
                method {
                    name = "getSubscriberId"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.IMSI) { generateImsi() }
                    }
                }

                runCatching {
                    method {
                        name = "getSubscriberId"
                        param(IntType)
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.IMSI) { generateImsi() }
                        }
                    }
                }

                // getSimSerialNumber() - ICCID
                method {
                    name = "getSimSerialNumber"
                    emptyParam()
                }.hook {
                    after {
                        result = getSpoofValue(SpoofType.ICCID) { generateSimSerial() }
                    }
                }

                runCatching {
                    method {
                        name = "getSimSerialNumber"
                        param(IntType)
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.ICCID) { generateSimSerial() }
                        }
                    }
                }
            }
        }
    }

    private fun hookBuildClass() {
        runCatching {
            "android.os.Build".toClass().apply {
                runCatching {
                    method {
                        name = "getSerial"
                        modifiers { isStatic }
                    }.hook {
                        after {
                            result = getSpoofValue(SpoofType.SERIAL) { fallbackSerial }
                        }
                    }
                }
            }
        }
        hookSystemProperties()
    }

    private fun hookSystemProperties() {
        runCatching {
            "android.os.SystemProperties".toClass().apply {
                method {
                    name = "get"
                    param(StringClass)
                }.hook {
                    after {
                        val key = args(0).string()
                        if (key in listOf("ro.serialno", "ro.boot.serialno", "ril.serialnumber")) {
                            result = getSpoofValue(SpoofType.SERIAL) { fallbackSerial }
                        }
                    }
                }

                method {
                    name = "get"
                    param(StringClass, StringClass)
                }.hook {
                    after {
                        val key = args(0).string()
                        if (key in listOf("ro.serialno", "ro.boot.serialno", "ril.serialnumber")) {
                            result = getSpoofValue(SpoofType.SERIAL) { fallbackSerial }
                        }
                    }
                }
            }
        }
    }

    private fun hookSettingsSecure() {
        runCatching {
            "android.provider.Settings\$Secure".toClass().apply {
                method {
                    name = "getString"
                    param("android.content.ContentResolver".toClass(), StringClass)
                }.hook {
                    after {
                        if (args(1).string() == "android_id") {
                            result = getSpoofValue(SpoofType.ANDROID_ID) { fallbackAndroidId }
                        }
                    }
                }

                runCatching {
                    method {
                        name = "getStringForUser"
                        param("android.content.ContentResolver".toClass(), StringClass, IntType)
                    }.hook {
                        after {
                            if (args(1).string() == "android_id") {
                                result = getSpoofValue(SpoofType.ANDROID_ID) { fallbackAndroidId }
                            }
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // GENERATORS (fallback values)
    // ═══════════════════════════════════════════════════════════

    private fun generateImei(): String {
        // TAC (Type Allocation Code) + Serial + Luhn checksum
        val tac = listOf("35", "86", "01", "45").random()
        val serial = (1000000..9999999).random().toString()
        val base = tac + serial.padStart(12, '0').take(12)
        return base + calculateLuhn(base)
    }

    private fun calculateLuhn(digits: String): Char {
        var sum = 0
        for ((index, char) in digits.reversed().withIndex()) {
            var digit = char.digitToInt()
            if (index % 2 == 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return ((10 - (sum % 10)) % 10).digitToChar()
    }

    private fun generateSerial(): String {
        val chars = "0123456789ABCDEF"
        return (1..16).map { chars.random() }.joinToString("")
    }

    private fun generateAndroidId(): String {
        val chars = "0123456789abcdef"
        return (1..16).map { chars.random() }.joinToString("")
    }

    private fun generateImsi(): String {
        return "310260" + (100000000L..999999999L).random().toString()
    }

    private fun generateSimSerial(): String {
        return "8901" + List(16) { (0..9).random() }.joinToString("")
    }
}
