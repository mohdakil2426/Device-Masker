package com.astrixforge.devicemasker.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Coherent device persona snapshot published per package for xposed-side runtime consumption.
 *
 * This sits alongside legacy flat per-type keys during migration and becomes the preferred runtime
 * contract for hookers that need cross-surface consistency.
 */
@Serializable
data class DevicePersona(
    val schemaVersion: Int = 1,
    val groupId: String,
    val packageName: String,
    val rootSeed: String,
    val version: Long,
    val generatedAt: Long,
    val deviceProfileId: String,
    val hardware: HardwarePersona,
    val subscriptions: List<SubscriptionPersona>,
    val location: LocationPersona,
    val networkEnvironment: NetworkEnvironmentPersona,
    val tracking: TrackingPersona,
    val browser: BrowserPersona,
) {
    fun getValue(type: SpoofType): String? {
        val primarySubscription = subscriptions.firstOrNull()
        return when (type) {
            SpoofType.IMEI -> hardware.primaryImei
            SpoofType.IMSI -> primarySubscription?.imsi
            SpoofType.SERIAL -> hardware.serial
            SpoofType.ICCID -> primarySubscription?.iccid
            SpoofType.PHONE_NUMBER -> primarySubscription?.phoneNumber
            SpoofType.SIM_COUNTRY_ISO -> primarySubscription?.simCountryIso
            SpoofType.NETWORK_COUNTRY_ISO -> primarySubscription?.networkCountryIso
            SpoofType.SIM_OPERATOR_NAME -> primarySubscription?.simOperatorName
            SpoofType.NETWORK_OPERATOR -> primarySubscription?.networkOperator
            SpoofType.WIFI_MAC -> hardware.wifiMac
            SpoofType.BLUETOOTH_MAC -> hardware.bluetoothMac
            SpoofType.WIFI_SSID -> networkEnvironment.ssid
            SpoofType.WIFI_BSSID -> networkEnvironment.bssid
            SpoofType.CARRIER_NAME -> primarySubscription?.carrierName
            SpoofType.CARRIER_MCC_MNC -> primarySubscription?.carrierMccMnc
            SpoofType.ANDROID_ID -> tracking.androidId
            SpoofType.GSF_ID -> tracking.gsfId
            SpoofType.ADVERTISING_ID -> tracking.advertisingId
            SpoofType.MEDIA_DRM_ID -> tracking.mediaDrmId
            SpoofType.DEVICE_PROFILE -> deviceProfileId
            SpoofType.LOCATION_LATITUDE -> formatCoordinate(location.latitude)
            SpoofType.LOCATION_LONGITUDE -> formatCoordinate(location.longitude)
            SpoofType.TIMEZONE -> location.timezone
            SpoofType.LOCALE -> location.locale
        }
    }

    fun toJsonString(): String = jsonSerializer.encodeToString(this)

    companion object {
        private val jsonSerializer = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun parse(json: String): DevicePersona = jsonSerializer.decodeFromString(json)

        fun parseOrNull(json: String?): DevicePersona? =
            runCatching {
                    if (json.isNullOrBlank()) {
                        null
                    } else {
                        parse(json)
                    }
                }
                .getOrNull()

        private fun formatCoordinate(value: Double): String =
            String.format(java.util.Locale.US, "%.6f", value)
    }
}

@Serializable
data class HardwarePersona(
    val primaryImei: String,
    val secondaryImei: String? = null,
    val serial: String,
    val wifiMac: String,
    val bluetoothMac: String,
)

@Serializable
data class SubscriptionPersona(
    val slotIndex: Int,
    val isActive: Boolean = true,
    val carrierName: String,
    val carrierMccMnc: String,
    val simOperatorName: String,
    val networkOperator: String,
    val simCountryIso: String,
    val networkCountryIso: String,
    val phoneNumber: String,
    val imsi: String,
    val iccid: String,
)

@Serializable
data class LocationPersona(
    val countryIso: String,
    val region: String? = null,
    val timezone: String,
    val locale: String,
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class NetworkEnvironmentPersona(
    val ssid: String,
    val bssid: String,
    val networkType: Int,
    val isRoaming: Boolean = false,
)

@Serializable
data class TrackingPersona(
    val androidId: String,
    val gsfId: String,
    val advertisingId: String,
    val mediaDrmId: String,
)

@Serializable data class BrowserPersona(val userAgentModel: String, val androidRelease: String)

@Serializable
data class PersonaValidationResult(val isValid: Boolean, val issues: List<String> = emptyList())
