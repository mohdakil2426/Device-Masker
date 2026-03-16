package com.astrixforge.devicemasker.ui.screens.groups

import com.astrixforge.devicemasker.data.models.SpoofGroup

/** UI state for the Groups list screen. */
data class GroupsState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val groups: List<SpoofGroup> = emptyList(),
)
