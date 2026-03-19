package com.astrixforge.devicemasker.xposed.hooker

import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.generators.MACGenerator
import com.astrixforge.devicemasker.xposed.DualLog
import com.astrixforge.devicemasker.xposed.PrefsHelper
import io.github.libxposed.api.XposedInterface

import java.net.NetworkInterface

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
        hookWifiInfo(cl, xi, prefs, pkg)
        hookNetworkInterface(cl, xi, prefs, pkg)
        hookBluetoothAdapter(cl, xi, prefs, pkg)
        hookTelephonyCarrier(cl, xi, prefs, pkg)
    }

    private fun hookWifiInfo(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        val wifiInfoClass = cl.loadClassOrNull("android.net.wifi.WifiInfo") ?: return
        safeHook("WifiInfo.getMacAddress()") {
            wifiInfoClass.methodOrNull("getMacAddress")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed = getSpoofValue(prefs, pkg, SpoofType.WIFI_MAC) { MACGenerator.generate() }
                    reportSpoofEvent(pkg, SpoofType.WIFI_MAC)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        safeHook("WifiInfo.getSSID()") {
            wifiInfoClass.methodOrNull("getSSID")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val ssid = getSpoofValue(prefs, pkg, SpoofType.WIFI_SSID) { "HomeNetwork" }
                    reportSpoofEvent(pkg, SpoofType.WIFI_SSID)
                    if (ssid.startsWith("\"")) ssid else "\"$ssid\""
                }
                xi.deoptimize(m)
            }
        }
        safeHook("WifiInfo.getBSSID()") {
            wifiInfoClass.methodOrNull("getBSSID")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed = getSpoofValue(prefs, pkg, SpoofType.WIFI_BSSID) { MACGenerator.generate() }
                    reportSpoofEvent(pkg, SpoofType.WIFI_BSSID)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
    }

    private fun hookNetworkInterface(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        val niClass = cl.loadClassOrNull("java.net.NetworkInterface") ?: return
        safeHook("NetworkInterface.getHardwareAddress()") {
            niClass.methodOrNull("getHardwareAddress")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val networkInterface = chain.thisObject as? NetworkInterface ?: return@intercept result
                    val interfaceName = networkInterface.name ?: return@intercept result
                    if (
                        !interfaceName.startsWith("wlan", ignoreCase = true) &&
                        !interfaceName.startsWith("wifi", ignoreCase = true) &&
                        !interfaceName.startsWith("p2p", ignoreCase = true)
                    ) {
                        return@intercept result
                    }
                    val mac = getSpoofValue(prefs, pkg, SpoofType.WIFI_MAC) { MACGenerator.generate() }
                    reportSpoofEvent(pkg, SpoofType.WIFI_MAC)
                    mac.split(":").map { it.toInt(16).toByte() }.toByteArray()
                }
                xi.deoptimize(m)
            }
        }
    }

    private fun hookBluetoothAdapter(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        val btClass = cl.loadClassOrNull("android.bluetooth.BluetoothAdapter") ?: return
        safeHook("BluetoothAdapter.getAddress()") {
            btClass.methodOrNull("getAddress")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed = getSpoofValue(prefs, pkg, SpoofType.BLUETOOTH_MAC) { MACGenerator.generate() }
                    reportSpoofEvent(pkg, SpoofType.BLUETOOTH_MAC)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
    }

    private fun hookTelephonyCarrier(cl: ClassLoader, xi: XposedInterface, prefs: SharedPreferences, pkg: String) {
        val tmClass = cl.loadClassOrNull("android.telephony.TelephonyManager") ?: return
        safeHook("TelephonyManager.getNetworkOperatorName()") {
            tmClass.methodOrNull("getNetworkOperatorName")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed = getSpoofValue(prefs, pkg, SpoofType.CARRIER_NAME) { (result as? String) ?: "Carrier" }
                    reportSpoofEvent(pkg, SpoofType.CARRIER_NAME)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
        safeHook("TelephonyManager.getNetworkOperator()") {
            tmClass.methodOrNull("getNetworkOperator")?.let { m ->
                xi.hook(m).intercept { chain ->
                    val result = chain.proceed()
                    val spoofed = getSpoofValue(prefs, pkg, SpoofType.CARRIER_MCC_MNC) { (result as? String) ?: "310260" }
                    reportSpoofEvent(pkg, SpoofType.CARRIER_MCC_MNC)
                    spoofed
                }
                xi.deoptimize(m)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Shared state
    // ─────────────────────────────────────────────────────────────


}
