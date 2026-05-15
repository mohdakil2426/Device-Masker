package com.astrixforge.devicemasker.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.ui.components.EmptyState
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveCard
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveSwitch
import com.astrixforge.devicemasker.ui.components.expressive.SectionHeader
import com.astrixforge.devicemasker.ui.theme.AppMotion
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Section showing apps currently selected in LSPosed scope. */
@Composable
fun HomeScopedAppsSection(
    scopedApps: ImmutableList<HomeScopedApp>,
    onAppEnabledChange: (HomeScopedApp, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(
            title = stringResource(R.string.home_scoped_apps),
            isExpanded = isExpanded,
            onExpandChange = { isExpanded = it },
        )
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = AppMotion.ReducedIntSize),
            exit = shrinkVertically(animationSpec = AppMotion.ReducedIntSize),
        ) {
            if (scopedApps.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Apps,
                    title = stringResource(R.string.home_scoped_apps_empty),
                    subtitle = stringResource(R.string.home_scoped_apps_empty_subtitle),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    scopedApps.forEach { app ->
                        key(app.packageName) {
                            HomeScopedAppCard(app = app, onAppEnabledChange = onAppEnabledChange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScopedAppCard(
    app: HomeScopedApp,
    onAppEnabledChange: (HomeScopedApp, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabledState = stringResource(R.string.group_app_selected_state)
    val disabledState = stringResource(R.string.group_app_unselected_state)

    ExpressiveCard(
        modifier =
            modifier.fillMaxWidth().semantics {
                role = Role.Switch
                stateDescription = if (app.isGloballyEnabled) enabledState else disabledState
            },
        onClick = { onAppEnabledChange(app, !app.isGloballyEnabled) },
        shape = MaterialTheme.shapes.small,
        containerColor = MaterialTheme.colorScheme.surface,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HomeScopedAppIcon(packageName = app.packageName, label = app.label)
            HomeScopedAppDetails(app = app, modifier = Modifier.weight(1f))
            ExpressiveSwitch(
                checked = app.isGloballyEnabled,
                onCheckedChange = { enabled -> onAppEnabledChange(app, enabled) },
            )
        }
    }
}

@Composable
private fun HomeScopedAppDetails(app: HomeScopedApp, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = app.statusSubtitle(),
            style = MaterialTheme.typography.bodySmall,
            color =
                if (app.status == HomeScopedAppStatus.Enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeScopedApp.statusSubtitle(): String =
    when (status) {
        HomeScopedAppStatus.Enabled -> groupName ?: packageName
        HomeScopedAppStatus.DisabledByApp -> stringResource(R.string.home_scoped_app_disabled)
        HomeScopedAppStatus.DisabledByGroup ->
            stringResource(R.string.home_scoped_app_group_disabled, groupName.orEmpty())
        HomeScopedAppStatus.NotConfigured -> stringResource(R.string.home_scoped_app_not_configured)
    }

@Composable
private fun HomeScopedAppIcon(packageName: String, label: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val iconBitmap by
        produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, packageName) {
            value =
                withContext(Dispatchers.IO) {
                    runCatching {
                            context.packageManager
                                .getApplicationIcon(packageName)
                                .toBitmap(width = APP_ICON_SIZE_PX, height = APP_ICON_SIZE_PX)
                                .asImageBitmap()
                        }
                        .getOrNull()
                }
        }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!,
            contentDescription = label,
            modifier = modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
        )
    } else {
        Box(
            modifier =
                modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private const val APP_ICON_SIZE_PX = 80
