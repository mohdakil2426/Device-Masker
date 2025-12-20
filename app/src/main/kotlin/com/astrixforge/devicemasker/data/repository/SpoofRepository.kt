package com.astrixforge.devicemasker.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.astrixforge.devicemasker.common.SpoofProfile
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.generators.FingerprintGenerator
import com.astrixforge.devicemasker.common.generators.ICCIDGenerator
import com.astrixforge.devicemasker.common.generators.IMEIGenerator
import com.astrixforge.devicemasker.common.generators.IMSIGenerator
import com.astrixforge.devicemasker.common.generators.MACGenerator
import com.astrixforge.devicemasker.common.generators.SerialGenerator
import com.astrixforge.devicemasker.common.generators.UUIDGenerator
import com.astrixforge.devicemasker.service.ConfigManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Main repository combining all spoof-related data operations.
 *
 * HMA-OSS Architecture: This repository now wraps ConfigManager and provides
 * the same API to the UI while using JsonConfig as the backing store.
 *
 * @param context Application context (for legacy compatibility)
 */
class SpoofRepository(private val context: Context) {

    /** Repository for installed apps access. */
    val appScopeRepository: AppScopeRepository = AppScopeRepository(context)

    // ═══════════════════════════════════════════════════════════
    // UI STATE FLOWS
    // ═══════════════════════════════════════════════════════════

    /** Flow of module enabled state. */
    val moduleEnabled: Flow<Boolean> = ConfigManager.config.map { it.isModuleEnabled }

    /** Flow of all profiles. */
    val profiles: Flow<List<SpoofProfile>> = ConfigManager.config.map { it.getAllProfiles() }

    /** Flow of the active profile (default profile). */
    val activeProfile: Flow<SpoofProfile?> = ConfigManager.config.map { it.getDefaultProfile() }

    /** Flow of enabled app count. */
    val enabledAppCount: Flow<Int> =
        profiles.map { profileList ->
            profileList.filter { it.isEnabled }.flatMap { it.assignedApps }.distinct().size
        }

    /** Combined UI state flow for dashboard. */
    data class DashboardState(
        val isModuleEnabled: Boolean,
        val activeProfile: SpoofProfile?,
        val enabledAppCount: Int,
        val profileCount: Int,
    )

    val dashboardState: Flow<DashboardState> =
        combine(moduleEnabled, activeProfile, enabledAppCount, profiles) { enabled,
                                                                           profile,
                                                                           appCount,
                                                                           profileList ->
            DashboardState(
                isModuleEnabled = enabled,
                activeProfile = profile,
                enabledAppCount = appCount,
                profileCount = profileList.size,
            )
        }

    // ═══════════════════════════════════════════════════════════
    // MODULE SETTINGS
    // ═══════════════════════════════════════════════════════════

    /** Enables or disables the module globally. */
    suspend fun setModuleEnabled(enabled: Boolean) {
        ConfigManager.setModuleEnabled(enabled)
    }

