package com.astrixforge.devicemasker.common

import com.astrixforge.devicemasker.common.models.Carrier
import com.astrixforge.devicemasker.common.models.GPSBounds
import com.astrixforge.devicemasker.common.models.LocationConfig
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/**
 * Builds a coherent per-package [DevicePersona] from a [SpoofGroup].
 *
 * During migration the group remains the persisted source of truth for UI state. This generator
 * resolves a stronger runtime persona from that group and overlays explicit post-generation
 * overrides where present.
 */
object PersonaGenerator {

    const val PREVIEW_PACKAGE = "persona.preview"

    fun generate(group: SpoofGroup, packageName: String): DevicePersona {
        val rootSeed = group.resolvedPersonaSeed()
        val preset = resolvePreset(group, rootSeed)
        val carrier = resolveCarrier(group, rootSeed)
        val location = resolveLocation(group, rootSeed, carrier)
        val hardware = resolveHardware(group, rootSeed, preset)
        val subscriptions = resolveSubscriptions(group, rootSeed, carrier, preset)
        val networkEnvironment =
            resolveNetworkEnvironment(group, carrier, location, preset, rootSeed)
        val tracking = resolveTracking(group, packageName, rootSeed)

        return DevicePersona(
            groupId = group.id,
            packageName = packageName,
            rootSeed = rootSeed,
            version = group.updatedAt,
            generatedAt = group.personaGeneratedAt,
            deviceProfileId = preset.id,
            hardware = hardware,
            subscriptions = subscriptions,
            location =
                LocationPersona(
                    countryIso = carrier.countryIsoLower,
                    region = carrier.region,
                    timezone = location.timezone,
                    locale = location.locale,
                    latitude = location.latitude,
                    longitude = location.longitude,
                ),
            networkEnvironment = networkEnvironment,
            tracking = tracking,
            browser =
                BrowserPersona(
                    userAgentModel = preset.model,
                    androidRelease = parseAndroidRelease(preset.fingerprint),
                ),
        )
    }

    fun validate(persona: DevicePersona): PersonaValidationResult {
        val issues = mutableListOf<String>()
        val preset = DeviceProfilePreset.findById(persona.deviceProfileId)
        if (preset == null) {
            issues += "Unknown device profile: ${persona.deviceProfileId}"
        } else {
            val tac = persona.hardware.primaryImei.take(8)
            if (preset.tacPrefixes.none { tac.startsWith(it.take(6)) }) {
                issues += "Primary IMEI does not align with preset TAC prefixes"
            }
        }

        val subscription = persona.subscriptions.firstOrNull()
        if (subscription != null) {
            if (!subscription.imsi.startsWith(subscription.carrierMccMnc)) {
                issues += "IMSI is not aligned with carrier MCC/MNC"
            }
            if (subscription.networkOperator != subscription.carrierMccMnc) {
                issues += "Network operator does not match carrier MCC/MNC"
            }
        }

        if (persona.networkEnvironment.ssid.isBlank()) {
            issues += "SSID must not be blank"
        }
        if (persona.networkEnvironment.bssid.count { it == ':' } != 5) {
            issues += "BSSID must be MAC-like"
        }
        if (
            setOf(
                    persona.tracking.androidId,
                    persona.tracking.gsfId,
                    persona.tracking.advertisingId,
                    persona.tracking.mediaDrmId,
                )
                .size < 4
        ) {
            issues += "Tracking identifiers must remain distinct"
        }

        return PersonaValidationResult(isValid = issues.isEmpty(), issues = issues)
    }

    fun materializeGroup(group: SpoofGroup, packageName: String = PREVIEW_PACKAGE): SpoofGroup {
        val persona = generate(group, packageName)
        val timestamp = group.personaGeneratedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        val updatedIdentifiers =
            SpoofType.entries.associateWith { type ->
                val current = group.getIdentifier(type) ?: DeviceIdentifier.createDefault(type)
                current.copy(value = persona.getValue(type), lastModified = timestamp)
            }

        return group.copy(identifiers = updatedIdentifiers, updatedAt = timestamp)
    }

