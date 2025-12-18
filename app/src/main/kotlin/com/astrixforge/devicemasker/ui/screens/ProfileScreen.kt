package com.astrixforge.devicemasker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.data.models.SpoofProfile
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.ui.components.ProfileCard
import com.astrixforge.devicemasker.ui.components.expressive.CompactExpressiveIconButton
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen for managing spoof profiles.
 *
 * Features:
 * - List of saved profiles
 * - Create new profile via FAB
 * - Edit profile details
 * - Delete profiles (except default)
 * - Set profile as default
 * - Export/Import profiles as JSON
 *
 * @param repository The SpoofRepository for data access
 * @param onProfileClick Callback when a profile is clicked
 * @param modifier Optional modifier
 */
@Composable
fun ProfileScreen(
        repository: SpoofRepository,
        onProfileClick: (SpoofProfile) -> Unit,
        modifier: Modifier = Modifier,
) {
    val profiles by repository.getAllProfiles().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<SpoofProfile?>(null) }
    var showDeleteDialog by remember { mutableStateOf<SpoofProfile?>(null) }

    // Export launcher - creates a JSON file
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val jsonData = repository.exportProfiles()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonData.toByteArray())
                    }
                }
            }
        }
    }

    // Import launcher - reads a JSON file
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        val jsonData = inputStream.bufferedReader().readText()
                        repository.importProfiles(jsonData)
                    }
                }
            }
        }
    }

    ProfileScreenContent(
            profiles = profiles,
            onProfileClick = onProfileClick,
            onCreateProfile = { showCreateDialog = true },
            onEditProfile = { showEditDialog = it },
            onDeleteProfile = { showDeleteDialog = it },
            onSetDefault = { profile ->
                scope.launch { repository.setDefaultProfile(profile.id) }
            },
            onEnableChange = { profile, enabled ->
                scope.launch { repository.setProfileEnabled(profile.id, enabled) }
            },
            onExport = {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                exportLauncher.launch("devicemasker_profiles_$timestamp.json")
            },
            onImport = {
                importLauncher.launch("application/json")
            },
            modifier = modifier,
    )

    // Create Profile Dialog
    if (showCreateDialog) {
        CreateProfileDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, description ->
                    scope.launch {
                        repository.createProfile(name, description)
                        showCreateDialog = false
                    }
                },
        )
    }

    // Edit Profile Dialog
    showEditDialog?.let { profile ->
        EditProfileDialog(
                profile = profile,
                onDismiss = { showEditDialog = null },
                onSave = { name, description ->
                    scope.launch {
                        repository.updateProfile(
                                profile.copy(
                                        name = name,
                                        description = description,
                                        updatedAt = System.currentTimeMillis(),
                                )
                        )
                        showEditDialog = null
                    }
                },
        )
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { profile ->
        DeleteProfileDialog(
                profile = profile,
                onDismiss = { showDeleteDialog = null },
                onConfirm = {
                    scope.launch {
                        repository.deleteProfile(profile.id)
                        showDeleteDialog = null
                    }
                },
        )
    }
}

/** Stateless content for ProfileScreen. */
@Composable
fun ProfileScreenContent(
        profiles: List<SpoofProfile>,
        onProfileClick: (SpoofProfile) -> Unit,
        onCreateProfile: () -> Unit,
        onEditProfile: (SpoofProfile) -> Unit,
        onDeleteProfile: (SpoofProfile) -> Unit,
        onSetDefault: (SpoofProfile) -> Unit,
        modifier: Modifier = Modifier,
        onEnableChange: (SpoofProfile, Boolean) -> Unit = { _, _ -> },
        onExport: () -> Unit = {},
        onImport: () -> Unit = {},
) {
    // Track scroll position for FAB animation
    val listState = rememberLazyListState()
    val expandedFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header with title and export/import buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                            text = "Profiles",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Import button
                        CompactExpressiveIconButton(
                            onClick = onImport,
                            icon = Icons.Outlined.FileDownload,
                            contentDescription = "Import Profiles",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        
                        // Export button
                        CompactExpressiveIconButton(
                            onClick = onExport,
                            icon = Icons.Outlined.FileUpload,
                            contentDescription = "Export Profiles",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (profiles.isEmpty()) {
                // Empty State
                item {
                    Box(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                    text = "No profiles yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                    text = "Tap + to create your first profile",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color =
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.7f
                                            ),
                            )
                        }
                    }
                }
            } else {
                // Profile List
                items(items = profiles, key = { it.id }) { profile ->
                    ProfileCard(
                            profile = profile,
                            isEnabled = profile.isEnabled,
                            onClick = { onProfileClick(profile) },
                            onEdit = { onEditProfile(profile) },
                            onDelete = { onDeleteProfile(profile) },
                            onSetDefault = { onSetDefault(profile) },
                            onEnableChange = { enabled -> onEnableChange(profile, enabled) },
                    )
                }

                // Bottom spacing for FAB
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        // Scroll-aware FAB - collapses on scroll, expands when at top
        ExtendedFloatingActionButton(
                onClick = onCreateProfile,
                expanded = expandedFab,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("New Profile") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

/** Dialog for creating a new profile. */
@Composable
fun CreateProfileDialog(
        onDismiss: () -> Unit,
        onCreate: (name: String, description: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val isValid = name.isNotBlank()

    AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
            title = { Text("Create Profile") },
            text = {
                Column {
                    val maxNameLength = 12
                    OutlinedTextField(
                            value = name,
                            onValueChange = { if (it.length <= maxNameLength) name = it },
                            label = { Text("Profile Name") },
                            placeholder = { Text("e.g., Samsung") },
                            singleLine = true,
                            supportingText = { Text("${name.length}/$maxNameLength") },
                            modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (optional)") },
                            placeholder = { Text("e.g., For banking apps") },
                            modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                        onClick = { onCreate(name.trim(), description.trim()) },
                        enabled = isValid
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Dialog for editing a profile. */
@Composable
fun EditProfileDialog(
        profile: SpoofProfile,
        onDismiss: () -> Unit,
        onSave: (name: String, description: String) -> Unit,
) {
    var name by remember { mutableStateOf(profile.name) }
    var description by remember { mutableStateOf(profile.description) }
    val isValid = name.isNotBlank()

    AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
            title = { Text("Edit Profile") },
            text = {
                Column {
                    OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Profile Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                        onClick = { onSave(name.trim(), description.trim()) },
                        enabled = isValid
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Dialog for confirming profile deletion. */
@Composable
fun DeleteProfileDialog(profile: SpoofProfile, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("Delete Profile?") },
            text = {
                Text(
                        "Are you sure you want to delete \"${profile.name}\"? " +
                                "Apps using this profile will switch to the default profile."
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ProfileScreenContentPreview() {
    DeviceMaskerTheme {
        ProfileScreenContent(
                profiles =
                        listOf(
                                SpoofProfile.createDefaultProfile(),
                                SpoofProfile.createNew("Samsung Galaxy S24"),
                                SpoofProfile.createNew("Pixel 9 Pro"),
                        ),
                onProfileClick = {},
                onCreateProfile = {},
                onEditProfile = {},
                onDeleteProfile = {},
                onSetDefault = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ProfileScreenEmptyPreview() {
    DeviceMaskerTheme {
        ProfileScreenContent(
                profiles = emptyList(),
                onProfileClick = {},
                onCreateProfile = {},
                onEditProfile = {},
                onDeleteProfile = {},
                onSetDefault = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun CreateProfileDialogPreview() {
    DeviceMaskerTheme { CreateProfileDialog(onDismiss = {}, onCreate = { _, _ -> }) }
}
