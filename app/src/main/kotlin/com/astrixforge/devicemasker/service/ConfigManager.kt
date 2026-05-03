package com.astrixforge.devicemasker.service

import android.content.Context
import android.util.AtomicFile
import com.astrixforge.devicemasker.common.AppConfig
import com.astrixforge.devicemasker.common.DeviceIdentifier
import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.data.ConfigSync
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Config Manager — manages application configuration (libxposed API 101 edition).
 *
 * Responsibilities:
 * 1. Load/save configuration from/to local JSON file (filesDir/config.json)
 * 2. Sync configuration to RemotePreferences via [ConfigSync] for live cross-process delivery
 * 3. Provide [StateFlow] for UI reactivity
 * 4. CRUD operations for groups and app configs
 *
 * Config save path (post-migration):
 * - UI → ConfigManager → local file + ConfigSync → RemotePreferences (live to hooks)
 *
 * No AIDL service calls: config delivery uses [XposedPrefs]/RemotePreferences exclusively. The AIDL
 * service ([ServiceClient]) is diagnostics-only (hook event counts + logs).
 */
object ConfigManager {

    private const val TAG = "ConfigManager"
    private const val CONFIG_FILE_NAME = "config.json"

    private lateinit var configFile: AtomicFile
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory configuration
    private val _config = MutableStateFlow(JsonConfig.createDefault())
    val config: StateFlow<JsonConfig> = _config.asStateFlow()

    // Initialization state
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * Initializes the ConfigManager with application context. Must be called in
     * Application.onCreate() or MainActivity.
     */
    fun init(context: Context) {
        if (_isInitialized.value) {
            Timber.tag(TAG).d("Already initialized")
            return
        }

        appContext = context.applicationContext
        configFile = AtomicFile(File(context.filesDir, CONFIG_FILE_NAME))
        Timber.tag(TAG).d("Config file: ${configFile.baseFile.absolutePath}")

        scope.launch {
            loadConfig()
            _isInitialized.value = true
        }
    }

    /**
     * Loads configuration from local file. Falls back to service config if local doesn't exist and
     * service is connected.
     */
    private suspend fun loadConfig() {
        withContext(Dispatchers.IO) {
            try {
                // Try loading from local file first
                if (configFile.baseFile.exists()) {
                    val json = String(configFile.readFully())
                    val loadedConfig =
                        JsonConfig.parse(json).withDerivedAppConfigsFromAssignedApps()
                    _config.value = loadedConfig
                    ConfigSync.syncFromConfig(appContext, loadedConfig)
                    Timber.tag(TAG).i("Config loaded from local file")
                    return@withContext
                }

                // Use default config
                val defaultConfig = JsonConfig.createDefault()
                _config.value = defaultConfig
                saveConfigInternal(defaultConfig)
                Timber.tag(TAG).i("Created default config")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to load config")
                _config.value = JsonConfig.createDefault()
            }
        }
    }

    /** Saves the current configuration to local file and syncs to service. */
    fun saveConfig() {
        scope.launch { saveConfigInternal(_config.value) }
    }

    fun syncCurrentConfig() {
        if (!::appContext.isInitialized) return
        scope.launch { ConfigSync.syncFromConfig(appContext, _config.value) }
    }

    /**
     * Internal save method.
     *
     * Write path (post-migration):
     * 1. Write JSON to local file (backup / UI reload)
     * 2. Sync flattened per-app keys to RemotePreferences via [ConfigSync] → LSPosed delivers these
     *    live to hooks via `getRemotePreferences()`
     *
     * No AIDL service write — config delivery is exclusively via RemotePreferences.
     */
    private suspend fun saveConfigInternal(config: JsonConfig) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Persist raw JSON locally as a backup and for UI reload on next launch
                val stream = configFile.startWrite()
                try {
                    stream.write(config.toJsonString().toByteArray())
                    configFile.finishWrite(stream)
                } catch (writeError: Exception) {
                    configFile.failWrite(stream)
                    throw writeError
                }
                Timber.tag(TAG).d("Config saved to local file")