    private fun resolvePreset(group: SpoofGroup, rootSeed: String): DeviceProfilePreset {
        val explicit =
            group.getExplicitOverrideValue(SpoofType.DEVICE_PROFILE)
                ?: group.getValue(SpoofType.DEVICE_PROFILE)
        return explicit?.let(DeviceProfilePreset::findById)
            ?: pickFrom(rootSeed, "preset", DeviceProfilePreset.PRESETS)
    }

    private fun resolveCarrier(group: SpoofGroup, rootSeed: String): Carrier {
        val explicitMccMnc =
            group.selectedCarrierMccMnc
                ?: group.getExplicitOverrideValue(SpoofType.CARRIER_MCC_MNC)
                ?: group.getValue(SpoofType.CARRIER_MCC_MNC)
        return explicitMccMnc?.let(Carrier::getByMccMnc)
            ?: pickFrom(rootSeed, "carrier", Carrier.ALL_CARRIERS)
    }

    private fun resolveLocation(
        group: SpoofGroup,
        rootSeed: String,
        carrier: Carrier,
    ): LocationConfig {
        val timezone =
            group.getExplicitOverrideValue(SpoofType.TIMEZONE) ?: group.getValue(SpoofType.TIMEZONE)
        val locale =
            group.getExplicitOverrideValue(SpoofType.LOCALE) ?: group.getValue(SpoofType.LOCALE)
        val latitude =
            (group.getExplicitOverrideValue(SpoofType.LOCATION_LATITUDE)
                    ?: group.getValue(SpoofType.LOCATION_LATITUDE))
                ?.toDoubleOrNull()
        val longitude =
            (group.getExplicitOverrideValue(SpoofType.LOCATION_LONGITUDE)
                    ?: group.getValue(SpoofType.LOCATION_LONGITUDE))
                ?.toDoubleOrNull()

        return if (timezone != null && locale != null && latitude != null && longitude != null) {
            LocationConfig(
                country = carrier.countryIso,
                timezone = timezone,
                locale = locale,
                latitude = latitude,
                longitude = longitude,
            )
        } else {
            val countryIso = carrier.countryIso.uppercase(Locale.US)
            val resolvedTimezone =
                timezone ?: pickFrom(rootSeed, "timezone:$countryIso", countryTimezones(countryIso))
            val resolvedLocale =
                locale ?: pickFrom(rootSeed, "locale:$countryIso", countryLocales(countryIso))
            val bounds =
                pickFrom(
                    rootSeed,
                    "bounds:$countryIso",
                    COUNTRY_GPS_BOUNDS[countryIso] ?: listOf(DEFAULT_GPS_BOUNDS),
                )
            LocationConfig(
                country = carrier.countryIso,
                timezone = resolvedTimezone,
                locale = resolvedLocale,
                latitude =
                    latitude
                        ?: deterministicCoordinate(
                            rootSeed,
                            "lat:$countryIso",
                            bounds.minLat,
                            bounds.maxLat,
                        ),
                longitude =
                    longitude
                        ?: deterministicCoordinate(
                            rootSeed,
                            "lon:$countryIso",
                            bounds.minLon,
                            bounds.maxLon,
                        ),
            )
        }
    }

