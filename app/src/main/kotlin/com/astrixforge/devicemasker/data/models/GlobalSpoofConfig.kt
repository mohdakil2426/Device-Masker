package com.astrixforge.devicemasker.data.models

import kotlinx.serialization.Serializable

/**
 * Global configuration for spoof settings.
 *
 * This configuration acts as the master control layer for all profiles:
 * - `enabledTypes`: Master switches that control whether a spoof type is active globally.
 *   When a type is disabled here, NO profile can spoof that type.
 * - `defaultValues`: Template values used when creating new profiles.
 *   Existing profiles are NOT affected when these values change.
 *
 * @property enabledTypes Set of SpoofTypes that are globally enabled (master switches)
 * @property defaultValues Map of SpoofType to default value strings (template for new profiles)
 * @property updatedAt Timestamp of last modification (epoch millis)
 */
@Serializable
data class GlobalSpoofConfig(
    val enabledTypes: Set<SpoofType> = SpoofType.entries.toSet(),
    val defaultValues: Map<SpoofType, String> = emptyMap(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks if a specific SpoofType is enabled globally.
     * When disabled globally, no profile can spoof this type.
     *
     * @param type The spoof type to check
     * @return True if the type is enabled globally
     */
    fun isTypeEnabled(type: SpoofType): Boolean {
        return type in enabledTypes
    }

    /**
     * Gets the default value for a specific SpoofType.
     * This value is used as a template when creating new profiles.
     *
     * @param type The spoof type to get the default value for
     * @return The default value, or null if not set
     */
    fun getDefaultValue(type: SpoofType): String? {
        return defaultValues[type]
    }

    /**
     * Creates a copy with a type toggled enabled/disabled.
     *
     * @param type The spoof type to toggle
     * @return Updated GlobalSpoofConfig
     */
    fun withTypeToggled(type: SpoofType): GlobalSpoofConfig {
        val newEnabledTypes = enabledTypes.toMutableSet()
        if (type in newEnabledTypes) {
            newEnabledTypes.remove(type)
        } else {
            newEnabledTypes.add(type)
        }
        return copy(enabledTypes = newEnabledTypes, updatedAt = System.currentTimeMillis())
    }

    /**
     * Creates a copy with a type explicitly enabled or disabled.
     *
     * @param type The spoof type to update
     * @param enabled Whether to enable or disable the type
     * @return Updated GlobalSpoofConfig
     */
    fun withTypeEnabled(type: SpoofType, enabled: Boolean): GlobalSpoofConfig {
        val newEnabledTypes = enabledTypes.toMutableSet()
        if (enabled) {
            newEnabledTypes.add(type)
        } else {
            newEnabledTypes.remove(type)
        }
        return copy(enabledTypes = newEnabledTypes, updatedAt = System.currentTimeMillis())
    }

    /**
     * Creates a copy with an updated default value for a type.
     *
     * @param type The spoof type to update
     * @param value The new default value (null to remove)
     * @return Updated GlobalSpoofConfig
     */
    fun withDefaultValue(type: SpoofType, value: String?): GlobalSpoofConfig {
        val newDefaultValues = defaultValues.toMutableMap()
        if (value != null) {
            newDefaultValues[type] = value
        } else {
            newDefaultValues.remove(type)
        }
        return copy(defaultValues = newDefaultValues, updatedAt = System.currentTimeMillis())
    }

    /**
     * Gets the count of enabled types.
     */
    fun enabledCount(): Int = enabledTypes.size

    /**
     * Gets the count of disabled types.
     */
    fun disabledCount(): Int = SpoofType.entries.size - enabledTypes.size

    /**
     * Gets all enabled types for a specific category.
     */
    fun getEnabledTypesForCategory(category: SpoofCategory): List<SpoofType> {
        return SpoofType.byCategory(category).filter { it in enabledTypes }
    }

    /**
     * Checks if all types in a category are enabled.
     */
    fun isCategoryFullyEnabled(category: SpoofCategory): Boolean {
        return SpoofType.byCategory(category).all { it in enabledTypes }
    }

    /**
     * Checks if any types in a category are enabled.
     */
    fun isCategoryPartiallyEnabled(category: SpoofCategory): Boolean {
        val categoryTypes = SpoofType.byCategory(category)
        val enabledInCategory = categoryTypes.count { it in enabledTypes }
        return enabledInCategory > 0 && enabledInCategory < categoryTypes.size
    }

    companion object {
        /**
         * Creates a default GlobalSpoofConfig with all types enabled and no default values.
         */
        fun createDefault(): GlobalSpoofConfig {
            return GlobalSpoofConfig(
                enabledTypes = SpoofType.entries.toSet(),
                defaultValues = emptyMap(),
                updatedAt = System.currentTimeMillis()
            )
        }

        /**
         * Creates a GlobalSpoofConfig with all types enabled and populated default values.
         * The `valueGenerator` function is called for each type to generate a default value.
         *
         * @param valueGenerator Function that generates a value for each SpoofType
         * @return GlobalSpoofConfig with generated default values
         */
        fun createWithDefaults(valueGenerator: (SpoofType) -> String): GlobalSpoofConfig {
            val defaultValues = SpoofType.entries.associateWith { valueGenerator(it) }
            return GlobalSpoofConfig(
                enabledTypes = SpoofType.entries.toSet(),
                defaultValues = defaultValues,
                updatedAt = System.currentTimeMillis()
            )
        }
    }
}
