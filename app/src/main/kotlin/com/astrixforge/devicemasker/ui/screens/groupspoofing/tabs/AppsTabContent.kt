package com.astrixforge.devicemasker.ui.screens.groupspoofing.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
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
import com.astrixforge.devicemasker.common.AppConfig
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.ui.components.AppListItem
import com.astrixforge.devicemasker.ui.components.EmptyState
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveLoadingIndicatorWithLabel
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import com.astrixforge.devicemasker.ui.theme.ElevationTokens
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
fun AppsTabContent(
    group: SpoofGroup?,
    allGroups: List<SpoofGroup>,
    appConfigs: Map<String, AppConfig>,
    installedApps: List<InstalledApp>,
    onAppToggle: (InstalledApp, Boolean) -> Unit,
    onRiskyHooksToggle: (String, Boolean) -> Unit,
    onClassLookupToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }.debounce(300).collect { debouncedQuery = it }
    }

    val filteredApps by
        remember(installedApps, group, debouncedQuery) {
            derivedStateOf {
                val query = debouncedQuery.lowercase()
                installedApps
                    .asSequence()
                    .filter { app ->
                        if (app.packageName == BuildConfig.APPLICATION_ID) return@filter false
                        if (app.isSystemApp) return@filter false
                        query.isEmpty() ||
                            app.label.lowercase().contains(query) ||
                            app.packageName.lowercase().contains(query)
                    }
                    .sortedWith(
                        compareByDescending<InstalledApp> {
                                group?.isAppAssigned(it.packageName) == true
                            }
                            .thenBy { it.label.lowercase() }
                    )
                    .toList()
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
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
                        count = filteredApps.size,
                        filteredApps.size,
                        group?.assignedAppCount() ?: 0,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            installedApps.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ExpressiveLoadingIndicatorWithLabel(
                        label = stringResource(id = R.string.group_spoofing_loading_apps)
                    )
                }
            }
            filteredApps.isEmpty() -> {
                EmptyState(
                    icon = Icons.Filled.Search,
                    title = stringResource(id = R.string.group_spoofing_no_apps_found),
                    subtitle = stringResource(id = R.string.group_spoofing_adjust_search),
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName },
                        contentType = { "app_item" },
                    ) { app ->
                        val isAssignedToThisGroup = group?.isAppAssigned(app.packageName) == true
                        val assignedToOtherGroup =
                            allGroups.firstOrNull {
                                it.id != group?.id && it.isAppAssigned(app.packageName)
                            }

                        AppListItem(
                            app = app,
                            isAssigned = isAssignedToThisGroup,
                            assignedToOtherGroupName = assignedToOtherGroup?.name,
                            onToggle = { checked -> onAppToggle(app, checked) },
                            modifier = Modifier.fillMaxWidth().animateItem(),
                        )

                        if (isAssignedToThisGroup) {
                            AppRiskControls(
                                appConfig = appConfigs[app.packageName],
                                packageName = app.packageName,
                                onRiskyHooksToggle = onRiskyHooksToggle,
                                onClassLookupToggle = onClassLookupToggle,
                                modifier = Modifier.fillMaxWidth().animateItem(),
                            )
                        }
                    }

                    item(key = "bottom_spacer", contentType = "spacer") {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRiskControls(
    appConfig: AppConfig?,
    packageName: String,
    onRiskyHooksToggle: (String, Boolean) -> Unit,
    onClassLookupToggle: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val riskyHooksEnabled = appConfig?.riskyHooksEnabled == true
    val classLookupEnabled =
        appConfig?.let { riskyHooksEnabled && it.classLookupHidingEnabled } == true

    Surface(
        modifier = modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(ElevationTokens.Level1),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            RiskControlRow(
                title = stringResource(id = R.string.group_spoofing_risky_hooks),
                subtitle = stringResource(id = R.string.group_spoofing_risky_hooks_desc),
                checked = riskyHooksEnabled,
                onCheckedChange = { checked -> onRiskyHooksToggle(packageName, checked) },
            )
            RiskControlRow(
                title = stringResource(id = R.string.group_spoofing_class_lookup_hiding),
                subtitle = stringResource(id = R.string.group_spoofing_class_lookup_hiding_desc),
                checked = classLookupEnabled,
                enabled = riskyHooksEnabled,
                onCheckedChange = { checked -> onClassLookupToggle(packageName, checked) },
            )
        }
    }
}

@Composable
private fun RiskControlRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppsTabContentLoadingPreview() {
    DeviceMaskerTheme {
        AppsTabContent(
            group = null,
            allGroups = emptyList(),
            appConfigs = emptyMap(),
            installedApps = emptyList(),
            onAppToggle = { _, _ -> },
            onRiskyHooksToggle = { _, _ -> },
            onClassLookupToggle = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppsTabContentPopulatedPreview() {
    DeviceMaskerTheme {
        AppsTabContent(
            group = SpoofGroup.createNew("Preview Group"),
            allGroups = emptyList(),
            appConfigs = emptyMap(),
            installedApps =
                listOf(
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
            onRiskyHooksToggle = { _, _ -> },
            onClassLookupToggle = { _, _ -> },
        )
    }
}
