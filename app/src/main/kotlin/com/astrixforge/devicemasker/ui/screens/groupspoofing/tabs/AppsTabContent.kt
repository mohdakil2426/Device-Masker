package com.astrixforge.devicemasker.ui.screens.groupspoofing.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.BuildConfig
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.common.assignedAppCount
import com.astrixforge.devicemasker.common.isAppAssigned
import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.ui.components.AppListItem
import com.astrixforge.devicemasker.ui.components.EmptyState
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveLoadingIndicatorWithLabel
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
fun AppsTabContent(
    group: SpoofGroup?,
    allGroups: ImmutableList<SpoofGroup>,
    installedApps: ImmutableList<InstalledApp>,
    onAppToggle: (InstalledApp, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(SEARCH_DEBOUNCE_MILLIS)
            .collect { debouncedQuery = it }
    }

    val filteredApps =
        rememberFilteredApps(
            installedApps = installedApps,
            group = group,
            debouncedQuery = debouncedQuery,
        )

    Column(modifier = modifier.fillMaxSize()) {
        AppsSearchHeader(
            searchQuery = searchQuery,
            queryChanged = { searchQuery = it },
            filteredCount = filteredApps.size,
            assignedCount = group?.assignedAppCount() ?: 0,
        )
        AppsTabBody(
            group = group,
            allGroups = allGroups,
            installedApps = installedApps,
            filteredApps = filteredApps,
            onAppToggle = onAppToggle,
        )
    }
}

private const val SEARCH_DEBOUNCE_MILLIS = 300L

@Composable
private fun rememberFilteredApps(
    installedApps: ImmutableList<InstalledApp>,
    group: SpoofGroup?,
    debouncedQuery: String,
): ImmutableList<InstalledApp> {
    val filteredApps by
        remember(installedApps, group, debouncedQuery) {
            derivedStateOf {
                val query = debouncedQuery.lowercase()
                installedApps
                    .asSequence()
                    .filter { app -> app.matchesAppSearch(query) }
                    .sortedWith(
                        compareByDescending<InstalledApp> {
                                group?.isAppAssigned(it.packageName) == true
                            }
                            .thenBy { it.label.lowercase() }
                    )
                    .toList()
                    .toImmutableList()
            }
        }
    return filteredApps
}

private fun InstalledApp.matchesAppSearch(query: String): Boolean =
    packageName != BuildConfig.APPLICATION_ID &&
        !isSystemApp &&
        (query.isEmpty() ||
            label.lowercase().contains(query) ||
            packageName.lowercase().contains(query))

@Composable
private fun AppsSearchHeader(
    searchQuery: String,
    queryChanged: (String) -> Unit,
    filteredCount: Int,
    assignedCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = queryChanged,
            placeholder = { Text(stringResource(id = R.string.group_spoofing_search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
        Text(
            text =
                pluralStringResource(
                    id = R.plurals.group_spoofing_apps_assigned_stats,
                    count = filteredCount,
                    filteredCount,
                    assignedCount,
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppsTabBody(
    group: SpoofGroup?,
    allGroups: ImmutableList<SpoofGroup>,
    installedApps: ImmutableList<InstalledApp>,
    filteredApps: ImmutableList<InstalledApp>,
    onAppToggle: (InstalledApp, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        installedApps.isEmpty() -> LoadingAppsState(modifier = modifier)
        filteredApps.isEmpty() -> EmptyAppsSearchState(modifier = modifier)
        else ->
            AppsList(
                group = group,
                allGroups = allGroups,
                apps = filteredApps,
                onAppToggle = onAppToggle,
                modifier = modifier,
            )
    }
}

@Composable
private fun LoadingAppsState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ExpressiveLoadingIndicatorWithLabel(
            label = stringResource(id = R.string.group_spoofing_loading_apps)
        )
    }
}

@Composable
private fun EmptyAppsSearchState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Filled.Search,
        title = stringResource(id = R.string.group_spoofing_no_apps_found),
        subtitle = stringResource(id = R.string.group_spoofing_adjust_search),
        modifier = modifier,
    )
}

@Composable
private fun AppsList(
    group: SpoofGroup?,
    allGroups: ImmutableList<SpoofGroup>,
    apps: ImmutableList<InstalledApp>,
    onAppToggle: (InstalledApp, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(items = apps, key = { it.packageName }, contentType = { "app_item" }) { app ->
            AppListItem(
                app = app,
                isAssigned = group?.isAppAssigned(app.packageName) == true,
                assignedToOtherGroupName = app.assignedGroupName(group, allGroups),
                onToggle = { checked -> onAppToggle(app, checked) },
                modifier = Modifier.fillMaxWidth().animateItem(),
            )
        }
        item(key = "bottom_spacer", contentType = "spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun InstalledApp.assignedGroupName(
    currentGroup: SpoofGroup?,
    allGroups: ImmutableList<SpoofGroup>,
): String? =
    allGroups.firstOrNull { it.id != currentGroup?.id && it.isAppAssigned(packageName) }?.name

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppsTabContentLoadingPreview() {
    DeviceMaskerTheme {
        AppsTabContent(
            group = null,
            allGroups = persistentListOf(),
            installedApps = persistentListOf(),
            onAppToggle = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppsTabContentPopulatedPreview() {
    DeviceMaskerTheme {
        AppsTabContent(
            group = SpoofGroup.createNew("Preview Group"),
            allGroups = persistentListOf(),
            installedApps =
                persistentListOf(
                    InstalledApp(
                        "com.example.app1",
                        "Example App",
                        isSystemApp = false,
                        versionName = "1.0",
                    ),
                    InstalledApp(
                        "com.example.app2",
                        "Another App",
                        isSystemApp = false,
                        versionName = "2.0",
                    ),
                    InstalledApp(
                        "com.example.system",
                        "System App",
                        isSystemApp = true,
                        versionName = "1.0",
                    ),
                ),
            onAppToggle = { _, _ -> },
        )
    }
}