    private fun resolveHardware(
        group: SpoofGroup,
        rootSeed: String,
        preset: DeviceProfilePreset,
    ): HardwarePersona {
        val primaryImei =
            group.getExplicitOverrideValue(SpoofType.IMEI)
                ?: group.getValue(SpoofType.IMEI)
                ?: deterministicImei(rootSeed, "imei-primary", preset)
        val serial =
            group.getExplicitOverrideValue(SpoofType.SERIAL)
                ?: group.getValue(SpoofType.SERIAL)
                ?: deterministicSerial(rootSeed, "serial", preset.manufacturer)
        val wifiMac =
            group.getExplicitOverrideValue(SpoofType.WIFI_MAC)
                ?: group.getValue(SpoofType.WIFI_MAC)
                ?: deterministicMac(rootSeed, "wifi-mac", preset.manufacturer)
        val bluetoothMac =
            group.getExplicitOverrideValue(SpoofType.BLUETOOTH_MAC)
                ?: group.getValue(SpoofType.BLUETOOTH_MAC)
                ?: deterministicMac(rootSeed, "bluetooth-mac", preset.manufacturer)
        val secondaryImei =
            primaryImei
                .takeIf { preset.simCount > 1 }
                ?.let {
                    group.getExplicitOverrideValue(SpoofType.IMEI)?.takeIf { override ->
                        override != primaryImei
                    } ?: deterministicImei(rootSeed, "imei-secondary", preset)
                }

        return HardwarePersona(
            primaryImei = primaryImei,
            secondaryImei = secondaryImei,
            serial = serial,
            wifiMac = wifiMac,
            bluetoothMac = bluetoothMac,
        )
    }

    private fun resolveSubscriptions(
        group: SpoofGroup,
        rootSeed: String,
        carrier: Carrier,
        preset: DeviceProfilePreset,
    ): List<SubscriptionPersona> {
        val primary =
            SubscriptionPersona(
                slotIndex = 0,
                carrierName =
                    group.getExplicitOverrideValue(SpoofType.CARRIER_NAME)
                        ?: group.getValue(SpoofType.CARRIER_NAME)
                        ?: carrier.name,
                carrierMccMnc =
                    group.getExplicitOverrideValue(SpoofType.CARRIER_MCC_MNC)
                        ?: group.getValue(SpoofType.CARRIER_MCC_MNC)
                        ?: carrier.mccMnc,
                simOperatorName =
                    group.getExplicitOverrideValue(SpoofType.SIM_OPERATOR_NAME)
                        ?: group.getValue(SpoofType.SIM_OPERATOR_NAME)
                        ?: carrier.name,
                networkOperator =
                    group.getExplicitOverrideValue(SpoofType.NETWORK_OPERATOR)
                        ?: group.getValue(SpoofType.NETWORK_OPERATOR)
                        ?: carrier.mccMnc,
                simCountryIso =
                    group.getExplicitOverrideValue(SpoofType.SIM_COUNTRY_ISO)
                        ?: group.getValue(SpoofType.SIM_COUNTRY_ISO)
                        ?: carrier.countryIsoLower,
                networkCountryIso =
                    group.getExplicitOverrideValue(SpoofType.NETWORK_COUNTRY_ISO)
                        ?: group.getValue(SpoofType.NETWORK_COUNTRY_ISO)
                        ?: carrier.countryIsoLower,
                phoneNumber =
                    group.getExplicitOverrideValue(SpoofType.PHONE_NUMBER)
                        ?: group.getValue(SpoofType.PHONE_NUMBER)
                        ?: deterministicPhoneNumber(rootSeed, "phone-primary", carrier),
                imsi =
                    group.getExplicitOverrideValue(SpoofType.IMSI)
                        ?: group.getValue(SpoofType.IMSI)
                        ?: deterministicImsi(rootSeed, "imsi-primary", carrier),
                iccid =
                    group.getExplicitOverrideValue(SpoofType.ICCID)
                        ?: group.getValue(SpoofType.ICCID)
                        ?: deterministicIccid(rootSeed, "iccid-primary", carrier),
            )

        if (preset.simCount <= 1) {
            return listOf(primary)
        }

        return listOf(
            primary,
            primary.copy(
                slotIndex = 1,
                phoneNumber = deterministicPhoneNumber(rootSeed, "phone-secondary", carrier),
                imsi = deterministicImsi(rootSeed, "imsi-secondary", carrier),
                iccid = deterministicIccid(rootSeed, "iccid-secondary", carrier),
            ),
        )
    }

