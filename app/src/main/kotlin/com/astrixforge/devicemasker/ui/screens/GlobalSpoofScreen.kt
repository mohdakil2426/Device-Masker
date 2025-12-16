package com.astrixforge.devicemasker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.data.models.GlobalSpoofConfig
import com.astrixforge.devicemasker.data.models.SpoofCategory
import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.ui.theme.AppMotion
import kotlinx.coroutines.launch

/**
 * Global Spoof Screen for managing master switches and default values.
 *
 * This screen provides:
 * - Master ON/OFF toggles for each spoof type (affects all profiles)
 * - Default values that are copied when creating new profiles
 * - Regenerate buttons for default values
 *
 * When a type is disabled here, it will be disabled across ALL profiles.
 *
 * @param repository The SpoofRepository for data access
 * @param modifier Optional modifier
 */
@Composable
fun GlobalSpoofScreen(repository: SpoofRepository, modifier: Modifier = Modifier) {
    val globalConfig by
            repository.globalConfig.collectAsState(initial = GlobalSpoofConfig.createDefault())
    val scope = rememberCoroutineScope()

    GlobalSpoofContent(
            globalConfig = globalConfig,
            onToggleType = { type, enabled ->
                scope.launch { repository.setTypeEnabledGlobally(type, enabled) }
            },
            onRegenerateDefault = { type ->
                scope.launch {
                    val newValue = repository.generateValue(type)
                    repository.setGlobalDefaultValue(type, newValue)
                }
            },
            modifier = modifier
    )
}

/** Stateless content for GlobalSpoofScreen. */
@Composable
fun GlobalSpoofContent(
        globalConfig: GlobalSpoofConfig,
        onToggleType: (SpoofType, Boolean) -> Unit,
        onRegenerateDefault: (SpoofType) -> Unit,
        modifier: Modifier = Modifier
) {
    LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Text(
                    text = "Global Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Info card explaining behavior
        item { GlobalInfoCard() }

        // Categories
        SpoofCategory.entries.forEach { category ->
            item(key = category.name) {
                GlobalCategorySection(
                        category = category,
                        globalConfig = globalConfig,
                        onToggleType = onToggleType,
                        onRegenerateDefault = onRegenerateDefault
                )
            }
        }

        // Bottom spacing
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

/** Info card explaining global settings behavior. */
@Composable
private fun GlobalInfoCard() {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.cardColors(
                            containerColor =
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
            shape = MaterialTheme.shapes.medium
    ) {
        Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
        ) {
            Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                        text = "Master Switches",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                        text =
                                "Toggle switches control which spoof types are active across all profiles. When disabled, the type will be hidden from all profiles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text = "Default values are used as templates when creating new profiles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/** Expandable category section for global settings. */
@Composable
private fun GlobalCategorySection(
        category: SpoofCategory,
        globalConfig: GlobalSpoofConfig,
        onToggleType: (SpoofType, Boolean) -> Unit,
        onRegenerateDefault: (SpoofType) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val rotationAngle by
            animateFloatAsState(
                    targetValue = if (isExpanded) 0f else 180f,
                    animationSpec = AppMotion.FastSpring,
                    label = "expandRotation"
            )

    val categoryIcon =
            when (category) {
                SpoofCategory.DEVICE -> Icons.Outlined.Devices
                SpoofCategory.NETWORK -> Icons.Outlined.Wifi
                SpoofCategory.ADVERTISING -> Icons.Outlined.TrackChanges
                SpoofCategory.SYSTEM -> Icons.Outlined.Settings
                SpoofCategory.LOCATION -> Icons.Outlined.LocationOn
            }

    val categoryColor =
            when (category) {
                SpoofCategory.DEVICE -> Color(0xFF00BCD4)
                SpoofCategory.NETWORK -> Color(0xFF4CAF50)
                SpoofCategory.ADVERTISING -> Color(0xFFFF9800)
                SpoofCategory.SYSTEM -> Color(0xFF9C27B0)
                SpoofCategory.LOCATION -> Color(0xFFE91E63)
            }

    val typesInCategory = SpoofType.byCategory(category)
    val enabledCount = typesInCategory.count { globalConfig.isTypeEnabled(it) }

    ElevatedCard(
            modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = spring()),
            colors =
                    CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
            shape = MaterialTheme.shapes.large
    ) {
        Column {
            // Header
            Row(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded }
                                    .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category icon with colored background
                    Box(
                            modifier =
                                    Modifier.size(40.dp)
                                            .clip(CircleShape)
                                            .background(categoryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                    ) {
                        Icon(
                                imageVector = categoryIcon,
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                        )
                        Text(
                                text = "$enabledCount/${typesInCategory.size} enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                        imageVector = Icons.Filled.ExpandLess,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.rotate(rotationAngle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expandable content
            AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(animationSpec = spring()) + fadeIn(),
                    exit = shrinkVertically(animationSpec = spring()) + fadeOut()
            ) {
                Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    typesInCategory.forEach { type ->
                        GlobalTypeItem(
                                type = type,
                                isEnabled = globalConfig.isTypeEnabled(type),
                                defaultValue = globalConfig.getDefaultValue(type),
                                onToggle = { enabled -> onToggleType(type, enabled) },
                                onRegenerate = { onRegenerateDefault(type) }
                        )
                    }
                }
            }
        }
    }
}

/** Individual spoof type item for global settings. */
@Composable
private fun GlobalTypeItem(
        type: SpoofType,
        isEnabled: Boolean,
        defaultValue: String?,
        onToggle: (Boolean) -> Unit,
        onRegenerate: () -> Unit
) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium
    ) {
        Column(
                modifier =
                        Modifier.fillMaxWidth().alpha(if (isEnabled) 1f else 0.5f).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top row: Name and toggle
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                            text = type.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                    )
                    Text(
                            text = type.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                    )
                }
                Switch(checked = isEnabled, onCheckedChange = onToggle)
            }

            // Bottom row: Default value and regenerate
            if (isEnabled) {
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = "Default Value",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                                text = defaultValue ?: "Not set",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color =
                                        if (defaultValue != null) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalIconButton(
                            onClick = onRegenerate,
                            modifier = Modifier.size(32.dp),
                            colors =
                                    IconButtonDefaults.filledTonalIconButtonColors(
                                            containerColor =
                                                    MaterialTheme.colorScheme.secondaryContainer
                                    )
                    ) {
                        Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Regenerate default",
                                modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
