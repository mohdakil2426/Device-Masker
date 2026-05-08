package com.astrixforge.devicemasker.ui.screens.home

import androidx.compose.runtime.Immutable
import com.astrixforge.devicemasker.data.models.SpoofGroup
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * UI state for the Home screen.
 *
 * Represents all the state needed to render the home screen, collected from the repository flows by
 * the ViewModel.
 */
@Immutable
data class HomeState(
    val isLoading: Boolean = true,
    val isXposedActive: Boolean = false,
    val isModuleEnabled: Boolean = false,
    val groups: ImmutableList<SpoofGroup> = persistentListOf(),
    val selectedGroup: SpoofGroup? = null,
    val enabledAppsCount: Int = 0,
    val maskedIdentifiersCount: Int = 0,
)
