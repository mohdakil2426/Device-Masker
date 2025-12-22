package com.astrixforge.devicemasker.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.data.models.SpoofProfile
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the Profile list screen.
 *
 * Manages profile list state and CRUD operations.
 *
 * @param repository The SpoofRepository for data access
 */
class ProfileViewModel(
    private val repository: SpoofRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        // Collect profiles
        viewModelScope.launch {
            repository.getAllProfiles().collect { profiles ->
                _state.update { it.copy(
                    profiles = profiles,
                    isLoading = false
                ) }
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            delay(1000) // Simulate refresh
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun showCreateDialog() {
        _state.update { it.copy(showCreateDialog = true) }
    }

    fun hideCreateDialog() {
        _state.update { it.copy(showCreateDialog = false) }
    }

    fun showEditDialog(profile: SpoofProfile) {
        _state.update { it.copy(showEditDialog = profile) }
    }

    fun hideEditDialog() {
        _state.update { it.copy(showEditDialog = null) }
    }

    fun showDeleteDialog(profile: SpoofProfile) {
        _state.update { it.copy(showDeleteDialog = profile) }
    }

    fun hideDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = null) }
    }

    fun createProfile(name: String, description: String) {
        viewModelScope.launch {
            repository.createProfile(name, description)
        }
        hideCreateDialog()
    }

    fun updateProfile(profile: SpoofProfile, name: String, description: String) {
        viewModelScope.launch {
            repository.updateProfile(
                profile.copy(
                    name = name,
                    description = description,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        hideEditDialog()
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            repository.deleteProfile(profileId)
        }
        hideDeleteDialog()
    }

    fun setDefaultProfile(profileId: String) {
        viewModelScope.launch {
            repository.setDefaultProfile(profileId)
        }
    }

    fun setProfileEnabled(profileId: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.setProfileEnabled(profileId, enabled)
        }
    }

    fun exportProfiles(context: Context, uri: Uri) {
        viewModelScope.launch {
            val jsonData = repository.exportProfiles()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonData.toByteArray())
                }
            }
        }
    }

    /**
     * Exports profiles and returns data via callback.
     * Useful when the screen needs to handle file writing.
     */
    fun exportProfiles(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val jsonData = repository.exportProfiles()
            onResult(jsonData)
        }
    }

    fun importProfiles(context: Context, uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonData = inputStream.bufferedReader().readText()
                    repository.importProfiles(jsonData)
                }
            }
        }
    }

    /**
     * Imports profiles from JSON string directly.
     * Useful when the screen handles file reading.
     */
    fun importProfiles(jsonData: String) {
        viewModelScope.launch {
            repository.importProfiles(jsonData)
        }
    }

    /**
     * Updates an existing profile.
     */
    fun updateProfile(profile: SpoofProfile) {
        viewModelScope.launch {
            repository.updateProfile(profile)
        }
    }
}
