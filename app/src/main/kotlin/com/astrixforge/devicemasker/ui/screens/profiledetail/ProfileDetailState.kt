package com.astrixforge.devicemasker.ui.screens.profiledetail

import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.common.SpoofProfile

/**
 * UI state for the Profile Detail screen.
 */
data class ProfileDetailState(
    val isLoading: Boolean = true,
    val profile: SpoofProfile? = null,
    val profiles: List<SpoofProfile> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    val selectedTab: Int = 0
)
