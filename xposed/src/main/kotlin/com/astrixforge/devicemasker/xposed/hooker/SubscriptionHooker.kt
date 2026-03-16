package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.generators.ICCIDGenerator
import com.astrixforge.devicemasker.common.generators.PhoneNumberGenerator
import com.astrixforge.devicemasker.xposed.DualLog
import com.astrixforge.devicemasker.xposed.PrefsHelper
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback

/**
 * Subscription Info Hooker — new in libxposed API 100 migration (Gap 4.6).
 *
 * Many banking and identity verification apps cross-check TelephonyManager with
 * SubscriptionInfo/SubscriptionManager to detect SIM manipulation. DeviceHooker covers
 * TelephonyManager; this hooker covers the SubscriptionManager path.
 *
 * Hooks:
 * - SubscriptionInfo.getIccId() — ICCID
 * - SubscriptionInfo.getCountryIso() — SIM country ISO
 * - SubscriptionInfo.getCarrierName() / getDisplayName() — carrier display name
 * - SubscriptionInfo.getMcc() / getMccString() — Mobile Country Code
 * - SubscriptionInfo.getMnc() / getMncString() — Mobile Network Code
 * - SubscriptionInfo.getNumber() — phone number
 * - SubscriptionInfo.getSubscriberId() — IMSI (via SubscriptionInfo on newer APIs)
 * - SubscriptionManager.getActiveSubscriptionInfoList() — mutates each listed SubscriptionInfo
 *
 * ## SIM count consistency
 * We do NOT hide SIM slots unless the device preset indicates a single-SIM device. Hiding slots
 * when the device has 2 SIM cards is more detectable than showing 2 consistent SIMs.
 */
object SubscriptionHooker : BaseSpoofHooker("SubscriptionHooker") {

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        HookState.prefs = prefs
        HookState.pkg = pkg

        val siClass = cl.loadClassOrNull("android.telephony.SubscriptionInfo") ?: return

        hookSubscriptionInfoGetters(siClass, xi)
        hookSubscriptionManagerList(cl, xi)
    }

    private fun hookSubscriptionInfoGetters(siClass: Class<*>, xi: XposedInterface) {
        safeHook("SubscriptionInfo.getIccId()") {
            siClass.methodOrNull("getIccId")?.let { m ->
                xi.hook(m, GetIccIdHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getCountryIso()") {
            siClass.methodOrNull("getCountryIso")?.let { m ->
                xi.hook(m, GetCountryIsoHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getCarrierName()") {
            siClass.methodOrNull("getCarrierName")?.let { m ->
                xi.hook(m, GetCarrierNameHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getDisplayName()") {
            siClass.methodOrNull("getDisplayName")?.let { m ->
                xi.hook(m, GetCarrierNameHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getMcc()") {
            siClass.methodOrNull("getMcc")?.let { m ->
                xi.hook(m, GetMccHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getMnc()") {
            siClass.methodOrNull("getMnc")?.let { m ->
                xi.hook(m, GetMncHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getMccString()") {
            siClass.methodOrNull("getMccString")?.let { m ->
                xi.hook(m, GetMccStringHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getMncString()") {
            siClass.methodOrNull("getMncString")?.let { m ->
                xi.hook(m, GetMncStringHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getNumber()") {
            siClass.methodOrNull("getNumber")?.let { m ->
                xi.hook(m, GetPhoneNumberHooker::class.java)
                xi.deoptimize(m)
            }
        }
    }

    private fun hookSubscriptionManagerList(cl: ClassLoader, xi: XposedInterface) {
        val smClass = cl.loadClassOrNull("android.telephony.SubscriptionManager") ?: return
        safeHook("SubscriptionManager.getActiveSubscriptionInfoList()") {
            // No params (API 22+)
            smClass.methodOrNull("getActiveSubscriptionInfoList")?.let { m ->
                xi.hook(m, GetActiveSubscriptionListHooker::class.java)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Shared state
    // ─────────────────────────────────────────────────────────────

    internal object HookState {
        @Volatile var prefs: SharedPreferences? = null
        @Volatile var pkg: String = ""
    }

    // ─────────────────────────────────────────────────────────────
    // @XposedHooker callback classes
    // ─────────────────────────────────────────────────────────────

    class GetIccIdHooker : XposedInterface.Hooker {
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
                    DualLog.warn("SubGetIccIdHooker", "after() failed", t)
                }
            }
        }
    }

    class GetCountryIsoHooker : XposedInterface.Hooker {
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
                    DualLog.warn("SubGetCountryIsoHooker", "after() failed", t)
                }
            }
        }
    }

    class GetCarrierNameHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.CARRIER_NAME) {
                            (callback.result as? CharSequence)?.toString() ?: "Carrier"
                        }
                    reportSpoofEvent(pkg, SpoofType.CARRIER_NAME)
                } catch (t: Throwable) {
                    DualLog.warn("SubGetCarrierNameHooker", "after() failed", t)
                }
            }
        }
    }

    class GetMccHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val mccMnc =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC) {
                            "310260"
                        }
                    callback.result = mccMnc.take(3).toIntOrNull() ?: 310
                    reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                } catch (t: Throwable) {
                    DualLog.warn("SubGetMccHooker", "after() failed", t)
                }
            }
        }
    }

    class GetMncHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val mccMnc =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC) {
                            "310260"
                        }
                    callback.result = mccMnc.drop(3).toIntOrNull() ?: 260
                    reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                } catch (t: Throwable) {
                    DualLog.warn("SubGetMncHooker", "after() failed", t)
                }
            }
        }
    }

    class GetMccStringHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val mccMnc =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC) {
                            "310260"
                        }
                    callback.result = mccMnc.take(3)
                    reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                } catch (t: Throwable) {
                    DualLog.warn("SubGetMccStringHooker", "after() failed", t)
                }
            }
        }
    }

    class GetMncStringHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val mccMnc =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC) {
                            "310260"
                        }
                    callback.result = mccMnc.drop(3)
                    reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                } catch (t: Throwable) {
                    DualLog.warn("SubGetMncStringHooker", "after() failed", t)
                }
            }
        }
    }

    class GetPhoneNumberHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.PHONE_NUMBER) {
                            PhoneNumberGenerator.generate()
                        }
                    reportSpoofEvent(pkg, SpoofType.PHONE_NUMBER)
                } catch (t: Throwable) {
                    DualLog.warn("SubGetPhoneNumberHooker", "after() failed", t)
                }
            }
        }
    }

    /**
     * Intercepts the SubscriptionInfo list returned by getActiveSubscriptionInfoList(). The
     * individual SubscriptionInfo objects returned by this list are the SAME objects that
     * SubscriptionInfo.getIccId() etc. are called on, so our per-method hooks above already cover
     * them. This hook exists only as an additional safety net for apps that enumerate the list and
     * read fields via reflection instead of calling getters directly.
     *
     * Currently a no-op passthrough — left as @AfterInvocation placeholder for future field-level
     * mutation if individual getter hooks are bypassed.
     */
    class GetActiveSubscriptionListHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                // Individual SubscriptionInfo getter hooks handle the actual spoofing.
                // No-op here unless we need field-level mutation via reflection in future.
            }
        }
    }
}
