package com.astrixforge.devicemasker.ui.screens.groups

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.data.models.SpoofGroup
import com.astrixforge.devicemasker.ui.components.EmptyState
import com.astrixforge.devicemasker.ui.components.GroupCard
import com.astrixforge.devicemasker.ui.components.ScreenHeader
import com.astrixforge.devicemasker.ui.components.expressive.ExpressivePullToRefresh
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Screen for managing spoof groups.
 *
 * Features:
 * - List of saved groups
 * - Create new group via FAB
 * - Edit group details
 * - Delete groups (except default)
 * - Set group as default
 * - Export/Import groups as JSON
 *
 * @param viewModel The GroupsViewModel for state management
 * @param onGroupClick Callback when a group is clicked
 * @param modifier Optional modifier
 */
@Composable
fun GroupsScreen(
    viewModel: GroupsViewModel,
    onGroupClick: (SpoofGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var editGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    val editGroup = state.groups.find { it.id == editGroupId }
    val deleteGroup = state.groups.find { it.id == deleteGroupId }

    // Export launcher - creates a JSON file
    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json")
        ) { uri: Uri? ->
            uri?.let {
                viewModel.exportGroups { result ->
                    scope.launch {
                        val message =
                            result.fold(
                                onSuccess = { jsonData ->
                                    runCatching {
                                            withContext(Dispatchers.IO) {
                                                val outputStream =
                                                    context.contentResolver.openOutputStream(uri)
                                                checkNotNull(outputStream)
                                                outputStream.use { it.write(jsonData.toByteArray()) }
                                            }
                                        }
                                        .fold(
                                            onSuccess = {
                                                context.getString(R.string.group_export_success)
                                            },
                                            onFailure = {
                                                context.getString(R.string.group_export_error)
                                            },
                                        )
                                },
                                onFailure = { context.getString(R.string.group_export_error) },
                            )
                        snackbarHostState.showSnackbar(message)
                    }
                }
            }
        }

    // Import launcher - reads a JSON file
    val importLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            uri: Uri? ->
            uri?.let {
                scope.launch {
                    val jsonResult =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    checkNotNull(inputStream)
                                    inputStream.bufferedReader().use { reader -> reader.readText() }
                                }
                                .getOrNull()
                        }

                    if (jsonResult.isNullOrBlank()) {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.group_import_empty_error)
                        )
                    } else {
                        viewModel.importGroups(jsonResult) { success ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    context.getString(
                                        if (success) {
                                            R.string.group_import_success
                                        } else {
                                            R.string.group_import_error
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

    Box(modifier = modifier.fillMaxSize()) {
        GroupsScreenContent(
            groups = state.groups,
            isRefreshing = state.isRefreshing,
            onGroupClick = onGroupClick,
            onCreateGroup = { showCreateDialog = true },
            onEditGroup = { editGroupId = it.id },
            onDeleteGroup = { deleteGroupId = it.id },
            onSetDefault = { group -> viewModel.setDefaultGroup(group.id) },
            onEnableChange = { group, enabled -> viewModel.setGroupEnabled(group.id, enabled) },
            onExport = {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                exportLauncher.launch("devicemasker_groups_$timestamp.json")
            },
            onImport = { importLauncher.launch("application/json") },
            modifier = Modifier.fillMaxSize(),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }

    // Create Group Dialog
    if (showCreateDialog) {
        CreateGroupDialog(
            existingNames = state.groups.map { it.name },
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description ->
                viewModel.createGroup(name, description)
                showCreateDialog = false
            },
        )
    }

    // Edit Group Dialog
    editGroup?.let { group ->
        EditGroupDialog(
            group = group,
            onDismiss = { editGroupId = null },
            onSave = { name, description ->
                viewModel.updateGroup(
                    group.copy(
                        name = name,
                        description = description,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
                editGroupId = null
            },
        )
    }

    // Delete Confirmation Dialog
    deleteGroup?.let { group ->
        DeleteGroupDialog(
            onDismiss = { deleteGroupId = null },
            onConfirm = {
                viewModel.deleteGroup(group.id)
                deleteGroupId = null
            },
        )
    }
}

/** Stateless content for GroupsScreen. */
@Composable
fun GroupsScreenContent(
    groups: List<SpoofGroup>,
    isRefreshing: Boolean,
    onGroupClick: (SpoofGroup) -> Unit,
    onCreateGroup: () -> Unit,
    onEditGroup: (SpoofGroup) -> Unit,
    onDeleteGroup: (SpoofGroup) -> Unit,
    onSetDefault: (SpoofGroup) -> Unit,
    modifier: Modifier = Modifier,
    onEnableChange: (SpoofGroup, Boolean) -> Unit = { _, _ -> },
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
) {
    // Track scroll position for FAB animation
    val listState = rememberLazyListState()
    val expandedFab by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    Box(modifier = modifier.fillMaxSize()) {
        ExpressivePullToRefresh(isRefreshing = isRefreshing, onRefresh = {}) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header with title and overflow menu
                item {
                    var showMenu by remember { mutableStateOf(false) }

                    ScreenHeader(
                        title = stringResource(id = R.string.group_screen_title),
                        actions = {
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription =
                                            stringResource(id = R.string.action_more_options),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(id = R.string.group_import_groups))
                                        },
                                        onClick = {
                                            showMenu = false
                                            onImport()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.FileDownload,
                                                contentDescription = null,
                                            )
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(stringResource(id = R.string.group_export_groups))
                                        },
                                        onClick = {
                                            showMenu = false
                                            onExport()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Outlined.FileUpload,
                                                contentDescription = null,
                                            )
                                        },
                                    )
                                }
                            }
                        },
                    )
                }

                if (groups.isEmpty()) {
                    // Empty State
                    item {
                        EmptyState(
                            icon = Icons.Default.Groups,
                            title = stringResource(id = R.string.group_list_empty),
                            subtitle = stringResource(id = R.string.group_create_new),
                        )
                    }
                } else {
                    // Group List
                    items(items = groups, key = { it.id }) { group ->
                        GroupCard(
                            group = group,
                            isEnabled = group.isEnabled,
                            onClick = { onGroupClick(group) },
                            onEdit = { onEditGroup(group) },
                            onDelete = { onDeleteGroup(group) },
                            onSetDefault = { onSetDefault(group) },
                            onEnableChange = { enabled -> onEnableChange(group, enabled) },
                        )
                    }

                    // Bottom spacing for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Scroll-aware FAB - collapses on scroll, expands when at top
        ExtendedFloatingActionButton(
            onClick = onCreateGroup,
            expanded = expandedFab,
            icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(id = R.string.group_new)) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

/** Dialog for creating a new group. */
@Composable
fun CreateGroupDialog(
    existingNames: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val nameExists = existingNames.any { it.equals(name.trim(), ignoreCase = true) }
    val isValid = name.isNotBlank() && !nameExists

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
        title = { Text(stringResource(id = R.string.group_create_new)) },
        text = {
            Column {
                val maxNameLength = 12
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= maxNameLength) name = it },
                    label = { Text(stringResource(id = R.string.group_name_hint)) },
                    placeholder = { Text(stringResource(id = R.string.group_name_example)) },
                    singleLine = true,
                    isError = nameExists,
                    supportingText = {
                        if (nameExists) {
                            Text(
                                text = stringResource(id = R.string.group_name_exists),
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            Text(
                                stringResource(
                                    id = R.string.group_name_length,
                                    name.length,
                                    maxNameLength,
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(id = R.string.group_description_hint)) },
                    placeholder = { Text(stringResource(id = R.string.group_description_example)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name.trim(), description.trim()) }, enabled = isValid) {
                Text(stringResource(id = R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.action_cancel)) }
        },
    )
}

/** Dialog for editing a group. */
@Composable
fun EditGroupDialog(
    group: SpoofGroup,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String) -> Unit,
) {
    var name by remember { mutableStateOf(group.name) }
    var description by remember { mutableStateOf(group.description) }
    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.Groups, contentDescription = null) },
        title = { Text(stringResource(id = R.string.group_edit_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.group_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(id = R.string.group_description_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim(), description.trim()) }, enabled = isValid) {
                Text(stringResource(id = R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.action_cancel)) }
        },
    )
}

