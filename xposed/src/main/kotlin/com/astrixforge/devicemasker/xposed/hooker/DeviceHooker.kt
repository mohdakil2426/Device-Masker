package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import android.util.Log
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.generators.ICCIDGenerator
import com.astrixforge.devicemasker.common.generators.IMEIGenerator
import com.astrixforge.devicemasker.common.generators.IMSIGenerator
import com.astrixforge.devicemasker.common.generators.SerialGenerator
import com.astrixforge.devicemasker.xposed.PrefsHelper
import com.astrixforge.devicemasker.xposed.XposedEntry
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback

/**
 * Device Identifier Hooker — libxposed API 100 edition.
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
 * 3. RemotePreferences read in @AfterInvocation — always reflects latest UI configuration
 * 4. `loadClassOrNull()` used everywhere — isolated renderer processes don't crash
 *
 * ## @XposedHooker companion object pattern
 * Because @BeforeInvocation/@AfterInvocation methods must be static (JVM @JvmStatic), all shared
 * state is in the companion object marked @Volatile for thread safety.
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
        // Publish state to companion objects of @XposedHooker inner classes
        // These are process-local — each process gets its own DeviceHooker instance + companion
        // state
        HookState.prefs = prefs
        HookState.pkg = pkg
        HookState.xi = xi

        hookTelephonyManager(cl, xi)
        hookBuildSerial(cl, xi)
        hookSettingsSecure(cl, xi)
        hookSystemProperties(cl, xi)
    }

    // ─────────────────────────────────────────────────────────────
    // Telephony Manager hooks
    // ─────────────────────────────────────────────────────────────

    private fun hookTelephonyManager(cl: ClassLoader, xi: XposedInterface) {
        val tm = cl.loadClassOrNull("android.telephony.TelephonyManager") ?: return
        val intClass = Int::class.javaPrimitiveType!!

        // getDeviceId() — no-arg (deprecated, still used by legacy apps)
        safeHook("getDeviceId()") {
            tm.methodOrNull("getDeviceId")?.let { m ->
                xi.hook(m, GetImeiHooker::class.java)
                xi.deoptimize(m)
            }
        }
        // getDeviceId(int slot) — slot-indexed
        safeHook("getDeviceId(int)") {
            tm.methodOrNull("getDeviceId", intClass)?.let { m ->
                xi.hook(m, GetImeiHooker::class.java)
                xi.deoptimize(m)
            }
        }
        // getImei() — no-arg (API 26+)
        safeHook("getImei()") {
            tm.methodOrNull("getImei")?.let { m ->
                xi.hook(m, GetImeiHooker::class.java)
                xi.deoptimize(m)
            }
        }
        // getImei(int slot) — slot-indexed (API 26+)
        safeHook("getImei(int)") {
            tm.methodOrNull("getImei", intClass)?.let { m ->
                xi.hook(m, GetImeiHooker::class.java)
                xi.deoptimize(m)
            }
        }
        // getSubscriberId() — IMSI, no-arg
        safeHook("getSubscriberId()") {
            tm.methodOrNull("getSubscriberId")?.let { m ->
                xi.hook(m, GetImsiHooker::class.java)
                xi.deoptimize(m)
            }
        }
        // getSubscriberId(int slot) — slot-indexed
        safeHook("getSubscriberId(int)") {
            tm.methodOrNull("getSubscriberId", intClass)?.let { m ->
                xi.hook(m, GetImsiHooker::class.java)
                xi.deoptimize(m)
            }
        }
        // getSimSerialNumber() — ICCID, no-arg
        safeHook("getSimSerialNumber()") {
            tm.methodOrNull("getSimSerialNumber")?.let { m ->
                xi.hook(m, GetIccidHooker::class.java)
                xi.deoptimize(m)
            }
        }
        // getSimSerialNumber(int slot)
        safeHook("getSimSerialNumber(int)") {
            tm.methodOrNull("getSimSerialNumber", intClass)?.let { m ->
                xi.hook(m, GetIccidHooker::class.java)
                xi.deoptimize(m)
            }
        }
        // getSimCountryIso() — SIM country code
        safeHook("getSimCountryIso()") {
            tm.methodOrNull("getSimCountryIso")?.let { m ->
                xi.hook(m, GetSimCountryIsoHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("getSimCountryIso(int)") {
            tm.methodOrNull("getSimCountryIso", intClass)?.let { m ->
                xi.hook(m, GetSimCountryIsoHooker::class.java)
                xi.deoptimize(m)
            }
        }
        // getNetworkCountryIso()
        safeHook("getNetworkCountryIso()") {
            tm.methodOrNull("getNetworkCountryIso")?.let { m ->
                xi.hook(m, GetNetworkCountryIsoHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("getNetworkCountryIso(int)") {
            tm.methodOrNull("getNetworkCountryIso", intClass)?.let { m ->
                xi.hook(m, GetNetworkCountryIsoHooker::class.java)
                xi.deoptimize(m)
            }
        }
        // getSimOperatorName() — carrier display name
        safeHook("getSimOperatorName()") {
            tm.methodOrNull("getSimOperatorName")?.let { m ->
                xi.hook(m, GetSimOperatorNameHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("getSimOperatorName(int)") {
            tm.methodOrNull("getSimOperatorName", intClass)?.let { m ->
                xi.hook(m, GetSimOperatorNameHooker::class.java)
                xi.deoptimize(m)
            }
        }
        // getSimOperator() — MCC+MNC string
        safeHook("getSimOperator()") {
            tm.methodOrNull("getSimOperator")?.let { m ->
                xi.hook(m, GetMccMncHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("getSimOperator(int)") {
            tm.methodOrNull("getSimOperator", intClass)?.let { m ->
                xi.hook(m, GetMccMncHooker::class.java)
                xi.deoptimize(m)
            }
        }
        // getNetworkOperator() — PLMN string
        safeHook("getNetworkOperator()") {
            tm.methodOrNull("getNetworkOperator")?.let { m ->
                xi.hook(m, GetNetworkOperatorHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("getNetworkOperator(int)") {
            tm.methodOrNull("getNetworkOperator", intClass)?.let { m ->
                xi.hook(m, GetNetworkOperatorHooker::class.java)
                xi.deoptimize(m)
            }
        }
        // getLine1Number() — phone number
        safeHook("getLine1Number()") {
            tm.methodOrNull("getLine1Number")?.let { m ->
                xi.hook(m, GetLine1NumberHooker::class.java)
                xi.deoptimize(m)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Build.getSerial() hook
    // ─────────────────────────────────────────────────────────────

    private fun hookBuildSerial(cl: ClassLoader, xi: XposedInterface) {
        safeHook("Build.getSerial()") {
            val buildClass = cl.loadClassOrNull("android.os.Build") ?: return@safeHook
            buildClass.methodOrNull("getSerial")?.let { m ->
                xi.hook(m, GetSerialHooker::class.java)
                xi.deoptimize(m)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Settings.Secure.getString() hook (intercepts android_id reads)
    // ─────────────────────────────────────────────────────────────

    private fun hookSettingsSecure(cl: ClassLoader, xi: XposedInterface) {
        safeHook("Settings.Secure.getString()") {
            val secureClass =
                cl.loadClassOrNull("android.provider.Settings\$Secure") ?: return@safeHook
            val resolverClass =
                cl.loadClassOrNull("android.content.ContentResolver") ?: return@safeHook
            secureClass.methodOrNull("getString", resolverClass, String::class.java)?.let { m ->
                xi.hook(m, GetSettingsSecureStringHooker::class.java)
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
                    xi.hook(m, GetSettingsSecureStringHooker::class.java)
                    xi.deoptimize(m)
                }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SystemProperties.get() — intercepts ro.serialno reads
    // ─────────────────────────────────────────────────────────────

    private fun hookSystemProperties(cl: ClassLoader, xi: XposedInterface) {
        safeHook("SystemProperties.get(String)") {
            val spClass = cl.loadClassOrNull("android.os.SystemProperties") ?: return@safeHook
            spClass.methodOrNull("get", String::class.java)?.let { m ->
                xi.hook(m, GetSystemPropertyHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("SystemProperties.get(String, String)") {
            val spClass = cl.loadClassOrNull("android.os.SystemProperties") ?: return@safeHook
            spClass.methodOrNull("get", String::class.java, String::class.java)?.let { m ->
                xi.hook(m, GetSystemPropertyHooker::class.java)
                xi.deoptimize(m)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Shared hook state — @Volatile for thread safety
    // Set once per process at hook registration time (XposedEntry.onPackageLoaded)
    // ─────────────────────────────────────────────────────────────

    internal object HookState {
        @Volatile var prefs: SharedPreferences? = null
        @Volatile var pkg: String = ""
        @Volatile var xi: XposedInterface? = null
    }

    // ─────────────────────────────────────────────────────────────
    // @XposedHooker inner classes — one per unique callback behavior
    // @JvmStatic required — libxposed API 100 calls these via JVM static dispatch
    // ─────────────────────────────────────────────────────────────

    class GetImeiHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.IMEI) {
                            IMEIGenerator.generate()
                        }
                    reportSpoofEvent(pkg, SpoofType.IMEI)
                } catch (t: Throwable) {
                    Log.w("GetImeiHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetImsiHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.IMSI) {
                            IMSIGenerator.generate()
                        }
                    reportSpoofEvent(pkg, SpoofType.IMSI)
                } catch (t: Throwable) {
                    Log.w("GetImsiHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetIccidHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.ICCID) {
                            ICCIDGenerator.generate()
                        }
                    reportSpoofEvent(pkg, SpoofType.ICCID)
                } catch (t: Throwable) {
                    Log.w("GetIccidHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetSimCountryIsoHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.SIM_COUNTRY_ISO) { "us" }
                    reportSpoofEvent(pkg, SpoofType.SIM_COUNTRY_ISO)
                } catch (t: Throwable) {
                    Log.w("GetSimCountryIsoHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetNetworkCountryIsoHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.NETWORK_COUNTRY_ISO) {
                            "us"
                        }
                    reportSpoofEvent(pkg, SpoofType.NETWORK_COUNTRY_ISO)
                } catch (t: Throwable) {
                    Log.w("GetNetworkCountryIsoHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetSimOperatorNameHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.SIM_OPERATOR_NAME) {
                            "Carrier"
                        }
                    reportSpoofEvent(pkg, SpoofType.SIM_OPERATOR_NAME)
                } catch (t: Throwable) {
                    Log.w("GetSimOperatorNameHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetMccMncHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC) {
                            "310260"
                        }
                    reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                } catch (t: Throwable) {
                    Log.w("GetMccMncHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetNetworkOperatorHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.NETWORK_OPERATOR) {
                            "310260"
                        }
                    reportSpoofEvent(pkg, SpoofType.NETWORK_OPERATOR)
                } catch (t: Throwable) {
                    Log.w("GetNetworkOperatorHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetLine1NumberHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.PHONE_NUMBER) {
                            com.astrixforge.devicemasker.common.generators.PhoneNumberGenerator
                                .generate()
                        }
                    reportSpoofEvent(pkg, SpoofType.PHONE_NUMBER)
                } catch (t: Throwable) {
                    Log.w("GetLine1NumberHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetSerialHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.SERIAL) {
                            SerialGenerator.generate()
                        }
                    reportSpoofEvent(pkg, SpoofType.SERIAL)
                } catch (t: Throwable) {
                    Log.w("GetSerialHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetSettingsSecureStringHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    // args[1] is the key name; check for "android_id"
                    val key = callback.args.getOrNull(1) as? String ?: return
                    if (key != "android_id") return
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.ANDROID_ID) {
                            com.astrixforge.devicemasker.common.generators.UUIDGenerator
                                .generateAndroidId()
                        }
                    reportSpoofEvent(pkg, SpoofType.ANDROID_ID)
                } catch (t: Throwable) {
                    Log.w("GetSettingsSecureStringHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetSystemPropertyHooker : XposedInterface.Hooker {
        private companion object {
            // SystemProperties keys that leak real serial number
            private val SERIAL_KEYS = setOf("ro.serialno", "ro.boot.serialno", "ril.serialnumber")

            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val key = callback.args.firstOrNull() as? String ?: return
                    if (key !in SERIAL_KEYS) return
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.SERIAL) {
                            SerialGenerator.generate()
                        }
                    reportSpoofEvent(pkg, SpoofType.SERIAL)
                } catch (t: Throwable) {
                    Log.w("GetSystemPropertyHooker", "after() failed: ${t.message}")
                }
            }
        }
    }
}