                // 2. Flatten per-app keys into RemotePreferences (live delivery to hooks)
                ConfigSync.syncFromConfig(appContext, config)
                Timber.tag(TAG).d("Config synced to RemotePreferences")
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to save config")
            }
        }
    }

    /** Updates the configuration and saves. */
    private fun updateConfig(transform: (JsonConfig) -> JsonConfig) {
        val newConfig = transform(_config.value)
        _config.value = newConfig
        saveConfig()
    }

    // ═══════════════════════════════════════════════════════════
    // Module Settings
    // ═══════════════════════════════════════════════════════════

    /** Gets whether the module is enabled. */
    fun isModuleEnabled(): Boolean = _config.value.isModuleEnabled

    /** Sets whether the module is enabled. */
    fun setModuleEnabled(enabled: Boolean) {
        updateConfig { it.copy(isModuleEnabled = enabled) }
    }

    // ═══════════════════════════════════════════════════════════
    // Group Management
    // ═══════════════════════════════════════════════════════════

    /** Gets all groups. */
    fun getAllGroups(): List<SpoofGroup> = _config.value.groups.values.toList()

    /** Gets a group by ID. */
    fun getGroup(groupId: String): SpoofGroup? = _config.value.getGroup(groupId)

    /** Creates a new group. */
    fun createGroup(name: String, copyFromGroupId: String? = null): SpoofGroup {
        val baseGroup = copyFromGroupId?.let { getGroup(it) }
        val newGroup =
            baseGroup?.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                assignedApps = emptySet(), // Don't copy app assignments
            ) ?: SpoofGroup.createNew(name = name, isDefault = false)

        updateConfig { it.addOrUpdateGroup(newGroup) }
        return newGroup
    }

    /** Updates an existing group. */
    fun updateGroup(group: SpoofGroup) {
        val updatedGroup = group.copy(updatedAt = System.currentTimeMillis())
        updateConfig { it.addOrUpdateGroup(updatedGroup) }
    }

    /** Deletes a group. */
    fun deleteGroup(groupId: String) {
        updateConfig { it.removeGroup(groupId) }
    }

    /** Gets the group for a specific app. */
    fun getGroupForApp(packageName: String): SpoofGroup? {
        return _config.value.getGroupForApp(packageName)
    }

    // ═══════════════════════════════════════════════════════════
    // Group Identifier Management
    // ═══════════════════════════════════════════════════════════

    /** Sets an identifier value in a group. */
    fun setIdentifierValue(groupId: String, type: SpoofType, value: String?) {
        val group = getGroup(groupId) ?: return
        val identifier =
            group.getIdentifier(type)?.copy(value = value)
                ?: DeviceIdentifier.createDefault(type).copy(value = value)
        val updatedGroup = group.setIdentifier(identifier)
        updateGroup(updatedGroup)
    }

    /** Sets whether a spoof type is enabled in a group. */
    fun setTypeEnabled(groupId: String, type: SpoofType, enabled: Boolean) {
        val group = getGroup(groupId) ?: return
        val identifier =
            group.getIdentifier(type)?.copy(isEnabled = enabled)
                ?: DeviceIdentifier.createDefault(type).copy(isEnabled = enabled)
        val updatedGroup = group.setIdentifier(identifier)
        updateGroup(updatedGroup)
    }

    /** Regenerates all values in a group. */
    fun regenerateAllValues(groupId: String) {
        val group = getGroup(groupId) ?: return
        val regenerated = group.regenerateAll()
        updateGroup(regenerated)
    }

    /** Returns the resolved persona seed for a group. */
    fun getPersonaSeed(group: SpoofGroup): String = group.resolvedPersonaSeed()

    /** Returns the version used to identify the current generated persona state. */
    fun getPersonaVersion(group: SpoofGroup): Long = group.updatedAt

    /** Rotates persona metadata while preserving group identity and assignments. */
    fun refreshPersonaLifecycle(group: SpoofGroup): SpoofGroup {
        return group.withPersona(
            seed = UUID.randomUUID().toString(),
            generatedAt = System.currentTimeMillis(),
        )
    }

    // ═══════════════════════════════════════════════════════════
    // App Config Management
    // ═══════════════════════════════════════════════════════════

    /** Gets app config for a package. */
    fun getAppConfig(packageName: String): AppConfig? = _config.value.getAppConfig(packageName)

    /** Assigns an app to a group. */
    fun assignAppToGroup(packageName: String, groupId: String) {
        updateConfig { config ->
            if (config.getGroup(groupId) == null) return@updateConfig config

            val groups =
                config.groups.mapValues { (_, group) ->
                    when {
                        group.id == groupId ->
                            group.copy(assignedApps = group.assignedApps + packageName)
                        packageName in group.assignedApps ->
                            group.copy(assignedApps = group.assignedApps - packageName)
                        else -> group
                    }
                }
            val appConfig =
                config.getAppConfig(packageName)?.copy(groupId = groupId, isEnabled = true)
                    ?: AppConfig(packageName = packageName, groupId = groupId)
            config.copy(groups = groups).setAppConfig(appConfig)
        }
    }

    /** Unassigns an app from its group. */
    fun unassignApp(packageName: String) {
        updateConfig { config ->
            val groups =
                config.groups.mapValues { (_, group) ->
                    if (packageName in group.assignedApps) {
                        group.copy(assignedApps = group.assignedApps - packageName)
                    } else {
                        group
                    }
                }
            config.copy(groups = groups).removeAppConfig(packageName)
        }
    }

    /** Sets whether spoofing is enabled for an app. */
    fun setAppEnabled(packageName: String, enabled: Boolean) {
        val appConfig =
            getAppConfig(packageName)?.copy(isEnabled = enabled)
                ?: AppConfig(packageName = packageName, isEnabled = enabled)
        updateConfig { it.setAppConfig(appConfig) }
    }
}
