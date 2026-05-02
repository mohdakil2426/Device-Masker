package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.SpoofType
import io.github.libxposed.api.XposedInterface

/**
 * Device Identifier Hooker — libxposed API 101 edition.
 *
 * Hooks TelephonyManager, Build.getSerial(), and Settings.Secure.getString() to spoof:
 * - IMEI (getDeviceId, getImei — both no-arg and slot-indexed variants)
 * - IMSI / SubscriberId (getSubscriberId)
 * - ICCID (getSimSerialNumber)
 * - Serial number (Build.getSerial(), SystemProperties ro.serialno)
 * - Android ID (Settings.Secure "android_id")
 * - SIM country ISO, network country ISO, operator names, MCC/MNC
 * - Phone number (getLine1Number)
 *
 * ## Key improvements over YukiHookAPI version
 * 1. `deoptimize()` called after every hook — bypasses ART inlining on heavily-used getters
 * 2. Individual `safeHook()` per method — one OEM missing a signature cannot cascade failures
 * 3. RemotePreferences read in intercept lambda — always reflects latest UI configuration
 * 4. `loadClassOrNull()` used everywhere — isolated renderer processes don't crash
 *
 * ## API 101 hook pattern
 * Lambda-based interceptor: `xi.hook(m).intercept { chain -> proceed(); return spoofed }`. No
 * static callback classes or hook annotations are needed.
 */
object DeviceHooker : BaseSpoofHooker("DeviceHooker") {

