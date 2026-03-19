package com.astrixforge.devicemasker.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.data.models.SpoofGroup
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Groups list screen.
 *
 * Manages group list state and CRUD operations.
 *
 * @param repository The SpoofRepository for data access
 */
class GroupsViewModel(private val repository: SpoofRepository) : ViewModel() {

    private val _state = MutableStateFlow(GroupsState())
    val state: StateFlow<GroupsState> = _state.asStateFlow()

    init {
        // Collect groups
        viewModelScope.launch {
            repository.getAllGroups().collect { groups ->
                _state.update { it.copy(groups = groups, isLoading = false) }
            }
        }
    }

    fun createGroup(name: String, description: String) {
        viewModelScope.launch { repository.createGroup(name, description) }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch { repository.deleteGroup(groupId) }
    }

    fun setDefaultGroup(groupId: String) {
        viewModelScope.launch { repository.setDefaultGroup(groupId) }
    }

    fun setGroupEnabled(groupId: String, enabled: Boolean) {
        viewModelScope.launch { repository.setGroupEnabled(groupId, enabled) }
    }

    /**
     * Exports groups and returns data via callback. The screen handles file writing via
     * ActivityResultContract.
     */
    fun exportGroups(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.exportGroups() }
                .fold(
                    onSuccess = { jsonData -> onResult(Result.success(jsonData)) },
                    onFailure = { error -> onResult(Result.failure(error)) },
                )
        }
    }

    /**
     * Imports groups from JSON string directly. The screen handles file reading via
     * ActivityResultContract.
     */
    fun importGroups(jsonData: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(repository.importGroups(jsonData)) }
    }

    /** Updates an existing group. */
    fun updateGroup(group: SpoofGroup) {
        viewModelScope.launch { repository.updateGroup(group) }
    }
}