/** Dialog for confirming group deletion. */
@Composable
fun DeleteGroupDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(stringResource(id = R.string.group_delete_dialog_title)) },
        text = {
            Text(
                stringResource(id = R.string.group_delete_confirm) +
                    " " +
                    stringResource(id = R.string.group_delete_warning)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    stringResource(id = R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.action_cancel)) }
        },
    )
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun GroupsScreenContentPreview() {
    DeviceMaskerTheme {
        GroupsScreenContent(
            groups =
                listOf(
                    SpoofGroup.createDefaultGroup(),
                    SpoofGroup.createNew("Samsung Galaxy S24"),
                    SpoofGroup.createNew("Pixel 9 Pro"),
                ),
            isRefreshing = false,
            onGroupClick = {},
            onCreateGroup = {},
            onEditGroup = {},
            onDeleteGroup = {},
            onSetDefault = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun GroupsScreenEmptyPreview() {
    DeviceMaskerTheme {
        GroupsScreenContent(
            groups = emptyList(),
            isRefreshing = false,
            onGroupClick = {},
            onCreateGroup = {},
            onEditGroup = {},
            onDeleteGroup = {},
            onSetDefault = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun CreateGroupDialogPreview() {
    DeviceMaskerTheme { CreateGroupDialog(onDismiss = {}, onCreate = { _, _ -> }) }
}
