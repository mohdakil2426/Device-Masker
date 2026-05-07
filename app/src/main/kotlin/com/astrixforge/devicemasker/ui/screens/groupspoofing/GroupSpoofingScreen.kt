package com.astrixforge.devicemasker.ui.screens.groupspoofing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveLoadingIndicator
import com.astrixforge.devicemasker.ui.screens.groupspoofing.tabs.AppsTabContent
import com.astrixforge.devicemasker.ui.screens.groupspoofing.tabs.SpoofTabContent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Group Spoofing Screen with tabbed interface.
 *
 * Shows group-specific spoof values and assigned apps in a two-tab layout:
 * - Tab 0: Spoof Values - Configure per-group spoof values
 * - Tab 1: Apps - Assign apps to this group
 *
 * Uses MVVM architecture with ViewModel for state management.
 *
 * @param viewModel The GroupSpoofingViewModel for state management
 * @param onNavigateBack Callback to navigate back
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("FunctionNaming", "ViewModelForwarding")
fun GroupSpoofingScreenContent(
    viewModel: GroupSpoofingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val group = state.group
    val groups = state.groups
    val installedApps = state.installedApps

    val selectedTab = state.selectedTab
    val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(group) {
        if (group == null) {
            onNavigateBack()
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                if (page != selectedTab) {
                    viewModel.setSelectedTab(page)
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (group == null) {
            ExpressiveLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                GroupSpoofingHeader(groupName = group.name, onNavigateBack = onNavigateBack)
                GroupSpoofingTabs(pagerState = pagerState, coroutineScope = coroutineScope)
                GroupSpoofingPager(
                    pagerState = pagerState,
                    group = group,
                    groups = groups,
                    installedApps = installedApps,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
private fun GroupSpoofingHeader(groupName: String, onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.group_spoofing_back),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = groupName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
@Suppress("FunctionNaming")
private fun GroupSpoofingTabs(
    pagerState: androidx.compose.foundation.pager.PagerState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
) {
    SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
        Tab(
            selected = pagerState.currentPage == 0,
            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
            text = { Text(stringResource(id = R.string.group_spoofing_tab_spoof)) },
            icon = { Icon(Icons.Filled.Tune, contentDescription = null) },
        )
        Tab(
            selected = pagerState.currentPage == 1,
            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
            text = { Text(stringResource(id = R.string.group_spoofing_tab_apps)) },
            icon = { Icon(Icons.Filled.Apps, contentDescription = null) },
        )
    }
}

@Composable
@Suppress("FunctionNaming", "ViewModelForwarding")
private fun GroupSpoofingPager(
    pagerState: androidx.compose.foundation.pager.PagerState,
    group: com.astrixforge.devicemasker.common.SpoofGroup,
    groups: List<com.astrixforge.devicemasker.common.SpoofGroup>,
    installedApps: List<InstalledApp>,
    viewModel: GroupSpoofingViewModel,
) {
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        when (page) {
            0 ->
                SpoofTabContent(
                    group = group,
                    onRegenerate = { type -> viewModel.regenerateValue(type) },
                    onRegenerateCategory = { category ->
                        viewModel.regenerateCategory(category.types, category.isCorrelated)
                    },
                    onToggle = { type, enabled -> viewModel.toggleSpoofType(type, enabled) },
                    onRegenerateLocation = { viewModel.regenerateLocation() },
                    onCarrierChange = { carrier -> viewModel.updateCarrier(carrier) },
                    onTimezoneSelected = { timezone -> viewModel.updateTimezone(timezone) },
                )

            1 ->
                AppsTabContent(
                    group = group,
                    allGroups = groups,
                    installedApps = installedApps,
                    onAppToggle = { app, checked ->
                        if (checked) {
                            viewModel.addAppToGroup(app.packageName)
                        } else {
                            viewModel.removeAppFromGroup(app.packageName)
                        }
                    },
                )
        }
    }
}
