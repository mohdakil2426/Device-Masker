package com.astrixforge.devicemasker.verifier

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

class VerifierActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val evidence = VerifierEvidenceCollector.capture(this)
        persistEvidence(evidence)
        setContentView(render(evidence))
    }

    private fun persistEvidence(evidence: JSONObject) {
        val outputDir = File(filesDir, "verifier").also(File::mkdirs)
        File(outputDir, "latest.json").writeText(evidence.toString(JSON_INDENT))
    }

    private fun render(evidence: JSONObject): ScrollView {
        val textView =
            TextView(this).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                setPadding(CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING)
                textSize = TEXT_SIZE_SP
                text = evidence.toString(JSON_INDENT)
            }
        return ScrollView(this).apply { addView(textView) }
    }

    private companion object {
        const val CONTENT_PADDING = 32
        const val TEXT_SIZE_SP = 12f
        const val JSON_INDENT = 2
    }
}

@Suppress("DEPRECATION", "TooManyFunctions")
@SuppressLint("HardwareIds", "MissingPermission")
private object VerifierEvidenceCollector {

    private val FEATURE_NAMES =
        listOf(
            "android.hardware.nfc",
            "android.hardware.nfc.hce",
            "android.hardware.telephony",
            "android.hardware.telephony.data",
            "android.hardware.telephony.radio.access",
            "android.hardware.telephony.subscription",
        )

    fun capture(context: Context): JSONObject =
        JSONObject()
            .put("packageName", context.packageName)
            .put("capturedAtMillis", System.currentTimeMillis())
            .put("build", buildEvidence())
            .put("settings", settingsEvidence(context))
            .put("telephony", telephonyEvidence(context))
            .put("subscription", subscriptionEvidence(context))
            .put("features", featureEvidence(context.packageManager))
            .put("procMaps", ProcMapsProbe.capture())
            .put("packageVisibility", PackageVisibilityProbe.capture(context))
            .put("runtime", CrashProbe.capture())
            .put("wifi", wifiEvidence(context))
            .put("bluetooth", bluetoothEvidence())

    private fun buildEvidence(): JSONObject =
        JSONObject()
            .putResult("BRAND") { Build.BRAND }
            .putResult("MANUFACTURER") { Build.MANUFACTURER }
            .putResult("MODEL") { Build.MODEL }
            .putResult("DEVICE") { Build.DEVICE }
            .putResult("PRODUCT") { Build.PRODUCT }
            .putResult("BOARD") { Build.BOARD }
            .putResult("HARDWARE") { Build.HARDWARE }
            .putResult("FINGERPRINT") { Build.FINGERPRINT }
            .putResult("ID") { Build.ID }
            .putResult("TIME") { Build.TIME }
            .putResult("SERIAL") { Build.getSerial() }
            .put("SUPPORTED_ABIS", JSONArray(Build.SUPPORTED_ABIS.toList()))
            .put("SUPPORTED_32_BIT_ABIS", JSONArray(Build.SUPPORTED_32_BIT_ABIS.toList()))
            .put("SUPPORTED_64_BIT_ABIS", JSONArray(Build.SUPPORTED_64_BIT_ABIS.toList()))
            .put(
                "VERSION",
                JSONObject()
                    .putResult("RELEASE") { Build.VERSION.RELEASE }
                    .putResult("SDK_INT") { Build.VERSION.SDK_INT }
                    .putResult("INCREMENTAL") { Build.VERSION.INCREMENTAL }
                    .putResult("SECURITY_PATCH") { Build.VERSION.SECURITY_PATCH },
            )

    private fun settingsEvidence(context: Context): JSONObject =
        JSONObject().putResult("android_id") {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }

    private fun telephonyEvidence(context: Context): JSONObject {
        val telephony = context.getSystemService(TelephonyManager::class.java)
        return JSONObject()
            .putResult("deviceId") { telephony.deviceId }
            .putResult("imei") { telephony.imei }
            .putResult("subscriberId") { telephony.subscriberId }
            .putResult("simSerialNumber") { telephony.simSerialNumber }
            .putResult("simCountryIso") { telephony.simCountryIso }
            .putResult("networkCountryIso") { telephony.networkCountryIso }
            .putResult("simOperatorName") { telephony.simOperatorName }
            .putResult("networkOperator") { telephony.networkOperator }
            .putResult("line1Number") { telephony.line1Number }
            .putResult("simCount") { telephony.invokeIntGetter("getSimCount") }
            .putResult("phoneCount") { telephony.phoneCount }
            .putResult("activeModemCount") { telephony.invokeIntGetter("getActiveModemCount") }
    }

    private fun subscriptionEvidence(context: Context): JSONObject {
        val subscriptions = context.getSystemService(SubscriptionManager::class.java)
        return JSONObject()
            .putResult("activeSubscriptionInfoCount") {
                subscriptions.invokeIntGetter("getActiveSubscriptionInfoCount")
            }
            .putResult("activeSubscriptionInfoCountMax") {
                subscriptions.invokeIntGetter("getActiveSubscriptionInfoCountMax")
            }
    }

    private fun featureEvidence(packageManager: PackageManager): JSONObject =
        JSONObject().also { output ->
            FEATURE_NAMES.forEach { feature ->
                output.putResult(feature) { packageManager.hasSystemFeature(feature) }
            }
        }

    private fun wifiEvidence(context: Context): JSONObject {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        return JSONObject()
            .putResult("macAddress") { wifi.connectionInfo.macAddress }
            .putResult("ssid") { wifi.connectionInfo.ssid }
            .putResult("bssid") { wifi.connectionInfo.bssid }
    }

    private fun bluetoothEvidence(): JSONObject =
        JSONObject().putResult("address") {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@putResult null
            adapter.javaClass.getMethod("getAddress").invoke(adapter)
        }

    private fun JSONObject.putResult(name: String, value: () -> Any?): JSONObject {
        val result =
            try {
                value() ?: JSONObject.NULL
            } catch (e: SecurityException) {
                "ERROR:${e.javaClass.simpleName}:${e.message.orEmpty()}"
            } catch (e: ReflectiveOperationException) {
                "ERROR:${e.javaClass.simpleName}:${e.message.orEmpty()}"
            } catch (e: UnsupportedOperationException) {
                "ERROR:${e.javaClass.simpleName}:${e.message.orEmpty()}"
            } catch (e: IllegalStateException) {
                "ERROR:${e.javaClass.simpleName}:${e.message.orEmpty()}"
            }
        return put(name, result)
    }

    private fun TelephonyManager.invokeIntGetter(methodName: String): Int =
        javaClass.getMethod(methodName).invoke(this) as Int

    private fun SubscriptionManager.invokeIntGetter(methodName: String): Int =
        javaClass.getMethod(methodName).invoke(this) as Int
}
