package com.astrixforge.devicemasker.hook

import android.content.Context
import com.astrixforge.devicemasker.data.SpoofDataStore
import com.astrixforge.devicemasker.data.models.GlobalSpoofConfig
import com.astrixforge.devicemasker.data.models.SpoofProfile
import com.astrixforge.devicemasker.data.models.SpoofType
import com.highcapable.yukihookapi.hook.log.YLog
import kotlinx.serialization.json.Json

/**
 * Provides access to spoof data during hook execution.
 *
 * This class handles reading GlobalSpoofConfig and SpoofProfile data from DataStore in a blocking
 * manner suitable for hook operations. It caches the data to minimize I/O during hooks.
 *
 * Usage in hookers:
 * ```kotlin
 * val provider = HookDataProvider.getInstance(context, packageName)
 * if (!provider.isTypeEnabledGlobally(SpoofType.IMEI)) return@hook
 * val value = provider.getSpoofValue(SpoofType.IMEI)
 * ```
 */
class HookDataProvider
private constructor(private val dataStore: SpoofDataStore, private val packageName: String) {
    private val json = Json { ignoreUnknownKeys = true }

    // Cached data - lazily loaded on first access
    private var _globalConfig: GlobalSpoofConfig? = null
    private var _profiles: List<SpoofProfile>? = null
    private var _profileForApp: SpoofProfile? = null
    private var _profileLoaded = false

    /** Gets the GlobalSpoofConfig, loading from DataStore if not cached. */
    val globalConfig: GlobalSpoofConfig
        get() {
            if (_globalConfig == null) {
                _globalConfig = loadGlobalConfig()
            }
            return _globalConfig ?: GlobalSpoofConfig.createDefault()
        }

    /** Gets all profiles, loading from DataStore if not cached. */
    val profiles: List<SpoofProfile>
        get() {
            if (_profiles == null) {
                _profiles = loadProfiles()
            }
            return _profiles ?: emptyList()
        }

    /**
     * Gets the profile to use for the current app.
     *
     * Resolution order:
     * 1. Profile with this app explicitly assigned
     * 2. Default profile (isDefault = true)
     * 3. First profile if any exist
     * 4. Null if no profiles exist
     */
    val profileForApp: SpoofProfile?
        get() {
            if (!_profileLoaded) {
                _profileForApp = resolveProfileForApp()
                _profileLoaded = true
            }
            return _profileForApp
        }

    /**
     * Checks if a spoof type is enabled globally. If disabled globally, the type should not be
     * spoofed for any app.
     */
    fun isTypeEnabledGlobally(type: SpoofType): Boolean {
        return globalConfig.isTypeEnabled(type)
    }

    /**
     * Gets the spoofed value for a type, considering global and profile settings.
     *
     * Returns null if:
     * - Type is disabled globally
     * - Type is disabled in the profile
     * - No value is set
     */
    fun getSpoofValue(type: SpoofType): String? {
        // Check global enabled state first
        if (!isTypeEnabledGlobally(type)) {
            YLog.debug("HookDataProvider: Type $type is disabled globally, skipping")
            return null
        }

        // Get profile for this app
        val profile = profileForApp
        if (profile == null) {
            // No profile, use global default value
            YLog.debug(
                    "HookDataProvider: No profile for $packageName, using global default for $type"
            )
            return globalConfig.getDefaultValue(type)
        }

        // Check if type is enabled in profile
        if (!profile.isTypeEnabled(type)) {
            YLog.debug(
                    "HookDataProvider: Type $type is disabled in profile ${profile.name}, skipping"
            )
            return null
        }

        // Get value from profile
        val value = profile.getValue(type)
        YLog.debug("HookDataProvider: Using profile '${profile.name}' value for $type: $value")
        return value
    }

    /** Gets the profile name being used for this app (for logging). */
    fun getProfileName(): String {
        return profileForApp?.name ?: "No Profile"
    }

    /** Clears cached data, forcing reload on next access. */
    fun invalidateCache() {
        _globalConfig = null
        _profiles = null
        _profileForApp = null
        _profileLoaded = false
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE METHODS
    // ═══════════════════════════════════════════════════════════

    private fun loadGlobalConfig(): GlobalSpoofConfig? {
        return try {
            val jsonString = dataStore.getGlobalConfigJsonBlocking()
            if (jsonString.isNullOrBlank()) {
                YLog.debug("HookDataProvider: No global config found, using defaults")
                GlobalSpoofConfig.createDefault()
            } else {
                json.decodeFromString<GlobalSpoofConfig>(jsonString)
            }
        } catch (e: Exception) {
            YLog.error("HookDataProvider: Failed to load global config: ${e.message}")
            GlobalSpoofConfig.createDefault()
        }
    }

    private fun loadProfiles(): List<SpoofProfile>? {
        return try {
            val jsonString = dataStore.getProfilesJsonBlocking()
            if (jsonString.isNullOrBlank()) {
                YLog.debug("HookDataProvider: No profiles found")
                emptyList()
            } else {
                json.decodeFromString<List<SpoofProfile>>(jsonString)
            }
        } catch (e: Exception) {
            YLog.error("HookDataProvider: Failed to load profiles: ${e.message}")
            emptyList()
        }
    }

    private fun resolveProfileForApp(): SpoofProfile? {
        val allProfiles = profiles
        if (allProfiles.isEmpty()) {
            YLog.debug("HookDataProvider: No profiles available for $packageName")
            return null
        }

        // 1. Find profile with this app explicitly assigned
        val assignedProfile = allProfiles.find { it.isAppAssigned(packageName) }
        if (assignedProfile != null) {
            YLog.info(
                    "HookDataProvider: Using assigned profile '${assignedProfile.name}' for $packageName"
            )
            return assignedProfile
        }

        // 2. Use default profile
        val defaultProfile = allProfiles.find { it.isDefault }
        if (defaultProfile != null) {
            YLog.debug(
                    "HookDataProvider: Using default profile '${defaultProfile.name}' for $packageName (no explicit assignment)"
            )
            return defaultProfile
        }

        // 3. Use first profile as fallback
        val firstProfile = allProfiles.firstOrNull()
        if (firstProfile != null) {
            YLog.debug(
                    "HookDataProvider: Using first profile '${firstProfile.name}' for $packageName (no default)"
            )
            return firstProfile
        }

        return null
    }

    companion object {
        @Volatile private var instances = mutableMapOf<String, HookDataProvider>()

        /**
         * Gets or creates a HookDataProvider for the given context and package.
         *
         * @param context Android context for DataStore access
         * @param packageName The package name of the app being hooked
         */
        fun getInstance(context: Context, packageName: String): HookDataProvider {
            return instances.getOrPut(packageName) {
                val dataStore = SpoofDataStore(context)
                HookDataProvider(dataStore, packageName)
            }
        }

        /** Clears all cached providers. Call when data changes externally. */
        fun invalidateAll() {
            instances.values.forEach { it.invalidateCache() }
        }
    }
}