    private fun resolveNetworkEnvironment(
        group: SpoofGroup,
        carrier: Carrier,
        location: LocationConfig,
        preset: DeviceProfilePreset,
        rootSeed: String,
    ): NetworkEnvironmentPersona {
        val ssid =
            group.getExplicitOverrideValue(SpoofType.WIFI_SSID)
                ?: group.getValue(SpoofType.WIFI_SSID)
                ?: generateSsid(rootSeed, carrier, location)
        val bssid =
            group.getExplicitOverrideValue(SpoofType.WIFI_BSSID)
                ?: group.getValue(SpoofType.WIFI_BSSID)
                ?: generateBssid(rootSeed, carrier)
        val networkType =
            if (preset.has5G) {
                NetworkTypeMapper.getForMccMnc(carrier.mccMnc)
            } else {
                NetworkTypeMapper.NETWORK_TYPE_LTE
            }

        return NetworkEnvironmentPersona(ssid = ssid, bssid = bssid, networkType = networkType)
    }

    private fun resolveTracking(
        group: SpoofGroup,
        packageName: String,
        rootSeed: String,
    ): TrackingPersona =
        TrackingPersona(
            androidId =
                group.getExplicitOverrideValue(SpoofType.ANDROID_ID)
                    ?: deterministicHex(rootSeed, "android-id:$packageName", 8),
            gsfId =
                group.getExplicitOverrideValue(SpoofType.GSF_ID)
                    ?: group.getValue(SpoofType.GSF_ID)
                    ?: deterministicHex(rootSeed, "gsf-id", 8),
            advertisingId =
                group.getExplicitOverrideValue(SpoofType.ADVERTISING_ID)
                    ?: group.getValue(SpoofType.ADVERTISING_ID)
                    ?: deterministicUuid(rootSeed, "advertising-id").toString(),
            mediaDrmId =
                group.getExplicitOverrideValue(SpoofType.MEDIA_DRM_ID)
                    ?: group.getValue(SpoofType.MEDIA_DRM_ID)
                    ?: deterministicHex(rootSeed, "media-drm-id", 32),
        )

    private fun generateSsid(rootSeed: String, carrier: Carrier, location: LocationConfig): String {
        val countryPatterns =
            when (carrier.countryIso.uppercase(Locale.US)) {
                "US" ->
                    listOf(
                        "\"NETGEAR-${deterministicHex(rootSeed, "ssid-us-netgear", 2).uppercase(Locale.US)}\"",
                        "\"ATT-${deterministicHex(rootSeed, "ssid-us-att", 3).uppercase(Locale.US)}\"",
                        "\"${carrier.name}_5G\"",
                    )
                "IN" ->
                    listOf(
                        "\"${carrier.name}Fiber-${deterministicDigits(rootSeed, "ssid-in-fiber", 4)}\"",
                        "\"Airtel_Xstream_${deterministicDigits(rootSeed, "ssid-in-airtel", 3)}\"",
                        "\"JioFiber-${deterministicHex(rootSeed, "ssid-in-jio", 2).uppercase(Locale.US)}\"",
                    )
                "AU" ->
                    listOf(
                        "\"Telstra-${deterministicDigits(rootSeed, "ssid-au-telstra", 4)}\"",
                        "\"Home-${location.timezone.substringAfter('/').take(4)}\"",
                        "\"TP-Link_${deterministicHex(rootSeed, "ssid-au-tplink", 2).uppercase(Locale.US)}\"",
                    )
                else ->
                    listOf(
                        "\"${carrier.name}_${deterministicDigits(rootSeed, "ssid-carrier", 4)}\"",
                        "\"Home_WiFi_${deterministicHex(rootSeed, "ssid-home", 2).uppercase(Locale.US)}\"",
                        "\"Guest_${carrier.countryIso.uppercase(Locale.US)}\"",
                    )
            }
        return pickFrom(rootSeed, "ssid-pattern:${carrier.mccMnc}", countryPatterns)
    }