    /**
     * Registers all TelephonyManager, Build, and Settings.Secure hooks. Called once per target
     * process from [XposedEntry.onPackageLoaded].
     *
     * @param cl The target app's ClassLoader
     * @param xi The XposedInterface hook engine
     * @param prefs RemotePreferences for this process (live, no restart needed)
     * @param pkg The target app's package name
     */
    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        hookTelephonyManager(cl, xi, prefs, pkg)
        hookBuildSerial(cl, xi, prefs, pkg)
        hookSettingsSecure(cl, xi, prefs, pkg)
        hookSystemProperties(cl, xi, prefs, pkg)
    }

    // ─────────────────────────────────────────────────────────────
    // Telephony Manager hooks
    // ─────────────────────────────────────────────────────────────

    private fun hookTelephonyManager(
        cl: ClassLoader,
        xi: XposedInterface,
        prefs: SharedPreferences,
        pkg: String,
    ) {
        val tm = cl.loadClassOrNull("android.telephony.TelephonyManager") ?: return
        val intClass = Int::class.javaPrimitiveType!!

        // getDeviceId() — no-arg (deprecated, still used by legacy apps)
        safeHook("getDeviceId()") {
            tm.methodOrNull("getDeviceId")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.IMEI)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.IMEI)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        // getDeviceId(int slot) — slot-indexed
        safeHook("getDeviceId(int)") {
            tm.methodOrNull("getDeviceId", intClass)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.IMEI)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.IMEI)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        // getImei() — no-arg (API 26+)
        safeHook("getImei()") {
            tm.methodOrNull("getImei")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.IMEI)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.IMEI)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        // getImei(int slot) — slot-indexed (API 26+)
        safeHook("getImei(int)") {
            tm.methodOrNull("getImei", intClass)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.IMEI)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.IMEI)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        // getSubscriberId() — IMSI, no-arg
        safeHook("getSubscriberId()") {
            tm.methodOrNull("getSubscriberId")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.IMSI)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.IMSI)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        // getSubscriberId(int slot) — slot-indexed
        safeHook("getSubscriberId(int)") {
            tm.methodOrNull("getSubscriberId", intClass)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.IMSI)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.IMSI)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        // getSimSerialNumber() — ICCID, no-arg
        safeHook("getSimSerialNumber()") {
            tm.methodOrNull("getSimSerialNumber")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.ICCID)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.ICCID)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        // getSimSerialNumber(int slot)
        safeHook("getSimSerialNumber(int)") {
            tm.methodOrNull("getSimSerialNumber", intClass)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.ICCID)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.ICCID)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        // getSimCountryIso() — SIM country code
        safeHook("getSimCountryIso()") {
            tm.methodOrNull("getSimCountryIso")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.SIM_COUNTRY_ISO)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.SIM_COUNTRY_ISO)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        safeHook("getSimCountryIso(int)") {
            tm.methodOrNull("getSimCountryIso", intClass)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.SIM_COUNTRY_ISO)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.SIM_COUNTRY_ISO)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        // getNetworkCountryIso()
        safeHook("getNetworkCountryIso()") {
            tm.methodOrNull("getNetworkCountryIso")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.NETWORK_COUNTRY_ISO)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.NETWORK_COUNTRY_ISO)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        safeHook("getNetworkCountryIso(int)") {
            tm.methodOrNull("getNetworkCountryIso", intClass)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.NETWORK_COUNTRY_ISO)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.NETWORK_COUNTRY_ISO)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        // getSimOperatorName() — carrier display name
        safeHook("getSimOperatorName()") {
            tm.methodOrNull("getSimOperatorName")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.SIM_OPERATOR_NAME)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.SIM_OPERATOR_NAME)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        safeHook("getSimOperatorName(int)") {
            tm.methodOrNull("getSimOperatorName", intClass)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.SIM_OPERATOR_NAME)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.SIM_OPERATOR_NAME)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        // getSimOperator() — MCC+MNC string
        safeHook("getSimOperator()") {
            tm.methodOrNull("getSimOperator")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        safeHook("getSimOperator(int)") {
            tm.methodOrNull("getSimOperator", intClass)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        // getNetworkOperator() — PLMN string
        safeHook("getNetworkOperator()") {
            tm.methodOrNull("getNetworkOperator")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.NETWORK_OPERATOR)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.NETWORK_OPERATOR)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        safeHook("getNetworkOperator(int)") {
            tm.methodOrNull("getNetworkOperator", intClass)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.NETWORK_OPERATOR)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.NETWORK_OPERATOR)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        // getLine1Number() — phone number
        safeHook("getLine1Number()") {
            tm.methodOrNull("getLine1Number")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.PHONE_NUMBER)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.PHONE_NUMBER)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Build.getSerial() hook
    // ─────────────────────────────────────────────────────────────

    private fun hookBuildSerial(
        cl: ClassLoader,
        xi: XposedInterface,
        prefs: SharedPreferences,
        pkg: String,
    ) {
        safeHook("Build.getSerial()") {
            val buildClass = cl.loadClassOrNull("android.os.Build") ?: return@safeHook
            buildClass.methodOrNull("getSerial")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.SERIAL)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.SERIAL)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Settings.Secure.getString() hook (intercepts android_id reads)
    // ─────────────────────────────────────────────────────────────

    private fun hookSettingsSecure(
        cl: ClassLoader,
        xi: XposedInterface,
        prefs: SharedPreferences,
        pkg: String,
    ) {
        safeHook("Settings.Secure.getString()") {
            val secureClass =
                cl.loadClassOrNull("android.provider.Settings\$Secure") ?: return@safeHook
            val resolverClass =
                cl.loadClassOrNull("android.content.ContentResolver") ?: return@safeHook
            secureClass.methodOrNull("getString", resolverClass, String::class.java)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val key = chain.args.getOrNull(1) as? String ?: return@intercept result
                    if (key != "android_id") return@intercept result
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.ANDROID_ID)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.ANDROID_ID)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        safeHook("Settings.Secure.getStringForUser()") {
            val secureClass =
                cl.loadClassOrNull("android.provider.Settings\$Secure") ?: return@safeHook
            val resolverClass =
                cl.loadClassOrNull("android.content.ContentResolver") ?: return@safeHook
            secureClass
                .methodOrNull(
                    "getStringForUser",
                    resolverClass,
                    String::class.java,
                    Int::class.javaPrimitiveType!!,
                )
                ?.let { m ->
                    xi.hook(m).intercept { chain ->
                        val result = chain.proceed()
                        val key = chain.args.getOrNull(1) as? String ?: return@intercept result
                        if (key != "android_id") return@intercept result
                        val spoofed =
                            getConfiguredSpoofValue(prefs, pkg, SpoofType.ANDROID_ID)
                                ?: return@intercept result
                        reportSpoofEvent(pkg, SpoofType.ANDROID_ID)
                        spoofed
                    }
                    xi.deoptimize(m)
                }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SystemProperties.get() — intercepts ro.serialno reads
    // ─────────────────────────────────────────────────────────────

    private fun hookSystemProperties(
        cl: ClassLoader,
        xi: XposedInterface,
        prefs: SharedPreferences,
        pkg: String,
    ) {
        val SERIAL_KEYS = setOf("ro.serialno", "ro.boot.serialno", "ril.serialnumber")

        safeHook("SystemProperties.get(String)") {
            val spClass = cl.loadClassOrNull("android.os.SystemProperties") ?: return@safeHook
            spClass.methodOrNull("get", String::class.java)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val key = chain.args.firstOrNull() as? String ?: return@intercept result
                    if (key !in SERIAL_KEYS) return@intercept result
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.SERIAL)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.SERIAL)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        safeHook("SystemProperties.get(String, String)") {
            val spClass = cl.loadClassOrNull("android.os.SystemProperties") ?: return@safeHook
            spClass.methodOrNull("get", String::class.java, String::class.java)?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val key = chain.args.firstOrNull() as? String ?: return@intercept result
                    if (key !in SERIAL_KEYS) return@intercept result
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.SERIAL)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.SERIAL)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
    }
}
