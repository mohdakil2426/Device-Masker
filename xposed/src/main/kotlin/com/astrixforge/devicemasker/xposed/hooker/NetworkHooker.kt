package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import android.util.Log
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.generators.MACGenerator
import com.astrixforge.devicemasker.xposed.PrefsHelper
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.AfterHookCallback

/**
 * Network Identifier Hooker — libxposed API 100 edition.
 *
 * Spoofs network-related identifiers:
 * - WiFi MAC address (WifiInfo.getMacAddress, NetworkInterface.getHardwareAddress)
 * - WiFi SSID (WifiInfo.getSSID)
 * - WiFi BSSID (WifiInfo.getBSSID)
 * - Bluetooth MAC address (BluetoothAdapter.getAddress)
 * - Carrier name (TelephonyManager.getNetworkOperatorName)
 * - Carrier MCC/MNC (TelephonyManager.getNetworkOperator)
 *
 * Key improvement: deoptimize() on all hooked methods to prevent ART from inlining these
 * frequently-called getters in networking and WiFi management code paths.
 */
object NetworkHooker : BaseSpoofHooker("NetworkHooker") {

    fun hook(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        HookState.prefs = prefs
        HookState.pkg = pkg

        hookWifiInfo(cl, xi)
        hookNetworkInterface(cl, xi)
        hookBluetoothAdapter(cl, xi)
        hookTelephonyCarrier(cl, xi)
    }

    private fun hookWifiInfo(cl: ClassLoader, xi: XposedInterface) {
        val wifiInfoClass = cl.loadClassOrNull("android.net.wifi.WifiInfo") ?: return
        safeHook("WifiInfo.getMacAddress()") {
            wifiInfoClass.methodOrNull("getMacAddress")?.let { m ->
                xi.hook(m, GetWifiMacHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("WifiInfo.getSSID()") {
            wifiInfoClass.methodOrNull("getSSID")?.let { m ->
                xi.hook(m, GetSsidHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("WifiInfo.getBSSID()") {
            wifiInfoClass.methodOrNull("getBSSID")?.let { m ->
                xi.hook(m, GetBssidHooker::class.java)
                xi.deoptimize(m)
            }
        }
    }

    private fun hookNetworkInterface(cl: ClassLoader, xi: XposedInterface) {
        val niClass = cl.loadClassOrNull("java.net.NetworkInterface") ?: return
        safeHook("NetworkInterface.getHardwareAddress()") {
            niClass.methodOrNull("getHardwareAddress")?.let { m ->
                xi.hook(m, GetHardwareAddressHooker::class.java)
                xi.deoptimize(m)
            }
        }
    }

    private fun hookBluetoothAdapter(cl: ClassLoader, xi: XposedInterface) {
        val btClass = cl.loadClassOrNull("android.bluetooth.BluetoothAdapter") ?: return
        safeHook("BluetoothAdapter.getAddress()") {
            btClass.methodOrNull("getAddress")?.let { m ->
                xi.hook(m, GetBluetoothMacHooker::class.java)
                xi.deoptimize(m)
            }
        }
    }

    private fun hookTelephonyCarrier(cl: ClassLoader, xi: XposedInterface) {
        val tmClass = cl.loadClassOrNull("android.telephony.TelephonyManager") ?: return
        safeHook("TelephonyManager.getNetworkOperatorName()") {
            tmClass.methodOrNull("getNetworkOperatorName")?.let { m ->
                xi.hook(m, GetCarrierNameHooker::class.java)
                xi.deoptimize(m)
            }
        }
        safeHook("TelephonyManager.getNetworkOperator()") {
            tmClass.methodOrNull("getNetworkOperator")?.let { m ->
                xi.hook(m, GetCarrierMccMncHooker::class.java)
                xi.deoptimize(m)
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

    class GetWifiMacHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.WIFI_MAC) {
                            MACGenerator.generate()
                        }
                    reportSpoofEvent(pkg, SpoofType.WIFI_MAC)
                } catch (t: Throwable) {
                    Log.w("GetWifiMacHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetSsidHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val ssid =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.WIFI_SSID) { "HomeNetwork" }
                    // Android wraps SSID in quotes for non-passpoint APs
                    callback.result = if (ssid.startsWith("\"")) ssid else "\"$ssid\""
                    reportSpoofEvent(pkg, SpoofType.WIFI_SSID)
                } catch (t: Throwable) {
                    Log.w("GetSsidHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetBssidHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.WIFI_BSSID) {
                            MACGenerator.generate()
                        }
                    reportSpoofEvent(pkg, SpoofType.WIFI_BSSID)
                } catch (t: Throwable) {
                    Log.w("GetBssidHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetHardwareAddressHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    val mac =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.WIFI_MAC) {
                            MACGenerator.generate()
                        }
                    // We need a helper to parse MAC to bytes. Since ValueGenerators is being
                    // demoted,
                    // we'll use a direct implementation or move it to common/utils.
                    // For now, let's keep it simple.
                    callback.result = mac.split(":").map { it.toInt(16).toByte() }.toByteArray()
                    reportSpoofEvent(pkg, SpoofType.WIFI_MAC)
                } catch (t: Throwable) {
                    Log.w("GetHardwareAddressHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetBluetoothMacHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.BLUETOOTH_MAC) {
                            MACGenerator.generate()
                        }
                    reportSpoofEvent(pkg, SpoofType.BLUETOOTH_MAC)
                } catch (t: Throwable) {
                    Log.w("GetBluetoothMacHooker", "after() failed: ${t.message}")
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
                            (callback.result as? String) ?: "Carrier"
                        }
                    reportSpoofEvent(pkg, SpoofType.CARRIER_NAME)
                } catch (t: Throwable) {
                    Log.w("GetCarrierNameHooker", "after() failed: ${t.message}")
                }
            }
        }
    }

    class GetCarrierMccMncHooker : XposedInterface.Hooker {
        companion object {
            @JvmStatic
            fun after(callback: AfterHookCallback) {
                try {
                    val prefs = HookState.prefs ?: return
                    val pkg = HookState.pkg
                    callback.result =
                        PrefsHelper.getSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC) {
                            (callback.result as? String) ?: "310260"
                        }
                    reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                } catch (t: Throwable) {
                    Log.w("GetCarrierMccMncHooker", "after() failed: ${t.message}")
                }
            }
        }
    }
}