    private fun generateBssid(rootSeed: String, carrier: Carrier): String {
        val bytes = digestBytes(rootSeed, "bssid:${carrier.mccMnc}")
        val mac = bytes.copyOfRange(0, 6)
        mac[0] = (mac[0].toInt() and 0xFC).toByte()
        return mac.joinToString(":") { "%02X".format(it) }
    }

    private fun countryTimezones(countryIso: String): List<String> =
        COUNTRY_TIMEZONES[countryIso] ?: listOf("UTC")

    private fun countryLocales(countryIso: String): List<String> =
        COUNTRY_LOCALES[countryIso] ?: listOf("en_US")

    private fun deterministicCoordinate(
        rootSeed: String,
        label: String,
        min: Double,
        max: Double,
    ): Double {
        if (max <= min) return min
        val ratio =
            digestBytes(rootSeed, label)
                .take(8)
                .fold(0L) { acc, byte -> (acc shl 8) or (byte.toLong() and 0xFF) }
                .toDouble() / ULong.MAX_VALUE.toLong().toDouble()
        return min + (max - min) * ratio.coerceIn(0.0, 1.0)
    }

    private fun deterministicImei(
        rootSeed: String,
        label: String,
        preset: DeviceProfilePreset,
    ): String {
        val tac =
            if (preset.tacPrefixes.isNotEmpty()) {
                pickFrom(rootSeed, "$label:tac", preset.tacPrefixes)
            } else {
                "35000000"
            }
        val serial = deterministicDigits(rootSeed, "$label:serial", 6)
        val partial = tac + serial
        return partial + calculateLuhnCheckDigit(partial)
    }

    private fun deterministicImsi(rootSeed: String, label: String, carrier: Carrier): String {
        val remaining = (15 - carrier.mccMnc.length).coerceAtLeast(1)
        return carrier.mccMnc + deterministicDigits(rootSeed, label, remaining)
    }

    private fun deterministicIccid(rootSeed: String, label: String, carrier: Carrier): String {
        val prefix = "89${carrier.countryCode}${carrier.iccidIssuerCode}"
        val remaining = (19 - prefix.length).coerceAtLeast(1)
        return prefix + deterministicDigits(rootSeed, label, remaining)
    }

    private fun deterministicPhoneNumber(
        rootSeed: String,
        label: String,
        carrier: Carrier,
    ): String {
        val nationalLength =
            COUNTRY_PHONE_LENGTH[carrier.countryIso.uppercase(Locale.US)] ?: DEFAULT_PHONE_LENGTH
        return "+${carrier.countryCode}${deterministicDigits(rootSeed, label, nationalLength)}"
    }

    private fun deterministicMac(rootSeed: String, label: String, manufacturer: String): String {
        val ouis =
            MANUFACTURER_OUIS[manufacturer.lowercase(Locale.US)]
                ?: MANUFACTURER_OUIS["generic"].orEmpty()
        val prefix = pickFrom(rootSeed, "$label:oui", ouis).split(':')
        val bytes = digestBytes(rootSeed, label).copyOf(3)
        val suffix = bytes.joinToString(":") { "%02X".format(it) }
        return (prefix + suffix.split(':')).joinToString(":")
    }

    private fun deterministicSerial(rootSeed: String, label: String, manufacturer: String): String =
        when (manufacturer.lowercase(Locale.US)) {
            "samsung" ->
                "R${deterministicDigits(rootSeed, "$label:prefix", 2)}" +
                    "ABCDEFGHJKLMNPRSTUVWXYZ"[deterministicInt(rootSeed, "$label:year", 21)] +
                    deterministicDigits(rootSeed, "$label:body", 8)
            "google" -> deterministicHex(rootSeed, "$label:pixel", 8).uppercase(Locale.US)
            "xiaomi",
            "redmi",
            "poco",
            "mi" -> deterministicAlphaNumeric(rootSeed, "$label:xiaomi", 14)
            else -> deterministicAlphaNumeric(rootSeed, "$label:generic", 12)
        }

