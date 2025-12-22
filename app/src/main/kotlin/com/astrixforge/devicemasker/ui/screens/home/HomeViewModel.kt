package com.astrixforge.devicemasker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.DeviceMaskerApp
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 *
 * Manages the UI state by collecting from repository flows and
 * provides action methods for UI events.
 *
 * @param repository The SpoofRepository for data access
 */
class HomeViewModel(
    private val repository: SpoofRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        // Set initial Xposed status
        _state.update { it.copy(isXposedActive = DeviceMaskerApp.isXposedModuleActive) }

        // Collect profiles
        viewModelScope.launch {
            repository.profiles.collect { profiles ->
                _state.update { currentState ->
                    val selectedProfile = currentState.selectedProfile
                        ?: profiles.find { it.isDefault }
                        ?: profiles.firstOrNull()
                    
                    currentState.copy(
                        profiles = profiles,
                        selectedProfile = selectedProfile,
                        maskedIdentifiersCount = selectedProfile?.enabledCount() ?: 0,
                        enabledAppsCount = if (selectedProfile?.isEnabled == true) {
                            selectedProfile.assignedAppCount()
                        } else 0,
                        isLoading = false
                    )
                }
            }
        }

        // Collect dashboard state
        viewModelScope.launch {
            repository.dashboardState.collect { dashboard ->
                _state.update { currentState ->
                    // If we have an active profile from dashboard and no selection yet, use it
                    val selectedProfile = currentState.selectedProfile
                        ?: dashboard.activeProfile
                    
                    currentState.copy(
                        isModuleEnabled = dashboard.isModuleEnabled,
                        selectedProfile = selectedProfile,
                        enabledAppsCount = if (selectedProfile?.isEnabled == true) {
                            selectedProfile.assignedAppCount()
                        } else 0,
                        maskedIdentifiersCount = selectedProfile?.enabledCount() ?: 0
                    )
                }
            }
        }
    }

    /**
     * Toggle module enabled/disabled state.
     */
    fun setModuleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setModuleEnabled(enabled)
        }
    }

    /**
     * Select a profile from the dropdown.
     */
    fun selectProfile(profileId: String) {
        viewModelScope.launch {
            repository.setActiveProfile(profileId)
            // Update local state immediately for responsiveness
            _state.update { currentState ->
                val profile = currentState.profiles.find { it.id == profileId }
                currentState.copy(
                    selectedProfile = profile,
                    enabledAppsCount = if (profile?.isEnabled == true) {
                        profile.assignedAppCount()
                    } else 0,
                    maskedIdentifiersCount = profile?.enabledCount() ?: 0
                )
            }
        }
    }

    /**
     * Regenerate all values for the selected profile.
     * 
     * @param onComplete Callback invoked after regeneration starts
     */
    fun regenerateAll(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            state.value.selectedProfile?.let { profile ->
                repository.setActiveProfile(profile.id)
            }
            onComplete()
        }
    }
}
