package com.astrixforge.devicemasker.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.data.XposedPrefs
import com.astrixforge.devicemasker.data.repository.ISpoofRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 *
 * Manages the UI state by collecting from repository flows and provides action methods for UI
 * events.
 *
 * @param repository The [ISpoofRepository] for data access
 * @param isXposedActiveFlow Flow that emits Xposed service connection state
 */
class HomeViewModel(
    private val repository: ISpoofRepository,
    private val isXposedActiveFlow: StateFlow<Boolean> = XposedPrefs.isServiceConnected,
    @Suppress("unused") private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        // Set initial Xposed status
        _state.update { it.copy(isXposedActive = isXposedActiveFlow.value) }

        viewModelScope.launch {
            isXposedActiveFlow.collect { connected ->
                _state.update { it.copy(isXposedActive = connected) }
            }
        }

        // Collect groups
        viewModelScope.launch {
            repository.groups.collect { groups ->
                _state.update { currentState ->
                    // Find the default group from the fresh list
                    val defaultGroup = groups.find { it.isDefault }

                    // Use default group (synced), or fallback to first
                    val selectedGroup = defaultGroup ?: groups.firstOrNull()

                    currentState.copy(
                        groups = groups.toImmutableList(),
                        selectedGroup = selectedGroup,
                        maskedIdentifiersCount = selectedGroup?.enabledCount() ?: 0,
                        enabledAppsCount =
                            if (selectedGroup?.isEnabled == true) {
                                selectedGroup.assignedAppCount()
                            } else 0,
                        isLoading = false,
                    )
                }
            }
        }

        // Collect active group changes (when default changes)
        viewModelScope.launch {
            repository.activeGroup.collect { activeGroup ->
                _state.update { currentState ->
                    // Always sync selectedGroup with the active (default) group
                    val selectedGroup = activeGroup ?: currentState.groups.firstOrNull()

                    currentState.copy(
                        selectedGroup = selectedGroup,
                        enabledAppsCount =
                            if (selectedGroup?.isEnabled == true) {
                                selectedGroup.assignedAppCount()
                            } else 0,
                        maskedIdentifiersCount = selectedGroup?.enabledCount() ?: 0,
                    )
                }
            }
        }

        // Collect module enabled state
        viewModelScope.launch {
            repository.moduleEnabled.collect { isEnabled ->
                _state.update { it.copy(isModuleEnabled = isEnabled) }
            }
        }
    }

    /** Toggle module enabled/disabled state. */
    fun setModuleEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setModuleEnabled(enabled) }
    }

    /** Select a group from the dropdown. */
    fun selectGroup(groupId: String) {
        viewModelScope.launch {
            repository.setActiveGroup(groupId)
            _state.update { currentState ->
                val group = currentState.groups.find { it.id == groupId }
                currentState.copy(
                    selectedGroup = group,
                    enabledAppsCount =
                        if (group?.isEnabled == true) {
                            group.assignedAppCount()
                        } else 0,
                    maskedIdentifiersCount = group?.enabledCount() ?: 0,
                )
            }
        }
    }

    /**
     * Regenerate all values for the selected group.
     *
     * @param onComplete Callback invoked after regeneration starts
     */
    fun regenerateAll(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            state.value.selectedGroup?.let { group ->
                repository.setActiveGroup(group.id)
                repository.regenerateAllValues(group.id)
            }
            onComplete()
        }
    }
}