    private fun deterministicAlphaNumeric(rootSeed: String, label: String, count: Int): String {
        val alphabet = "0123456789ABCDEFGHJKLMNPRSTUVWXYZ"
        val bytes = digestBytes(rootSeed, label)
        return buildString {
            repeat(count) { index ->
                append(alphabet[(bytes[index].toInt() and 0xFF) % alphabet.length])
            }
        }
    }

    private fun calculateLuhnCheckDigit(partial: String): Int {
        require(partial.all(Char::isDigit)) { "Luhn input must be decimal digits only" }
        var sum = 0
        for (index in partial.indices) {
            var digit = partial[index].digitToInt()
            if (index % 2 != 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return (10 - (sum % 10)) % 10
    }

    private fun parseAndroidRelease(fingerprint: String): String =
        fingerprint.substringAfter(':', "16").substringBefore('/')

    private fun <T> pickFrom(rootSeed: String, label: String, options: List<T>): T {
        require(options.isNotEmpty()) { "Options must not be empty" }
        val index = deterministicInt(rootSeed, label, options.size)
        return options[index]
    }

    private fun deterministicInt(rootSeed: String, label: String, bound: Int): Int {
        if (bound <= 1) return 0
        val value =
            digestBytes(rootSeed, label).take(4).fold(0) { acc, byte ->
                (acc shl 8) or (byte.toInt() and 0xFF)
            }
        return (value and Int.MAX_VALUE) % bound
    }

    private fun deterministicDigits(rootSeed: String, label: String, count: Int): String =
        buildString {
            val bytes = digestBytes(rootSeed, label)
            repeat(count) { index -> append((bytes[index].toInt() and 0xFF) % 10) }
        }

    private fun deterministicHex(rootSeed: String, label: String, byteCount: Int): String =
        digestBytes(rootSeed, label).copyOf(byteCount).joinToString("") { "%02x".format(it) }

    private fun deterministicUuid(rootSeed: String, label: String): UUID {
        val bytes = digestBytes(rootSeed, label).copyOf(16)
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
        var mostSigBits = 0L
        var leastSigBits = 0L
        for (index in 0 until 8) {
            mostSigBits = (mostSigBits shl 8) or (bytes[index].toLong() and 0xFF)
        }
        for (index in 8 until 16) {
            leastSigBits = (leastSigBits shl 8) or (bytes[index].toLong() and 0xFF)
        }
        return UUID(mostSigBits, leastSigBits)
    }

    private fun digestBytes(rootSeed: String, label: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest("$rootSeed|$label".encodeToByteArray())

    private val COUNTRY_TIMEZONES =
        mapOf(
            "US" to listOf("America/New_York", "America/Chicago", "America/Los_Angeles"),
            "CA" to listOf("America/Toronto", "America/Vancouver"),
            "GB" to listOf("Europe/London"),
            "DE" to listOf("Europe/Berlin"),
            "FR" to listOf("Europe/Paris"),
            "IN" to listOf("Asia/Kolkata"),
            "CN" to listOf("Asia/Shanghai"),
            "JP" to listOf("Asia/Tokyo"),
            "AU" to listOf("Australia/Sydney", "Australia/Perth"),
            "KR" to listOf("Asia/Seoul"),
            "BR" to listOf("America/Sao_Paulo"),
            "RU" to listOf("Europe/Moscow", "Asia/Novosibirsk"),
            "MX" to listOf("America/Mexico_City"),
            "ID" to listOf("Asia/Jakarta"),
            "SA" to listOf("Asia/Riyadh"),
            "AE" to listOf("Asia/Dubai"),
        )

    private val COUNTRY_LOCALES =
        mapOf(
            "US" to listOf("en_US", "es_US"),
            "CA" to listOf("en_CA", "fr_CA"),
            "GB" to listOf("en_GB"),
            "DE" to listOf("de_DE"),
            "FR" to listOf("fr_FR"),
            "IN" to listOf("en_IN", "hi_IN"),
            "CN" to listOf("zh_CN"),
            "JP" to listOf("ja_JP"),
            "AU" to listOf("en_AU"),
            "KR" to listOf("ko_KR"),
            "BR" to listOf("pt_BR"),
            "RU" to listOf("ru_RU"),
            "MX" to listOf("es_MX"),
            "ID" to listOf("id_ID"),
            "SA" to listOf("ar_SA"),
            "AE" to listOf("ar_AE", "en_AE"),
        )

    private val COUNTRY_GPS_BOUNDS =
        mapOf(
            "US" to
                listOf(
                    GPSBounds(40.4774, 40.9176, -74.2591, -73.7004),
                    GPSBounds(33.7037, 34.3373, -118.6682, -117.6462),
                ),
            "CA" to listOf(GPSBounds(43.5810, 43.8555, -79.6393, -79.1158)),
            "GB" to listOf(GPSBounds(51.3841, 51.6723, -0.3514, 0.1484)),
            "DE" to listOf(GPSBounds(52.3382, 52.6755, 13.0884, 13.7611)),
            "FR" to listOf(GPSBounds(48.8155, 48.9022, 2.2242, 2.4699)),
            "IN" to
                listOf(
                    GPSBounds(18.8928, 19.2705, 72.7758, 72.9866),
                    GPSBounds(28.4041, 28.8835, 76.8380, 77.3419),
                ),
            "CN" to listOf(GPSBounds(39.7555, 40.0235, 116.1850, 116.5428)),
            "JP" to listOf(GPSBounds(35.5306, 35.8174, 139.4670, 139.9108)),
            "AU" to listOf(GPSBounds(-33.9991, -33.5781, 150.9204, 151.3430)),
            "KR" to listOf(GPSBounds(37.4138, 37.7017, 126.7642, 127.1838)),
            "BR" to listOf(GPSBounds(-23.7494, -23.3569, -46.8755, -46.3654)),
            "RU" to listOf(GPSBounds(55.5699, 55.9116, 37.3193, 37.9451)),
            "MX" to listOf(GPSBounds(19.2040, 19.5929, -99.3647, -98.9454)),
            "ID" to listOf(GPSBounds(-6.3702, -6.0873, 106.6829, 107.0200)),
            "SA" to listOf(GPSBounds(24.5501, 24.8677, 46.5420, 46.8571)),
            "AE" to listOf(GPSBounds(25.0657, 25.3589, 55.0976, 55.4023)),
        )

    private val MANUFACTURER_OUIS =
        mapOf(
            "samsung" to listOf("00:16:32", "78:AB:BB"),
            "google" to listOf("3C:5A:B4", "94:EB:2C"),
            "xiaomi" to listOf("04:CF:8C", "34:CE:00"),
            "oneplus" to listOf("02:00:00", "06:00:00"),
            "sony" to listOf("00:1A:11", "3C:2E:F9"),
            "nothing" to listOf("02:00:00", "0A:00:00"),
            "generic" to listOf("02:00:00", "06:00:00", "0A:00:00"),
        )

    private val COUNTRY_PHONE_LENGTH =
        mapOf(
            "US" to 10,
            "CA" to 10,
            "GB" to 10,
            "DE" to 10,
            "FR" to 9,
            "IN" to 10,
            "CN" to 11,
            "JP" to 10,
            "AU" to 9,
            "KR" to 10,
            "BR" to 11,
            "RU" to 10,
            "MX" to 10,
            "ID" to 10,
            "SA" to 9,
            "AE" to 9,
        )

    private val DEFAULT_GPS_BOUNDS = GPSBounds(-33.8688, -33.7, 151.0, 151.3)
    private const val DEFAULT_PHONE_LENGTH = 10
}
