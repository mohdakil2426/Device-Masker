package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.hooker.callback.stableHooker
import io.github.libxposed.api.XposedInterface

/**
 * Subscription Info Hooker — new in libxposed API 101 migration (Gap 4.6).
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
 * - SubscriptionManager.getActiveSubscriptionInfoList() — preserves list shape; individual
 *   SubscriptionInfo getters are spoofed when apps inspect each entry
 *
 * ## SIM count consistency
 * We do NOT hide SIM slots unless the device preset indicates a single-SIM device. Hiding slots
 * when the device has 2 SIM cards is more detectable than showing 2 consistent SIMs.
 */
object SubscriptionHooker : BaseSpoofHooker("SubscriptionHooker") {

    private const val MCC_LENGTH = 3
    private const val MIN_MCC_MNC_LENGTH = 5
    private const val MAX_MCC_MNC_LENGTH = 6

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        val siClass = cl.loadClassOrNull("android.telephony.SubscriptionInfo") ?: return

        hookSubscriptionInfoGetters(siClass, xi, prefs, pkg)
        hookSubscriptionManagerList(cl, xi)
    }

    private fun hookSubscriptionInfoGetters(
        siClass: Class<*>,
        xi: XposedInterface,
        prefs: SharedPreferences,
        pkg: String,
    ) {
        hookSubscriptionStringGetter(siClass, xi, prefs, pkg, "getIccId", SpoofType.ICCID)
        hookSubscriptionStringGetter(
            siClass,
            xi,
            prefs,
            pkg,
            "getCountryIso",
            SpoofType.SIM_COUNTRY_ISO,
        )
        hookSubscriptionStringGetter(
            siClass,
            xi,
            prefs,
            pkg,
            "getCarrierName",
            SpoofType.CARRIER_NAME,
        )
        hookSubscriptionStringGetter(
            siClass,
            xi,
            prefs,
            pkg,
            "getDisplayName",
            SpoofType.CARRIER_NAME,
        )
        hookCarrierCodeGetter(siClass, xi, prefs, pkg, "getMcc") { it.first.toInt() }
        hookCarrierCodeGetter(siClass, xi, prefs, pkg, "getMnc") { it.second.toInt() }
        hookCarrierCodeGetter(siClass, xi, prefs, pkg, "getMccString") { it.first }
        hookCarrierCodeGetter(siClass, xi, prefs, pkg, "getMncString") { it.second }
        hookSubscriptionStringGetter(siClass, xi, prefs, pkg, "getNumber", SpoofType.PHONE_NUMBER)
    }

    private fun hookSubscriptionStringGetter(
        siClass: Class<*>,
        xi: XposedInterface,
        prefs: SharedPreferences,
        pkg: String,
        methodName: String,
        spoofType: SpoofType,
    ) {
        safeHook("SubscriptionInfo.$methodName()") {
            siClass.methodOrNull(methodName)?.let { m ->
                xi.hook(m)
                    .intercept(
                        stableHooker { chain ->
                            val result = chain.proceed()
                            val spoofed =
                                getConfiguredSpoofValue(prefs, pkg, spoofType)
                                    ?: return@stableHooker result
                            reportSpoofEvent(pkg, spoofType)
                            spoofed
                        }
                    )
                xi.deoptimize(m)
            }
        }
    }

    private fun hookCarrierCodeGetter(
        siClass: Class<*>,
        xi: XposedInterface,
        prefs: SharedPreferences,
        pkg: String,
        methodName: String,
        codePart: (Pair<String, String>) -> Any,
    ) {
        safeHook("SubscriptionInfo.$methodName()") {
            siClass.methodOrNull(methodName)?.let { m ->
                xi.hook(m)
                    .intercept(
                        stableHooker { chain ->
                            val result = chain.proceed()
                            val mccMnc =
                                getConfiguredSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC)
                                    ?: return@stableHooker result
                            val carrier = parseCarrierMccMnc(mccMnc) ?: return@stableHooker result
                            reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                            codePart(carrier)
                        }
                    )
                xi.deoptimize(m)
            }
        }
    }

    private fun hookSubscriptionManagerList(cl: ClassLoader, xi: XposedInterface) {
        val smClass = cl.loadClassOrNull("android.telephony.SubscriptionManager") ?: return
        safeHook("SubscriptionManager.getActiveSubscriptionInfoList()") {
            // No params (API 22+)
            smClass.methodOrNull("getActiveSubscriptionInfoList")?.let { m ->
                xi.hook(m).intercept(stableHooker { chain -> chain.proceed() })
                xi.deoptimize(m)
            }
        }
    }

    private fun parseCarrierMccMnc(value: String): Pair<String, String>? {
        val digits = value.trim()
        if (
            digits.length !in MIN_MCC_MNC_LENGTH..MAX_MCC_MNC_LENGTH || digits.any { !it.isDigit() }
        )
            return null
        return digits.take(MCC_LENGTH) to digits.drop(MCC_LENGTH)
    }
}
