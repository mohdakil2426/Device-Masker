package com.astrixforge.devicemasker.ui.screens.profile

import com.astrixforge.devicemasker.data.models.SpoofProfile

/**
 * UI state for the Profile list screen.
 */
data class ProfileState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val profiles: List<SpoofProfile> = emptyList(),
    val showCreateDialog: Boolean = false,
    val showEditDialog: SpoofProfile? = null,
    val showDeleteDialog: SpoofProfile? = null
)
