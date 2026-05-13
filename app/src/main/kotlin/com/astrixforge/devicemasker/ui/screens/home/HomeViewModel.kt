package com.astrixforge.devicemasker.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.common.AppConfig
import com.astrixforge.devicemasker.common.enabledCount
import com.astrixforge.devicemasker.data.XposedPrefs
import com.astrixforge.devicemasker.data.repository.ISpoofRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
        viewModelScope.launch {
            combine(
                    isXposedActiveFlow,
                    repository.moduleEnabled,
                    repository.groups,
                    repository.activeGroup,
                    repository.appConfigs,
                ) { connected, moduleEnabled, groups, activeGroup, appConfigs ->
                    val selectedGroup =
                        activeGroup ?: groups.find { it.isDefault } ?: groups.firstOrNull()
                    HomeState(
                        isXposedActive = connected,
                        isModuleEnabled = moduleEnabled,
                        groups = groups.toImmutableList(),
                        appConfigs = appConfigs.toImmutableMap(),
                        selectedGroup = selectedGroup,
                        maskedIdentifiersCount = selectedGroup?.enabledCount() ?: 0,
                        enabledAppsCount =
                            if (selectedGroup?.isEnabled == true) {
                                appConfigs.countAssignedToGroup(selectedGroup.id)
                            } else 0,
                        isLoading = false,
                    )
                }
                .collect { homeState -> _state.value = homeState }
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
                            currentState.appConfigs.countAssignedToGroup(group.id)
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

private fun Map<String, AppConfig>.countAssignedToGroup(groupId: String): Int =
    values.count { it.groupId == groupId && it.isEnabled }
