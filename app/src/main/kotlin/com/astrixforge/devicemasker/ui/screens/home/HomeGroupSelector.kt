package com.astrixforge.devicemasker.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.common.assignedAppCount
import com.astrixforge.devicemasker.data.models.SpoofGroup
import com.astrixforge.devicemasker.ui.components.IconCircle
import com.astrixforge.devicemasker.ui.components.expressive.CompactExpressiveIconButton
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun GroupSelectorHeader(
    selectedGroup: SpoofGroup?,
    rotationAngle: Float,
    onViewGroup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconCircle(
            icon = Icons.Filled.Groups,
            size = 48.dp,
            iconSize = 24.dp,
            contentDescription = stringResource(id = R.string.home_active_group_label),
        )
        Spacer(modifier = Modifier.width(16.dp))
        SelectedGroupText(selectedGroup = selectedGroup, modifier = Modifier.weight(1f))
        GroupSelectorActions(rotationAngle = rotationAngle, onViewGroup = onViewGroup)
    }
}

@Composable
private fun SelectedGroupText(selectedGroup: SpoofGroup?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.home_active_group_label),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = selectedGroup?.name ?: stringResource(id = R.string.home_no_group),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (selectedGroup?.isEnabled == false) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.home_group_disabled_tag),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun GroupSelectorActions(rotationAngle: Float, onViewGroup: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.home_select_group),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(rotationAngle),
        )
        CompactExpressiveIconButton(
            onClick = onViewGroup,
            icon = Icons.Outlined.Visibility,
            contentDescription = stringResource(R.string.home_view_group),
        )
    }
}

@Composable
internal fun GroupDropdownMenu(
    groups: ImmutableList<SpoofGroup>,
    selectedGroup: SpoofGroup?,
    expanded: Boolean,
    groupSelected: (SpoofGroup) -> Unit,
    dismiss: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = dismiss,
        modifier = Modifier.fillMaxWidth(DROPDOWN_WIDTH_FRACTION),
    ) {
        if (groups.isEmpty()) {
            EmptyGroupMenuItem(dismiss)
        } else {
            groups.forEach { group ->
                GroupMenuItem(
                    group = group,
                    isSelected = group.id == selectedGroup?.id,
                    onClick = {
                        groupSelected(group)
                        dismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun EmptyGroupMenuItem(dismiss: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(id = R.string.home_no_groups_available)) },
        onClick = dismiss,
        enabled = false,
    )
}

@Composable
private fun GroupMenuItem(group: SpoofGroup, isSelected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                GroupMenuText(group = group, isSelected = isSelected)
                GroupMenuState(group = group, isSelected = isSelected)
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun GroupMenuText(group: SpoofGroup, isSelected: Boolean) {
    Column {
        Text(text = group.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        Text(
            text =
                pluralStringResource(
                    id = R.plurals.home_apps_count,
                    count = group.assignedAppCount(),
                    group.assignedAppCount(),
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GroupMenuState(group: SpoofGroup, isSelected: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!group.isEnabled) {
            Text(
                text = stringResource(id = R.string.home_group_disabled_tag).trim('(', ')'),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.home_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private const val DROPDOWN_WIDTH_FRACTION = 0.9f