    /** Sets the active profile by ID (makes it default). */
    suspend fun setActiveProfile(profileId: String) {
        val profile = ConfigManager.getProfile(profileId) ?: return
        // Set this profile as default
        val updatedProfile = profile.copy(isDefault = true)
        ConfigManager.updateProfile(updatedProfile)
        
        // Unset other profiles as default
        ConfigManager.getAllProfiles().forEach { other ->
            if (other.id != profileId && other.isDefault) {
                ConfigManager.updateProfile(other.copy(isDefault = false))
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // VALUE GENERATION
    // ═══════════════════════════════════════════════════════════

    /** Generates a new random value for a spoof type. */
    fun generateValue(type: SpoofType): String {
        return when (type) {
            // Device
            SpoofType.IMEI -> IMEIGenerator.generate()
            SpoofType.MEID -> IMEIGenerator.generate()
            SpoofType.IMSI -> IMSIGenerator.generate()
            SpoofType.SERIAL -> SerialGenerator.generate()
            SpoofType.ICCID -> ICCIDGenerator.generate()
            SpoofType.PHONE_NUMBER -> generatePhoneNumber()

            // Network
            SpoofType.WIFI_MAC -> MACGenerator.generateWiFiMAC()
            SpoofType.BLUETOOTH_MAC -> MACGenerator.generateBluetoothMAC()
            SpoofType.WIFI_SSID -> "\"Network_${(1000..9999).random()}\""
            SpoofType.WIFI_BSSID -> MACGenerator.generate()
            SpoofType.CARRIER_NAME -> listOf("Carrier", "T-Mobile", "Verizon", "AT&T").random()
            SpoofType.CARRIER_MCC_MNC -> "310${(100..999).random()}"

            // Advertising
            SpoofType.ANDROID_ID -> UUIDGenerator.generateAndroidId()
            SpoofType.GSF_ID -> UUIDGenerator.generateGSFId()
            SpoofType.ADVERTISING_ID -> UUIDGenerator.generateAdvertisingId()
            SpoofType.MEDIA_DRM_ID -> UUIDGenerator.generateMediaDrmId()

            // System - Device Profile (returns a random preset ID)
            SpoofType.DEVICE_PROFILE -> 
                com.astrixforge.devicemasker.common.DeviceProfilePreset.PRESETS.random().id

            // Location
            SpoofType.LOCATION_LATITUDE -> String.format(java.util.Locale.US, "%.6f", (-90.0..90.0).random())
            SpoofType.LOCATION_LONGITUDE -> String.format(java.util.Locale.US, "%.6f", (-180.0..180.0).random())
            SpoofType.TIMEZONE ->
                listOf(
                    "America/New_York",
                    "America/Los_Angeles",
                    "Europe/London",
                    "Asia/Tokyo",
                    "Australia/Sydney",
                )
                    .random()

            SpoofType.LOCALE -> listOf("en_US", "en_GB", "ja_JP", "de_DE", "fr_FR").random()
        }
    }

    private fun generatePhoneNumber(): String {
        return "+1${(200..999).random()}${(100..999).random()}${(1000..9999).random()}"
    }

    private fun ClosedFloatingPointRange<Double>.random(): Double {
        return start + (endInclusive - start) * kotlin.random.Random.nextDouble()
    }

    // ═══════════════════════════════════════════════════════════
    // BLOCKING FUNCTIONS (For Hook Context)
    // ═══════════════════════════════════════════════════════════

    /** Gets the active profile (blocking). */
    fun getActiveProfileBlocking(): SpoofProfile? {
        return runBlocking { activeProfile.first() }
    }

    // ═══════════════════════════════════════════════════════════
    // PROFILE MANAGEMENT (For ProfileScreen)
    // ═══════════════════════════════════════════════════════════

    /** Gets all profiles as a Flow. */
    fun getAllProfiles(): Flow<List<SpoofProfile>> = profiles

    /** Creates a new profile with generated spoof values. */
    suspend fun createProfile(name: String, description: String = "") {
        // Create the profile with generated values
        var newProfile = ConfigManager.createProfile(name)
        
        // Add description and initialize with generated values
        var updatedProfile = newProfile.copy(description = description)
        SpoofType.entries.forEach { type ->
            val value = generateValue(type)
            updatedProfile = updatedProfile.withValue(type, value)
        }
        ConfigManager.updateProfile(updatedProfile)
    }

    /** Updates an existing profile. */
    suspend fun updateProfile(profile: SpoofProfile) {
        ConfigManager.updateProfile(profile)
    }

    /** Deletes a profile by ID. */
    suspend fun deleteProfile(profileId: String) {
        ConfigManager.deleteProfile(profileId)
    }

    /** Sets a profile as the default. */
    suspend fun setDefaultProfile(profileId: String) {
        setActiveProfile(profileId)
    }

    /** Sets whether a profile is enabled (master switch for all its apps). */
    suspend fun setProfileEnabled(profileId: String, enabled: Boolean) {
        val profile = ConfigManager.getProfile(profileId) ?: return
        val updatedProfile = profile.withEnabled(enabled)
        ConfigManager.updateProfile(updatedProfile)
    }

    // ═══════════════════════════════════════════════════════════
    // APP ASSIGNMENT (For ProfileDetailScreen)
    // ═══════════════════════════════════════════════════════════

    /** Adds an app to a profile. */
    suspend fun addAppToProfile(profileId: String, packageName: String) {
        val profile = ConfigManager.getProfile(profileId) ?: return
        val updatedProfile = profile.addApp(packageName)
        ConfigManager.updateProfile(updatedProfile)
    }

    /** Removes an app from a profile. */
    suspend fun removeAppFromProfile(profileId: String, packageName: String) {
        val profile = ConfigManager.getProfile(profileId) ?: return
        val updatedProfile = profile.removeApp(packageName)
        ConfigManager.updateProfile(updatedProfile)
    }

    // ═══════════════════════════════════════════════════════════
    // EXPORT / IMPORT
    // ═══════════════════════════════════════════════════════════

    /** Exports all profiles as JSON string. */
    suspend fun exportProfiles(): String {
        return ConfigManager.config.first().toJsonString()
    }

    /** Imports profiles from JSON string. Returns true on success. */
    suspend fun importProfiles(jsonString: String): Boolean {
        return try {
            val config = com.astrixforge.devicemasker.common.JsonConfig.parse(jsonString)
            if (config != null) {
                config.getAllProfiles().forEach { profile ->
                    ConfigManager.updateProfile(profile)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: SpoofRepository? = null

        /** Gets the singleton instance. */
        fun getInstance(context: Context): SpoofRepository {
            return INSTANCE
                ?: synchronized(this) {
                    INSTANCE
                        ?: SpoofRepository(context.applicationContext)
                            .also { INSTANCE = it }
                }
        }
    }
}
