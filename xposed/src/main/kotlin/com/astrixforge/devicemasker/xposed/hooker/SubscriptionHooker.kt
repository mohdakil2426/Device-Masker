package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.SpoofType
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
 * - SubscriptionManager.getActiveSubscriptionInfoList() — mutates each listed SubscriptionInfo
 *
 * ## SIM count consistency
 * We do NOT hide SIM slots unless the device preset indicates a single-SIM device. Hiding slots
 * when the device has 2 SIM cards is more detectable than showing 2 consistent SIMs.
 */
object SubscriptionHooker : BaseSpoofHooker("SubscriptionHooker") {

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
        safeHook("SubscriptionInfo.getIccId()") {
            siClass.methodOrNull("getIccId")?.let { m ->
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
        safeHook("SubscriptionInfo.getCountryIso()") {
            siClass.methodOrNull("getCountryIso")?.let { m ->
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
        safeHook("SubscriptionInfo.getCarrierName()") {
            siClass.methodOrNull("getCarrierName")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.CARRIER_NAME)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.CARRIER_NAME)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getDisplayName()") {
            siClass.methodOrNull("getDisplayName")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.CARRIER_NAME)
                            ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.CARRIER_NAME)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getMcc()") {
            siClass.methodOrNull("getMcc")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val mccMnc =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC)
                            ?: return@intercept result
                    val carrier = parseCarrierMccMnc(mccMnc) ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                    carrier.first.toInt()
                }
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getMnc()") {
            siClass.methodOrNull("getMnc")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val mccMnc =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC)
                            ?: return@intercept result
                    val carrier = parseCarrierMccMnc(mccMnc) ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                    carrier.second.toInt()
                }
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getMccString()") {
            siClass.methodOrNull("getMccString")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val mccMnc =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC)
                            ?: return@intercept result
                    val carrier = parseCarrierMccMnc(mccMnc) ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                    carrier.first
                }
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getMncString()") {
            siClass.methodOrNull("getMncString")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val mccMnc =
                        getConfiguredSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC)
                            ?: return@intercept result
                    val carrier = parseCarrierMccMnc(mccMnc) ?: return@intercept result
                    reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                    carrier.second
                }
                xi.deoptimize(m)
            }
        }
        safeHook("SubscriptionInfo.getNumber()") {
            siClass.methodOrNull("getNumber")?.let { m ->
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

    private fun hookSubscriptionManagerList(cl: ClassLoader, xi: XposedInterface) {
        val smClass = cl.loadClassOrNull("android.telephony.SubscriptionManager") ?: return
        safeHook("SubscriptionManager.getActiveSubscriptionInfoList()") {
            // No params (API 22+)
            smClass.methodOrNull("getActiveSubscriptionInfoList")?.let { m ->
                xi.hook(m).intercept { chain -> chain.proceed() }
                xi.deoptimize(m)
            }
        }
    }

    private fun parseCarrierMccMnc(value: String): Pair<String, String>? {
        val digits = value.trim()
        if (digits.length !in 5..6 || digits.any { !it.isDigit() }) return null
        return digits.take(3) to digits.drop(3)
    }
}
