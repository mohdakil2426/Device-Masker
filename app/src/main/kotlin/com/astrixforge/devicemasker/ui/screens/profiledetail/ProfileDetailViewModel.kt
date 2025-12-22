package com.astrixforge.devicemasker.ui.screens.profiledetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.common.CorrelationGroup
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.models.Carrier
import com.astrixforge.devicemasker.data.models.DeviceIdentifier
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Profile Detail screen.
 *
 * Manages profile detail state and spoof value operations.
 *
 * @param repository The SpoofRepository for data access
 * @param profileId The ID of the profile to display
 */
class ProfileDetailViewModel(
    private val repository: SpoofRepository,
    private val profileId: String
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileDetailState())
    val state: StateFlow<ProfileDetailState> = _state.asStateFlow()

    init {
        // Collect profiles
        viewModelScope.launch {
            repository.profiles.collect { profiles ->
                val profile = profiles.find { it.id == profileId }
                _state.update { it.copy(
                    profiles = profiles,
                    profile = profile,
                    isLoading = false
                ) }
            }
        }

        // Collect installed apps
        viewModelScope.launch {
            repository.appScopeRepository.getInstalledAppsFlow().collect { apps ->
                _state.update { it.copy(installedApps = apps) }
            }
        }
    }

    fun setSelectedTab(tab: Int) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun regenerateValue(type: SpoofType) {
        val profile = state.value.profile ?: return
        viewModelScope.launch {
            val correlationGroup = type.correlationGroup
            
            // For SIM values: use regenerateSIMValueOnly to keep same carrier
            val newValue = when (correlationGroup) {
                CorrelationGroup.SIM_CARD -> repository.regenerateSIMValueOnly(type)
                else -> {
                    // For non-SIM correlated values, reset cache first
                    if (correlationGroup != CorrelationGroup.NONE) {
                        repository.resetCorrelationGroup(correlationGroup)
                    }
                    repository.generateValue(type)
                }
            }
            
            val updated = profile.withValue(type, newValue)
            repository.updateProfile(updated)
        }
    }

    fun regenerateCategory(types: List<SpoofType>, isCorrelated: Boolean) {
        val profile = state.value.profile ?: return
        viewModelScope.launch {
            // Reset the cache for this correlation group first
            if (isCorrelated) {
                val correlationGroup = types.firstOrNull()?.correlationGroup
                if (correlationGroup != null) {
                    repository.resetCorrelationGroup(correlationGroup)
                }
            }
            
            // Now regenerate all types in this category
            var updatedProfile = profile
            types.forEach { type ->
                val newValue = repository.generateValue(type)
                updatedProfile = updatedProfile.withValue(type, newValue)
            }
            repository.updateProfile(updatedProfile)
        }
    }

    fun toggleSpoofType(type: SpoofType, enabled: Boolean) {
        val profile = state.value.profile ?: return
        viewModelScope.launch {
            val identifier = profile.getIdentifier(type) ?: DeviceIdentifier.createDefault(type)
            val updated = profile.withIdentifier(identifier.copy(isEnabled = enabled))
            repository.updateProfile(updated)
        }
    }

    fun regenerateLocation() {
        val profile = state.value.profile ?: return
        viewModelScope.launch {
            repository.regenerateLocationValues(profile.id)
        }
    }

    fun updateCarrier(carrier: Carrier) {
        val profile = state.value.profile ?: return
        viewModelScope.launch {
            repository.updateProfileWithCarrier(profile.id, carrier)
        }
    }

    fun addAppToProfile(packageName: String) {
        viewModelScope.launch {
            repository.addAppToProfile(profileId, packageName)
        }
    }

    fun removeAppFromProfile(packageName: String) {
        viewModelScope.launch {
            repository.removeAppFromProfile(profileId, packageName)
        }
    }
}
